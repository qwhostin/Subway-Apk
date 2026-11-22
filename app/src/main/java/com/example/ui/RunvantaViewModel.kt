package com.example.ui

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RunvantaApp
import com.example.ads.AdConsentState
import com.example.audio.SoundEffect
import com.example.data.local.PhotoFaceProcessor
import com.example.data.local.RunRecordEntity
import com.example.data.model.AchievementItem
import com.example.data.model.CharacterAppearance
import com.example.data.model.CustomRunConfig
import com.example.data.model.DefaultCharactersCatalog
import com.example.data.model.EnvironmentTheme
import com.example.data.model.EnvironmentsCatalog
import com.example.data.model.GameMode
import com.example.data.model.GameSettings
import com.example.data.model.MissionItem
import com.example.data.model.PlayerStats
import com.example.data.model.RunSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RunvantaUiState(
    val characters: List<CharacterAppearance> = DefaultCharactersCatalog.all,
    val selectedCharacter: CharacterAppearance = DefaultCharactersCatalog.all.first(),
    val selectedEnvironment: EnvironmentTheme = EnvironmentsCatalog.all.first(),
    val selectedGameMode: GameMode = GameMode.CLASSIC,
    val customRunConfig: CustomRunConfig = CustomRunConfig(),
    val coins: Int = 150,
    val unlockedIds: Set<String> = emptySet(),
    val settings: GameSettings = GameSettings(),
    val stats: PlayerStats = PlayerStats(),
    val missions: List<MissionItem> = emptyList(),
    val achievements: List<AchievementItem> = emptyList(),
    val recentRuns: List<RunRecordEntity> = emptyList(),
    val photoNoticeAccepted: Boolean = false,
    val adConsent: AdConsentState = AdConsentState(),
    val lastRunSummary: RunSummary? = null,
    val statusMessage: String? = null
)

class RunvantaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RunvantaApp
    private val repo = app.repository
    val soundManager = app.soundManager
    val adMobManager = app.adMobManager

    private val _lastRunSummary = MutableStateFlow<RunSummary?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _editingCharacter = MutableStateFlow<CharacterAppearance?>(null)
    val editingCharacter: StateFlow<CharacterAppearance?> = _editingCharacter.asStateFlow()

    private val coreFlow = combine(
        repo.charactersFlow,
        repo.selectedCharacterIdFlow,
        repo.selectedEnvironmentIdFlow,
        repo.selectedGameModeFlow,
        repo.coinsFlow
    ) { chars, charId, envId, mode, coins ->
        CoreSelection(chars, charId, envId, mode, coins)
    }

    private val metaFlow = combine(
        repo.unlockedIdsFlow,
        repo.settingsFlow,
        repo.statsFlow,
        repo.customRunConfigFlow,
        repo.photoNoticeAcceptedFlow
    ) { unlocked, settings, stats, customCfg, notice ->
        soundManager.updateAudioConfig(settings.musicVolume, settings.sfxVolume, settings.vibrationEnabled)
        MetaState(unlocked, settings, stats, customCfg, notice)
    }

    private val progressFlow = combine(
        repo.missionsFlow,
        repo.achievementsFlow,
        repo.recentRunsFlow,
        adMobManager.adState
    ) { missions, achievements, runs, adState ->
        ProgressBundle(missions, achievements, runs, adState)
    }

    val uiState: StateFlow<RunvantaUiState> = combine(
        coreFlow,
        metaFlow,
        progressFlow,
        _lastRunSummary,
        _statusMessage
    ) { core, meta, prog, lastSummary, msg ->
        val list = core.chars.ifEmpty { DefaultCharactersCatalog.all }
        val selectedChar = list.firstOrNull { it.id == core.charId } ?: list.first()
        val selectedEnv = EnvironmentsCatalog.byId(core.envId)
        RunvantaUiState(
            characters = list,
            selectedCharacter = selectedChar,
            selectedEnvironment = selectedEnv,
            selectedGameMode = core.mode,
            customRunConfig = meta.customCfg,
            coins = core.coins,
            unlockedIds = meta.unlocked,
            settings = meta.settings,
            stats = meta.stats,
            missions = prog.missions,
            achievements = prog.achievements,
            recentRuns = prog.runs,
            photoNoticeAccepted = meta.notice,
            adConsent = prog.adState,
            lastRunSummary = lastSummary,
            statusMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RunvantaUiState()
    )

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun playUiClick() {
        soundManager.playSfx(SoundEffect.UI_CLICK)
    }

    fun initializeAds(activity: Activity) {
        adMobManager.requestConsentAndInitialize(activity)
    }

    fun selectGameMode(mode: GameMode) {
        playUiClick()
        viewModelScope.launch { repo.selectGameMode(mode) }
    }

    fun saveCustomRunConfig(config: CustomRunConfig) {
        viewModelScope.launch { repo.saveCustomRunConfig(config) }
    }

    fun selectOrUnlockEnvironment(env: EnvironmentTheme) {
        playUiClick()
        viewModelScope.launch {
            val key = "env:${env.id}"
            val unlocked = repo.unlockedIdsFlow.first()
            if (env.unlockCoins == 0 || key in unlocked) {
                repo.selectEnvironment(env.id)
                showMessage("${env.name} selected!")
            } else {
                val ok = repo.unlockItemWithCoins(key, env.unlockCoins)
                if (ok) {
                    repo.selectEnvironment(env.id)
                    soundManager.playSfx(SoundEffect.ACHIEVEMENT)
                    showMessage("Unlocked ${env.name}!")
                } else {
                    showMessage("Need ${env.unlockCoins} coins to unlock ${env.name}.")
                }
            }
        }
    }

    fun selectOrUnlockCharacter(char: CharacterAppearance) {
        playUiClick()
        viewModelScope.launch {
            val key = "char:${char.id}"
            val unlocked = repo.unlockedIdsFlow.first()
            if (!char.isDefault || char.unlockCoins == 0 || key in unlocked) {
                repo.selectCharacter(char.id)
                showMessage("${char.name} ready to run!")
            } else {
                val ok = repo.unlockItemWithCoins(key, char.unlockCoins)
                if (ok) {
                    repo.selectCharacter(char.id)
                    soundManager.playSfx(SoundEffect.ACHIEVEMENT)
                    showMessage("Unlocked ${char.name}!")
                } else {
                    showMessage("Need ${char.unlockCoins} coins to unlock ${char.name}.")
                }
            }
        }
    }

    fun unlockCosmetic(key: String, cost: Int, label: String, onUnlocked: () -> Unit) {
        viewModelScope.launch {
            val ok = repo.unlockItemWithCoins(key, cost)
            if (ok) {
                soundManager.playSfx(SoundEffect.ACHIEVEMENT)
                showMessage("Unlocked $label!")
                onUnlocked()
            } else {
                showMessage("Need $cost coins to unlock $label.")
            }
        }
    }

    fun startEditingCharacter(character: CharacterAppearance?) {
        _editingCharacter.value = character
    }

    suspend fun loadPickedPhoto(uri: Uri): Bitmap? {
        return PhotoFaceProcessor.loadPreviewBitmap(getApplication(), uri)
    }

    fun saveStudioCharacter(
        appearance: CharacterAppearance,
        pickedBitmap: Bitmap?,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val finalPath = if (pickedBitmap != null) {
                PhotoFaceProcessor.bakeFaceTexture(
                    context = getApplication(),
                    sourceBitmap = pickedBitmap,
                    faceScale = appearance.faceScale,
                    faceOffsetX = appearance.faceOffsetX,
                    faceOffsetY = appearance.faceOffsetY,
                    skinColorRgb = appearance.skinColorRgb,
                    existingPathToReplace = appearance.faceImagePath
                )
            } else {
                appearance.faceImagePath
            }
            val updated = appearance.copy(
                name = appearance.name.trim().ifEmpty { "Custom Runner" },
                faceImagePath = finalPath,
                isDefault = false
            )
            repo.saveCustomCharacter(updated)
            soundManager.playSfx(SoundEffect.ACHIEVEMENT)
            showMessage("Saved ${updated.name}!")
            _editingCharacter.value = null
            onSaved()
        }
    }

    fun deleteCustomCharacter(character: CharacterAppearance) {
        playUiClick()
        viewModelScope.launch {
            repo.deleteCustomCharacter(character)
            showMessage("Deleted ${character.name} and local photo.")
        }
    }

    fun renameCharacter(character: CharacterAppearance, newName: String) {
        playUiClick()
        viewModelScope.launch {
            repo.renameCharacter(character, newName)
            showMessage("Renamed to $newName")
        }
    }

    fun acceptPhotoNotice() {
        viewModelScope.launch { repo.setPhotoNoticeAccepted(true) }
    }

    fun claimMission(mission: MissionItem) {
        viewModelScope.launch {
            if (repo.claimMissionReward(mission)) {
                soundManager.playSfx(SoundEffect.ACHIEVEMENT)
                showMessage("+${mission.rewardCoins} coins claimed!")
            }
        }
    }

    fun claimAchievement(achievement: AchievementItem) {
        viewModelScope.launch {
            if (repo.claimAchievementReward(achievement)) {
                soundManager.playSfx(SoundEffect.ACHIEVEMENT)
                showMessage("+${achievement.rewardCoins} coins claimed!")
            }
        }
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        viewModelScope.launch { repo.updateSettings(transform) }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            repo.resetAllGameProgress()
            showMessage("All local game progress has been reset.")
        }
    }

    fun onRunCompleted(summary: RunSummary) {
        viewModelScope.launch {
            val isBest = repo.recordCompletedRun(summary)
            _lastRunSummary.value = summary.copy(isNewPersonalBest = isBest)
        }
    }

    fun clearLastRunSummary() {
        _lastRunSummary.value = null
    }

    fun handleNaturalBreakInterstitial(activity: Activity?, onContinue: () -> Unit) {
        viewModelScope.launch {
            val shouldShow = repo.shouldShowInterstitialOnRunEnd()
            if (shouldShow) {
                adMobManager.showInterstitialIfReady(activity, onContinue)
            } else {
                onContinue()
            }
        }
    }

    fun watchRewardedForCoins(activity: Activity?, bonusCoins: Int = 100) {
        adMobManager.showRewardedAd(
            activity = activity,
            onRewardEarned = {
                viewModelScope.launch {
                    repo.addBonusCoins(bonusCoins)
                    soundManager.playSfx(SoundEffect.ACHIEVEMENT)
                    showMessage("Earned +$bonusCoins bonus coins!")
                }
            },
            onUnavailableOrCancelled = { reason ->
                showMessage(reason)
            }
        )
    }

    fun watchRewardedForRevive(activity: Activity?, onReviveGranted: () -> Unit) {
        adMobManager.showRewardedAd(
            activity = activity,
            onRewardEarned = {
                soundManager.playSfx(SoundEffect.POWER_UP)
                onReviveGranted()
            },
            onUnavailableOrCancelled = { reason ->
                showMessage(reason)
            }
        )
    }

    private data class CoreSelection(
        val chars: List<CharacterAppearance>,
        val charId: String,
        val envId: String,
        val mode: GameMode,
        val coins: Int
    )

    private data class MetaState(
        val unlocked: Set<String>,
        val settings: GameSettings,
        val stats: PlayerStats,
        val customCfg: CustomRunConfig,
        val notice: Boolean
    )

    private data class ProgressBundle(
        val missions: List<MissionItem>,
        val achievements: List<AchievementItem>,
        val runs: List<RunRecordEntity>,
        val adState: AdConsentState
    )
}
