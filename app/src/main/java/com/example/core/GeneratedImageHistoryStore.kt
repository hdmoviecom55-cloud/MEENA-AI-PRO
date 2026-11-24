package com.example.core

import android.content.Context
import android.graphics.Bitmap
import com.example.data.model.GeneratedImageRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class GeneratedImageHistoryStore(context: Context) {

    private val imagesDir = File(context.applicationContext.filesDir, "generated_images").apply { mkdirs() }
    private val indexFile = File(imagesDir, "history.json")

    private val _history = MutableStateFlow(loadRecords())
    val history: StateFlow<List<GeneratedImageRecord>> = _history.asStateFlow()

    fun saveGeneratedImage(
        bitmap: Bitmap,
        prompt: String,
        style: String,
        aspectRatio: String
    ): GeneratedImageRecord {
        val id = UUID.randomUUID().toString()
        val file = File(imagesDir, "meena_img_${System.currentTimeMillis()}_${id.take(6)}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val record = GeneratedImageRecord(
            id = id,
            prompt = prompt.trim().take(300),
            style = style,
            aspectRatio = aspectRatio,
            filePath = file.absolutePath,
            createdAtMillis = System.currentTimeMillis()
        )
        val updated = (listOf(record) + _history.value).take(40)
        persistRecords(updated)
        _history.value = updated
        return record
    }

    fun deleteRecord(recordId: String) {
        val target = _history.value.firstOrNull { it.id == recordId }
        if (target != null) {
            runCatching { File(target.filePath).delete() }
        }
        val updated = _history.value.filterNot { it.id == recordId }
        persistRecords(updated)
        _history.value = updated
    }

    fun clearAll() {
        _history.value.forEach { record ->
            runCatching { File(record.filePath).delete() }
        }
        persistRecords(emptyList())
        _history.value = emptyList()
    }

    private fun loadRecords(): List<GeneratedImageRecord> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val raw = indexFile.readText()
            if (raw.isBlank()) return emptyList()
            val arr = JSONArray(raw)
            val list = mutableListOf<GeneratedImageRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val path = obj.getString("filePath")
                if (File(path).exists()) {
                    list.add(
                        GeneratedImageRecord(
                            id = obj.getString("id"),
                            prompt = obj.getString("prompt"),
                            style = obj.optString("style", "Studio"),
                            aspectRatio = obj.optString("aspectRatio", "1:1"),
                            filePath = path,
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistRecords(records: List<GeneratedImageRecord>) {
        try {
            val arr = JSONArray()
            records.forEach { rec ->
                arr.put(
                    JSONObject().apply {
                        put("id", rec.id)
                        put("prompt", rec.prompt)
                        put("style", rec.style)
                        put("aspectRatio", rec.aspectRatio)
                        put("filePath", rec.filePath)
                        put("createdAtMillis", rec.createdAtMillis)
                    }
                )
            }
            indexFile.writeText(arr.toString())
        } catch (_: Exception) {
        }
    }
}
