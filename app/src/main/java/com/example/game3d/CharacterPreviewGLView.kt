package com.example.game3d

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CharacterAppearance
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Interactive 3D Turntable OpenGL ES 3.0 preview used in Character Studio and Characters screen.
 * Displays the live 3D cartoon character with the player's cropped face texture mapped onto the head.
 */
class CharacterPreviewRenderer : GLSurfaceView.Renderer {

    private lateinit var shader: ShaderProgram
    private lateinit var meshes: MeshLibrary
    private lateinit var model: Character3DModel

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    @Volatile var appearance: CharacterAppearance? = null
    @Volatile var userYawDeg: Float = 18f
    @Volatile var autoRotate: Boolean = true

    @Volatile private var pendingBitmap: Bitmap? = null
    @Volatile private var pendingPath: String? = null
    @Volatile private var textureDirty: Boolean = false
    private var faceTextureId: Int = 0
    private var animPhase: Float = 0f
    private var lastNs: Long = 0L

    fun updatePreview(char: CharacterAppearance, liveBitmap: Bitmap?) {
        val pathChanged = char.faceImagePath != pendingPath
        appearance = char
        if (liveBitmap != null || pathChanged) {
            pendingBitmap = liveBitmap
            pendingPath = char.faceImagePath
            textureDirty = true
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        shader = ShaderProgram()
        meshes = MeshLibrary()
        model = Character3DModel(meshes)
        lastNs = System.nanoTime()
        textureDirty = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projMatrix, 0, 40f, ratio, 0.3f, 30f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastNs = now
        animPhase += dt * 3.0f
        if (autoRotate) {
            userYawDeg = (userYawDeg + dt * 26f) % 360f
        }

        if (textureDirty) {
            textureDirty = false
            GlTextureHelper.deleteTexture(faceTextureId)
            val bmp = pendingBitmap
            faceTextureId = if (bmp != null && !bmp.isRecycled) {
                GlTextureHelper.loadTextureFromBitmap(bmp)
            } else {
                GlTextureHelper.loadTextureFromFile(pendingPath)
            }
        }

        GLES30.glClearColor(0.08f, 0.11f, 0.21f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        shader.use()
        GLES30.glUniform3f(shader.uFogColorLoc, 0.08f, 0.11f, 0.21f)
        GLES30.glUniform1f(shader.uFogDensityLoc, 0.01f)
        GLES30.glUniform3f(shader.uLightDirLoc, 0.4f, 0.8f, 0.6f)

        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 1.38f, 3.85f,
            0f, 1.10f, 0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // Holographic turntable pedestal
        meshes.drawPart(shader, meshes.prism, vpMatrix, null, 0f, -0.06f, 0f, 0f, 0f, 0f, 1.6f, 0.12f, 1.6f, 0x1E294A)
        meshes.drawPart(shader, meshes.prism, vpMatrix, null, 0f, 0.01f, 0f, 0f, 0f, 0f, 1.65f, 0.03f, 1.65f, 0x00E5FF, emissive = 0.55f)

        val char = appearance ?: return
        model.drawCharacter(
            shader = shader,
            vpMatrix = vpMatrix,
            appearance = char,
            faceTextureId = faceTextureId,
            worldX = 0f,
            worldY = 0.02f,
            worldZ = 0f,
            yawDeg = userYawDeg,
            leanDeg = 0f,
            pose = CharacterPose.IDLE_TURNTABLE,
            animPhase = animPhase
        )
    }
}

class CharacterPreviewGLView(context: Context) : GLSurfaceView(context) {
    val renderer = CharacterPreviewRenderer()
    private var prevX = 0f

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                renderer.autoRotate = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - prevX
                prevX = event.x
                renderer.userYawDeg += dx * 0.65f
                return true
            }
            MotionEvent.ACTION_UP -> {
                renderer.autoRotate = true
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

@Composable
fun Character3DTurntable(
    appearance: CharacterAppearance,
    liveFaceBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glView = remember(context) { CharacterPreviewGLView(context) }

    LaunchedEffect(appearance, liveFaceBitmap) {
        glView.renderer.updatePreview(appearance, liveFaceBitmap)
    }

    DisposableEffect(glView) {
        glView.onResume()
        onDispose { glView.onPause() }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier
    )
}
