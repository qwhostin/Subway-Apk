package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdMobBannerSlot
import com.example.data.model.GameMode
import com.example.data.model.formatCoinDisplay
import com.example.ui.RunvantaUiState
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.VioletPulse

@Composable
fun HomeScreen(
    state: RunvantaUiState,
    onPlay: () -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onOpenCharacters: () -> Unit,
    onOpenCharacterStudio: () -> Unit,
    onOpenEnvironments: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenCustomRun: () -> Unit,
    onOpenSettings: () -> Unit,
    onWatchRewardedCoins: (Activity?) -> Unit
) {
    val activity = LocalActivity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepMidnight, Color(0xFF101836), DeepMidnight)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Brand & Coin Wallet Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RUNVANTA",
                            style = MaterialTheme.typography.displayMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = "BY QW HOSTING • BEST: ${formatCoinDisplay(state.stats.bestScore)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MidnightCard,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = "Coins",
                                tint = CyberGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = formatCoinDisplay(state.coins),
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Hero Banner Card with Active Runner & World
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Box(modifier = Modifier.height(176.dp).fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = "RUNVANTA 3D Endless Runner Arena",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.25f),
                                            DeepMidnight.copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = ElectricCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "RUNNER: ${state.selectedCharacter.name.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    color = HotCoral.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = state.selectedEnvironment.name.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HotCoral,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "PLAY AS ANYONE IN 3D",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Snap or pick any photo in Character Studio to sprint as your own 3D hero.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                // Game Mode Selector Strip
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECT GAME MODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(GameMode.entries) { mode ->
                            val selected = state.selectedGameMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (mode == GameMode.CUSTOM) {
                                        onSelectMode(mode)
                                        onOpenCustomRun()
                                    } else {
                                        onSelectMode(mode)
                                    }
                                },
                                label = {
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan,
                                    selectedLabelColor = DeepMidnight,
                                    containerColor = MidnightCard,
                                    labelColor = Color.White
                                ),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                    Text(
                        text = state.selectedGameMode.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan
                    )
                }

                // Primary PLAY Button
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("play_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = DeepMidnight
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "PLAY ${state.selectedGameMode.displayName.uppercase()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Signature Character Studio Highlight Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCharacterStudio() }
                        .testTag("character_studio_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1844)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, VioletPulse)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(VioletPulse.copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Face,
                                contentDescription = "Character Studio",
                                tint = ElectricCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CHARACTER STUDIO",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Upload photo • Crop & map face onto 3D hero",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ElectricCyan
                            )
                        }
                    }
                }

                // Main Menu Action Grid (All 8 Menu Destinations)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuTileCard(
                        title = "CHARACTERS",
                        subtitle = "${state.characters.size} Runners",
                        icon = Icons.Filled.Groups,
                        accent = ElectricCyan,
                        tag = "characters_button",
                        onClick = onOpenCharacters,
                        modifier = Modifier.weight(1f)
                    )
                    MenuTileCard(
                        title = "ENVIRONMENTS",
                        subtitle = "8 3D Worlds",
                        icon = Icons.Filled.Landscape,
                        accent = MintEmerald,
                        tag = "environments_button",
                        onClick = onOpenEnvironments,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuTileCard(
                        title = "MISSIONS",
                        subtitle = "Daily & Weekly",
                        icon = Icons.Filled.Flag,
                        accent = CyberGold,
                        tag = "missions_button",
                        onClick = onOpenMissions,
                        modifier = Modifier.weight(1f)
                    )
                    MenuTileCard(
                        title = "ACHIEVEMENTS",
                        subtitle = "Records & Badges",
                        icon = Icons.Filled.EmojiEvents,
                        accent = HotCoral,
                        tag = "achievements_button",
                        onClick = onOpenAchievements,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuTileCard(
                        title = "CUSTOM RUN",
                        subtitle = "Tune All Rules",
                        icon = Icons.Filled.Tune,
                        accent = VioletPulse,
                        tag = "custom_run_button",
                        onClick = onOpenCustomRun,
                        modifier = Modifier.weight(1f)
                    )
                    MenuTileCard(
                        title = "SETTINGS",
                        subtitle = "Audio, GFX, Legal",
                        icon = Icons.Filled.Settings,
                        accent = Color(0xFF94A3B8),
                        tag = "settings_button",
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Voluntary Rewarded Ad Option (Clearly explained before user chooses)
                OutlinedButton(
                    onClick = { onWatchRewardedCoins(activity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("rewarded_coins_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberGold.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = CyberGold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Optional: Watch Ad for +100 Bonus Coins",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberGold
                    )
                }
            }

            // Non-intrusive Banner Ad Slot strictly on Menu (never over gameplay controls)
            AdMobBannerSlot(
                canRequestAds = state.adConsent.canRequestAds,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun MenuTileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(92.dp)
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
