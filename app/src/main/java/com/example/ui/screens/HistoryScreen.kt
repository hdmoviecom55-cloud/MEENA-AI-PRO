package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettingsState
import com.example.data.model.ChatMessage
import com.example.data.model.GeneratedImageRecord
import com.example.data.model.UiState
import com.example.ui.components.AmberGradientButton
import com.example.ui.components.GlassCard
import com.example.ui.components.MinaBackdrop
import com.example.ui.components.MinaTopBar
import com.example.ui.theme.LocalMinaPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    settings: AppSettingsState,
    allChatsState: UiState<List<ChatMessage>>,
    imageHistory: List<GeneratedImageRecord>,
    onResumeInChat: () -> Unit,
    onStartNewChat: () -> Unit,
    onSpeakText: (String) -> Unit,
    onClearHistory: () -> Unit,
    onNavigate: (String) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalMinaPalette.current
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    val allMessages = (allChatsState as? UiState.Success)?.data.orEmpty()
    val filteredMessages = remember(allMessages, searchQuery) {
        val q = searchQuery.trim()
        val ordered = allMessages.asReversed()
        if (q.isEmpty()) ordered
        else ordered.filter { it.text.contains(q, ignoreCase = true) || it.role.contains(q, ignoreCase = true) }
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
                title = "History & Vault",
                subtitle = "Conversations, AI Artwork & Memory",
                trailing = {
                    if (allMessages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("history_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear conversation history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search messages, topics, or replies...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = palette.accentBlue) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentBlue,
                        unfocusedBorderColor = palette.glassBorderStart.copy(alpha = 0.5f),
                        focusedContainerColor = palette.glassFill,
                        unfocusedContainerColor = palette.glassFill
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Quick Navigation Shortcuts: AI Images & Memory Vault
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        onClick = { onNavigate("image_studio") }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Brush, contentDescription = null, tint = palette.accentBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("AI Artwork", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text("${imageHistory.size} saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        onClick = { onNavigate("creator_hub") }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Psychology, contentDescription = null, tint = palette.goldHighlight, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Memory Vault", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text("Preferences & facts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AmberGradientButton(
                        text = "Open Full Chat",
                        onClick = onResumeInChat,
                        modifier = Modifier.weight(1f).testTag("history_open_chat_button"),
                        height = 44.dp
                    )
                    OutlinedButton(
                        onClick = onStartNewChat,
                        modifier = Modifier.weight(1f).height(44.dp).testTag("history_new_chat_button")
                    ) {
                        Icon(Icons.Filled.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New Chat")
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (allChatsState) {
                    is UiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = palette.accentBlue)
                        }
                    }
                    is UiState.Error -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(allChatsState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is UiState.Success -> {
                        if (filteredMessages.isEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.History, contentDescription = null, tint = palette.accentBlue, modifier = Modifier.size(34.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isBlank()) "No conversation history yet." else "No messages match \"$searchQuery\".",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Start talking or chatting with ${settings.assistantName} and your conversations will appear here.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = filteredMessages,
                                    key = { it.id.ifBlank { "${it.role}_${it.text.hashCode()}_${it.createdAt?.seconds ?: 0}" } }
                                ) { msg ->
                                    val isUser = msg.role == "USER"
                                    val timeStr = remember(msg.createdAt) {
                                        val date = msg.createdAt?.toDate() ?: Date()
                                        SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()).format(date)
                                    }
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(14.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isUser) "You" else settings.assistantName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUser) palette.accentBlue else palette.goldHighlight
                                                )
                                                Text(
                                                    text = timeStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 6,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!isUser) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Speak",
                                                        tint = palette.accentBlue,
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .clickable { onSpeakText(msg.text) }
                                                    )
                                                    Spacer(Modifier.width(14.dp))
                                                }
                                                Icon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = "Copy",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .size(17.dp)
                                                        .clickable {
                                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            cb.setPrimaryClip(ClipData.newPlainText("MEENA History", msg.text))
                                                            onToast("Copied to clipboard")
                                                        }
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
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Chat History?") },
            text = { Text("This permanently deletes saved messages from your account.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearHistory()
                }) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}
