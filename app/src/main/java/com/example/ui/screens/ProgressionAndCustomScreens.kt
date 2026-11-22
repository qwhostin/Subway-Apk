package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AchievementItem
import com.example.data.model.CustomRunConfig
import com.example.data.model.EnvironmentsCatalog
import com.example.data.model.GameMode
import com.example.data.model.MissionItem
import com.example.data.model.MissionPeriod
import com.example.data.model.formatCoinDisplay
import com.example.data.model.formatDuration
import com.example.data.model.round1
import com.example.ui.RunvantaUiState
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MintEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(
    state: RunvantaUiState,
    onClaimMission: (MissionItem) -> Unit,
    onWatchRewardedBonus: (Activity?) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val activity = LocalActivity.current
    val filtered = remember(state.missions, selectedTab) {
        val period = if (selectedTab == 0) MissionPeriod.DAILY else MissionPeriod.WEEKLY
        state.missions.filter { it.period == period }
    }

    Scaffold(
        containerColor = DeepMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("MISSIONS", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepMidnight,
                    titleContentColor = CyberGold,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab, containerColor = MidnightCard, contentColor = ElectricCyan) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Daily Missions") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Weekly Challenges") })
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { mission ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightCard)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(mission.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Text("+${mission.rewardCoins} Coins", style = MaterialTheme.typography.labelLarge, color = CyberGold)
                                }
                                Text(mission.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LinearProgressIndicator(
                                    progress = { mission.progressFraction },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = if (mission.isCompleted) MintEmerald else ElectricCyan
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${mission.progress} / ${mission.target}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                                    Button(
                                        onClick = { onClaimMission(mission) },
                                        enabled = mission.isCompleted && !mission.claimed,
                                        colors = ButtonDefaults.buttonColors(containerColor = MintEmerald, contentColor = DeepMidnight)
                                    ) {
                                        if (mission.claimed) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("CLAIMED")
                                        } else {
                                            Text(if (mission.isCompleted) "CLAIM" else "IN PROGRESS")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { onWatchRewardedBonus(activity) },
                            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = CyberGold)
                            Spacer(Modifier.width(8.dp))
                            Text("Optional Mission Bonus: Watch Ad for +100 Coins", color = CyberGold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    state: RunvantaUiState,
    onClaimAchievement: (AchievementItem) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = DeepMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("ACHIEVEMENTS & STATS", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepMidnight,
                    titleContentColor = HotCoral,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PERSONAL RECORDS & LIFETIME STATS", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatBlock("Best Score", formatCoinDisplay(state.stats.bestScore))
                            StatBlock("Best Distance", "${state.stats.bestDistanceMeters}m")
                            StatBlock("Total Runs", "${state.stats.totalRuns}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatBlock("Total Distance", "${state.stats.totalDistanceMeters}m")
                            StatBlock("Coins Earned", formatCoinDisplay(state.stats.totalCoinsCollected.toInt()))
                            StatBlock("Time Played", formatDuration(state.stats.totalTimePlayedSec))
                        }
                    }
                }
            }

            items(state.achievements, key = { it.id }) { ach ->
                Card(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (ach.unlocked) CyberGold else Color.Gray
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(ach.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                            Text("+${ach.rewardCoins}", style = MaterialTheme.typography.labelLarge, color = CyberGold)
                        }
                        Text(ach.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(
                            progress = { ach.progressFraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (ach.unlocked) CyberGold else ElectricCyan
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${ach.progress} / ${ach.target}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                            Button(
                                onClick = { onClaimAchievement(ach) },
                                enabled = ach.unlocked && !ach.claimed,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGold, contentColor = DeepMidnight)
                            ) {
                                Text(if (ach.claimed) "CLAIMED" else if (ach.unlocked) "CLAIM" else "LOCKED")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRunScreen(
    state: RunvantaUiState,
    onSaveAndPlayCustom: (CustomRunConfig) -> Unit,
    onBack: () -> Unit
) {
    var cfg by remember(state.customRunConfig) { mutableStateOf(state.customRunConfig.clamped()) }

    Scaffold(
        containerColor = DeepMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("CUSTOM RUN", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepMidnight,
                    titleContentColor = ElectricCyan,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Custom runs use safe balanced limits and do not overwrite your official Classic/Hard personal best records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CustomSliderCard("Starting Speed", "${cfg.startingSpeed.toInt()} m/s", cfg.startingSpeed, 9f..28f) {
                    cfg = cfg.copy(startingSpeed = it).clamped()
                }
                CustomSliderCard("Maximum Speed", "${cfg.maxSpeed.toInt()} m/s", cfg.maxSpeed, 14f..54f) {
                    cfg = cfg.copy(maxSpeed = it).clamped()
                }
                CustomSliderCard("Difficulty Level", "Tier ${cfg.difficultyLevel}/5", cfg.difficultyLevel.toFloat(), 1f..5f) {
                    cfg = cfg.copy(difficultyLevel = it.toInt()).clamped()
                }
                CustomSliderCard("Obstacle Frequency", "${(cfg.obstacleFrequency * 100).toInt()}%", cfg.obstacleFrequency, 0.15f..0.95f) {
                    cfg = cfg.copy(obstacleFrequency = it).clamped()
                }
                CustomSliderCard("Coin Frequency", "${(cfg.coinFrequency * 100).toInt()}%", cfg.coinFrequency, 0.15f..1.0f) {
                    cfg = cfg.copy(coinFrequency = it).clamped()
                }
                CustomSliderCard("Power-up Frequency", "${(cfg.powerUpFrequency * 100).toInt()}%", cfg.powerUpFrequency, 0f..0.60f) {
                    cfg = cfg.copy(powerUpFrequency = it).clamped()
                }
                CustomSliderCard("Starting Lives", "${cfg.startingLives}", cfg.startingLives.toFloat(), 1f..5f) {
                    cfg = cfg.copy(startingLives = it.toInt()).clamped()
                }
                CustomSliderCard("Score Multiplier", "${cfg.scoreMultiplier.round1()}x", cfg.scoreMultiplier, 0.5f..3.0f) {
                    cfg = cfg.copy(scoreMultiplier = it.round1()).clamped()
                }

                // Run Duration Options
                Text("Run Duration", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Unlimited", 60 to "60s", 120 to "2 min", 180 to "3 min").forEach { (sec, label) ->
                        FilterChip(
                            selected = cfg.durationSeconds == sec,
                            onClick = { cfg = cfg.copy(durationSeconds = sec) },
                            label = { Text(label) }
                        )
                    }
                }

                // Environment Picker
                Text("Environment", style = MaterialTheme.typography.labelLarge, color = Color.White)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
                    items(EnvironmentsCatalog.all) { env ->
                        FilterChip(
                            selected = cfg.environmentId == env.id,
                            onClick = { cfg = cfg.copy(environmentId = env.id) },
                            label = { Text(env.name) }
                        )
                    }
                }

                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MidnightCard)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("In-Run Music Enabled", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Switch(checked = cfg.soundEnabled, onCheckedChange = { cfg = cfg.copy(soundEnabled = it) })
                    }
                }

                Button(
                    onClick = { onSaveAndPlayCustom(cfg.clamped()) },
                    modifier = Modifier.fillMaxWidth().height(58.dp).testTag("launch_custom_run_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepMidnight)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("LAUNCH CUSTOM RUN", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun CustomSliderCard(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MidnightCard)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text(valueText, style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
            }
            Slider(value = value, onValueChange = onChange, valueRange = range)
        }
    }
}
