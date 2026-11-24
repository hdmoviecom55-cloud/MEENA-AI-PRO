package com.example.ui.navigation

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.AmbientAudioPlayer
import com.example.core.CryptoVault
import com.example.core.VoiceEngine
import com.example.data.model.AppSettingsState
import com.example.data.model.DocumentType
import com.example.data.model.MinaLanguage
import com.example.data.model.MinaPersonality
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.MinaAiRepository
import com.example.data.repository.MinaFirestoreRepository
import com.example.ui.auth.signOut
import com.example.ui.components.BottomNavTab
import com.example.ui.components.MeenaDrawerContent
import com.example.ui.components.MinaFloatingBottomBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CameraVisionScreen
import com.example.ui.screens.CharactersScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CodingScreen
import com.example.ui.screens.ConnectionsScreen
import com.example.ui.screens.CreatorHubScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageGenerationScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.LocationScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PersonalDetailsScreen
import com.example.ui.screens.PhoneControlScreen
import com.example.ui.screens.QuickActionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.MinaAiTheme
import com.example.ui.viewmodel.MinaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

object MinaRoutes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val VOICE = "voice"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val CHARACTERS = "characters"
    const val IMAGE_STUDIO = "image_studio"
    const val CONNECTIONS = "connections"
    const val QUICK_ACTIONS = "quick_actions"
    const val VISION = "vision"
    const val CREATOR_HUB = "creator_hub"
    const val PERSONAL_DETAILS = "personal_details"
    const val PHONE_CONTROL = "phone_control"
    const val THEMES = "themes"
    const val DOCUMENTS = "documents"
    const val CODING = "coding"
    const val STUDY = "study"
    const val MUSIC = "music"
    const val JOURNAL = "journal"
    const val LOCATION = "location"
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun MinaRootApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val settingsStore = remember(context) { SettingsDataStore(context.applicationContext) }
    val appSettings by settingsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettingsState())
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var splashFinished by remember { mutableStateOf(false) }

    MinaAiTheme(
        darkMode = appSettings.darkMode,
        themeStyle = appSettings.themeStyle
    ) {
        if (!splashFinished) {
            SplashScreen(onFinished = { splashFinished = true })
        } else {
            val user = currentUser
            if (user == null) {
                AuthScreen(onAuthSuccess = {})
            } else {
                val uid = user.uid
                val fallbackName = user.displayName?.takeIf { it.isNotBlank() } ?: "Friend"
                val userEmail = user.email.orEmpty()
                androidx.compose.runtime.key(uid) {
                    val minaViewModel: MinaViewModel = viewModel(
                        key = uid,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                                val databaseId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(databaseId)
                                MinaViewModel(
                                    application = app,
                                    currentUserId = uid,
                                    defaultDisplayName = fallbackName,
                                    firestoreRepo = MinaFirestoreRepository(db, auth),
                                    aiRepo = MinaAiRepository(),
                                    settingsStore = SettingsDataStore(app),
                                    voiceEngine = VoiceEngine(app),
                                    ambientPlayer = AmbientAudioPlayer()
                                )
                            }
                        }
                    )
                    MinaAuthenticatedApp(viewModel = minaViewModel, userEmail = userEmail)
                }
            }
        }
    }
}

@Composable
private fun MinaAuthenticatedApp(
    viewModel: MinaViewModel,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val connectionsState by viewModel.connectionsState.collectAsStateWithLifecycle()
    val imageHistory by viewModel.generatedImageHistory.collectAsStateWithLifecycle()
    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val memoriesUi by viewModel.memoriesState.collectAsStateWithLifecycle()
    val chatsUi by viewModel.chatsState.collectAsStateWithLifecycle()
    val allChatsHistoryUi by viewModel.allChatsHistoryState.collectAsStateWithLifecycle()
    val journalsUi by viewModel.journalsState.collectAsStateWithLifecycle()
    val documentsUi by viewModel.documentsState.collectAsStateWithLifecycle()
    val placesUi by viewModel.placesState.collectAsStateWithLifecycle()

    val assistantStatus by viewModel.assistantStatus.collectAsStateWithLifecycle()
    val voicePipelineStage by viewModel.voicePipelineStage.collectAsStateWithLifecycle()
    val voiceAmplitude by viewModel.voiceAmplitude.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val availableVoices by viewModel.voiceEngine.availableVoices.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val isChatBusy by viewModel.isChatBusy.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val canRetryLastChat by viewModel.canRetryLastChat.collectAsStateWithLifecycle()
    val attachedChatBitmap by viewModel.attachedChatBitmap.collectAsStateWithLifecycle()
    val appCheckDiagnostics by viewModel.appCheckDiagnostics.collectAsStateWithLifecycle()
    val backendTestState by viewModel.backendTestState.collectAsStateWithLifecycle()
    val lastVoiceResponse by viewModel.lastVoiceResponse.collectAsStateWithLifecycle()
    val visionState by viewModel.visionState.collectAsStateWithLifecycle()
    val imageGenState by viewModel.imageGenState.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val docGenState by viewModel.docGenState.collectAsStateWithLifecycle()
    val codeState by viewModel.codeState.collectAsStateWithLifecycle()
    val studyAnswerState by viewModel.studyAnswerState.collectAsStateWithLifecycle()
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    val musicAiState by viewModel.musicAiState.collectAsStateWithLifecycle()
    val smartReplyState by viewModel.smartReplyState.collectAsStateWithLifecycle()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsStateWithLifecycle()
    val playingTrackId by viewModel.ambientPlayer.playingTrackId.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val profile = (profileUi as? UiState.Success)?.data ?: UserProfile()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (!settings.onboardingCompleted) {
        OnboardingScreen(
            initialLanguage = MinaLanguage.fromRaw(profile.language),
            initialPersonality = MinaPersonality.fromRaw(profile.personality),
            onComplete = { lang, personality ->
                viewModel.completeOnboarding(lang, personality)
            }
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topTabRoutes = remember { BottomNavTab.entries.map { it.route }.toSet() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            if (route in topTabRoutes) {
                popUpTo(MinaRoutes.HOME) { saveState = true }
                restoreState = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MeenaDrawerContent(
                profile = profile,
                settings = settings,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navigateTo(route)
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = MinaRoutes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(MinaRoutes.HOME) {
                    HomeScreen(
                        profile = profile,
                        settings = settings,
                        assistantStatus = assistantStatus,
                        voiceAmplitude = voiceAmplitude,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onTapToTalk = { navigateTo(MinaRoutes.VOICE) },
                        onStopVoice = {
                            viewModel.voiceEngine.stopSpeaking()
                            viewModel.voiceEngine.stopListening()
                        },
                        onAskPrompt = { prompt ->
                            viewModel.sendChatMessage(prompt, speakAloud = false)
                            navigateTo(MinaRoutes.CHAT)
                        },
                        onNavigate = { navigateTo(it) }
                    )
                }

                composable(MinaRoutes.CHAT) {
                    ChatScreen(
                        profile = profile,
                        settings = settings,
                        chatsState = chatsUi,
                        streamingReply = streamingReply,
                        isBusy = isChatBusy,
                        chatError = chatError,
                        canRetry = canRetryLastChat,
                        attachedBitmap = attachedChatBitmap,
                        hasGroqKey = connectionsState.groq.isConfigured,
                        onAttachBitmap = viewModel::setAttachedChatBitmap,
                        onNewConversation = viewModel::startNewConversation,
                        onRegenerateLast = viewModel::regenerateLastResponse,
                        onSwitchProvider = { provider ->
                            viewModel.updateSettings { it.copy(preferredChatProvider = provider) }
                        },
                        onSendMessage = viewModel::sendChatMessage,
                        onRetryLastMessage = viewModel::retryLastChatMessage,
                        onTestReply = viewModel::sendTestReplyInChat,
                        onVoiceInput = { callback -> viewModel.startVoiceListening(callback) },
                        onSpeakText = { viewModel.voiceEngine.speak(it) },
                        onClearChat = viewModel::clearChatHistory,
                        onUpdateTone = viewModel::updateCompanionTone,
                        onOpenConnections = { navigateTo(MinaRoutes.CONNECTIONS) },
                        onToast = viewModel::showToast
                    )
                }

                composable(MinaRoutes.VOICE) {
                    VoiceAssistantScreen(
                        profile = profile,
                        settings = settings,
                        status = assistantStatus,
                        pipelineStage = voicePipelineStage,
                        amplitude = voiceAmplitude,
                        partialTranscript = partialTranscript,
                        lastResponse = lastVoiceResponse,
                        availableVoices = availableVoices,
                        hasElevenLabsKey = connectionsState.elevenLabs.isConfigured,
                        onStartListening = { viewModel.startVoiceListening() },
                        onStopListening = { viewModel.voiceEngine.stopListening() },
                        onStopSpeaking = { viewModel.voiceEngine.stopSpeaking() },
                        onSendPrompt = viewModel::sendVoicePrompt,
                        onTestVoice = viewModel::testVoice,
                        onUpdateSettings = viewModel::updateSettings,
                        onUpdateLanguage = { lang ->
                            viewModel.updateCompanionTone(
                                MinaPersonality.fromRaw(profile.personality),
                                profile.emotionLevel.toInt(),
                                lang
                            )
                        },
                        onToast = viewModel::showToast
                    )
                }

                composable(MinaRoutes.HISTORY) {
                    HistoryScreen(
                        settings = settings,
                        allChatsState = allChatsHistoryUi,
                        imageHistory = imageHistory,
                        onResumeInChat = {
                            viewModel.showFullHistoryInChat()
                            navigateTo(MinaRoutes.CHAT)
                        },
                        onStartNewChat = {
                            viewModel.startNewConversation()
                            navigateTo(MinaRoutes.CHAT)
                        },
                        onSpeakText = { viewModel.voiceEngine.speak(it) },
                        onClearHistory = viewModel::clearChatHistory,
                        onNavigate = { navigateTo(it) },
                        onToast = viewModel::showToast
                    )
                }

                composable(MinaRoutes.SETTINGS) {
                    SettingsScreen(
                        profile = profile,
                        settings = settings,
                        availableVoices = availableVoices,
                        connectionsState = connectionsState,
                        appCheckDiagnostics = appCheckDiagnostics,
                        backendTestState = backendTestState,
                        isDeletingAccount = isDeletingAccount,
                        userEmail = userEmail,
                        onUpdateUserName = viewModel::updateUserDisplayName,
                        onTestBackendConnection = viewModel::runBackendConnectionTest,
                        onUpdateSettings = viewModel::updateSettings,
                        onUpdateTone = viewModel::updateCompanionTone,
                        onNavigate = { navigateTo(it) },
                        onSignOut = {
                            viewModel.voiceEngine.stopSpeaking()
                            viewModel.ambientPlayer.stop()
                            signOut(credentialManager, scope)
                        },
                        onDeleteAccount = {
                            viewModel.deleteAccount(credentialManager) {}
                        },
                        onToast = viewModel::showToast
                    )
                }

                composable(MinaRoutes.CHARACTERS) {
                    CharactersScreen(
                        settings = settings,
                        onBack = { navController.popBackStack() },
                        onUpdateSettings = viewModel::updateSettings,
                        onToast = viewModel::showToast
                    )
                }

                composable(MinaRoutes.IMAGE_STUDIO) {
                    ImageGenerationScreen(
                        settings = settings,
                        imageGenState = imageGenState,
                        imageHistory = imageHistory,
                        onBack = { navController.popBackStack() },
                        onGenerateImage = viewModel::generateAiImage,
                        onSaveToGallery = viewModel::saveImageRecordToGallery,
                        onShareImage = viewModel::shareImageRecord,
                        onDeleteHistoryItem = viewModel::deleteImageRecord
                    )
                }

                composable(MinaRoutes.CONNECTIONS) {
                    ConnectionsScreen(
                        settings = settings,
                        connectionsState = connectionsState,
                        onBack = { navController.popBackStack() },
                        onSaveKey = viewModel::saveApiKey,
                        onClearKey = viewModel::clearApiKey,
                        onTestConnection = viewModel::testApiKeyConnection,
                        onUpdateSettings = viewModel::updateSettings
                    )
                }

                composable(MinaRoutes.QUICK_ACTIONS) {
                    QuickActionsScreen(
                        settings = settings,
                        weatherState = weatherState,
                        onBack = { navController.popBackStack() },
                        onNavigate = { navigateTo(it) },
                        onSendChatPrompt = { prompt ->
                            viewModel.sendChatMessage(prompt, speakAloud = false)
                        },
                        onFetchWeather = viewModel::loadWeather,
                        onUpdateSettings = viewModel::updateSettings,
                        onToast = viewModel::showToast
                    )
                }

                composable(MinaRoutes.VISION) {
                    CameraVisionScreen(
                        settings = settings,
                        visionState = visionState,
                        onAnalyzeBitmap = viewModel::analyzeVisionBitmap,
                        onSpeakResult = { viewModel.voiceEngine.speak(it) }
                    )
                }

                composable(MinaRoutes.CREATOR_HUB) {
                    CreatorHubScreen(
                        profile = profile,
                        settings = settings,
                        memoriesState = memoriesUi,
                        onBack = { navController.popBackStack() },
                        onOpenPersonalDetails = { navController.navigate(MinaRoutes.PERSONAL_DETAILS) },
                        onSaveAvatarUri = { uri ->
                            viewModel.saveFullProfile(
                                profile.displayName,
                                profile.bio,
                                CryptoVault.decrypt(profile.privateNote),
                                uri,
                                profile.youtubeUrl,
                                profile.instagramUrl,
                                profile.socialUrl,
                                profile.musicUrl,
                                MinaPersonality.fromRaw(profile.personality),
                                profile.emotionLevel.toInt(),
                                MinaLanguage.fromRaw(profile.language)
                            )
                        },
                        onToggleRememberConversations = { enabled ->
                            viewModel.updateSettings { it.copy(rememberConversations = enabled) }
                        },
                        onSaveMemory = viewModel::addOrUpdateMemory,
                        onDeleteMemory = viewModel::deleteMemory,
                        onExportData = viewModel::exportPersonalDataAndShare
                    )
                }

                composable(MinaRoutes.PERSONAL_DETAILS) {
                    PersonalDetailsScreen(
                        profile = profile,
                        settings = settings,
                        onBack = { navController.popBackStack() },
                        onSaveProfile = viewModel::saveFullProfile
                    )
                }

                composable(MinaRoutes.PHONE_CONTROL) {
                    PhoneControlScreen(
                        settings = settings,
                        smartReplyState = smartReplyState,
                        onBack = { navController.popBackStack() },
                        onVoiceCommand = { cb -> viewModel.startVoiceListening(cb) },
                        onGenerateSmartReply = viewModel::generateSmartReply,
                        onSpeakText = { viewModel.voiceEngine.speak(it) },
                        onToast = viewModel::showToast
                    )
                }

                composable(MinaRoutes.THEMES) {
                    ThemesScreen(
                        settings = settings,
                        onBack = { navController.popBackStack() },
                        onUpdateSettings = viewModel::updateSettings
                    )
                }

                composable(MinaRoutes.DOCUMENTS) {
                    DocumentsScreen(
                        settings = settings,
                        documentsState = documentsUi,
                        docGenState = docGenState,
                        onBack = { navController.popBackStack() },
                        onGenerateDraft = viewModel::generateDocumentDraft,
                        onSaveDocument = viewModel::saveDocument,
                        onDeleteDocument = viewModel::deleteDocument,
                        onExportPdf = { title, type, content ->
                            viewModel.exportDocumentToPdf(title, type, content)
                        }
                    )
                }

                composable(MinaRoutes.CODING) {
                    CodingScreen(
                        settings = settings,
                        codeState = codeState,
                        onBack = { navController.popBackStack() },
                        onRunCoding = viewModel::runCodingStudio,
                        onSaveSnippet = { title, content ->
                            viewModel.saveDocument(null, title, DocumentType.CODE, content)
                        },
                        onToast = viewModel::showToast
                    )
                }

                composable(MinaRoutes.STUDY) {
                    StudyScreen(
                        settings = settings,
                        studyAnswerState = studyAnswerState,
                        quizState = quizState,
                        onBack = { navController.popBackStack() },
                        onAskStudy = viewModel::askStudyAssistant,
                        onGenerateQuiz = viewModel::generateQuiz,
                        onSpeakText = { viewModel.voiceEngine.speak(it) }
                    )
                }

                composable(MinaRoutes.MUSIC) {
                    MusicScreen(
                        settings = settings,
                        tracks = viewModel.ambientPlayer.tracks,
                        playingTrackId = playingTrackId,
                        musicAiState = musicAiState,
                        onBack = { navController.popBackStack() },
                        onToggleTrack = viewModel.ambientPlayer::toggleTrack,
                        onRecommendMusic = viewModel::getMusicRecommendations
                    )
                }

                composable(MinaRoutes.JOURNAL) {
                    JournalScreen(
                        settings = settings,
                        journalsState = journalsUi,
                        onBack = { navController.popBackStack() },
                        onSaveJournal = viewModel::saveJournalEntry,
                        onDeleteJournal = viewModel::deleteJournalEntry
                    )
                }

                composable(MinaRoutes.LOCATION) {
                    LocationScreen(
                        settings = settings,
                        placesState = placesUi,
                        onBack = { navController.popBackStack() },
                        onSavePlace = viewModel::saveFavoritePlace,
                        onDeletePlace = viewModel::deleteFavoritePlace,
                        onToast = viewModel::showToast
                    )
                }
            }

            if (currentRoute in topTabRoutes) {
                MinaFloatingBottomBar(
                    currentRoute = currentRoute,
                    selectedCharacter = settings.selectedCharacter,
                    onNavigate = { navigateTo(it) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }
    }
}
