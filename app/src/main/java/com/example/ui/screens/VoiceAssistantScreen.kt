package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AppSettingsState
import com.example.data.model.AssistantStatus
import com.example.data.model.MinaLanguage
import com.example.data.model.UserProfile
import com.example.data.model.VoiceEmotionStyle
import com.example.data.model.VoicePipelineStage
import com.example.ui.components.AmberGradientButton
import com.example.ui.components.AssistantStatusPill
import com.example.ui.components.GlassCard
import com.example.ui.components.MeenaCharacterStage
import com.example.ui.components.MinaBackdrop
import com.example.ui.components.MinaTopBar
import com.example.ui.theme.LocalMinaPalette
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun VoiceAssistantScreen(
    profile: UserProfile,
    settings: AppSettingsState,
    status: AssistantStatus,
    pipelineStage: VoicePipelineStage,
    amplitude: Float,
    partialTranscript: String,
    lastResponse: String,
    availableVoices: List<String>,
    hasElevenLabsKey: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onTestVoice: () -> Unit,
    onUpdateSettings: ((AppSettingsState) -> AppSettingsState) -> Unit,
    onUpdateLanguage: (MinaLanguage) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalMinaPalette.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartListening() else onToast("Microphone permission is needed to talk to ${settings.assistantName}.")
    }

    fun triggerVoice() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (status == AssistantStatus.LISTENING) onStopListening() else onStartListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Interruptible continuous conversation loop when Auto-Listen is active
    LaunchedEffect(settings.autoListenEnabled, status, lastResponse) {
        if (settings.autoListenEnabled && status == AssistantStatus.READY && lastResponse.isNotBlank()) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                kotlinx.coroutines.delay(900)
                onStartListening()
            }
        }
    }

    MinaBackdrop(
        particlesEnabled = settings.particlesEnabled,
        glowIntensity = settings.glowIntensity,
        reduceMotion = settings.reduceMotion
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MinaTopBar(
                title = "${settings.assistantName} Voice Studio",
                subtitle = if (hasElevenLabsKey) "ElevenLabs Neural Voice Active" else "Expressive Hinglish & Hindi Engine",
                trailing = { AssistantStatusPill(status = status) }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 660.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Character + Reactive Orb Stage
                MeenaCharacterStage(
                    character = settings.selectedCharacter,
                    orbStyle = settings.orbStyle,
                    status = status,
                    amplitude = amplitude,
                    reduceMotion = settings.reduceMotion,
                    size = 196.dp,
                    showExpressionBadge = true,
                    onClick = { triggerVoice() }
                )

                Spacer(Modifier.height(10.dp))

                // 4-Stage Voice Pipeline Indicator: Listen -> Understand -> Respond -> Speak
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    val stages = remember {
                        listOf(
                            VoicePipelineStage.LISTEN,
                            VoicePipelineStage.UNDERSTAND,
                            VoicePipelineStage.RESPOND,
                            VoicePipelineStage.SPEAK
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            stages.forEach { st ->
                                val active = pipelineStage == st
                                val completed = pipelineStage.stepNumber > st.stepNumber
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                active -> palette.gradientStart.copy(alpha = 0.28f)
                                                completed -> palette.accentBlue.copy(alpha = 0.14f)
                                                else -> androidx.compose.ui.graphics.Color.Transparent
                                            }
                                        )
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${st.stepNumber}. ${st.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) palette.accentBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Text(
                            text = pipelineStage.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Live Waveform Visualizer
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    val bars = 30
                    val step = size.width / bars
                    val centerY = size.height / 2f
                    val activeAmp = when (status) {
                        AssistantStatus.LISTENING -> 0.30f + amplitude * 0.70f
                        AssistantStatus.SPEAKING -> 0.60f
                        AssistantStatus.THINKING -> 0.28f
                        AssistantStatus.READY -> 0.10f
                    }
                    for (i in 0 until bars) {
                        val x = step * i + step / 2f
                        val wave = if (settings.reduceMotion) 0.5f else (0.35f + 0.65f * abs(sin(i * 0.65f + amplitude * 6f)))
                        val h = (size.height * 0.86f) * activeAmp * wave
                        drawLine(
                            brush = Brush.verticalGradient(listOf(palette.gradientStart, palette.gradientEnd)),
                            start = Offset(x, centerY - h / 2f),
                            end = Offset(x, centerY + h / 2f),
                            strokeWidth = 3.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Transcript & Live AI Reply
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (partialTranscript.isNotBlank()) {
                            Text(
                                text = "You: \"$partialTranscript\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.accentBlue,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            text = lastResponse.ifBlank {
                                "Bol ke dekho! Say \"Hey ${settings.assistantName.lowercase().replaceFirstChar { it.uppercase() }}, aaj ka din kaise plan karein?\" or tap Talk below."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AmberGradientButton(
                        text = if (status == AssistantStatus.LISTENING) "Stop Listening" else "Talk to ${settings.assistantName}",
                        icon = Icons.Filled.Mic,
                        onClick = { triggerVoice() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_mic_button")
                    )

                    if (status == AssistantStatus.SPEAKING) {
                        Box(
                            modifier = Modifier
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(26.dp))
                                .clickable { onStopSpeaking() }
                                .padding(horizontal = 16.dp)
                                .testTag("voice_stop_speaking_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Stop, contentDescription = "Interrupt", tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(6.dp))
                                Text("Interrupt", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick Voice Starters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Hey ${settings.assistantName}, motivate me!",
                        "Ek funny Hinglish joke sunao",
                        "Aaj mood fresh karne ki tip do",
                        "Teach me 1 smart habit"
                    ).forEach { chip ->
                        GlassCard(
                            cornerRadius = 50.dp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            onClick = { onSendPrompt(chip) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(chip, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Full Voice Settings Control Center
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Voice & Emotion Controls", style = MaterialTheme.typography.titleMedium, color = palette.accentBlue)
                            OutlinedButton(
                                onClick = onTestVoice,
                                modifier = Modifier.testTag("voice_test_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Test Voice")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Wake Word Prefix", style = MaterialTheme.typography.bodyLarge)
                                Text("Automatically strips \"Hey ${settings.assistantName}\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.heyMinaEnabled,
                                onCheckedChange = { checked -> onUpdateSettings { it.copy(heyMinaEnabled = checked) } }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Listen Conversation", style = MaterialTheme.typography.bodyLarge)
                                Text("Automatically listen after ${settings.assistantName} replies", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.autoListenEnabled,
                                onCheckedChange = { checked -> onUpdateSettings { it.copy(autoListenEnabled = checked) } }
                            )
                        }

                        Text("Emotion & Expression", style = MaterialTheme.typography.bodyLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VoiceEmotionStyle.entries.forEach { emo ->
                                FilterChip(
                                    selected = settings.voiceEmotion == emo,
                                    onClick = { onUpdateSettings { it.copy(voiceEmotion = emo) } },
                                    label = { Text(emo.label) }
                                )
                            }
                        }

                        Text("Voice Language", style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val currentLang = MinaLanguage.fromRaw(profile.language)
                            MinaLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = currentLang == lang,
                                    onClick = { onUpdateLanguage(lang) },
                                    label = { Text(lang.displayName) }
                                )
                            }
                        }

                        Text(
                            text = "Voice Volume: ${(settings.voiceVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Slider(
                            value = settings.voiceVolume,
                            onValueChange = { vol -> onUpdateSettings { it.copy(voiceVolume = vol) } },
                            valueRange = 0.1f..1.0f
                        )

                        Text(
                            text = "Speaking Speed: ${String.format(Locale.US, "%.2fx", settings.speechRate)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Slider(
                            value = settings.speechRate,
                            onValueChange = { rate -> onUpdateSettings { it.copy(speechRate = rate) } },
                            valueRange = 0.6f..1.6f
                        )

                        Text(
                            text = "Voice Pitch: ${String.format(Locale.US, "%.2f", settings.voicePitch)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Slider(
                            value = settings.voicePitch,
                            onValueChange = { pitch -> onUpdateSettings { it.copy(voicePitch = pitch) } },
                            valueRange = 0.75f..1.35f
                        )

                        if (availableVoices.isNotEmpty()) {
                            Text("Voice Selection", style = MaterialTheme.typography.bodyLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableVoices.forEach { voiceName ->
                                    FilterChip(
                                        selected = settings.selectedVoiceName == voiceName,
                                        onClick = { onUpdateSettings { it.copy(selectedVoiceName = voiceName) } },
                                        label = { Text(voiceName.take(24)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
