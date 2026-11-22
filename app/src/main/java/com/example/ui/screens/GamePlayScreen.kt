package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ads.AdMobBannerSlot
import com.example.audio.SoundManager
import com.example.data.model.EnvironmentsCatalog
import com.example.data.model.GameMode
import com.example.data.model.RunHudState
import com.example.data.model.RunSummary
import com.example.data.model.formatCoinDisplay
import com.example.game3d.RunnerGLRenderer
import com.example.game3d.RunnerGLSurfaceView
import com.example.game3d.RunnerWorldEngine
import com.example.ui.RunvantaUiState
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MintEmerald

@Composable
fun GamePlayScreen(
    state: RunvantaUiState,
    soundManager: SoundManager,
    onRunCompleted: (RunSummary) -> Unit,
    onClearRunSummary: () -> Unit,
    onWatchRewardedRevive: (Activity?, () -> Unit) -> Unit,
    onNaturalBreakInterstitial: (Activity?, () -> Unit) -> Unit,
    onExitToMenu: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusRequester = remember { FocusRequester() }

    var hud by remember { mutableStateOf(RunHudState()) }

    val activeEnvironment = remember(state.selectedGameMode, state.selectedEnvironment, state.customRunConfig) {
        if (state.selectedGameMode == GameMode.CUSTOM) {
            EnvironmentsCatalog.byId(state.customRunConfig.environmentId)
        } else {
            state.selectedEnvironment
        }
    }

    val engine = remember {
        RunnerWorldEngine(
            onSound = { sfx -> soundManager.playSfx(sfx) },
            onVibrate = { heavy -> soundManager.vibrateImpact(heavy) },
            onHudChanged = { newHud -> hud = newHud },
            onGameOver = { summary -> onRunCompleted(summary) }
        )
    }

    val renderer = remember(engine) { RunnerGLRenderer(engine) }
    val glView = remember(context, engine, renderer) {
        RunnerGLSurfaceView(context, engine, renderer)
    }

    LaunchedEffect(state.selectedCharacter, state.settings) {
        renderer.setCharacter(state.selectedCharacter)
        renderer.settings = state.settings
        glView.swipeSensitivity = state.settings.swipeSensitivity
    }

    LaunchedEffect(Unit) {
        onClearRunSummary()
        engine.startNewRun(
            mode = state.selectedGameMode,
            env = activeEnvironment,
            custom = state.customRunConfig,
            isCustomChar = !state.selectedCharacter.isDefault
        )
        val effectiveSound = if (state.selectedGameMode == GameMode.CUSTOM && !state.customRunConfig.soundEnabled) {
            0f
        } else {
            state.settings.musicVolume
        }
        soundManager.updateAudioConfig(effectiveSound, state.settings.sfxVolume, state.settings.vibrationEnabled)
        soundManager.setGameplayMode(true, activeEnvironment.baseTempoBpm)
        runCatching { focusRequester.requestFocus() }
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    glView.onPause()
                    if (!hud.isGameOver) engine.setPaused(true)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        glView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
            soundManager.updateAudioConfig(state.settings.musicVolume, state.settings.sfxVolume, state.settings.vibrationEnabled)
            soundManager.setGameplayMode(false)
        }
    }

    BackHandler {
        if (!hud.isGameOver) {
            engine.setPaused(!hud.isPaused)
        } else {
            onNaturalBreakInterstitial(activity, onExitToMenu)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft, Key.A -> { engine.moveLeft(); true }
                    Key.DirectionRight, Key.D -> { engine.moveRight(); true }
                    Key.DirectionUp, Key.W, Key.Spacebar -> { engine.jump(); true }
                    Key.DirectionDown, Key.S -> { engine.slide(); true }
                    Key.P, Key.Escape -> { engine.setPaused(!hud.isPaused); true }
                    else -> false
                }
            }
    ) {
        // 3D OpenGL ES Viewport
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize()
        )

        // Active Gameplay HUD (No Ads ever overlap active controls!)
        if (!hud.isGameOver) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top HUD Bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = DeepMidnight.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text(
                                    text = formatCoinDisplay(hud.score),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${hud.distanceMeters}m",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ElectricCyan
                                    )
                                    Text(
                                        text = "${"%.1f".format(hud.multiplier)}x",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CyberGold
                                    )
                                    Text(
                                        text = "${hud.speedKmh} km/h",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = DeepMidnight.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MonetizationOn,
                                        contentDescription = "Run coins",
                                        tint = CyberGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${hud.coinsEarned}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CyberGold
                                    )
                                }
                            }

                            FilledIconButton(
                                onClick = { engine.setPaused(true) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = DeepMidnight.copy(alpha = 0.78f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("pause_button")
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = "Pause game")
                            }
                        }
                    }

                    // Lives, Shield & Active Power-ups row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hud.lives > 0) {
                            Surface(
                                color = DeepMidnight.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(hud.lives.coerceAtMost(5)) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = "Life",
                                            tint = HotCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (hud.hasShield) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Shield,
                                            contentDescription = "Shield active",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        hud.remainingDurationSec?.let { rem ->
                            Surface(
                                color = MintEmerald.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "TIME LEFT: ${rem}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MintEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        hud.activePowerUps.forEach { (type, remSec) ->
                            Surface(
                                color = Color(type.colorRgb or 0xFF000000L).copy(alpha = 0.26f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${type.label} ${remSec.toInt() + 1}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Near Miss floating callout
                AnimatedVisibility(
                    visible = hud.nearMissBanner != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Surface(
                        color = CyberGold,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = hud.nearMissBanner ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepMidnight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // Bottom Left/Right-Handed Tactile Backup Controls (in addition to full-screen swipe)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val leftAlignedJump = state.settings.leftHandedControls
                    if (leftAlignedJump) {
                        JumpSlideCluster(onJump = { engine.jump() }, onSlide = { engine.slide() })
                        LaneMoveCluster(onLeft = { engine.moveLeft() }, onRight = { engine.moveRight() })
                    } else {
                        LaneMoveCluster(onLeft = { engine.moveLeft() }, onRight = { engine.moveRight() })
                        JumpSlideCluster(onJump = { engine.jump() }, onSlide = { engine.slide() })
                    }
                }
            }
        }

        // Pause Overlay
        if (hud.isPaused && !hud.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepMidnight.copy(alpha = 0.80f))
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.88f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PAUSED",
                            style = MaterialTheme.typography.displayMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = "${hud.distanceMeters}m • Score ${formatCoinDisplay(hud.score)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Button(
                            onClick = { engine.setPaused(false) },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("resume_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepMidnight)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("RESUME", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                onClearRunSummary()
                                engine.startNewRun(
                                    state.selectedGameMode,
                                    activeEnvironment,
                                    state.customRunConfig,
                                    !state.selectedCharacter.isDefault
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("RESTART")
                        }
                        OutlinedButton(
                            onClick = onExitToMenu,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Filled.Home, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("MAIN MENU")
                        }
                    }
                }
            }
        }

        // Game Over Overlay (Natural break for optional Rewarded Revive & Banner)
        if (hud.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepMidnight.copy(alpha = 0.86f))
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth(0.90f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (hud.endReason.contains("Complete")) "RUN COMPLETE!" else "RUN ENDED",
                                style = MaterialTheme.typography.displayMedium,
                                color = if (hud.endReason.contains("Complete")) MintEmerald else HotCoral
                            )
                            Text(
                                text = hud.endReason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (state.lastRunSummary?.isNewPersonalBest == true) {
                                Surface(
                                    color = CyberGold,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "NEW PERSONAL BEST!",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DeepMidnight,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ResultStat("SCORE", formatCoinDisplay(hud.score), ElectricCyan)
                                ResultStat("DISTANCE", "${hud.distanceMeters}m", Color.White)
                                ResultStat("COINS", "+${hud.coinsEarned}", CyberGold)
                            }

                            Text(
                                text = "Personal Best: ${formatCoinDisplay(maxOf(state.stats.bestScore, hud.score))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!hud.reviveUsed && !hud.endReason.contains("Complete")) {
                                Button(
                                    onClick = {
                                        onWatchRewardedRevive(activity) {
                                            engine.reviveAfterAd()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("revive_ad_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGold, contentColor = DeepMidnight)
                                ) {
                                    Icon(Icons.Filled.PlayCircle, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Watch Ad for +1 Revive", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            Button(
                                onClick = {
                                    onNaturalBreakInterstitial(activity) {
                                        onClearRunSummary()
                                        engine.startNewRun(
                                            state.selectedGameMode,
                                            activeEnvironment,
                                            state.customRunConfig,
                                            !state.selectedCharacter.isDefault
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("restart_run_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepMidnight)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("RUN AGAIN", style = MaterialTheme.typography.titleMedium)
                            }

                            OutlinedButton(
                                onClick = {
                                    onNaturalBreakInterstitial(activity, onExitToMenu)
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("menu_after_run_button")
                            ) {
                                Icon(Icons.Filled.Home, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("MAIN MENU")
                            }
                        }
                    }

                    AdMobBannerSlot(
                        canRequestAds = state.adConsent.canRequestAds,
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun LaneMoveCluster(onLeft: () -> Unit, onRight: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledIconButton(
            onClick = onLeft,
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = DeepMidnight.copy(alpha = 0.50f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left")
        }
        FilledIconButton(
            onClick = onRight,
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = DeepMidnight.copy(alpha = 0.50f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right")
        }
    }
}

@Composable
private fun JumpSlideCluster(onJump: () -> Unit, onSlide: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledIconButton(
            onClick = onSlide,
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = DeepMidnight.copy(alpha = 0.50f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = "Slide")
        }
        FilledIconButton(
            onClick = onJump,
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ElectricCyan.copy(alpha = 0.55f),
                contentColor = DeepMidnight
            )
        ) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = "Jump")
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
