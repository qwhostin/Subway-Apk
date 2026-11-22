package com.example.data.model

import android.app.ActivityManager
import android.content.Context
import kotlin.math.roundToInt

enum class GameMode(
    val displayName: String,
    val subtitle: String,
    val startSpeed: Float,
    val maxSpeed: Float,
    val speedRampPerSec: Float,
    val obstacleDensity: Float,
    val coinDensity: Float,
    val powerUpDensity: Float,
    val startingLives: Int,
    val scoreMultiplier: Float
) {
    CLASSIC(
        displayName = "Classic Run",
        subtitle = "Gradually rising arcade speed",
        startSpeed = 14f,
        maxSpeed = 34f,
        speedRampPerSec = 0.14f,
        obstacleDensity = 0.55f,
        coinDensity = 0.72f,
        powerUpDensity = 0.22f,
        startingLives = 1,
        scoreMultiplier = 1.0f
    ),
    EASY(
        displayName = "Easy Run",
        subtitle = "Relaxed pace & generous reaction time",
        startSpeed = 11f,
        maxSpeed = 23f,
        speedRampPerSec = 0.08f,
        obstacleDensity = 0.35f,
        coinDensity = 0.80f,
        powerUpDensity = 0.30f,
        startingLives = 2,
        scoreMultiplier = 0.8f
    ),
    NORMAL(
        displayName = "Normal Run",
        subtitle = "Balanced speed & obstacle flow",
        startSpeed = 15f,
        maxSpeed = 32f,
        speedRampPerSec = 0.13f,
        obstacleDensity = 0.52f,
        coinDensity = 0.70f,
        powerUpDensity = 0.24f,
        startingLives = 1,
        scoreMultiplier = 1.0f
    ),
    HARD(
        displayName = "Hard Run",
        subtitle = "High velocity & dense hazards",
        startSpeed = 19f,
        maxSpeed = 42f,
        speedRampPerSec = 0.18f,
        obstacleDensity = 0.74f,
        coinDensity = 0.65f,
        powerUpDensity = 0.20f,
        startingLives = 1,
        scoreMultiplier = 1.5f
    ),
    EXTREME(
        displayName = "Extreme Run",
        subtitle = "Maximum reflex challenge",
        startSpeed = 24f,
        maxSpeed = 52f,
        speedRampPerSec = 0.24f,
        obstacleDensity = 0.90f,
        coinDensity = 0.60f,
        powerUpDensity = 0.16f,
        startingLives = 1,
        scoreMultiplier = 2.0f
    ),
    CUSTOM(
        displayName = "Custom Run",
        subtitle = "Your custom speed, hazards & rules",
        startSpeed = 15f,
        maxSpeed = 35f,
        speedRampPerSec = 0.14f,
        obstacleDensity = 0.55f,
        coinDensity = 0.70f,
        powerUpDensity = 0.25f,
        startingLives = 1,
        scoreMultiplier = 1.0f
    )
}

data class CustomRunConfig(
    val startingSpeed: Float = 15f,
    val maxSpeed: Float = 36f,
    val difficultyLevel: Int = 2, // 1..5
    val obstacleFrequency: Float = 0.55f,
    val coinFrequency: Float = 0.70f,
    val powerUpFrequency: Float = 0.25f,
    val startingLives: Int = 2,
    val environmentId: String = "neon_city",
    val durationSeconds: Int = 0, // 0 = Unlimited
    val scoreMultiplier: Float = 1.0f,
    val soundEnabled: Boolean = true
) {
    fun clamped(): CustomRunConfig {
        val safeStart = startingSpeed.coerceIn(9f, 30f)
        val safeMax = maxSpeed.coerceIn(safeStart + 2f, 54f)
        return copy(
            startingSpeed = safeStart,
            maxSpeed = safeMax,
            difficultyLevel = difficultyLevel.coerceIn(1, 5),
            obstacleFrequency = obstacleFrequency.coerceIn(0.15f, 0.95f),
            coinFrequency = coinFrequency.coerceIn(0.15f, 1.0f),
            powerUpFrequency = powerUpFrequency.coerceIn(0.0f, 0.60f),
            startingLives = startingLives.coerceIn(1, 5),
            durationSeconds = if (durationSeconds <= 0) 0 else durationSeconds.coerceIn(30, 600),
            scoreMultiplier = scoreMultiplier.coerceIn(0.5f, 3.0f)
        )
    }
}

enum class SceneryStyle {
    NEON_TOWERS,
    TROPICAL_PALMS,
    DESERT_MESAS,
    ALPINE_PINES,
    SPACE_GANTRIES,
    CYBER_HOLOGRAMS,
    SNOW_CRYSTALS,
    ANCIENT_TEMPLES
}

data class EnvironmentTheme(
    val id: String,
    val name: String,
    val tagline: String,
    val unlockCoins: Int,
    val skyTopRgb: Long,
    val skyBottomRgb: Long,
    val fogRgb: Long,
    val trackDeckRgb: Long,
    val trackLaneRgb: Long,
    val railAccentRgb: Long,
    val obstaclePrimaryRgb: Long,
    val obstacleAccentRgb: Long,
    val sceneryPrimaryRgb: Long,
    val sceneryAccentRgb: Long,
    val sceneryStyle: SceneryStyle,
    val baseTempoBpm: Int
)

object EnvironmentsCatalog {
    val all: List<EnvironmentTheme> = listOf(
        EnvironmentTheme(
            id = "neon_city",
            name = "Neon City",
            tagline = "Electric skyline & glowing skyways",
            unlockCoins = 0,
            skyTopRgb = 0x0A0E24,
            skyBottomRgb = 0x1A1B4B,
            fogRgb = 0x161942,
            trackDeckRgb = 0x1B223C,
            trackLaneRgb = 0x263156,
            railAccentRgb = 0x00E5FF,
            obstaclePrimaryRgb = 0xFF3366,
            obstacleAccentRgb = 0x00E5FF,
            sceneryPrimaryRgb = 0x19234A,
            sceneryAccentRgb = 0x00E5FF,
            sceneryStyle = SceneryStyle.NEON_TOWERS,
            baseTempoBpm = 128
        ),
        EnvironmentTheme(
            id = "tropical_island",
            name = "Tropical Island",
            tagline = "Sunlit boardwalks & turquoise shores",
            unlockCoins = 250,
            skyTopRgb = 0x0B7EC8,
            skyBottomRgb = 0x6EE7F8,
            fogRgb = 0x5AD6EB,
            trackDeckRgb = 0xC89F65,
            trackLaneRgb = 0xDFB87C,
            railAccentRgb = 0x06D6A0,
            obstaclePrimaryRgb = 0xE76F51,
            obstacleAccentRgb = 0xFFD166,
            sceneryPrimaryRgb = 0x2A9D8F,
            sceneryAccentRgb = 0x1E6F5C,
            sceneryStyle = SceneryStyle.TROPICAL_PALMS,
            baseTempoBpm = 120
        ),
        EnvironmentTheme(
            id = "desert_highway",
            name = "Desert Highway",
            tagline = "Golden canyons & blazing asphalt",
            unlockCoins = 450,
            skyTopRgb = 0xA83B24,
            skyBottomRgb = 0xF4A261,
            fogRgb = 0xE78F52,
            trackDeckRgb = 0x3D3431,
            trackLaneRgb = 0x524642,
            railAccentRgb = 0xFFB703,
            obstaclePrimaryRgb = 0xD62828,
            obstacleAccentRgb = 0xFCBF49,
            sceneryPrimaryRgb = 0xBC6C25,
            sceneryAccentRgb = 0xDDA15E,
            sceneryStyle = SceneryStyle.DESERT_MESAS,
            baseTempoBpm = 126
        ),
        EnvironmentTheme(
            id = "mountain_valley",
            name = "Mountain Valley",
            tagline = "Emerald ridges & crisp alpine bridges",
            unlockCoins = 700,
            skyTopRgb = 0x264653,
            skyBottomRgb = 0x8ECAE6,
            fogRgb = 0x7AB5D1,
            trackDeckRgb = 0x4A5759,
            trackLaneRgb = 0x627274,
            railAccentRgb = 0x80ED99,
            obstaclePrimaryRgb = 0xE63946,
            obstacleAccentRgb = 0xF1FAEE,
            sceneryPrimaryRgb = 0x2D6A4F,
            sceneryAccentRgb = 0x52B788,
            sceneryStyle = SceneryStyle.ALPINE_PINES,
            baseTempoBpm = 122
        ),
        EnvironmentTheme(
            id = "futuristic_spaceport",
            name = "Futuristic Spaceport",
            tagline = "Orbital launchways & zero-G beacons",
            unlockCoins = 950,
            skyTopRgb = 0x050510,
            skyBottomRgb = 0x1E1038,
            fogRgb = 0x160D2C,
            trackDeckRgb = 0x252A3E,
            trackLaneRgb = 0x363D5A,
            railAccentRgb = 0x7B61FF,
            obstaclePrimaryRgb = 0xFF6B35,
            obstacleAccentRgb = 0x4CC9F0,
            sceneryPrimaryRgb = 0x3A3F58,
            sceneryAccentRgb = 0x9D4EDD,
            sceneryStyle = SceneryStyle.SPACE_GANTRIES,
            baseTempoBpm = 132
        ),
        EnvironmentTheme(
            id = "cyberpunk_district",
            name = "Cyberpunk District",
            tagline = "Hologram alleys & magenta overpasses",
            unlockCoins = 1200,
            skyTopRgb = 0x140621,
            skyBottomRgb = 0x3B0A45,
            fogRgb = 0x2C0838,
            trackDeckRgb = 0x181326,
            trackLaneRgb = 0x261E3B,
            railAccentRgb = 0xFF2A85,
            obstaclePrimaryRgb = 0x00F5D4,
            obstacleAccentRgb = 0xFF2A85,
            sceneryPrimaryRgb = 0x221738,
            sceneryAccentRgb = 0xF72585,
            sceneryStyle = SceneryStyle.CYBER_HOLOGRAMS,
            baseTempoBpm = 136
        ),
        EnvironmentTheme(
            id = "snow_adventure",
            name = "Snow Adventure",
            tagline = "Glacier tracks & aurora skies",
            unlockCoins = 1500,
            skyTopRgb = 0x0F2942,
            skyBottomRgb = 0x90E0EF,
            fogRgb = 0x77CBE0,
            trackDeckRgb = 0xCFE8F3,
            trackLaneRgb = 0xE6F4FA,
            railAccentRgb = 0x00B4D8,
            obstaclePrimaryRgb = 0xD90429,
            obstacleAccentRgb = 0x48CAE4,
            sceneryPrimaryRgb = 0xADE8F4,
            sceneryAccentRgb = 0x0096C7,
            sceneryStyle = SceneryStyle.SNOW_CRYSTALS,
            baseTempoBpm = 124
        ),
        EnvironmentTheme(
            id = "ancient_fantasy_city",
            name = "Ancient Fantasy City",
            tagline = "Gilded stoneways & emerald shrines",
            unlockCoins = 1800,
            skyTopRgb = 0x1E152A,
            skyBottomRgb = 0x6B4226,
            fogRgb = 0x523428,
            trackDeckRgb = 0x7A6855,
            trackLaneRgb = 0x948069,
            railAccentRgb = 0xFFD166,
            obstaclePrimaryRgb = 0xC9184A,
            obstacleAccentRgb = 0xFFD166,
            sceneryPrimaryRgb = 0x8D7B68,
            sceneryAccentRgb = 0x06D6A0,
            sceneryStyle = SceneryStyle.ANCIENT_TEMPLES,
            baseTempoBpm = 118
        )
    )

    fun byId(id: String): EnvironmentTheme = all.firstOrNull { it.id == id } ?: all.first()
}

enum class HairStyle(val label: String, val unlockCoins: Int) {
    SPIKY("Turbo Spikes", 0),
    SLEEK_BOB("Sleek Wave", 0),
    CYBER_MOHAWK("Neon Mohawk", 120),
    CURLY_AFRO("Cloud Curl", 0),
    PONYTAIL("Sprint Tail", 0),
    RUNNER_CAP("Street Cap", 150),
    ASTRO_HELMET("Orbital Crest", 200),
    ROYAL_CROWN("Champion Crown", 350)
}

enum class OutfitStyle(val label: String, val unlockCoins: Int) {
    RUNNER_SUIT("Aero Suit", 0),
    STREET_HOODIE("Street Hoodie", 0),
    CYBER_ARMOR("Cyber Jacket", 180),
    ASTRO_GEAR("Astro Rig", 0),
    ALPINE_PARKA("Frost Parka", 0),
    MYTH_TUNIC("Guardian Tunic", 240)
}

enum class ShoeStyle(val label: String, val unlockCoins: Int) {
    TURBO_SNEAKERS("Turbo Kicks", 0),
    HOVER_BOOTS("Hover Boots", 150),
    ROCKET_KICKS("Rocket Soles", 250),
    NEON_BLADES("Neon Blades", 300)
}

enum class AccessoryType(val label: String, val unlockCoins: Int) {
    NONE("None", 0),
    SUNGLASSES("Neon Shades", 0),
    HEADPHONES("Pulse Phones", 0),
    JETPACK("Mini Thruster", 250),
    SCARF("Speed Scarf", 140),
    HALO_RING("Energy Halo", 400)
}

enum class TrailEffect(val label: String, val unlockCoins: Int) {
    CYAN_SPARKS("Cyan Sparks", 0),
    CORAL_FLAME("Coral Flame", 150),
    GOLD_STARDUST("Gold Stardust", 220),
    PRISM_WAVE("Prism Pulse", 320)
}

data class CharacterAppearance(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val unlockCoins: Int = 0,
    val faceImagePath: String? = null,
    val faceScale: Float = 1.0f,
    val faceOffsetX: Float = 0.0f,
    val faceOffsetY: Float = 0.0f,
    val skinColorRgb: Long = 0xF1C27D,
    val hairStyle: HairStyle = HairStyle.SPIKY,
    val hairColorRgb: Long = 0x1D2538,
    val outfitStyle: OutfitStyle = OutfitStyle.RUNNER_SUIT,
    val outfitColorRgb: Long = 0x00E5FF,
    val accentColorRgb: Long = 0xFF4D6D,
    val shoeStyle: ShoeStyle = ShoeStyle.TURBO_SNEAKERS,
    val shoeColorRgb: Long = 0xFFFFFF,
    val accessory: AccessoryType = AccessoryType.NONE,
    val trailEffect: TrailEffect = TrailEffect.CYAN_SPARKS,
    val createdAt: Long = System.currentTimeMillis()
)

object DefaultCharactersCatalog {
    val all: List<CharacterAppearance> = listOf(
        CharacterAppearance(
            id = "char_kael",
            name = "Kael Volt",
            isDefault = true,
            unlockCoins = 0,
            skinColorRgb = 0xE8B48A,
            hairStyle = HairStyle.SPIKY,
            hairColorRgb = 0x0D1B2A,
            outfitStyle = OutfitStyle.RUNNER_SUIT,
            outfitColorRgb = 0x00B4D8,
            accentColorRgb = 0x00F5D4,
            shoeStyle = ShoeStyle.TURBO_SNEAKERS,
            shoeColorRgb = 0xF8F9FA,
            accessory = AccessoryType.SUNGLASSES,
            trailEffect = TrailEffect.CYAN_SPARKS
        ),
        CharacterAppearance(
            id = "char_zuri",
            name = "Zuri Blaze",
            isDefault = true,
            unlockCoins = 0,
            skinColorRgb = 0x8D5524,
            hairStyle = HairStyle.CURLY_AFRO,
            hairColorRgb = 0x2B193D,
            outfitStyle = OutfitStyle.STREET_HOODIE,
            outfitColorRgb = 0xFF4D6D,
            accentColorRgb = 0xFFD166,
            shoeStyle = ShoeStyle.TURBO_SNEAKERS,
            shoeColorRgb = 0xFFD166,
            accessory = AccessoryType.HEADPHONES,
            trailEffect = TrailEffect.CORAL_FLAME
        ),
        CharacterAppearance(
            id = "char_orion",
            name = "Orion Prime",
            isDefault = true,
            unlockCoins = 200,
            skinColorRgb = 0xF1C27D,
            hairStyle = HairStyle.ASTRO_HELMET,
            hairColorRgb = 0xE0E1DD,
            outfitStyle = OutfitStyle.ASTRO_GEAR,
            outfitColorRgb = 0xEDF2F4,
            accentColorRgb = 0xFF6B35,
            shoeStyle = ShoeStyle.HOVER_BOOTS,
            shoeColorRgb = 0xFF6B35,
            accessory = AccessoryType.JETPACK,
            trailEffect = TrailEffect.GOLD_STARDUST
        ),
        CharacterAppearance(
            id = "char_nyx",
            name = "Nyx Cipher",
            isDefault = true,
            unlockCoins = 350,
            skinColorRgb = 0xD2A679,
            hairStyle = HairStyle.CYBER_MOHAWK,
            hairColorRgb = 0xFF2A85,
            outfitStyle = OutfitStyle.CYBER_ARMOR,
            outfitColorRgb = 0x2B1E4A,
            accentColorRgb = 0xFF2A85,
            shoeStyle = ShoeStyle.NEON_BLADES,
            shoeColorRgb = 0x00F5D4,
            accessory = AccessoryType.SUNGLASSES,
            trailEffect = TrailEffect.PRISM_WAVE
        ),
        CharacterAppearance(
            id = "char_bram",
            name = "Bram Frost",
            isDefault = true,
            unlockCoins = 500,
            skinColorRgb = 0xFFDBAC,
            hairStyle = HairStyle.RUNNER_CAP,
            hairColorRgb = 0x264653,
            outfitStyle = OutfitStyle.ALPINE_PARKA,
            outfitColorRgb = 0x1D9BF0,
            accentColorRgb = 0xE0FBFC,
            shoeStyle = ShoeStyle.ROCKET_KICKS,
            shoeColorRgb = 0xE0FBFC,
            accessory = AccessoryType.SCARF,
            trailEffect = TrailEffect.CYAN_SPARKS
        ),
        CharacterAppearance(
            id = "char_aria",
            name = "Aria Myth",
            isDefault = true,
            unlockCoins = 750,
            skinColorRgb = 0xC68642,
            hairStyle = HairStyle.PONYTAIL,
            hairColorRgb = 0x3D1E10,
            outfitStyle = OutfitStyle.MYTH_TUNIC,
            outfitColorRgb = 0x06D6A0,
            accentColorRgb = 0xFFD166,
            shoeStyle = ShoeStyle.HOVER_BOOTS,
            shoeColorRgb = 0xFFD166,
            accessory = AccessoryType.HALO_RING,
            trailEffect = TrailEffect.GOLD_STARDUST
        )
    )
}

enum class PowerUpType(val label: String, val durationSec: Float, val colorRgb: Long) {
    MAGNET("Coin Magnet", 8.0f, 0xFF4D6D),
    DOUBLE_SCORE("2x Multiplier", 10.0f, 0xFFD166),
    SHIELD("Energy Shield", 12.0f, 0x00E5FF),
    TURBO_BOOST("Turbo Dash", 5.0f, 0x06D6A0)
}

enum class GraphicsQuality(val label: String, val sceneryDensity: Int, val shadowDetails: Boolean) {
    LOW("Low", 6, false),
    MEDIUM("Medium", 10, true),
    HIGH("High", 14, true),
    ULTRA("Ultra", 18, true)
}

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    ARABIC("ar", "العربية"),
    URDU("ur", "اردو"),
    FRENCH("fr", "Français")
}

data class GameSettings(
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val targetFps: Int = 60,
    val musicVolume: Float = 0.65f,
    val sfxVolume: Float = 0.85f,
    val vibrationEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val leftHandedControls: Boolean = false,
    val swipeSensitivity: Float = 1.0f,
    val cameraShakeEnabled: Boolean = true,
    val motionEffectsEnabled: Boolean = true,
    val performanceMode: Boolean = false,
    val batterySaverMode: Boolean = false
) {
    fun effectiveTargetFps(): Int = when {
        batterySaverMode -> 30
        performanceMode -> targetFps.coerceAtLeast(60)
        else -> targetFps
    }

    companion object {
        fun detectRecommendedQuality(context: Context): GraphicsQuality {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            val totalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            val cores = Runtime.getRuntime().availableProcessors()
            return when {
                totalGb >= 6.0 && cores >= 8 -> GraphicsQuality.ULTRA
                totalGb >= 3.5 && cores >= 6 -> GraphicsQuality.HIGH
                totalGb >= 2.0 -> GraphicsQuality.MEDIUM
                else -> GraphicsQuality.LOW
            }
        }
    }
}

enum class MissionPeriod { DAILY, WEEKLY }

data class MissionItem(
    val id: String,
    val title: String,
    val description: String,
    val period: MissionPeriod,
    val target: Int,
    val progress: Int,
    val rewardCoins: Int,
    val claimed: Boolean
) {
    val isCompleted: Boolean get() = progress >= target
    val progressFraction: Float get() = if (target <= 0) 0f else (progress.toFloat() / target).coerceIn(0f, 1f)
}

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val progress: Int,
    val rewardCoins: Int,
    val unlocked: Boolean,
    val claimed: Boolean
) {
    val progressFraction: Float get() = if (target <= 0) 0f else (progress.toFloat() / target).coerceIn(0f, 1f)
}

data class PlayerStats(
    val bestScore: Int = 0,
    val bestDistanceMeters: Int = 0,
    val totalDistanceMeters: Long = 0L,
    val totalCoinsCollected: Long = 0L,
    val totalRuns: Int = 0,
    val totalTimePlayedSec: Long = 0L,
    val totalObstaclesAvoided: Long = 0L,
    val customCharacterRuns: Int = 0,
    val hardRunsCompleted: Int = 0
)

data class RunHudState(
    val score: Int = 0,
    val distanceMeters: Int = 0,
    val coinsEarned: Int = 0,
    val multiplier: Float = 1.0f,
    val lives: Int = 1,
    val hasShield: Boolean = false,
    val speedKmh: Int = 50,
    val activePowerUps: Map<PowerUpType, Float> = emptyMap(),
    val nearMissBanner: String? = null,
    val remainingDurationSec: Int? = null,
    val obstaclesCleared: Int = 0,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val reviveUsed: Boolean = false,
    val endReason: String = ""
)

data class RunSummary(
    val score: Int,
    val distanceMeters: Int,
    val coinsEarned: Int,
    val obstaclesCleared: Int,
    val nearMissCount: Int,
    val durationSec: Int,
    val isNewPersonalBest: Boolean,
    val mode: GameMode,
    val environmentId: String,
    val usedCustomCharacter: Boolean
)

fun formatCoinDisplay(value: Int): String = "%,d".format(value)
fun formatDuration(totalSec: Long): String {
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) "${hrs}h ${mins}m" else if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

fun Float.round1(): Float = (this * 10f).roundToInt() / 10f
