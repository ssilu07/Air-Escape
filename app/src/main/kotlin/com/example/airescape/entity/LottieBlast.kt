package com.example.airescape.entity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieDrawable
import com.example.airescape.util.Vector2

/**
 * An animated explosion using a Lottie JSON animation.
 *
 * All Lottie frames are **pre-rendered to Bitmaps** at load time
 * so that drawing during the game loop is just a simple drawBitmap —
 * this works reliably on a SurfaceView Canvas unlike LottieDrawable.draw().
 */
class LottieBlast(
    /** Center of the blast in world coordinates. */
    val position: Vector2,
    /** Rendered size (width & height) in world pixels. */
    val size: Float = 350f
) {
    /** How long this blast has been alive (seconds). */
    var elapsed: Float = 0f

    /** Total animation duration (seconds). */
    var duration: Float = frameDuration

    /** False once the animation has finished playing. */
    var alive: Boolean = true
        private set

    /** Current animation progress 0..1. */
    val progress: Float
        get() = (elapsed / duration).coerceIn(0f, 1f)

    fun update(dt: Float) {
        elapsed += dt
        if (elapsed >= duration) {
            alive = false
        }
    }

    companion object {
        /** Pre-rendered animation frames. */
        private var frames: Array<Bitmap>? = null
        private var frameCount: Int = 0
        private var frameDuration: Float = 0.667f

        /** Reusable Paint & RectF for drawing — avoids GC. */
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val destRect = RectF()

        /** Whether frames have been pre-rendered. */
        val isReady: Boolean get() = frames != null

        /**
         * Pre-render every frame of the Lottie animation into bitmaps.
         * Call once during game init.
         */
        fun preRenderFrames(composition: LottieComposition) {
            val drawable = LottieDrawable()
            drawable.composition = composition

            val renderSize = 256 // px per frame bitmap
            val totalFrames = ((composition.endFrame - composition.startFrame)).toInt().coerceAtLeast(1)
            frameDuration = composition.duration / 1000f // ms -> seconds

            val bitmaps = Array(totalFrames) { i ->
                val bitmap = Bitmap.createBitmap(renderSize, renderSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, renderSize, renderSize)
                drawable.progress = i.toFloat() / (totalFrames - 1).coerceAtLeast(1)
                drawable.draw(canvas)
                bitmap
            }

            frames = bitmaps
            frameCount = totalFrames
        }

        /**
         * Render a single blast onto [canvas] using pre-rendered bitmap frames.
         */
        fun renderBlast(canvas: Canvas, blast: LottieBlast) {
            val framesArr = frames ?: return
            if (frameCount == 0) return

            val frameIndex = (blast.progress * (frameCount - 1)).toInt().coerceIn(0, frameCount - 1)
            val bitmap = framesArr[frameIndex]

            val half = blast.size / 2f
            destRect.set(
                blast.position.x - half,
                blast.position.y - half,
                blast.position.x + half,
                blast.position.y + half
            )
            canvas.drawBitmap(bitmap, null, destRect, paint)
        }
    }
}
