package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.AppCheckDiagnostics
import com.example.core.PdfAndExportHelper
import com.example.data.model.AppSettingsState
import com.example.data.model.CodeSyntaxStyle
import com.example.data.model.ConnectionKeysState
import com.example.data.model.MeenaCharacter
import com.example.data.model.MinaLanguage
import com.example.data.model.MinaPersonality
import com.example.data.model.MinaThemeStyle
import com.example.data.model.OrbStyle
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AmberGradientButton
import com.example.ui.components.GlassCard
import com.example.ui.components.MinaBackdrop
import com.example.ui.components.MinaTopBar
import com.example.ui.theme.LocalMinaPalette

@Composable
fun SettingsScreen(
    profile: UserProfile,
    settings: AppSettingsState,
    availableVoices: List<String>,
    connectionsState: ConnectionKeysState,
    appCheckDiagnostics: AppCheckDiagnostics,
    backendTestState: UiState<String>?,
    isDeletingAccount: Boolean,
    userEmail: String,
    onUpdateUserName: (String) -> Unit,
    onTestBackendConnection: () -> Unit,
    onUpdateSettings: ((AppSettingsState) -> AppSettingsState) -> Unit,
    onUpdateTone: (MinaPersonality, Int, MinaLanguage) -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalMinaPalette.current

    var yourNameInput by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var assistantNameInput by remember(settings.assistantName) { mutableStateOf(settings.assistantName) }
    var accessKeyInput by remember(settings.accessKey) { mutableStateOf(settings.accessKey) }

    var showDeleteStep1 by remember { mutableStateOf(false) }
    var showDeleteStep2 by remember { mutableStateOf(false) }
    var legalDialogTitle by remember { mutableStateOf<String?>(null) }
    var legalDialogBody by remember { mutableStateOf("") }

    val emotionLabel = when (profile.emotionLevel.toInt().coerceIn(1, 5)) {
        1 -> "1/5 – Calm"
        2 -> "2/5 – Friendly"
        3 -> "3/5 – Normal"
        4 -> "4/5 – Caring"
        else -> "5/5 – Highly Expressive"
    }

    MinaBackdrop(
        particlesEnabled = settings.particlesEnabled,
        glowIntensity = settings.glowIntensity,
        reduceMotion = settings.reduceMotion
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp)
        ) {
            MinaTopBar(
                title = "Settings Control Center",
                subtitle = "${settings.assistantName} AI PRO • Complete Configuration"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. YOU
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader("YOU")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = yourNameInput,
                                onValueChange = { yourNameInput = it },
                                label = { Text("Your Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            AmberGradientButton(
                                text = "Save",
                                onClick = { onUpdateUserName(yourNameInput) },
                                enabled = yourNameInput.trim().isNotEmpty(),
                                height = 48.dp
                            )
                        }

                        SettingsToggleRow(
                            title = "Privacy Controls & Conversation Memory",
                            subtitle = "Save chats & let ${settings.assistantName} personalize replies",
                            checked = settings.rememberConversations,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(rememberConversations = c) } }
                        )

                        SettingsNavRow(
                            icon = Icons.Filled.Person,
                            title = "Personal Details & Privacy Vault",
                            subtitle = "Profile bio, AES-256 notes & data export",
                            onClick = { onNavigate("creator_hub") }
                        )
                    }
                }

                // 2. ACCOUNT
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader("ACCOUNT")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = palette.accentBlue, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Username: ${profile.displayName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userEmail.ifBlank { "Google Account Connected" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (settings.isPremiumUnlocked) Icons.Filled.Verified else Icons.Filled.Key,
                                contentDescription = null,
                                tint = palette.goldHighlight
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = accessKeyInput,
                                onValueChange = { accessKeyInput = it },
                                label = { Text("PRO Access Key") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    onUpdateSettings { it.copy(accessKey = accessKeyInput.trim()) }
                                    onToast("Access key saved")
                                },
                                modifier = Modifier.height(48.dp)
                            ) { Text("Apply") }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Log out")
                            }

                            OutlinedButton(
                                onClick = { showDeleteStep1 = true },
                                enabled = !isDeletingAccount,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("delete_account_button")
                            ) {
                                if (isDeletingAccount) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Delete Account")
                                }
                            }
                        }
                    }
                }

                // 3. YOUR ASSISTANT
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader("YOUR ASSISTANT")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = assistantNameInput,
                                onValueChange = { assistantNameInput = it },
                                label = { Text("Assistant Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            AmberGradientButton(
                                text = "Rename",
                                onClick = {
                                    val clean = assistantNameInput.trim().ifBlank { "MEENA" }
                                    onUpdateSettings { it.copy(assistantName = clean) }
                                    onToast("Assistant renamed to $clean")
                                },
                                height = 48.dp
                            )
                        }

                        val currentPersonality = MinaPersonality.fromRaw(profile.personality)
                        val currentLanguage = MinaLanguage.fromRaw(profile.language)

                        Text("Personality", style = MaterialTheme.typography.bodyLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MinaPersonality.entries.forEach { p ->
                                FilterChip(
                                    selected = currentPersonality == p,
                                    onClick = { onUpdateTone(p, profile.emotionLevel.toInt(), currentLanguage) },
                                    label = { Text(p.displayName) }
                                )
                            }
                        }

                        Text("Emotion Level: $emotionLabel", style = MaterialTheme.typography.bodyLarge)
                        Slider(
                            value = profile.emotionLevel.toFloat().coerceIn(1f, 5f),
                            onValueChange = { v ->
                                onUpdateTone(currentPersonality, v.toInt().coerceIn(1, 5), currentLanguage)
                            },
                            valueRange = 1f..5f,
                            steps = 3
                        )

                        Text("Language", style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MinaLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = currentLanguage == lang,
                                    onClick = { onUpdateTone(currentPersonality, profile.emotionLevel.toInt(), lang) },
                                    label = { Text(lang.displayName) }
                                )
                            }
                        }

                        SettingsNavRow(
                            icon = Icons.Filled.RecordVoiceOver,
                            title = "Voice",
                            subtitle = "${settings.selectedVoiceName} • ${settings.voiceEmotion.label}",
                            onClick = { onNavigate("voice") }
                        )

                        if (availableVoices.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableVoices.take(8).forEach { v ->
                                    FilterChip(
                                        selected = settings.selectedVoiceName == v,
                                        onClick = { onUpdateSettings { it.copy(selectedVoiceName = v) } },
                                        label = { Text(v.take(22)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. AI SETTINGS
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader("AI SETTINGS")

                        val activeKeysCount = listOf(
                            connectionsState.gemini.isConfigured,
                            connectionsState.groq.isConfigured,
                            connectionsState.elevenLabs.isConfigured
                        ).count { it }

                        SettingsNavRow(
                            icon = Icons.Filled.Hub,
                            title = "Connections",
                            subtitle = "$activeKeysCount custom API keys encrypted • Built-in Gemini active",
                            onClick = { onNavigate("connections") }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.Key,
                            title = "Gemini API Key",
                            subtitle = if (connectionsState.gemini.isConfigured) connectionsState.gemini.maskedPreview else "Firebase AI Logic built-in",
                            onClick = { onNavigate("connections") }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.Bolt,
                            title = "Groq API Keys",
                            subtitle = if (connectionsState.groq.isConfigured) "${connectionsState.groq.maskedPreview} • Fast Chat ready" else "Tap to connect Llama 3.3",
                            onClick = { onNavigate("connections") }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.GraphicEq,
                            title = "ElevenLabs API Key",
                            subtitle = if (connectionsState.elevenLabs.isConfigured) "${connectionsState.elevenLabs.maskedPreview} • Neural TTS" else "Optional studio voice",
                            onClick = { onNavigate("connections") }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.RecordVoiceOver,
                            title = "Voice and Language",
                            subtitle = "Speed, pitch, volume & 4-stage pipeline",
                            onClick = { onNavigate("voice") }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.Psychology,
                            title = "Chats and Memory",
                            subtitle = "Manage memories & chat history",
                            onClick = { onNavigate("history") }
                        )

                        SettingsToggleRow(
                            title = "Hey ${settings.assistantName} Wake Word",
                            subtitle = "Strip wake prefix cleanly on voice input",
                            checked = settings.heyMinaEnabled,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(heyMinaEnabled = c) } }
                        )
                        SettingsToggleRow(
                            title = "Keep Chatting Follow-ups",
                            subtitle = "${settings.assistantName} keeps conversations lively",
                            checked = settings.keepChattingEnabled,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(keepChattingEnabled = c) } }
                        )

                        val tokenStatusText = when {
                            appCheckDiagnostics.isTokenVerified -> "Verified"
                            appCheckDiagnostics.lastError != null -> "Attention Needed"
                            else -> "Ready (On-Demand)"
                        }
                        Text(
                            text = "App Check: ${appCheckDiagnostics.providerMode.label} • $tokenStatusText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (appCheckDiagnostics.lastError != null) MaterialTheme.colorScheme.error else palette.accentBlue
                        )
                        appCheckDiagnostics.lastError?.let { err ->
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }

                        OutlinedButton(
                            onClick = onTestBackendConnection,
                            enabled = backendTestState !is UiState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_test_connection_button")
                        ) {
                            if (backendTestState is UiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Testing App Check & Gemini...")
                            } else {
                                Text("Test Gemini & App Check")
                            }
                        }
                        when (backendTestState) {
                            is UiState.Success -> Text("Live Reply: ${backendTestState.data}", style = MaterialTheme.typography.bodyMedium, color = palette.accentBlue)
                            is UiState.Error -> Text(backendTestState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            else -> Unit
                        }
                    }
                }

                // 5. APP SETTINGS
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader("APP SETTINGS")

                        SettingsNavRow(
                            icon = Icons.Filled.Palette,
                            title = "Look and Feel / Theme",
                            subtitle = settings.themeStyle.label,
                            onClick = { onNavigate("themes") }
                        )

                        SettingsToggleRow(
                            title = "Dark Mode",
                            subtitle = "Futuristic obsidian dark vs daylight theme",
                            checked = settings.darkMode,
                            onCheckedChange = { c ->
                                onUpdateSettings {
                                    it.copy(
                                        darkMode = c,
                                        themeStyle = if (!c) MinaThemeStyle.SOLAR_LIGHT else if (it.themeStyle == MinaThemeStyle.SOLAR_LIGHT) MinaThemeStyle.VIOLET_OBSIDIAN else it.themeStyle
                                    )
                                }
                            }
                        )

                        Text("Orb Style", style = MaterialTheme.typography.bodyLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrbStyle.entries.forEach { orb ->
                                FilterChip(
                                    selected = settings.orbStyle == orb,
                                    onClick = { onUpdateSettings { it.copy(orbStyle = orb) } },
                                    label = { Text(orb.displayName) }
                                )
                            }
                        }

                        Text("Home Character", style = MaterialTheme.typography.bodyLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MeenaCharacter.entries.forEach { char ->
                                FilterChip(
                                    selected = settings.selectedCharacter == char,
                                    onClick = { onUpdateSettings { it.copy(selectedCharacter = char) } },
                                    label = { Text(char.displayName) }
                                )
                            }
                        }

                        SettingsNavRow(
                            icon = Icons.Filled.FaceRetouchingNatural,
                            title = "Open Characters & Orbs Gallery",
                            subtitle = "Preview Idle, Listening & Speaking animations",
                            onClick = { onNavigate("characters") }
                        )

                        SettingsToggleRow(
                            title = "Show Character Everywhere",
                            subtitle = "Display companion badge in chat & navigation",
                            checked = settings.showCharacterEverywhere,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(showCharacterEverywhere = c) } }
                        )
                        SettingsToggleRow(
                            title = "Reduce Motion",
                            subtitle = "Minimize orb rotations & background particle drift",
                            checked = settings.reduceMotion,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(reduceMotion = c) } }
                        )

                        Text("Code Syntax Colour", style = MaterialTheme.typography.bodyLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CodeSyntaxStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = settings.codeSyntaxStyle == style,
                                    onClick = { onUpdateSettings { it.copy(codeSyntaxStyle = style) } },
                                    label = { Text(style.label) }
                                )
                            }
                        }
                    }
                }

                // 6. PHONE CONTROL
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader("PHONE CONTROL")

                        SettingsNavRow(
                            icon = Icons.Filled.Bolt,
                            title = "Quick Actions",
                            subtitle = "${settings.pinnedQuickActions.size} instant shortcuts",
                            onClick = { onNavigate("quick_actions") }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.Lock,
                            title = "Permissions",
                            subtitle = "Manage Camera, Mic & Location permissions",
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        )
                        SettingsToggleRow(
                            title = "App Launching & Hardware",
                            subtitle = "Enable launching installed apps, flashlight & alarms",
                            checked = settings.phoneControlEnabled,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(phoneControlEnabled = c) } }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.Call,
                            title = "Calling & Dialer",
                            subtitle = "Safe confirmation-based Dialer launcher",
                            onClick = { onNavigate("quick_actions") }
                        )
                        SettingsToggleRow(
                            title = "Messaging & Reply Drafts",
                            subtitle = "Draft polite replies & read messages aloud",
                            checked = settings.aiAutoReplyDrafts,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(aiAutoReplyDrafts = c) } }
                        )
                        SettingsToggleRow(
                            title = "Notifications & Message Reader",
                            subtitle = "Enable TTS message helper",
                            checked = settings.messageReadingEnabled,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(messageReadingEnabled = c) } }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.Message,
                            title = "Open Phone Control Studio",
                            subtitle = "Flashlight, Volume, Alarms & Reply Drafter",
                            onClick = { onNavigate("phone_control") }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.PhoneAndroid,
                            title = "Notification Listener Settings",
                            subtitle = "Android system notification access",
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        )
                    }
                }

                // 7. ABOUT
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsSectionHeader("ABOUT")

                        SettingsNavRow(
                            icon = Icons.Filled.Policy,
                            title = "Privacy Policy",
                            subtitle = "AES-256 encryption, Google Sign-In & data protection",
                            onClick = {
                                legalDialogTitle = "Privacy Policy"
                                legalDialogBody =
                                    "MEENA AI PRO stores your chats, memories, journals, and documents under your private Google Sign-In Firebase UID. " +
                                    "API keys entered in Connections (Gemini, Groq, ElevenLabs) are encrypted on-device with Android Keystore AES-256-GCM and never uploaded to external third-party servers except the chosen API provider when you send a request. " +
                                    "You can export or permanently delete your account data anytime from Settings -> Account."
                            }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.Info,
                            title = "Terms and Conditions",
                            subtitle = "AI companion transparency & responsible use",
                            onClick = {
                                legalDialogTitle = "Terms & Conditions"
                                legalDialogBody =
                                    "MEENA AI PRO is an artificial intelligence companion built for helpful conversation, creative writing, study help, coding assistance, and device shortcuts. " +
                                    "Always verify critical medical, legal, or financial information independently. Phone calls and SMS messages always open Android's native confirmation UI before sending."
                            }
                        )
                        SettingsNavRow(
                            icon = Icons.Filled.SupportAgent,
                            title = "Support",
                            subtitle = "Share diagnostic details or contact support",
                            onClick = {
                                val diag = "MEENA AI PRO v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                                    "App Check: ${appCheckDiagnostics.providerMode.label}\n" +
                                    "Character: ${settings.selectedCharacter.displayName}\n" +
                                    "Provider: ${settings.preferredChatProvider.displayName}"
                                PdfAndExportHelper.shareText(context, diag, "MEENA AI PRO Support Info")
                            }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("App Version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accentBlue
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
        }
    }

    if (showDeleteStep1) {
        AlertDialog(
            onDismissRequest = { showDeleteStep1 = false },
            title = { Text("Delete Account?") },
            text = { Text("This permanently deletes your profile, chats, memories, journals, saved documents, and favorite places from Firebase.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteStep1 = false
                    showDeleteStep2 = true
                }) { Text("Continue", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStep1 = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteStep2) {
        AlertDialog(
            onDismissRequest = { showDeleteStep2 = false },
            title = { Text("Confirm Permanent Deletion") },
            text = { Text("Are you completely sure? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteStep2 = false
                    onDeleteAccount()
                }) { Text("Delete Forever", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStep2 = false }) { Text("Keep Account") }
            }
        )
    }

    if (legalDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { legalDialogTitle = null },
            title = { Text(legalDialogTitle ?: "") },
            text = { Text(legalDialogBody) },
            confirmButton = {
                TextButton(onClick = { legalDialogTitle = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val palette = LocalMinaPalette.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = palette.accentBlue
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = LocalMinaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.accentBlue)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
