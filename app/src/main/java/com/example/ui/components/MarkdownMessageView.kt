package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalMinaPalette

sealed interface MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
}

object MeenaMarkdownParser {
    fun splitBlocks(raw: String): List<MarkdownBlock> {
        if (raw.isEmpty()) return emptyList()
        val result = mutableListOf<MarkdownBlock>()
        val fenceRegex = Regex("```([a-zA-Z0-9_+#.-]*)\\s*\\n?([\\s\\S]*?)```")
        var lastIndex = 0
        for (match in fenceRegex.findAll(raw)) {
            if (match.range.first > lastIndex) {
                val before = raw.substring(lastIndex, match.range.first).trim()
                if (before.isNotEmpty()) result.add(MarkdownBlock.TextBlock(before))
            }
            val lang = match.groupValues[1].trim().ifBlank { "code" }
            val code = match.groupValues[2].trimEnd('\n', '\r')
            if (code.isNotEmpty()) {
                result.add(MarkdownBlock.CodeBlock(lang, code))
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < raw.length) {
            val tail = raw.substring(lastIndex).trim()
            if (tail.isNotEmpty()) result.add(MarkdownBlock.TextBlock(tail))
        }
        if (result.isEmpty()) {
            result.add(MarkdownBlock.TextBlock(raw))
        }
        return result
    }

    fun formatInline(text: String, codeBg: Color, codeFg: Color): AnnotatedString = buildAnnotatedString {
        val normalized = text.lines().joinToString("\n") { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("### ") -> "**${trimmed.removePrefix("### ")}**"
                trimmed.startsWith("## ") -> "**${trimmed.removePrefix("## ")}**"
                trimmed.startsWith("# ") -> "**${trimmed.removePrefix("# ")}**"
                trimmed.startsWith("- ") -> "• ${trimmed.removePrefix("- ")}"
                trimmed.startsWith("* ") -> "• ${trimmed.removePrefix("* ")}"
                else -> line
            }
        }
        val tokenRegex = Regex("(\\*\\*.+?\\*\\*|\\*.+?\\*|`.+?`)")
        var idx = 0
        for (m in tokenRegex.findAll(normalized)) {
            if (m.range.first > idx) {
                append(normalized.substring(idx, m.range.first))
            }
            val tok = m.value
            when {
                tok.startsWith("**") && tok.endsWith("**") && tok.length > 4 -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(tok.substring(2, tok.length - 2))
                    pop()
                }
                tok.startsWith("*") && tok.endsWith("*") && tok.length > 2 -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(tok.substring(1, tok.length - 1))
                    pop()
                }
                tok.startsWith("`") && tok.endsWith("`") && tok.length > 2 -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBg,
                            color = codeFg
                        )
                    )
                    append(" ${tok.substring(1, tok.length - 1)} ")
                    pop()
                }
                else -> append(tok)
            }
            idx = m.range.last + 1
        }
        if (idx < normalized.length) {
            append(normalized.substring(idx))
        }
    }
}

@Composable
fun MarkdownMessageView(
    rawText: String,
    textColor: Color,
    isUserBubble: Boolean,
    onCopyCode: (String) -> Unit
) {
    val palette = LocalMinaPalette.current
    val blocks = remember(rawText) { MeenaMarkdownParser.splitBlocks(rawText) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.TextBlock -> {
                    val inlineCodeBg = if (isUserBubble) Color(0x33000000) else palette.codeBackground
                    val inlineCodeFg = if (isUserBubble) Color.White else palette.accentBlue
                    val formatted = remember(block.text, inlineCodeBg, inlineCodeFg) {
                        MeenaMarkdownParser.formatInline(block.text, inlineCodeBg, inlineCodeFg)
                    }
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.codeBackground)
                            .border(1.dp, palette.glassBorderEnd, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.gradientStart.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = block.language.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentBlue
                            )
                            Row(
                                modifier = Modifier
                                    .clickable { onCopyCode(block.code) }
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Copy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accentBlue
                                )
                            }
                        }
                        Text(
                            text = block.code,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFE6E1FF),
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
