package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AiChatProvider
import com.example.data.model.ApiKeyStatus
import com.example.data.model.ApiProviderKey
import com.example.data.model.AppSettingsState
import com.example.data.model.ConnectionKeysState
import com.example.data.model.UiState
import com.example.ui.components.AmberGradientButton
import com.example.ui.components.GlassCard
import com.example.ui.components.MinaBackdrop
import com.example.ui.components.MinaTopBar
import com.example.ui.theme.LocalMinaPalette
import com.example.ui.theme.StatusReadyGreen

@Composable
fun ConnectionsScreen(
    settings: AppSettingsState,
    connectionsState: ConnectionKeysState,
    onBack: () -> Unit,
    onSaveKey: (ApiProviderKey, String) -> Unit,
    onClearKey: (ApiProviderKey) -> Unit,
    onTestConnection: (ApiProviderKey, String?) -> Unit,
    onUpdateSettings: ((AppSettingsState) -> AppSettingsState) -> Unit
) {
    val palette = LocalMinaPalette.current

    MinaBackdrop(
        particlesEnabled = settings.particlesEnabled,
        glowIntensity = settings.glowIntensity,
        reduceMotion = settings.reduceMotion
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            MinaTopBar(
                title = "Connections & API Keys",
                subtitle = "AES-256 Keystore Encrypted Credentials",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = palette.accentBlue)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Encrypted Local Storage",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.goldHighlight
                            )
                            Text(
                                text = "Keys are encrypted with Android Keystore AES-256-GCM on your device and never uploaded or shown in logs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1. Gemini API Key – AI
                ConnectionProviderCard(
                    provider = ApiProviderKey.GEMINI,
                    icon = Icons.Filled.Hub,
                    status = connectionsState.gemini,
                    builtInActiveLabel = "Built-in Firebase AI Logic Active",
                    onSave = { onSaveKey(ApiProviderKey.GEMINI, it) },
                    onClear = { onClearKey(ApiProviderKey.GEMINI) },
                    onTest = { onTestConnection(ApiProviderKey.GEMINI, it) }
                )

                // 2. Groq API Keys – Fast Chat
                ConnectionProviderCard(
                    provider = ApiProviderKey.GROQ,
                    icon = Icons.Filled.Bolt,
                    status = connectionsState.groq,
                    builtInActiveLabel = null,
                    extraFooter = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Prefer Groq for Fast Text Chat", style = MaterialTheme.typography.bodyLarge)
                                Text("Uses Llama 3.3 70B when Groq key is configured", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.preferredChatProvider == AiChatProvider.GROQ_FAST,
                                enabled = connectionsState.groq.isConfigured,
                                onCheckedChange = { useGroq ->
                                    onUpdateSettings {
                                        it.copy(preferredChatProvider = if (useGroq) AiChatProvider.GROQ_FAST else AiChatProvider.GEMINI)
                                    }
                                }
                            )
                        }
                    },
                    onSave = { onSaveKey(ApiProviderKey.GROQ, it) },
                    onClear = { onClearKey(ApiProviderKey.GROQ) },
                    onTest = { onTestConnection(ApiProviderKey.GROQ, it) }
                )

                // 3. ElevenLabs API Key – Voice
                ConnectionProviderCard(
                    provider = ApiProviderKey.ELEVENLABS,
                    icon = Icons.Filled.GraphicEq,
                    status = connectionsState.elevenLabs,
                    builtInActiveLabel = "Optional • Falls back cleanly to Android Voice",
                    onSave = { onSaveKey(ApiProviderKey.ELEVENLABS, it) },
                    onClear = { onClearKey(ApiProviderKey.ELEVENLABS) },
                    onTest = { onTestConnection(ApiProviderKey.ELEVENLABS, it) }
                )

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ConnectionProviderCard(
    provider: ApiProviderKey,
    icon: ImageVector,
    status: ApiKeyStatus,
    builtInActiveLabel: String?,
    extraFooter: (@Composable () -> Unit)? = null,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onTest: (String?) -> Unit
) {
    val palette = LocalMinaPalette.current
    var keyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    val badgeText = when {
        status.isConfigured -> "Encrypted (${status.maskedPreview})"
        builtInActiveLabel != null -> builtInActiveLabel
        else -> "Not Configured"
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(icon, contentDescription = null, tint = palette.accentBlue, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(provider.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(provider.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (status.isConfigured) StatusReadyGreen.copy(alpha = 0.18f) else palette.glassFill
                        )
                        .border(
                            1.dp,
                            if (status.isConfigured) StatusReadyGreen else palette.glassBorderEnd,
                            RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (status.isConfigured) "Connected" else "Ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.isConfigured) StatusReadyGreen else palette.accentBlue
                    )
                }
            }

            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = if (status.isConfigured) StatusReadyGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text(if (status.isConfigured) "Replace ${provider.label}" else "Enter ${provider.label}") },
                placeholder = { Text(provider.hint) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key"
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_input_${provider.name}")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AmberGradientButton(
                    text = "Save Key",
                    enabled = keyInput.trim().length >= 8,
                    onClick = {
                        val v = keyInput.trim()
                        keyInput = ""
                        onSave(v)
                    },
                    modifier = Modifier.weight(1f).testTag("connection_save_${provider.name}"),
                    height = 44.dp
                )
                OutlinedButton(
                    onClick = { onTest(keyInput.trim().ifBlank { null }) },
                    enabled = status.testResult !is UiState.Loading,
                    modifier = Modifier.weight(1f).height(44.dp).testTag("connection_test_${provider.name}")
                ) {
                    if (status.testResult is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Test Connection")
                    }
                }
                if (status.isConfigured) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.testTag("connection_clear_${provider.name}")
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Clear API key", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            when (val res = status.testResult) {
                is UiState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusReadyGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(res.data, style = MaterialTheme.typography.bodyMedium, color = StatusReadyGreen)
                    }
                }
                is UiState.Error -> {
                    Text(res.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                else -> Unit
            }

            if (extraFooter != null) {
                extraFooter()
            }
        }
    }
}
