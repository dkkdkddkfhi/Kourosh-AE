package com.kourosh.ae

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.text.TextPaint
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The Orbit dial: the one control that matters on the main screen.
 *
 * Kourosh-AE 3.0 turns the glass core into a living 3D orb. Layers, outermost
 * first:
 * 1. breathing halo (connected only)
 * 2. two ripple rings that expand and fade (connected only)
 * 3. static hairline ring + slowly rotating dashed ring
 * 4. 60 gauge ticks, lighting up as the tunnel comes up
 * 5. progress arc: sweeps while connecting, settles at ~86% when connected
 * 6. the orb: a lit sphere with a fresnel rim in the state accent, flowing
 *    energy under the glass, a rotating orbital grid, a specular highlight
 *    that follows the phone's tilt, and a sheen band when connected.
 *    API 33+ renders the sphere with [OrbCoreShader] (AGSL); older releases
 *    and any device whose driver rejects the shader use the Canvas path.
 * 7. contents: shield + "TAP TO CONNECT" when down, an outward radar sweep +
 *    "CONNECTING" while negotiating, crown + shield + timer when up. The shield
 *    with a checkmark must never appear before CONNECTED, see [drawSeekingGlyph].
 *
 * State changes blend the accent colour and the orb's energy over
 * [STATE_BLEND_MS] instead of snapping.
 *
 * GEOMETRY: the halo and the ripples grow beyond the ring, so the view is
 * measured as ring + [BLEED_DP]. See [onMeasure].
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
            val wasActive = previous == State.CONNECTED || previous == State.DEGRADED
            val isActive = value == State.CONNECTED || value == State.DEGRADED
            if (isActive) {
                if (!wasActive) {
                    tickReveal = 0f
                    animateTickReveal()
                } else if (tickReveal < 1f && tickAnimator?.isRunning != true) {
                    animateTickReveal()
                }
            } else {
                tickAnimator?.cancel()
                tickAnimator = null
                tickReveal = 0f
            }
            if (previous != value) animateStateBlend()
            // The loop tempo is chosen when the animator is created: 1150ms
            // while CONNECTING, 8s otherwise. Before 3.0 the animator was only
            // created once, so the connecting wave never sped up. Restart it
            // whenever the state crosses the CONNECTING boundary.
            if ((previous == State.CONNECTING) != (value == State.CONNECTING)) {
                restartLoop()
            } else {
                startLoop()
            }
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
     * Connect progress, 0..100, or -1 for "no measurable progress".
     *
     * Only drawn in [State.CONNECTING], and only when non-negative: a transport
     * that cannot report real progress shows the spinner alone rather than a
     * fabricated number. See KouroshAeVpnService.EXTRA_PROGRESS.
     */
    var progressPercent: Int = -1
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    // White speculars over a dark canvas, dark speculars over a light one.
    // Read once: the palette cannot change without recreate().
    private val light = Sculpt.lighting
    private val density = resources.displayMetrics.density
    private val bounds = RectF()
    private val corePath = Path()

    private var loopFraction = 0f
    private var pulse = 0f
    private var tickReveal = 0f
    private var loopAnimator: ValueAnimator? = null
    private var tickAnimator: ValueAnimator? = null

    // State blend: accent colour and orb energy ease between states.
    private val argb = ArgbEvaluator()
    private var blend = 1f
    private var blendFromAccent = 0
    private var blendFromEnergy = 0f
    private var blendAnimator: ValueAnimator? = null

    // Tilt parallax, fed by the gravity sensor while the dial is visible.
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val tiltSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var sensorsOn = false
    private var tiltX = 0f
    private var tiltY = 0f
    private var targetTiltX = 0f
    private var targetTiltY = 0f
    private val tiltListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values.size < 2) return
            val g = SensorManager.GRAVITY_EARTH
            targetTiltX = (-event.values[0] / g).coerceIn(-1f, 1f)
            targetTiltY = (event.values[1] / g - 0.7f).coerceIn(-1f, 1f)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private var orbShader: OrbCoreShader? = null

    private val monoTypeface: Typeface = Typefaces.mono(context)
    private val labelTypeface: Typeface
        get() = Typefaces.medium(context)

    init {
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = false
        contentDescription = "اتصال"
        if (Build.VERSION.SDK_INT >= 33) {
            orbShader = OrbCoreShader.createOrNull()
        }
        // The AGSL orb needs the hardware pipeline. Without it, shadow layers
        // and sweep gradients are rendered in software to stay exact on older
        // GPUs, as before.
        if (orbShader == null) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    fun applyPalette(next: AppAppearance.Palette) {
        palette = next
        blendAnimator?.cancel()
        blendAnimator = null
        blend = 1f
        invalidate()
    }

    private fun accentFor(state: State): Int = when (state) {
        State.DISCONNECTED -> palette.muted
        State.CONNECTING -> palette.amber
        State.CONNECTED -> palette.connected
        State.DEGRADED -> palette.amber
        State.FAILED -> palette.danger
    }

    private fun energyFor(state: State): Float = when (state) {
        State.DISCONNECTED -> 0.18f
        State.CONNECTING -> 0.6f
        State.CONNECTED -> 1f
        State.DEGRADED -> 0.7f
        State.FAILED -> 0.35f
    }

    private fun displayAccent(): Int {
        val target = accentFor(state)
        if (blend >= 1f) return target
        return argb.evaluate(blend, blendFromAccent, target) as Int
    }

    private fun displayEnergy(): Float {
        val target = energyFor(state)
        if (blend >= 1f) return target
        return blendFromEnergy + (target - blendFromEnergy) * blend
    }

    private fun animateStateBlend() {
        // Start from what is on screen now, so a quick second change never jumps.
        blendFromAccent = if (blend >= 1f) blendFromAccentFallback() else currentBlendAccent()
        blendFromEnergy = currentBlendEnergy()
        blendAnimator?.cancel()
        blendAnimator = null
        if (!isAttachedToWindow) {
            blend = 1f
            return
        }
        blend = 0f
        blendAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = STATE_BLEND_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                blend = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // Colour and energy of the frame that was last drawn, tracked in onDraw.
    private var lastDrawnAccent = 0
    private var lastDrawnEnergy = 0f
    private var hasDrawn = false

    private fun blendFromAccentFallback(): Int = if (hasDrawn) lastDrawnAccent else accentFor(state)
    private fun currentBlendAccent(): Int = if (hasDrawn) lastDrawnAccent else accentFor(state)
    private fun currentBlendEnergy(): Float = if (hasDrawn) lastDrawnEnergy else energyFor(state)

    /**
     * Uniform shrink factor for the whole dial, bleed included.
     *
     * The console asks for this when its natural height would overflow the
     * viewport: shrinking the dial is how the screen stops scrolling. Because
     * the factor scales the measured box AND the ring together, the halo and
     * ripples keep their proportional room and cannot be cropped.
     */
    var sizeScale: Float = 1f
        set(value) {
            val clamped = value.coerceIn(MIN_SIZE_SCALE, 1f)
            if (field != clamped) {
                field = clamped
                requestLayout()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // The measured box is the RING plus [BLEED_DP], not the ring alone.
        // In software-layer mode Android allocates an offscreen bitmap exactly
        // the size of the view, so anything painted outside it is lost. The
        // halo and ripples reach past the ring, and BLEED_DP is derived from
        // those reaches.
        val desired = dp(((RING_DP + BLEED_DP) * 2 * sizeScale).roundToInt())
        val size = resolveSize(desired, widthMeasureSpec)
            .coerceAtMost(resolveSize(desired, heightMeasureSpec))
        // Always square: a non-square canvas would put the ring off-centre.
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val half = minOf(width, height) / 2f
        // Second term: if a parent hands this view less than it asked for, the
        // ring shrinks so the bleed stays intact. Never remove it.
        val ring = minOf(
            dp(RING_DP) * sizeScale,
            half * RING_DP / (RING_DP + BLEED_DP).toFloat(),
        )
        val geo = ring / dp(RING_DP)
        val accent = displayAccent()
        val energy = displayEnergy()
        lastDrawnAccent = accent
        lastDrawnEnergy = energy
        hasDrawn = true
        val active = state == State.CONNECTED || state == State.DEGRADED

        if (active) {
            drawHalo(canvas, cx, cy, ring, accent, geo)
            drawRipples(canvas, cx, cy, ring, accent)
        }
        drawRings(canvas, cx, cy, ring, geo)
        drawTicks(canvas, cx, cy, ring, accent, geo)
        drawArc(canvas, cx, cy, ring, geo)
        drawCore(canvas, cx, cy, ring, accent, active, energy)
        drawContents(canvas, cx, cy, ring, accent, active, geo)
        if (isFocused) drawFocusRing(canvas, cx, cy, ring, geo)
    }

    /** Soft breathing bloom just outside the ring. */
    private fun drawHalo(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int, geo: Float) {
        val radius = ring + dp(HALO_OUTSET_DP) * geo + pulse * dp(HALO_PULSE_DP) * geo
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(
                Sculpt.withAlpha(accent, 0.001f),
                Sculpt.withAlpha(accent, 0.15f + pulse * 0.07f),
                Sculpt.withAlpha(accent, 0f),
            ),
            floatArrayOf(0.60f, 0.86f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null
    }

    /** Two rings, half a cycle apart, expanding past the ring and fading out. */
    private fun drawRipples(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * density
        for (offset in RIPPLE_OFFSETS) {
            val phase = (loopFraction + offset) % 1f
            paint.color = Sculpt.withAlpha(accent, 0.40f * (1f - phase))
            canvas.drawCircle(cx, cy, ring * (1f + phase * RIPPLE_GROWTH), paint)
        }
    }

    private fun drawRings(canvas: Canvas, cx: Float, cy: Float, ring: Float, geo: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * density
        paint.color = Sculpt.withAlpha(palette.ink, 0.055f)
        canvas.drawCircle(cx, cy, ring, paint)

        // Slowly rotating dashed ring, drawn as short arcs rather than a
        // DashPathEffect so the rotation is exact and cheap.
        val inner = ring - dp(20) * geo
        paint.color = Sculpt.withAlpha(palette.ink, 0.07f)
        bounds.set(cx - inner, cy - inner, cx + inner, cy + inner)
        val spin = loopFraction * 12f
        var angle = spin
        while (angle < 360f + spin) {
            canvas.drawArc(bounds, angle, 4.5f, false, paint)
            angle += 11f
        }
    }

    /**
     * The gauge: 60 ticks, the first 44 lit when connected. While CONNECTING a
     * three-lobe standing wave runs around the rim, driven by the 1150ms loop.
     */
    private fun drawTicks(canvas: Canvas, cx: Float, cy: Float, ring: Float, accent: Int, geo: Float) {
        val litCount = when (state) {
            State.CONNECTED, State.DEGRADED -> (TICK_LIT * tickReveal).roundToInt()
            else -> 0
        }
        val wavePhase = loopFraction * TWO_PI
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val length = dp(7) * geo
        for (i in 0 until TICK_COUNT) {
            // -90 degrees so tick 0 sits at the top and the gauge fills clockwise.
            val rad = Math.toRadians((i * (360.0 / TICK_COUNT)) - 90.0)
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()
            val lit = i < litCount
            var tickLength = length
            paint.strokeWidth = 1.5f * density
            when {
                lit -> {
                    paint.color = Sculpt.withAlpha(accent, 0.95f)
                    paint.setShadowLayer(3f * density, 0f, 0f, Sculpt.withAlpha(accent, 0.8f))
                }
                state == State.CONNECTING -> {
                    val theta = (i.toFloat() / TICK_COUNT) * TWO_PI
                    val raw = sin((theta * WAVE_LOBES - wavePhase).toDouble()).toFloat()
                    val w = if (raw <= 0f) 0f else Math.pow(raw.toDouble(), WAVE_SHARPNESS).toFloat()
                    tickLength = (6.4f + 4.6f * w) * density * geo
                    paint.strokeWidth = (1.5f + 0.8f * w) * density
                    if (w <= 0.05f) {
                        paint.color = Sculpt.withAlpha(palette.ink, 0.09f)
                        paint.clearShadowLayer()
                    } else {
                        // Crests tip into mint: a hot centre with amber shoulders.
                        val hue = if (w > 0.55f) palette.mint else palette.amber
                        paint.color = Sculpt.withAlpha(hue, 0.09f + 0.78f * w)
                        if (w > 0.6f) {
                            paint.setShadowLayer(4f * density * w, 0f, 0f, Sculpt.withAlpha(hue, 0.75f * w))
                        } else {
                            paint.clearShadowLayer()
                        }
                    }
                }
                else -> {
                    paint.color = Sculpt.withAlpha(palette.ink, 0.11f)
                    paint.clearShadowLayer()
                }
            }
            val startR = ring - tickLength
            canvas.drawLine(
                cx + cosA * startR, cy + sinA * startR,
                cx + cosA * ring, cy + sinA * ring,
                paint,
            )
        }
        paint.clearShadowLayer()
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawArc(canvas: Canvas, cx: Float, cy: Float, ring: Float, geo: Float) {
        val r = ring - dp(13) * geo
        bounds.set(cx - r, cy - r, cx + r, cy + r)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f * density
        paint.strokeCap = Paint.Cap.ROUND

        paint.color = Sculpt.withAlpha(palette.ink, 0.06f)
        canvas.drawArc(bounds, 0f, 360f, false, paint)

        // CONNECTING: two soft crests on a full circle, turning with the wave.
        if (state == State.CONNECTING) {
            paint.strokeWidth = 2.2f * density
            val sweepShader = SweepGradient(
                cx, cy,
                intArrayOf(
                    Sculpt.withAlpha(palette.amber, 0.55f),
                    Sculpt.withAlpha(palette.amber, 0f),
                    Sculpt.withAlpha(palette.amber, 0f),
                    Sculpt.withAlpha(palette.amber, 0.55f),
                ),
                floatArrayOf(0f, 0.35f, 0.65f, 1f),
            )
            // SweepGradient starts at 3 o'clock; rotate it so the crest leads
            // from the top and travels with loopFraction.
            sweepShader.setLocalMatrix(
                Matrix().apply { setRotate(loopFraction * 360f - 90f, cx, cy) },
            )
            paint.shader = sweepShader
            canvas.drawArc(bounds, 0f, 360f, false, paint)
            paint.shader = null
            paint.strokeCap = Paint.Cap.BUTT
            return
        }

        val sweep = when (state) {
            State.CONNECTED, State.DEGRADED -> 310f
            else -> 0f
        }
        if (sweep <= 0f) {
            paint.strokeCap = Paint.Cap.BUTT
            return
        }
        paint.shader = SweepGradient(
            cx, cy,
            intArrayOf(palette.connected, palette.mint, palette.connected),
            floatArrayOf(0f, 0.5f, 1f),
        )
        canvas.drawArc(bounds, -90f, sweep, false, paint)
        paint.shader = null
        paint.strokeCap = Paint.Cap.BUTT
    }

    /** The orb: drop shadow, lit sphere, orbital grid, specular, sheen, bevel. */
    private fun drawCore(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        ring: Float,
        accent: Int,
        active: Boolean,
        energy: Float,
    ) {
        val r = ring * CORE_RATIO
        val base = Sculpt.blend(palette.surface, palette.ink, 0.035f)
        val px = tiltX * PARALLAX
        val py = tiltY * PARALLAX

        // Drop shadow; it slides against the tilt so the orb seems to float.
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = base
        val shadowColor = if (active) {
            Sculpt.withAlpha(accent, 0.38f)
        } else {
            Sculpt.withAlpha(Color.BLACK, light.dialShadowAlpha)
        }
        paint.setShadowLayer(
            dp(if (active) 22 else 16).toFloat(),
            -px * dp(12),
            dp(6) - py * dp(8),
            shadowColor,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.clearShadowLayer()

        val onGpu = canvas.isHardwareAccelerated &&
            drawShaderBody(canvas, cx, cy, r, accent, base, energy, px, py)
        if (!onGpu) drawCanvasBody(canvas, cx, cy, r, accent, base, energy, px, py)

        drawOrbitalGlobe(canvas, cx, cy, r, accent, active, px, py)

        // Specular highlight, following the light. The GPU path already has a
        // tight specular, so it only gets a soft wide one.
        val lx = cx + (-0.34f + px) * r
        val ly = cy + (-0.42f + py) * r
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            lx, ly, r * if (onGpu) 0.9f else 0.7f,
            intArrayOf(
                Sculpt.withAlpha(light.bevelColor, light.dialSpecular * if (onGpu) 0.45f else 1f),
                Sculpt.withAlpha(light.bevelColor, 0f),
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        // Sheen band crossing the glass, connected only. Clipped to the circle.
        if (active) {
            val save = canvas.save()
            corePath.reset()
            corePath.addCircle(cx, cy, r, Path.Direction.CW)
            canvas.clipPath(corePath)
            val travel = -1.4f + 2.8f * ((loopFraction * 0.6f) % 1f)
            val bandX = cx + travel * r
            paint.shader = LinearGradient(
                bandX - r * 0.30f, cy - r, bandX + r * 0.30f, cy + r,
                intArrayOf(
                    Sculpt.withAlpha(light.bevelColor, 0f),
                    Sculpt.withAlpha(light.bevelColor, light.dialSheen),
                    Sculpt.withAlpha(light.bevelColor, 0f),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null
            canvas.restoreToCount(save)
        }

        // Bevel: bright on the lit top edge, accent-tinted on the bottom edge.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f * density
        paint.shader = LinearGradient(
            cx, cy - r, cx, cy + r,
            intArrayOf(
                Sculpt.withAlpha(light.bevelColor, 0.35f),
                Sculpt.withAlpha(light.bevelColor, 0f),
                Sculpt.withAlpha(accent, 0.25f + 0.40f * energy),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r - paint.strokeWidth / 2f, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    /** GPU sphere. Returns false when the shader is not available. */
    private fun drawShaderBody(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        accent: Int,
        base: Int,
        energy: Float,
        px: Float,
        py: Float,
    ): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        val orb = orbShader ?: return false
        return try {
            orb.update(cx, cy, r, loopFraction * TWO_PI, px, py, energy, accent, base)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.shader = orb.shader
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null
            true
        } catch (t: Throwable) {
            // Never let a driver problem take the main screen down.
            paint.shader = null
            orbShader = null
            false
        }
    }

    /** Canvas sphere for API 26-32 and for the software-layer fallback. */
    private fun drawCanvasBody(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        accent: Int,
        base: Int,
        energy: Float,
        px: Float,
        py: Float,
    ) {
        paint.style = Paint.Style.FILL
        // Spherical body: a radial ramp centred on the lit point, not a flat
        // linear gradient, so the disc reads as a ball.
        paint.shader = RadialGradient(
            cx + (-0.34f + px) * r, cy + (-0.42f + py) * r, r * 1.55f,
            intArrayOf(
                Sculpt.lighten(base, light.dialBodyLift),
                base,
                Sculpt.darken(base, light.dialBodyDrop),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)

        // Fresnel rim in the state accent.
        paint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(
                Sculpt.withAlpha(accent, 0f),
                Sculpt.withAlpha(accent, 0f),
                Sculpt.withAlpha(accent, 0.18f + 0.30f * energy),
            ),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)

        // Inner energy glow, breathing with the loop.
        paint.shader = RadialGradient(
            cx, cy + r * 0.25f, r * 0.9f,
            intArrayOf(
                Sculpt.withAlpha(accent, (0.14f + 0.08f * pulse) * energy),
                Sculpt.withAlpha(accent, 0f),
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null
    }

    /** Rotating latitude/longitude grid inside the orb, shifted by the tilt. */
    private fun drawOrbitalGlobe(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        accent: Int,
        active: Boolean,
        px: Float,
        py: Float,
    ) {
        val save = canvas.save()
        corePath.reset()
        corePath.addCircle(cx, cy, r * 0.985f, Path.Direction.CW)
        canvas.clipPath(corePath)

        val gx = cx + px * r * 0.35f
        val gy = cy + py * r * 0.35f
        val phase = loopFraction * TWO_PI
        val grid = if (active) Sculpt.withAlpha(accent, 0.18f) else Sculpt.withAlpha(palette.muted, 0.13f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.65f * density
        paint.color = grid

        for (i in -2..2) {
            val latitude = i / 3f
            val y = gy + latitude * r * 0.58f
            val w = r * kotlin.math.sqrt((1f - latitude * latitude).coerceAtLeast(0.12f))
            bounds.set(gx - w, y - r * 0.055f, gx + w, y + r * 0.055f)
            canvas.drawOval(bounds, paint)
        }

        for (i in 0 until 6) {
            val angle = phase.toDouble() + i * Math.PI / 3.0
            val w = (kotlin.math.abs(kotlin.math.cos(angle)) * r.toDouble() * 0.82)
                .coerceAtLeast(r.toDouble() * 0.06).toFloat()
            bounds.set(gx - w, gy - r * 0.86f, gx + w, gy + r * 0.86f)
            canvas.drawOval(bounds, paint)
        }

        paint.strokeWidth = 1.25f * density
        paint.color = Sculpt.withAlpha(
            if (active) accent else light.bevelColor,
            if (active) 0.38f else 0.22f,
        )
        bounds.set(cx - r * 0.89f, cy - r * 0.89f, cx + r * 0.89f, cy + r * 0.89f)
        canvas.drawArc(bounds, -58f + phase * 57.3f, 72f, false, paint)
        canvas.restoreToCount(save)
        paint.style = Paint.Style.FILL
    }

    private fun drawFocusRing(canvas: Canvas, cx: Float, cy: Float, ring: Float, geo: Float) {
        paint.style = Paint.Style.STROKE
        paint.shader = null
        paint.strokeWidth = 2f * density
        paint.color = Sculpt.withAlpha(palette.primary, 0.85f)
        canvas.drawCircle(cx, cy, ring + dp(6) * geo, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawContents(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        ring: Float,
        accent: Int,
        active: Boolean,
        geo: Float,
    ) {
        if (active) {
            drawActiveBadge(canvas, cx, cy, ring, accent, geo)
            return
        }

        // CONNECTING gets its own glyph, never the shield: the shield carries a
        // checkmark, and nothing that reads as "done" may appear before
        // State.CONNECTED.
        if (state == State.CONNECTING) {
            drawSeekingGlyph(canvas, cx, cy, geo)

            textPaint.typeface = labelTypeface
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 10.5f * density * geo
            textPaint.letterSpacing = if (AppLanguage.current() != "en") 0f else 0.19f
            textPaint.color = palette.amberText
            // Transports that cannot measure progress print no figure.
            val caption = if (progressPercent >= 0) {
                Strings.tf("CONNECTING %s%%", progressPercent)
            } else {
                Strings.t("CONNECTING")
            }
            canvas.drawText(caption, cx, cy + dp(26) * geo, textPaint)
            textPaint.letterSpacing = spacing(0f)
            return
        }

        // Shield glyph + call to action.
        val shieldTop = cy - dp(30) * geo
        val shieldW = dp(30) * geo
        val shieldH = dp(34) * geo
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * density
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = when (state) {
            State.FAILED -> palette.danger
            else -> Sculpt.withAlpha(palette.muted, 0.9f)
        }
        val path = Path().apply {
            moveTo(cx, shieldTop)
            lineTo(cx + shieldW / 2f, shieldTop + shieldH * 0.13f)
            lineTo(cx + shieldW / 2f, shieldTop + shieldH * 0.52f)
            cubicTo(
                cx + shieldW / 2f, shieldTop + shieldH * 0.82f,
                cx + shieldW * 0.22f, shieldTop + shieldH * 0.97f,
                cx, shieldTop + shieldH,
            )
            cubicTo(
                cx - shieldW * 0.22f, shieldTop + shieldH * 0.97f,
                cx - shieldW / 2f, shieldTop + shieldH * 0.82f,
                cx - shieldW / 2f, shieldTop + shieldH * 0.52f,
            )
            lineTo(cx - shieldW / 2f, shieldTop + shieldH * 0.13f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.strokeWidth = 1.7f * density
        canvas.drawPath(Path().apply {
            moveTo(cx - shieldW * 0.15f, shieldTop + shieldH * 0.50f)
            lineTo(cx - shieldW * 0.02f, shieldTop + shieldH * 0.63f)
            lineTo(cx + shieldW * 0.20f, shieldTop + shieldH * 0.36f)
        }, paint)
        paint.strokeJoin = Paint.Join.MITER

        textPaint.typeface = labelTypeface
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 10.5f * density * geo
        textPaint.letterSpacing = if (AppLanguage.current() != "en") 0f else 0.19f
        textPaint.color = when (state) {
            State.FAILED -> palette.dangerText
            else -> Sculpt.withAlpha(palette.faint, 0.95f)
        }
        val cta = when (state) {
            State.FAILED -> Strings.t("RETRY")
            else -> Strings.t("TAP TO CONNECT")
        }
        canvas.drawText(cta, cx, cy + dp(26) * geo, textPaint)
        textPaint.letterSpacing = spacing(0f)
    }

    private fun drawActiveBadge(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        ring: Float,
        accent: Int,
        geo: Float,
    ) {
        val shieldTop = cy - dp(36) * geo
        val shieldW = dp(35) * geo
        val shieldH = dp(41) * geo
        val crownY = cy - dp(55) * geo

        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 2.2f * density * geo
        paint.color = Sculpt.withAlpha(palette.primary, 0.96f)
        val crown = Path().apply {
            moveTo(cx - dp(18) * geo, crownY + dp(13) * geo)
            lineTo(cx - dp(14) * geo, crownY - dp(5) * geo)
            lineTo(cx - dp(5) * geo, crownY + dp(4) * geo)
            lineTo(cx, crownY - dp(12) * geo)
            lineTo(cx + dp(6) * geo, crownY + dp(4) * geo)
            lineTo(cx + dp(15) * geo, crownY - dp(5) * geo)
            lineTo(cx + dp(18) * geo, crownY + dp(13) * geo)
            close()
        }
        canvas.drawPath(crown, paint)
        canvas.drawLine(
            cx - dp(17) * geo, crownY + dp(15) * geo,
            cx + dp(17) * geo, crownY + dp(15) * geo,
            paint,
        )

        val shield = Path().apply {
            moveTo(cx, shieldTop)
            lineTo(cx + shieldW / 2f, shieldTop + shieldH * 0.14f)
            lineTo(cx + shieldW / 2f, shieldTop + shieldH * 0.54f)
            cubicTo(
                cx + shieldW / 2f, shieldTop + shieldH * 0.82f,
                cx + shieldW * 0.22f, shieldTop + shieldH * 0.97f,
                cx, shieldTop + shieldH,
            )
            cubicTo(
                cx - shieldW * 0.22f, shieldTop + shieldH * 0.97f,
                cx - shieldW / 2f, shieldTop + shieldH * 0.82f,
                cx - shieldW / 2f, shieldTop + shieldH * 0.54f,
            )
            lineTo(cx - shieldW / 2f, shieldTop + shieldH * 0.14f)
            close()
        }
        paint.style = Paint.Style.FILL
        paint.color = Sculpt.withAlpha(palette.ink, 0.72f)
        paint.setShadowLayer(dp(12) * geo, 0f, 0f, Sculpt.withAlpha(accent, 0.62f))
        canvas.drawPath(shield, paint)
        paint.clearShadowLayer()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f * density * geo
        paint.color = Sculpt.withAlpha(palette.primary, 0.98f)
        canvas.drawPath(shield, paint)
        paint.strokeWidth = 2.8f * density * geo
        paint.color = Sculpt.withAlpha(palette.mint, 0.98f)
        val check = Path().apply {
            moveTo(cx - shieldW * 0.22f, shieldTop + shieldH * 0.52f)
            lineTo(cx - shieldW * 0.03f, shieldTop + shieldH * 0.69f)
            lineTo(cx + shieldW * 0.27f, shieldTop + shieldH * 0.34f)
        }
        canvas.drawPath(check, paint)

        textPaint.typeface = labelTypeface
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 10.5f * density * geo
        textPaint.letterSpacing = if (AppLanguage.current() != "en") 0f else 0.13f
        textPaint.color = Sculpt.onGlass(accent)
        textPaint.setShadowLayer(dp(8) * geo, 0f, 0f, Sculpt.withAlpha(accent, 0.55f))
        canvas.drawText(Strings.t("CONNECTED"), cx, cy + dp(28) * geo, textPaint)
        textPaint.clearShadowLayer()
        if (timerText.isNotEmpty()) {
            textPaint.typeface = monoTypeface
            textPaint.textSize = 8.5f * density * geo
            textPaint.letterSpacing = 0f
            textPaint.color = Sculpt.withAlpha(palette.faint, 0.92f)
            canvas.drawText(timerText, cx, cy + dp(43) * geo, textPaint)
        }
        textPaint.letterSpacing = spacing(0f)
        paint.strokeCap = Paint.Cap.BUTT
        paint.strokeJoin = Paint.Join.MITER
    }

    /** Radar sweep for CONNECTING: three arcs leaving the core in sequence. */
    private fun drawSeekingGlyph(canvas: Canvas, cx: Float, cy: Float, geo: Float) {
        val amber = palette.amber
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = Sculpt.withAlpha(amber, 0.85f)
        canvas.drawCircle(cx, cy - dp(6) * geo, (2.6f + pulse * 0.9f) * density * geo, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val base = dp(7) * geo
        val step = 7.5f * density * geo
        for (index in 0 until 3) {
            val phase = (loopFraction + index / 3f) % 1f
            val radius = base + step * index + phase * step
            val alpha = (0.72f - index * 0.18f) * (1f - phase)
            if (alpha <= 0.02f) continue
            paint.color = Sculpt.withAlpha(amber, alpha)
            paint.strokeWidth = (2.1f - index * 0.35f) * density
            bounds.set(
                cx - radius,
                cy - dp(6) * geo - radius,
                cx + radius,
                cy - dp(6) * geo + radius,
            )
            canvas.drawArc(bounds, -128f, 76f, false, paint)
        }
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun spacing(v: Float): Float = if (AppLanguage.current() != "en") 0f else v

    private fun animateTickReveal() {
        tickAnimator?.cancel()
        tickAnimator = ValueAnimator.ofFloat(tickReveal, 1f).apply {
            duration = 900
            addUpdateListener {
                tickReveal = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (!isEnabled) return true
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

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: android.graphics.Rect?,
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // A view can be detached mid-connection and reattached still
        // CONNECTED. Without this the halo and sheen stay frozen.
        startLoop()
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        tickAnimator?.cancel()
        tickAnimator = null
        blendAnimator?.cancel()
        blendAnimator = null
        blend = 1f
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        // Before 3.0 the loop kept redrawing at full frame rate while the app
        // sat in the background. Pause it with the window.
        if (visibility == VISIBLE) startLoop(visibility) else stopLoop()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) startLoop() else stopLoop()
    }

    private fun loopDuration(): Long =
        if (state == State.CONNECTING) CONNECTING_LOOP_MS else IDLE_LOOP_MS

    private fun startLoop(windowVis: Int = windowVisibility) {
        if (loopAnimator != null) return
        // Never run while detached or hidden: an animator started here would
        // hold the view and burn frames nobody sees.
        if (!isAttachedToWindow || windowVis != VISIBLE || visibility != VISIBLE) return
        loopAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = loopDuration()
            repeatCount = ValueAnimator.INFINITE
            interpolator = null
            addUpdateListener {
                loopFraction = it.animatedFraction
                pulse = if (loopFraction < 0.5f) loopFraction * 2f else (1f - loopFraction) * 2f
                tiltX += (targetTiltX - tiltX) * TILT_SMOOTHING
                tiltY += (targetTiltY - tiltY) * TILT_SMOOTHING
                invalidate()
            }
            start()
        }
        startTilt()
    }

    /** Switch the loop tempo without a visible jump in phase. */
    private fun restartLoop() {
        val keep = loopFraction
        loopAnimator?.cancel()
        loopAnimator = null
        startLoop()
        loopAnimator?.setCurrentFraction(keep)
    }

    private fun stopLoop() {
        loopAnimator?.cancel()
        loopAnimator = null
        loopFraction = 0f
        pulse = 0f
        stopTilt()
    }

    private fun startTilt() {
        if (sensorsOn) return
        // Respect "remove animations": no parallax when animators are off.
        if (!ValueAnimator.areAnimatorsEnabled()) return
        val manager = sensorManager ?: return
        val sensor = tiltSensor ?: return
        sensorsOn = manager.registerListener(tiltListener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun stopTilt() {
        if (!sensorsOn) return
        sensorManager?.unregisterListener(tiltListener)
        sensorsOn = false
        targetTiltX = 0f
        targetTiltY = 0f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // A disabled dial must not toggle the tunnel. Consume the touch so it
        // does not fall through to the view behind.
        if (!isEnabled) return isClickable
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animate().scaleX(0.965f).scaleY(0.965f).setDuration(110).start()
                true
            }
            MotionEvent.ACTION_UP -> {
                animate().scaleX(1f).scaleY(1f).setDuration(190).start()
                // Only a release inside the dial counts as a tap. Before 3.0 a
                // finger dragged off the dial still toggled the VPN.
                val inside = event.x >= 0f && event.x <= width.toFloat() &&
                    event.y >= 0f && event.y <= height.toFloat()
                if (inside) {
                    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    performClick()
                }
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1f).scaleY(1f).setDuration(190).start()
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    companion object {
        const val TICK_COUNT = 60
        /** Ticks lit when connected: 44 of 60. */
        const val TICK_LIT = 44
        /** How far a ripple grows past the ring. */
        const val RIPPLE_GROWTH = 0.32f
        /**
         * Ring radius in dp. 112dp is the largest ring that lets the full
         * console fit without scrolling on a 1080x2400 phone.
         */
        const val RING_DP = 112
        /** How far past the ring the halo's outer edge sits, at rest. */
        const val HALO_OUTSET_DP = 24
        /** Extra reach the halo gains at the top of its breath. */
        const val HALO_PULSE_DP = 7
        /** Slack so a feathered edge never lands on the last row of pixels. */
        const val BLEED_MARGIN_DP = 4
        /**
         * Extra radius the view is measured with, beyond the ring. Derived from
         * the two layers that paint outside the ring, so changing RING_DP alone
         * can never crop the dial.
         */
        val BLEED_DP: Int = ceil(
            maxOf(RING_DP * RIPPLE_GROWTH, (HALO_OUTSET_DP + HALO_PULSE_DP).toFloat())
        ).toInt() + BLEED_MARGIN_DP
        /** Floor for [sizeScale]. Below this the dial stops reading as primary. */
        const val MIN_SIZE_SCALE = 0.78f
        /** Core radius as a fraction of the ring. */
        const val CORE_RATIO = 0.744f

        private val RIPPLE_OFFSETS = floatArrayOf(0f, 0.5f)
        /** 2pi as a float literal (const val needs a compile-time constant). */
        private const val TWO_PI = 6.2831855f
        /** Lobes in the CONNECTING standing wave. */
        private const val WAVE_LOBES = 3f
        /** Exponent applied to the rectified sine, keeps crests compact. */
        private const val WAVE_SHARPNESS = 2.4
        private const val CONNECTING_LOOP_MS = 1_150L
        private const val IDLE_LOOP_MS = 8_000L
        /** State blend time. Within the 300ms motion budget. */
        private const val STATE_BLEND_MS = 280L
        /** How far the light and the grid move at full tilt, in orb radii. */
        private const val PARALLAX = 0.22f
        /** Per-frame low-pass on the tilt, so sensor jitter never shows. */
        private const val TILT_SMOOTHING = 0.12f
    }
}
