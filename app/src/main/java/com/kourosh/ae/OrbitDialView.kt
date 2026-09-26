package com.kourosh.ae

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.os.SystemClock
import android.text.TextPaint
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The imperial connect dial, Kourosh-AE 3.0.
 *
 * Layers, outermost first:
 * 1. drop shadow and a breathing halo in the state accent,
 * 2. two ripple rings that expand and fade (connected only),
 * 3. a crest of gold rays around the bezel,
 * 4. the ornate gold bezel: metal sweep, engraved ticks that light up in the
 *    accent as the tunnel comes up, and four cardinal studs,
 * 5. the neon state ring, with a comet that sweeps while connecting,
 * 6. an inner gold ring carrying the real connect progress,
 * 7. the dark glass core with a holographic wireframe globe,
 * 8. contents: a code-drawn crown emblem, then the power glyph and
 *    "TAP TO CONNECT" when down, a radar sweep and "CONNECTING" while
 *    negotiating, and "CONNECTED" plus the session timer when up.
 *
 * GEOMETRY: the halo, crest and ripples grow beyond the ring, so every radius
 * is derived from [RING_DP] inside a square that reserves [BLEED_DP] on each
 * side. Nothing is clipped by the parent.
 *
 * Accents: gold when idle, amber while connecting or degraded, cyan when
 * connected, danger after a failure.
 */
class OrbitDialView(
    context: Context,
    private var palette: AppAppearance.Palette,
) : View(context) {

    enum class State { DISCONNECTED, CONNECTING, CONNECTED, DEGRADED, FAILED }

    var state: State = State.DISCONNECTED
        set(value) {
            val previous = field
            field = value
            contentDescription = when (value) {
                State.DISCONNECTED, State.FAILED -> "اتصال"
                State.CONNECTING -> "در حال اتصال"
                State.CONNECTED, State.DEGRADED -> "قطع اتصال"
            }
            if (value == State.CONNECTED || value == State.DEGRADED) {
                if (previous != State.CONNECTED && previous != State.DEGRADED) tickReveal = 0f
                animateTickReveal()
            } else {
                tickAnimator?.cancel()
                tickReveal = 0f
            }
            // Restarts the loop when the tempo changes (connecting is fast).
            startLoop()
            invalidate()
        }

    /** Session uptime text drawn inside the core. Empty hides it. */
    var timerText: String = ""
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /**
     * Connect progress, 0..100, or -1 for "no measurable progress". Only drawn
     * in [State.CONNECTING], and only when non-negative.
     */
    var progressPercent: Int = -1
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** Shrinks the dial on short screens. Clamped to [MIN_SIZE_SCALE]..1. */
    var sizeScale: Float = 1f
        set(value) {
            val next = value.coerceIn(MIN_SIZE_SCALE, 1f)
            if (field == next) return
            field = next
            requestLayout()
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val light = Sculpt.lighting
    private val density = resources.displayMetrics.density
    private val arcRect = RectF()
    private val ovalRect = RectF()
    private val corePath = Path()
    private val shaderMatrix = Matrix()

    private var loopFraction = 0f
    private var pulse = 0f
    private var tickReveal = 0f
    private var loopAnimator: ValueAnimator? = null
    private var tickAnimator: ValueAnimator? = null
    private var lastFrameAt = 0L
    private var pressedInside = false
    private var lastRing = 0f

    private var dark = true
    private var gh = DARK_GOLD_HI
    private var gm = DARK_GOLD
    private var gl = DARK_GOLD_LO
    private var groove = DARK_GROOVE

    private val monoTypeface: Typeface = Typefaces.mono(context)
    private val labelTypeface: Typeface
        get() = Typefaces.medium(context)
    private val boldTypeface: Typeface
        get() = Typefaces.bold(context)

    init {
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = false
        contentDescription = "اتصال"
        // Shadow layers on text and strokes need software rendering on older
        // GPUs. Redraws are capped at ~30fps.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun applyPalette(next: AppAppearance.Palette) {
        palette = next
        invalidate()
    }

    private fun accentFor(value: State): Int = when (value) {
        State.DISCONNECTED -> palette.primary
        State.CONNECTING -> palette.amber
        State.CONNECTED -> palette.connected
        State.DEGRADED -> palette.amber
        State.FAILED -> palette.danger
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = dp(((RING_DP + BLEED_DP) * 2 * sizeScale).roundToInt())
        val size = resolveSize(desired, widthMeasureSpec)
            .coerceAtMost(resolveSize(desired, heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private fun px(value: Float): Float = value * density

    // ---------------------------------------------------------------- drawing

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val half = min(w, h) / 2f
        val cx = w / 2f
        val cy = h / 2f
        val geo = half / ((RING_DP + BLEED_DP) * density)
        val ring = RING_DP * density * geo
        lastRing = ring
        val accent = accentFor(state)
        val active = state == State.CONNECTED || state == State.DEGRADED

        dark = AppAppearance.isDark(palette.canvas)
        gh = if (dark) DARK_GOLD_HI else LIGHT_GOLD_HI
        gm = if (dark) DARK_GOLD else LIGHT_GOLD
        gl = if (dark) DARK_GOLD_LO else LIGHT_GOLD_LO
        groove = if (dark) DARK_GROOVE else LIGHT_GROOVE

        drawHalo(canvas, cx, cy, ring, accent, geo, active)
        if (active) drawRipples(canvas, cx, cy, ring, accent, geo)
        drawCrest(canvas, cx, cy, ring)
        drawBezel(canvas, cx, cy, ring, accent, geo)
        drawNeonRing(canvas, cx, cy, ring, accent, geo)
        drawCore(canvas, cx, cy, ring, accent, geo, active)
        drawGlobe(canvas, cx, cy, ring * CORE_RATIO * 0.88f, accent, active, geo)
        drawContents(canvas, cx, cy, ring, accent, active, geo)

        if (isFocused) {
            stroke(Sculpt.withAlpha(accent, 0.9f), px(2f) * geo)
            canvas.drawCircle(cx, cy, ring * 1.13f, paint)
        }
    }

    private fun fill(color: Int): Paint {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = color
        return paint
    }

    private fun stroke(color: Int, strokeW: Float): Paint {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeW
        paint.color = color
        return paint
    }

    private fun goldSweep(cx: Float, cy: Float): Shader =
        SweepGradient(cx, cy, intArrayOf(gh, gl, gm, gh, gl, gm, gh), null)

    private fun drawHalo(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int, geo: Float, active: Boolean) {
        // Drop shadow under the whole medallion.
        val shadowR = ring * 1.12f
        fill(Color.BLACK)
        paint.shader = RadialGradient(
            cx, cy + ring * 0.05f, shadowR,
            intArrayOf(
                Sculpt.withAlpha(SHADOW_TINT, light.dialShadowAlpha),
                Sculpt.withAlpha(SHADOW_TINT, light.dialShadowAlpha),
                Sculpt.withAlpha(SHADOW_TINT, 0f),
            ),
            floatArrayOf(0f, 0.84f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy + ring * 0.05f, shadowR, paint)

        val outer = ring + px(HALO_OUTSET_DP + HALO_PULSE_DP * pulse) * geo
        val base = when {
            active -> 0.46f
            state == State.CONNECTING -> 0.34f
            state == State.FAILED -> 0.28f
            else -> 0.20f
        } * (if (dark) 1f else 0.6f)
        fill(accent)
        paint.shader = RadialGradient(
            cx, cy, outer,
            intArrayOf(
                Sculpt.withAlpha(accent, base),
                Sculpt.withAlpha(accent, base * 0.45f),
                Sculpt.withAlpha(accent, 0f),
            ),
            floatArrayOf(ring * 0.80f / outer, ring / outer, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, outer, paint)
        paint.shader = null
    }

    private fun drawRipples(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int, geo: Float) {
        for (i in 0 until 2) {
            val t = (loopFraction * 4f + i * 0.5f) % 1f
            val r = ring * (1f + RIPPLE_GROWTH * t)
            stroke(Sculpt.withAlpha(accent, 0.50f * (1f - t)), px(0.6f + 1.6f * (1f - t)) * geo)
            canvas.drawCircle(cx, cy, r, paint)
        }
    }

    /** Gold rays around the bezel: long every third, short between. */
    private fun drawCrest(canvas: Canvas, cx: Float, cy: Float, ring: Float) {
        corePath.reset()
        val count = 24
        for (i in 0 until count) {
            val a = -HALF_PI + TWO_PI * i / count
            val big = i % 3 == 0
            val tip = ring * (if (big) 1.085f else 1.045f)
            val base = ring * 0.99f
            val spread = if (big) 0.036f else 0.022f
            corePath.moveTo(cx + cos(a) * tip, cy + sin(a) * tip)
            corePath.lineTo(cx + cos(a + spread) * base, cy + sin(a + spread) * base)
            corePath.lineTo(cx + cos(a - spread) * base, cy + sin(a - spread) * base)
            corePath.close()
        }
        fill(gm)
        paint.shader = goldSweep(cx, cy)
        canvas.drawPath(corePath, paint)
        paint.shader = null
    }

    private fun drawBezel(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int, geo: Float) {
        val bandOuter = ring * 0.995f
        val bandInner = ring * 0.86f
        val bandW = bandOuter - bandInner
        val bandR = (bandOuter + bandInner) / 2f

        // Dark channel between the bezel and the core, where the neon glows.
        val channel = if (dark) Sculpt.darken(palette.canvas, 0.45f) else Sculpt.darken(palette.canvas, 0.10f)
        fill(channel)
        canvas.drawCircle(cx, cy, bandOuter, paint)

        // Backing shadow for depth, then the metal band.
        stroke(Sculpt.withAlpha(Color.BLACK, if (dark) 0.55f else 0.20f), bandW + px(2.5f) * geo)
        canvas.drawCircle(cx, cy, bandR, paint)
        stroke(gm, bandW)
        paint.shader = goldSweep(cx, cy)
        canvas.drawCircle(cx, cy, bandR, paint)
        // Light from above: bright top, shaded bottom.
        paint.shader = LinearGradient(
            0f, cy - ring, 0f, cy + ring,
            intArrayOf(
                Sculpt.withAlpha(Color.WHITE, 0.30f),
                Sculpt.withAlpha(Color.WHITE, 0f),
                Sculpt.withAlpha(Color.BLACK, 0.30f),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, bandR, paint)
        paint.shader = null

        // Edge lines and an engraved groove.
        stroke(gh, px(1f) * geo)
        canvas.drawCircle(cx, cy, bandOuter, paint)
        stroke(gl, px(1f) * geo)
        canvas.drawCircle(cx, cy, bandInner, paint)
        stroke(Sculpt.withAlpha(groove, 0.65f), px(0.8f) * geo)
        canvas.drawCircle(cx, cy, bandInner + bandW * 0.24f, paint)

        // Engraved ticks, lit in the accent as the tunnel comes up.
        val lit = litTicks()
        for (i in 0 until TICK_COUNT) {
            val a = -HALF_PI + TWO_PI * i / TICK_COUNT
            val major = i % 5 == 0
            val r1 = if (major) bandInner + bandW * 0.30f else bandInner + bandW * 0.42f
            val r2 = bandOuter - bandW * 0.14f
            val c = cos(a)
            val s = sin(a)
            if (i < lit) {
                stroke(Sculpt.withAlpha(accent, 0.35f), px(4f) * geo)
                paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(cx + c * r1, cy + s * r1, cx + c * r2, cy + s * r2, paint)
                stroke(Sculpt.lighten(accent, if (dark) 0.25f else 0f), px(if (major) 1.8f else 1.4f) * geo)
            } else {
                stroke(Sculpt.withAlpha(groove, 0.80f), px(if (major) 1.6f else 1.1f) * geo)
            }
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(cx + c * r1, cy + s * r1, cx + c * r2, cy + s * r2, paint)
        }

        // Four cardinal studs.
        val s = bandW * 0.40f
        for (k in 0 until 4) {
            val a = -HALF_PI + HALF_PI * k
            val x = cx + cos(a) * bandR
            val y = cy + sin(a) * bandR
            diamond(x, y, s)
            fill(groove)
            canvas.drawPath(corePath, paint)
            diamond(x, y, s * 0.72f)
            fill(gh)
            canvas.drawPath(corePath, paint)
            fill(if (state == State.DISCONNECTED) palette.connected else accent)
            canvas.drawCircle(x, y, s * 0.26f, paint)
        }
    }

    private fun diamond(x: Float, y: Float, s: Float) {
        corePath.reset()
        corePath.moveTo(x, y - s)
        corePath.lineTo(x + s * 0.72f, y)
        corePath.lineTo(x, y + s)
        corePath.lineTo(x - s * 0.72f, y)
        corePath.close()
    }

    private fun litTicks(): Int = when (state) {
        State.CONNECTED, State.DEGRADED -> (TICK_LIT * tickReveal).roundToInt()
        State.CONNECTING -> if (progressPercent in 0..100) (TICK_COUNT * progressPercent / 100f).roundToInt() else 0
        else -> 0
    }

    private fun drawNeonRing(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int, geo: Float) {
        val nr = ring * 0.815f
        val scale = if (dark) 1f else 0.65f
        val widths = floatArrayOf(12f, 6.5f, 3.4f)
        val alphas = floatArrayOf(0.08f, 0.17f, 0.40f)
        for (i in widths.indices) {
            stroke(Sculpt.withAlpha(accent, alphas[i] * scale), px(widths[i]) * geo)
            canvas.drawCircle(cx, cy, nr, paint)
        }
        stroke(if (dark) Sculpt.lighten(accent, 0.35f) else accent, px(1.8f) * geo)
        canvas.drawCircle(cx, cy, nr, paint)

        if (state == State.CONNECTING) {
            val start = loopFraction * 360f - 90f
            arcRect.set(cx - nr, cy - nr, cx + nr, cy + nr)
            val comet = SweepGradient(
                cx, cy,
                intArrayOf(Sculpt.withAlpha(accent, 0f), Sculpt.lighten(accent, 0.3f), Sculpt.withAlpha(accent, 0f)),
                floatArrayOf(0f, 0.25f, 0.26f),
            )
            shaderMatrix.setRotate(start, cx, cy)
            comet.setLocalMatrix(shaderMatrix)
            stroke(accent, px(4f) * geo)
            paint.strokeCap = Paint.Cap.ROUND
            paint.shader = comet
            canvas.drawArc(arcRect, start, 90f, false, paint)
            paint.shader = null
        }

        // Inner gold ring, carrying real progress while connecting.
        val ir = ring * 0.765f
        stroke(gm, px(1.6f) * geo)
        canvas.drawCircle(cx, cy, ir, paint)
        if (state == State.CONNECTING && progressPercent in 0..100) {
            arcRect.set(cx - ir, cy - ir, cx + ir, cy + ir)
            stroke(accent, px(2.6f) * geo)
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(arcRect, -90f, progressPercent * 3.6f, false, paint)
        }
    }

    private fun drawCore(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int, geo: Float, active: Boolean) {
        val cr = ring * CORE_RATIO
        val center = if (dark) Sculpt.blend(palette.surface, accent, 0.12f) else Sculpt.lighten(palette.surface, 0.6f)
        val midTone = if (dark) Sculpt.darken(palette.surface, 0.15f) else Sculpt.blend(palette.surface, palette.canvas, 0.5f)
        val edge = if (dark) Sculpt.darken(palette.canvas, 0.35f) else Sculpt.blend(palette.canvas, gm, 0.12f)
        fill(center)
        paint.shader = RadialGradient(
            cx, cy - cr * 0.30f, cr * 1.30f,
            intArrayOf(center, midTone, edge),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, cr, paint)

        // Inner shadow at the bottom.
        paint.shader = LinearGradient(
            0f, cy + cr * 0.15f, 0f, cy + cr,
            Sculpt.withAlpha(light.dialInnerShadowColor, 0f),
            Sculpt.withAlpha(light.dialInnerShadowColor, light.dialInnerShadow),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, cr, paint)

        // Glass specular across the top.
        paint.shader = LinearGradient(
            0f, cy - cr, 0f, cy - cr * 0.05f,
            Sculpt.withAlpha(Color.WHITE, light.dialSpecular),
            Sculpt.withAlpha(Color.WHITE, 0f),
            Shader.TileMode.CLAMP,
        )
        ovalRect.set(cx - cr * 0.78f, cy - cr * 0.95f, cx + cr * 0.78f, cy - cr * 0.05f)
        canvas.drawOval(ovalRect, paint)
        paint.shader = null

        // Travelling sheen band, connected only.
        if (active && light.dialSheen > 0f) {
            corePath.reset()
            corePath.addCircle(cx, cy, cr, Path.Direction.CW)
            val saved = canvas.save()
            canvas.clipPath(corePath)
            val x = cx - cr * 1.6f + cr * 3.2f * ((loopFraction * 1.6f) % 1f)
            fill(Color.WHITE)
            paint.shader = LinearGradient(
                x - cr * 0.35f, 0f, x + cr * 0.35f, 0f,
                intArrayOf(
                    Sculpt.withAlpha(Color.WHITE, 0f),
                    Sculpt.withAlpha(Color.WHITE, light.dialSheen),
                    Sculpt.withAlpha(Color.WHITE, 0f),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(cx - cr, cy - cr, cx + cr, cy + cr, paint)
            paint.shader = null
            canvas.restoreToCount(saved)
        }

        // Gold rim.
        stroke(gh, px(1.2f) * geo)
        canvas.drawCircle(cx, cy, cr, paint)
    }

    /** A holographic wireframe globe behind the emblem. */
    private fun drawGlobe(canvas: Canvas, cx: Float, cy: Float, r: Float, accent: Int, active: Boolean, geo: Float) {
        val base = (if (active) 0.30f else 0.15f) * (if (dark) 1f else 0.75f)
        val lineW = px(0.9f) * geo
        stroke(Sculpt.withAlpha(accent, base), lineW)
        canvas.drawCircle(cx, cy, r, paint)
        // Latitudes.
        for (k in -2..2) {
            val f = k / 3f
            val y = cy + r * f
            val hw = r * sqrt(1f - f * f)
            ovalRect.set(cx - hw, y - hw * 0.16f, cx + hw, y + hw * 0.16f)
            stroke(Sculpt.withAlpha(accent, if (k == 0) base * 1.5f else base), lineW)
            canvas.drawOval(ovalRect, paint)
        }
        // Longitudes, turning with the loop.
        val spin = loopFraction * TWO_PI
        for (j in 0 until 6) {
            val theta = spin + j * (PI_F / 6f)
            val rx = abs(cos(theta)) * r
            val front = if (sin(theta) > 0f) 1f else 0.5f
            ovalRect.set(cx - rx, cy - r, cx + rx, cy + r)
            stroke(Sculpt.withAlpha(accent, base * front), lineW)
            canvas.drawOval(ovalRect, paint)
        }
        // Scan glints on the equator.
        if (active) {
            val gx = cx + cos(spin * 2f) * r * 0.92f
            fill(Sculpt.withAlpha(accent, 0.8f))
            canvas.drawCircle(gx, cy, px(1.8f) * geo, paint)
        }
    }

    private fun drawContents(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int, active: Boolean, geo: Float) {
        val cr = ring * CORE_RATIO
        if (active) {
            drawActiveBadge(canvas, cx, cy, ring, accent, geo)
            return
        }
        if (state == State.CONNECTING) {
            drawCrown(canvas, cx, cy - cr * 0.60f, cr * 0.34f, accent, geo, false)
            drawSeekingGlyph(canvas, cx, cy + cr * 0.08f, geo, accent)
            val caption = if (progressPercent in 0..100) {
                Strings.tf("CONNECTING %s%%", progressPercent)
            } else {
                Strings.t("CONNECTING")
            }
            drawLabel(canvas, caption, cx, cy + cr * 0.44f, px(10.5f) * geo, Sculpt.onGlass(accent), labelTypeface, cr * 1.45f, null)
            return
        }
        // Idle or failed. No check mark before CONNECTED.
        val failed = state == State.FAILED
        val glyph = if (failed) palette.danger else accent
        drawCrown(canvas, cx, cy - cr * 0.52f, cr * 0.44f, if (failed) palette.danger else palette.connected, geo, false)
        drawPowerGlyph(canvas, cx, cy + cr * 0.02f, cr * 0.20f, glyph, geo, true)
        val cta = if (failed) Strings.t("RETRY") else Strings.t("TAP TO CONNECT")
        drawLabel(canvas, cta, cx, cy + cr * 0.50f, px(10.5f) * geo, Sculpt.onGlass(glyph), labelTypeface, cr * 1.45f, null)
    }

    private fun drawActiveBadge(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int, geo: Float) {
        val cr = ring * CORE_RATIO
        // Soft glow behind the crown.
        val gr = cr * 0.45f
        fill(accent)
        paint.shader = RadialGradient(
            cx, cy - cr * 0.50f, gr,
            Sculpt.withAlpha(accent, if (dark) 0.30f else 0.15f), Sculpt.withAlpha(accent, 0f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy - cr * 0.50f, gr, paint)
        paint.shader = null
        drawCrown(canvas, cx, cy - cr * 0.50f, cr * 0.48f, accent, geo, true)

        drawLabel(
            canvas, Strings.t("CONNECTED"), cx, cy + cr * 0.06f, px(14.5f) * geo,
            Sculpt.onGlass(accent), boldTypeface, cr * 1.50f, accent,
        )
        if (timerText.isNotEmpty()) {
            drawLabel(
                canvas, timerText, cx, cy + cr * 0.30f, px(10.5f) * geo,
                Sculpt.withAlpha(palette.ink, 0.9f), monoTypeface, cr * 1.3f, null, spaced = false,
            )
        }
        drawPowerGlyph(canvas, cx, cy + cr * 0.58f, cr * 0.10f, accent, geo, true)
    }

    /** Outward radar arcs. Used while negotiating instead of any check mark. */
    private fun drawSeekingGlyph(canvas: Canvas, cx: Float, cy: Float, geo: Float, accent: Int) {
        for (k in 0 until 3) {
            val phase = (loopFraction + k / 3f) % 1f
            val r = px(10f + 11f * k) * geo
            stroke(Sculpt.withAlpha(accent, 0.25f + 0.75f * (1f - phase)), px(2.2f) * geo)
            paint.strokeCap = Paint.Cap.ROUND
            arcRect.set(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(arcRect, -128f, 76f, false, paint)
        }
        fill(accent)
        canvas.drawCircle(cx, cy, px(2.6f) * geo, paint)
    }

    private fun drawPowerGlyph(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, geo: Float, glow: Boolean) {
        stroke(color, px(2.4f) * geo)
        paint.strokeCap = Paint.Cap.ROUND
        if (glow) paint.setShadowLayer(px(6f) * geo, 0f, 0f, Sculpt.withAlpha(color, if (dark) 0.9f else 0.4f))
        arcRect.set(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(arcRect, -60f, 300f, false, paint)
        canvas.drawLine(cx, cy - r * 1.18f, cx, cy - r * 0.18f, paint)
        paint.clearShadowLayer()
    }

    /** The Kourosh crown: five points, jewelled tips, a band with a centre gem. */
    private fun drawCrown(canvas: Canvas, cx: Float, cy: Float, crownW: Float, jewel: Int, geo: Float, lit: Boolean) {
        val h = crownW * 0.66f
        val top = cy - h / 2f
        fun px0(f: Float) = cx + f * crownW
        fun py0(f: Float) = top + f * h
        corePath.reset()
        corePath.moveTo(px0(-0.42f), py0(0.80f))
        corePath.lineTo(px0(-0.50f), py0(0.22f))
        corePath.lineTo(px0(-0.30f), py0(0.50f))
        corePath.lineTo(px0(-0.25f), py0(0.10f))
        corePath.lineTo(px0(-0.10f), py0(0.46f))
        corePath.lineTo(px0(0f), py0(0f))
        corePath.lineTo(px0(0.10f), py0(0.46f))
        corePath.lineTo(px0(0.25f), py0(0.10f))
        corePath.lineTo(px0(0.30f), py0(0.50f))
        corePath.lineTo(px0(0.50f), py0(0.22f))
        corePath.lineTo(px0(0.42f), py0(0.80f))
        corePath.close()
        val goldFill = LinearGradient(0f, top, 0f, top + h, intArrayOf(gh, gm, gl), null, Shader.TileMode.CLAMP)
        fill(gm)
        if (lit) paint.setShadowLayer(px(5f) * geo, 0f, 0f, Sculpt.withAlpha(gm, if (dark) 0.7f else 0.3f))
        paint.shader = goldFill
        canvas.drawPath(corePath, paint)
        paint.clearShadowLayer()
        // Band.
        ovalRect.set(px0(-0.45f), py0(0.84f), px0(0.45f), py0(1f))
        canvas.drawRoundRect(ovalRect, h * 0.05f, h * 0.05f, paint)
        paint.shader = null
        stroke(groove, px(0.8f) * geo)
        paint.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(corePath, paint)
        canvas.drawRoundRect(ovalRect, h * 0.05f, h * 0.05f, paint)
        // Jewelled tips.
        val tipR = crownW * 0.04f
        fill(gh)
        canvas.drawCircle(px0(-0.50f), py0(0.22f), tipR, paint)
        canvas.drawCircle(px0(-0.25f), py0(0.10f), tipR, paint)
        canvas.drawCircle(px0(0f), py0(0f), tipR * 1.2f, paint)
        canvas.drawCircle(px0(0.25f), py0(0.10f), tipR, paint)
        canvas.drawCircle(px0(0.50f), py0(0.22f), tipR, paint)
        // Gems.
        fill(jewel)
        paint.setShadowLayer(px(4f) * geo, 0f, 0f, Sculpt.withAlpha(jewel, if (dark) 0.9f else 0.4f))
        canvas.drawCircle(px0(0f), py0(0.62f), crownW * 0.055f, paint)
        canvas.drawCircle(px0(0f), py0(0.92f), h * 0.055f, paint)
        canvas.drawCircle(px0(-0.28f), py0(0.92f), h * 0.04f, paint)
        canvas.drawCircle(px0(0.28f), py0(0.92f), h * 0.04f, paint)
        paint.clearShadowLayer()
    }

    private fun drawLabel(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Int,
        face: Typeface,
        maxWidth: Float,
        glow: Int?,
        spaced: Boolean = true,
    ) {
        if (text.isEmpty()) return
        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = face
        textPaint.textSize = size
        // Latin-only: Persian/Chinese glyphs shatter under letter-spacing.
        textPaint.letterSpacing = if (spaced && AppLanguage.current() == "en") 0.16f else 0f
        val measured = textPaint.measureText(text)
        if (measured > maxWidth && measured > 0f) textPaint.textSize = size * maxWidth / measured
        textPaint.color = color
        if (glow != null) {
            textPaint.setShadowLayer(px(7f), 0f, 0f, Sculpt.withAlpha(glow, if (dark) 0.85f else 0.35f))
        }
        canvas.drawText(text, x, baseline, textPaint)
        textPaint.clearShadowLayer()
    }

    // ------------------------------------------------------------ input

    private fun isInsideDial(x: Float, y: Float): Boolean {
        val ring = if (lastRing > 0f) lastRing else RING_DP * density * sizeScale
        return hypot(x - width / 2f, y - height / 2f) <= ring * 1.1f
    }

    private fun release() {
        animate().scaleX(1f).scaleY(1f).setDuration(190L).start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // A disabled dial swallows the touch but never toggles the tunnel.
        if (!isEnabled) return isClickable
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isInsideDial(event.x, event.y)) return false
                pressedInside = true
                animate().scaleX(PRESS_SCALE).scaleY(PRESS_SCALE).setDuration(110L).start()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressedInside && !isInsideDial(event.x, event.y)) {
                    pressedInside = false
                    release()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                release()
                val hit = pressedInside && isInsideDial(event.x, event.y)
                pressedInside = false
                if (hit) {
                    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedInside = false
                release()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val confirm = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
        if (confirm && isEnabled && event.repeatCount == 0) {
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            performClick()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        invalidate()
    }

    // ------------------------------------------------------------ lifecycle

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startLoop()
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        tickAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        // No redraws while the window is hidden.
        if (visibility == VISIBLE) startLoop() else stopLoop()
    }

    private fun startLoop() {
        if (!isAttachedToWindow || windowVisibility != VISIBLE) return
        val tempo = if (state == State.CONNECTING) CONNECTING_LOOP_MS else IDLE_LOOP_MS
        val running = loopAnimator
        if (running != null && running.duration == tempo) return
        running?.cancel()
        loopAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = tempo
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                loopFraction = animator.animatedValue as Float
                pulse = if (loopFraction < 0.5f) loopFraction * 2f else (1f - loopFraction) * 2f
                val now = SystemClock.uptimeMillis()
                if (now - lastFrameAt >= FRAME_MS) {
                    lastFrameAt = now
                    invalidate()
                }
            }
            start()
        }
    }

    private fun stopLoop() {
        loopAnimator?.cancel()
        loopAnimator = null
        pulse = 0f
    }

    private fun animateTickReveal() {
        tickAnimator?.cancel()
        if (!isAttachedToWindow) {
            tickReveal = 1f
            return
        }
        tickAnimator = ValueAnimator.ofFloat(tickReveal, 1f).apply {
            duration = 900L
            addUpdateListener { animator ->
                tickReveal = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    companion object {
        const val TICK_COUNT = 60
        const val TICK_LIT = 44
        const val RIPPLE_GROWTH = 0.32f
        const val RING_DP = 112
        const val HALO_OUTSET_DP = 24
        const val HALO_PULSE_DP = 7
        const val BLEED_MARGIN_DP = 4
        val BLEED_DP: Int = ceil(
            maxOf(RING_DP * RIPPLE_GROWTH, (HALO_OUTSET_DP + HALO_PULSE_DP).toFloat())
        ).toInt() + BLEED_MARGIN_DP
        const val MIN_SIZE_SCALE = 0.78f
        const val CORE_RATIO = 0.744f

        private const val TWO_PI = 6.2831855f
        private const val PI_F = 3.1415927f
        private const val HALF_PI = 1.5707964f
        private const val PRESS_SCALE = 0.965f
        private const val CONNECTING_LOOP_MS = 1150L
        private const val IDLE_LOOP_MS = 8000L
        private const val FRAME_MS = 33L

        private val DARK_GOLD_HI = 0xFFF6D98B.toInt()
        private val DARK_GOLD = 0xFFD4A64A.toInt()
        private val DARK_GOLD_LO = 0xFF8A6420.toInt()
        private val DARK_GROOVE = 0xFF3E2A08.toInt()
        private val LIGHT_GOLD_HI = 0xFFE6C36F.toInt()
        private val LIGHT_GOLD = 0xFFB8892F.toInt()
        private val LIGHT_GOLD_LO = 0xFF7A5718.toInt()
        private val LIGHT_GROOVE = 0xFF5A3F10.toInt()
        private val SHADOW_TINT = 0xFF000000.toInt()
    }
}
