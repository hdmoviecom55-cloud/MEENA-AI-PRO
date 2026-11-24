package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.core.AppCheckManager
import com.example.core.AppCheckProviderMode
import com.example.data.model.AiChatProvider
import com.example.data.model.ChatMessage
import com.example.data.model.DocumentType
import com.example.data.model.ImageArtStyle
import com.example.data.model.ImageQualityOption
import com.example.data.model.MemoryItem
import com.example.data.model.MinaLanguage
import com.example.data.model.MinaPersonality
import com.example.data.model.QuizQuestion
import com.example.data.model.UserProfile
import com.example.data.model.VoiceEmotionStyle
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class NormalizedChatTurn(
    val role: String,
    val text: String
)

object MinaChatHistoryFormatter {

    /**
     * Converts persisted [ChatMessage] history into valid, strictly alternating
     * ("user", "model") pairs ending on a "model" turn so `startChat(history)` +
     * `sendMessageStream(currentUserMessage)` never violates Gemini turn ordering.
     */
    fun normalizeChatHistory(
        history: List<ChatMessage>,
        currentUserMessage: String = "",
        maxTurns: Int = 12
    ): List<NormalizedChatTurn> {
        val merged = mutableListOf<NormalizedChatTurn>()

        for (msg in history.takeLast(maxTurns + 6)) {
            val cleanText = msg.text.trim()
            if (cleanText.isEmpty()) continue
            val role = if (msg.role.equals("USER", ignoreCase = true)) "user" else "model"

            val last = merged.lastOrNull()
            if (last != null && last.role == role) {
                if (last.text != cleanText) {
                    merged[merged.lastIndex] = last.copy(text = "${last.text}\n$cleanText")
                }
            } else {
                merged.add(NormalizedChatTurn(role = role, text = cleanText))
            }
        }

        while (merged.firstOrNull()?.role == "model") {
            merged.removeAt(0)
        }

        if (merged.lastOrNull()?.role == "user") {
            merged.removeAt(merged.lastIndex)
        }

        val capped = if (merged.size > maxTurns) {
            merged.takeLast(maxTurns).toMutableList()
        } else {
            merged
        }

        while (capped.firstOrNull()?.role == "model") {
            capped.removeAt(0)
        }
        if (capped.lastOrNull()?.role == "user") {
            capped.removeAt(capped.lastIndex)
        }

        return capped
    }
}

object MinaAiErrorClassifier {

    fun isAppCheckRelated(error: Throwable): Boolean {
        val combined = collectErrorChainText(error).lowercase()
        return combined.contains("app check") ||
            combined.contains("appcheck") ||
            combined.contains("attestation failed") ||
            combined.contains("403")
    }

    fun classify(
        error: Throwable,
        providerMode: AppCheckProviderMode = AppCheckManager.diagnostics.value.providerMode
    ): String {
        if (error is TimeoutCancellationException) {
            return "MEENA response timed out after 60 seconds. Check your internet connection and retry."
        }
        val combined = collectErrorChainText(error)
        val lower = combined.lowercase()

        return when {
            isAppCheckRelated(error) -> {
                AppCheckManager.classifyAppCheckError(error, providerMode)
            }
            lower.contains("429") || lower.contains("quota") || lower.contains("resource_exhausted") || lower.contains("rate limit") ->
                "AI rate limit or quota reached temporarily. Please wait a moment and retry."
            lower.contains("unable to resolve host") ||
                lower.contains("failed to connect") ||
                lower.contains("network") ||
                lower.contains("unknownhost") ||
                lower.contains("sockettimeout") ||
                lower.contains("timed out") ->
                "Network connection failed while reaching MEENA's AI backend. Check internet and tap Retry."
            lower.contains("404") || (lower.contains("not found") && lower.contains("model")) ->
                "Configured AI model is unavailable on backend."
            lower.contains("permission_denied") || lower.contains("api has not been used") ->
                "Firebase AI Logic API permission denied for this project. Verify Firebase AI Logic provisioning."
            else -> error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Unable to reach MEENA AI right now. Please try again."
        }
    }

    private fun collectErrorChainText(error: Throwable): String {
        val sb = StringBuilder()
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 8) {
            sb.append(current::class.java.simpleName).append(' ')
            current.message?.let { sb.append(it).append(' ') }
            current = current.cause
            depth++
        }
        return sb.toString()
    }
}

class MinaAiRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val generalModel by lazy {
        Firebase.ai.generativeModel(DEFAULT_MODEL)
    }

    private val quizModel by lazy {
        val quizSchema = Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "question" to Schema.string(description = "Quiz question text"),
                            "options" to Schema.array(
                                items = Schema.string(),
                                description = "Four multiple-choice options"
                            ),
                            "correctIndex" to Schema.integer(description = "Zero-based index of correct option (0..3)"),
                            "explanation" to Schema.string(description = "Short friendly explanation")
                        )
                    ),
                    description = "List of 4 quiz questions"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    private fun emotionLabel(level: Int): String = when (level.coerceIn(1, 5)) {
        1 -> "1/5 Calm (serene, grounded, gentle)"
        2 -> "2/5 Friendly (warm, polite, pleasant)"
        3 -> "3/5 Normal (balanced, conversational, helpful)"
        4 -> "4/5 Caring (deeply supportive, warm, expressive)"
        else -> "5/5 Highly Expressive (vibrant, witty, lively, deeply warm)"
    }

    private fun buildSystemPrompt(
        profile: UserProfile,
        memories: List<MemoryItem>,
        assistantName: String = "MEENA",
        voiceEmotion: VoiceEmotionStyle = VoiceEmotionStyle.WARM_COMPANION,
        extraInstruction: String = ""
    ): String {
        val personality = MinaPersonality.fromRaw(profile.personality)
        val language = MinaLanguage.fromRaw(profile.language)
        val memoryBlock = if (memories.isNotEmpty()) {
            memories.take(10).joinToString("\n") { "- ${it.title}: ${it.content}" }
        } else {
            "No saved personal preferences yet."
        }

        return """
            You are $assistantName (MEENA AI PRO), a warm, human-like, lovely, witty, and emotionally intelligent female personal AI companion.
            Talk like a genuine caring friend — avoid stiff robotic boilerplate. Use natural conversational warmth, appropriate gentle humor, and relatable Hindi/English/Hinglish flow.
            User's name: ${profile.displayName}.
            ${if (profile.bio.isNotBlank()) "User bio: ${profile.bio}." else ""}
            Selected Personality: ${personality.displayName} — ${personality.promptGuidance}
            Expressive Tone: ${voiceEmotion.label} — ${voiceEmotion.promptGuidance}
            Emotion Intensity: ${emotionLabel(profile.emotionLevel.toInt())}.
            Language Rule: ${language.promptGuidance}
            Safety & Honesty Boundary: Stay respectful and be honest that you are an AI companion if asked directly, while remaining warm and engaging.
            Remembered Personal Context:
            $memoryBlock
            Keep responses natural, clear, well-structured (use Markdown code blocks for code), and concise unless asked for deep detail.
            ${if (extraInstruction.isNotBlank()) extraInstruction else ""}
        """.trimIndent()
    }

    private suspend fun ensureValidAppCheck(forceRefresh: Boolean) {
        val result = AppCheckManager.verifyAppCheckToken(forceRefresh = forceRefresh)
        if (result.isFailure) {
            throw result.exceptionOrNull()
                ?: IllegalStateException("Firebase App Check token verification failed.")
        }
    }

    private suspend fun <T> executeWithAppCheck(
        forceRefreshInitially: Boolean = false,
        block: suspend () -> T
    ): T {
        ensureValidAppCheck(forceRefresh = forceRefreshInitially)
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!forceRefreshInitially && MinaAiErrorClassifier.isAppCheckRelated(e)) {
                Log.w(TAG, "AI call hit App Check error; refreshing token and retrying once.", e)
                try {
                    ensureValidAppCheck(forceRefresh = true)
                    return block()
                } catch (retryError: CancellationException) {
                    throw retryError
                } catch (retryError: Exception) {
                    Log.e(TAG, "AI retry after App Check refresh failed", retryError)
                    throw IllegalStateException(MinaAiErrorClassifier.classify(retryError), retryError)
                }
            }
            Log.e(TAG, "AI request failed", e)
            throw IllegalStateException(MinaAiErrorClassifier.classify(e), e)
        }
    }

    fun streamChatReply(
        userMessage: String,
        attachedImage: Bitmap? = null,
        history: List<ChatMessage>,
        profile: UserProfile,
        memories: List<MemoryItem>,
        keepChatting: Boolean,
        assistantName: String = "MEENA",
        voiceEmotion: VoiceEmotionStyle = VoiceEmotionStyle.WARM_COMPANION,
        provider: AiChatProvider = AiChatProvider.GEMINI,
        groqApiKey: String? = null
    ): Flow<String> = flow {
        val extraPrompt = if (keepChatting) {
            "After answering, optionally add one brief, natural follow-up question to keep the conversation flowing."
        } else ""

        val systemText = buildSystemPrompt(
            profile = profile,
            memories = memories,
            assistantName = assistantName,
            voiceEmotion = voiceEmotion,
            extraInstruction = extraPrompt
        )

        if (provider == AiChatProvider.GROQ_FAST && attachedImage == null) {
            val key = groqApiKey?.trim()
            if (key.isNullOrBlank()) {
                throw IllegalStateException("Groq API key not configured. Add your Groq key in Connections or switch to Google Gemini.")
            }
            streamGroqReply(
                apiKey = key,
                systemPrompt = systemText,
                userMessage = userMessage.trim(),
                history = MinaChatHistoryFormatter.normalizeChatHistory(history, userMessage)
            ).collect { emit(it) }
            return@flow
        }

        val systemInstructionContent = content { text(systemText) }
        val normalizedHistory: List<Content> = MinaChatHistoryFormatter
            .normalizeChatHistory(history, userMessage)
            .map { turn -> content(turn.role) { text(turn.text) } }

        val chatModel = Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            systemInstruction = systemInstructionContent
        )

        var emittedAnyChunk = false
        suspend fun runStreamAttempt(forceRefreshToken: Boolean) {
            ensureValidAppCheck(forceRefresh = forceRefreshToken)
            val chat = chatModel.startChat(history = normalizedHistory)
            val accumulated = StringBuilder()
            val messageContent = content("user") {
                if (attachedImage != null) {
                    image(scaleBitmapSafely(attachedImage, 1280))
                }
                text(userMessage.trim())
            }
            withTimeout(60_000L) {
                chat.sendMessageStream(messageContent).collect { chunk ->
                    val part = chunk.text.orEmpty()
                    if (part.isNotEmpty()) {
                        emittedAnyChunk = true
                        accumulated.append(part)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("$assistantName returned an empty response. Please try again.")
            }
        }

        try {
            runStreamAttempt(forceRefreshToken = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!emittedAnyChunk && MinaAiErrorClassifier.isAppCheckRelated(e)) {
                Log.w(TAG, "Chat stream hit App Check error; forcing token refresh and retrying once.", e)
                try {
                    runStreamAttempt(forceRefreshToken = true)
                    return@flow
                } catch (retryError: CancellationException) {
                    throw retryError
                } catch (retryError: Exception) {
                    Log.e(TAG, "Chat stream retry after App Check refresh failed", retryError)
                    throw IllegalStateException(MinaAiErrorClassifier.classify(retryError), retryError)
                }
            }
            Log.e(TAG, "Chat stream failed", e)
            throw IllegalStateException(MinaAiErrorClassifier.classify(e), e)
        }
    }

    private fun streamGroqReply(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        history: List<NormalizedChatTurn>
    ): Flow<String> = flow {
        val messagesJson = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            history.forEach { turn ->
                val groqRole = if (turn.role == "user") "user" else "assistant"
                put(JSONObject().put("role", groqRole).put("content", turn.text))
            }
            put(JSONObject().put("role", "user").put("content", userMessage))
        }
        val payload = JSONObject().apply {
            put("model", GROQ_MODEL)
            put("messages", messagesJson)
            put("temperature", 0.78)
            put("stream", true)
        }

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val accumulated = StringBuilder()
        withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errText = response.body?.string().orEmpty()
                    throw IllegalStateException("Groq API error (${response.code}): ${errText.take(180)}")
                }
                val body = response.body ?: throw IllegalStateException("Empty response from Groq.")
                BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        val json = runCatching { JSONObject(data) }.getOrNull() ?: continue
                        val delta = json.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.optString("content")
                            .orEmpty()
                        if (delta.isNotEmpty()) {
                            accumulated.append(delta)
                            emit(accumulated.toString())
                        }
                    }
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Groq returned an empty reply.")
        }
    }

    suspend fun generateAiImage(
        prompt: String,
        style: ImageArtStyle,
        quality: ImageQualityOption
    ): Bitmap = executeWithAppCheck {
        val fullPrompt = "${prompt.trim()}, ${style.promptModifier}"
        val aspect = when (quality) {
            ImageQualityOption.SQUARE_HD -> AspectRatio.SQUARE_1x1
            ImageQualityOption.PORTRAIT_PRO -> AspectRatio.PORTRAIT_3x4
            ImageQualityOption.WIDE_CINEMA -> AspectRatio.LANDSCAPE_16x9
        }
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(aspect)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        val imageModel = Firebase.ai.generativeModel(
            modelName = IMAGE_MODEL,
            generationConfig = config
        )
        val response = withTimeout(60_000L) {
            imageModel.generateContent(fullPrompt)
        }
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        imagePart?.image ?: throw IllegalStateException("No image was generated. Try refining your prompt.")
    }

    suspend fun runTestReply(profile: UserProfile, assistantName: String = "MEENA"): Result<String> {
        return try {
            val reply = executeWithAppCheck(forceRefreshInitially = true) {
                val systemContent = content { text(buildSystemPrompt(profile, emptyList(), assistantName = assistantName)) }
                val model = Firebase.ai.generativeModel(
                    modelName = FAST_MODEL,
                    systemInstruction = systemContent
                )
                val prompt = "Connection test for ${profile.displayName}: please reply with one brief friendly sentence confirming $assistantName AI PRO is connected and ready."
                val response = withTimeout(60_000L) {
                    model.generateContent(prompt)
                }
                response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Gemini backend returned an empty test response.")
            }
            Result.success(reply)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Test reply failed", e)
            Result.failure(IllegalStateException(MinaAiErrorClassifier.classify(e), e))
        }
    }

    suspend fun testCustomGeminiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Gemini key rejected (HTTP ${resp.code})."))
                }
                Result.success("Gemini API Key verified & active!")
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to test Gemini key.", e))
        }
    }

    suspend fun testGroqConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.groq.com/openai/v1/models")
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Groq API key rejected (HTTP ${resp.code})."))
                }
                Result.success("Groq Fast Chat connected ($GROQ_MODEL ready)!")
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to connect to Groq.", e))
        }
    }

    suspend fun testElevenLabsConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.elevenlabs.io/v1/user")
                .addHeader("xi-api-key", apiKey.trim())
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("ElevenLabs key rejected (HTTP ${resp.code})."))
                }
                Result.success("ElevenLabs neural voice connected!")
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to verify ElevenLabs key.", e))
        }
    }

    suspend fun generateSingleReply(
        prompt: String,
        profile: UserProfile,
        memories: List<MemoryItem> = emptyList(),
        useFastModel: Boolean = false,
        assistantName: String = "MEENA",
        voiceEmotion: VoiceEmotionStyle = VoiceEmotionStyle.WARM_COMPANION
    ): String = executeWithAppCheck {
        val systemContent = content {
            text(
                buildSystemPrompt(
                    profile = profile,
                    memories = memories,
                    assistantName = assistantName,
                    voiceEmotion = voiceEmotion
                )
            )
        }
        val targetModel = Firebase.ai.generativeModel(
            modelName = if (useFastModel) FAST_MODEL else DEFAULT_MODEL,
            systemInstruction = systemContent
        )
        val response = withTimeout(60_000L) {
            targetModel.generateContent(prompt)
        }
        response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Empty response from AI.")
    }

    suspend fun analyzeVision(
        bitmap: Bitmap,
        taskPrompt: String,
        profile: UserProfile
    ): String = executeWithAppCheck {
        val scaled = scaleBitmapSafely(bitmap, 1280)
        val language = MinaLanguage.fromRaw(profile.language)
        val prompt = """
            You are MEENA AI PRO's Live Vision assistant speaking to ${profile.displayName}.
            ${language.promptGuidance}
            User request: $taskPrompt
        """.trimIndent()

        val response = withTimeout(60_000L) {
            generalModel.generateContent(
                content {
                    image(scaled)
                    text(prompt)
                }
            )
        }
        response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Could not extract visual insight from the image.")
    }

    suspend fun generateDocument(
        docType: DocumentType,
        details: String,
        profile: UserProfile
    ): String = executeWithAppCheck {
        val language = MinaLanguage.fromRaw(profile.language)
        val prompt = """
            Create a polished, ready-to-use ${docType.label} for ${profile.displayName}.
            User details & instructions:
            $details
            Format cleanly with clear headings and bullet points (plain text / markdown-free clean layout suitable for PDF export).
            Language preference: ${if (docType == DocumentType.RESUME || docType == DocumentType.BIODATA) "Professional English (unless requested otherwise)" else language.displayName}.
        """.trimIndent()
        val response = withTimeout(60_000L) {
            generalModel.generateContent(prompt)
        }
        response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Document generation returned empty output.")
    }

    suspend fun generateCodeAssistant(
        mode: String,
        languageOrStack: String,
        userPrompt: String
    ): String = executeWithAppCheck {
        val prompt = """
            You are MEENA Coding Studio.
            Mode: $mode
            Target Stack/Language: $languageOrStack
            Request: $userPrompt
            If generating HTML/CSS/JS for a website or prototype, provide a complete single-file self-contained HTML document (with embedded <style> and <script>) inside a ```html code block so it can be previewed live.
            If explaining or debugging, provide clear step-by-step analysis and corrected code.
        """.trimIndent()
        val response = withTimeout(60_000L) {
            generalModel.generateContent(prompt)
        }
        response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Code generation returned empty output.")
    }

    suspend fun generateStudyQuiz(
        topic: String,
        language: MinaLanguage
    ): List<QuizQuestion> = executeWithAppCheck {
        val prompt = """
            Generate 4 high-quality multiple-choice quiz questions on the topic: "$topic".
            Language style: ${language.displayName}.
            Each question must have 4 concise options, correctIndex (0 to 3), and a short helpful explanation.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            quizModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("Quiz model returned empty JSON.")
        val root = JSONObject(rawJson)
        val arr = root.getJSONArray("questions")
        val list = mutableListOf<QuizQuestion>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val optsJson = obj.getJSONArray("options")
            val opts = (0 until optsJson.length()).map { optsJson.getString(it) }
            list.add(
                QuizQuestion(
                    question = obj.getString("question"),
                    options = opts,
                    correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                    explanation = obj.optString("explanation", "")
                )
            )
        }
        list
    }

    suspend fun generateJournalReflection(
        title: String,
        content: String,
        mood: String,
        profile: UserProfile
    ): String {
        return try {
            val prompt = """
                The user (${profile.displayName}) wrote a journal entry titled "$title" with mood "$mood":
                "$content"
                Write a short, warm, caring 2-3 sentence personal reflection from MEENA AI PRO in ${MinaLanguage.fromRaw(profile.language).displayName}.
            """.trimIndent()
            generateSingleReply(prompt, profile, useFastModel = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Reflection timed out", e)
            ""
        } catch (e: Exception) {
            Log.w(TAG, "Reflection skipped", e)
            ""
        }
    }

    private fun scaleBitmapSafely(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / maxOf(w, h)
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    companion object {
        private const val TAG = "MinaAiRepository"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        const val FAST_MODEL = "gemini-3.5-flash-lite"
        const val IMAGE_MODEL = "gemini-nano-banana-2.1"
        const val GROQ_MODEL = "llama-3.3-70b-versatile"
    }
}
