package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.GameMode
import com.example.ui.RunvantaViewModel
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.CharacterStudioScreen
import com.example.ui.screens.CharactersScreen
import com.example.ui.screens.CustomRunScreen
import com.example.ui.screens.EnvironmentsScreen
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.SettingsScreen

object RunvantaRoutes {
    const val HOME = "home"
    const val PLAY = "play"
    const val CHARACTERS = "characters"
    const val STUDIO = "character_studio"
    const val ENVIRONMENTS = "environments"
    const val MISSIONS = "missions"
    const val ACHIEVEMENTS = "achievements"
    const val CUSTOM_RUN = "custom_run"
    const val SETTINGS = "settings"
}

@Composable
fun RunvantaNavGraph(viewModel: RunvantaViewModel) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editingChar by viewModel.editingCharacter.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = RunvantaRoutes.HOME
        ) {
            composable(RunvantaRoutes.HOME) {
                HomeScreen(
                    state = state,
                    onPlay = {
                        viewModel.playUiClick()
                        navController.navigate(RunvantaRoutes.PLAY)
                    },
                    onSelectMode = { mode -> viewModel.selectGameMode(mode) },
                    onOpenCharacters = {
                        viewModel.playUiClick()
                        navController.navigate(RunvantaRoutes.CHARACTERS)
                    },
                    onOpenCharacterStudio = {
                        viewModel.playUiClick()
                        viewModel.startEditingCharacter(null)
                        navController.navigate(RunvantaRoutes.STUDIO)
                    },
                    onOpenEnvironments = {
                        viewModel.playUiClick()
                        navController.navigate(RunvantaRoutes.ENVIRONMENTS)
                    },
                    onOpenMissions = {
                        viewModel.playUiClick()
                        navController.navigate(RunvantaRoutes.MISSIONS)
                    },
                    onOpenAchievements = {
                        viewModel.playUiClick()
                        navController.navigate(RunvantaRoutes.ACHIEVEMENTS)
                    },
                    onOpenCustomRun = {
                        viewModel.playUiClick()
                        navController.navigate(RunvantaRoutes.CUSTOM_RUN)
                    },
                    onOpenSettings = {
                        viewModel.playUiClick()
                        navController.navigate(RunvantaRoutes.SETTINGS)
                    },
                    onWatchRewardedCoins = { act ->
                        viewModel.watchRewardedForCoins(act, 100)
                    }
                )
            }

            composable(RunvantaRoutes.PLAY) {
                GamePlayScreen(
                    state = state,
                    soundManager = viewModel.soundManager,
                    onRunCompleted = { summary -> viewModel.onRunCompleted(summary) },
                    onClearRunSummary = { viewModel.clearLastRunSummary() },
                    onWatchRewardedRevive = { act, onGranted ->
                        viewModel.watchRewardedForRevive(act, onGranted)
                    },
                    onNaturalBreakInterstitial = { act, onDone ->
                        viewModel.handleNaturalBreakInterstitial(act, onDone)
                    },
                    onExitToMenu = {
                        navController.popBackStack(RunvantaRoutes.HOME, inclusive = false)
                    }
                )
            }

            composable(RunvantaRoutes.CHARACTERS) {
                CharactersScreen(
                    state = state,
                    onSelectOrUnlock = { char -> viewModel.selectOrUnlockCharacter(char) },
                    onEditInStudio = { char ->
                        viewModel.playUiClick()
                        viewModel.startEditingCharacter(char)
                        navController.navigate(RunvantaRoutes.STUDIO)
                    },
                    onRenameCharacter = { char, newName -> viewModel.renameCharacter(char, newName) },
                    onDeleteCustom = { char -> viewModel.deleteCustomCharacter(char) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(RunvantaRoutes.STUDIO) {
                CharacterStudioScreen(
                    state = state,
                    editingCharacter = editingChar,
                    onLoadPhoto = { uri -> viewModel.loadPickedPhoto(uri) },
                    onAcceptPhotoNotice = { viewModel.acceptPhotoNotice() },
                    onUnlockCosmetic = { key, cost, label, onDone ->
                        viewModel.unlockCosmetic(key, cost, label, onDone)
                    },
                    onSaveCharacter = { char, bmp, onDone ->
                        viewModel.saveStudioCharacter(char, bmp, onDone)
                    },
                    onDeleteCharacter = { char -> viewModel.deleteCustomCharacter(char) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(RunvantaRoutes.ENVIRONMENTS) {
                EnvironmentsScreen(
                    state = state,
                    onSelectOrUnlockEnvironment = { env -> viewModel.selectOrUnlockEnvironment(env) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(RunvantaRoutes.MISSIONS) {
                MissionsScreen(
                    state = state,
                    onClaimMission = { m -> viewModel.claimMission(m) },
                    onWatchRewardedBonus = { act -> viewModel.watchRewardedForCoins(act, 100) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(RunvantaRoutes.ACHIEVEMENTS) {
                AchievementsScreen(
                    state = state,
                    onClaimAchievement = { a -> viewModel.claimAchievement(a) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(RunvantaRoutes.CUSTOM_RUN) {
                CustomRunScreen(
                    state = state,
                    onSaveAndPlayCustom = { cfg ->
                        viewModel.saveCustomRunConfig(cfg)
                        viewModel.selectGameMode(GameMode.CUSTOM)
                        navController.navigate(RunvantaRoutes.PLAY)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(RunvantaRoutes.SETTINGS) {
                SettingsScreen(
                    state = state,
                    onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                    onOpenPrivacyOptions = { act -> viewModel.adMobManager.showPrivacyOptionsForm(act) },
                    onResetProgress = { viewModel.resetAllProgress() },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 58.dp, start = 16.dp, end = 16.dp)
        )
    }
}
