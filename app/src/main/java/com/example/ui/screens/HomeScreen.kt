package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettingsState
import com.example.data.model.AssistantStatus
import com.example.data.model.UserProfile
import com.example.ui.components.AssistantStatusPill
import com.example.ui.components.GlassCard
import com.example.ui.components.MeenaCharacterStage
import com.example.ui.components.MinaBackdrop
import com.example.ui.theme.LocalMinaPalette
import kotlin.math.abs
import kotlin.math.sin

private data class HeroFeatureCardData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun HomeScreen(
    profile: UserProfile,
    settings: AppSettingsState,
    assistantStatus: AssistantStatus,
    voiceAmplitude: Float,
    onOpenMenu: () -> Unit,
    onTapToTalk: () -> Unit,
    onStopVoice: () -> Unit,
    onAskPrompt: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val palette = LocalMinaPalette.current
    var askText by remember { mutableStateOf("") }

    val inf = rememberInfiniteTransition(label = "home_mic")
    val micPulse by inf.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    val featureCards = remember {
        listOf(
            HeroFeatureCardData(
                title = "Chat",
                subtitle = "Get instant answers.",
                icon = Icons.Filled.ChatBubble,
                route = "chat"
            ),
            HeroFeatureCardData(
                title = "Create Image",
                subtitle = "Generate AI images.",
                icon = Icons.Filled.Brush,
                route = "image_studio"
            ),
            HeroFeatureCardData(
                title = "Write",
                subtitle = "Notes, code and content.",
                icon = Icons.Filled.EditNote,
                route = "documents"
            )
        )
    }

    MinaBackdrop(
        particlesEnabled = settings.particlesEnabled,
        glowIntensity = settings.glowIntensity,
        reduceMotion = settings.reduceMotion
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 8.dp, bottom = 114.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 680.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar: Menu Left, MEENA AI PRO Logo + Title Center, Settings Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(palette.glassFill)
                            .border(1.dp, palette.glassBorderStart.copy(alpha = 0.55f), CircleShape)
                            .testTag("home_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.cd_menu),
                            tint = palette.accentBlue
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.meena_launcher_icon_1795505211660),
                            contentDescription = stringResource(R.string.cd_mina_logo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, palette.accentBlue, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "MEENA AI PRO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.tagline),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentBlue
                            )
                        }
                    }

                    IconButton(
                        onClick = { onNavigate("settings") },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(palette.glassFill)
                            .border(1.dp, palette.glassBorderStart.copy(alpha = 0.55f), CircleShape)
                            .testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            tint = palette.goldHighlight
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hi ${profile.displayName.ifBlank { "Friend" }} ✨",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AssistantStatusPill(status = assistantStatus)
                }

                Spacer(Modifier.height(12.dp))

                // Large Animated Female AI Character + Live Orb in Center
                MeenaCharacterStage(
                    character = settings.selectedCharacter,
                    orbStyle = settings.orbStyle,
                    status = assistantStatus,
                    amplitude = voiceAmplitude,
                    reduceMotion = settings.reduceMotion,
                    size = 216.dp,
                    showExpressionBadge = true,
                    onClick = onTapToTalk
                )

                Spacer(Modifier.height(14.dp))

                // Live Voice Animation Bar
                Canvas(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    val bars = 24
                    val step = size.width / bars
                    val centerY = size.height / 2f
                    val baseAmp = when (assistantStatus) {
                        AssistantStatus.LISTENING -> 0.35f + voiceAmplitude * 0.65f
                        AssistantStatus.SPEAKING -> 0.55f + voiceAmplitude * 0.35f
                        AssistantStatus.THINKING -> 0.30f
                        AssistantStatus.READY -> 0.14f
                    }
                    for (i in 0 until bars) {
                        val x = step * i + step / 2f
                        val wave = if (settings.reduceMotion) 0.5f else (0.35f + 0.65f * abs(sin(i * 0.6f + voiceAmplitude * 6f)))
                        val h = size.height * 0.85f * baseAmp * wave
                        drawLine(
                            brush = Brush.verticalGradient(listOf(palette.gradientStart, palette.gradientEnd)),
                            start = Offset(x, centerY - h / 2f),
                            end = Offset(x, centerY + h / 2f),
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Circular Glowing Mic Button + "Tap to Talk"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .scale(if (settings.reduceMotion) 1f else micPulse)
                            .shadow(16.dp, CircleShape, ambientColor = palette.gradientStart, spotColor = palette.gradientEnd)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd)))
                            .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable(onClick = onTapToTalk)
                            .testTag("tap_to_talk_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = stringResource(R.string.tap_to_talk),
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    if (assistantStatus == AssistantStatus.SPEAKING || assistantStatus == AssistantStatus.LISTENING) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.22f))
                                .border(1.dp, MaterialTheme.colorScheme.error, CircleShape)
                                .clickable { onStopVoice() }
                                .testTag("home_stop_voice_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop voice",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (assistantStatus == AssistantStatus.LISTENING) "Listening… Tap to manage" else stringResource(R.string.tap_to_talk),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accentBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // "Ask Meena anything…" Inline Text Composer + Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = askText,
                        onValueChange = { askText = it },
                        placeholder = { Text("Ask ${settings.assistantName.lowercase().replaceFirstChar { it.uppercase() }} anything…") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (askText.isNotBlank()) {
                                    val q = askText.trim()
                                    askText = ""
                                    onAskPrompt(q)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_ask_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accentBlue,
                            unfocusedBorderColor = palette.glassBorderStart.copy(alpha = 0.55f),
                            focusedContainerColor = palette.glassFill,
                            unfocusedContainerColor = palette.glassFill
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(10.dp, CircleShape, ambientColor = palette.gradientStart)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd)))
                            .clickable(enabled = askText.isNotBlank()) {
                                val q = askText.trim()
                                askText = ""
                                onAskPrompt(q)
                            }
                            .testTag("home_ask_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Three Premium Hero Feature Cards (Chat, Create Image, Write)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    featureCards.forEach { card ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hero_card_${card.route}"),
                            cornerRadius = 22.dp,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            onClick = { onNavigate(card.route) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = card.icon,
                                        contentDescription = card.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = card.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = card.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Fast Access Chips: Characters, Connections, Quick Actions, Coding, Study, Journal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("Characters", Icons.Filled.FaceRetouchingNatural, "characters"),
                        Triple("Quick Actions", Icons.Filled.Bolt, "quick_actions"),
                        Triple("Connections", Icons.Filled.Hub, "connections"),
                        Triple("Coding Studio", Icons.Filled.AutoAwesome, "coding"),
                        Triple("Phone Control", Icons.Filled.Settings, "phone_control"),
                        Triple("Journal", Icons.Filled.EditNote, "journal")
                    ).forEach { (label, icon, route) ->
                        GlassCard(
                            cornerRadius = 50.dp,
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp),
                            onClick = { onNavigate(route) },
                            modifier = Modifier.testTag("home_chip_$route")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
