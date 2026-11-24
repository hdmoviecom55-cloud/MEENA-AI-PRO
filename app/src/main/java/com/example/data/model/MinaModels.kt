package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MinaPersonality(
    val displayName: String,
    val subtitle: String,
    val promptGuidance: String
) {
    GIRLFRIEND(
        "Romantic Companion",
        "Sweet, affectionate & caring AI",
        "Speak with warm affection, playful sweetness, and genuine emotional attentiveness like a devoted romantic companion, while always staying respectful and transparent that you are an AI companion."
    ),
    BEST_FRIEND(
        "Best Friend",
        "Fun, casual, loyal buddy",
        "Talk like a loyal, witty best friend—relaxed, humorous, supportive, uplifting, and ready to banter naturally."
    ),
    PROFESSIONAL(
        "Professional Assistant",
        "Crisp, organized & executive",
        "Be sharp, structured, reliable, and productivity-focused like an executive chief of staff."
    ),
    CARING(
        "Caring Companion",
        "Gentle, empathetic & supportive",
        "Be deeply empathetic, soothing, patient, and emotionally supportive—like a calm, caring confidant."
    ),
    TEACHER(
        "Teacher",
        "Clear, patient & insightful",
        "Explain concepts step-by-step with relatable examples, patience, and encouraging feedback."
    ),
    COACH(
        "Motivational Coach",
        "Energetic, focused & inspiring",
        "Be high-energy, action-oriented, encouraging, and focused on building momentum and confidence."
    );

    companion object {
        fun fromRaw(raw: String?): MinaPersonality =
            entries.firstOrNull { it.name == raw } ?: CARING
    }
}

enum class MinaLanguage(
    val displayName: String,
    val localeTag: String,
    val promptGuidance: String
) {
    HINGLISH(
        "Hinglish",
        "en-IN",
        "Respond naturally in warm conversational Hinglish (a natural human mix of Hindi written in Roman script and everyday English, e.g., 'Haan bilkul! Chalo saath mein solve karte hain')."
    ),
    HINDI(
        "Hindi (हिन्दी)",
        "hi-IN",
        "Respond naturally in warm, expressive Hindi using Devanagari script with friendly everyday phrasing."
    ),
    ENGLISH(
        "English",
        "en-US",
        "Respond naturally in clear, expressive, human-like warm English."
    );

    companion object {
        fun fromRaw(raw: String?): MinaLanguage =
            entries.firstOrNull { it.name == raw } ?: HINGLISH
    }
}

enum class AssistantStatus(val label: String) {
    READY("Ready"),
    LISTENING("Listening..."),
    THINKING("Thinking..."),
    SPEAKING("Speaking...")
}

enum class VoicePipelineStage(val stepNumber: Int, val title: String, val subtitle: String) {
    IDLE(0, "Ready", "Tap mic to start speaking"),
    LISTEN(1, "Listen", "Capturing your voice"),
    UNDERSTAND(2, "Understand", "Detecting language, tone & intent"),
    RESPOND(3, "Respond", "Crafting expressive companion reply"),
    SPEAK(4, "Speak", "Delivering natural human voice")
}

enum class VoiceEmotionStyle(val label: String, val promptGuidance: String) {
    WARM_COMPANION(
        "Warm & Lovely",
        "Use a warm, affectionate, comforting human tone with subtle smile and kindness."
    ),
    PLAYFUL_FUNNY(
        "Playful & Witty",
        "Add lighthearted humor, natural laughter cues like 'haha' where natural, and playful banter."
    ),
    CALM_SOOTHING(
        "Calm & Soothing",
        "Speak gently, calmly, and thoughtfully so the user feels relaxed and heard."
    ),
    ENERGETIC_CHEERFUL(
        "Cheerful & Uplifting",
        "Sound upbeat, enthusiastic, and sparkling with positive energy."
    ),
    EXPRESSIVE_STORYTELLER(
        "Expressive & Deep",
        "Use vivid emotional storytelling, natural pauses, and rich warmth."
    );

    companion object {
        fun fromRaw(raw: String?): VoiceEmotionStyle =
            entries.firstOrNull { it.name == raw } ?: WARM_COMPANION
    }
}

enum class MeenaCharacter(
    val displayName: String,
    val subtitle: String,
    val idleBadge: String,
    val listeningBadge: String,
    val speakingBadge: String,
    @DrawableRes val drawableRes: Int
) {
    ORIGINAL_MEENA(
        "Original MEENA",
        "Signature futuristic cosmic companion",
        "Serene Cosmic Glow",
        "Listening Intently",
        "Warm Expressive Voice",
        R.drawable.char_original_meena_1795505210735
    ),
    HEADPHONES_GIRL(
        "Headphones Girl",
        "Lo-fi beats, gaming & focus buddy",
        "Vibing in Lo-Fi",
        "Audio Mic Active",
        "Upbeat Studio Chat",
        R.drawable.char_headphones_girl_1795505212345
    ),
    SAREE_GIRL(
        "Saree Girl",
        "Graceful modern Indian elegance",
        "Warm Cultural Grace",
        "Attentive & Caring",
        "Melodic & Poetic",
        R.drawable.char_saree_girl_1795505226254
    ),
    MODERN_GIRL(
        "Modern Girl",
        "Stylish, witty & energetic bestie",
        "Confident Smile",
        "Locked On Your Words",
        "Lively & Playful",
        R.drawable.char_modern_girl_1795505226549
    ),
    FUTURISTIC_GIRL(
        "Futuristic AI Girl",
        "Holographic cyber-neural prodigy",
        "Neural Core Idle",
        "Acoustic Matrix Scan",
        "Harmonic Synth Voice",
        R.drawable.char_futuristic_girl_1795505226709
    );

    companion object {
        fun fromRaw(raw: String?): MeenaCharacter =
            entries.firstOrNull { it.name == raw } ?: ORIGINAL_MEENA
    }
}

enum class OrbStyle(val displayName: String, val subtitle: String) {
    CLASSIC_VIOLET_NEBULA("Classic Violet Nebula", "Swirling deep purple & neon violet stardust"),
    HOLOGRAM("Hologram", "Futuristic cyan & violet wireframe HUD rings"),
    BLACK_HOLE("Black Hole", "Event horizon core with glowing accretion disk"),
    ECLIPSE("Eclipse", "Radiant violet-blue corona around obsidian disk"),
    SATURN("Saturn", "Tilted orbital neon rings & cosmic core"),
    BUBBLE("Bubble", "Iridescent liquid plasma soap-film sphere"),
    TESLA_BALL("Tesla Ball", "Live electric violet & cyan plasma arcs"),
    PLASMA_ENERGY("Plasma Energy", "Multi-layered harmonic energy waves");

    companion object {
        fun fromRaw(raw: String?): OrbStyle =
            entries.firstOrNull { it.name == raw } ?: CLASSIC_VIOLET_NEBULA
    }
}

enum class AiChatProvider(val displayName: String, val shortBadge: String) {
    GEMINI("Google Gemini", "Gemini Pro"),
    GROQ_FAST("Groq Ultra-Fast", "Groq Llama 3.3");

    companion object {
        fun fromRaw(raw: String?): AiChatProvider =
            entries.firstOrNull { it.name == raw } ?: GEMINI
    }
}

enum class ApiProviderKey(val label: String, val subtitle: String, val hint: String) {
    GEMINI("Gemini API Key", "AI & Multimodal Intelligence", "AIza... (Optional override; built-in Firebase AI active)"),
    GROQ("Groq API Key", "Fast Chat Engine (Llama 3.3)", "gsk_..."),
    ELEVENLABS("ElevenLabs API Key", "Ultra-Natural Voice Engine", "sk_...")
}

data class ApiKeyStatus(
    val isConfigured: Boolean = false,
    val maskedPreview: String = "",
    val testResult: UiState<String>? = null
)

data class ConnectionKeysState(
    val gemini: ApiKeyStatus = ApiKeyStatus(),
    val groq: ApiKeyStatus = ApiKeyStatus(),
    val elevenLabs: ApiKeyStatus = ApiKeyStatus()
)

enum class ImageArtStyle(val label: String, val promptModifier: String) {
    CYBERPUNK_NEON("Cyberpunk Neon", "ultra detailed cyberpunk futuristic neon violet and cyan lighting"),
    CINEMATIC_REALISTIC("Cinematic Photo", "cinematic photorealistic 8k lighting, shallow depth of field"),
    ANIME_VIBRANT("Anime Art", "high-detail vibrant modern anime key visual aesthetic"),
    FANTASY_ETHEREAL("Cosmic Fantasy", "ethereal glowing fantasy digital illustration, magical atmosphere"),
    MINIMAL_3D("3D Render", "sleek glossy 3D octane render studio lighting"),
    OIL_PAINTING("Fine Art", "expressive textured fine art digital painting")
}

enum class ImageQualityOption(val label: String, val aspectRatio: String) {
    SQUARE_HD("1:1 Square", "1:1"),
    PORTRAIT_PRO("3:4 Portrait", "3:4"),
    WIDE_CINEMA("16:9 Cinema", "16:9")
}

data class GeneratedImageRecord(
    val id: String,
    val prompt: String,
    val style: String,
    val aspectRatio: String,
    val filePath: String,
    val createdAtMillis: Long
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

enum class QuickActionId(val title: String, val subtitle: String) {
    MAKE_CALL("Make a Call", "Dialer with number pad"),
    SEND_MESSAGE("Send Message", "SMS or WhatsApp draft"),
    OPEN_APPS("Open Apps", "Launch installed apps"),
    SET_ALARM("Set Alarm", "Quick alarm & timers"),
    WEATHER("Weather", "Live local forecast"),
    SEARCH_INTERNET("Search Internet", "Instant web search"),
    CREATE_IMAGE("Create Image", "AI Image Studio"),
    TRANSLATE("Translate", "Hindi / Hinglish / Global"),
    CALCULATOR("Calculator", "Quick math & tip calc"),
    NOTES("Notes", "Quick note & documents"),
    REMINDERS("Reminders", "Alarms & daily tasks"),
    CAMERA("Camera", "Live Vision & OCR");

    companion object {
        fun fromRaw(raw: String?): QuickActionId? =
            entries.firstOrNull { it.name == raw }
    }
}

enum class MemoryCategory(val label: String) {
    PREFERENCE("Preference"),
    CONVERSATION("Conversation"),
    PERSONAL("Personal"),
    GOAL("Goal");

    companion object {
        fun fromRaw(raw: String?): MemoryCategory =
            entries.firstOrNull { it.name == raw } ?: PREFERENCE
    }
}

enum class JournalMood(val label: String, val emoji: String) {
    HAPPY("Happy", "😊"),
    CALM("Calm", "😌"),
    EXCITED("Excited", "✨"),
    TIRED("Tired", "😴"),
    STRESSED("Stressed", "🌧️"),
    GRATEFUL("Grateful", "🙏");

    companion object {
        fun fromRaw(raw: String?): JournalMood =
            entries.firstOrNull { it.name == raw } ?: CALM
    }
}

enum class DocumentType(val label: String) {
    RESUME("Resume"),
    BIODATA("Biodata"),
    LETTER("Letter"),
    CODE("Code"),
    NOTE("Note");

    companion object {
        fun fromRaw(raw: String?): DocumentType =
            entries.firstOrNull { it.name == raw } ?: RESUME
    }
}

enum class MinaThemeStyle(val label: String, val subtitle: String) {
    VIOLET_OBSIDIAN("Meena Obsidian Violet", "Black, deep purple, neon violet & soft blue"),
    CYBER_NEBULA("Cyber Nebula", "Cosmic indigo black & electric cyan-violet"),
    ROYAL_AMETHYST("Royal Amethyst", "Velvet midnight & radiant orchid glow"),
    MIDNIGHT_AMBER("Classic Obsidian Amber", "Deep espresso & warm champagne gold"),
    SOLAR_LIGHT("Pearl Daylight", "Clean frosted pearl & soft violet accents");

    companion object {
        fun fromRaw(raw: String?): MinaThemeStyle = when (raw) {
            "OBSIDIAN_EMBER" -> CYBER_NEBULA
            "ROYAL_BRONZE" -> ROYAL_AMETHYST
            else -> entries.firstOrNull { it.name == raw } ?: VIOLET_OBSIDIAN
        }
    }
}

enum class CodeSyntaxStyle(val label: String) {
    VIOLET_CYBER("Neon Violet Terminal"),
    CYAN_CYBER("Cyber Blue"),
    EMERALD_MATRIX("Emerald Matrix"),
    AMBER_DARK("Amber Terminal"),
    MONOCHROME_INK("Clean Ink");

    companion object {
        fun fromRaw(raw: String?): CodeSyntaxStyle =
            entries.firstOrNull { it.name == raw } ?: VIOLET_CYBER
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Friend",
    val bio: String = "",
    val privateNote: String = "",
    val avatarUri: String = "",
    val youtubeUrl: String = "",
    val instagramUrl: String = "",
    val socialUrl: String = "",
    val musicUrl: String = "",
    val personality: String = MinaPersonality.CARING.name,
    val emotionLevel: Long = 4L,
    val language: String = MinaLanguage.HINGLISH.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MemoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = MemoryCategory.PREFERENCE.name,
    val encrypted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "USER",
    val text: String = "",
    val personality: String = MinaPersonality.CARING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JournalEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val mood: String = JournalMood.CALM.name,
    val encrypted: Boolean = false,
    val aiReflection: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val docType: String = DocumentType.RESUME.name,
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoritePlace(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class AppSettingsState(
    val onboardingCompleted: Boolean = false,
    val assistantName: String = "MEENA",
    val darkMode: Boolean = true,
    val themeStyle: MinaThemeStyle = MinaThemeStyle.VIOLET_OBSIDIAN,
    val codeSyntaxStyle: CodeSyntaxStyle = CodeSyntaxStyle.VIOLET_CYBER,
    val selectedCharacter: MeenaCharacter = MeenaCharacter.ORIGINAL_MEENA,
    val orbStyle: OrbStyle = OrbStyle.CLASSIC_VIOLET_NEBULA,
    val showCharacterEverywhere: Boolean = true,
    val reduceMotion: Boolean = false,
    val particlesEnabled: Boolean = true,
    val glowIntensity: Float = 0.90f,
    val heyMinaEnabled: Boolean = true,
    val autoListenEnabled: Boolean = false,
    val keepChattingEnabled: Boolean = true,
    val rememberConversations: Boolean = true,
    val speechRate: Float = 1.0f,
    val voicePitch: Float = 1.08f,
    val voiceVolume: Float = 0.92f,
    val voiceEmotion: VoiceEmotionStyle = VoiceEmotionStyle.WARM_COMPANION,
    val selectedVoiceName: String = "Meena Expressive Female",
    val preferredChatProvider: AiChatProvider = AiChatProvider.GEMINI,
    val pinnedQuickActions: List<QuickActionId> = QuickActionId.entries,
    val phoneControlEnabled: Boolean = true,
    val screenLockAutomation: Boolean = false,
    val messageReadingEnabled: Boolean = false,
    val aiAutoReplyDrafts: Boolean = true,
    val accessKey: String = "",
    val isPremiumUnlocked: Boolean = true
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
