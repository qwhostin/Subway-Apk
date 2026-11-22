package com.example.game3d

import android.opengl.Matrix
import com.example.data.model.AccessoryType
import com.example.data.model.CharacterAppearance
import com.example.data.model.HairStyle
import com.example.data.model.OutfitStyle
import com.example.data.model.ShoeStyle
import com.example.data.model.TrailEffect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class CharacterPose {
    RUNNING,
    JUMPING,
    SLIDING,
    FALLING,
    VICTORY,
    IDLE_TURNTABLE
}

/**
 * Articulated 3D Cartoon Character Renderer for RUNVANTA.
 * Renders all 6 original characters AND any custom "Play As Anyone" character with a
 * live cropped photo texture mapped onto the 3D cartoon head's front face surface.
 */
class Character3DModel(private val meshes: MeshLibrary) {

    private val rootMatrix = FloatArray(16)
    private val torsoMatrix = FloatArray(16)
    private val headMatrix = FloatArray(16)

    fun drawCharacter(
        shader: ShaderProgram,
        vpMatrix: FloatArray,
        appearance: CharacterAppearance,
        faceTextureId: Int,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        yawDeg: Float,
        leanDeg: Float,
        pose: CharacterPose,
        animPhase: Float,
        hasShield: Boolean = false,
        showTrail: Boolean = false
    ) {
        val stride = sin(animPhase)
        val strideOpp = -stride
        val bounce = when (pose) {
            CharacterPose.RUNNING -> abs(stride) * 0.08f
            CharacterPose.VICTORY -> abs(sin(animPhase * 1.4f)) * 0.14f
            CharacterPose.IDLE_TURNTABLE -> sin(animPhase * 0.5f) * 0.04f
            else -> 0f
        }

        Matrix.setIdentityM(rootMatrix, 0)
        Matrix.translateM(rootMatrix, 0, worldX, worldY + bounce, worldZ)
        Matrix.rotateM(rootMatrix, 0, yawDeg, 0f, 1f, 0f)

        val bodyPitch = when (pose) {
            CharacterPose.RUNNING -> 10f
            CharacterPose.JUMPING -> -12f
            CharacterPose.SLIDING -> -62f
            CharacterPose.FALLING -> (animPhase * 120f) % 360f
            CharacterPose.VICTORY -> -4f
            CharacterPose.IDLE_TURNTABLE -> 2f
        }
        val slideYOffset = if (pose == CharacterPose.SLIDING) -0.52f else 0f

        Matrix.translateM(rootMatrix, 0, 0f, slideYOffset, 0f)
        Matrix.rotateM(rootMatrix, 0, bodyPitch, 1f, 0f, 0f)
        Matrix.rotateM(rootMatrix, 0, leanDeg, 0f, 0f, 1f)

        val legSwing = when (pose) {
            CharacterPose.RUNNING -> stride * 46f
            CharacterPose.JUMPING -> 32f
            CharacterPose.SLIDING -> 18f
            CharacterPose.FALLING -> stride * 55f
            CharacterPose.VICTORY -> 6f
            CharacterPose.IDLE_TURNTABLE -> 0f
        }
        val armSwing = when (pose) {
            CharacterPose.RUNNING -> strideOpp * 44f
            CharacterPose.JUMPING -> -125f
            CharacterPose.SLIDING -> -45f
            CharacterPose.FALLING -> strideOpp * 70f
            CharacterPose.VICTORY -> -145f + sin(animPhase * 2f) * 20f
            CharacterPose.IDLE_TURNTABLE -> sin(animPhase * 0.5f) * 8f
        }

        // 1. Legs & Shoes
        val pantsColor = darkenRgb(appearance.outfitColorRgb, 0.55f)
        // Left Leg
        drawLimbWithShoe(
            shader, vpMatrix, rootMatrix,
            x = -0.17f, y = 0.52f, swingDeg = legSwing,
            pantsColor = pantsColor,
            shoeColor = appearance.shoeColorRgb,
            accentColor = appearance.accentColorRgb,
            shoeStyle = appearance.shoeStyle
        )
        // Right Leg
        drawLimbWithShoe(
            shader, vpMatrix, rootMatrix,
            x = 0.17f, y = 0.52f, swingDeg = -legSwing,
            pantsColor = pantsColor,
            shoeColor = appearance.shoeColorRgb,
            accentColor = appearance.accentColorRgb,
            shoeStyle = appearance.shoeStyle
        )

        // 2. Torso & Outfit Styling
        System.arraycopy(rootMatrix, 0, torsoMatrix, 0, 16)
        Matrix.translateM(torsoMatrix, 0, 0f, 0.96f, 0f)

        meshes.drawPart(
            shader, meshes.box, vpMatrix, torsoMatrix,
            0f, 0f, 0f, 0f, 0f, 0f,
            0.54f, 0.64f, 0.34f,
            appearance.outfitColorRgb
        )
        // Outfit chest stripe / emblem
        meshes.drawPart(
            shader, meshes.box, vpMatrix, torsoMatrix,
            0f, 0.05f, 0.18f, 0f, 0f, 0f,
            0.34f, 0.18f, 0.04f,
            appearance.accentColorRgb,
            emissive = 0.25f
        )
        when (appearance.outfitStyle) {
            OutfitStyle.CYBER_ARMOR -> {
                // Shoulder armor pads
                meshes.drawPart(shader, meshes.box, vpMatrix, torsoMatrix, -0.34f, 0.24f, 0f, 0f, 0f, 12f, 0.22f, 0.14f, 0.38f, appearance.accentColorRgb, emissive = 0.2f)
                meshes.drawPart(shader, meshes.box, vpMatrix, torsoMatrix, 0.34f, 0.24f, 0f, 0f, 0f, -12f, 0.22f, 0.14f, 0.38f, appearance.accentColorRgb, emissive = 0.2f)
            }
            OutfitStyle.STREET_HOODIE -> {
                // Collar hood on back
                meshes.drawPart(shader, meshes.prism, vpMatrix, torsoMatrix, 0f, 0.30f, -0.16f, 22f, 0f, 0f, 0.42f, 0.18f, 0.24f, appearance.accentColorRgb)
            }
            OutfitStyle.MYTH_TUNIC -> {
                // Belt & tunic sash
                meshes.drawPart(shader, meshes.box, vpMatrix, torsoMatrix, 0f, -0.26f, 0f, 0f, 0f, 0f, 0.58f, 0.12f, 0.38f, appearance.accentColorRgb)
            }
            else -> {}
        }

        // 3. Arms
        val sleeveColor = appearance.outfitColorRgb
        drawArm(shader, vpMatrix, torsoMatrix, -0.37f, armSwing, sleeveColor, appearance.skinColorRgb)
        drawArm(shader, vpMatrix, torsoMatrix, 0.37f, -armSwing, sleeveColor, appearance.skinColorRgb)

        // 4. Accessories on Torso (Jetpack / Scarf)
        if (appearance.accessory == AccessoryType.JETPACK) {
            meshes.drawPart(shader, meshes.prism, vpMatrix, torsoMatrix, -0.12f, 0.04f, -0.24f, 0f, 0f, 0f, 0.18f, 0.44f, 0.18f, 0xCBD5E1)
            meshes.drawPart(shader, meshes.prism, vpMatrix, torsoMatrix, 0.12f, 0.04f, -0.24f, 0f, 0f, 0f, 0.18f, 0.44f, 0.18f, 0xCBD5E1)
            meshes.drawPart(shader, meshes.cone, vpMatrix, torsoMatrix, -0.12f, -0.24f, -0.24f, 180f, 0f, 0f, 0.14f, 0.22f, 0.14f, 0xFF6B35, emissive = 0.5f)
            meshes.drawPart(shader, meshes.cone, vpMatrix, torsoMatrix, 0.12f, -0.24f, -0.24f, 180f, 0f, 0f, 0.14f, 0.22f, 0.14f, 0xFF6B35, emissive = 0.5f)
        } else if (appearance.accessory == AccessoryType.SCARF) {
            meshes.drawPart(shader, meshes.prism, vpMatrix, torsoMatrix, 0f, 0.31f, 0f, 0f, 0f, 0f, 0.46f, 0.11f, 0.42f, appearance.accentColorRgb)
            val flutter = sin(animPhase * 1.8f) * 14f
            meshes.drawPart(shader, meshes.box, vpMatrix, torsoMatrix, 0.08f, 0.18f, -0.28f, 25f + flutter, 0f, 0f, 0.16f, 0.42f, 0.06f, appearance.accentColorRgb)
        }

        // 5. Head & Custom Photo Face Mapping
        System.arraycopy(torsoMatrix, 0, headMatrix, 0, 16)
        Matrix.translateM(headMatrix, 0, 0f, 0.58f, 0f)

        // Rounded cartoon 3D head
        meshes.drawPart(
            shader, meshes.prism, vpMatrix, headMatrix,
            0f, 0f, 0f, 0f, 0f, 0f,
            0.54f, 0.52f, 0.54f,
            appearance.skinColorRgb
        )

        if (faceTextureId != 0) {
            // Map player's cropped photo texture onto the front face plate of the 3D cartoon head
            meshes.drawPart(
                shader, meshes.faceQuad, vpMatrix, headMatrix,
                0f, 0f, 0.275f, 0f, 0f, 0f,
                0.44f, 0.44f, 1f,
                0xFFFFFF,
                emissive = 0.18f,
                textureId = faceTextureId
            )
        } else {
            // Expressive original 3D cartoon eyes & smile
            meshes.drawPart(shader, meshes.box, vpMatrix, headMatrix, -0.10f, 0.05f, 0.26f, 0f, 0f, 0f, 0.10f, 0.11f, 0.04f, 0xFFFFFF, emissive = 0.15f)
            meshes.drawPart(shader, meshes.box, vpMatrix, headMatrix, 0.10f, 0.05f, 0.26f, 0f, 0f, 0f, 0.10f, 0.11f, 0.04f, 0xFFFFFF, emissive = 0.15f)
            meshes.drawPart(shader, meshes.box, vpMatrix, headMatrix, -0.10f, 0.05f, 0.28f, 0f, 0f, 0f, 0.05f, 0.06f, 0.03f, 0x0F172A)
            meshes.drawPart(shader, meshes.box, vpMatrix, headMatrix, 0.10f, 0.05f, 0.28f, 0f, 0f, 0f, 0.05f, 0.06f, 0.03f, 0x0F172A)
            // Smile
            meshes.drawPart(shader, meshes.box, vpMatrix, headMatrix, 0f, -0.10f, 0.27f, 0f, 0f, 0f, 0.16f, 0.04f, 0.03f, 0xFFFFFF)
        }

        // 6. Hair / Helmet / Crown
        drawHair(shader, vpMatrix, headMatrix, appearance)

        // 7. Head Accessories
        when (appearance.accessory) {
            AccessoryType.SUNGLASSES -> {
                meshes.drawPart(shader, meshes.box, vpMatrix, headMatrix, -0.11f, 0.06f, 0.29f, 0f, 0f, 0f, 0.17f, 0.09f, 0.04f, 0x090D16)
                meshes.drawPart(shader, meshes.box, vpMatrix, headMatrix, 0.11f, 0.06f, 0.29f, 0f, 0f, 0f, 0.17f, 0.09f, 0.04f, 0x090D16)
                meshes.drawPart(shader, meshes.box, vpMatrix, headMatrix, 0f, 0.07f, 0.29f, 0f, 0f, 0f, 0.42f, 0.03f, 0.03f, appearance.accentColorRgb, emissive = 0.3f)
            }
            AccessoryType.HEADPHONES -> {
                meshes.drawPart(shader, meshes.prism, vpMatrix, headMatrix, -0.29f, 0.02f, 0f, 0f, 0f, 90f, 0.16f, 0.08f, 0.18f, appearance.accentColorRgb, emissive = 0.25f)
                meshes.drawPart(shader, meshes.prism, vpMatrix, headMatrix, 0.29f, 0.02f, 0f, 0f, 0f, 90f, 0.16f, 0.08f, 0.18f, appearance.accentColorRgb, emissive = 0.25f)
                meshes.drawPart(shader, meshes.box, vpMatrix, headMatrix, 0f, 0.29f, 0f, 0f, 0f, 0f, 0.56f, 0.06f, 0.08f, 0x1E293B)
            }
            AccessoryType.HALO_RING -> {
                val bob = sin(animPhase * 1.5f) * 0.04f
                meshes.drawPart(shader, meshes.prism, vpMatrix, headMatrix, 0f, 0.42f + bob, 0f, 0f, 0f, 0f, 0.52f, 0.05f, 0.52f, 0xFFD166, emissive = 0.55f)
            }
            else -> {}
        }

        // 8. Shoe/Trail Sparkles during gameplay
        if (showTrail && (pose == CharacterPose.RUNNING || pose == CharacterPose.JUMPING)) {
            val trailRgb = when (appearance.trailEffect) {
                TrailEffect.CYAN_SPARKS -> 0x00E5FFL
                TrailEffect.CORAL_FLAME -> 0xFF4D6DL
                TrailEffect.GOLD_STARDUST -> 0xFFD166L
                TrailEffect.PRISM_WAVE -> if ((animPhase.toInt() % 2) == 0) 0x00E5FFL else 0xFF2A85L
            }
            val pOffset = (abs(sin(animPhase * 2f)) * 0.25f)
            meshes.drawPart(shader, meshes.box, vpMatrix, rootMatrix, -0.16f, 0.10f, -0.35f - pOffset, 0f, 0f, 0f, 0.10f, 0.10f, 0.28f, trailRgb, alpha = 0.85f, emissive = 0.65f)
            meshes.drawPart(shader, meshes.box, vpMatrix, rootMatrix, 0.16f, 0.10f, -0.45f - pOffset, 0f, 0f, 0f, 0.10f, 0.10f, 0.28f, trailRgb, alpha = 0.85f, emissive = 0.65f)
        }

        // 9. Energy Shield Bubble
        if (hasShield) {
            val pulse = 1.0f + sin(animPhase * 2.5f) * 0.05f
            meshes.drawPart(
                shader, meshes.prism, vpMatrix, rootMatrix,
                0f, 0.95f, 0f, 0f, animPhase * 30f, 0f,
                1.25f * pulse, 1.95f * pulse, 1.25f * pulse,
                0x00E5FF, alpha = 0.25f, emissive = 0.45f
            )
        }
    }

    private fun drawLimbWithShoe(
        shader: ShaderProgram,
        vpMatrix: FloatArray,
        parent: FloatArray,
        x: Float, y: Float, swingDeg: Float,
        pantsColor: Long,
        shoeColor: Long,
        accentColor: Long,
        shoeStyle: ShoeStyle
    ) {
        val limbM = FloatArray(16)
        System.arraycopy(parent, 0, limbM, 0, 16)
        Matrix.translateM(limbM, 0, x, y, 0f)
        Matrix.rotateM(limbM, 0, swingDeg, 1f, 0f, 0f)

        // Leg segment
        meshes.drawPart(shader, meshes.box, vpMatrix, limbM, 0f, -0.22f, 0f, 0f, 0f, 0f, 0.19f, 0.44f, 0.20f, pantsColor)
        // Shoe base
        meshes.drawPart(shader, meshes.box, vpMatrix, limbM, 0f, -0.46f, 0.07f, 0f, 0f, 0f, 0.22f, 0.14f, 0.34f, shoeColor)
        // Shoe accent sole / rocket fin
        val soleColor = if (shoeStyle == ShoeStyle.TURBO_SNEAKERS) accentColor else 0x00E5FF
        meshes.drawPart(shader, meshes.box, vpMatrix, limbM, 0f, -0.52f, 0.07f, 0f, 0f, 0f, 0.23f, 0.05f, 0.36f, soleColor, emissive = 0.3f)
        if (shoeStyle == ShoeStyle.HOVER_BOOTS || shoeStyle == ShoeStyle.ROCKET_KICKS) {
            meshes.drawPart(shader, meshes.cone, vpMatrix, limbM, 0f, -0.46f, -0.14f, -90f, 0f, 0f, 0.12f, 0.16f, 0.12f, accentColor, emissive = 0.4f)
        }
    }

    private fun drawArm(
        shader: ShaderProgram,
        vpMatrix: FloatArray,
        parent: FloatArray,
        x: Float, swingDeg: Float,
        sleeveColor: Long,
        skinColor: Long
    ) {
        val armM = FloatArray(16)
        System.arraycopy(parent, 0, armM, 0, 16)
        Matrix.translateM(armM, 0, x, 0.22f, 0f)
        Matrix.rotateM(armM, 0, swingDeg, 1f, 0f, 0f)
        meshes.drawPart(shader, meshes.box, vpMatrix, armM, 0f, -0.18f, 0f, 0f, 0f, 0f, 0.16f, 0.38f, 0.18f, sleeveColor)
        meshes.drawPart(shader, meshes.prism, vpMatrix, armM, 0f, -0.41f, 0f, 0f, 0f, 0f, 0.15f, 0.14f, 0.15f, skinColor)
    }

    private fun drawHair(
        shader: ShaderProgram,
        vpMatrix: FloatArray,
        headM: FloatArray,
        appearance: CharacterAppearance
    ) {
        val hc = appearance.hairColorRgb
        when (appearance.hairStyle) {
            HairStyle.SPIKY -> {
                meshes.drawPart(shader, meshes.cone, vpMatrix, headM, 0f, 0.34f, 0.02f, 10f, 0f, 0f, 0.22f, 0.28f, 0.22f, hc)
                meshes.drawPart(shader, meshes.cone, vpMatrix, headM, -0.14f, 0.31f, -0.02f, 15f, 0f, 22f, 0.18f, 0.24f, 0.18f, hc)
                meshes.drawPart(shader, meshes.cone, vpMatrix, headM, 0.14f, 0.31f, -0.02f, 15f, 0f, -22f, 0.18f, 0.24f, 0.18f, hc)
                meshes.drawPart(shader, meshes.box, vpMatrix, headM, 0f, 0.08f, -0.12f, 0f, 0f, 0f, 0.56f, 0.40f, 0.38f, hc)
            }
            HairStyle.SLEEK_BOB -> {
                meshes.drawPart(shader, meshes.prism, vpMatrix, headM, 0f, 0.24f, -0.02f, 0f, 0f, 0f, 0.58f, 0.20f, 0.58f, hc)
                meshes.drawPart(shader, meshes.box, vpMatrix, headM, 0f, 0.02f, -0.16f, 0f, 0f, 0f, 0.58f, 0.44f, 0.30f, hc)
            }
            HairStyle.CYBER_MOHAWK -> {
                meshes.drawPart(shader, meshes.box, vpMatrix, headM, 0f, 0.36f, -0.02f, 0f, 0f, 0f, 0.14f, 0.28f, 0.54f, hc, emissive = 0.35f)
            }
            HairStyle.CURLY_AFRO -> {
                meshes.drawPart(shader, meshes.prism, vpMatrix, headM, 0f, 0.26f, -0.08f, 0f, 0f, 0f, 0.68f, 0.36f, 0.64f, hc)
                meshes.drawPart(shader, meshes.prism, vpMatrix, headM, -0.24f, 0.14f, -0.12f, 0f, 0f, 0f, 0.30f, 0.30f, 0.30f, hc)
                meshes.drawPart(shader, meshes.prism, vpMatrix, headM, 0.24f, 0.14f, -0.12f, 0f, 0f, 0f, 0.30f, 0.30f, 0.30f, hc)
            }
            HairStyle.PONYTAIL -> {
                meshes.drawPart(shader, meshes.prism, vpMatrix, headM, 0f, 0.24f, -0.02f, 0f, 0f, 0f, 0.56f, 0.18f, 0.56f, hc)
                meshes.drawPart(shader, meshes.cone, vpMatrix, headM, 0f, 0.10f, -0.34f, -125f, 0f, 0f, 0.24f, 0.44f, 0.24f, hc)
            }
            HairStyle.RUNNER_CAP -> {
                meshes.drawPart(shader, meshes.prism, vpMatrix, headM, 0f, 0.24f, 0f, 0f, 0f, 0f, 0.58f, 0.20f, 0.58f, hc)
                meshes.drawPart(shader, meshes.box, vpMatrix, headM, 0f, 0.16f, 0.30f, 8f, 0f, 0f, 0.50f, 0.05f, 0.24f, appearance.accentColorRgb)
            }
            HairStyle.ASTRO_HELMET -> {
                meshes.drawPart(shader, meshes.prism, vpMatrix, headM, 0f, 0.22f, -0.04f, 0f, 0f, 0f, 0.64f, 0.28f, 0.64f, hc)
                meshes.drawPart(shader, meshes.box, vpMatrix, headM, 0.28f, 0.38f, 0f, 0f, 0f, 0f, 0.04f, 0.24f, 0.04f, appearance.accentColorRgb, emissive = 0.4f)
            }
            HairStyle.ROYAL_CROWN -> {
                meshes.drawPart(shader, meshes.prism, vpMatrix, headM, 0f, 0.30f, 0f, 0f, 0f, 0f, 0.48f, 0.14f, 0.48f, 0xFFD166, emissive = 0.3f)
                meshes.drawPart(shader, meshes.cone, vpMatrix, headM, 0f, 0.42f, 0.18f, 0f, 0f, 0f, 0.12f, 0.16f, 0.12f, 0xFFD166, emissive = 0.35f)
                meshes.drawPart(shader, meshes.cone, vpMatrix, headM, -0.18f, 0.42f, 0f, 0f, 0f, 0f, 0.12f, 0.16f, 0.12f, 0xFFD166, emissive = 0.35f)
                meshes.drawPart(shader, meshes.cone, vpMatrix, headM, 0.18f, 0.42f, 0f, 0f, 0f, 0f, 0.12f, 0.16f, 0.12f, 0xFFD166, emissive = 0.35f)
            }
        }
    }

    private fun darkenRgb(rgb: Long, factor: Float): Long {
        val r = (((rgb shr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = (((rgb shr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((rgb and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }
}
