package com.example.core

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ApiKeyStatus
import com.example.data.model.ApiProviderKey
import com.example.data.model.ConnectionKeysState
import com.example.data.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecureApiKeyVault(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _connectionsState = MutableStateFlow(readState())
    val connectionsState: StateFlow<ConnectionKeysState> = _connectionsState.asStateFlow()

    fun saveKey(provider: ApiProviderKey, rawKey: String): Result<Unit> {
        val clean = rawKey.trim()
        if (clean.length < 8) {
            return Result.failure(IllegalArgumentException("Please enter a valid ${provider.label}."))
        }
        val encrypted = CryptoVault.encrypt(clean)
        prefs.edit().putString(prefKeyFor(provider), encrypted).apply()
        refreshState(provider = provider, testResult = null)
        return Result.success(Unit)
    }

    fun clearKey(provider: ApiProviderKey) {
        prefs.edit().remove(prefKeyFor(provider)).apply()
        refreshState(provider = provider, testResult = null)
    }

    fun getDecryptedKey(provider: ApiProviderKey): String? {
        val stored = prefs.getString(prefKeyFor(provider), null)?.takeIf { it.isNotBlank() } ?: return null
        val decrypted = CryptoVault.decrypt(stored).trim()
        if (decrypted.isBlank() || decrypted == "[Encrypted on another device]") return null
        return decrypted
    }

    fun updateTestResult(provider: ApiProviderKey, result: UiState<String>?) {
        refreshState(provider = provider, testResult = result)
    }

    private fun refreshState(provider: ApiProviderKey? = null, testResult: UiState<String>? = null) {
        val current = _connectionsState.value
        val fresh = readState()
        _connectionsState.value = ConnectionKeysState(
            gemini = fresh.gemini.copy(
                testResult = if (provider == ApiProviderKey.GEMINI) testResult else current.gemini.testResult
            ),
            groq = fresh.groq.copy(
                testResult = if (provider == ApiProviderKey.GROQ) testResult else current.groq.testResult
            ),
            elevenLabs = fresh.elevenLabs.copy(
                testResult = if (provider == ApiProviderKey.ELEVENLABS) testResult else current.elevenLabs.testResult
            )
        )
    }

    private fun readState(): ConnectionKeysState = ConnectionKeysState(
        gemini = statusFor(ApiProviderKey.GEMINI),
        groq = statusFor(ApiProviderKey.GROQ),
        elevenLabs = statusFor(ApiProviderKey.ELEVENLABS)
    )

    private fun statusFor(provider: ApiProviderKey): ApiKeyStatus {
        val key = getDecryptedKey(provider)
        return if (key.isNullOrBlank()) {
            ApiKeyStatus(isConfigured = false, maskedPreview = "")
        } else {
            ApiKeyStatus(isConfigured = true, maskedPreview = maskKey(key))
        }
    }

    private fun prefKeyFor(provider: ApiProviderKey): String = when (provider) {
        ApiProviderKey.GEMINI -> KEY_GEMINI
        ApiProviderKey.GROQ -> KEY_GROQ
        ApiProviderKey.ELEVENLABS -> KEY_ELEVENLABS
    }

    companion object {
        private const val PREFS_NAME = "meena_secure_api_keys"
        private const val KEY_GEMINI = "enc_gemini_api_key"
        private const val KEY_GROQ = "enc_groq_api_key"
        private const val KEY_ELEVENLABS = "enc_elevenlabs_api_key"

        fun maskKey(raw: String): String {
            val clean = raw.trim()
            if (clean.length <= 8) return "••••••••"
            return "${clean.take(4)}••••••••${clean.takeLast(4)}"
        }
    }
}
