package com.example.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppSettingsState
import com.example.data.model.GeneratedImageRecord
import com.example.data.model.ImageArtStyle
import com.example.data.model.ImageQualityOption
import com.example.data.model.UiState
import com.example.ui.components.AmberGradientButton
import com.example.ui.components.GlassCard
import com.example.ui.components.MinaBackdrop
import com.example.ui.components.MinaTopBar
import com.example.ui.theme.LocalMinaPalette
import java.io.File

@Composable
fun ImageGenerationScreen(
    settings: AppSettingsState,
    imageGenState: UiState<GeneratedImageRecord>?,
    imageHistory: List<GeneratedImageRecord>,
    onBack: () -> Unit,
    onGenerateImage: (String, ImageArtStyle, ImageQualityOption) -> Unit,
    onSaveToGallery: (GeneratedImageRecord) -> Unit,
    onShareImage: (GeneratedImageRecord) -> Unit,
    onDeleteHistoryItem: (String) -> Unit
) {
    val palette = LocalMinaPalette.current
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(ImageArtStyle.CYBERPUNK_NEON) }
    var selectedQuality by remember { mutableStateOf(ImageQualityOption.SQUARE_HD) }
    var selectedHistoryRecord by remember { mutableStateOf<GeneratedImageRecord?>(null) }

    val activeRecord = (imageGenState as? UiState.Success)?.data ?: selectedHistoryRecord

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
                title = "AI Image Studio",
                subtitle = "Create artwork with Gemini Nano Banana 2.1",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            label = { Text("Describe the image you want to create") },
                            placeholder = { Text("e.g. A glowing cyberpunk cityscape in monsoon rain, neon purple reflections") },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("image_prompt_input"),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Futuristic purple nebula astronaut",
                                "Cozy cyberpunk Mumbai street at night",
                                "Golden lotus floating on crystal lake",
                                "Minimalist glowing AI neural sculpture"
                            ).forEach { idea ->
                                GlassCard(
                                    cornerRadius = 50.dp,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                                    onClick = { prompt = idea }
                                ) {
                                    Text(idea, style = MaterialTheme.typography.bodyMedium, color = palette.accentBlue)
                                }
                            }
                        }

                        Text("Art Style", style = MaterialTheme.typography.titleMedium, color = palette.goldHighlight)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImageArtStyle.entries.forEach { st ->
                                FilterChip(
                                    selected = selectedStyle == st,
                                    onClick = { selectedStyle = st },
                                    label = { Text(st.label) }
                                )
                            }
                        }

                        Text("Aspect Ratio & Quality", style = MaterialTheme.typography.titleMedium, color = palette.goldHighlight)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImageQualityOption.entries.forEach { q ->
                                FilterChip(
                                    selected = selectedQuality == q,
                                    onClick = { selectedQuality = q },
                                    label = { Text(q.label) }
                                )
                            }
                        }

                        AmberGradientButton(
                            text = if (imageGenState is UiState.Loading) "Synthesizing Artwork..." else "Generate AI Image",
                            icon = Icons.Filled.Brush,
                            enabled = prompt.isNotBlank() && imageGenState !is UiState.Loading,
                            onClick = {
                                selectedHistoryRecord = null
                                onGenerateImage(prompt, selectedStyle, selectedQuality)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("image_generate_button")
                        )
                    }
                }

                // Active Synthesis / Result / Real Error
                when (imageGenState) {
                    UiState.Loading -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = palette.accentBlue)
                                Text(
                                    text = "Creating your ${selectedStyle.label} image...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.goldHighlight
                                )
                                Text(
                                    text = "High-detail Gemini synthesis typically takes 15–45 seconds.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Generation Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = imageGenState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                OutlinedButton(
                                    onClick = { onGenerateImage(prompt, selectedStyle, selectedQuality) },
                                    enabled = prompt.isNotBlank()
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    else -> Unit
                }

                if (activeRecord != null && imageGenState !is UiState.Loading) {
                    val bmp = remember(activeRecord.filePath) { BitmapFactory.decodeFile(activeRecord.filePath) }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accentBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${activeRecord.style} • ${activeRecord.aspectRatio}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.accentBlue
                                    )
                                }
                                Text(
                                    text = "Edit Prompt",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.goldHighlight,
                                    modifier = Modifier.clickable { prompt = activeRecord.prompt }
                                )
                            }

                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = activeRecord.prompt,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .border(1.dp, palette.glassBorderEnd, RoundedCornerShape(18.dp))
                                )
                            }

                            Text(
                                text = activeRecord.prompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AmberGradientButton(
                                    text = "Save to Gallery",
                                    icon = Icons.Filled.Download,
                                    onClick = { onSaveToGallery(activeRecord) },
                                    modifier = Modifier.weight(1f).testTag("image_save_button"),
                                    height = 46.dp
                                )
                                OutlinedButton(
                                    onClick = { onShareImage(activeRecord) },
                                    modifier = Modifier.weight(1f).height(46.dp).testTag("image_share_button")
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }

                // Image History
                if (imageHistory.isNotEmpty()) {
                    Text(
                        text = "Image History (${imageHistory.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        imageHistory.forEach { rec ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp),
                                onClick = {
                                    selectedHistoryRecord = rec
                                    prompt = rec.prompt
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = File(rec.filePath),
                                        contentDescription = rec.prompt,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, palette.glassBorderStart, RoundedCornerShape(12.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.prompt,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = "${rec.style} • ${rec.aspectRatio}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.accentBlue
                                        )
                                    }
                                    IconButton(onClick = { onSaveToGallery(rec) }) {
                                        Icon(Icons.Filled.Download, contentDescription = "Save image", tint = palette.goldHighlight)
                                    }
                                    IconButton(onClick = { onDeleteHistoryItem(rec.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete image", tint = MaterialTheme.colorScheme.error)
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
