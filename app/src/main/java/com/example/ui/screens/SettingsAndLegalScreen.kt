package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.GameSettings
import com.example.data.model.GraphicsQuality
import com.example.ui.RunvantaUiState
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MintEmerald

private enum class LegalDialogType { NONE, PRIVACY, TERMS, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: RunvantaUiState,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onOpenPrivacyOptions: (Activity) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val recommendedGfx = remember(context) { GameSettings.detectRecommendedQuality(context) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var legalDialog by remember { mutableStateOf(LegalDialogType.NONE) }
    val s = state.settings

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Game Progress?") },
            text = {
                Text("This deletes all saved coins, unlocked worlds, missions, achievements, and custom Character Studio photos on this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetProgress()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotCoral)
                ) { Text("Reset Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (legalDialog != LegalDialogType.NONE) {
        AlertDialog(
            onDismissRequest = { legalDialog = LegalDialogType.NONE },
            title = {
                Text(
                    when (legalDialog) {
                        LegalDialogType.PRIVACY -> "Privacy Policy — RUNVANTA"
                        LegalDialogType.TERMS -> "Terms of Service — RUNVANTA"
                        LegalDialogType.ABOUT -> "About RUNVANTA & QW HOSTING"
                        LegalDialogType.NONE -> ""
                    }
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = when (legalDialog) {
                            LegalDialogType.PRIVACY -> PRIVACY_POLICY_TEXT
                            LegalDialogType.TERMS -> TERMS_OF_SERVICE_TEXT
                            LegalDialogType.ABOUT -> ABOUT_GAME_TEXT
                            LegalDialogType.NONE -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { legalDialog = LegalDialogType.NONE }) { Text("Close") }
            }
        )
    }

    Scaffold(
        containerColor = DeepMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", style = MaterialTheme.typography.headlineLarge) },
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
                // Graphics Quality
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MidnightCard)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Graphics Quality (Recommended: ${recommendedGfx.label})", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GraphicsQuality.entries.forEach { q ->
                                FilterChip(
                                    selected = s.graphicsQuality == q,
                                    onClick = { onUpdateSettings { it.copy(graphicsQuality = q) } },
                                    label = { Text(q.label) }
                                )
                            }
                        }

                        Text("Target Frame Rate (FPS)", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 60, 90, 120).forEach { fps ->
                                FilterChip(
                                    selected = s.targetFps == fps,
                                    onClick = { onUpdateSettings { it.copy(targetFps = fps) } },
                                    label = { Text("$fps FPS") }
                                )
                            }
                        }
                    }
                }

                // Audio Sliders
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MidnightCard)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Music Volume (${(s.musicVolume * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Slider(value = s.musicVolume, onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } })
                        Text("Sound Effects Volume (${(s.sfxVolume * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Slider(value = s.sfxVolume, onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } })
                    }
                }

                // Controls & Language
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MidnightCard)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Swipe Sensitivity (${"%.1f".format(s.swipeSensitivity)}x)", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Slider(
                            value = s.swipeSensitivity,
                            onValueChange = { v -> onUpdateSettings { it.copy(swipeSensitivity = v) } },
                            valueRange = 0.5f..2.0f
                        )
                        SettingsSwitchRow("Left-Handed Controls", s.leftHandedControls) { v ->
                            onUpdateSettings { it.copy(leftHandedControls = v) }
                        }
                        SettingsSwitchRow("Vibration Feedback", s.vibrationEnabled) { v ->
                            onUpdateSettings { it.copy(vibrationEnabled = v) }
                        }
                        SettingsSwitchRow("Camera Shake", s.cameraShakeEnabled) { v ->
                            onUpdateSettings { it.copy(cameraShakeEnabled = v) }
                        }
                        SettingsSwitchRow("Motion Trail Effects", s.motionEffectsEnabled) { v ->
                            onUpdateSettings { it.copy(motionEffectsEnabled = v) }
                        }
                        SettingsSwitchRow("Performance Mode", s.performanceMode) { v ->
                            onUpdateSettings { it.copy(performanceMode = v, batterySaverMode = if (v) false else it.batterySaverMode) }
                        }
                        SettingsSwitchRow("Battery-Saving Mode (30 FPS)", s.batterySaverMode) { v ->
                            onUpdateSettings { it.copy(batterySaverMode = v, performanceMode = if (v) false else it.performanceMode) }
                        }

                        Text("Language", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.take(3).forEach { lang ->
                                FilterChip(
                                    selected = s.language == lang,
                                    onClick = { onUpdateSettings { it.copy(language = lang) } },
                                    label = { Text(lang.label) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.drop(3).forEach { lang ->
                                FilterChip(
                                    selected = s.language == lang,
                                    onClick = { onUpdateSettings { it.copy(language = lang) } },
                                    label = { Text(lang.label) }
                                )
                            }
                        }
                    }
                }

                // Ad Consent & Legal
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MidnightCard)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Privacy, Compliance & Legal", style = MaterialTheme.typography.titleMedium, color = MintEmerald)
                        Text(
                            "Ad Status: ${state.adConsent.statusNote}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.adConsent.privacyOptionsRequired && activity != null) {
                            OutlinedButton(
                                onClick = { onOpenPrivacyOptions(activity) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Manage Ad Privacy Choices (UMP)")
                            }
                        }
                        OutlinedButton(onClick = { legalDialog = LegalDialogType.PRIVACY }, modifier = Modifier.fillMaxWidth()) {
                            Text("Privacy Policy (QW HOSTING)")
                        }
                        OutlinedButton(onClick = { legalDialog = LegalDialogType.TERMS }, modifier = Modifier.fillMaxWidth()) {
                            Text("Terms of Service")
                        }
                        OutlinedButton(onClick = { legalDialog = LegalDialogType.ABOUT }, modifier = Modifier.fillMaxWidth()) {
                            Text("About RUNVANTA & Data Safety")
                        }
                    }
                }

                Button(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reset_progress_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = HotCoral, contentColor = Color.White)
                ) {
                    Text("RESET ALL LOCAL PROGRESS")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private const val PRIVACY_POLICY_TEXT = """RUNVANTA Privacy Policy
Publisher: QW HOSTING (Subsidiary of Al Quddus Group (Pvt) Ltd)
Website: https://qwhosting.com
Contact: info@qwhosting.com

1. Local-Only Photo Processing (Play As Anyone):
• RUNVANTA uses the zero-permission Android System Photo Picker. We never request broad storage, camera, contacts, microphone, or location permissions.
• Photos selected in Character Studio remain 100% on your device inside private app storage.
• Photos are NEVER uploaded to external servers and are NEVER used for AI model training.
• Temporary image files are automatically deleted after processing. You can delete any custom character and its photo at any time.

2. Local Game Saves:
• Coins, scores, missions, achievements, and custom characters are stored locally on your device via Room & DataStore. No account or login is required.

3. Advertising (Google AdMob & UMP):
• RUNVANTA is free to play and supported solely by Google AdMob advertisements (Banner, frequency-capped Interstitial, and optional Rewarded ads).
• Where required, Google's User Messaging Platform (UMP) asks for your consent and supports non-personalized ads."""

private const val TERMS_OF_SERVICE_TEXT = """RUNVANTA Terms of Service
Publisher: QW HOSTING (https://qwhosting.com)

1. Acceptable Use of Play As Anyone:
• You must obtain permission before using a photograph of another person.
• Do not create characters that promote impersonation fraud, harassment, hate speech, or sexually explicit/abusive content.

2. Virtual Economy:
• Coins and unlockable cosmetics/environments are virtual in-game items earned solely through gameplay or optional rewarded ads.
• RUNVANTA contains NO real-money purchases, NO subscriptions, and NO loot boxes.

3. Disclaimer:
• Provided 'as is' under applicable law. Contact info@qwhosting.com for support."""

private const val ABOUT_GAME_TEXT = """RUNVANTA v1.0
Original 3D Endless Runner by QW HOSTING
Website: https://qwhosting.com
Email: info@qwhosting.com
Location: 18th Floor, B.R.R Tower, I.I. Chundrigar Road, Karachi, Pakistan

Engine: Native Android Kotlin + Jetpack Compose + OpenGL ES 3.0 + Procedural Audio Synthesizer.
Target SDK: Android API 36 (ARM64 & Universal).

Google Play Data Safety Summary:
• Photos & Videos: Processed locally on-device only; never collected or transmitted off-device.
• Device / Ad Identifiers: Used solely by Google Mobile Ads SDK subject to UMP consent."""
