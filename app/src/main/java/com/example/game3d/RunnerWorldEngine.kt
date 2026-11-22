package com.example.game3d

import com.example.audio.SoundEffect
import com.example.data.model.CustomRunConfig
import com.example.data.model.EnvironmentTheme
import com.example.data.model.EnvironmentsCatalog
import com.example.data.model.GameMode
import com.example.data.model.PowerUpType
import com.example.data.model.RunHudState
import com.example.data.model.RunSummary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

enum class ObstacleKind {
    LOW_BARRIER,   // Jump over
    HIGH_ARCH,     // Slide under
    FULL_BLOCK,    // Dodge left/right
    MOVING_DRONE,  // Moves horizontally across lanes
    TRACK_GAP      // Broken lane section - jump or switch lane
}

class TrackSegment {
    var zStart: Float = 0f
    var gapLane: Int = -1 // -1 means intact across all 3 lanes
}

class ObstacleInstance {
    var active: Boolean = false
    var kind: ObstacleKind = ObstacleKind.LOW_BARRIER
    var lane: Int = 1 // 0, 1, 2
    var worldX: Float = 0f
    var worldZ: Float = 0f
    var moveDir: Float = 1f
    var passedChecked: Boolean = false
    var nearMissAwarded: Boolean = false
}

class CoinInstance {
    var active: Boolean = false
    var lane: Int = 1
    var worldX: Float = 0f
    var worldY: Float = 0.8f
    var worldZ: Float = 0f
}

class PowerUpInstance {
    var active: Boolean = false
    var type: PowerUpType = PowerUpType.MAGNET
    var worldX: Float = 0f
    var worldY: Float = 1.0f
    var worldZ: Float = 0f
}

class SceneryPropInstance {
    var z: Float = 0f
    var side: Int = -1 // -1 left, +1 right
    var height: Float = 6f
    var width: Float = 2f
    var variant: Int = 0
}

/**
 * High-performance object-pooled 3D runner simulation engine.
 * Coordinates lanes, jumping, sliding, procedural track chunks, obstacles, coins, power-ups,
 * near-miss bonuses, lives/shield, and game modes with zero per-frame allocations.
 */
class RunnerWorldEngine(
    private val onSound: (SoundEffect) -> Unit,
    private val onVibrate: (Boolean) -> Unit,
    private val onHudChanged: (RunHudState) -> Unit,
    private val onGameOver: (RunSummary) -> Unit
) {
    companion object {
        const val LANE_SPACING = 2.35f
        const val SEGMENT_LENGTH = 6.0f
        const val SEGMENT_COUNT = 20
        const val GRAVITY = -34.0f
        const val JUMP_VELOCITY = 13.6f
        const val SLIDE_DURATION = 0.72f
    }

    // Object Pools
    val segments = Array(SEGMENT_COUNT) { TrackSegment() }
    val obstacles = Array(32) { ObstacleInstance() }
    val coins = Array(64) { CoinInstance() }
    val powerUps = Array(8) { PowerUpInstance() }
    val sceneryProps = Array(32) { SceneryPropInstance() }

    private val random = Random(System.currentTimeMillis())

    // Run Configuration
    var activeMode: GameMode = GameMode.CLASSIC
        private set
    var customConfig: CustomRunConfig = CustomRunConfig()
        private set
    var environment: EnvironmentTheme = EnvironmentsCatalog.all.first()
        private set
    var usedCustomCharacter: Boolean = false
        private set

    // Player Kinematics
    var playerLane: Int = 1
        private set
    var playerX: Float = 0f
        private set
    var playerY: Float = 0f
        private set
    var playerZ: Float = 0f
        private set
    var velocityY: Float = 0f
        private set
    var isGrounded: Boolean = true
        private set
    var slideTimer: Float = 0f
        private set
    var leanDeg: Float = 0f
        private set
    var animPhase: Float = 0f
        private set
    var currentPose: CharacterPose = CharacterPose.RUNNING
        private set
    var cameraShakeAmount: Float = 0f
        private set

    // Run State
    private var currentSpeed: Float = 14f
    private var maxSpeed: Float = 34f
    private var speedRamp: Float = 0.14f
    private var obstacleDensity: Float = 0.55f
    private var coinDensity: Float = 0.72f
    private var powerUpDensity: Float = 0.22f
    private var baseScoreMultiplier: Float = 1.0f
    private var durationLimitSec: Int = 0
    private var runElapsedSec: Float = 0f

    private var scoreAccum: Float = 0f
    private var coinsCollected: Int = 0
    private var obstaclesCleared: Int = 0
    private var nearMissCount: Int = 0
    private var lives: Int = 1
    private var invulnTimer: Float = 0f
    private var reviveUsed: Boolean = false
    private var isPaused: Boolean = false
    private var isGameOver: Boolean = false
    private var endReason: String = ""

    private var nextSpawnZ: Float = 18f
    private var hudEmitAccum: Float = 0f
    private var nearMissBanner: String? = null
    private var nearMissBannerTimer: Float = 0f
    private var footstepAccum: Float = 0f

    private val activePowerUps = mutableMapOf<PowerUpType, Float>()

    @Synchronized
    fun startNewRun(
        mode: GameMode,
        env: EnvironmentTheme,
        custom: CustomRunConfig,
        isCustomChar: Boolean
    ) {
        activeMode = mode
        environment = env
        customConfig = custom.clamped()
        usedCustomCharacter = isCustomChar

        if (mode == GameMode.CUSTOM) {
            currentSpeed = customConfig.startingSpeed
            maxSpeed = customConfig.maxSpeed
            speedRamp = 0.08f + customConfig.difficultyLevel * 0.04f
            obstacleDensity = customConfig.obstacleFrequency
            coinDensity = customConfig.coinFrequency
            powerUpDensity = customConfig.powerUpFrequency
            lives = customConfig.startingLives
            baseScoreMultiplier = customConfig.scoreMultiplier
            durationLimitSec = customConfig.durationSeconds
        } else {
            currentSpeed = mode.startSpeed
            maxSpeed = mode.maxSpeed
            speedRamp = mode.speedRampPerSec
            obstacleDensity = mode.obstacleDensity
            coinDensity = mode.coinDensity
            powerUpDensity = mode.powerUpDensity
            lives = mode.startingLives
            baseScoreMultiplier = mode.scoreMultiplier
            durationLimitSec = 0
        }

        playerLane = 1
        playerX = 0f
        playerY = 0f
        playerZ = 0f
        velocityY = 0f
        isGrounded = true
        slideTimer = 0f
        leanDeg = 0f
        animPhase = 0f
        currentPose = CharacterPose.RUNNING
        cameraShakeAmount = 0f

        runElapsedSec = 0f
        scoreAccum = 0f
        coinsCollected = 0
        obstaclesCleared = 0
        nearMissCount = 0
        invulnTimer = 0f
        reviveUsed = false
        isPaused = false
        isGameOver = false
        endReason = ""
        nearMissBanner = null
        nearMissBannerTimer = 0f
        activePowerUps.clear()

        for (i in 0 until SEGMENT_COUNT) {
            segments[i].zStart = (i - 2) * SEGMENT_LENGTH
            segments[i].gapLane = -1
        }
        obstacles.forEach { it.active = false }
        coins.forEach { it.active = false }
        powerUps.forEach { it.active = false }

        for (i in sceneryProps.indices) {
            val side = if (i % 2 == 0) -1 else 1
            sceneryProps[i].apply {
                z = (i / 2) * 7.5f - 6f
                this.side = side
                height = 4.5f + random.nextFloat() * 7.5f
                width = 1.6f + random.nextFloat() * 1.4f
                variant = random.nextInt(3)
            }
        }

        nextSpawnZ = 24f
        while (nextSpawnZ < 102f) {
            spawnPatternAt(nextSpawnZ)
            nextSpawnZ += patternSpacing()
        }

        emitHud()
    }

    @Synchronized
    fun moveLeft() {
        if (isPaused || isGameOver) return
        if (playerLane > 0) {
            playerLane--
            onSound(SoundEffect.UI_CLICK)
        }
    }

    @Synchronized
    fun moveRight() {
        if (isPaused || isGameOver) return
        if (playerLane < 2) {
            playerLane++
            onSound(SoundEffect.UI_CLICK)
        }
    }

    @Synchronized
    fun jump() {
        if (isPaused || isGameOver) return
        if (isGrounded) {
            isGrounded = false
            velocityY = JUMP_VELOCITY
            slideTimer = 0f
            currentPose = CharacterPose.JUMPING
            onSound(SoundEffect.JUMP)
        }
    }

    @Synchronized
    fun slide() {
        if (isPaused || isGameOver) return
        if (!isGrounded) {
            // Fast-fall slam into slide
            velocityY = -22f
        }
        slideTimer = SLIDE_DURATION
        currentPose = CharacterPose.SLIDING
        onSound(SoundEffect.SLIDE)
    }

    @Synchronized
    fun setPaused(paused: Boolean) {
        if (isGameOver) return
        isPaused = paused
        emitHud()
    }

    @Synchronized
    fun reviveAfterAd() {
        if (!isGameOver || reviveUsed) return
        reviveUsed = true
        isGameOver = false
        isPaused = false
        lives = 1
        invulnTimer = 2.6f
        playerY = 0f
        velocityY = 0f
        isGrounded = true
        slideTimer = 0f
        currentPose = CharacterPose.RUNNING
        endReason = ""

        // Clear obstacles immediately around player for fair recovery
        obstacles.forEach { obs ->
            if (obs.active && obs.worldZ in (playerZ - 4f)..(playerZ + 18f)) {
                obs.active = false
            }
        }
        emitHud()
    }

    @Synchronized
    fun update(dtRaw: Float) {
        if (isPaused || isGameOver) return
        val dt = dtRaw.coerceIn(0.001f, 0.05f)

        runElapsedSec += dt
        if (durationLimitSec > 0 && runElapsedSec >= durationLimitSec) {
            finishRun(victory = true, reason = "Run Time Complete!")
            return
        }

        // Power-up timers
        val it = activePowerUps.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val rem = entry.value - dt
            if (rem <= 0f) it.remove() else entry.setValue(rem)
        }
        if (invulnTimer > 0f) invulnTimer = (invulnTimer - dt).coerceAtLeast(0f)
        if (cameraShakeAmount > 0f) cameraShakeAmount = (cameraShakeAmount - dt * 2.4f).coerceAtLeast(0f)
        if (nearMissBannerTimer > 0f) {
            nearMissBannerTimer -= dt
            if (nearMissBannerTimer <= 0f) nearMissBanner = null
        }

        val hasTurbo = activePowerUps.containsKey(PowerUpType.TURBO_BOOST)
        val hasMagnet = activePowerUps.containsKey(PowerUpType.MAGNET)
        val hasDouble = activePowerUps.containsKey(PowerUpType.DOUBLE_SCORE)
        val hasShield = activePowerUps.containsKey(PowerUpType.SHIELD)

        // Speed & forward movement
        currentSpeed = (currentSpeed + speedRamp * dt).coerceAtMost(maxSpeed)
        val effectiveSpeed = if (hasTurbo) currentSpeed * 1.45f else currentSpeed
        val dz = effectiveSpeed * dt
        playerZ += dz

        // Score accumulation
        val mult = baseScoreMultiplier * (if (hasDouble) 2f else 1f) * (if (hasTurbo) 1.5f else 1f)
        scoreAccum += dz * 2.2f * mult

        // Lateral lane spring interpolation
        val targetX = laneToX(playerLane)
        val dx = targetX - playerX
        playerX += dx * (dt * 15f).coerceAtMost(1f)
        leanDeg = (dx * -10f).coerceIn(-22f, 22f)

        // Vertical jump physics
        if (!isGrounded) {
            velocityY += GRAVITY * dt
            playerY += velocityY * dt
            if (playerY <= 0f) {
                playerY = 0f
                velocityY = 0f
                isGrounded = true
                onSound(SoundEffect.LAND)
            }
        }

        // Slide timer & pose
        if (slideTimer > 0f) {
            slideTimer = (slideTimer - dt).coerceAtLeast(0f)
        }
        currentPose = when {
            !isGrounded -> CharacterPose.JUMPING
            slideTimer > 0f -> CharacterPose.SLIDING
            else -> CharacterPose.RUNNING
        }
        animPhase += dt * (effectiveSpeed * 0.72f)

        if (isGrounded && slideTimer <= 0f) {
            footstepAccum += dt * (effectiveSpeed / 12f)
            if (footstepAccum >= 1f) {
                footstepAccum = 0f
                onSound(SoundEffect.FOOTSTEP)
            }
        }

        // Recycle Track Segments
        for (seg in segments) {
            if (seg.zStart + SEGMENT_LENGTH < playerZ - 12f) {
                val maxZ = segments.maxOf { it.zStart }
                seg.zStart = maxZ + SEGMENT_LENGTH
                seg.gapLane = -1
            }
        }

        // Recycle Scenery Props
        for (prop in sceneryProps) {
            if (prop.z < playerZ - 14f) {
                val maxPropZ = sceneryProps.maxOf { it.z }
                prop.z = maxPropZ + 7.5f
                prop.height = 4.5f + random.nextFloat() * 8f
                prop.variant = random.nextInt(3)
            }
        }

        // Procedural Pattern Spawning ahead
        while (nextSpawnZ < playerZ + 105f) {
            spawnPatternAt(nextSpawnZ)
            nextSpawnZ += patternSpacing()
        }

        // Update Coins & Magnet attraction
        for (coin in coins) {
            if (!coin.active) continue
            if (coin.worldZ < playerZ - 6f) {
                coin.active = false
                continue
            }
            if (hasMagnet && abs(coin.worldZ - playerZ) < 11.5f) {
                val tx = playerX
                val ty = playerY + 0.9f
                coin.worldX += (tx - coin.worldX) * dt * 10f
                coin.worldY += (ty - coin.worldY) * dt * 10f
                coin.worldZ += (playerZ - coin.worldZ) * dt * 5f
            }
            val distX = abs(coin.worldX - playerX)
            val distY = abs(coin.worldY - (playerY + (if (slideTimer > 0f) 0.45f else 0.95f)))
            val distZ = abs(coin.worldZ - playerZ)
            if (distX < 0.95f && distY < 1.15f && distZ < 1.05f) {
                coin.active = false
                coinsCollected += if (hasDouble) 2 else 1
                scoreAccum += 25f * mult
                onSound(SoundEffect.COIN)
            }
        }

        // Update Power-Ups
        for (pu in powerUps) {
            if (!pu.active) continue
            if (pu.worldZ < playerZ - 6f) {
                pu.active = false
                continue
            }
            val distX = abs(pu.worldX - playerX)
            val distY = abs(pu.worldY - (playerY + 0.9f))
            val distZ = abs(pu.worldZ - playerZ)
            if (distX < 1.05f && distY < 1.3f && distZ < 1.15f) {
                pu.active = false
                activePowerUps[pu.type] = pu.type.durationSec
                scoreAccum += 75f * mult
                onSound(SoundEffect.POWER_UP)
                onVibrate(false)
            }
        }

        // Update Obstacles & Collisions
        val playerFeetY = playerY
        val playerHeadY = playerY + (if (slideTimer > 0f) 0.78f else 1.72f)
        for (obs in obstacles) {
            if (!obs.active) continue
            if (obs.worldZ < playerZ - 8f) {
                obs.active = false
                continue
            }
            if (obs.kind == ObstacleKind.MOVING_DRONE) {
                obs.worldX += obs.moveDir * dt * 2.8f
                val limit = LANE_SPACING * 1.05f
                if (obs.worldX > limit) {
                    obs.worldX = limit
                    obs.moveDir = -1f
                } else if (obs.worldX < -limit) {
                    obs.worldX = -limit
                    obs.moveDir = 1f
                }
            }

            // Count cleared obstacles & Near Misses
            if (!obs.passedChecked && obs.worldZ < playerZ - 0.6f) {
                obs.passedChecked = true
                obstaclesCleared++
            }

            val dzObs = abs(obs.worldZ - playerZ)
            val dxObs = abs(obs.worldX - playerX)

            if (dzObs < 0.72f && dxObs < 0.78f) {
                val hit = when (obs.kind) {
                    ObstacleKind.LOW_BARRIER -> playerFeetY < 0.92f
                    ObstacleKind.HIGH_ARCH -> playerHeadY > 0.92f
                    ObstacleKind.FULL_BLOCK -> playerFeetY < 2.15f
                    ObstacleKind.MOVING_DRONE -> playerFeetY < 1.55f
                    ObstacleKind.TRACK_GAP -> playerFeetY < 0.35f
                }
                if (hit) {
                    if (hasTurbo || invulnTimer > 0f) {
                        obs.active = false
                        scoreAccum += 60f * mult
                        cameraShakeAmount = 0.25f
                    } else if (hasShield) {
                        activePowerUps.remove(PowerUpType.SHIELD)
                        obs.active = false
                        invulnTimer = 1.4f
                        cameraShakeAmount = 0.55f
                        onSound(SoundEffect.COLLISION)
                        onVibrate(true)
                    } else {
                        obs.active = false
                        lives--
                        cameraShakeAmount = 0.85f
                        onSound(SoundEffect.COLLISION)
                        onVibrate(true)
                        if (lives <= 0) {
                            finishRun(victory = false, reason = obstacleHitLabel(obs.kind))
                            return
                        } else {
                            invulnTimer = 1.8f
                        }
                    }
                }
            } else if (!obs.nearMissAwarded && dzObs < 0.95f && dxObs in 0.80f..1.45f) {
                obs.nearMissAwarded = true
                nearMissCount++
                scoreAccum += 50f * mult
                nearMissBanner = "NEAR MISS +${(50f * mult).toInt()}"
                nearMissBannerTimer = 1.1f
                onSound(SoundEffect.NEAR_MISS)
            }
        }

        hudEmitAccum += dt
        if (hudEmitAccum >= 0.06f) {
            hudEmitAccum = 0f
            emitHud()
        }
    }

    private fun finishRun(victory: Boolean, reason: String) {
        isGameOver = true
        endReason = reason
        currentPose = if (victory) CharacterPose.VICTORY else CharacterPose.FALLING
        onSound(if (victory) SoundEffect.ACHIEVEMENT else SoundEffect.GAME_OVER)
        emitHud()

        val summary = RunSummary(
            score = scoreAccum.toInt(),
            distanceMeters = playerZ.toInt().coerceAtLeast(0),
            coinsEarned = coinsCollected,
            obstaclesCleared = obstaclesCleared,
            nearMissCount = nearMissCount,
            durationSec = runElapsedSec.toInt(),
            isNewPersonalBest = false,
            mode = activeMode,
            environmentId = environment.id,
            usedCustomCharacter = usedCustomCharacter
        )
        onGameOver(summary)
    }

    private fun emitHud() {
        val remSec = if (durationLimitSec > 0) {
            (durationLimitSec - runElapsedSec.toInt()).coerceAtLeast(0)
        } else null

        val hasDouble = activePowerUps.containsKey(PowerUpType.DOUBLE_SCORE)
        val hasTurbo = activePowerUps.containsKey(PowerUpType.TURBO_BOOST)
        val effMult = baseScoreMultiplier * (if (hasDouble) 2f else 1f) * (if (hasTurbo) 1.5f else 1f)

        onHudChanged(
            RunHudState(
                score = scoreAccum.toInt(),
                distanceMeters = playerZ.toInt().coerceAtLeast(0),
                coinsEarned = coinsCollected,
                multiplier = effMult,
                lives = lives.coerceAtLeast(0),
                hasShield = activePowerUps.containsKey(PowerUpType.SHIELD),
                speedKmh = (currentSpeed * 3.6f).toInt(),
                activePowerUps = activePowerUps.toMap(),
                nearMissBanner = nearMissBanner,
                remainingDurationSec = remSec,
                obstaclesCleared = obstaclesCleared,
                isPaused = isPaused,
                isGameOver = isGameOver,
                reviveUsed = reviveUsed,
                endReason = endReason
            )
        )
    }

    private fun patternSpacing(): Float {
        val base = 17.5f - (obstacleDensity * 6.5f)
        return (base + random.nextFloat() * 4.5f).coerceIn(10.5f, 22f)
    }

    private fun spawnPatternAt(z: Float) {
        // Choose 1 or 2 blocked lanes, ALWAYS leaving at least 1 safe path!
        val primaryLane = random.nextInt(3)
        val spawnObstacle = random.nextFloat() < (0.45f + obstacleDensity * 0.50f)

        if (spawnObstacle) {
            val roll = random.nextFloat()
            val kind = when {
                roll < 0.28f -> ObstacleKind.LOW_BARRIER
                roll < 0.52f -> ObstacleKind.HIGH_ARCH
                roll < 0.76f -> ObstacleKind.FULL_BLOCK
                roll < 0.90f -> ObstacleKind.MOVING_DRONE
                else -> ObstacleKind.TRACK_GAP
            }
            spawnObstacle(kind, primaryLane, z)

            // Secondary obstacle on hard density in a different lane (never blocking all 3 as FULL_BLOCK)
            if (random.nextFloat() < obstacleDensity * 0.55f) {
                val otherLane = (primaryLane + 1 + random.nextInt(2)) % 3
                val secondKind = if (random.nextBoolean()) ObstacleKind.LOW_BARRIER else ObstacleKind.HIGH_ARCH
                spawnObstacle(secondKind, otherLane, z)
            }
        }

        // Spawn coins along a safe lane or arc over low barrier
        if (random.nextFloat() < coinDensity) {
            val safeLane = (primaryLane + 1 + random.nextInt(2)) % 3
            val coinCount = 3 + random.nextInt(3)
            for (i in 0 until coinCount) {
                val coin = coins.firstOrNull { !it.active } ?: break
                coin.active = true
                coin.lane = safeLane
                coin.worldX = laneToX(safeLane)
                coin.worldY = 0.85f + sin((i / coinCount.toFloat()) * Math.PI).toFloat() * 0.25f
                coin.worldZ = z - 3.5f + i * 2.1f
            }
        }

        // Spawn occasional power-up
        if (random.nextFloat() < powerUpDensity * 0.38f) {
            val pu = powerUps.firstOrNull { !it.active } ?: return
            val puLane = random.nextInt(3)
            pu.active = true
            pu.type = PowerUpType.entries[random.nextInt(PowerUpType.entries.size)]
            pu.worldX = laneToX(puLane)
            pu.worldY = 1.05f
            pu.worldZ = z + 4.5f
        }
    }

    private fun spawnObstacle(kind: ObstacleKind, lane: Int, z: Float) {
        if (kind == ObstacleKind.TRACK_GAP) {
            // Find closest track segment and mark its gap lane
            val seg = segments.minByOrNull { abs(it.zStart + SEGMENT_LENGTH * 0.5f - z) }
            if (seg != null) {
                seg.gapLane = lane
            }
        }
        val obs = obstacles.firstOrNull { !it.active } ?: return
        obs.active = true
        obs.kind = kind
        obs.lane = lane
        obs.worldX = laneToX(lane)
        obs.worldZ = if (kind == ObstacleKind.TRACK_GAP) {
            val seg = segments.minByOrNull { abs(it.zStart + SEGMENT_LENGTH * 0.5f - z) }
            (seg?.zStart ?: z) + SEGMENT_LENGTH * 0.5f
        } else z
        obs.moveDir = if (random.nextBoolean()) 1f else -1f
        obs.passedChecked = false
        obs.nearMissAwarded = false
    }

    private fun laneToX(lane: Int): Float = (lane - 1) * LANE_SPACING

    private fun obstacleHitLabel(kind: ObstacleKind): String = when (kind) {
        ObstacleKind.LOW_BARRIER -> "Clipped a Speed Barrier"
        ObstacleKind.HIGH_ARCH -> "Hit an Overhead Gate"
        ObstacleKind.FULL_BLOCK -> "Crashed into a Cargo Block"
        ObstacleKind.MOVING_DRONE -> "Intercepted by Patrol Drone"
        ObstacleKind.TRACK_GAP -> "Fell into a Track Gap"
    }
}
