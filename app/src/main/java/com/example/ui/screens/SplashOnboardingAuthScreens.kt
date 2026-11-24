package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.AssistantStatus
import com.example.data.model.MeenaCharacter
import com.example.data.model.MinaLanguage
import com.example.data.model.MinaPersonality
import com.example.data.model.OrbStyle
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.AmberGradientButton
import com.example.ui.components.GlassCard
import com.example.ui.components.MeenaAnimatedOrb
import com.example.ui.components.MeenaCharacterStage
import com.example.ui.components.MinaBackdrop
import com.example.ui.theme.LocalMinaPalette
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val palette = LocalMinaPalette.current
    val scale = remember { Animatable(0.78f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(650))
        alpha.animateTo(1f, tween(450))
        delay(850)
        onFinished()
    }

    MinaBackdrop(showHeroArt = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                MeenaAnimatedOrb(
                    orbStyle = OrbStyle.CLASSIC_VIOLET_NEBULA,
                    status = AssistantStatus.READY,
                    amplitude = 0.25f,
                    size = 164.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.meena_launcher_icon_1795505211660),
                        contentDescription = stringResource(R.string.cd_mina_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .border(2.dp, palette.accentBlue, CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "MEENA AI PRO",
                style = MaterialTheme.typography.displayLarge,
                color = palette.goldHighlight,
                modifier = Modifier.alpha(alpha.value)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.titleMedium,
                color = palette.accentBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val palette = LocalMinaPalette.current

    var isLoading by remember { mutableStateOf(false) }
    var autoChecking by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                autoChecking = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                autoChecking = false
            },
            scope = scope
        )
    }

    MinaBackdrop(showHeroArt = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.widthIn(max = 500.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MeenaCharacterStage(
                        character = MeenaCharacter.ORIGINAL_MEENA,
                        orbStyle = OrbStyle.CLASSIC_VIOLET_NEBULA,
                        status = AssistantStatus.READY,
                        amplitude = 0.2f,
                        size = 176.dp,
                        showExpressionBadge = false
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "MEENA AI PRO",
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.goldHighlight,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Namaste! Main MEENA hoon — aapki personal Hindi, Hinglish & English AI companion.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(22.dp))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AuthFeatureRow(
                                icon = Icons.Filled.RecordVoiceOver,
                                title = "Human-Like Hindi & Hinglish Voice",
                                subtitle = "4-stage voice companion, 5 characters & 8 animated orbs."
                            )
                            AuthFeatureRow(
                                icon = Icons.Filled.AutoAwesome,
                                title = "Gemini, Groq & AI Image Studio",
                                subtitle = "Fast chat, image generation, coding, study, and journal."
                            )
                            AuthFeatureRow(
                                icon = Icons.Filled.Shield,
                                title = "AES-256 Encrypted & Private",
                                subtitle = "Google Sign-In syncs your memories & vault securely."
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    if (errorMessage != null) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (autoChecking || isLoading) {
                        CircularProgressIndicator(
                            color = palette.accentBlue,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (autoChecking) "Checking session..." else "Signing in with Google...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        AmberGradientButton(
                            text = stringResource(R.string.sign_in_google),
                            icon = Icons.Filled.Lock,
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_sign_in_button")
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.ai_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val palette = LocalMinaPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.gradientStart.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accentBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    initialLanguage: MinaLanguage,
    initialPersonality: MinaPersonality,
    onComplete: (MinaLanguage, MinaPersonality) -> Unit
) {
    val palette = LocalMinaPalette.current
    var step by remember { mutableIntStateOf(0) }
    var selectedLang by remember { mutableStateOf(initialLanguage) }
    var selectedPersonality by remember { mutableStateOf(initialPersonality) }

    MinaBackdrop(showHeroArt = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEENA AI PRO",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.goldHighlight
                )
                TextButton(
                    onClick = { onComplete(selectedLang, selectedPersonality) },
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = step,
                label = "onboarding_step",
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 540.dp)
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (currentStep) {
                        0 -> {
                            MeenaCharacterStage(
                                character = MeenaCharacter.ORIGINAL_MEENA,
                                orbStyle = OrbStyle.CLASSIC_VIOLET_NEBULA,
                                status = AssistantStatus.READY,
                                amplitude = 0.25f,
                                size = 210.dp,
                                showExpressionBadge = true
                            )
                            Spacer(Modifier.height(22.dp))
                            Text(
                                text = "Your Personal AI Companion",
                                style = MaterialTheme.typography.headlineLarge,
                                color = palette.goldHighlight,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Baat karo dil se — Hinglish, Hindi ya English mein. MEENA aapki voice assistant, image creator, coding studio aur daily journal buddy hai.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        1 -> {
                            Text(
                                text = "Choose Your Language",
                                style = MaterialTheme.typography.headlineLarge,
                                color = palette.goldHighlight,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Select how MEENA should talk and listen by default.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                MinaLanguage.entries.forEach { lang ->
                                    val isSelected = selectedLang == lang
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        borderWidth = if (isSelected) 2.dp else 1.dp,
                                        onClick = { selectedLang = lang }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = lang.displayName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = if (isSelected) palette.accentBlue else MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = when (lang) {
                                                        MinaLanguage.HINGLISH -> "Natural mix of Hindi + English"
                                                        MinaLanguage.HINDI -> "Shuddh & friendly Hindi"
                                                        MinaLanguage.ENGLISH -> "Global warm English"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSelected) {
                                                Text(
                                                    text = "SELECTED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.accentBlue
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            Text(
                                text = "Pick MEENA's Personality",
                                style = MaterialTheme.typography.headlineLarge,
                                color = palette.goldHighlight,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "You can customize emotion level (1/5 – 5/5) & characters anytime.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MinaPersonality.entries.forEach { p ->
                                    val isSelected = selectedPersonality == p
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        borderWidth = if (isSelected) 2.dp else 1.dp,
                                        contentPadding = PaddingValues(14.dp),
                                        onClick = { selectedPersonality = p }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = p.displayName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = if (isSelected) palette.accentBlue else MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = p.subtitle,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSelected) {
                                                Text(
                                                    text = "ACTIVE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.accentBlue
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

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { idx ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (step == idx) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (step == idx) palette.accentBlue
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f)
                    ) {
                        Text("Back")
                    }
                }
                AmberGradientButton(
                    text = if (step < 2) "Continue" else "Launch MEENA AI PRO",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        if (step < 2) step++ else onComplete(selectedLang, selectedPersonality)
                    },
                    modifier = Modifier
                        .weight(1.6f)
                        .testTag("onboarding_next_button")
                )
            }
        }
    }
}
