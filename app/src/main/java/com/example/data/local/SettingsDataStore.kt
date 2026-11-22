package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.CustomRunConfig
import com.example.data.model.GameMode
import com.example.data.model.GameSettings
import com.example.data.model.GraphicsQuality
import com.example.data.model.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.runvantaDataStore: DataStore<Preferences> by preferencesDataStore(name = "runvanta_prefs")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val TARGET_FPS = intPreferencesKey("target_fps")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val VIBRATION = booleanPreferencesKey("vibration")
        val LANGUAGE = stringPreferencesKey("language")
        val LEFT_HANDED = booleanPreferencesKey("left_handed")
        val SWIPE_SENSITIVITY = floatPreferencesKey("swipe_sensitivity")
        val CAMERA_SHAKE = booleanPreferencesKey("camera_shake")
        val MOTION_EFFECTS = booleanPreferencesKey("motion_effects")
        val PERFORMANCE_MODE = booleanPreferencesKey("performance_mode")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")

        val COINS_BALANCE = intPreferencesKey("coins_balance")
        val SELECTED_CHARACTER_ID = stringPreferencesKey("selected_character_id")
        val SELECTED_ENVIRONMENT_ID = stringPreferencesKey("selected_environment_id")
        val SELECTED_GAME_MODE = stringPreferencesKey("selected_game_mode")
        val UNLOCKED_IDS = stringSetPreferencesKey("unlocked_ids")

        val BEST_SCORE = intPreferencesKey("best_score")
        val BEST_DISTANCE = intPreferencesKey("best_distance")
        val TOTAL_DISTANCE = longPreferencesKey("total_distance")
        val TOTAL_COINS = longPreferencesKey("total_coins")
        val TOTAL_RUNS = intPreferencesKey("total_runs")
        val TOTAL_TIME_SEC = longPreferencesKey("total_time_sec")
        val TOTAL_OBSTACLES = longPreferencesKey("total_obstacles")
        val CUSTOM_CHAR_RUNS = intPreferencesKey("custom_char_runs")
        val HARD_RUNS_COMPLETED = intPreferencesKey("hard_runs_completed")

        val PHOTO_NOTICE_ACCEPTED = booleanPreferencesKey("photo_notice_accepted")
        val COMPLETED_RUNS_SINCE_AD = intPreferencesKey("completed_runs_since_ad")
        val LAST_INTERSTITIAL_MS = longPreferencesKey("last_interstitial_ms")
        val CUSTOM_RUN_JSON = stringPreferencesKey("custom_run_config")
    }

    val defaultUnlockedIds = setOf(
        "env:neon_city",
        "char:char_kael",
        "char:char_zuri",
        "hair:SPIKY", "hair:SLEEK_BOB", "hair:CURLY_AFRO", "hair:PONYTAIL",
        "outfit:RUNNER_SUIT", "outfit:STREET_HOODIE", "outfit:ASTRO_GEAR", "outfit:ALPINE_PARKA",
        "shoe:TURBO_SNEAKERS",
        "acc:NONE", "acc:SUNGLASSES", "acc:HEADPHONES",
        "trail:CYAN_SPARKS"
    )

    val settingsFlow: Flow<GameSettings> = context.runvantaDataStore.data.map { prefs ->
        val defaultQuality = GameSettings.detectRecommendedQuality(context)
        GameSettings(
            graphicsQuality = prefs[Keys.GRAPHICS_QUALITY]
                ?.let { runCatching { GraphicsQuality.valueOf(it) }.getOrNull() }
                ?: defaultQuality,
            targetFps = prefs[Keys.TARGET_FPS] ?: 60,
            musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.65f,
            sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.85f,
            vibrationEnabled = prefs[Keys.VIBRATION] ?: true,
            language = prefs[Keys.LANGUAGE]
                ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.ENGLISH,
            leftHandedControls = prefs[Keys.LEFT_HANDED] ?: false,
            swipeSensitivity = prefs[Keys.SWIPE_SENSITIVITY] ?: 1.0f,
            cameraShakeEnabled = prefs[Keys.CAMERA_SHAKE] ?: true,
            motionEffectsEnabled = prefs[Keys.MOTION_EFFECTS] ?: true,
            performanceMode = prefs[Keys.PERFORMANCE_MODE] ?: false,
            batterySaverMode = prefs[Keys.BATTERY_SAVER] ?: false
        )
    }

    val coinsFlow: Flow<Int> = context.runvantaDataStore.data.map { it[Keys.COINS_BALANCE] ?: 150 }
    val selectedCharacterIdFlow: Flow<String> = context.runvantaDataStore.data.map {
        it[Keys.SELECTED_CHARACTER_ID] ?: "char_kael"
    }
    val selectedEnvironmentIdFlow: Flow<String> = context.runvantaDataStore.data.map {
        it[Keys.SELECTED_ENVIRONMENT_ID] ?: "neon_city"
    }
    val selectedGameModeFlow: Flow<GameMode> = context.runvantaDataStore.data.map { prefs ->
        prefs[Keys.SELECTED_GAME_MODE]?.let { runCatching { GameMode.valueOf(it) }.getOrNull() }
            ?: GameMode.CLASSIC
    }
    val unlockedIdsFlow: Flow<Set<String>> = context.runvantaDataStore.data.map { prefs ->
        (prefs[Keys.UNLOCKED_IDS] ?: emptySet()) + defaultUnlockedIds
    }
    val photoNoticeAcceptedFlow: Flow<Boolean> = context.runvantaDataStore.data.map {
        it[Keys.PHOTO_NOTICE_ACCEPTED] ?: false
    }

    val statsFlow: Flow<PlayerStats> = context.runvantaDataStore.data.map { prefs ->
        PlayerStats(
            bestScore = prefs[Keys.BEST_SCORE] ?: 0,
            bestDistanceMeters = prefs[Keys.BEST_DISTANCE] ?: 0,
            totalDistanceMeters = prefs[Keys.TOTAL_DISTANCE] ?: 0L,
            totalCoinsCollected = prefs[Keys.TOTAL_COINS] ?: 0L,
            totalRuns = prefs[Keys.TOTAL_RUNS] ?: 0,
            totalTimePlayedSec = prefs[Keys.TOTAL_TIME_SEC] ?: 0L,
            totalObstaclesAvoided = prefs[Keys.TOTAL_OBSTACLES] ?: 0L,
            customCharacterRuns = prefs[Keys.CUSTOM_CHAR_RUNS] ?: 0,
            hardRunsCompleted = prefs[Keys.HARD_RUNS_COMPLETED] ?: 0
        )
    }

    val customRunConfigFlow: Flow<CustomRunConfig> = context.runvantaDataStore.data.map { prefs ->
        decodeCustomConfig(prefs[Keys.CUSTOM_RUN_JSON])
    }

    suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        context.runvantaDataStore.edit { prefs ->
            val current = GameSettings(
                graphicsQuality = prefs[Keys.GRAPHICS_QUALITY]
                    ?.let { runCatching { GraphicsQuality.valueOf(it) }.getOrNull() }
                    ?: GameSettings.detectRecommendedQuality(context),
                targetFps = prefs[Keys.TARGET_FPS] ?: 60,
                musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.65f,
                sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.85f,
                vibrationEnabled = prefs[Keys.VIBRATION] ?: true,
                language = prefs[Keys.LANGUAGE]
                    ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                    ?: AppLanguage.ENGLISH,
                leftHandedControls = prefs[Keys.LEFT_HANDED] ?: false,
                swipeSensitivity = prefs[Keys.SWIPE_SENSITIVITY] ?: 1.0f,
                cameraShakeEnabled = prefs[Keys.CAMERA_SHAKE] ?: true,
                motionEffectsEnabled = prefs[Keys.MOTION_EFFECTS] ?: true,
                performanceMode = prefs[Keys.PERFORMANCE_MODE] ?: false,
                batterySaverMode = prefs[Keys.BATTERY_SAVER] ?: false
            )
            val updated = transform(current)
            prefs[Keys.GRAPHICS_QUALITY] = updated.graphicsQuality.name
            prefs[Keys.TARGET_FPS] = updated.targetFps
            prefs[Keys.MUSIC_VOLUME] = updated.musicVolume.coerceIn(0f, 1f)
            prefs[Keys.SFX_VOLUME] = updated.sfxVolume.coerceIn(0f, 1f)
            prefs[Keys.VIBRATION] = updated.vibrationEnabled
            prefs[Keys.LANGUAGE] = updated.language.name
            prefs[Keys.LEFT_HANDED] = updated.leftHandedControls
            prefs[Keys.SWIPE_SENSITIVITY] = updated.swipeSensitivity.coerceIn(0.5f, 2.0f)
            prefs[Keys.CAMERA_SHAKE] = updated.cameraShakeEnabled
            prefs[Keys.MOTION_EFFECTS] = updated.motionEffectsEnabled
            prefs[Keys.PERFORMANCE_MODE] = updated.performanceMode
            prefs[Keys.BATTERY_SAVER] = updated.batterySaverMode
        }
    }

    suspend fun addCoins(amount: Int) {
        if (amount <= 0) return
        context.runvantaDataStore.edit { prefs ->
            val current = prefs[Keys.COINS_BALANCE] ?: 150
            prefs[Keys.COINS_BALANCE] = (current + amount).coerceAtLeast(0)
        }
    }

    suspend fun spendCoinsAndUnlock(cost: Int, unlockKey: String): Boolean {
        var success = false
        context.runvantaDataStore.edit { prefs ->
            val currentCoins = prefs[Keys.COINS_BALANCE] ?: 150
            val unlocked = (prefs[Keys.UNLOCKED_IDS] ?: emptySet()) + defaultUnlockedIds
            if (unlockKey in unlocked) {
                success = true
            } else if (currentCoins >= cost) {
                prefs[Keys.COINS_BALANCE] = currentCoins - cost
                prefs[Keys.UNLOCKED_IDS] = unlocked + unlockKey
                success = true
            }
        }
        return success
    }

    suspend fun setSelectedCharacter(id: String) {
        context.runvantaDataStore.edit { it[Keys.SELECTED_CHARACTER_ID] = id }
    }

    suspend fun setSelectedEnvironment(id: String) {
        context.runvantaDataStore.edit { it[Keys.SELECTED_ENVIRONMENT_ID] = id }
    }

    suspend fun setSelectedGameMode(mode: GameMode) {
        context.runvantaDataStore.edit { it[Keys.SELECTED_GAME_MODE] = mode.name }
    }

    suspend fun setPhotoNoticeAccepted(accepted: Boolean) {
        context.runvantaDataStore.edit { it[Keys.PHOTO_NOTICE_ACCEPTED] = accepted }
    }

    suspend fun saveCustomRunConfig(config: CustomRunConfig) {
        val safe = config.clamped()
        context.runvantaDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_RUN_JSON] = encodeCustomConfig(safe)
        }
    }

    suspend fun recordRunStats(
        score: Int,
        distanceMeters: Int,
        coinsEarned: Int,
        obstaclesCleared: Int,
        durationSec: Int,
        isCustomRun: Boolean,
        usedCustomCharacter: Boolean,
        isHardOrExtreme: Boolean
    ): Boolean {
        var isNewBest = false
        context.runvantaDataStore.edit { prefs ->
            val currentBest = prefs[Keys.BEST_SCORE] ?: 0
            if (!isCustomRun && score > currentBest) {
                prefs[Keys.BEST_SCORE] = score
                isNewBest = true
            }
            val currentBestDist = prefs[Keys.BEST_DISTANCE] ?: 0
            if (!isCustomRun && distanceMeters > currentBestDist) {
                prefs[Keys.BEST_DISTANCE] = distanceMeters
            }
            prefs[Keys.TOTAL_DISTANCE] = (prefs[Keys.TOTAL_DISTANCE] ?: 0L) + distanceMeters
            prefs[Keys.TOTAL_COINS] = (prefs[Keys.TOTAL_COINS] ?: 0L) + coinsEarned
            prefs[Keys.TOTAL_RUNS] = (prefs[Keys.TOTAL_RUNS] ?: 0) + 1
            prefs[Keys.TOTAL_TIME_SEC] = (prefs[Keys.TOTAL_TIME_SEC] ?: 0L) + durationSec
            prefs[Keys.TOTAL_OBSTACLES] = (prefs[Keys.TOTAL_OBSTACLES] ?: 0L) + obstaclesCleared
            if (usedCustomCharacter) {
                prefs[Keys.CUSTOM_CHAR_RUNS] = (prefs[Keys.CUSTOM_CHAR_RUNS] ?: 0) + 1
            }
            if (isHardOrExtreme) {
                prefs[Keys.HARD_RUNS_COMPLETED] = (prefs[Keys.HARD_RUNS_COMPLETED] ?: 0) + 1
            }
            prefs[Keys.COINS_BALANCE] = (prefs[Keys.COINS_BALANCE] ?: 150) + coinsEarned
        }
        return isNewBest
    }

    suspend fun shouldShowInterstitialOnRunEnd(): Boolean {
        var show = false
        val now = System.currentTimeMillis()
        context.runvantaDataStore.edit { prefs ->
            val count = (prefs[Keys.COMPLETED_RUNS_SINCE_AD] ?: 0) + 1
            val lastMs = prefs[Keys.LAST_INTERSTITIAL_MS] ?: 0L
            // Frequency cap: at least 3 completed runs AND at least 90 seconds between interstitials
            if (count >= 3 && (now - lastMs) >= 90_000L) {
                show = true
                prefs[Keys.COMPLETED_RUNS_SINCE_AD] = 0
                prefs[Keys.LAST_INTERSTITIAL_MS] = now
            } else {
                prefs[Keys.COMPLETED_RUNS_SINCE_AD] = count
            }
        }
        return show
    }

    suspend fun resetAllProgress() {
        context.runvantaDataStore.edit { prefs ->
            prefs[Keys.COINS_BALANCE] = 150
            prefs[Keys.SELECTED_CHARACTER_ID] = "char_kael"
            prefs[Keys.SELECTED_ENVIRONMENT_ID] = "neon_city"
            prefs[Keys.SELECTED_GAME_MODE] = GameMode.CLASSIC.name
            prefs[Keys.UNLOCKED_IDS] = defaultUnlockedIds
            prefs[Keys.BEST_SCORE] = 0
            prefs[Keys.BEST_DISTANCE] = 0
            prefs[Keys.TOTAL_DISTANCE] = 0L
            prefs[Keys.TOTAL_COINS] = 0L
            prefs[Keys.TOTAL_RUNS] = 0
            prefs[Keys.TOTAL_TIME_SEC] = 0L
            prefs[Keys.TOTAL_OBSTACLES] = 0L
            prefs[Keys.CUSTOM_CHAR_RUNS] = 0
            prefs[Keys.HARD_RUNS_COMPLETED] = 0
        }
    }

    private fun encodeCustomConfig(c: CustomRunConfig): String = listOf(
        c.startingSpeed, c.maxSpeed, c.difficultyLevel, c.obstacleFrequency,
        c.coinFrequency, c.powerUpFrequency, c.startingLives, c.environmentId,
        c.durationSeconds, c.scoreMultiplier, c.soundEnabled
    ).joinToString("|")

    private fun decodeCustomConfig(raw: String?): CustomRunConfig {
        if (raw.isNullOrBlank()) return CustomRunConfig()
        val p = raw.split("|")
        if (p.size < 11) return CustomRunConfig()
        return runCatching {
            CustomRunConfig(
                startingSpeed = p[0].toFloat(),
                maxSpeed = p[1].toFloat(),
                difficultyLevel = p[2].toInt(),
                obstacleFrequency = p[3].toFloat(),
                coinFrequency = p[4].toFloat(),
                powerUpFrequency = p[5].toFloat(),
                startingLives = p[6].toInt(),
                environmentId = p[7],
                durationSeconds = p[8].toInt(),
                scoreMultiplier = p[9].toFloat(),
                soundEnabled = p[10].toBoolean()
            ).clamped()
        }.getOrDefault(CustomRunConfig())
    }
}
