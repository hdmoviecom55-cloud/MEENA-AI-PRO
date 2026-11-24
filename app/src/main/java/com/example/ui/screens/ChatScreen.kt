package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.PdfAndExportHelper
import com.example.data.model.AiChatProvider
import com.example.data.model.AppSettingsState
import com.example.data.model.ChatMessage
import com.example.data.model.MinaLanguage
import com.example.data.model.MinaPersonality
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.MinaBackdrop
import com.example.ui.components.MinaTopBar
import com.example.ui.theme.LocalMinaPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    profile: UserProfile,
    settings: AppSettingsState,
    chatsState: UiState<List<ChatMessage>>,
    streamingReply: String?,
    isBusy: Boolean,
    chatError: String?,
    canRetry: Boolean,
    attachedBitmap: Bitmap?,
    hasGroqKey: Boolean,
    onAttachBitmap: (Bitmap?) -> Unit,
    onNewConversation: () -> Unit,
    onRegenerateLast: () -> Unit,
    onSwitchProvider: (AiChatProvider) -> Unit,
    onSendMessage: (String, Boolean) -> Unit,
    onRetryLastMessage: () -> Unit,
    onTestReply: () -> Unit,
    onVoiceInput: ((String) -> Unit) -> Unit,
    onSpeakText: (String) -> Unit,
    onClearChat: () -> Unit,
    onUpdateTone: (MinaPersonality, Int, MinaLanguage) -> Unit,
    onOpenConnections: () -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalMinaPalette.current
    var inputText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showPersonalitySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onVoiceInput { spoken -> onSendMessage(spoken, true) }
        } else {
            onToast("Microphone permission required for voice chat.")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val maxSide = maxOf(info.size.width, info.size.height)
                        if (maxSide > 1080) {
                            decoder.setTargetSampleSize(maxSide / 1080 + 1)
                        }
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
            }.onSuccess { bmp ->
                if (bmp != null) onAttachBitmap(bmp)
                else onToast("Could not load selected image.")
            }.onFailure {
                onToast("Unable to read image.")
            }
        }
    }

    val allMessages = (chatsState as? UiState.Success)?.data.orEmpty()
    val visibleMessages = remember(allMessages, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) allMessages
        else allMessages.filter { it.text.contains(q, ignoreCase = true) }
    }

    LaunchedEffect(visibleMessages.size, streamingReply) {
        val total = visibleMessages.size + if (streamingReply != null) 1 else 0
        if (total > 0 && searchQuery.isBlank()) {
            listState.animateScrollToItem(total - 1)
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
                .imePadding()
                .padding(bottom = 88.dp)
        ) {
            MinaTopBar(
                title = "Chat with ${settings.assistantName}",
                subtitle = "${settings.preferredChatProvider.shortBadge} • ${MinaLanguage.fromRaw(profile.language).displayName}",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (settings.showCharacterEverywhere) {
                            Image(
                                painter = painterResource(id = settings.selectedCharacter.drawableRes),
                                contentDescription = settings.selectedCharacter.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, palette.accentBlue, CircleShape)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = {
                                showSearch = !showSearch
                                if (!showSearch) searchQuery = ""
                            },
                            modifier = Modifier.testTag("chat_search_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search chat",
                                tint = if (showSearch) palette.accentBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onNewConversation,
                            modifier = Modifier.testTag("chat_new_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddComment,
                                contentDescription = "New conversation",
                                tint = palette.accentBlue
                            )
                        }
                        IconButton(
                            onClick = { showPersonalitySheet = true },
                            modifier = Modifier.testTag("chat_personality_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Customize personality",
                                tint = palette.goldHighlight
                            )
                        }
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("chat_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            AnimatedVisibility(visible = showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search messages in conversation...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("chat_search_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = palette.glassFill,
                        unfocusedContainerColor = palette.glassFill
                    )
                )
            }

            // AI Engine + Quick Starter Chips + Test Reply
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AiChatProvider.entries.forEach { provider ->
                    val selected = settings.preferredChatProvider == provider
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (provider == AiChatProvider.GROQ_FAST && !hasGroqKey) {
                                onToast("Add your Groq API key in Connections for Llama 3.3 speed")
                                onOpenConnections()
                            } else {
                                onSwitchProvider(provider)
                            }
                        },
                        leadingIcon = if (provider == AiChatProvider.GROQ_FAST) {
                            { Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = { Text(provider.shortBadge) }
                    )
                }

                GlassCard(
                    modifier = Modifier.testTag("chat_test_reply_button"),
                    cornerRadius = 50.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    onClick = { onTestReply() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Test Reply",
                            tint = palette.goldHighlight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isBusy) "Testing..." else "Test Reply",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.goldHighlight
                        )
                    }
                }

                listOf(
                    "Kaise ho ${settings.assistantName.lowercase().replaceFirstChar { it.uppercase() }}?",
                    "Aaj ka plan banayein?",
                    "Write clean Kotlin code",
                    "Explain Quantum Computing simply",
                    "Motivate me today!"
                ).forEach { prompt ->
                    GlassCard(
                        cornerRadius = 50.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        onClick = { onSendMessage(prompt, false) }
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.accentBlue
                        )
                    }
                }
            }

            if (chatError != null) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = chatError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        if (canRetry) {
                            TextButton(
                                onClick = onRetryLastMessage,
                                enabled = !isBusy,
                                colors = ButtonDefaults.textButtonColors(contentColor = palette.goldHighlight),
                                modifier = Modifier.testTag("chat_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Retry message",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Retry", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (visibleMessages.isEmpty() && streamingReply == null) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            contentPadding = PaddingValues(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) {
                                        "Namaste ${profile.displayName}! Main ${settings.assistantName} hoon."
                                    } else {
                                        "No messages found for \"$searchQuery\""
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Ask anything in Hinglish, Hindi or English — attach photos, write formatted code, or tap mic to speak.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(
                    items = visibleMessages,
                    key = { it.id.ifBlank { "${it.role}_${it.text.hashCode()}_${it.createdAt?.seconds ?: 0}" } }
                ) { msg ->
                    val isLastAi = msg.role != "USER" && msg == visibleMessages.lastOrNull() && !isBusy
                    ChatBubble(
                        assistantName = settings.assistantName,
                        message = msg,
                        showRegenerate = isLastAi,
                        onRegenerate = onRegenerateLast,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("${settings.assistantName} Chat", msg.text))
                            onToast("Copied to clipboard")
                        },
                        onCopyCode = { code ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code Snippet", code))
                            onToast("Code copied to clipboard")
                        },
                        onShare = { PdfAndExportHelper.shareText(context, msg.text, "${settings.assistantName} AI Conversation") },
                        onSpeak = { onSpeakText(msg.text) }
                    )
                }

                if (streamingReply != null) {
                    item(key = "streaming_bubble") {
                        ChatBubble(
                            assistantName = settings.assistantName,
                            message = ChatMessage(
                                role = "MINA",
                                text = streamingReply.ifBlank { "${settings.assistantName} is thinking..." },
                                personality = profile.personality
                            ),
                            isStreaming = true,
                            showRegenerate = false,
                            onRegenerate = {},
                            onCopy = {},
                            onCopyCode = {},
                            onShare = {},
                            onSpeak = {}
                        )
                    }
                }
            }

            // Optional attached image thumbnail pill above composer
            if (attachedBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCard(
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                bitmap = attachedBitmap.asImageBitmap(),
                                contentDescription = "Attached image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Photo attached", style = MaterialTheme.typography.labelMedium)
                            IconButton(
                                onClick = { onAttachBitmap(null) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove photo", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Input Composer with Photo Attach + Text Input + Mic + Send
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(palette.glassFill)
                        .border(1.dp, palette.glassBorderStart.copy(alpha = 0.55f), CircleShape)
                        .testTag("chat_attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attach image",
                        tint = palette.accentBlue
                    )
                }

                Spacer(Modifier.width(6.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask ${settings.assistantName.lowercase().replaceFirstChar { it.uppercase() }} anything…") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(26.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = palette.glassFill,
                        unfocusedContainerColor = palette.glassFill
                    )
                )
                Spacer(Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(palette.glassFill)
                        .border(1.dp, palette.goldHighlight.copy(alpha = 0.7f), CircleShape)
                        .clickable {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onVoiceInput { spoken -> onSendMessage(spoken, true) }
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        .testTag("chat_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.cd_mic),
                        tint = palette.goldHighlight
                    )
                }

                Spacer(Modifier.width(6.dp))

                val canSend = (inputText.isNotBlank() || attachedBitmap != null) && !isBusy
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd))
                        )
                        .clickable(enabled = canSend) {
                            val toSend = inputText
                            inputText = ""
                            onSendMessage(toSend, false)
                        }
                        .testTag("chat_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Chat History?") },
            text = { Text("This deletes saved conversation messages from your account.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearChat()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPersonalitySheet) {
        var selectedP by remember { mutableStateOf(MinaPersonality.fromRaw(profile.personality)) }
        var selectedL by remember { mutableStateOf(MinaLanguage.fromRaw(profile.language)) }
        var emotion by remember { mutableIntStateOf(profile.emotionLevel.toInt().coerceIn(1, 5)) }

        ModalBottomSheet(onDismissRequest = { showPersonalitySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Customize ${settings.assistantName} Personality", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MinaPersonality.entries.forEach { p ->
                        FilterChip(
                            selected = selectedP == p,
                            onClick = { selectedP = p },
                            label = { Text(p.displayName) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Emotion Level: $emotion/5", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = emotion.toFloat(),
                    onValueChange = { emotion = it.toInt().coerceIn(1, 5) },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Spacer(Modifier.height(8.dp))
                Text("Language", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MinaLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = selectedL == lang,
                            onClick = { selectedL = lang },
                            label = { Text(lang.displayName) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        onUpdateTone(selectedP, emotion, selectedL)
                        showPersonalitySheet = false
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Tone", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(
    assistantName: String,
    message: ChatMessage,
    isStreaming: Boolean = false,
    showRegenerate: Boolean,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
    onCopyCode: (String) -> Unit,
    onShare: () -> Unit,
    onSpeak: () -> Unit
) {
    val palette = LocalMinaPalette.current
    val isUser = message.role == "USER"
    val timeText = remember(message.createdAt) {
        val date = message.createdAt?.toDate() ?: Date()
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 336.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd))
                    } else {
                        Brush.linearGradient(listOf(palette.glassFill, palette.glassFill))
                    }
                )
                .border(
                    1.dp,
                    if (isUser) Color.Transparent else palette.glassBorderStart.copy(alpha = 0.5f),
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (!isUser) {
                    Text(
                        text = assistantName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentBlue
                    )
                    Spacer(Modifier.height(2.dp))
                }
                MarkdownMessageView(
                    rawText = message.text,
                    textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onBackground,
                    isUserBubble = isUser,
                    onCopyCode = onCopyCode
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isStreaming) "Streaming..." else timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isStreaming) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (showRegenerate) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Regenerate response",
                                    tint = palette.accentBlue,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable(onClick = onRegenerate)
                                        .testTag("chat_regenerate_button")
                                )
                            }
                            if (!isUser) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak response",
                                    tint = palette.goldHighlight,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable(onClick = onSpeak)
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy message",
                                tint = if (isUser) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(17.dp)
                                    .clickable(onClick = onCopy)
                            )
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share message",
                                tint = if (isUser) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(17.dp)
                                    .clickable(onClick = onShare)
                            )
                        }
                    }
                }
            }
        }
    }
}
