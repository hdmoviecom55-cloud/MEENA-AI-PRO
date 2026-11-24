package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.MinaThemeStyle

@Immutable
data class MinaExtendedPalette(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val glassFill: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val goldHighlight: Color,
    val accentBlue: Color,
    val particleColor: Color,
    val codeBackground: Color
)

val LocalMinaPalette = staticCompositionLocalOf {
    MinaExtendedPalette(
        isDark = true,
        bgTop = ObsidianBlack,
        bgBottom = DeepPurpleVoid,
        glassFill = Color(0xCC160C2B),
        glassBorderStart = Color(0xAA9D4EDD),
        glassBorderEnd = Color(0x6656CCF2),
        gradientStart = NeonViolet,
        gradientEnd = SoftBlue,
        goldHighlight = ElectricViolet,
        accentBlue = SoftBlue,
        particleColor = SoftBlue,
        codeBackground = Color(0xFF0A0614)
    )
}

val MinaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MinaAiTheme(
    darkMode: Boolean = true,
    themeStyle: MinaThemeStyle = MinaThemeStyle.VIOLET_OBSIDIAN,
    content: @Composable () -> Unit
) {
    val effectiveDark = darkMode && themeStyle != MinaThemeStyle.SOLAR_LIGHT

    val extended = when {
        !effectiveDark -> MinaExtendedPalette(
            isDark = false,
            bgTop = PearlBackground,
            bgBottom = PearlSurface,
            glassFill = Color(0xEBFFFFFF),
            glassBorderStart = Color(0x889D4EDD),
            glassBorderEnd = Color(0x6656CCF2),
            gradientStart = NeonViolet,
            gradientEnd = SkyGlowBlue,
            goldHighlight = Color(0xFF6D28D9),
            accentBlue = Color(0xFF0284C7),
            particleColor = NeonViolet,
            codeBackground = Color(0xFF170F2E)
        )
        themeStyle == MinaThemeStyle.CYBER_NEBULA -> MinaExtendedPalette(
            isDark = true,
            bgTop = CyberVoid,
            bgBottom = CyberSurface,
            glassFill = Color(0xCC0E1630),
            glassBorderStart = Color(0xAA38BDF8),
            glassBorderEnd = Color(0x66A855F7),
            gradientStart = CyberPurple,
            gradientEnd = CyberCyan,
            goldHighlight = CyberCyan,
            accentBlue = CyberCyan,
            particleColor = CyberCyan,
            codeBackground = Color(0xFF070B18)
        )
        themeStyle == MinaThemeStyle.ROYAL_AMETHYST -> MinaExtendedPalette(
            isDark = true,
            bgTop = AmethystDeep,
            bgBottom = AmethystSurface,
            glassFill = Color(0xCC210C3A),
            glassBorderStart = Color(0xAAE879F9),
            glassBorderEnd = Color(0x6660A5FA),
            gradientStart = NeonViolet,
            gradientEnd = OrchidGlow,
            goldHighlight = OrchidGlow,
            accentBlue = SkyGlowBlue,
            particleColor = OrchidGlow,
            codeBackground = Color(0xFF0E051A)
        )
        themeStyle == MinaThemeStyle.MIDNIGHT_AMBER -> MinaExtendedPalette(
            isDark = true,
            bgTop = EspressoDeep,
            bgBottom = EspressoSurface,
            glassFill = Color(0xCC26170E),
            glassBorderStart = Color(0xAAFFD54F),
            glassBorderEnd = Color(0x44FF6D00),
            gradientStart = AmberOrange,
            gradientEnd = SunGold,
            goldHighlight = SunGold,
            accentBlue = SunGold,
            particleColor = SunGold,
            codeBackground = Color(0xFF100906)
        )
        else -> MinaExtendedPalette(
            isDark = true,
            bgTop = ObsidianBlack,
            bgBottom = DeepPurpleVoid,
            glassFill = Color(0xCC160C2B),
            glassBorderStart = Color(0xAAB366FF),
            glassBorderEnd = Color(0x6656CCF2),
            gradientStart = NeonViolet,
            gradientEnd = SoftBlue,
            goldHighlight = ElectricViolet,
            accentBlue = SoftBlue,
            particleColor = SoftBlue,
            codeBackground = Color(0xFF090514)
        )
    }

    val colorScheme = if (effectiveDark) {
        if (themeStyle == MinaThemeStyle.MIDNIGHT_AMBER) {
            darkColorScheme(
                primary = SunGold,
                onPrimary = EspressoDeep,
                primaryContainer = Color(0xFF472508),
                onPrimaryContainer = SunGold,
                secondary = AmberOrange,
                onSecondary = CreamText,
                background = extended.bgTop,
                onBackground = CreamText,
                surface = extended.bgBottom,
                onSurface = CreamText,
                surfaceVariant = EspressoCard,
                onSurfaceVariant = MutedBronzeText,
                outline = Color(0x66FFB300),
                error = ErrorCoral,
                onError = CreamText
            )
        } else {
            darkColorScheme(
                primary = extended.goldHighlight,
                onPrimary = ObsidianBlack,
                primaryContainer = DeepPurpleCard,
                onPrimaryContainer = GhostWhiteText,
                secondary = extended.accentBlue,
                onSecondary = ObsidianBlack,
                secondaryContainer = Color(0xFF1E1945),
                onSecondaryContainer = GhostWhiteText,
                tertiary = ElectricViolet,
                background = extended.bgTop,
                onBackground = GhostWhiteText,
                surface = extended.bgBottom,
                onSurface = GhostWhiteText,
                surfaceVariant = DeepPurpleCard,
                onSurfaceVariant = MutedLavenderText,
                outline = Color(0x669D4EDD),
                error = ErrorCoral,
                onError = GhostWhiteText
            )
        }
    } else {
        lightColorScheme(
            primary = NeonViolet,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE9D5FF),
            onPrimaryContainer = PearlText,
            secondary = SkyGlowBlue,
            onSecondary = Color.White,
            secondaryContainer = PearlSurface,
            onSecondaryContainer = PearlText,
            tertiary = Color(0xFF7C3AED),
            background = PearlBackground,
            onBackground = PearlText,
            surface = PearlCard,
            onSurface = PearlText,
            surfaceVariant = PearlSurface,
            onSurfaceVariant = PearlMutedText,
            outline = Color(0x887C3AED),
            error = ErrorCoral,
            onError = Color.White
        )
    }

    CompositionLocalProvider(LocalMinaPalette provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MinaTypography,
            shapes = MinaShapes,
            content = content
        )
    }
}
