package com.example.data.local

import android.content.Context
import com.example.data.model.AchievementItem
import com.example.data.model.CharacterAppearance
import com.example.data.model.CustomRunConfig
import com.example.data.model.DefaultCharactersCatalog
import com.example.data.model.GameMode
import com.example.data.model.GameSettings
import com.example.data.model.MissionItem
import com.example.data.model.MissionPeriod
import com.example.data.model.PlayerStats
import com.example.data.model.RunSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class MissionTemplate(
    val id: String,
    val title: String,
    val description: String,
    val period: MissionPeriod,
    val target: Int,
    val rewardCoins: Int
)

data class AchievementTemplate(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val rewardCoins: Int
)

class RunvantaRepository(
    private val context: Context,
    private val db: RunvantaDatabase,
    private val settingsStore: SettingsDataStore
) {
    private val characterDao = db.characterDao()
    private val progressionDao = db.progressionDao()

    val settingsFlow: Flow<GameSettings> = settingsStore.settingsFlow
    val coinsFlow: Flow<Int> = settingsStore.coinsFlow
    val selectedCharacterIdFlow: Flow<String> = settingsStore.selectedCharacterIdFlow
    val selectedEnvironmentIdFlow: Flow<String> = settingsStore.selectedEnvironmentIdFlow
    val selectedGameModeFlow: Flow<GameMode> = settingsStore.selectedGameModeFlow
    val unlockedIdsFlow: Flow<Set<String>> = settingsStore.unlockedIdsFlow
    val photoNoticeAcceptedFlow: Flow<Boolean> = settingsStore.photoNoticeAcceptedFlow
    val statsFlow: Flow<PlayerStats> = settingsStore.statsFlow
    val customRunConfigFlow: Flow<CustomRunConfig> = settingsStore.customRunConfigFlow

    val charactersFlow: Flow<List<CharacterAppearance>> = characterDao.observeAll().map { list ->
        list.map { it.toModel() }
    }

    val recentRunsFlow: Flow<List<RunRecordEntity>> = progressionDao.observeRecentRuns()

    private val missionTemplates = listOf(
        MissionTemplate(
            id = "daily_coins_100",
            title = "Coin Sprinter",
            description = "Collect 100 coins today",
            period = MissionPeriod.DAILY,
            target = 100,
            rewardCoins = 80
        ),
        MissionTemplate(
            id = "daily_dist_1000",
            title = "Daily Kilometer",
            description = "Run 1,000 meters in total today",
            period = MissionPeriod.DAILY,
            target = 1000,
            rewardCoins = 100
        ),
        MissionTemplate(
            id = "daily_runs_3",
            title = "Hat Trick",
            description = "Complete 3 runs today",
            period = MissionPeriod.DAILY,
            target = 3,
            rewardCoins = 90
        ),
        MissionTemplate(
            id = "daily_obstacles_20",
            title = "Reflex Master",
            description = "Clear 20 obstacles today",
            period = MissionPeriod.DAILY,
            target = 20,
            rewardCoins = 85
        ),
        MissionTemplate(
            id = "weekly_score_5000",
            title = "High Roller",
            description = "Reach a single-run score of 5,000",
            period = MissionPeriod.WEEKLY,
            target = 5000,
            rewardCoins = 220
        ),
        MissionTemplate(
            id = "weekly_dist_5000",
            title = "Marathon Runner",
            description = "Run 5,000 meters this week",
            period = MissionPeriod.WEEKLY,
            target = 5000,
            rewardCoins = 260
        ),
        MissionTemplate(
            id = "weekly_custom_run",
            title = "Star of the Show",
            description = "Complete 2 runs with a Character Studio hero",
            period = MissionPeriod.WEEKLY,
            target = 2,
            rewardCoins = 200
        )
    )

    private val achievementTemplates = listOf(
        AchievementTemplate(
            id = "ach_first_run",
            title = "First Stride",
            description = "Complete your first RUNVANTA run",
            target = 1,
            rewardCoins = 50
        ),
        AchievementTemplate(
            id = "ach_100_coins",
            title = "Vault Builder",
            description = "Collect 500 lifetime coins",
            target = 500,
            rewardCoins = 150
        ),
        AchievementTemplate(
            id = "ach_5k_score",
            title = "Arcade Legend",
            description = "Achieve a personal best score of 5,000",
            target = 5000,
            rewardCoins = 250
        ),
        AchievementTemplate(
            id = "ach_hard_mode",
            title = "Fearless Runner",
            description = "Complete a Hard or Extreme mode run",
            target = 1,
            rewardCoins = 200
        ),
        AchievementTemplate(
            id = "ach_custom_char",
            title = "Play As Anyone",
            description = "Run using your own Character Studio creation",
            target = 1,
            rewardCoins = 180
        ),
        AchievementTemplate(
            id = "ach_unlock_3_chars",
            title = "Squad Goals",
            description = "Unlock 3 original RUNVANTA characters",
            target = 3,
            rewardCoins = 300
        ),
        AchievementTemplate(
            id = "ach_10k_meters",
            title = "World Traveler",
            description = "Run 10,000 lifetime meters",
            target = 10000,
            rewardCoins = 400
        )
    )

    val missionsFlow: Flow<List<MissionItem>> = progressionDao.observeMissions().map { saved ->
        val dayKey = currentDayKey()
        val weekKey = currentWeekKey()
        val byId = saved.associateBy { it.id }
        missionTemplates.map { tpl ->
            val expectedKey = if (tpl.period == MissionPeriod.DAILY) dayKey else weekKey
            val row = byId[tpl.id]?.takeIf { it.cycleKey == expectedKey }
            MissionItem(
                id = tpl.id,
                title = tpl.title,
                description = tpl.description,
                period = tpl.period,
                target = tpl.target,
                progress = row?.progress ?: 0,
                rewardCoins = tpl.rewardCoins,
                claimed = row?.claimed ?: false
            )
        }
    }

    val achievementsFlow: Flow<List<AchievementItem>> = combine(
        progressionDao.observeAchievements(),
        statsFlow,
        unlockedIdsFlow
    ) { saved, stats, unlockedIds ->
        val byId = saved.associateBy { it.id }
        val unlockedCharsCount = unlockedIds.count { it.startsWith("char:") }
        achievementTemplates.map { tpl ->
            val computedProgress = when (tpl.id) {
                "ach_first_run" -> stats.totalRuns
                "ach_100_coins" -> stats.totalCoinsCollected.toInt()
                "ach_5k_score" -> stats.bestScore
                "ach_hard_mode" -> stats.hardRunsCompleted
                "ach_custom_char" -> stats.customCharacterRuns
                "ach_unlock_3_chars" -> unlockedCharsCount
                "ach_10k_meters" -> stats.totalDistanceMeters.toInt()
                else -> 0
            }
            val row = byId[tpl.id]
            val maxProgress = maxOf(computedProgress, row?.progress ?: 0).coerceAtMost(tpl.target)
            AchievementItem(
                id = tpl.id,
                title = tpl.title,
                description = tpl.description,
                target = tpl.target,
                progress = maxProgress,
                rewardCoins = tpl.rewardCoins,
                unlocked = (row?.unlocked == true) || maxProgress >= tpl.target,
                claimed = row?.claimed ?: false
            )
        }
    }

    suspend fun ensureSeeded() {
        val defaults = DefaultCharactersCatalog.all.map { CharacterEntity.fromModel(it) }
        characterDao.insertAllIgnore(defaults)
    }

    suspend fun saveCustomCharacter(character: CharacterAppearance) {
        characterDao.upsert(CharacterEntity.fromModel(character))
        settingsStore.setSelectedCharacter(character.id)
    }

    suspend fun deleteCustomCharacter(character: CharacterAppearance) {
        if (character.isDefault) return
        PhotoFaceProcessor.deleteFaceFile(character.faceImagePath)
        characterDao.deleteCustomById(character.id)
        if (settingsStore.selectedCharacterIdFlow.first() == character.id) {
            settingsStore.setSelectedCharacter("char_kael")
        }
    }

    suspend fun renameCharacter(character: CharacterAppearance, newName: String) {
        val trimmed = newName.trim().take(24)
        if (trimmed.isEmpty()) return
        characterDao.upsert(CharacterEntity.fromModel(character.copy(name = trimmed)))
    }

    suspend fun selectCharacter(id: String) {
        settingsStore.setSelectedCharacter(id)
    }

    suspend fun selectEnvironment(id: String) {
        settingsStore.setSelectedEnvironment(id)
    }

    suspend fun selectGameMode(mode: GameMode) {
        settingsStore.setSelectedGameMode(mode)
    }

    suspend fun saveCustomRunConfig(config: CustomRunConfig) {
        settingsStore.saveCustomRunConfig(config)
    }

    suspend fun unlockItemWithCoins(unlockKey: String, cost: Int): Boolean {
        return settingsStore.spendCoinsAndUnlock(cost, unlockKey)
    }

    suspend fun addBonusCoins(amount: Int) {
        settingsStore.addCoins(amount)
    }

    suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        settingsStore.updateSettings(transform)
    }

    suspend fun setPhotoNoticeAccepted(accepted: Boolean) {
        settingsStore.setPhotoNoticeAccepted(accepted)
    }

    suspend fun shouldShowInterstitialOnRunEnd(): Boolean {
        return settingsStore.shouldShowInterstitialOnRunEnd()
    }

    suspend fun recordCompletedRun(summary: RunSummary): Boolean {
        val isCustomRun = summary.mode == GameMode.CUSTOM
        val isHardOrExtreme = summary.mode == GameMode.HARD || summary.mode == GameMode.EXTREME
        val isNewBest = settingsStore.recordRunStats(
            score = summary.score,
            distanceMeters = summary.distanceMeters,
            coinsEarned = summary.coinsEarned,
            obstaclesCleared = summary.obstaclesCleared,
            durationSec = summary.durationSec,
            isCustomRun = isCustomRun,
            usedCustomCharacter = summary.usedCustomCharacter,
            isHardOrExtreme = isHardOrExtreme
        )

        progressionDao.insertRun(
            RunRecordEntity(
                score = summary.score,
                distanceMeters = summary.distanceMeters,
                coinsEarned = summary.coinsEarned,
                obstaclesCleared = summary.obstaclesCleared,
                durationSec = summary.durationSec,
                modeName = summary.mode.displayName,
                environmentId = summary.environmentId,
                usedCustomCharacter = summary.usedCustomCharacter
            )
        )

        updateMissionsForRun(summary)
        return isNewBest
    }

    private suspend fun updateMissionsForRun(summary: RunSummary) {
        val dayKey = currentDayKey()
        val weekKey = currentWeekKey()
        val existing = progressionDao.getAllMissions().associateBy { it.id }

        missionTemplates.forEach { tpl ->
            val cycleKey = if (tpl.period == MissionPeriod.DAILY) dayKey else weekKey
            val prev = existing[tpl.id]?.takeIf { it.cycleKey == cycleKey }
            val currentVal = prev?.progress ?: 0
            val nextVal = when (tpl.id) {
                "daily_coins_100" -> currentVal + summary.coinsEarned
                "daily_dist_1000" -> currentVal + summary.distanceMeters
                "daily_runs_3" -> currentVal + 1
                "daily_obstacles_20" -> currentVal + summary.obstaclesCleared
                "weekly_score_5000" -> maxOf(currentVal, summary.score)
                "weekly_dist_5000" -> currentVal + summary.distanceMeters
                "weekly_custom_run" -> currentVal + if (summary.usedCustomCharacter) 1 else 0
                else -> currentVal
            }.coerceAtMost(tpl.target)

            progressionDao.upsertMission(
                MissionProgressEntity(
                    id = tpl.id,
                    progress = nextVal,
                    claimed = prev?.claimed ?: false,
                    cycleKey = cycleKey
                )
            )
        }
    }

    suspend fun claimMissionReward(mission: MissionItem): Boolean {
        if (!mission.isCompleted || mission.claimed) return false
        val cycleKey = if (mission.period == MissionPeriod.DAILY) currentDayKey() else currentWeekKey()
        progressionDao.upsertMission(
            MissionProgressEntity(
                id = mission.id,
                progress = mission.progress,
                claimed = true,
                cycleKey = cycleKey
            )
        )
        settingsStore.addCoins(mission.rewardCoins)
        return true
    }

    suspend fun claimAchievementReward(achievement: AchievementItem): Boolean {
        if (!achievement.unlocked || achievement.claimed) return false
        progressionDao.upsertAchievement(
            AchievementProgressEntity(
                id = achievement.id,
                progress = achievement.progress,
                unlocked = true,
                claimed = true
            )
        )
        settingsStore.addCoins(achievement.rewardCoins)
        return true
    }

    suspend fun resetAllGameProgress() {
        PhotoFaceProcessor.deleteAllCustomFaces(context)
        characterDao.deleteAllCustom()
        progressionDao.clearMissions()
        progressionDao.clearAchievements()
        progressionDao.clearRuns()
        settingsStore.resetAllProgress()
        ensureSeeded()
    }

    private fun currentDayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun currentWeekKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }
}
