package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.core.SecureApiKeyVault
import com.example.data.model.MeenaCharacter
import com.example.data.model.MinaThemeStyle
import com.example.data.model.OrbStyle
import com.example.data.model.QuickActionId
import com.example.ui.components.MarkdownBlock
import com.example.ui.components.MeenaMarkdownParser
import com.example.ui.screens.evaluateSimpleMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeenaProCoreTest {

    @Test
    fun markdownParser_splitsTextAndCodeBlocksCleanly() {
        val raw = """
            Here is your Kotlin snippet:
            ```kotlin
            fun greet() = println("Namaste")
            ```
            Let me know if you need more!
        """.trimIndent()

        val blocks = MeenaMarkdownParser.splitBlocks(raw)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
        val codeBlock = blocks[1] as MarkdownBlock.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertTrue(codeBlock.code.contains("Namaste"))
        assertTrue(blocks[2] is MarkdownBlock.TextBlock)
    }

    @Test
    fun markdownParser_formatsBoldItalicInlineCodeAndBullets() {
        val annotated = MeenaMarkdownParser.formatInline(
            "- **MEENA** is *fast* with `Kotlin`",
            codeBg = Color.DarkGray,
            codeFg = Color.Cyan
        )
        assertTrue(annotated.text.startsWith("• "))
        assertTrue(annotated.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(annotated.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun apiKeyMasking_hidesSecretMiddleCharacters() {
        assertEquals("••••••••", SecureApiKeyVault.maskKey("12345678"))
        assertEquals("gsk_••••••••wxyz", SecureApiKeyVault.maskKey("gsk_abcdef123456wxyz"))
    }

    @Test
    fun quickCalculator_handlesPrecedenceAndNegatives() {
        assertEquals("14", evaluateSimpleMath("2 + 3 * 4"))
        assertEquals("2891", evaluateSimpleMath("2450 * 0.18 + 2450"))
        assertEquals("5", evaluateSimpleMath("-5 + 10"))
    }

    @Test
    fun themeStylesCharactersOrbsAndQuickActions_preserveCountsAndLegacyAliases() {
        assertEquals(5, MeenaCharacter.entries.size)
        assertEquals(8, OrbStyle.entries.size)
        assertEquals(12, QuickActionId.entries.size)
        assertEquals(MinaThemeStyle.CYBER_NEBULA, MinaThemeStyle.fromRaw("OBSIDIAN_EMBER"))
        assertEquals(MinaThemeStyle.ROYAL_AMETHYST, MinaThemeStyle.fromRaw("ROYAL_BRONZE"))
        assertEquals(MinaThemeStyle.VIOLET_OBSIDIAN, MinaThemeStyle.fromRaw("UNKNOWN"))
    }
}
