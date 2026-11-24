package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AiChatProvider
import com.example.data.model.AppSettingsState
import com.example.data.model.CodeSyntaxStyle
import com.example.data.model.MeenaCharacter
import com.example.data.model.MinaThemeStyle
import com.example.data.model.OrbStyle
import com.example.data.model.QuickActionId
import com.example.data.model.VoiceEmotionStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.minaSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mina_ai_settings"
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val CODE_STYLE = stringPreferencesKey("code_style")
        val SELECTED_CHARACTER = stringPreferencesKey("selected_character")
        val ORB_STYLE = stringPreferencesKey("orb_style")
        val SHOW_CHAR_EVERYWHERE = booleanPreferencesKey("show_char_everywhere")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val PARTICLES = booleanPreferencesKey("particles_enabled")
        val GLOW = floatPreferencesKey("glow_intensity")
        val HEY_MINA = booleanPreferencesKey("hey_mina_enabled")
        val AUTO_LISTEN = booleanPreferencesKey("auto_listen_enabled")
        val KEEP_CHATTING = booleanPreferencesKey("keep_chatting_enabled")
        val REMEMBER_CHATS = booleanPreferencesKey("remember_conversations")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val VOICE_EMOTION = stringPreferencesKey("voice_emotion")
        val VOICE_NAME = stringPreferencesKey("selected_voice_name")
        val CHAT_PROVIDER = stringPreferencesKey("preferred_chat_provider")
        val PINNED_QUICK_ACTIONS = stringPreferencesKey("pinned_quick_actions")
        val PHONE_CONTROL = booleanPreferencesKey("phone_control_enabled")
        val SCREEN_LOCK = booleanPreferencesKey("screen_lock_automation")
        val MSG_READING = booleanPreferencesKey("message_reading_enabled")
        val AI_AUTO_REPLY = booleanPreferencesKey("ai_auto_reply_drafts")
        val ACCESS_KEY = stringPreferencesKey("access_key")
    }

    private fun fromPrefs(prefs: Preferences): AppSettingsState {
        val accessKey = prefs[Keys.ACCESS_KEY] ?: ""
        val darkMode = prefs[Keys.DARK_MODE] ?: true
        val themeStyle = MinaThemeStyle.fromRaw(prefs[Keys.THEME_STYLE])
        val pinnedRaw = prefs[Keys.PINNED_QUICK_ACTIONS]
        val pinned = if (pinnedRaw.isNullOrBlank()) {
            QuickActionId.entries
        } else {
            pinnedRaw.split(",").mapNotNull { QuickActionId.fromRaw(it.trim()) }
                .ifEmpty { QuickActionId.entries }
        }
        return AppSettingsState(
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            assistantName = prefs[Keys.ASSISTANT_NAME]?.takeIf { it.isNotBlank() } ?: "MEENA",
            darkMode = darkMode,
            themeStyle = themeStyle,
            codeSyntaxStyle = CodeSyntaxStyle.fromRaw(prefs[Keys.CODE_STYLE]),
            selectedCharacter = MeenaCharacter.fromRaw(prefs[Keys.SELECTED_CHARACTER]),
            orbStyle = OrbStyle.fromRaw(prefs[Keys.ORB_STYLE]),
            showCharacterEverywhere = prefs[Keys.SHOW_CHAR_EVERYWHERE] ?: true,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            particlesEnabled = prefs[Keys.PARTICLES] ?: true,
            glowIntensity = prefs[Keys.GLOW] ?: 0.90f,
            heyMinaEnabled = prefs[Keys.HEY_MINA] ?: true,
            autoListenEnabled = prefs[Keys.AUTO_LISTEN] ?: false,
            keepChattingEnabled = prefs[Keys.KEEP_CHATTING] ?: true,
            rememberConversations = prefs[Keys.REMEMBER_CHATS] ?: true,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.08f,
            voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.92f,
            voiceEmotion = VoiceEmotionStyle.fromRaw(prefs[Keys.VOICE_EMOTION]),
            selectedVoiceName = prefs[Keys.VOICE_NAME] ?: "Meena Expressive Female",
            preferredChatProvider = AiChatProvider.fromRaw(prefs[Keys.CHAT_PROVIDER]),
            pinnedQuickActions = pinned,
            phoneControlEnabled = prefs[Keys.PHONE_CONTROL] ?: true,
            screenLockAutomation = prefs[Keys.SCREEN_LOCK] ?: false,
            messageReadingEnabled = prefs[Keys.MSG_READING] ?: false,
            aiAutoReplyDrafts = prefs[Keys.AI_AUTO_REPLY] ?: true,
            accessKey = accessKey,
            isPremiumUnlocked = accessKey.isBlank() ||
                (accessKey.trim().startsWith("MEENA-", ignoreCase = true) ||
                    accessKey.trim().startsWith("MINA-", ignoreCase = true))
        )
    }

    val settingsFlow: Flow<AppSettingsState> = context.minaSettingsDataStore.data.map(::fromPrefs)

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.minaSettingsDataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun updateSettings(transform: (AppSettingsState) -> AppSettingsState) {
        context.minaSettingsDataStore.edit { prefs ->
            val current = fromPrefs(prefs)
            val updated = transform(current)
            prefs[Keys.ONBOARDING_DONE] = updated.onboardingCompleted
            prefs[Keys.ASSISTANT_NAME] = updated.assistantName.trim().ifBlank { "MEENA" }.take(24)
            prefs[Keys.DARK_MODE] = updated.darkMode
            prefs[Keys.THEME_STYLE] = updated.themeStyle.name
            prefs[Keys.CODE_STYLE] = updated.codeSyntaxStyle.name
            prefs[Keys.SELECTED_CHARACTER] = updated.selectedCharacter.name
            prefs[Keys.ORB_STYLE] = updated.orbStyle.name
            prefs[Keys.SHOW_CHAR_EVERYWHERE] = updated.showCharacterEverywhere
            prefs[Keys.REDUCE_MOTION] = updated.reduceMotion
            prefs[Keys.PARTICLES] = updated.particlesEnabled
            prefs[Keys.GLOW] = updated.glowIntensity
            prefs[Keys.HEY_MINA] = updated.heyMinaEnabled
            prefs[Keys.AUTO_LISTEN] = updated.autoListenEnabled
            prefs[Keys.KEEP_CHATTING] = updated.keepChattingEnabled
            prefs[Keys.REMEMBER_CHATS] = updated.rememberConversations
            prefs[Keys.SPEECH_RATE] = updated.speechRate
            prefs[Keys.VOICE_PITCH] = updated.voicePitch
            prefs[Keys.VOICE_VOLUME] = updated.voiceVolume
            prefs[Keys.VOICE_EMOTION] = updated.voiceEmotion.name
            prefs[Keys.VOICE_NAME] = updated.selectedVoiceName
            prefs[Keys.CHAT_PROVIDER] = updated.preferredChatProvider.name
            prefs[Keys.PINNED_QUICK_ACTIONS] = updated.pinnedQuickActions.joinToString(",") { it.name }
            prefs[Keys.PHONE_CONTROL] = updated.phoneControlEnabled
            prefs[Keys.SCREEN_LOCK] = updated.screenLockAutomation
            prefs[Keys.MSG_READING] = updated.messageReadingEnabled
            prefs[Keys.AI_AUTO_REPLY] = updated.aiAutoReplyDrafts
            prefs[Keys.ACCESS_KEY] = updated.accessKey
        }
    }
}
