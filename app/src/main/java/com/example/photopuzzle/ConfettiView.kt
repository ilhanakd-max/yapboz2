package com.example.photopuzzle

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var size: Float,
        var color: Int,
        var velocityY: Float
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = mutableListOf<Particle>()
    private var animator: ValueAnimator? = null

    fun start() {
        visibility = VISIBLE
        createParticles()
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        visibility = GONE
    }

    private fun createParticles() {
        particles.clear()
        val width = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val height = height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        val colors = listOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.parseColor("#FFA500")
        )
        repeat(80) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * width,
                    y = Random.nextFloat() * height / 3f,
                    size = 10f + Random.nextFloat() * 10f,
                    color = colors.random(),
                    velocityY = 3f + Random.nextFloat() * 6f
                )
            )
        }
    }

    private fun updateParticles() {
        val height = height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        particles.forEach { particle ->
            particle.y += particle.velocityY
            if (particle.y > height) {
                particle.y = 0f
                particle.x = Random.nextFloat() * (width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        particles.forEach { particle ->
            paint.color = particle.color
            canvas.drawCircle(particle.x, particle.y, particle.size, paint)
        }
    }
}
