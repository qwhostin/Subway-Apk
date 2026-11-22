package com.example.game3d

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import java.io.File

class ShaderProgram {

    val programId: Int
    val aPositionLoc: Int
    val aNormalLoc: Int
    val aTexCoordLoc: Int

    val uMvpMatrixLoc: Int
    val uModelMatrixLoc: Int
    val uColorLoc: Int
    val uUseTextureLoc: Int
    val uTextureLoc: Int
    val uLightDirLoc: Int
    val uFogColorLoc: Int
    val uFogDensityLoc: Int
    val uEmissiveLoc: Int

    init {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_SRC)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC)
        programId = GLES30.glCreateProgram().also { id ->
            GLES30.glAttachShader(id, vertexShader)
            GLES30.glAttachShader(id, fragmentShader)
            GLES30.glLinkProgram(id)
        }
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)

        aPositionLoc = GLES30.glGetAttribLocation(programId, "aPosition")
        aNormalLoc = GLES30.glGetAttribLocation(programId, "aNormal")
        aTexCoordLoc = GLES30.glGetAttribLocation(programId, "aTexCoord")

        uMvpMatrixLoc = GLES30.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrixLoc = GLES30.glGetUniformLocation(programId, "uModelMatrix")
        uColorLoc = GLES30.glGetUniformLocation(programId, "uColor")
        uUseTextureLoc = GLES30.glGetUniformLocation(programId, "uUseTexture")
        uTextureLoc = GLES30.glGetUniformLocation(programId, "uTexture")
        uLightDirLoc = GLES30.glGetUniformLocation(programId, "uLightDir")
        uFogColorLoc = GLES30.glGetUniformLocation(programId, "uFogColor")
        uFogDensityLoc = GLES30.glGetUniformLocation(programId, "uFogDensity")
        uEmissiveLoc = GLES30.glGetUniformLocation(programId, "uEmissive")
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    fun delete() {
        GLES30.glDeleteProgram(programId)
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER_SRC = """#version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec2 aTexCoord;

            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;

            out vec3 vNormal;
            out vec2 vTexCoord;
            out float vDepth;

            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vec4 clipPos = uMVPMatrix * vec4(aPosition, 1.0);
                gl_Position = clipPos;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vTexCoord = aTexCoord;
                vDepth = clipPos.w;
            }
        """

        private const val FRAGMENT_SHADER_SRC = """#version 300 es
            precision mediump float;

            in vec3 vNormal;
            in vec2 vTexCoord;
            in float vDepth;

            uniform vec4 uColor;
            uniform int uUseTexture;
            uniform sampler2D uTexture;
            uniform vec3 uLightDir;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform float uEmissive;

            out vec4 fragColor;

            void main() {
                vec4 baseColor = uColor;
                if (uUseTexture == 1) {
                    vec4 texColor = texture(uTexture, vTexCoord);
                    baseColor = vec4(texColor.rgb, texColor.a * uColor.a);
                }

                float ndotl = max(dot(normalize(vNormal), normalize(uLightDir)), 0.0);
                float light = 0.48 + 0.52 * ndotl + uEmissive;
                vec3 lit = baseColor.rgb * min(light, 1.35);

                float fogFactor = clamp(exp(-pow(max(vDepth, 0.0) * uFogDensity, 2.0)), 0.0, 1.0);
                vec3 finalRgb = mix(uFogColor, lit, fogFactor);
                fragColor = vec4(finalRgb, baseColor.a);
            }
        """
    }
}

object GlTextureHelper {
    fun loadTextureFromFile(path: String?): Int {
        if (path.isNullOrBlank()) return 0
        val file = File(path)
        if (!file.exists()) return 0
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return 0
        val texId = loadTextureFromBitmap(bitmap)
        bitmap.recycle()
        return texId
    }

    fun loadTextureFromBitmap(bitmap: Bitmap): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        val texId = ids[0]
        if (texId == 0) return 0
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return texId
    }

    fun deleteTexture(texId: Int) {
        if (texId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(texId), 0)
        }
    }
}
