package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AssistantStatus
import com.example.data.model.MeenaCharacter
import com.example.ui.theme.LocalMinaPalette
import com.example.ui.theme.StatusListeningAmber
import com.example.ui.theme.StatusReadyGreen
import com.example.ui.theme.StatusSpeakingGold
import com.example.ui.theme.StatusThinkingCoral
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class StarParticle(
    val xFrac: Float,
    val yFrac: Float,
    val radiusDp: Float,
    val speedFactor: Float,
    val phase: Float
)

@Composable
fun MinaBackdrop(
    particlesEnabled: Boolean = true,
    glowIntensity: Float = 0.90f,
    showHeroArt: Boolean = false,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalMinaPalette.current
    val particles = remember {
        val rng = Random(42)
        List(28) {
            StarParticle(
                xFrac = rng.nextFloat(),
                yFrac = rng.nextFloat(),
                radiusDp = 1.1f + rng.nextFloat() * 2.1f,
                speedFactor = 0.35f + rng.nextFloat() * 0.75f,
                phase = rng.nextFloat() * 6.28f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "particles")
    val rawDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )
    val drift = if (reduceMotion) 0.15f else rawDrift

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.bgTop, palette.bgBottom, palette.bgTop)
                )
            )
    ) {
        if (particlesEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.gradientStart.copy(alpha = 0.18f * glowIntensity),
                            palette.gradientEnd.copy(alpha = 0.08f * glowIntensity),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.24f),
                        radius = size.width * 0.78f
                    ),
                    radius = size.width * 0.78f,
                    center = Offset(size.width * 0.5f, size.height * 0.24f)
                )

                particles.forEachIndexed { idx, p ->
                    val y = ((p.yFrac - drift * p.speedFactor) % 1f + 1f) % 1f
                    val x = p.xFrac + 0.016f * sin((drift * 2f * PI + p.phase).toFloat())
                    val alpha = (0.16f + 0.38f * sin((drift * 4f * PI + p.phase).toFloat()).let { (it + 1f) / 2f }) * glowIntensity.coerceIn(0.3f, 1f)
                    val dotColor = if (idx % 2 == 0) palette.particleColor else palette.goldHighlight
                    drawCircle(
                        color = dotColor.copy(alpha = alpha),
                        radius = p.radiusDp.dp.toPx(),
                        center = Offset(x * size.width, y * size.height)
                    )
                }
            }
        }

        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalMinaPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        colors = listOf(palette.glassBorderStart, palette.glassBorderEnd)
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(borderWidth, borderBrush), shape)
            .then(clickModifier),
        shape = shape,
        color = palette.glassFill,
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun AmberGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp
) {
    val palette = LocalMinaPalette.current
    val shape = RoundedCornerShape(26.dp)
    val gradient = if (enabled) {
        Brush.horizontalGradient(listOf(palette.gradientStart, palette.gradientEnd))
    } else {
        Brush.horizontalGradient(
            listOf(
                palette.gradientStart.copy(alpha = 0.35f),
                palette.gradientEnd.copy(alpha = 0.35f)
            )
        )
    }
    val textColor = if (palette.isDark) Color(0xFFFFFFFF) else Color(0xFFFFFFFF)

    Box(
        modifier = modifier
            .height(height)
            .shadow(if (enabled) 12.dp else 0.dp, shape, ambientColor = palette.gradientStart, spotColor = palette.gradientEnd)
            .clip(shape)
            .background(gradient)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MinaTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val palette = LocalMinaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = palette.goldHighlight
                )
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun AssistantStatusPill(status: AssistantStatus, modifier: Modifier = Modifier) {
    val dotColor = when (status) {
        AssistantStatus.READY -> StatusReadyGreen
        AssistantStatus.LISTENING -> StatusListeningAmber
        AssistantStatus.THINKING -> StatusThinkingCoral
        AssistantStatus.SPEAKING -> StatusSpeakingGold
    }
    GlassCard(
        modifier = modifier,
        cornerRadius = 50.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

enum class BottomNavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    CHAT("chat", R.string.nav_chat, Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    VOICE("voice", R.string.nav_voice, Icons.Filled.Mic, Icons.Outlined.MicNone),
    HISTORY("history", R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun MinaFloatingBottomBar(
    currentRoute: String?,
    selectedCharacter: MeenaCharacter = MeenaCharacter.ORIGINAL_MEENA,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinaPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = palette.gradientStart),
            cornerRadius = 32.dp,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val isCenterCharacter = tab == BottomNavTab.VOICE
                    if (isCenterCharacter) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(10.dp, CircleShape, ambientColor = palette.gradientStart)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd))
                                )
                                .border(
                                    width = if (selected) 2.5.dp else 1.5.dp,
                                    color = palette.accentBlue,
                                    shape = CircleShape
                                )
                                .clickable { onNavigate(tab.route) }
                                .testTag("nav_${tab.route}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = selectedCharacter.drawableRes),
                                contentDescription = stringResource(tab.labelRes),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                                    .clip(CircleShape)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (selected) palette.gradientStart.copy(alpha = 0.22f) else Color.Transparent
                                )
                                .clickable { onNavigate(tab.route) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("nav_${tab.route}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.labelRes),
                                tint = if (selected) palette.accentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) palette.accentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
