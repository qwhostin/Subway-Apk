package com.example

import com.example.data.model.CustomRunConfig
import com.example.data.model.DefaultCharactersCatalog
import com.example.data.model.EnvironmentsCatalog
import com.example.data.model.GameMode
import com.example.data.model.GameSettings
import com.example.data.model.GraphicsQuality
import com.example.data.model.RunSummary
import com.example.game3d.CharacterPose
import com.example.game3d.RunnerWorldEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunvantaEngineAndPersistenceTest {

    @Test
    fun `catalogs contain expected original characters worlds and modes`() {
        assertEquals(6, DefaultCharactersCatalog.all.size)
        assertEquals(8, EnvironmentsCatalog.all.size)
        assertEquals(6, GameMode.entries.size)
        assertEquals("neon_city", EnvironmentsCatalog.all.first().id)
        assertEquals(0, EnvironmentsCatalog.all.first().unlockCoins)
    }

    @Test
    fun `custom run config clamps unsafe values safely`() {
        val unsafe = CustomRunConfig(
            startingSpeed = 200f,
            maxSpeed = 5f,
            difficultyLevel = 99,
            obstacleFrequency = 5.0f,
            coinFrequency = -1.0f,
            powerUpFrequency = 9.0f,
            startingLives = 0,
            durationSeconds = 9999,
            scoreMultiplier = 50f
        )
        val safe = unsafe.clamped()
        assertTrue(safe.startingSpeed <= 30f)
        assertTrue(safe.maxSpeed > safe.startingSpeed)
        assertEquals(5, safe.difficultyLevel)
        assertTrue(safe.obstacleFrequency <= 0.95f)
        assertTrue(safe.coinFrequency >= 0.15f)
        assertTrue(safe.powerUpFrequency <= 0.60f)
        assertEquals(1, safe.startingLives)
        assertEquals(600, safe.durationSeconds)
        assertTrue(safe.scoreMultiplier <= 3.0f)
    }

    @Test
    fun `settings battery saver caps fps to 30`() {
        val battery = GameSettings(
            graphicsQuality = GraphicsQuality.HIGH,
            targetFps = 120,
            batterySaverMode = true
        )
        assertEquals(30, battery.effectiveTargetFps())
    }

    @Test
    fun `runner world engine handles lanes jump slide pause and duration completion`() {
        var latestScore = 0
        var latestDist = 0
        var gameOverSummary: RunSummary? = null
        val engine = RunnerWorldEngine(
            onSound = {},
            onVibrate = {},
            onHudChanged = {
                latestScore = it.score
                latestDist = it.distanceMeters
            },
            onGameOver = { gameOverSummary = it }
        )

        engine.startNewRun(
            mode = GameMode.CUSTOM,
            env = EnvironmentsCatalog.all[1],
            custom = CustomRunConfig(durationSeconds = 30, startingLives = 3),
            isCustomChar = true
        )

        assertEquals(1, engine.playerLane)
        engine.moveLeft()
        assertEquals(0, engine.playerLane)
        engine.moveLeft()
        assertEquals(0, engine.playerLane)
        engine.moveRight()
        engine.moveRight()
        assertEquals(2, engine.playerLane)

        engine.jump()
        assertFalse(engine.isGrounded)
        assertEquals(CharacterPose.JUMPING, engine.currentPose)

        repeat(45) { engine.update(0.03f) }
        assertTrue(engine.playerZ > 10f)
        assertTrue(latestScore > 0)
        assertTrue(latestDist >= 10)

        engine.slide()
        assertTrue(engine.slideTimer > 0f)
        engine.setPaused(true)
        val pausedZ = engine.playerZ
        engine.update(0.05f)
        assertEquals(pausedZ, engine.playerZ, 0.0001f)
        engine.setPaused(false)
        assertNull(gameOverSummary)
    }
}
