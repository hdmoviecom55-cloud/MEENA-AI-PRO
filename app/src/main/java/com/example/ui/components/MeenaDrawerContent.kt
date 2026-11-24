package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettingsState
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalMinaPalette

private data class DrawerMenuItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun MeenaDrawerContent(
    profile: UserProfile,
    settings: AppSettingsState,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val palette = LocalMinaPalette.current

    val primaryItems = listOf(
        DrawerMenuItem("Home", "home", Icons.Filled.Home),
        DrawerMenuItem("AI Chat", "chat", Icons.Filled.ChatBubble),
        DrawerMenuItem("Voice Companion", "voice", Icons.Filled.Mic),
        DrawerMenuItem("AI Characters", "characters", Icons.Filled.FaceRetouchingNatural),
        DrawerMenuItem("AI Image Studio", "image_studio", Icons.Filled.Brush),
        DrawerMenuItem("Chat History", "history", Icons.Filled.History)
    )

    val studioItems = listOf(
        DrawerMenuItem("Quick Actions", "quick_actions", Icons.Filled.Bolt),
        DrawerMenuItem("Connections & API Keys", "connections", Icons.Filled.Hub),
        DrawerMenuItem("AI Memory Vault", "creator_hub", Icons.Filled.Psychology),
        DrawerMenuItem("Phone Control", "phone_control", Icons.Filled.PhoneAndroid),
        DrawerMenuItem("Live Vision & Camera", "vision", Icons.Filled.CameraAlt)
    )

    val writeAndMoreItems = listOf(
        DrawerMenuItem("Documents & PDF", "documents", Icons.Filled.Description),
        DrawerMenuItem("Coding Studio", "coding", Icons.Filled.Code),
        DrawerMenuItem("Study & Quiz", "study", Icons.Filled.MenuBook),
        DrawerMenuItem("Private Journal", "journal", Icons.Filled.Book),
        DrawerMenuItem("Music & Vibes", "music", Icons.Filled.MusicNote),
        DrawerMenuItem("Location & Places", "location", Icons.Filled.LocationOn),
        DrawerMenuItem("Themes & Orbs", "themes", Icons.Filled.Palette),
        DrawerMenuItem("Settings", "settings", Icons.Filled.Settings)
    )

    ModalDrawerSheet(
        drawerContainerColor = palette.bgBottom,
        drawerContentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.width(304.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd)))
                        .padding(2.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.meena_launcher_icon_1795505211660),
                        contentDescription = "MEENA AI PRO",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MEENA AI PRO",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.goldHighlight,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${profile.displayName} • ${settings.selectedCharacter.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = palette.glassBorderStart.copy(alpha = 0.25f))
            Spacer(Modifier.height(10.dp))

            DrawerSectionHeader("CORE EXPERIENCE")
            primaryItems.forEach { item ->
                DrawerNavRow(item, currentRoute == item.route) { onNavigate(item.route) }
            }

            Spacer(Modifier.height(10.dp))
            DrawerSectionHeader("AI & PHONE SYSTEM")
            studioItems.forEach { item ->
                DrawerNavRow(item, currentRoute == item.route) { onNavigate(item.route) }
            }

            Spacer(Modifier.height(10.dp))
            DrawerSectionHeader("WRITE & TOOLS")
            writeAndMoreItems.forEach { item ->
                DrawerNavRow(item, currentRoute == item.route) { onNavigate(item.route) }
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    val palette = LocalMinaPalette.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = palette.accentBlue,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@Composable
private fun DrawerNavRow(
    item: DrawerMenuItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalMinaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) palette.gradientStart.copy(alpha = 0.24f) else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (selected) palette.accentBlue else palette.goldHighlight,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) palette.accentBlue else MaterialTheme.colorScheme.onBackground
        )
    }
}
