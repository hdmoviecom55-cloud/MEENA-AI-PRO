package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettingsState
import com.example.data.model.AssistantStatus
import com.example.data.model.MeenaCharacter
import com.example.data.model.OrbStyle
import com.example.ui.components.GlassCard
import com.example.ui.components.MeenaAnimatedOrb
import com.example.ui.components.MeenaCharacterStage
import com.example.ui.components.MinaBackdrop
import com.example.ui.components.MinaTopBar
import com.example.ui.theme.LocalMinaPalette

@Composable
fun CharactersScreen(
    settings: AppSettingsState,
    onBack: () -> Unit,
    onUpdateSettings: ((AppSettingsState) -> AppSettingsState) -> Unit,
    onToast: (String) -> Unit
) {
    val palette = LocalMinaPalette.current
    var previewStatus by remember { mutableStateOf(AssistantStatus.READY) }

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
                title = "AI Characters & Orbs",
                subtitle = "Select your companion look & cosmic energy core",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Stage Preview
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MeenaCharacterStage(
                            character = settings.selectedCharacter,
                            orbStyle = settings.orbStyle,
                            status = previewStatus,
                            amplitude = if (previewStatus == AssistantStatus.SPEAKING || previewStatus == AssistantStatus.LISTENING) 0.72f else 0.15f,
                            reduceMotion = settings.reduceMotion,
                            size = 188.dp,
                            showExpressionBadge = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Preview Animation State",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentBlue
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistantStatus.entries.forEach { st ->
                                FilterChip(
                                    selected = previewStatus == st,
                                    onClick = { previewStatus = st },
                                    label = {
                                        Text(
                                            when (st) {
                                                AssistantStatus.READY -> "Idle"
                                                AssistantStatus.LISTENING -> "Listening"
                                                AssistantStatus.THINKING -> "Processing"
                                                AssistantStatus.SPEAKING -> "Speaking"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // 5 AI Characters
                Text(
                    text = "Choose AI Character",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MeenaCharacter.entries.forEach { character ->
                        val selected = settings.selectedCharacter == character
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("character_card_${character.name}"),
                            borderWidth = if (selected) 2.dp else 1.dp,
                            contentPadding = PaddingValues(12.dp),
                            onClick = {
                                onUpdateSettings { it.copy(selectedCharacter = character) }
                                onToast("${character.displayName} selected")
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = character.drawableRes),
                                    contentDescription = character.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(66.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) palette.accentBlue else palette.glassBorderEnd,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = character.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) palette.accentBlue else MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = character.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${character.idleBadge} • ${character.speakingBadge}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.goldHighlight
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = palette.accentBlue
                                    )
                                }
                            }
                        }
                    }
                }

                // 8 Animated Orb Styles
                Text(
                    text = "Animated Orb System (8 Styles)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OrbStyle.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { orb ->
                                val selected = settings.orbStyle == orb
                                GlassCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("orb_card_${orb.name}"),
                                    borderWidth = if (selected) 2.dp else 1.dp,
                                    contentPadding = PaddingValues(12.dp),
                                    onClick = {
                                        onUpdateSettings { it.copy(orbStyle = orb) }
                                        onToast("${orb.displayName} orb active")
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        MeenaAnimatedOrb(
                                            orbStyle = orb,
                                            status = previewStatus,
                                            amplitude = 0.4f,
                                            size = 74.dp,
                                            reduceMotion = settings.reduceMotion
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(palette.gradientStart, palette.gradientEnd)
                                                        )
                                                    )
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = orb.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) palette.accentBlue else MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = orb.subtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
