package com.example.core

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object PhoneControlHelper {
    private const val TAG = "PhoneControl"

    private val weatherHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun toggleFlashlight(context: Context, enable: Boolean): Result<Boolean> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.failure(IllegalStateException("Flashlight hardware unavailable."))
            cameraManager.setTorchMode(cameraId, enable)
            Result.success(enable)
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight toggle failed", e)
            Result.failure(e)
        }
    }

    fun getVolumePercent(context: Context, streamType: Int): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audioManager.getStreamMaxVolume(streamType).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(streamType)
        return ((cur.toFloat() / max) * 100f).toInt().coerceIn(0, 100)
    }

    fun setVolumePercent(context: Context, streamType: Int, percent: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = audioManager.getStreamMaxVolume(streamType)
            val target = ((percent.coerceIn(0, 100) / 100f) * max).toInt()
            audioManager.setStreamVolume(streamType, target, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Volume adjustment failed", e)
        }
    }

    fun getInstalledLaunchableApps(context: Context): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }
            resolved.mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: pkg
                InstalledAppInfo(appName = label, packageName = pkg)
            }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query launchable apps", e)
            emptyList()
        }
    }

    fun launchPackage(context: Context, packageName: String): Result<String> {
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return Result.failure(IllegalStateException("App cannot be launched directly."))
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
            Result.success("Opened app")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openDialer(context: Context, phoneNumber: String): Result<Unit> {
        return try {
            val sanitized = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            val uri = if (sanitized.isNotBlank()) Uri.parse("tel:$sanitized") else Uri.parse("tel:")
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Dialer launch failed", e)
            Result.failure(e)
        }
    }

    fun openSmsComposer(context: Context, phoneNumber: String, messageBody: String): Result<Unit> {
        return try {
            val sanitized = phoneNumber.filter { it.isDigit() || it == '+' }
            val uri = Uri.parse("smsto:${Uri.encode(sanitized)}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "SMS composer launch failed", e)
            Result.failure(e)
        }
    }

    fun openExternalUrlOrApp(context: Context, url: String, fallbackPackage: String? = null): Result<Unit> {
        return try {
            if (!fallbackPackage.isNullOrBlank()) {
                val launch = context.packageManager.getLaunchIntentForPackage(fallbackPackage)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launch)
                    return Result.success(Unit)
                }
            }
            val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app/url", e)
            Result.failure(e)
        }
    }

    fun openAppByKeyword(context: Context, query: String): Result<String> {
        val q = query.lowercase().trim()
        val matchedInstalled = getInstalledLaunchableApps(context)
            .firstOrNull { it.appName.lowercase() == q || q.contains(it.appName.lowercase()) }
        if (matchedInstalled != null) {
            return launchPackage(context, matchedInstalled.packageName)
                .map { "Opening ${matchedInstalled.appName}" }
        }
        return when {
            "youtube" in q -> {
                openExternalUrlOrApp(context, "https://www.youtube.com", "com.google.android.youtube")
                    .map { "Opening YouTube" }
            }
            "instagram" in q -> {
                openExternalUrlOrApp(context, "https://www.instagram.com", "com.instagram.android")
                    .map { "Opening Instagram" }
            }
            "spotify" in q || "music" in q -> {
                openExternalUrlOrApp(context, "https://open.spotify.com", "com.spotify.music")
                    .map { "Opening Spotify" }
            }
            "whatsapp" in q -> {
                openExternalUrlOrApp(context, "https://wa.me", "com.whatsapp")
                    .map { "Opening WhatsApp" }
            }
            "map" in q || "navigation" in q -> {
                openExternalUrlOrApp(context, "https://maps.google.com", "com.google.android.apps.maps")
                    .map { "Opening Maps" }
            }
            "setting" in q -> {
                openSystemSettings(context, Settings.ACTION_SETTINGS).map { "Opening Phone Settings" }
            }
            else -> {
                openExternalUrlOrApp(context, "https://www.google.com/search?q=${Uri.encode(query)}")
                    .map { "Searching for $query" }
            }
        }
    }

    fun setAlarm(context: Context, hour: Int, minute: Int, message: String): Result<Unit> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Set alarm failed", e)
            Result.failure(e)
        }
    }

    fun setTimer(context: Context, seconds: Int, message: String): Result<Unit> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(5))
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Set timer failed", e)
            Result.failure(e)
        }
    }

    fun openSystemSettings(context: Context, action: String): Result<Unit> {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                Result.success(Unit)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    fun openMapNavigation(context: Context, lat: Double, lng: Double, label: String): Result<Unit> {
        return try {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            openExternalUrlOrApp(context, "https://maps.google.com/?q=$lat,$lng")
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Result<Uri> {
        return try {
            val fileName = "MEENA_${title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(28)}_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MeenaAIPro")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Could not create MediaStore entry."))
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Result.success(uri)
            } else {
                val dir = File(context.filesDir, "generated_images").apply { mkdirs() }
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
                Result.success(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Save bitmap failed", e)
            Result.failure(e)
        }
    }

    fun shareImageFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    suspend fun fetchLiveWeather(cityQuery: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanCity = cityQuery.trim().ifBlank { "New Delhi" }
            val geoReq = Request.Builder()
                .url("https://geocoding-api.open-meteo.com/v1/search?name=${Uri.encode(cleanCity)}&count=1&language=en&format=json")
                .build()
            val (lat, lon, resolvedName) = weatherHttpClient.newCall(geoReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Geocoding HTTP ${resp.code}")
                val json = JSONObject(resp.body?.string().orEmpty())
                val arr = json.optJSONArray("results")
                if (arr == null || arr.length() == 0) {
                    throw IllegalStateException("City '$cleanCity' not found.")
                }
                val first = arr.getJSONObject(0)
                Triple(
                    first.getDouble("latitude"),
                    first.getDouble("longitude"),
                    "${first.getString("name")}, ${first.optString("country", "")}".trimEnd(',', ' ')
                )
            }

            val forecastReq = Request.Builder()
                .url("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,weather_code&timezone=auto")
                .build()
            weatherHttpClient.newCall(forecastReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Weather HTTP ${resp.code}")
                val json = JSONObject(resp.body?.string().orEmpty())
                val cur = json.getJSONObject("current")
                val temp = cur.getDouble("temperature_2m")
                val feels = cur.getDouble("apparent_temperature")
                val humidity = cur.getInt("relative_humidity_2m")
                val wind = cur.getDouble("wind_speed_10m")
                val code = cur.getInt("weather_code")
                val condition = weatherCodeSummary(code)
                Result.success("$resolvedName\n$condition • ${temp}°C (Feels like ${feels}°C)\nHumidity: $humidity% • Wind: $wind km/h")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun weatherCodeSummary(code: Int): String = when (code) {
        0 -> "Clear Sky ☀️"
        1, 2, 3 -> "Partly Cloudy ⛅"
        45, 48 -> "Foggy / Misty 🌫️"
        51, 53, 55, 61, 63, 65 -> "Rain Showers 🌧️"
        71, 73, 75 -> "Snowfall ❄️"
        80, 81, 82 -> "Heavy Rain 🌦️"
        95, 96, 99 -> "Thunderstorm ⛈️"
        else -> "Moderate Conditions"
    }
}
