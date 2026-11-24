package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantStatus
import com.example.data.model.MeenaCharacter
import com.example.data.model.OrbStyle
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalMinaPalette
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeenaAnimatedOrb(
    orbStyle: OrbStyle = OrbStyle.CLASSIC_VIOLET_NEBULA,
    status: AssistantStatus,
    amplitude: Float,
    size: Dp = 172.dp,
    reduceMotion: Boolean = false,
    onClick: (() -> Unit)? = null,
    centerContent: (@Composable () -> Unit)? = null
) {
    val palette = LocalMinaPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "meena_orb")

    val rawPulse by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.LISTENING -> 580
                    AssistantStatus.SPEAKING -> 460
                    AssistantStatus.THINKING -> 760
                    AssistantStatus.READY -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )
    val rawRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == AssistantStatus.THINKING) 2600 else 7200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val pulse = if (reduceMotion) 1f else rawPulse
    val rotation = if (reduceMotion) 35f else rawRotation
    val dynamicBoost = if (reduceMotion) 1f else 1f + (amplitude * 0.16f)

    Box(
        modifier = Modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxR = (this.size.minDimension / 2f) * 0.94f
            val r = maxR * pulse * dynamicBoost
            val v1 = palette.gradientStart
            val v2 = palette.gradientEnd
            val hi = palette.goldHighlight

            when (orbStyle) {
                OrbStyle.CLASSIC_VIOLET_NEBULA -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(v1.copy(alpha = 0.42f), v2.copy(alpha = 0.18f), Color.Transparent),
                            center = c,
                            radius = r
                        ),
                        radius = r,
                        center = c
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(v1, v2, hi, Color.Transparent, v1)),
                        startAngle = rotation,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = v2.copy(alpha = 0.55f),
                        startAngle = -rotation * 1.2f,
                        sweepAngle = 150f,
                        useCenter = false,
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                OrbStyle.HOLOGRAM -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(SoftBlue.copy(alpha = 0.34f), NeonViolet.copy(alpha = 0.12f), Color.Transparent),
                            center = c,
                            radius = r
                        ),
                        radius = r,
                        center = c
                    )
                    val segments = 8
                    for (i in 0 until segments) {
                        drawArc(
                            color = if (i % 2 == 0) SoftBlue.copy(alpha = 0.78f) else hi.copy(alpha = 0.55f),
                            startAngle = rotation + i * (360f / segments),
                            sweepAngle = 28f,
                            useCenter = false,
                            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Butt)
                        )
                    }
                    drawCircle(
                        color = SoftBlue.copy(alpha = 0.25f),
                        radius = maxR * 0.80f,
                        center = c,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                OrbStyle.BLACK_HOLE -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ObsidianBlack,
                                v1.copy(alpha = 0.65f),
                                v2.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = c,
                            radius = r
                        ),
                        radius = r,
                        center = c
                    )
                    rotate(degrees = rotation * 0.6f, pivot = c) {
                        drawOval(
                            brush = Brush.horizontalGradient(listOf(v2, hi, v1, v2)),
                            topLeft = Offset(c.x - maxR * 0.92f, c.y - maxR * 0.24f),
                            size = Size(maxR * 1.84f, maxR * 0.48f),
                            style = Stroke(width = 3.5.dp.toPx())
                        )
                    }
                }

                OrbStyle.ECLIPSE -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF06040C),
                                Color(0xFF06040C),
                                hi.copy(alpha = 0.85f),
                                v2.copy(alpha = 0.40f),
                                Color.Transparent
                            ),
                            center = c,
                            radius = r
                        ),
                        radius = r,
                        center = c
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(hi, v2, Color.Transparent, hi)),
                        startAngle = rotation,
                        sweepAngle = 310f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                OrbStyle.SATURN -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(v1.copy(alpha = 0.4f), Color.Transparent),
                            center = c,
                            radius = maxR * 0.82f * pulse
                        ),
                        radius = maxR * 0.82f * pulse,
                        center = c
                    )
                    rotate(degrees = -22f + sin(rotation * PI / 180.0).toFloat() * 8f, pivot = c) {
                        drawOval(
                            brush = Brush.linearGradient(listOf(v2, hi, v1)),
                            topLeft = Offset(c.x - maxR * 0.98f, c.y - maxR * 0.28f),
                            size = Size(maxR * 1.96f, maxR * 0.56f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawOval(
                            color = v2.copy(alpha = 0.42f),
                            topLeft = Offset(c.x - maxR * 0.86f, c.y - maxR * 0.20f),
                            size = Size(maxR * 1.72f, maxR * 0.40f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                OrbStyle.BUBBLE -> {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                v1.copy(alpha = 0.35f),
                                SoftBlue.copy(alpha = 0.45f),
                                ElectricViolet.copy(alpha = 0.40f),
                                v2.copy(alpha = 0.35f),
                                v1.copy(alpha = 0.35f)
                            )
                        ),
                        radius = maxR * 0.90f * pulse,
                        center = c
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = maxR * 0.90f * pulse,
                        center = c,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                OrbStyle.TESLA_BALL -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(v1.copy(alpha = 0.42f), v2.copy(alpha = 0.16f), Color.Transparent),
                            center = c,
                            radius = r
                        ),
                        radius = r,
                        center = c
                    )
                    drawTeslaLightningBolts(c, maxR * 0.92f, rotation, hi, v2)
                }

                OrbStyle.PLASMA_ENERGY -> {
                    for (layer in 0..2) {
                        val layerRadius = maxR * (0.76f + layer * 0.09f) * pulse
                        drawArc(
                            brush = Brush.sweepGradient(listOf(v1, v2, hi, Color.Transparent, v1)),
                            startAngle = rotation * (if (layer % 2 == 0) 1f else -1.3f) + layer * 45f,
                            sweepAngle = 210f,
                            useCenter = false,
                            topLeft = Offset(c.x - layerRadius, c.y - layerRadius),
                            size = Size(layerRadius * 2, layerRadius * 2),
                            style = Stroke(width = (3.2f - layer * 0.7f).dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        if (centerContent != null) {
            centerContent()
        }
    }
}

private fun DrawScope.drawTeslaLightningBolts(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    colorA: Color,
    colorB: Color
) {
    val bolts = 6
    for (i in 0 until bolts) {
        val baseAngle = ((rotationDeg + i * (360f / bolts)) * PI / 180.0)
        val midAngle = baseAngle + 0.18 * sin(rotationDeg * 0.08f + i)
        val p1 = Offset(
            center.x + (radius * 0.45f * cos(baseAngle)).toFloat(),
            center.y + (radius * 0.45f * sin(baseAngle)).toFloat()
        )
        val p2 = Offset(
            center.x + (radius * 0.72f * cos(midAngle)).toFloat(),
            center.y + (radius * 0.72f * sin(midAngle)).toFloat()
        )
        val p3 = Offset(
            center.x + (radius * 0.96f * cos(baseAngle)).toFloat(),
            center.y + (radius * 0.96f * sin(baseAngle)).toFloat()
        )
        val path = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
        }
        drawPath(
            path = path,
            color = if (i % 2 == 0) colorA else colorB,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MeenaCharacterStage(
    character: MeenaCharacter,
    orbStyle: OrbStyle,
    status: AssistantStatus,
    amplitude: Float,
    reduceMotion: Boolean = false,
    size: Dp = 200.dp,
    showExpressionBadge: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalMinaPalette.current
    val inf = rememberInfiniteTransition(label = "char_stage")
    val breathe by inf.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.SPEAKING -> 420
                    AssistantStatus.LISTENING -> 680
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "char_breathe"
    )

    val effectiveScale = if (reduceMotion) 1f else breathe + (if (status == AssistantStatus.SPEAKING) amplitude * 0.04f else 0f)
    val badgeText = when (status) {
        AssistantStatus.READY -> character.idleBadge
        AssistantStatus.LISTENING -> character.listeningBadge
        AssistantStatus.THINKING -> "Understanding & Thinking..."
        AssistantStatus.SPEAKING -> character.speakingBadge
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MeenaAnimatedOrb(
            orbStyle = orbStyle,
            status = status,
            amplitude = amplitude,
            size = size,
            reduceMotion = reduceMotion,
            onClick = onClick
        ) {
            val avatarSize = size * 0.74f
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .scale(effectiveScale)
                    .clip(CircleShape)
                    .border(
                        width = if (status == AssistantStatus.LISTENING || status == AssistantStatus.SPEAKING) 3.dp else 2.dp,
                        brush = Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = character.drawableRes),
                    contentDescription = "${character.displayName} AI character",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showExpressionBadge) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(palette.glassFill)
                    .border(1.dp, palette.glassBorderStart.copy(alpha = 0.55f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when (status) {
                                    AssistantStatus.READY -> palette.accentBlue
                                    AssistantStatus.LISTENING -> SoftBlue
                                    AssistantStatus.THINKING -> palette.goldHighlight
                                    AssistantStatus.SPEAKING -> ElectricViolet
                                }
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${character.displayName} • $badgeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
