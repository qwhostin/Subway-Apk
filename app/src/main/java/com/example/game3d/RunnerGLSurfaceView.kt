package com.example.game3d

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.abs

/**
 * Custom OpenGL ES 3.0 GLSurfaceView for RUNVANTA gameplay with responsive swipe detection
 * (adjustable sensitivity & left/right handed support).
 */
class RunnerGLSurfaceView(
    context: Context,
    private val engine: RunnerWorldEngine,
    val renderer: RunnerGLRenderer
) : GLSurfaceView(context) {

    var swipeSensitivity: Float = 1.0f
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var gestureHandled: Boolean = false

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val threshold = 58f / swipeSensitivity.coerceIn(0.5f, 2.0f)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                gestureHandled = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (gestureHandled) return true
                val dx = event.x - downX
                val dy = event.y - downY
                if (abs(dx) > threshold || abs(dy) > threshold) {
                    gestureHandled = true
                    if (abs(dx) > abs(dy)) {
                        if (dx > 0) engine.moveRight() else engine.moveLeft()
                    } else {
                        if (dy < 0) engine.jump() else engine.slide()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
