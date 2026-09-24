package com.kourosh.ae

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * # The Aurora dial
 *
 * The one control on the home screen, and the only object allowed to be loud.
 *
 * ## What it is
 *
 * A glass disc inside a single continuous ring. The ring is the state: gold at
 * rest, an amber comet while the tunnel is being dialled, a closed mint ring with
 * a breathing bloom when it is up, red only once the core has reported a failure.
 * Inside the disc the glyph changes with the state, and the disc carries the two
 * live numbers a user actually reads — the connect estimate while turning on, the
 * session timer once it is up.
 *
 * ## What changed from the previous dial, and why it is not just a reskin
 *
 * The old dial was an instrument: 60 gauge ticks, a dashed ring, a progress arc, a
 * radar sweep, a travelling sheen and a ripple pair, all on at once. Every one of
 * those layers was defensible on its own; together they competed, and the control
 * read as busy rather than as *premium*. In particular the 60 lit ticks were the
 * loudest thing on the screen and they carried the least information — "connected"
 * was already said three times (the ring, the pill, the headline).
 *
 * Aurora keeps one idea per state and gives it room:
 *
 *  - **One ring, one sweep.** The ring itself animates: its head travels while
 *    dialling and closes when the tunnel is up. Nothing else moves.
 *  - **No ticks.** The orbit dot replaces them: a single point of light travelling
 *    the ring, slow at rest and quicker while connecting. It gives the control life
 *    without turning it into a dashboard.
 *  - **A real glass disc.** Linear body gradient, top-left specular, inner bottom
 *    shadow, 1dp bevel — the same four layers the panels use, so the dial belongs
 *    to the same material as the screen it sits on.
 *  - **The numbers moved inside.** The timer and the connect percent are set in the
 *    disc's own type, centred, so the dial answers "how long / how much left"
 *    without the eye leaving it.
 *
 * ## Geometry
 *
 * The measured box is a square of [BOX_DP] scaled by [sizeScale]. The ring is drawn
 * at [RING_RATIO] of the half-extent, which leaves a deliberate margin: the bloom,
 * the pulse rings and the orbit dot all paint *outside* the ring, and the view runs
 * with a software layer (see [init]), so anything outside its own bounds is discarded
 * before a parent could clip it. Sizing the box to ring + bleed is what stops the
 * glow from being shaved flat, and it is why [onMeasure] is written the way it is.
 */
class AuroraDialView(
    context: Context,
    private var palette: AppAppearance.Palette,
) : View(context) {

    enum class State { DISCONNECTED, CONNECTING, CONNECTED, DEGRADED, FAILED }

    var state: State = State.DISCONNECTED
        set(value) {
            val previous = field
            field = value
            contentDescription = when (value) {
                State.DISCONNECTED -> "اتصال"
                State.CONNECTING -> "در حال اتصال"
                State.CONNECTED, State.DEGRADED -> "قطع اتصال"
                State.FAILED -> "اتصال ناموفق"
            }
            val wasUp = previous == State.CONNECTED || previous == State.DEGRADED
            val isUp = value == State.CONNECTED || value == State.DEGRADED
            if (isUp && !wasUp) {
                reveal = 0f
                animateReveal()
            } else if (!isUp) {
                revealAnimator?.cancel()
                reveal = 0f
            }
            restartLoop()
            invalidate()
        }

    /** Session uptime, drawn inside the disc while the tunnel is up. */
    var timerText: String = ""
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /**
     * Connect estimate 0..100, or -1 for "nothing to show".
     *
     * Only meaningful while [state] is CONNECTING. A real number from the transport
     * (Tor's bootstrap percent) always wins over the UI-side estimate; see
     * MainActivity's connect estimator.
     */
    var progressPercent: Int = -1
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /**
     * Uniform scale for the whole dial, bloom included.
     *
     * The console shrinks the dial — never the type or the cards — when its natural
     * height would overflow the viewport, so the connect control is always reachable
     * without scrolling.
     */
    var sizeScale: Float = 1f
        set(value) {
            val clamped = value.coerceIn(MIN_SIZE_SCALE, 1f)
            if (field != clamped) {
                field = clamped
                requestLayout()
            }
        }

    private val density = resources.displayMetrics.density
    private val light = Sculpt.lighting
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private val shield = Path()
    private val tick = Path()

    private val monoTypeface = Typefaces.mono(context)
    private val mediumTypeface: android.graphics.Typeface
        get() = Typefaces.medium(context)

    private var loopFraction = 0f
    private var pulse = 0f
    private var reveal = 0f
    private var loopAnimator: ValueAnimator? = null
    private var revealAnimator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = false
        contentDescription = "اتصال"
        // The disc stacks gradients and a soft inner shadow; a software layer keeps
        // them identical across GPU drivers, and the view repaints at 20-30fps at
        // most, on a 236dp square.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun applyPalette(next: AppAppearance.Palette) {
        palette = next
        invalidate()
    }

    private fun accentFor(state: State): Int = when (state) {
        State.DISCONNECTED -> palette.primary
        State.CONNECTING -> palette.amber
        State.CONNECTED -> palette.connected
        State.DEGRADED -> palette.amber
        State.FAILED -> palette.danger
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = dp((BOX_DP * sizeScale).roundToInt())
        val width = resolveSize(desired, widthMeasureSpec)
        val height = resolveSize(desired, heightMeasureSpec)
        // Always square: a non-square box would put the ring off-centre and give the
        // bloom more room on one axis than the other.
        val size = minOf(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val half = minOf(width, height) / 2f
        if (half <= 2f) return
        val cx = width / 2f
        val cy = height / 2f
        val ring = half * RING_RATIO
        val geo = ring / (RING_RATIO * dp(BOX_DP) / 2f)
        val accent = accentFor(state)
        val up = state == State.CONNECTED || state == State.DEGRADED
        val stroke = dp(STROKE_DP)

        drawBloom(canvas, cx, cy, half, ring, accent, up)
        if (up) drawPulses(canvas, cx, cy, ring, accent, stroke)
        drawTrack(canvas, cx, cy, ring, stroke, accent, up)
        drawOrbitDot(canvas, cx, cy, ring, accent, up)
        drawDisc(canvas, cx, cy, ring, accent, up)
        drawContents(canvas, cx, cy, ring, accent, up, geo)
    }

    /**
     * The ambient bloom under the whole dial.
     *
     * Radial, centred slightly above the disc so the light appears to fall from
     * above rather than from inside, and stronger when the tunnel is up — this is the
     * "connected is a lighting state" rule, expressed on the object that owns the
     * state rather than on the panels around it.
     */
    private fun drawBloom(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        half: Float,
        ring: Float,
        accent: Int,
        up: Boolean,
    ) {
        val breath = if (state == State.CONNECTING) 0.5f + 0.5f * sin(loopFraction * TWO_PI) else pulse * 0.5f
        val radius = half * 1.02f
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy - half * 0.12f, radius,
            intArrayOf(
                Sculpt.withAlpha(accent, (if (up) 0.22f else 0.12f) + 0.05f * breath),
                Sculpt.withAlpha(accent, (if (up) 0.07f else 0.035f)),
                Sculpt.withAlpha(accent, 0f),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null
    }

    /** Two rings expanding off the closed ring while the tunnel carries traffic. */
    private fun drawPulses(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        ring: Float,
        accent: Int,
        stroke: Float,
    ) {
        paint.style = Paint.Style.STROKE
        for (index in 0 until 2) {
            val phase = (loopFraction + index * 0.5f) % 1f
            val radius = ring + phase * dp(16)
            val alpha = (1f - phase) * 0.30f
            if (alpha <= 0.02f) continue
            paint.strokeWidth = stroke * 0.55f
            paint.color = Sculpt.withAlpha(accent, alpha)
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }

    /**
     * The ring: track, then state sweep.
     *
     * The track is always drawn, so the control has a fixed silhouette in every
     * state — a dial that grows a ring only when it connects reads as two different
     * controls. What changes is the lit portion on top of it:
     *
     *  - at rest: a 40° cap at the top, the "ready" position;
     *  - connecting: a rotating 84° comet plus, when the transport reports one, an
     *    amber progress arc growing from 12 o'clock;
     *  - up: the full ring, revealed once by a 640 ms sweep so the transition reads
     *    as the ring closing rather than as a colour swap;
     *  - failed: the full ring in the failure colour, static.
     */
    private fun drawTrack(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        ring: Float,
        stroke: Float,
        accent: Int,
        up: Boolean,
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        // track
        paint.strokeWidth = stroke
        paint.color = if (light.elevationDp > 0f) {
            Sculpt.withAlpha(Color.BLACK, 0.07f)
        } else {
            Sculpt.withAlpha(Color.WHITE, 0.09f)
        }
        canvas.drawCircle(cx, cy, ring, paint)

        bounds.set(cx - ring, cy - ring, cx + ring, cy + ring)
        when (state) {
            State.DISCONNECTED -> {
                paint.strokeWidth = stroke
                paint.color = Sculpt.withAlpha(accent, 0.85f)
                canvas.drawArc(bounds, -110f, 40f, false, paint)
                paint.color = Sculpt.withAlpha(accent, 0.18f)
                canvas.drawArc(bounds, -60f, 300f, false, paint)
            }
            State.CONNECTING -> {
                val percent = progressPercent
                if (percent in 0..100) {
                    paint.strokeWidth = stroke
                    paint.color = Sculpt.withAlpha(accent, 0.20f)
                    canvas.drawArc(bounds, -90f, 360f, false, paint)
                    paint.color = accent
                    canvas.drawArc(bounds, -90f, 3.6f * percent, false, paint)
                } else {
                    paint.strokeWidth = stroke
                    paint.color = Sculpt.withAlpha(accent, 0.16f)
                    canvas.drawArc(bounds, -90f, 360f, false, paint)
                }
                // The comet: a short bright arc travelling clockwise, plus a tail of
                // three progressively dimmer arcs behind it, which reads as motion at
                // a glance where a single arc reads as a stalled ring.
                val head = loopFraction * 360f
                for (index in 0 until 4) {
                    val sweep = 84f - index * 19f
                    if (sweep <= 4f) continue
                    paint.color = Sculpt.withAlpha(accent, 0.95f - index * 0.22f)
                    paint.strokeWidth = stroke * (1f - index * 0.14f)
                    canvas.drawArc(bounds, head - index * 13f, sweep, false, paint)
                }
            }
            State.CONNECTED, State.DEGRADED, State.FAILED -> {
                val sweep = 360f * reveal
                paint.strokeWidth = stroke
                paint.color = Sculpt.withAlpha(accent, 0.22f)
                canvas.drawArc(bounds, -90f, 360f, false, paint)
                if (sweep > 0.5f) {
                    paint.color = accent
                    canvas.drawArc(bounds, -90f, sweep, false, paint)
                    // A brighter head at the leading edge of the sweep — the same
                    // "travelling light" cue the connecting state uses, worn as a
                    // settled marker once the ring is closed.
                    if (sweep > 6f) {
                        val angle = Math.toRadians((-90f + sweep).toDouble())
                        val hx = cx + (cos(angle) * ring).toFloat()
                        val hy = cy + (sin(angle) * ring).toFloat()
                        paint.style = Paint.Style.FILL
                        paint.color = Sculpt.onGlass(accent)
                        canvas.drawCircle(hx, hy, stroke * 0.36f, paint)
                        paint.style = Paint.Style.STROKE
                    }
                }
            }
        }
        paint.strokeCap = Paint.Cap.BUTT
    }

    /**
     * The orbit dot: one point of light travelling the ring.
     *
     * Slow (9s) at rest so the control feels alive without asking for attention,
     * quick (1.5s) while dialling so the same element reads as work in progress. It
     * replaces the old 60-tick gauge entirely — that gauge was the single busiest
     * thing on the screen and it described a state the ring already described.
     */
    private fun drawOrbitDot(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        ring: Float,
        accent: Int,
        up: Boolean,
    ) {
        val angle = (loopFraction * 360f - 90f) * (Math.PI / 180f)
        val x = cx + (cos(angle) * ring).toFloat()
        val y = cy + (sin(angle) * ring).toFloat()
        // A short fading tail behind the dot, drawn as a gradient arc so it needs no
        // per-frame point list.
        bounds.set(cx - ring, cy - ring, cx + ring, cy + ring)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(3.4f)
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Sculpt.withAlpha(accent, if (up) 0.55f else 0.35f)
        canvas.drawArc(bounds, loopFraction * 360f - 118f, 18f, false, paint)
        paint.style = Paint.Style.FILL
        paint.color = Sculpt.onGlass(accent)
        canvas.drawCircle(x, y, dp(if (up) 3.1f else 2.5f), paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    /** The glass disc. */
    private fun drawDisc(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        ring: Float,
        accent: Int,
        up: Boolean,
    ) {
        val radius = ring - dp(15f)
        if (radius <= 4f) return

        // body
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, cy - radius, 0f, cy + radius,
            intArrayOf(
                Sculpt.lighten(palette.surface, light.dialBodyLift + if (up) 0.04f else 0f),
                Sculpt.blend(palette.surface, accent, if (up) 0.06f else 0.02f),
                Sculpt.darken(palette.surface, light.dialBodyDrop),
            ),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, paint)

        // specular, top-left — the convex read
        if (!light.let { it.elevationDp > 0f }) {
            paint.shader = RadialGradient(
                cx - radius * 0.34f, cy - radius * 0.52f, radius * 1.35f,
                intArrayOf(
                    Sculpt.withAlpha(Color.WHITE, light.dialSpecular),
                    Sculpt.withAlpha(Color.WHITE, 0f),
                ),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
        }

        // the state wash inside the disc, so the glass itself takes the colour
        paint.shader = RadialGradient(
            cx, cy + radius * 0.55f, radius * 1.15f,
            intArrayOf(
                Sculpt.withAlpha(accent, if (up) 0.20f else 0.10f),
                Sculpt.withAlpha(accent, 0f),
            ),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, paint)

        // inner bottom shadow
        paint.shader = LinearGradient(
            0f, cy - radius, 0f, cy + radius,
            intArrayOf(
                Sculpt.withAlpha(light.dialInnerShadowColor, 0f),
                Sculpt.withAlpha(light.dialInnerShadowColor, light.dialInnerShadow),
            ),
            floatArrayOf(0.35f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null

        // bevel
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1.1f)
        paint.color = if (light.elevationDp > 0f) {
            Sculpt.withAlpha(Color.BLACK, 0.10f)
        } else {
            Sculpt.withAlpha(Color.WHITE, light.dialEdgeStrong * 0.75f)
        }
        canvas.drawCircle(cx, cy, radius, paint)
        paint.strokeWidth = dp(2.4f)
        paint.color = Sculpt.withAlpha(accent, if (up) 0.22f else 0.10f)
        canvas.drawCircle(cx, cy, radius + dp(1.2f), paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    /** State glyph + the live numbers inside the disc. */
    private fun drawContents(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        ring: Float,
        accent: Int,
        up: Boolean,
        geo: Float,
    ) {
        val radius = ring - dp(15f)
        fun px(value: Float) = value * density * geo
        text.style = Paint.Style.FILL
        text.typeface = mediumTypeface
        text.textAlign = Paint.Align.CENTER

        when (state) {
            State.DISCONNECTED, State.CONNECTING -> {
                if (state == State.CONNECTING) {
                    drawSeeking(canvas, cx, cy - px(6f), accent, geo)
                } else {
                    drawPowerGlyph(canvas, cx, cy - px(8f), radius * 0.30f, Sculpt.onGlass(palette.primary))
                }
                val percent = progressPercent
                text.typeface = mediumTypeface
                text.textSize = px(9.5f)
                text.letterSpacing = if (AppLanguage.current() == "en") 0.12f else 0f
                text.color = Sculpt.withAlpha(accent, 0.95f)
                val caption = when {
                    state == State.DISCONNECTED -> Strings.t("STANDBY")
                    percent in 0..100 -> Strings.tf("CONNECTING %s%%", percent)
                    else -> Strings.t("CONNECTING")
                }
                canvas.drawText(caption, cx, cy + px(34f), text)
                text.letterSpacing = 0f
                // A hairline under the caption, the width of the word, so the label
                // sits on something instead of floating in the disc.
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = px(1f)
                paint.color = Sculpt.withAlpha(accent, 0.28f)
                val halfWidth = mathWidth(caption, px(9.5f), 0.12f) / 2f
                canvas.drawLine(cx - halfWidth, cy + px(39f), cx + halfWidth, cy + px(39f), paint)
                paint.strokeCap = Paint.Cap.BUTT
            }
            State.CONNECTED, State.DEGRADED -> {
                drawShield(canvas, cx, cy - px(20f), px(30f), accent)
                text.typeface = mediumTypeface
                text.textSize = px(9.5f)
                text.letterSpacing = if (AppLanguage.current() == "en") 0.14f else 0f
                text.color = Sculpt.withAlpha(accent, 1f)
                canvas.drawText(
                    Strings.t(if (state == State.DEGRADED) "DEGRADED" else "SECURE"),
                    cx, cy + px(15f), text,
                )
                text.letterSpacing = 0f
                if (timerText.isNotEmpty()) {
                    text.typeface = monoTypeface
                    text.textSize = px(16f)
                    text.color = Sculpt.onGlass(palette.ink)
                    canvas.drawText(timerText, cx, cy + px(37f), text)
                }
            }
            State.FAILED -> {
                drawCross(canvas, cx, cy - px(14f), px(26f), accent)
                text.typeface = mediumTypeface
                text.textSize = px(9.5f)
                text.letterSpacing = if (AppLanguage.current() == "en") 0.14f else 0f
                text.color = Sculpt.withAlpha(accent, 1f)
                canvas.drawText(Strings.t("FAILED"), cx, cy + px(26f), text)
                text.letterSpacing = 0f
            }
        }
    }

    /** The power glyph: an open ring with a bar through the top. */
    private fun drawPowerGlyph(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = dp(2.6f)
        paint.color = color
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(bounds, 118f, 304f, false, paint)
        canvas.drawLine(cx, cy - radius * 1.30f, cx, cy - radius * 0.14f, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    /**
     * The CONNECTING glyph: a core dot with two wavefronts leaving it.
     *
     * Deliberately open-ended — arcs that expand and fade have no terminal shape, so
     * the glyph cannot be mistaken for the tick that means "up". Two arcs rather than
     * the three the old dial drew: at this size three arcs collapsed into a blur, and
     * the third carried no extra meaning.
     */
    private fun drawSeeking(canvas: Canvas, cx: Float, cy: Float, accent: Int, geo: Float) {
        val base = dp(8f) * geo
        val step = dp(9f) * geo
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (index in 0 until 2) {
            val phase = (loopFraction + index / 2f) % 1f
            val radius = base + step * index + phase * step
            val alpha = (0.85f - index * 0.30f) * (1f - phase)
            if (alpha <= 0.03f) continue
            paint.strokeWidth = dp((2.2f - index * 0.5f)) * geo
            paint.color = Sculpt.withAlpha(accent, alpha)
            bounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(bounds, -132f, 84f, false, paint)
        }
        paint.strokeCap = Paint.Cap.BUTT
        paint.style = Paint.Style.FILL
        paint.color = Sculpt.withAlpha(accent, 0.9f)
        canvas.drawCircle(cx, cy, (2.4f + pulse * 0.8f) * density * geo, paint)
    }

    /**
     * The up glyph: a shield with a tick.
     *
     * Shown only in CONNECTED/DEGRADED, which is the whole reason the previous dial's
     * shield was a bug: it appeared while the transport was still negotiating, so the
     * one mark that is supposed to mean "protected" was drawn over an unproven
     * tunnel. Nothing here is filled until the state says up.
     */
    private fun drawShield(canvas: Canvas, cx: Float, cy: Float, size: Float, accent: Int) {
        val s = size / 24f
        fun x(v: Float) = cx + (v - 12f) * s
        fun y(v: Float) = cy + (v - 12f) * s
        shield.reset()
        shield.moveTo(x(12f), y(2.6f))
        shield.lineTo(x(20.6f), y(6.2f))
        shield.lineTo(x(20.6f), y(12.6f))
        shield.cubicTo(x(20.6f), y(18.2f), x(16.6f), y(21.4f), x(12f), y(22.6f))
        shield.cubicTo(x(7.4f), y(21.4f), x(3.4f), y(18.2f), x(3.4f), y(12.6f))
        shield.lineTo(x(3.4f), y(6.2f))
        shield.close()
        tick.reset()
        tick.moveTo(x(8.2f), y(12.1f))
        tick.lineTo(x(11.2f), y(15.1f))
        tick.lineTo(x(16.4f), y(9.1f))

        paint.style = Paint.Style.FILL
        paint.color = Sculpt.withAlpha(accent, 0.16f)
        canvas.drawPath(shield, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = size * 0.075f
        paint.color = Sculpt.withAlpha(accent, 0.95f)
        canvas.drawPath(shield, paint)

        paint.strokeWidth = size * 0.11f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Sculpt.onGlass(accent)
        canvas.drawPath(tick, paint)
        paint.strokeCap = Paint.Cap.BUTT
        paint.strokeJoin = Paint.Join.MITER
    }

    /** The failure glyph: a plain cross. Unambiguous in every language. */
    private fun drawCross(canvas: Canvas, cx: Float, cy: Float, size: Float, accent: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = dp(2.8f)
        paint.color = Sculpt.withAlpha(accent, 0.95f)
        val r = size / 2f
        canvas.drawLine(cx - r, cy - r, cx + r, cy + r, paint)
        canvas.drawLine(cx + r, cy - r, cx - r, cy + r, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    /**
     * Width of [value] in [size], used to underline a caption exactly.
     *
     * Measured with the same [TextPaint] the text is drawn with, so density, the font
     * scale and the localized typeface are all already accounted for. Letter spacing
     * is only tracked for Latin; [tracking] is expected to be 0 for other scripts.
     */
    private fun mathWidth(value: String, size: Float, tracking: Float): Float {
        text.textSize = size
        text.letterSpacing = tracking
        return text.measureText(value) + tracking * size * (value.length - 1).coerceAtLeast(0)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
            true
        }
        MotionEvent.ACTION_UP -> {
            animate().scaleX(1f).scaleY(1f).setDuration(180).start()
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            performClick()
            true
        }
        MotionEvent.ACTION_CANCEL -> {
            animate().scaleX(1f).scaleY(1f).setDuration(180).start()
            true
        }
        else -> super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            performClick()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // A view can be detached mid-connection and reattached still connected, which
        // would leave the bloom and the pulse frozen without this.
        restartLoop()
    }

    override fun onDetachedFromWindow() {
        loopAnimator?.cancel()
        loopAnimator = null
        revealAnimator?.cancel()
        revealAnimator = null
        super.onDetachedFromWindow()
    }

    private fun restartLoop() {
        loopAnimator?.cancel()
        loopAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            // Fast while dialling: the same travel that reads as "alive" at rest has
            // to read as "working" while connecting.
            duration = if (state == State.CONNECTING) 1_500L else 9_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = null
            addUpdateListener {
                loopFraction = it.animatedFraction
                // A single 0→1→0 breath per cycle, used by the bloom and the core dot.
                pulse = if (loopFraction < 0.5f) loopFraction * 2f else (1f - loopFraction) * 2f
                invalidate()
            }
            start()
        }
    }

    private fun animateReveal() {
        revealAnimator?.cancel()
        revealAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 640L
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                reveal = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun dp(value: Int): Float = value * density

    private fun dp(value: Float): Float = value * density

    companion object {
        /**
         * Side of the dial's measured square, in dp.
         *
         * Budgeted from the bottom up rather than picked for looks: the whole console
         * — hero, live-traffic card, exit node, protocol grid, slot card and the
         * signal strip — has to fit between the header and the dock on a 1080×2400
         * phone, and the dial is the only element that can give height back. 214dp
         * leaves the ring 160dp across: still comfortably the largest object on the
         * screen, with the remaining 25% of the box left for the bloom and the pulses.
         */
        const val BOX_DP = 214

        /**
         * Ring radius as a fraction of the half-extent.
         *
         * 0.75 leaves a quarter of the box outside the ring, which is where the
         * bloom, the pulse rings and the orbit dot live. Because every radius is
         * derived from this one number, shrinking the dial through [sizeScale] keeps
         * that proportion exactly — the glow can never be cropped by a smaller box.
         */
        const val RING_RATIO = 0.75f

        /** Ring thickness, in dp. */
        const val STROKE_DP = 9f

        /** Floor for [sizeScale]; below this the dial stops reading as the control. */
        const val MIN_SIZE_SCALE = 0.80f

        private const val TWO_PI = 6.2831855f
    }
}
