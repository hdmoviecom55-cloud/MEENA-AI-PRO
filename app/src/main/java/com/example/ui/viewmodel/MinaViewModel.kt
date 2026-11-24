package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AmbientAudioPlayer
import com.example.core.AppCheckDiagnostics
import com.example.core.AppCheckManager
import com.example.core.CryptoVault
import com.example.core.GeneratedImageHistoryStore
import com.example.core.PdfAndExportHelper
import com.example.core.PhoneControlHelper
import com.example.core.SecureApiKeyVault
import com.example.core.VoiceEngine
import com.example.data.model.AiChatProvider
import com.example.data.model.ApiProviderKey
import com.example.data.model.AppSettingsState
import com.example.data.model.AssistantStatus
import com.example.data.model.ChatMessage
import com.example.data.model.ConnectionKeysState
import com.example.data.model.DocumentType
import com.example.data.model.FavoritePlace
import com.example.data.model.GeneratedImageRecord
import com.example.data.model.ImageArtStyle
import com.example.data.model.ImageQualityOption
import com.example.data.model.JournalEntry
import com.example.data.model.JournalMood
import com.example.data.model.MemoryCategory
import com.example.data.model.MemoryItem
import com.example.data.model.MinaLanguage
import com.example.data.model.MinaPersonality
import com.example.data.model.QuizQuestion
import com.example.data.model.SavedDocument
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VoicePipelineStage
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.MinaAiErrorClassifier
import com.example.data.repository.MinaAiRepository
import com.example.data.repository.MinaFirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class MinaViewModel(
    application: Application,
    private val currentUserId: String,
    private val defaultDisplayName: String,
    private val firestoreRepo: MinaFirestoreRepository,
    private val aiRepo: MinaAiRepository,
    val settingsStore: SettingsDataStore,
    val voiceEngine: VoiceEngine,
    val ambientPlayer: AmbientAudioPlayer,
    val keyVault: SecureApiKeyVault = SecureApiKeyVault(application),
    val imageHistoryStore: GeneratedImageHistoryStore = GeneratedImageHistoryStore(application)
) : AndroidViewModel(application) {

    private fun <T> Flow<T>.toUiStateFlow(errorLabel: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, errorLabel, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: errorLabel))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val settingsState: StateFlow<AppSettingsState> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettingsState())

    val connectionsState: StateFlow<ConnectionKeysState> = keyVault.connectionsState
    val generatedImageHistory: StateFlow<List<GeneratedImageRecord>> = imageHistoryStore.history

    val profileState: StateFlow<UiState<UserProfile>> = firestoreRepo.observeProfile(currentUserId)
        .map { it ?: UserProfile(userId = currentUserId, displayName = defaultDisplayName) }
        .toUiStateFlow("Failed to load profile")

    val memoriesState: StateFlow<UiState<List<MemoryItem>>> = firestoreRepo.observeMemories(currentUserId)
        .toUiStateFlow("Failed to load memories")

    private val firestoreChatsState: StateFlow<UiState<List<ChatMessage>>> = firestoreRepo.observeChats(currentUserId)
        .toUiStateFlow("Failed to load chats")

    private val _localChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _activeConversationStartMillis = MutableStateFlow<Long?>(null)

    val chatsState: StateFlow<UiState<List<ChatMessage>>> = combine(
        firestoreChatsState,
        _localChatMessages,
        _activeConversationStartMillis
    ) { remoteUi, localList, sessionCutoff ->
        val base = when (remoteUi) {
            is UiState.Loading -> if (localList.isNotEmpty()) UiState.Success(localList) else UiState.Loading
            is UiState.Error -> UiState.Success(localList)
            is UiState.Success -> UiState.Success(mergeChatMessages(remoteUi.data, localList))
        }
        if (sessionCutoff == null || base !is UiState.Success) {
            base
        } else {
            UiState.Success(
                base.data.filter { msg ->
                    val ts = msg.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                    ts >= sessionCutoff
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allChatsHistoryState: StateFlow<UiState<List<ChatMessage>>> = combine(
        firestoreChatsState,
        _localChatMessages
    ) { remoteUi, localList ->
        when (remoteUi) {
            is UiState.Loading -> if (localList.isNotEmpty()) UiState.Success(localList) else UiState.Loading
            is UiState.Error -> UiState.Success(localList)
            is UiState.Success -> UiState.Success(mergeChatMessages(remoteUi.data, localList))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val assistantStatus: StateFlow<AssistantStatus> = voiceEngine.status
    val voicePipelineStage: StateFlow<VoicePipelineStage> = voiceEngine.pipelineStage
    val voiceAmplitude: StateFlow<Float> = voiceEngine.amplitude
    val partialTranscript: StateFlow<String> = voiceEngine.partialTranscript

    val journalsState: StateFlow<UiState<List<JournalEntry>>> = firestoreRepo.observeJournals(currentUserId)
        .toUiStateFlow("Failed to load journals")

    val documentsState: StateFlow<UiState<List<SavedDocument>>> = firestoreRepo.observeDocuments(currentUserId)
        .toUiStateFlow("Failed to load documents")

    val placesState: StateFlow<UiState<List<FavoritePlace>>> = firestoreRepo.observePlaces(currentUserId)
        .toUiStateFlow("Failed to load favorite places")

    private val _streamingReply = MutableStateFlow<String?>(null)
    val streamingReply: StateFlow<String?> = _streamingReply.asStateFlow()

    private val _isChatBusy = MutableStateFlow(false)
    val isChatBusy: StateFlow<Boolean> = _isChatBusy.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _lastFailedChatRequest = MutableStateFlow<Pair<String, Boolean>?>(null)
    val canRetryLastChat: StateFlow<Boolean> = _lastFailedChatRequest
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val _attachedChatBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedChatBitmap: StateFlow<Bitmap?> = _attachedChatBitmap.asStateFlow()

    val appCheckDiagnostics: StateFlow<AppCheckDiagnostics> = AppCheckManager.diagnostics

    private val _backendTestState = MutableStateFlow<UiState<String>?>(null)
    val backendTestState: StateFlow<UiState<String>?> = _backendTestState.asStateFlow()

    private val _lastVoiceResponse = MutableStateFlow("")
    val lastVoiceResponse: StateFlow<String> = _lastVoiceResponse.asStateFlow()

    private val _visionState = MutableStateFlow<UiState<String>?>(null)
    val visionState: StateFlow<UiState<String>?> = _visionState.asStateFlow()

    private val _imageGenState = MutableStateFlow<UiState<GeneratedImageRecord>?>(null)
    val imageGenState: StateFlow<UiState<GeneratedImageRecord>?> = _imageGenState.asStateFlow()

    private val _weatherState = MutableStateFlow<UiState<String>?>(null)
    val weatherState: StateFlow<UiState<String>?> = _weatherState.asStateFlow()

    private val _docGenState = MutableStateFlow<UiState<String>?>(null)
    val docGenState: StateFlow<UiState<String>?> = _docGenState.asStateFlow()

    private val _codeState = MutableStateFlow<UiState<String>?>(null)
    val codeState: StateFlow<UiState<String>?> = _codeState.asStateFlow()

    private val _studyAnswerState = MutableStateFlow<UiState<String>?>(null)
    val studyAnswerState: StateFlow<UiState<String>?> = _studyAnswerState.asStateFlow()

    private val _quizState = MutableStateFlow<UiState<List<QuizQuestion>>?>(null)
    val quizState: StateFlow<UiState<List<QuizQuestion>>?> = _quizState.asStateFlow()

    private val _musicAiState = MutableStateFlow<UiState<String>?>(null)
    val musicAiState: StateFlow<UiState<String>?> = _musicAiState.asStateFlow()

    private val _smartReplyState = MutableStateFlow<UiState<String>?>(null)
    val smartReplyState: StateFlow<UiState<String>?> = _smartReplyState.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        voiceEngine.attachElevenLabsKeyProvider { keyVault.getDecryptedKey(ApiProviderKey.ELEVENLABS) }
        viewModelScope.launch {
            firestoreRepo.ensureProfileExists(currentUserId, defaultDisplayName)
        }
        viewModelScope.launch {
            combine(profileState, settingsState) { profUi, settings ->
                val profile = (profUi as? UiState.Success)?.data
                    ?: UserProfile(userId = currentUserId, displayName = defaultDisplayName)
                MinaLanguage.fromRaw(profile.language) to settings
            }.collect { (lang, settings) ->
                voiceEngine.configure(
                    language = lang,
                    speechRate = settings.speechRate,
                    pitch = settings.voicePitch,
                    volume = settings.voiceVolume,
                    emotion = settings.voiceEmotion,
                    voiceName = settings.selectedVoiceName
                )
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    private fun currentProfile(): UserProfile =
        (profileState.value as? UiState.Success)?.data
            ?: UserProfile(userId = currentUserId, displayName = defaultDisplayName)

    private fun currentMemories(): List<MemoryItem> =
        (memoriesState.value as? UiState.Success)?.data.orEmpty().map { mem ->
            if (mem.encrypted) mem.copy(content = CryptoVault.decrypt(mem.content)) else mem
        }

    private fun currentChats(): List<ChatMessage> =
        (chatsState.value as? UiState.Success)?.data.orEmpty()

    // --- Onboarding, Profile & Settings ---

    fun completeOnboarding(language: MinaLanguage, personality: MinaPersonality) {
        viewModelScope.launch {
            settingsStore.setOnboardingCompleted(true)
            val p = currentProfile()
            firestoreRepo.updateCompanionTone(personality, p.emotionLevel.toInt(), language)
        }
    }

    fun updateSettings(transform: (AppSettingsState) -> AppSettingsState) {
        viewModelScope.launch {
            settingsStore.updateSettings(transform)
        }
    }

    fun updateCompanionTone(
        personality: MinaPersonality,
        emotionLevel: Int,
        language: MinaLanguage
    ) {
        viewModelScope.launch {
            val res = firestoreRepo.updateCompanionTone(personality, emotionLevel, language)
            if (res.isFailure) {
                _toastMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update tone"
            }
        }
    }

    fun updateUserDisplayName(newName: String) {
        val clean = newName.trim().ifBlank { return }
        val p = currentProfile()
        viewModelScope.launch {
            val res = firestoreRepo.updateProfile(
                displayName = clean,
                bio = p.bio,
                privateNote = p.privateNote,
                avatarUri = p.avatarUri,
                youtubeUrl = p.youtubeUrl,
                instagramUrl = p.instagramUrl,
                socialUrl = p.socialUrl,
                musicUrl = p.musicUrl,
                personality = MinaPersonality.fromRaw(p.personality),
                emotionLevel = p.emotionLevel.toInt(),
                language = MinaLanguage.fromRaw(p.language)
            )
            _toastMessage.value = if (res.isSuccess) "Name updated to $clean" else "Could not update name"
        }
    }

    fun deleteAccount(credentialManager: CredentialManager, onComplete: () -> Unit) {
        if (_isDeletingAccount.value) return
        viewModelScope.launch {
            _isDeletingAccount.value = true
            try {
                voiceEngine.stopSpeaking()
                ambientPlayer.stop()
                firestoreRepo.deleteAllUserData(currentUserId)
                val user = Firebase.auth.currentUser
                if (user != null) {
                    user.delete().await()
                }
                runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                _toastMessage.value = "Account and cloud data deleted"
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Account deletion failed", e)
                _toastMessage.value = e.localizedMessage ?: "Re-sign in required before deleting account."
            } finally {
                _isDeletingAccount.value = false
            }
        }
    }

    // --- Connections (Encrypted API Keys) ---

    fun saveApiKey(provider: ApiProviderKey, rawKey: String) {
        keyVault.saveKey(provider, rawKey)
            .onSuccess { _toastMessage.value = "${provider.label} encrypted & saved" }
            .onFailure { _toastMessage.value = it.localizedMessage ?: "Invalid API key" }
    }

    fun clearApiKey(provider: ApiProviderKey) {
        keyVault.clearKey(provider)
        _toastMessage.value = "${provider.label} removed"
    }

    fun testApiKeyConnection(provider: ApiProviderKey, pendingInput: String? = null) {
        viewModelScope.launch {
            val keyToUse = pendingInput?.trim()?.takeIf { it.isNotBlank() }
                ?: keyVault.getDecryptedKey(provider)

            keyVault.updateTestResult(provider, UiState.Loading)
            val result = when (provider) {
                ApiProviderKey.GEMINI -> {
                    if (keyToUse.isNullOrBlank()) {
                        aiRepo.runTestReply(currentProfile(), settingsState.value.assistantName)
                    } else {
                        aiRepo.testCustomGeminiKey(keyToUse)
                    }
                }
                ApiProviderKey.GROQ -> {
                    if (keyToUse.isNullOrBlank()) {
                        Result.failure(IllegalArgumentException("Enter or save your Groq API key first."))
                    } else {
                        aiRepo.testGroqConnection(keyToUse)
                    }
                }
                ApiProviderKey.ELEVENLABS -> {
                    if (keyToUse.isNullOrBlank()) {
                        Result.failure(IllegalArgumentException("Enter or save your ElevenLabs API key first."))
                    } else {
                        aiRepo.testElevenLabsConnection(keyToUse)
                    }
                }
            }
            keyVault.updateTestResult(
                provider,
                result.fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it.localizedMessage ?: "Connection test failed.") }
                )
            )
        }
    }

    // --- AI Chat ---

    fun setAttachedChatBitmap(bitmap: Bitmap?) {
        _attachedChatBitmap.value = bitmap
    }

    fun startNewConversation() {
        _activeConversationStartMillis.value = System.currentTimeMillis()
        _streamingReply.value = null
        _chatError.value = null
        _lastFailedChatRequest.value = null
        _toastMessage.value = "Started fresh conversation"
    }

    fun showFullHistoryInChat() {
        _activeConversationStartMillis.value = null
    }

    fun sendChatMessage(text: String, speakAloud: Boolean = false) {
        val clean = text.trim()
        val attached = _attachedChatBitmap.value
        if (clean.isEmpty() && attached == null) return
        val promptToSend = clean.ifEmpty { "Please inspect this image and explain it clearly." }
        if (_isChatBusy.value) {
            _toastMessage.value = "${settingsState.value.assistantName} is still replying. Please wait a moment."
            return
        }
        _attachedChatBitmap.value = null
        launchChatRequest(
            clean = promptToSend,
            attachedImage = attached,
            speakAloud = speakAloud,
            appendUserBubble = true
        )
    }

    fun retryLastChatMessage() {
        val pending = _lastFailedChatRequest.value ?: return
        if (_isChatBusy.value) return
        val existing = currentChats()
        val alreadyShowsPrompt = existing.lastOrNull()?.let {
            it.role == "USER" && it.text.trim() == pending.first
        } == true
        launchChatRequest(
            clean = pending.first,
            attachedImage = null,
            speakAloud = pending.second,
            appendUserBubble = !alreadyShowsPrompt
        )
    }

    fun regenerateLastResponse() {
        if (_isChatBusy.value) return
        val lastUserPrompt = currentChats().lastOrNull { it.role == "USER" }?.text ?: return
        launchChatRequest(
            clean = lastUserPrompt,
            attachedImage = null,
            speakAloud = false,
            appendUserBubble = false
        )
    }

    fun sendTestReplyInChat() {
        if (_isChatBusy.value) {
            _toastMessage.value = "${settingsState.value.assistantName} is busy with a reply."
            return
        }
        val assistantName = settingsState.value.assistantName
        val testPrompt = "Test reply: Verify $assistantName AI, App Check, and Gemini backend."
        viewModelScope.launch {
            _isChatBusy.value = true
            _chatError.value = null
            _lastFailedChatRequest.value = testPrompt to false
            _streamingReply.value = "Verifying App Check & contacting Gemini..."
            voiceEngine.setThinking()

            val profile = currentProfile()
            val settings = settingsState.value

            appendLocalChatMessage("USER", testPrompt, profile.personality)
            if (settings.rememberConversations) {
                firestoreRepo.addChatMessage("USER", testPrompt, profile.personality)
            }

            try {
                val result = aiRepo.runTestReply(profile, assistantName)
                val reply = result.getOrElse { throw it }
                _streamingReply.value = null
                _lastFailedChatRequest.value = null
                _lastVoiceResponse.value = reply

                appendLocalChatMessage("MINA", reply, profile.personality)
                if (settings.rememberConversations) {
                    firestoreRepo.addChatMessage("MINA", reply, profile.personality)
                }
                voiceEngine.setReady()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Test reply in chat failed", e)
                _streamingReply.value = null
                voiceEngine.setReady()
                _chatError.value = MinaAiErrorClassifier.classify(e)
            } finally {
                _isChatBusy.value = false
            }
        }
    }

    fun runBackendConnectionTest() {
        if (_backendTestState.value is UiState.Loading) return
        viewModelScope.launch {
            _backendTestState.value = UiState.Loading
            val res = aiRepo.runTestReply(currentProfile(), settingsState.value.assistantName)
            _backendTestState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(MinaAiErrorClassifier.classify(it)) }
            )
        }
    }

    private fun launchChatRequest(
        clean: String,
        attachedImage: Bitmap?,
        speakAloud: Boolean,
        appendUserBubble: Boolean
    ) {
        viewModelScope.launch {
            _isChatBusy.value = true
            _chatError.value = null
            _lastFailedChatRequest.value = clean to speakAloud
            _streamingReply.value = ""
            voiceEngine.setThinking()

            val profile = currentProfile()
            val settings = settingsState.value
            val historySnapshot = currentChats()
            val memories = if (settings.rememberConversations) currentMemories() else emptyList()
            val displayLabel = if (attachedImage != null) "[Image attached]\n$clean" else clean

            if (appendUserBubble) {
                appendLocalChatMessage("USER", displayLabel, profile.personality)
                if (settings.rememberConversations) {
                    firestoreRepo.addChatMessage("USER", displayLabel, profile.personality)
                }
            }

            try {
                var finalReply = ""
                val groqKey = keyVault.getDecryptedKey(ApiProviderKey.GROQ)
                val provider = if (attachedImage != null) AiChatProvider.GEMINI else settings.preferredChatProvider

                aiRepo.streamChatReply(
                    userMessage = clean,
                    attachedImage = attachedImage,
                    history = historySnapshot,
                    profile = profile,
                    memories = memories,
                    keepChatting = settings.keepChattingEnabled,
                    assistantName = settings.assistantName,
                    voiceEmotion = settings.voiceEmotion,
                    provider = provider,
                    groqApiKey = groqKey
                ).collect { partial ->
                    finalReply = partial
                    _streamingReply.value = partial
                }

                _streamingReply.value = null
                _lastFailedChatRequest.value = null
                _lastVoiceResponse.value = finalReply

                if (finalReply.isNotBlank()) {
                    appendLocalChatMessage("MINA", finalReply, profile.personality)
                    if (settings.rememberConversations) {
                        firestoreRepo.addChatMessage("MINA", finalReply, profile.personality)
                    }
                }

                if (speakAloud && finalReply.isNotBlank()) {
                    voiceEngine.speak(finalReply)
                } else {
                    voiceEngine.setReady()
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat timed out", e)
                _streamingReply.value = null
                voiceEngine.setReady()
                _chatError.value = "${settingsState.value.assistantName} response timed out after 60 seconds. Tap Retry."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat AI failed", e)
                _streamingReply.value = null
                voiceEngine.setReady()
                _chatError.value = MinaAiErrorClassifier.classify(e)
            } finally {
                _isChatBusy.value = false
            }
        }
    }

    private fun appendLocalChatMessage(role: String, text: String, personality: String) {
        val item = ChatMessage(
            id = "local_${UUID.randomUUID()}",
            userId = currentUserId,
            role = role,
            text = text,
            personality = personality,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _localChatMessages.value = _localChatMessages.value + item
    }

    private fun mergeChatMessages(
        remote: List<ChatMessage>,
        local: List<ChatMessage>
    ): List<ChatMessage> {
        if (local.isEmpty()) return remote
        if (remote.isEmpty()) return local
        val remainingRemoteCounts = mutableMapOf<String, Int>()
        for (msg in remote) {
            val key = "${msg.role}|${msg.text.trim()}"
            remainingRemoteCounts[key] = (remainingRemoteCounts[key] ?: 0) + 1
        }
        val unsyncedLocal = mutableListOf<ChatMessage>()
        for (msg in local) {
            val key = "${msg.role}|${msg.text.trim()}"
            val count = remainingRemoteCounts[key] ?: 0
            if (count > 0) {
                remainingRemoteCounts[key] = count - 1
            } else {
                unsyncedLocal.add(msg)
            }
        }
        return if (unsyncedLocal.isEmpty()) remote else remote + unsyncedLocal
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            _localChatMessages.value = emptyList()
            _activeConversationStartMillis.value = null
            _chatError.value = null
            _lastFailedChatRequest.value = null
            val res = firestoreRepo.clearChats()
            _toastMessage.value = if (res.isSuccess) "Conversation history cleared" else "Cleared local chat"
        }
    }

    // --- Voice Assistant (4-Stage Pipeline) ---

    fun testVoice() {
        val profile = currentProfile()
        voiceEngine.speakTestPhrase(
            assistantName = settingsState.value.assistantName,
            language = MinaLanguage.fromRaw(profile.language)
        )
    }

    fun startVoiceListening(onAutoCommand: ((String) -> Unit)? = null) {
        val lang = MinaLanguage.fromRaw(currentProfile().language)
        voiceEngine.startListening(
            language = lang,
            onResult = { spoken ->
                val settings = settingsState.value
                val assistantLower = settings.assistantName.lowercase()
                val normalized = spoken.lowercase()
                val stripped = when {
                    settings.heyMinaEnabled && normalized.startsWith("hey $assistantLower") ->
                        spoken.substringAfter("hey $assistantLower", "").trim().ifBlank { "Haan bolo!" }
                    settings.heyMinaEnabled && normalized.startsWith("hey mina") ->
                        spoken.substringAfter("hey mina", "").trim().ifBlank { "Haan bolo!" }
                    settings.heyMinaEnabled && normalized.startsWith("hey meena") ->
                        spoken.substringAfter("hey meena", "").trim().ifBlank { "Haan bolo!" }
                    else -> spoken
                }
                if (onAutoCommand != null) {
                    onAutoCommand(stripped)
                } else {
                    sendVoicePrompt(stripped)
                }
            },
            onError = { err ->
                _toastMessage.value = err
            }
        )
    }

    fun sendVoicePrompt(spoken: String) {
        viewModelScope.launch {
            voiceEngine.setUnderstanding()
            delay(180)
            voiceEngine.setResponding()
            val profile = currentProfile()
            val settings = settingsState.value
            val memories = if (settings.rememberConversations) currentMemories() else emptyList()
            try {
                if (settings.rememberConversations) {
                    firestoreRepo.addChatMessage("USER", spoken, profile.personality)
                }
                val reply = aiRepo.generateSingleReply(
                    prompt = spoken,
                    profile = profile,
                    memories = memories,
                    useFastModel = false,
                    assistantName = settings.assistantName,
                    voiceEmotion = settings.voiceEmotion
                )
                _lastVoiceResponse.value = reply
                if (settings.rememberConversations) {
                    firestoreRepo.addChatMessage("MINA", reply, profile.personality)
                }
                voiceEngine.speak(reply)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice AI error", e)
                voiceEngine.setReady()
                val errMsg = MinaAiErrorClassifier.classify(e)
                _lastVoiceResponse.value = "Error: $errMsg"
                _toastMessage.value = errMsg
            }
        }
    }

    // --- AI Image Studio ---

    fun generateAiImage(prompt: String, style: ImageArtStyle, quality: ImageQualityOption) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _imageGenState.value = UiState.Loading
            try {
                val bitmap = aiRepo.generateAiImage(clean, style, quality)
                val record = imageHistoryStore.saveGeneratedImage(
                    bitmap = bitmap,
                    prompt = clean,
                    style = style.label,
                    aspectRatio = quality.aspectRatio
                )
                _imageGenState.value = UiState.Success(record)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI image generation failed", e)
                _imageGenState.value = UiState.Error(MinaAiErrorClassifier.classify(e))
            }
        }
    }

    fun saveImageRecordToGallery(record: GeneratedImageRecord) {
        val bmp = BitmapFactory.decodeFile(record.filePath)
        if (bmp == null) {
            _toastMessage.value = "Image file not found on device."
            return
        }
        PhoneControlHelper.saveBitmapToGallery(getApplication(), bmp, record.prompt)
            .onSuccess { _toastMessage.value = "Saved to Pictures/MeenaAIPro" }
            .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to save image" }
    }

    fun shareImageRecord(record: GeneratedImageRecord) {
        val file = File(record.filePath)
        if (!file.exists()) {
            _toastMessage.value = "Image file missing."
            return
        }
        PhoneControlHelper.shareImageFile(getApplication(), file, "Share ${settingsState.value.assistantName} AI Artwork")
    }

    fun deleteImageRecord(recordId: String) {
        imageHistoryStore.deleteRecord(recordId)
        val current = (_imageGenState.value as? UiState.Success)?.data
        if (current?.id == recordId) {
            _imageGenState.value = null
        }
    }

    // --- Live Weather ---

    fun loadWeather(cityQuery: String) {
        viewModelScope.launch {
            _weatherState.value = UiState.Loading
            val res = PhoneControlHelper.fetchLiveWeather(cityQuery)
            _weatherState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Weather unavailable") }
            )
        }
    }

    // --- Live Vision ---

    fun analyzeVisionBitmap(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            _visionState.value = UiState.Loading
            try {
                val result = aiRepo.analyzeVision(bitmap, prompt, currentProfile())
                _visionState.value = UiState.Success(result)
            } catch (e: TimeoutCancellationException) {
                _visionState.value = UiState.Error("Vision analysis timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Vision failed", e)
                _visionState.value = UiState.Error(MinaAiErrorClassifier.classify(e))
            }
        }
    }

    // --- Creator Hub & Memories ---

    fun saveFullProfile(
        displayName: String,
        bio: String,
        privateNotePlain: String,
        avatarUri: String,
        youtubeUrl: String,
        instagramUrl: String,
        socialUrl: String,
        musicUrl: String,
        personality: MinaPersonality,
        emotionLevel: Int,
        language: MinaLanguage
    ) {
        viewModelScope.launch {
            val encryptedPrivateNote = if (privateNotePlain.isNotBlank()) {
                CryptoVault.encrypt(privateNotePlain)
            } else ""
            val res = firestoreRepo.updateProfile(
                displayName = displayName,
                bio = bio,
                privateNote = encryptedPrivateNote,
                avatarUri = avatarUri,
                youtubeUrl = youtubeUrl,
                instagramUrl = instagramUrl,
                socialUrl = socialUrl,
                musicUrl = musicUrl,
                personality = personality,
                emotionLevel = emotionLevel,
                language = language
            )
            _toastMessage.value = if (res.isSuccess) "Profile saved securely" else (res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile")
        }
    }

    fun addOrUpdateMemory(
        existingId: String?,
        title: String,
        contentPlain: String,
        category: MemoryCategory,
        encrypt: Boolean
    ) {
        viewModelScope.launch {
            val payloadContent = if (encrypt) CryptoVault.encrypt(contentPlain) else contentPlain
            val res = if (existingId.isNullOrBlank()) {
                firestoreRepo.addMemory(title, payloadContent, category, encrypt)
            } else {
                firestoreRepo.updateMemory(existingId, title, payloadContent, category, encrypt)
            }
            _toastMessage.value = if (res.isSuccess) "Memory saved" else (res.exceptionOrNull()?.localizedMessage ?: "Memory save failed")
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteMemory(memoryId)
        }
    }

    fun exportPersonalDataAndShare() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val profile = currentProfile()
                val memories = (memoriesState.value as? UiState.Success)?.data.orEmpty()
                val journals = (journalsState.value as? UiState.Success)?.data.orEmpty()
                val docs = (documentsState.value as? UiState.Success)?.data.orEmpty()
                val places = (placesState.value as? UiState.Success)?.data.orEmpty()
                val file = PdfAndExportHelper.exportUserDataJson(
                    context = context,
                    profile = profile,
                    memories = memories,
                    journals = journals,
                    documents = docs,
                    places = places
                )
                PdfAndExportHelper.shareFile(context, file, "application/json", "Export MEENA AI PRO Personal Data")
            } catch (e: Exception) {
                Log.e(TAG, "Data export failed", e)
                _toastMessage.value = e.localizedMessage ?: "Export failed"
            }
        }
    }

    // --- Documents ---

    fun generateDocumentDraft(docType: DocumentType, details: String) {
        viewModelScope.launch {
            _docGenState.value = UiState.Loading
            try {
                val content = aiRepo.generateDocument(docType, details, currentProfile())
                _docGenState.value = UiState.Success(content)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Document generation failed", e)
                _docGenState.value = UiState.Error(MinaAiErrorClassifier.classify(e))
            }
        }
    }

    fun saveDocument(existingId: String?, title: String, docType: DocumentType, content: String) {
        viewModelScope.launch {
            val res = firestoreRepo.saveDocument(existingId, title, docType, content)
            _toastMessage.value = if (res.isSuccess) "Document saved to cloud" else (res.exceptionOrNull()?.localizedMessage ?: "Save failed")
        }
    }

    fun deleteDocument(docId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteDocument(docId)
        }
    }

    fun exportDocumentToPdf(title: String, docType: DocumentType, content: String): File? {
        return try {
            val context = getApplication<Application>()
            val file = PdfAndExportHelper.generatePdf(context, title, docType.label, content)
            PdfAndExportHelper.shareFile(context, file, "application/pdf", "Share ${docType.label} PDF")
            file
        } catch (e: Exception) {
            Log.e(TAG, "PDF generation failed", e)
            _toastMessage.value = e.localizedMessage ?: "PDF export failed"
            null
        }
    }

    // --- Coding Studio ---

    fun runCodingStudio(mode: String, stack: String, prompt: String) {
        viewModelScope.launch {
            _codeState.value = UiState.Loading
            try {
                val output = aiRepo.generateCodeAssistant(mode, stack, prompt)
                _codeState.value = UiState.Success(output)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Coding Studio failed", e)
                _codeState.value = UiState.Error(MinaAiErrorClassifier.classify(e))
            }
        }
    }

    // --- Study ---

    fun askStudyAssistant(mode: String, prompt: String) {
        viewModelScope.launch {
            _studyAnswerState.value = UiState.Loading
            try {
                val fullPrompt = "Study Mode: $mode. Provide clear, student-friendly structured help for:\n$prompt"
                val answer = aiRepo.generateSingleReply(
                    prompt = fullPrompt,
                    profile = currentProfile(),
                    assistantName = settingsState.value.assistantName
                )
                _studyAnswerState.value = UiState.Success(answer)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Study assistant failed", e)
                _studyAnswerState.value = UiState.Error(MinaAiErrorClassifier.classify(e))
            }
        }
    }

    fun generateQuiz(topic: String) {
        viewModelScope.launch {
            _quizState.value = UiState.Loading
            try {
                val lang = MinaLanguage.fromRaw(currentProfile().language)
                val questions = aiRepo.generateStudyQuiz(topic, lang)
                _quizState.value = UiState.Success(questions)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Quiz generation failed", e)
                _quizState.value = UiState.Error(MinaAiErrorClassifier.classify(e))
            }
        }
    }

    // --- Music ---

    fun getMusicRecommendations(moodOrQuery: String) {
        viewModelScope.launch {
            _musicAiState.value = UiState.Loading
            try {
                val prompt = "Suggest 6 curated songs & artists (Hindi, Bollywood, Indie, and English mix) matching: '$moodOrQuery'. Include brief why-it-fits notes."
                val answer = aiRepo.generateSingleReply(
                    prompt = prompt,
                    profile = currentProfile(),
                    useFastModel = true,
                    assistantName = settingsState.value.assistantName
                )
                _musicAiState.value = UiState.Success(answer)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _musicAiState.value = UiState.Error(MinaAiErrorClassifier.classify(e))
            }
        }
    }

    // --- Journal ---

    fun saveJournalEntry(title: String, contentPlain: String, mood: JournalMood, encrypt: Boolean) {
        viewModelScope.launch {
            val profile = currentProfile()
            val reflection = aiRepo.generateJournalReflection(title, contentPlain, mood.label, profile)
            val storedContent = if (encrypt) CryptoVault.encrypt(contentPlain) else contentPlain
            val res = firestoreRepo.addJournalEntry(title, storedContent, mood, encrypt, reflection)
            _toastMessage.value = if (res.isSuccess) "Journal entry saved" else (res.exceptionOrNull()?.localizedMessage ?: "Failed to save entry")
        }
    }

    fun deleteJournalEntry(entryId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteJournalEntry(entryId)
        }
    }

    // --- Phone Control Smart Reply ---

    fun generateSmartReply(incomingMessage: String) {
        viewModelScope.launch {
            _smartReplyState.value = UiState.Loading
            try {
                val prompt = "Draft 3 short, polite reply options (1 casual Hinglish, 1 friendly, 1 professional) for this incoming message:\n\"$incomingMessage\""
                val reply = aiRepo.generateSingleReply(
                    prompt = prompt,
                    profile = currentProfile(),
                    useFastModel = true,
                    assistantName = settingsState.value.assistantName
                )
                _smartReplyState.value = UiState.Success(reply)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _smartReplyState.value = UiState.Error(MinaAiErrorClassifier.classify(e))
            }
        }
    }

    // --- Location Favorite Places ---

    fun saveFavoritePlace(name: String, address: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            val res = firestoreRepo.addPlace(name, address, lat, lng)
            _toastMessage.value = if (res.isSuccess) "Saved '$name' to favorites" else (res.exceptionOrNull()?.localizedMessage ?: "Failed to save place")
        }
    }

    fun deleteFavoritePlace(placeId: String) {
        viewModelScope.launch {
            firestoreRepo.deletePlace(placeId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
        ambientPlayer.stop()
    }

    companion object {
        private const val TAG = "MinaViewModel"
    }
}
