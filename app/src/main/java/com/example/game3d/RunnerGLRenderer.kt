package com.example.game3d

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.model.CharacterAppearance
import com.example.data.model.EnvironmentTheme
import com.example.data.model.GameSettings
import com.example.data.model.SceneryStyle
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin
import kotlin.random.Random

/**
 * OpenGL ES 3.0 3D Renderer for active RUNVANTA endless-runner gameplay.
 * Renders all 8 environment themes, atmospheric fog, procedural 3D scenery,
 * track slabs/gaps, obstacles, spinning coins, power-ups, and the animated 3D runner.
 */
class RunnerGLRenderer(
    private val engine: RunnerWorldEngine
) : GLSurfaceView.Renderer {

    private lateinit var shader: ShaderProgram
    private lateinit var meshes: MeshLibrary
    private lateinit var characterModel: Character3DModel

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    @Volatile var currentCharacter: CharacterAppearance? = null
    @Volatile var settings: GameSettings = GameSettings()
    @Volatile private var pendingFacePath: String? = null
    @Volatile private var facePathDirty: Boolean = true
    private var loadedFaceTextureId: Int = 0
    private var loadedFacePath: String? = null

    private var lastFrameNs: Long = 0L
    private var spinAngle: Float = 0f

    fun setCharacter(char: CharacterAppearance) {
        currentCharacter = char
        if (char.faceImagePath != loadedFacePath) {
            pendingFacePath = char.faceImagePath
            facePathDirty = true
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        shader = ShaderProgram()
        meshes = MeshLibrary()
        characterModel = Character3DModel(meshes)
        lastFrameNs = System.nanoTime()
        facePathDirty = true
        pendingFacePath = currentCharacter?.faceImagePath
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projMatrix, 0, 56f, ratio, 0.4f, 145f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val rawDt = ((nowNs - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        val targetFps = settings.effectiveTargetFps()
        val minFrameMs = (1000L / targetFps.coerceIn(24, 120))
        val elapsedMs = (nowNs - lastFrameNs) / 1_000_000L
        if (elapsedMs < minFrameMs) {
            runCatching { Thread.sleep((minFrameMs - elapsedMs).coerceAtMost(25L)) }
        }
        val frameNow = System.nanoTime()
        val dt = ((frameNow - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastFrameNs = frameNow

        engine.update(dt)
        spinAngle = (spinAngle + dt * 165f) % 360f

        if (facePathDirty) {
            facePathDirty = false
            GlTextureHelper.deleteTexture(loadedFaceTextureId)
            loadedFaceTextureId = GlTextureHelper.loadTextureFromFile(pendingFacePath)
            loadedFacePath = pendingFacePath
        }

        val env = engine.environment
        val skyR = ((env.skyBottomRgb shr 16) and 0xFF) / 255f
        val skyG = ((env.skyBottomRgb shr 8) and 0xFF) / 255f
        val skyB = (env.skyBottomRgb and 0xFF) / 255f
        GLES30.glClearColor(skyR, skyG, skyB, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        shader.use()

        // Atmospheric fog & directional lighting
        val fogR = ((env.fogRgb shr 16) and 0xFF) / 255f
        val fogG = ((env.fogRgb shr 8) and 0xFF) / 255f
        val fogB = (env.fogRgb and 0xFF) / 255f
        GLES30.glUniform3f(shader.uFogColorLoc, fogR, fogG, fogB)
        GLES30.glUniform1f(shader.uFogDensityLoc, 0.0115f)
        GLES30.glUniform3f(shader.uLightDirLoc, 0.45f, 0.85f, -0.35f)

        // Smooth camera follow + optional shake
        val shakeX = if (settings.cameraShakeEnabled && engine.cameraShakeAmount > 0f) {
            (Random.nextFloat() - 0.5f) * engine.cameraShakeAmount * 0.5f
        } else 0f
        val shakeY = if (settings.cameraShakeEnabled && engine.cameraShakeAmount > 0f) {
            (Random.nextFloat() - 0.5f) * engine.cameraShakeAmount * 0.4f
        } else 0f

        val camX = engine.playerX * 0.55f + shakeX
        val camY = 4.3f + engine.playerY * 0.45f + shakeY
        val camZ = engine.playerZ - 6.6f
        Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, camZ,
            engine.playerX * 0.45f, 1.3f + engine.playerY * 0.3f, engine.playerZ + 14f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        drawSkySunOrMoon(env)
        drawTrackSegments(env)
        drawScenery(env)
        drawObstacles(env)
        drawCoins()
        drawPowerUps()

        val char = currentCharacter
        if (char != null) {
            characterModel.drawCharacter(
                shader = shader,
                vpMatrix = vpMatrix,
                appearance = char,
                faceTextureId = loadedFaceTextureId,
                worldX = engine.playerX,
                worldY = engine.playerY,
                worldZ = engine.playerZ,
                yawDeg = 0f,
                leanDeg = engine.leanDeg,
                pose = engine.currentPose,
                animPhase = engine.animPhase,
                hasShield = false,
                showTrail = settings.motionEffectsEnabled
            )
        }
    }

    private fun drawSkySunOrMoon(env: EnvironmentTheme) {
        val celestialZ = engine.playerZ + 96f
        meshes.drawPart(
            shader, meshes.prism, vpMatrix, null,
            0f, 22f, celestialZ, 90f, 0f, 0f,
            14f, 1.5f, 14f,
            env.railAccentRgb, emissive = 0.65f
        )
    }

    private fun drawTrackSegments(env: EnvironmentTheme) {
        val len = RunnerWorldEngine.SEGMENT_LENGTH
        val laneW = RunnerWorldEngine.LANE_SPACING
        for (seg in engine.segments) {
            val cz = seg.zStart + len * 0.5f
            for (lane in 0..2) {
                if (seg.gapLane == lane) continue // Broken gap!
                val lx = (lane - 1) * laneW
                val laneColor = if (lane == 1) env.trackLaneRgb else env.trackDeckRgb
                meshes.drawPart(
                    shader, meshes.box, vpMatrix, null,
                    lx, -0.25f, cz, 0f, 0f, 0f,
                    laneW - 0.14f, 0.5f, len - 0.08f,
                    laneColor
                )
            }
            // Side glowing rails
            meshes.drawPart(
                shader, meshes.box, vpMatrix, null,
                -3.65f, 0.12f, cz, 0f, 0f, 0f,
                0.22f, 0.35f, len,
                env.railAccentRgb, emissive = 0.35f
            )
            meshes.drawPart(
                shader, meshes.box, vpMatrix, null,
                3.65f, 0.12f, cz, 0f, 0f, 0f,
                0.22f, 0.35f, len,
                env.railAccentRgb, emissive = 0.35f
            )
        }
    }

    private fun drawScenery(env: EnvironmentTheme) {
        val limit = settings.graphicsQuality.sceneryDensity
        var drawn = 0
        for (prop in engine.sceneryProps) {
            if (drawn >= limit * 2) break
            drawn++
            val px = prop.side * (6.2f + prop.variant * 1.4f)
            when (env.sceneryStyle) {
                SceneryStyle.NEON_TOWERS, SceneryStyle.CYBER_HOLOGRAMS -> {
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, px, prop.height * 0.5f - 1f, prop.z, 0f, 0f, 0f, prop.width, prop.height, prop.width, env.sceneryPrimaryRgb)
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, px - prop.side * (prop.width * 0.51f), prop.height * 0.55f, prop.z, 0f, 0f, 0f, 0.08f, prop.height * 0.65f, prop.width * 0.55f, env.sceneryAccentRgb, emissive = 0.55f)
                }
                SceneryStyle.TROPICAL_PALMS -> {
                    meshes.drawPart(shader, meshes.prism, vpMatrix, null, px, prop.height * 0.35f, prop.z, 0f, 0f, prop.side * -8f, 0.42f, prop.height * 0.7f, 0.42f, 0x8B5A2B)
                    meshes.drawPart(shader, meshes.cone, vpMatrix, null, px, prop.height * 0.72f, prop.z, 0f, 0f, 0f, 2.8f, 1.3f, 2.8f, env.sceneryPrimaryRgb)
                }
                SceneryStyle.DESERT_MESAS -> {
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, px, prop.height * 0.32f, prop.z, 0f, 0f, 0f, prop.width * 1.7f, prop.height * 0.65f, prop.width * 1.5f, env.sceneryPrimaryRgb)
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, px, prop.height * 0.66f, prop.z, 0f, 0f, 0f, prop.width * 1.2f, 0.5f, prop.width * 1.1f, env.sceneryAccentRgb)
                }
                SceneryStyle.ALPINE_PINES -> {
                    meshes.drawPart(shader, meshes.prism, vpMatrix, null, px, 1.0f, prop.z, 0f, 0f, 0f, 0.4f, 2.0f, 0.4f, 0x5C4033)
                    meshes.drawPart(shader, meshes.cone, vpMatrix, null, px, 3.2f, prop.z, 0f, 0f, 0f, 2.4f, 3.6f, 2.4f, env.sceneryPrimaryRgb)
                    meshes.drawPart(shader, meshes.cone, vpMatrix, null, px, 5.1f, prop.z, 0f, 0f, 0f, 1.7f, 2.6f, 1.7f, env.sceneryAccentRgb)
                }
                SceneryStyle.SPACE_GANTRIES -> {
                    meshes.drawPart(shader, meshes.prism, vpMatrix, null, px, prop.height * 0.45f, prop.z, 0f, 0f, 0f, 0.6f, prop.height * 0.9f, 0.6f, env.sceneryPrimaryRgb)
                    meshes.drawPart(shader, meshes.cone, vpMatrix, null, px, prop.height * 0.95f, prop.z, 0f, 0f, 0f, 1.1f, 1.6f, 1.1f, env.sceneryAccentRgb, emissive = 0.45f)
                }
                SceneryStyle.SNOW_CRYSTALS -> {
                    meshes.drawPart(shader, meshes.cone, vpMatrix, null, px, prop.height * 0.4f, prop.z, 0f, prop.variant * 30f, 0f, prop.width * 1.4f, prop.height * 0.8f, prop.width * 1.4f, env.sceneryPrimaryRgb, emissive = 0.2f)
                }
                SceneryStyle.ANCIENT_TEMPLES -> {
                    meshes.drawPart(shader, meshes.prism, vpMatrix, null, px, prop.height * 0.4f, prop.z, 0f, 0f, 0f, 0.9f, prop.height * 0.8f, 0.9f, env.sceneryPrimaryRgb)
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, px, prop.height * 0.82f, prop.z, 0f, 0f, 0f, 1.5f, 0.45f, 1.5f, env.sceneryAccentRgb, emissive = 0.35f)
                }
            }
        }
    }

    private fun drawObstacles(env: EnvironmentTheme) {
        for (obs in engine.obstacles) {
            if (!obs.active) continue
            when (obs.kind) {
                ObstacleKind.LOW_BARRIER -> {
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, obs.worldX, 0.50f, obs.worldZ, 0f, 0f, 0f, 1.85f, 0.92f, 0.45f, env.obstaclePrimaryRgb)
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, obs.worldX, 0.96f, obs.worldZ, 0f, 0f, 0f, 1.92f, 0.16f, 0.50f, env.obstacleAccentRgb, emissive = 0.35f)
                }
                ObstacleKind.HIGH_ARCH -> {
                    // Two side posts + overhead beam to slide under!
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, obs.worldX - 0.85f, 1.3f, obs.worldZ, 0f, 0f, 0f, 0.26f, 2.6f, 0.4f, env.obstaclePrimaryRgb)
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, obs.worldX + 0.85f, 1.3f, obs.worldZ, 0f, 0f, 0f, 0.26f, 2.6f, 0.4f, env.obstaclePrimaryRgb)
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, obs.worldX, 1.78f, obs.worldZ, 0f, 0f, 0f, 2.0f, 1.35f, 0.48f, env.obstacleAccentRgb, emissive = 0.2f)
                }
                ObstacleKind.FULL_BLOCK -> {
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, obs.worldX, 1.2f, obs.worldZ, 0f, 0f, 0f, 1.85f, 2.35f, 1.6f, env.obstaclePrimaryRgb)
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, obs.worldX, 1.8f, obs.worldZ - 0.82f, 0f, 0f, 0f, 1.55f, 0.45f, 0.08f, env.obstacleAccentRgb, emissive = 0.45f)
                }
                ObstacleKind.MOVING_DRONE -> {
                    val hoverY = 0.95f + sin(spinAngle * 0.06f) * 0.15f
                    meshes.drawPart(shader, meshes.prism, vpMatrix, null, obs.worldX, hoverY, obs.worldZ, 0f, spinAngle, 0f, 1.5f, 0.55f, 1.5f, env.obstaclePrimaryRgb)
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, obs.worldX, hoverY, obs.worldZ, 0f, -spinAngle * 1.5f, 0f, 1.9f, 0.15f, 0.28f, env.obstacleAccentRgb, emissive = 0.55f)
                }
                ObstacleKind.TRACK_GAP -> {
                    // Warning neon rim at gap edge
                    meshes.drawPart(shader, meshes.box, vpMatrix, null, obs.worldX, 0.02f, obs.worldZ - 2.6f, 0f, 0f, 0f, 2.0f, 0.10f, 0.22f, 0xFF3366, emissive = 0.65f)
                }
            }
        }
    }

    private fun drawCoins() {
        for (coin in engine.coins) {
            if (!coin.active) continue
            meshes.drawPart(
                shader, meshes.prism, vpMatrix, null,
                coin.worldX, coin.worldY, coin.worldZ,
                90f, spinAngle, 0f,
                0.62f, 0.16f, 0.62f,
                0xFFD166, emissive = 0.35f
            )
        }
    }

    private fun drawPowerUps() {
        for (pu in engine.powerUps) {
            if (!pu.active) continue
            val bobY = pu.worldY + sin(spinAngle * 0.04f) * 0.18f
            meshes.drawPart(
                shader, meshes.box, vpMatrix, null,
                pu.worldX, bobY, pu.worldZ,
                35f, spinAngle, 35f,
                0.78f, 0.78f, 0.78f,
                pu.type.colorRgb, emissive = 0.50f
            )
        }
    }
}
