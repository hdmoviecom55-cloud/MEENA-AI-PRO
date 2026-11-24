package com.example.core

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.AssistantStatus
import com.example.data.model.MinaLanguage
import com.example.data.model.VoiceEmotionStyle
import com.example.data.model.VoicePipelineStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class VoiceEngine(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var activeElevenLabsJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow(AssistantStatus.READY)
    val status: StateFlow<AssistantStatus> = _status.asStateFlow()

    private val _pipelineStage = MutableStateFlow(VoicePipelineStage.IDLE)
    val pipelineStage: StateFlow<VoicePipelineStage> = _pipelineStage.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
    val availableVoices: StateFlow<List<String>> = _availableVoices.asStateFlow()

    private var currentLanguage: MinaLanguage = MinaLanguage.HINGLISH
    private var currentSpeechRate: Float = 1.0f
    private var currentPitch: Float = 1.08f
    private var currentVolume: Float = 0.92f
    private var currentEmotion: VoiceEmotionStyle = VoiceEmotionStyle.WARM_COMPANION
    private var preferredVoiceHint: String = "Meena Expressive Female"
    private var elevenLabsKeyProvider: (() -> String?)? = null

    private var onSpeakFinishedCallback: (() -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "TTS init failed", e)
        }
    }

    fun attachElevenLabsKeyProvider(provider: () -> String?) {
        elevenLabsKeyProvider = provider
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            applyTtsConfig()
            try {
                val voices = tts?.voices
                    ?.filter { it.locale.language in listOf("hi", "en") }
                    ?.map { it.name }
                    ?.distinct()
                    ?.take(12)
                    ?: emptyList()
                _availableVoices.value = listOf("Meena Expressive Female", "Meena Sweet Companion", "ElevenLabs Neural") + voices
            } catch (e: Exception) {
                Log.w(TAG, "Unable to list TTS voices", e)
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _status.value = AssistantStatus.SPEAKING
                    _pipelineStage.value = VoicePipelineStage.SPEAK
                }

                override fun onDone(utteranceId: String?) {
                    _status.value = AssistantStatus.READY
                    _pipelineStage.value = VoicePipelineStage.IDLE
                    _amplitude.value = 0f
                    onSpeakFinishedCallback?.invoke()
                    onSpeakFinishedCallback = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _status.value = AssistantStatus.READY
                    _pipelineStage.value = VoicePipelineStage.IDLE
                    _amplitude.value = 0f
                }
            })
        } else {
            Log.w(TAG, "TTS initialization returned status $status")
        }
    }

    fun configure(
        language: MinaLanguage,
        speechRate: Float,
        pitch: Float,
        volume: Float,
        emotion: VoiceEmotionStyle,
        voiceName: String
    ) {
        currentLanguage = language
        currentSpeechRate = speechRate.coerceIn(0.6f, 1.8f)
        currentPitch = pitch.coerceIn(0.75f, 1.4f)
        currentVolume = volume.coerceIn(0.1f, 1.0f)
        currentEmotion = emotion
        preferredVoiceHint = voiceName
        applyTtsConfig()
    }

    private fun applyTtsConfig() {
        val engine = tts ?: return
        if (!ttsReady) return
        val locale = when (currentLanguage) {
            MinaLanguage.HINDI -> Locale("hi", "IN")
            MinaLanguage.HINGLISH -> Locale("en", "IN")
            MinaLanguage.ENGLISH -> Locale.US
        }
        val res = engine.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        val emotionPitchDelta = when (currentEmotion) {
            VoiceEmotionStyle.PLAYFUL_FUNNY -> 0.05f
            VoiceEmotionStyle.CALM_SOOTHING -> -0.04f
            VoiceEmotionStyle.ENERGETIC_CHEERFUL -> 0.06f
            else -> 0f
        }
        val emotionRateDelta = when (currentEmotion) {
            VoiceEmotionStyle.CALM_SOOTHING -> -0.06f
            VoiceEmotionStyle.ENERGETIC_CHEERFUL -> 0.06f
            else -> 0f
        }
        engine.setSpeechRate((currentSpeechRate + emotionRateDelta).coerceIn(0.6f, 1.8f))
        engine.setPitch((currentPitch + emotionPitchDelta).coerceIn(0.75f, 1.4f))

        try {
            val voices = engine.voices ?: return
            val exact = voices.firstOrNull { it.name == preferredVoiceHint }
            val femaleMatch = voices.firstOrNull { voice ->
                voice.locale.language == locale.language &&
                    (voice.name.contains("female", ignoreCase = true) ||
                        voice.name.contains("-f-", ignoreCase = true) ||
                        voice.name.contains("in-x-", ignoreCase = true))
            }
            (exact ?: femaleMatch)?.let { engine.voice = it }
        } catch (e: Exception) {
            Log.w(TAG, "Voice selection skipped", e)
        }
    }

    fun speakTestPhrase(assistantName: String, language: MinaLanguage) {
        val phrase = when (language) {
            MinaLanguage.HINDI -> "नमस्ते! मैं $assistantName हूँ, आपकी पर्सनल AI साथी। बताइए, आज मैं आपकी क्या मदद कर सकती हूँ?"
            MinaLanguage.HINGLISH -> "Hey! Main $assistantName hoon, aapki personal AI companion! Haha, bolo aaj kya amazing karein?"
            MinaLanguage.ENGLISH -> "Hi there! I'm $assistantName, your personal AI companion. Ready whenever you want to talk!"
        }
        speak(phrase)
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        val clean = text
            .replace(Regex("[*#`_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (clean.isBlank()) {
            onComplete?.invoke()
            return
        }

        val elevenKey = elevenLabsKeyProvider?.invoke()?.takeIf { it.isNotBlank() }
        val wantsElevenLabs = elevenKey != null &&
            (preferredVoiceHint.contains("ElevenLabs", ignoreCase = true) || elevenKey.startsWith("sk_"))

        if (wantsElevenLabs && elevenKey != null) {
            speakViaElevenLabsOrFallback(clean, elevenKey, onComplete)
            return
        }

        speakWithAndroidTts(clean, onComplete)
    }

    private fun speakViaElevenLabsOrFallback(
        cleanText: String,
        apiKey: String,
        onComplete: (() -> Unit)?
    ) {
        stopSpeaking()
        onSpeakFinishedCallback = onComplete
        _status.value = AssistantStatus.SPEAKING
        _pipelineStage.value = VoicePipelineStage.SPEAK

        activeElevenLabsJob = engineScope.launch {
            val audioFile = withContext(Dispatchers.IO) {
                fetchElevenLabsAudio(cleanText.take(1000), apiKey)
            }
            if (audioFile != null) {
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(audioFile.absolutePath)
                        setVolume(currentVolume, currentVolume)
                        setOnCompletionListener { mp ->
                            mp.release()
                            mediaPlayer = null
                            audioFile.delete()
                            _status.value = AssistantStatus.READY
                            _pipelineStage.value = VoicePipelineStage.IDLE
                            _amplitude.value = 0f
                            onSpeakFinishedCallback?.invoke()
                            onSpeakFinishedCallback = null
                        }
                        setOnErrorListener { mp, _, _ ->
                            mp.release()
                            mediaPlayer = null
                            audioFile.delete()
                            speakWithAndroidTts(cleanText, onSpeakFinishedCallback)
                            true
                        }
                        prepare()
                        start()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "ElevenLabs MediaPlayer failed, falling back to Android TTS", e)
                    audioFile.delete()
                    speakWithAndroidTts(cleanText, onComplete)
                }
            } else {
                speakWithAndroidTts(cleanText, onComplete)
            }
        }
    }

    private fun fetchElevenLabsAudio(text: String, apiKey: String): File? {
        return try {
            val bodyJson = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.45)
                    put("similarity_boost", 0.78)
                    put("style", 0.35)
                })
            }
            val req = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$ELEVENLABS_FEMALE_VOICE_ID")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "audio/mpeg")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "ElevenLabs HTTP ${resp.code}")
                    return null
                }
                val bytes = resp.body?.bytes() ?: return null
                val out = File(appContext.cacheDir, "meena_tts_${System.currentTimeMillis()}.mp3")
                out.writeBytes(bytes)
                out
            }
        } catch (e: Exception) {
            Log.w(TAG, "ElevenLabs request failed, using Android TTS fallback", e)
            null
        }
    }

    private fun speakWithAndroidTts(clean: String, onComplete: (() -> Unit)?) {
        if (!ttsReady || tts == null) {
            _status.value = AssistantStatus.READY
            _pipelineStage.value = VoicePipelineStage.IDLE
            onComplete?.invoke()
            return
        }
        applyTtsConfig()
        onSpeakFinishedCallback = onComplete
        _status.value = AssistantStatus.SPEAKING
        _pipelineStage.value = VoicePipelineStage.SPEAK
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
        }
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
    }

    fun stopSpeaking() {
        activeElevenLabsJob?.cancel()
        activeElevenLabsJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {
        }
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        onSpeakFinishedCallback = null
        if (_status.value == AssistantStatus.SPEAKING) {
            _status.value = AssistantStatus.READY
        }
        if (_pipelineStage.value == VoicePipelineStage.SPEAK) {
            _pipelineStage.value = VoicePipelineStage.IDLE
        }
        _amplitude.value = 0f
    }

    fun setUnderstanding() {
        _status.value = AssistantStatus.THINKING
        _pipelineStage.value = VoicePipelineStage.UNDERSTAND
    }

    fun setResponding() {
        _status.value = AssistantStatus.THINKING
        _pipelineStage.value = VoicePipelineStage.RESPOND
    }

    fun setThinking() {
        _status.value = AssistantStatus.THINKING
        _pipelineStage.value = VoicePipelineStage.RESPOND
    }

    fun setReady() {
        _status.value = AssistantStatus.READY
        _pipelineStage.value = VoicePipelineStage.IDLE
        _amplitude.value = 0f
    }

    fun startListening(
        language: MinaLanguage,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError("Speech recognition service is not available on this device.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _status.value = AssistantStatus.LISTENING
                        _pipelineStage.value = VoicePipelineStage.LISTEN
                        _partialTranscript.value = ""
                    }

                    override fun onBeginningOfSpeech() {
                        _status.value = AssistantStatus.LISTENING
                        _pipelineStage.value = VoicePipelineStage.LISTEN
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _amplitude.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _amplitude.value = 0f
                        _pipelineStage.value = VoicePipelineStage.UNDERSTAND
                    }

                    override fun onError(error: Int) {
                        _status.value = AssistantStatus.READY
                        _pipelineStage.value = VoicePipelineStage.IDLE
                        _amplitude.value = 0f
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Sun nahi paayi, please dobara boliye."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Awaaz detect nahi hui, tap mic to try again."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during voice recognition."
                            else -> "Voice input stopped (code $error)."
                        }
                        onError(message)
                    }

                    override fun onResults(results: Bundle?) {
                        _amplitude.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()?.trim().orEmpty()
                        _partialTranscript.value = spoken
                        if (spoken.isNotBlank()) {
                            _status.value = AssistantStatus.THINKING
                            _pipelineStage.value = VoicePipelineStage.UNDERSTAND
                            onResult(spoken)
                        } else {
                            _status.value = AssistantStatus.READY
                            _pipelineStage.value = VoicePipelineStage.IDLE
                            onError("Kuch samajh nahi aaya, please try again.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (partial.isNotBlank()) {
                            _partialTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            _status.value = AssistantStatus.LISTENING
            _pipelineStage.value = VoicePipelineStage.LISTEN
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            _status.value = AssistantStatus.READY
            _pipelineStage.value = VoicePipelineStage.IDLE
            onError(e.localizedMessage ?: "Unable to start microphone.")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _amplitude.value = 0f
        if (_status.value == AssistantStatus.LISTENING) {
            _status.value = AssistantStatus.READY
            _pipelineStage.value = VoicePipelineStage.IDLE
        }
    }

    fun release() {
        engineScope.cancel()
        stopSpeaking()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {
        }
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "VoiceEngine"
        private const val ELEVENLABS_FEMALE_VOICE_ID = "EXAVITQu4vr4xnSDxMaL"
    }
}
