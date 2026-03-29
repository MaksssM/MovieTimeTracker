package com.example.movietime.ui.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * Animated nebula background: floating particles, pulsing gradient orbs,
 * subtle grid lines, and occasional shooting stars.
 * Runs a continuous render loop via postInvalidateOnAnimation().
 */
class NebulaBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Paint objects (pre-allocated) ────────────────────────────────────
    private val basePaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.5f }
    private val shootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }

    // ── Time ─────────────────────────────────────────────────────────────
    private var startTime     = System.nanoTime()
    private var lastFrameNanos = startTime
    private val elapsed: Float get() = (System.nanoTime() - startTime) / 1_000_000_000f

    init { setLayerType(LAYER_TYPE_NONE, null) }

    // ── Theme ─────────────────────────────────────────────────────────────
    private val isDark: Boolean
        get() = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    // ── Orbs ──────────────────────────────────────────────────────────────
    private data class Orb(
        val cx: Float, val cy: Float,
        val radiusFraction: Float,
        val colorCenter: Int,
        val colorEdge: Int = Color.TRANSPARENT,
        val speedX: Float, val speedY: Float,
        val pulsePeriod: Float, val pulseAmp: Float,
        val phaseOffset: Float
    )

    private var orbs: List<Orb> = emptyList()

    // ── Particles ─────────────────────────────────────────────────────────
    private data class Particle(
        var x: Float, var y: Float,
        val radius: Float,
        val speedX: Float, val speedY: Float,
        val alpha: Float,
        val twinklePeriod: Float, val twinklePhase: Float
    )
    private val particles = mutableListOf<Particle>()

    // ── Shooting Stars ────────────────────────────────────────────────────
    private data class ShootingStar(
        var x: Float, var y: Float,          // head position (relative 0..1)
        val angle: Float,                     // travel direction radians
        val speed: Float,                     // relative/sec
        val length: Float,                    // tail length (relative)
        var progress: Float,                  // 0..1 lifetime progress
        val duration: Float,                  // seconds for full crossing
        var active: Boolean = true
    )
    private val shootingStars = mutableListOf<ShootingStar>()
    private var nextStarIn = 3f   // seconds until next star spawns

    // ── Grid ──────────────────────────────────────────────────────────────
    private var gridAlpha = 0

    // ── State ─────────────────────────────────────────────────────────────
    private var screenW = 0f
    private var screenH = 0f
    private var initialized = false

    // ── Init helpers ──────────────────────────────────────────────────────
    private fun buildOrbs(dark: Boolean): List<Orb> = if (dark) listOf(
        Orb(0.05f, 0.08f, 1.15f, 0x503B82F6.toInt(), phaseOffset = 0f,   speedX =  0.006f, speedY =  0.004f, pulsePeriod = 7f,  pulseAmp = 0.12f),
        Orb(0.95f, 0.87f, 0.95f, 0x408B5CF6.toInt(), phaseOffset = 1.1f, speedX = -0.005f, speedY = -0.003f, pulsePeriod = 9f,  pulseAmp = 0.10f),
        Orb(0.18f, 0.50f, 0.65f, 0x30EC4899.toInt(), phaseOffset = 2.3f, speedX =  0.004f, speedY = -0.005f, pulsePeriod = 6f,  pulseAmp = 0.15f),
        Orb(0.62f, 0.55f, 0.60f, 0x2810B981.toInt(), phaseOffset = 3.7f, speedX = -0.003f, speedY =  0.006f, pulsePeriod = 11f, pulseAmp = 0.08f),
        Orb(0.90f, 0.08f, 0.55f, 0x207C3AED.toInt(), phaseOffset = 5.1f, speedX =  0.002f, speedY =  0.003f, pulsePeriod = 8f,  pulseAmp = 0.10f),
    ) else listOf(
        Orb(0.05f, 0.08f, 1.10f, 0x283B82F6.toInt(), phaseOffset = 0f,   speedX =  0.005f, speedY =  0.003f, pulsePeriod = 8f,  pulseAmp = 0.12f),
        Orb(0.95f, 0.87f, 0.85f, 0x228B5CF6.toInt(), phaseOffset = 1.4f, speedX = -0.004f, speedY = -0.003f, pulsePeriod = 10f, pulseAmp = 0.10f),
        Orb(0.42f, 0.35f, 0.60f, 0x20EC4899.toInt(), phaseOffset = 2.6f, speedX =  0.003f, speedY = -0.004f, pulsePeriod = 7f,  pulseAmp = 0.14f),
        Orb(0.88f, 0.42f, 0.55f, 0x1806B6D4.toInt(), phaseOffset = 4.0f, speedX = -0.003f, speedY =  0.005f, pulsePeriod = 12f, pulseAmp = 0.08f),
        Orb(0.92f, 0.06f, 0.50f, 0x166366F1.toInt(), phaseOffset = 5.5f, speedX =  0.002f, speedY =  0.003f, pulsePeriod = 9f,  pulseAmp = 0.10f),
    )

    private fun initParticles(w: Float, h: Float, dark: Boolean) {
        particles.clear()
        val count = if (dark) 60 else 35
        val rnd = Random.Default
        repeat(count) {
            particles += Particle(
                x = rnd.nextFloat() * w, y = rnd.nextFloat() * h,
                radius = rnd.nextFloat() * 2.2f + 0.8f,
                speedX = (rnd.nextFloat() - 0.5f) * 18f,
                speedY = (rnd.nextFloat() - 0.5f) * 18f,
                alpha = rnd.nextFloat() * 0.35f + (if (dark) 0.25f else 0.12f),
                twinklePeriod = rnd.nextFloat() * 3f + 2f,
                twinklePhase  = rnd.nextFloat() * PI.toFloat() * 2f
            )
        }
    }

    private fun spawnShootingStar() {
        val rnd = Random.Default
        // Start from top edge or left edge, travel diagonally down-right
        val angle = (PI / 4 + (rnd.nextFloat() - 0.5) * 0.4).toFloat()  // ~45° ± small variance
        val startX = rnd.nextFloat()            // relative [0..1]
        val startY = -0.05f                      // just above screen
        shootingStars += ShootingStar(
            x = startX, y = startY,
            angle = angle,
            speed = rnd.nextFloat() * 0.35f + 0.25f,
            length = rnd.nextFloat() * 0.12f + 0.08f,
            progress = 0f,
            duration = rnd.nextFloat() * 1.2f + 0.6f
        )
        // Next star in 4-10 seconds
        nextStarIn = rnd.nextFloat() * 6f + 4f
    }

    // ── Layout ────────────────────────────────────────────────────────────
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenW = w.toFloat(); screenH = h.toFloat()
        val dark = isDark
        orbs = buildOrbs(dark)
        initParticles(screenW, screenH, dark)
        gridAlpha = if (dark) 8 else 6
        initialized = true
    }

    // ── Draw ──────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        if (!initialized || screenW == 0f) return

        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        lastFrameNanos = now
        val t = elapsed
        val dark = isDark

        // 1. Base fill
        basePaint.color = if (dark) 0xFF080B12.toInt() else 0xFFFCF5FF.toInt()
        canvas.drawRect(0f, 0f, screenW, screenH, basePaint)

        val minDim = min(screenW, screenH)

        // 2. Grid — subtle perspective dots
        val gridSpacing = 55f
        gridPaint.color = if (dark) Color.argb(gridAlpha, 100, 140, 255)
                          else Color.argb(gridAlpha, 80, 80, 200)
        var gx = 0f
        while (gx < screenW) {
            var gy = 0f
            while (gy < screenH) {
                canvas.drawPoint(gx, gy, gridPaint)
                gy += gridSpacing
            }
            gx += gridSpacing
        }

        // 3. Animated orbs
        for (orb in orbs) {
            val driftX = sin((t * orb.speedX * PI).toFloat() + orb.phaseOffset) * 0.18f * screenW
            val driftY = cos((t * orb.speedY * PI).toFloat() + orb.phaseOffset + 1f) * 0.18f * screenH
            val cx = orb.cx * screenW + driftX
            val cy = orb.cy * screenH + driftY
            val pulse = 1f + orb.pulseAmp * sin((t / orb.pulsePeriod * 2f * PI + orb.phaseOffset).toFloat())
            val r = orb.radiusFraction * minDim * pulse
            orbPaint.shader = RadialGradient(cx, cy, r, intArrayOf(orb.colorCenter, orb.colorEdge),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, orbPaint)
        }

        // 4. Particles
        val baseAlpha = if (dark) 1f else 0.7f
        for (p in particles) {
            p.x += p.speedX * dt; p.y += p.speedY * dt
            if (p.x < -4f) p.x += screenW + 8f
            if (p.x > screenW + 4f) p.x -= screenW + 8f
            if (p.y < -4f) p.y += screenH + 8f
            if (p.y > screenH + 4f) p.y -= screenH + 8f
            val twinkle = 0.5f + 0.5f * sin((t / p.twinklePeriod * 2f * PI + p.twinklePhase).toFloat())
            val a = (p.alpha * twinkle * baseAlpha * 255).toInt().coerceIn(0, 255)
            starPaint.color = if (dark) Color.argb(a, 180, 200, 255) else Color.argb(a, 100, 80, 180)
            canvas.drawCircle(p.x, p.y, p.radius, starPaint)
        }

        // 5. Shooting stars
        nextStarIn -= dt
        if (nextStarIn <= 0f) spawnShootingStar()

        val starsIter = shootingStars.iterator()
        while (starsIter.hasNext()) {
            val s = starsIter.next()
            s.progress += dt / s.duration
            if (s.progress >= 1.2f) { starsIter.remove(); continue }

            s.x += cos(s.angle) * s.speed * dt
            s.y += sin(s.angle) * s.speed * dt

            // Fade in then fade out
            val fade = when {
                s.progress < 0.15f -> s.progress / 0.15f
                s.progress > 0.75f -> (1f - s.progress) / 0.25f
                else -> 1f
            }.coerceIn(0f, 1f)

            val headX = s.x * screenW
            val headY = s.y * screenH
            val tailLen = s.length * minDim
            val tailX = headX - cos(s.angle) * tailLen
            val tailY = headY - sin(s.angle) * tailLen

            val alpha = (fade * (if (dark) 220 else 160)).toInt()
            shootPaint.strokeWidth = if (dark) 2.5f else 1.8f
            shootPaint.shader = LinearGradient(
                tailX, tailY, headX, headY,
                Color.TRANSPARENT,
                if (dark) Color.argb(alpha, 200, 220, 255) else Color.argb(alpha, 150, 130, 230),
                Shader.TileMode.CLAMP
            )
            canvas.drawLine(tailX, tailY, headX, headY, shootPaint)
        }

        postInvalidateOnAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            startTime = System.nanoTime()
            lastFrameNanos = startTime
            invalidate()
        }
    }
}
