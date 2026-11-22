package com.example.game3d

import android.opengl.GLES30
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GlMesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / STRIDE_FLOATS
    private val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    fun draw(shader: ShaderProgram) {
        buffer.position(0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, STRIDE_BYTES, buffer)

        buffer.position(3)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, STRIDE_BYTES, buffer)

        buffer.position(6)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, STRIDE_BYTES, buffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
    }

    companion object {
        private const val STRIDE_FLOATS = 8 // x,y,z + nx,ny,nz + u,v
        private const val STRIDE_BYTES = STRIDE_FLOATS * 4
    }
}

class MeshLibrary {
    val box: GlMesh = buildUnitBox()
    val prism: GlMesh = buildPrism(12)
    val cone: GlMesh = buildCone(10)
    val faceQuad: GlMesh = buildFaceQuad()

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun drawPart(
        shader: ShaderProgram,
        mesh: GlMesh,
        vpMatrix: FloatArray,
        parentMatrix: FloatArray?,
        tx: Float, ty: Float, tz: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        rgb: Long,
        alpha: Float = 1.0f,
        emissive: Float = 0.0f,
        textureId: Int = 0
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        if (parentMatrix != null) {
            System.arraycopy(parentMatrix, 0, modelMatrix, 0, 16)
        }
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)
        if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(shader.uMvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)

        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        GLES30.glUniform4f(shader.uColorLoc, r, g, b, alpha)
        GLES30.glUniform1f(shader.uEmissiveLoc, emissive)

        if (textureId != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
            GLES30.glUniform1i(shader.uTextureLoc, 0)
            GLES30.glUniform1i(shader.uUseTextureLoc, 1)
        } else {
            GLES30.glUniform1i(shader.uUseTextureLoc, 0)
        }

        mesh.draw(shader)
    }

    private fun buildUnitBox(): GlMesh {
        val list = ArrayList<Float>(36 * 8)
        fun addTri(
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float,
            nx: Float, ny: Float, nz: Float,
            u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float
        ) {
            list.addAll(listOf(x1, y1, z1, nx, ny, nz, u1, v1))
            list.addAll(listOf(x2, y2, z2, nx, ny, nz, u2, v2))
            list.addAll(listOf(x3, y3, z3, nx, ny, nz, u3, v3))
        }
        fun addFace(
            ax: Float, ay: Float, az: Float,
            bx: Float, by: Float, bz: Float,
            cx: Float, cy: Float, cz: Float,
            dx: Float, dy: Float, dz: Float,
            nx: Float, ny: Float, nz: Float
        ) {
            addTri(ax, ay, az, bx, by, bz, cx, cy, cz, nx, ny, nz, 0f, 1f, 1f, 1f, 1f, 0f)
            addTri(ax, ay, az, cx, cy, cz, dx, dy, dz, nx, ny, nz, 0f, 1f, 1f, 0f, 0f, 0f)
        }
        val h = 0.5f
        // Front (+Z)
        addFace(-h, -h, h, h, -h, h, h, h, h, -h, h, h, 0f, 0f, 1f)
        // Back (-Z)
        addFace(h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0f, 0f, -1f)
        // Left (-X)
        addFace(-h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1f, 0f, 0f)
        // Right (+X)
        addFace(h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1f, 0f, 0f)
        // Top (+Y)
        addFace(-h, h, h, h, h, h, h, h, -h, -h, h, -h, 0f, 1f, 0f)
        // Bottom (-Y)
        addFace(-h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0f, -1f, 0f)
        return GlMesh(list.toFloatArray())
    }

    private fun buildPrism(segments: Int): GlMesh {
        val list = ArrayList<Float>(segments * 12 * 8)
        val r = 0.5f
        val h = 0.5f
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val x0 = cos(a0) * r
            val z0 = sin(a0) * r
            val x1 = cos(a1) * r
            val z1 = sin(a1) * r
            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)

            // Side quad
            list.addAll(listOf(x0, -h, z0, nx, 0f, nz, 0f, 1f))
            list.addAll(listOf(x1, -h, z1, nx, 0f, nz, 1f, 1f))
            list.addAll(listOf(x1, h, z1, nx, 0f, nz, 1f, 0f))

            list.addAll(listOf(x0, -h, z0, nx, 0f, nz, 0f, 1f))
            list.addAll(listOf(x1, h, z1, nx, 0f, nz, 1f, 0f))
            list.addAll(listOf(x0, h, z0, nx, 0f, nz, 0f, 0f))

            // Top cap
            list.addAll(listOf(0f, h, 0f, 0f, 1f, 0f, 0.5f, 0.5f))
            list.addAll(listOf(x0, h, z0, 0f, 1f, 0f, 0f, 0f))
            list.addAll(listOf(x1, h, z1, 0f, 1f, 0f, 1f, 0f))

            // Bottom cap
            list.addAll(listOf(0f, -h, 0f, 0f, -1f, 0f, 0.5f, 0.5f))
            list.addAll(listOf(x1, -h, z1, 0f, -1f, 0f, 1f, 1f))
            list.addAll(listOf(x0, -h, z0, 0f, -1f, 0f, 0f, 1f))
        }
        return GlMesh(list.toFloatArray())
    }

    private fun buildCone(segments: Int): GlMesh {
        val list = ArrayList<Float>(segments * 6 * 8)
        val r = 0.5f
        val h = 0.5f
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val x0 = cos(a0) * r
            val z0 = sin(a0) * r
            val x1 = cos(a1) * r
            val z1 = sin(a1) * r
            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)

            list.addAll(listOf(0f, h, 0f, nx, 0.45f, nz, 0.5f, 0f))
            list.addAll(listOf(x1, -h, z1, nx, 0.45f, nz, 1f, 1f))
            list.addAll(listOf(x0, -h, z0, nx, 0.45f, nz, 0f, 1f))

            list.addAll(listOf(0f, -h, 0f, 0f, -1f, 0f, 0.5f, 0.5f))
            list.addAll(listOf(x0, -h, z0, 0f, -1f, 0f, 0f, 1f))
            list.addAll(listOf(x1, -h, z1, 0f, -1f, 0f, 1f, 1f))
        }
        return GlMesh(list.toFloatArray())
    }

    private fun buildFaceQuad(): GlMesh {
        // Vertical quad facing +Z (or -Z in runner). UVs oriented top-down for Android bitmaps.
        val h = 0.5f
        val verts = floatArrayOf(
            -h, -h, 0f, 0f, 0f, 1f, 0f, 1f,
            h, -h, 0f, 0f, 0f, 1f, 1f, 1f,
            h, h, 0f, 0f, 0f, 1f, 1f, 0f,

            -h, -h, 0f, 0f, 0f, 1f, 0f, 1f,
            h, h, 0f, 0f, 0f, 1f, 1f, 0f,
            -h, h, 0f, 0f, 0f, 1f, 0f, 0f
        )
        return GlMesh(verts)
    }
}
