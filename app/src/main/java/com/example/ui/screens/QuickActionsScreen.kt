package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.PhoneControlHelper
import com.example.data.model.AppSettingsState
import com.example.data.model.QuickActionId
import com.example.data.model.UiState
import com.example.ui.components.AmberGradientButton
import com.example.ui.components.GlassCard
import com.example.ui.components.MinaBackdrop
import com.example.ui.components.MinaTopBar
import com.example.ui.theme.LocalMinaPalette

private fun QuickActionId.iconVector(): ImageVector = when (this) {
    QuickActionId.MAKE_CALL -> Icons.Filled.Call
    QuickActionId.SEND_MESSAGE -> Icons.Filled.Message
    QuickActionId.OPEN_APPS -> Icons.Filled.Apps
    QuickActionId.SET_ALARM -> Icons.Filled.Alarm
    QuickActionId.WEATHER -> Icons.Filled.WbSunny
    QuickActionId.SEARCH_INTERNET -> Icons.Filled.Search
    QuickActionId.CREATE_IMAGE -> Icons.Filled.Brush
    QuickActionId.TRANSLATE -> Icons.Filled.Translate
    QuickActionId.CALCULATOR -> Icons.Filled.Calculate
    QuickActionId.NOTES -> Icons.Filled.EditNote
    QuickActionId.REMINDERS -> Icons.Filled.NotificationsActive
    QuickActionId.CAMERA -> Icons.Filled.CameraAlt
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionsScreen(
    settings: AppSettingsState,
    weatherState: UiState<String>?,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onSendChatPrompt: (String) -> Unit,
    onFetchWeather: (String) -> Unit,
    onUpdateSettings: ((AppSettingsState) -> AppSettingsState) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalMinaPalette.current

    var customizeMode by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<QuickActionId?>(null) }

    val orderedActions = remember(settings.pinnedQuickActions) {
        val pinned = settings.pinnedQuickActions
        val rest = QuickActionId.entries.filterNot { it in pinned }
        pinned + rest
    }

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
                title = "Quick Actions",
                subtitle = "12 Instant Companion & Phone Shortcuts",
                onBack = onBack,
                trailing = {
                    TextButton(onClick = { customizeMode = !customizeMode }) {
                        Text(
                            text = if (customizeMode) "Done" else "Customize",
                            color = palette.accentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                orderedActions.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { action ->
                            val isPinned = action in settings.pinnedQuickActions
                            GlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_action_tile_${action.name}"),
                                cornerRadius = 20.dp,
                                contentPadding = PaddingValues(14.dp),
                                onClick = {
                                    if (customizeMode) {
                                        onUpdateSettings { st ->
                                            val cur = st.pinnedQuickActions.toMutableList()
                                            if (action in cur && cur.size > 4) cur.remove(action)
                                            else if (action !in cur) cur.add(0, action)
                                            st.copy(pinnedQuickActions = cur)
                                        }
                                    } else {
                                        when (action) {
                                            QuickActionId.CREATE_IMAGE -> onNavigate("image_studio")
                                            QuickActionId.NOTES -> onNavigate("journal")
                                            QuickActionId.CAMERA -> onNavigate("vision")
                                            QuickActionId.WEATHER -> {
                                                activeDialog = QuickActionId.WEATHER
                                                if (weatherState == null) onFetchWeather("New Delhi")
                                            }
                                            else -> activeDialog = action
                                        }
                                    }
                                }
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(palette.gradientStart, palette.gradientEnd)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = action.iconVector(),
                                                contentDescription = action.title,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        if (customizeMode) {
                                            Icon(
                                                imageVector = Icons.Filled.PushPin,
                                                contentDescription = "Pin quick action",
                                                tint = if (isPinned) palette.accentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = action.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = action.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // Functional Quick Action Dialogs
    when (activeDialog) {
        QuickActionId.MAKE_CALL -> {
            var phoneInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Make a Call") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter a phone number to open Android Dialer safely (you confirm before calling).")
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        activeDialog = null
                        PhoneControlHelper.openDialer(context, phoneInput)
                            .onFailure { onToast("Dialer unavailable") }
                    }) { Text("Open Dialer") }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Cancel") }
                }
            )
        }

        QuickActionId.SEND_MESSAGE -> {
            var phone by remember { mutableStateOf("") }
            var body by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Send Message") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Recipient number (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text("Message text") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        activeDialog = null
                        PhoneControlHelper.openSmsComposer(context, phone, body)
                            .onFailure { onToast("Messaging app unavailable") }
                    }) { Text("Open Messages") }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Cancel") }
                }
            )
        }

        QuickActionId.OPEN_APPS -> {
            val installed = remember { PhoneControlHelper.getInstalledLaunchableApps(context) }
            var filter by remember { mutableStateOf("") }
            val filtered = installed.filter { filter.isBlank() || it.appName.contains(filter, ignoreCase = true) }.take(24)
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Launch Installed App") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = filter,
                            onValueChange = { filter = it },
                            label = { Text("Search apps...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Column(
                            modifier = Modifier
                                .height(240.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (filtered.isEmpty()) {
                                Text("No matching launcher apps found.")
                            } else {
                                filtered.forEach { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                activeDialog = null
                                                PhoneControlHelper.launchPackage(context, app.packageName)
                                                    .onFailure { onToast("Could not open ${app.appName}") }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Apps, contentDescription = null, tint = palette.accentBlue, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Close") }
                }
            )
        }

        QuickActionId.SET_ALARM, QuickActionId.REMINDERS -> {
            var hour by remember { mutableIntStateOf(7) }
            var minute by remember { mutableIntStateOf(30) }
            var label by remember { mutableStateOf("MEENA Reminder") }
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text(if (activeDialog == QuickActionId.REMINDERS) "Set Quick Reminder Alarm" else "Set Alarm") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Alarm / Reminder label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { hour = (hour + 1) % 24 }, modifier = Modifier.weight(1f)) {
                                Text("Hour: ${String.format("%02d", hour)}")
                            }
                            OutlinedButton(onClick = { minute = (minute + 5) % 60 }, modifier = Modifier.weight(1f)) {
                                Text("Min: ${String.format("%02d", minute)}")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        activeDialog = null
                        PhoneControlHelper.setAlarm(context, hour, minute, label)
                            .onFailure { onToast("Clock app unavailable") }
                    }) { Text("Set Alarm") }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Cancel") }
                }
            )
        }

        QuickActionId.WEATHER -> {
            var city by remember { mutableStateOf("New Delhi") }
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Live Weather") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AmberGradientButton(
                            text = "Check Forecast",
                            onClick = { onFetchWeather(city) },
                            modifier = Modifier.fillMaxWidth(),
                            height = 42.dp
                        )
                        when (weatherState) {
                            UiState.Loading -> CircularProgressIndicator(color = palette.accentBlue, modifier = Modifier.size(24.dp))
                            is UiState.Success -> Text(weatherState.data, style = MaterialTheme.typography.bodyLarge)
                            is UiState.Error -> Text(weatherState.message, color = MaterialTheme.colorScheme.error)
                            null -> Unit
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Close") }
                }
            )
        }

        QuickActionId.SEARCH_INTERNET -> {
            var query by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Search Internet") },
                text = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search query") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(enabled = query.isNotBlank(), onClick = {
                        activeDialog = null
                        PhoneControlHelper.openExternalUrlOrApp(
                            context,
                            "https://www.google.com/search?q=${Uri.encode(query.trim())}"
                        )
                    }) { Text("Search") }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Cancel") }
                }
            )
        }

        QuickActionId.TRANSLATE -> {
            var textToTranslate by remember { mutableStateOf("") }
            var targetLang by remember { mutableStateOf("Hindi") }
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("AI Translate with MEENA") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = textToTranslate,
                            onValueChange = { textToTranslate = it },
                            label = { Text("Text to translate") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Hindi", "English", "Hinglish", "Spanish", "French").forEach { l ->
                                FilterChip(
                                    selected = targetLang == l,
                                    onClick = { targetLang = l },
                                    label = { Text(l) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = textToTranslate.isNotBlank(), onClick = {
                        activeDialog = null
                        onSendChatPrompt("Translate this naturally into $targetLang:\n\"${textToTranslate.trim()}\"")
                        onNavigate("chat")
                    }) { Text("Translate") }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Cancel") }
                }
            )
        }

        QuickActionId.CALCULATOR -> {
            var expr by remember { mutableStateOf("") }
            var calcResult by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Quick Calculator") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expr,
                            onValueChange = { expr = it },
                            label = { Text("Expression (e.g. 2450 * 0.18 + 2450)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (calcResult.isNotBlank()) {
                            Text(
                                text = "= $calcResult",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.accentBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        calcResult = evaluateSimpleMath(expr)
                    }) { Text("Calculate") }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Close") }
                }
            )
        }

        else -> Unit
    }
}

internal fun evaluateSimpleMath(input: String): String {
    return try {
        val clean = input.replace(" ", "")
        if (clean.isEmpty()) return "Enter expression"
        val tokens = mutableListOf<String>()
        var num = StringBuilder()
        clean.forEachIndexed { idx, ch ->
            if (ch.isDigit() || ch == '.' || (ch == '-' && (idx == 0 || clean[idx - 1] in "+-*/("))) {
                num.append(ch)
            } else if (ch in "+-*/") {
                if (num.isNotEmpty()) {
                    tokens.add(num.toString())
                    num = StringBuilder()
                }
                tokens.add(ch.toString())
            } else {
                return "Only +, -, *, / supported"
            }
        }
        if (num.isNotEmpty()) tokens.add(num.toString())
        val values = mutableListOf<Double>()
        val ops = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            if (t == "*" || t == "/") {
                val lhs = values.removeAt(values.lastIndex)
                val rhs = tokens[++i].toDouble()
                values.add(if (t == "*") lhs * rhs else lhs / rhs)
            } else if (t == "+" || t == "-") {
                ops.add(t)
            } else {
                values.add(t.toDouble())
            }
            i++
        }
        var acc = values.first()
        for (j in ops.indices) {
            val rhs = values[j + 1]
            acc = if (ops[j] == "+") acc + rhs else acc - rhs
        }
        if (acc % 1.0 == 0.0) acc.toLong().toString() else String.format("%.4f", acc).trimEnd('0').trimEnd('.')
    } catch (_: Exception) {
        "Invalid expression"
    }
}
