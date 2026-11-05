package com.kourosh.ae

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.content.res.ColorStateList
import android.os.Build
import android.os.SystemClock
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Shared drawing helpers for the Kourosh-AE 3.0 "imperial holo" look.
 *
 * Every surface in the app goes through [GlassDrawable]: a chamfered
 * (cut-corner) panel with an ornate gold frame, a dark glass body with a top
 * sheen and an inner shadow, and, when the caller passes an accent, a neon
 * inner glow in that accent. Large panels get a second inner frame and gold
 * diamond studs, like the stat cards in the reference design.
 */
object Sculpt {

    /**
     * How a raised surface is lit. The dark palette builds depth with a sheen at
     * the top and an inner shadow at the bottom. The light palette uses a real
     * outer drop shadow ([elevationDp]) instead. Held as a single mutable field
     * set once by [AppAppearance.load].
     */
    data class Lighting(
        /** Body gradient: how much lighter the top is than the fill. */
        val topLift: Float,
        /** Body gradient: how much darker the bottom is than the fill. */
        val bottomDrop: Float,
        /** White sheen at the top. 0 disables it. */
        val specular: Float,
        /** Inner shadow at the bottom of a raised surface. */
        val innerShadow: Float,
        /** Inner shadow at the top of a pressed surface. */
        val pressedInnerShadow: Float,
        /** Bevel line alpha on a raised surface. */
        val bevel: Float,
        /** Bevel line alpha on a pressed surface. */
        val pressedBevel: Float,
        /** Colour of the bevel line. */
        val bevelColor: Int,
        /** Outer drop shadow radius in dp. 0 disables it (dark palette). */
        val elevationDp: Float,
        /** Outer drop shadow opacity. */
        val elevationAlpha: Float,
        /** Default hairline when a caller passes neither accent nor stroke. */
        val defaultOutline: Int,
        /** Fallback outline for recessed wells. */
        val recessOutline: Int,
        // --- OrbitDialView only ---------------------------------------------
        /** Drop shadow opacity under the dial. */
        val dialShadowAlpha: Float,
        /** Specular on the glass core. */
        val dialSpecular: Float,
        /** Travelling sheen band, connected state only. */
        val dialSheen: Float,
        /** Inner shadow at the bottom of the core. */
        val dialInnerShadow: Float,
        /** Colour of that inner shadow. */
        val dialInnerShadowColor: Int,
        /** Bevel edge alpha at the top of the core. */
        val dialEdgeStrong: Float,
        /** Bevel edge alpha at the bottom of the core. */
        val dialEdgeSoft: Float,
        /** Multiplier applied to every [Sculpt.recess] depth. */
        val recessScale: Float,
        /** Dial body gradient: lift at the top. */
        val dialBodyLift: Float,
        /** Dial body gradient: drop at the bottom. */
        val dialBodyDrop: Float,
        /**
         * How dial text is shifted away from the accent so it reads on the glass.
         * Positive lightens (dark palette), negative darkens (light palette).
         */
        val dialTextShift: Float,
    )

    val DARK_LIGHTING = Lighting(
        topLift = 0.07f,
        bottomDrop = 0.14f,
        specular = 0.075f,
        innerShadow = 0.32f,
        pressedInnerShadow = 0.48f,
        bevel = 0.22f,
        pressedBevel = 0.05f,
        bevelColor = Color.WHITE,
        elevationDp = 0f,
        elevationAlpha = 0f,
        defaultOutline = Color.argb(46, 212, 166, 74),
        recessOutline = Color.argb(34, 212, 166, 74),
        dialShadowAlpha = 0.70f,
        dialSpecular = 0.14f,
        dialSheen = 0.09f,
        dialInnerShadow = 0.40f,
        dialInnerShadowColor = Color.BLACK,
        dialEdgeStrong = 0.24f,
        dialEdgeSoft = 0.05f,
        recessScale = 1f,
        dialBodyLift = 0.11f,
        dialBodyDrop = 0.16f,
        dialTextShift = 0.50f,
    )

    val LIGHT_LIGHTING = Lighting(
        topLift = 0.02f,
        bottomDrop = 0.05f,
        specular = 0.35f,
        innerShadow = 0.05f,
        pressedInnerShadow = 0.14f,
        bevel = 0.05f,
        pressedBevel = 0.10f,
        bevelColor = Color.BLACK,
        elevationDp = 6f,
        elevationAlpha = 0.22f,
        defaultOutline = Color.argb(70, 176, 133, 43),
        recessOutline = Color.argb(52, 176, 133, 43),
        dialShadowAlpha = 0.22f,
        dialSpecular = 0.30f,
        dialSheen = 0.05f,
        dialInnerShadow = 0.08f,
        dialInnerShadowColor = Color.BLACK,
        dialEdgeStrong = 0.06f,
        dialEdgeSoft = 0.14f,
        recessScale = 0.22f,
        dialBodyLift = 0.03f,
        dialBodyDrop = 0.06f,
        dialTextShift = -0.32f,
    )

    /** The active lighting model. Written once per Activity by [AppAppearance.load]. */
    @Volatile
    var lighting: Lighting = DARK_LIGHTING

    /** Alpha-blend [overlay] onto [base]. */
    fun blend(base: Int, overlay: Int, alpha: Float): Int {
        val a = alpha.coerceIn(0f, 1f)
        val r = ((Color.red(base) * (1 - a)) + (Color.red(overlay) * a)).roundToInt()
        val g = ((Color.green(base) * (1 - a)) + (Color.green(overlay) * a)).roundToInt()
        val b = ((Color.blue(base) * (1 - a)) + (Color.blue(overlay) * a)).roundToInt()
        return Color.rgb(r, g, b)
    }

    fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))

    /** Lift a colour towards white. */
    fun lighten(color: Int, amount: Float): Int = blend(color, Color.WHITE, amount)

    /** Push a colour towards black. */
    fun darken(color: Int, amount: Float): Int = blend(color, Color.BLACK, amount)

    /** A surface that should read as sunk below [base]. */
    fun recess(base: Int, depth: Float): Int = darken(base, depth * lighting.recessScale)

    /** [color] moved to where it can be read as text on this palette's glass. */
    fun onGlass(color: Int): Int = lighting.dialTextShift.let { shift ->
        if (shift >= 0f) lighten(color, shift) else darken(color, -shift)
    }

    /** Linear interpolation between two colours, alpha included. */
    fun mix(from: Int, to: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        fun channel(a: Int, b: Int) = (a + (b - a) * f).roundToInt().coerceIn(0, 255)
        return Color.argb(
            channel(Color.alpha(from), Color.alpha(to)),
            channel(Color.red(from), Color.red(to)),
            channel(Color.green(from), Color.green(to)),
            channel(Color.blue(from), Color.blue(to)),
        )
    }

    /**
     * A raised panel with a pressed state. [accent] lights the neon inner glow;
     * without it the panel still gets its gold frame.
     */
    fun sculptedBackground(
        density: Float,
        fill: Int,
        radius: Int,
        accent: Int? = null,
        stroke: Int? = null,
        strokeWidth: Int = 1,
        pressed: Boolean = false,
    ): Drawable {
        val outline = accent ?: stroke ?: lighting.defaultOutline
        fun layer(down: Boolean): Drawable = GlassDrawable(
            density = density,
            fill = fill,
            radiusDp = radius.toFloat(),
            stroke = outline,
            strokeWidthDp = strokeWidth * 1.1f,
            pressed = down,
            glow = accent,
        )
        if (pressed) return layer(true)
        return StateListDrawable().apply {
            setEnterFadeDuration(0)
            setExitFadeDuration(140)
            addState(intArrayOf(android.R.attr.state_pressed), layer(true))
            addState(intArrayOf(), layer(false))
        }
    }

    /** A well sunk into its parent. */
    fun recessedBackground(
        density: Float,
        fill: Int,
        radius: Int,
        accent: Int? = null,
    ): Drawable = GlassDrawable(
        density = density,
        fill = fill,
        radiusDp = radius.toFloat(),
        stroke = accent ?: lighting.recessOutline,
        strokeWidthDp = 1.1f,
        pressed = true,
    )

    /** [sculptedBackground] with a touch ripple on top. */
    fun sculptedRipple(
        density: Float,
        fill: Int,
        radius: Int,
        rippleColor: Int,
        accent: Int? = null,
    ): RippleDrawable {
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius * density * 0.5f
            setColor(Color.WHITE)
        }
        return RippleDrawable(
            ColorStateList.valueOf(withAlpha(rippleColor, 0.20f)),
            sculptedBackground(density, fill, radius, accent),
            mask,
        )
    }
}

/**
 * The imperial panel. Layers, back to front:
 * 0. outer drop shadow (light palette only),
 * 1. body gradient,
 * 2. accent wash from the bottom, top sheen, inner shadow, neon inner glow
 *    (all clipped to the panel),
 * 3. gold gradient frame,
 * 4. inner frame line (accent, or dark gold on large panels),
 * 5. gold diamond studs on large panels.
 * Pressing darkens the body, drops the sheen and brightens the glow.
 */
class GlassDrawable(
    private val density: Float,
    private val fill: Int,
    private val radiusDp: Float,
    private val stroke: Int,
    private val strokeWidthDp: Float = 1.1f,
    private val pressed: Boolean = false,
    private val glow: Int? = null,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val inner = RectF()
    private val shape = Path()
    private val innerShape = Path()
    private val stud = Path()
    private val light = Sculpt.lighting

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        val dark = light.elevationDp <= 0f
        val sw = max(strokeWidthDp * density, 1f)
        val frameW = sw * 1.25f
        val drop = if (!dark && !pressed) light.elevationDp * density else 0f
        rect.set(
            b.left + frameW / 2f + drop * 0.5f,
            b.top + frameW / 2f + drop * 0.2f,
            b.right - frameW / 2f - drop * 0.5f,
            b.bottom - frameW / 2f - drop * 0.8f,
        )
        if (rect.width() < 2f || rect.height() < 2f) return
        val cut = min(radiusDp * density * CUT_RATIO, min(rect.width(), rect.height()) * MAX_CUT_SHARE)
        chamferPath(shape, rect, cut)
        val accent = glow?.takeIf { Color.alpha(it) >= 40 }
        val strong = accent != null
        val framed = strong || Color.alpha(stroke) > 0
        val big = rect.height() >= 30f * density && rect.width() >= 60f * density
        val gold = if (dark) DARK_GOLD else LIGHT_GOLD

        // 0. Outer drop shadow.
        if (drop > 0f) {
            resetPaint(Paint.Style.FILL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                paint.color = fill
                paint.setShadowLayer(drop, 0f, drop * 0.35f, Sculpt.withAlpha(SHADOW_TINT, light.elevationAlpha))
                canvas.drawPath(shape, paint)
                paint.clearShadowLayer()
            } else {
                paint.color = Sculpt.withAlpha(SHADOW_TINT, light.elevationAlpha * 0.6f)
                canvas.save()
                canvas.translate(0f, drop * 0.4f)
                canvas.drawPath(shape, paint)
                canvas.restore()
            }
        }

        // 1. Body.
        val top = if (pressed) Sculpt.darken(fill, light.bottomDrop * 2.2f) else Sculpt.lighten(fill, light.topLift)
        val mid = if (pressed) Sculpt.darken(fill, light.bottomDrop * 0.7f) else fill
        val bottom = if (pressed) fill else Sculpt.darken(fill, light.bottomDrop)
        resetPaint(Paint.Style.FILL)
        paint.shader = LinearGradient(
            0f, rect.top, 0f, rect.bottom,
            intArrayOf(withFillAlpha(top), withFillAlpha(mid), withFillAlpha(bottom)),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(shape, paint)
        paint.shader = null

        // 2. Interior light, clipped to the panel.
        val saved = canvas.save()
        canvas.clipPath(shape)
        if (accent != null) {
            resetPaint(Paint.Style.FILL)
            paint.shader = RadialGradient(
                rect.centerX(), rect.bottom, max(rect.width(), rect.height()) * 0.8f,
                Sculpt.withAlpha(accent, if (dark) 0.16f else 0.08f),
                Sculpt.withAlpha(accent, 0f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(rect, paint)
            paint.shader = null
        }
        if (!pressed && light.specular > 0f) {
            val sheenBottom = rect.top + rect.height() * 0.48f
            resetPaint(Paint.Style.FILL)
            paint.shader = LinearGradient(
                0f, rect.top, 0f, sheenBottom,
                Sculpt.withAlpha(Color.WHITE, light.specular),
                Sculpt.withAlpha(Color.WHITE, 0f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(rect.left, rect.top, rect.right, sheenBottom, paint)
            paint.shader = null
        }
        val shadowAlpha = if (pressed) light.pressedInnerShadow else light.innerShadow
        if (shadowAlpha > 0f) {
            resetPaint(Paint.Style.FILL)
            if (pressed) {
                val edge = rect.top + rect.height() * 0.35f
                paint.shader = LinearGradient(
                    0f, rect.top, 0f, edge,
                    Sculpt.withAlpha(Color.BLACK, shadowAlpha), Sculpt.withAlpha(Color.BLACK, 0f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(rect.left, rect.top, rect.right, edge, paint)
            } else {
                val edge = rect.bottom - rect.height() * 0.32f
                paint.shader = LinearGradient(
                    0f, edge, 0f, rect.bottom,
                    Sculpt.withAlpha(Color.BLACK, 0f), Sculpt.withAlpha(Color.BLACK, shadowAlpha),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(rect.left, edge, rect.right, rect.bottom, paint)
            }
            paint.shader = null
        }
        if (accent != null) {
            val boost = if (pressed) 1.4f else 1f
            val widths = floatArrayOf(8f, 4.5f, 2.2f)
            val alphas = floatArrayOf(0.07f, 0.14f, 0.28f)
            val scale = if (dark) 1f else 0.6f
            for (i in widths.indices) {
                resetPaint(Paint.Style.STROKE)
                paint.strokeJoin = Paint.Join.MITER
                paint.strokeWidth = sw * widths[i]
                paint.color = Sculpt.withAlpha(accent, alphas[i] * boost * scale)
                canvas.drawPath(shape, paint)
            }
        }
        canvas.restoreToCount(saved)

        // 3. Gold frame.
        if (framed) {
            val frameAlpha = when {
                strong -> 1f
                big -> if (dark) 0.78f else 0.85f
                else -> if (dark) 0.55f else 0.65f
            } * (if (pressed) 0.85f else 1f)
            resetPaint(Paint.Style.STROKE)
            paint.strokeJoin = Paint.Join.MITER
            paint.strokeWidth = frameW
            paint.shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                IntArray(gold.size) { Sculpt.withAlpha(gold[it], frameAlpha) },
                GOLD_POS,
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(shape, paint)
            paint.shader = null
        }

        // 4. Inner frame line.
        if (strong || (big && framed)) {
            val gap = frameW * 2f
            inner.set(rect.left + gap, rect.top + gap, rect.right - gap, rect.bottom - gap)
            if (inner.width() > 4f && inner.height() > 4f) {
                chamferPath(innerShape, inner, max(cut - gap * 0.41f, 0f))
                resetPaint(Paint.Style.STROKE)
                paint.strokeJoin = Paint.Join.MITER
                paint.strokeWidth = max(sw * 0.7f, 1f)
                paint.color = if (accent != null) {
                    Sculpt.withAlpha(accent, if (pressed) 1f else 0.85f)
                } else {
                    Sculpt.withAlpha(gold[2], if (dark) 0.55f else 0.40f)
                }
                canvas.drawPath(innerShape, paint)
            }
        }

        // 5. Gold diamond studs, top and bottom centre, on large panels.
        if (big && framed) {
            val s = 3.4f * density
            drawStud(canvas, rect.centerX(), rect.top, s, gold, accent)
            drawStud(canvas, rect.centerX(), rect.bottom, s, gold, accent)
        }
    }

    private fun drawStud(canvas: Canvas, x: Float, y: Float, s: Float, gold: IntArray, accent: Int?) {
        stud.reset()
        stud.moveTo(x, y - s)
        stud.lineTo(x + s * 1.5f, y)
        stud.lineTo(x, y + s)
        stud.lineTo(x - s * 1.5f, y)
        stud.close()
        resetPaint(Paint.Style.FILL)
        paint.color = gold[1]
        canvas.drawPath(stud, paint)
        resetPaint(Paint.Style.STROKE)
        paint.strokeWidth = max(density * 0.7f, 1f)
        paint.color = gold[0]
        canvas.drawPath(stud, paint)
        resetPaint(Paint.Style.FILL)
        paint.color = accent ?: gold[0]
        canvas.drawCircle(x, y, s * 0.38f, paint)
    }

    private fun resetPaint(style: Paint.Style) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = style
    }

    /** Keeps a translucent fill translucent through the body gradient. */
    private fun withFillAlpha(color: Int): Int =
        Color.argb(Color.alpha(fill), Color.red(color), Color.green(color), Color.blue(color))

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Drawable", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getPadding(padding: Rect): Boolean = false

    companion object {
        /** Corner cut as a share of the caller's corner radius. */
        private const val CUT_RATIO = 0.62f
        /** The cut never exceeds this share of the short side. */
        private const val MAX_CUT_SHARE = 0.30f
        private val SHADOW_TINT = 0xFF3A2A0A.toInt()
        private val GOLD_POS = floatArrayOf(0f, 0.28f, 0.52f, 0.78f, 1f)
        private val DARK_GOLD = intArrayOf(
            0xFFF6D98B.toInt(), 0xFFD4A64A.toInt(), 0xFF8A6420.toInt(), 0xFFD4A64A.toInt(), 0xFFF6D98B.toInt(),
        )
        private val LIGHT_GOLD = intArrayOf(
            0xFFE6C36F.toInt(), 0xFFB8892F.toInt(), 0xFF7A5718.toInt(), 0xFFB8892F.toInt(), 0xFFE6C36F.toInt(),
        )

        /** An octagonal panel outline: [r] with each corner cut by [cut]. */
        fun chamferPath(path: Path, r: RectF, cut: Float) {
            val c = cut.coerceIn(0f, min(r.width(), r.height()) / 2f)
            path.reset()
            path.moveTo(r.left + c, r.top)
            path.lineTo(r.right - c, r.top)
            path.lineTo(r.right, r.top + c)
            path.lineTo(r.right, r.bottom - c)
            path.lineTo(r.right - c, r.bottom)
            path.lineTo(r.left + c, r.bottom)
            path.lineTo(r.left, r.bottom - c)
            path.lineTo(r.left, r.top + c)
            path.close()
        }
    }
}

/**
 * Traffic bars for a metric tile. Colour walks across the row, quiet bars
 * stay dim, and every bar carries a bright cap so a busy tile glows.
 */
class MicroBarsView(
    context: Context,
    private var barColor: Int,
    private var barColorAlt: Int = barColor,
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private val samples = ArrayDeque<Float>()
    private val maxBars = 11
    private val bar = RectF()

    fun setColors(primary: Int, secondary: Int = primary) {
        barColor = primary
        barColorAlt = secondary
        invalidate()
    }

    fun push(value: Float) {
        samples.addLast(value.coerceAtLeast(0f))
        while (samples.size > maxBars) samples.removeFirst()
        invalidate()
    }

    fun seed() {
        if (samples.isNotEmpty()) return
        repeat(maxBars) { samples.addLast(0f) }
        invalidate()
    }

    fun reset() {
        samples.clear()
        seed()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (samples.isEmpty()) return
        val peak = samples.maxOrNull() ?: 0f
        val gap = 1.8f * density
        val slot = (width - gap * (maxBars - 1)) / maxBars
        if (slot <= 0f) return
        val radius = 1.2f * density
        val cap = 1.6f * density
        samples.forEachIndexed { index, value ->
            // A zero peak means "no traffic yet": draw a 10% stub, never NaN.
            val ratio = if (peak <= 0f) 0.10f else (0.10f + 0.90f * (value / peak))
            val barHeight = height * ratio
            val left = index * (slot + gap)
            val hue = Sculpt.mix(barColor, barColorAlt, index / (maxBars - 1f))
            paint.shader = LinearGradient(
                0f, height - barHeight, 0f, height.toFloat(),
                Sculpt.withAlpha(hue, 0.30f + 0.55f * ratio), Sculpt.withAlpha(hue, 0.04f),
                Shader.TileMode.CLAMP,
            )
            bar.set(left, height - barHeight, left + slot, height.toFloat())
            canvas.drawRoundRect(bar, radius, radius, paint)
            paint.shader = null
            paint.color = Sculpt.withAlpha(hue, 0.55f + 0.45f * ratio)
            bar.set(left, height - barHeight, left + slot, height - barHeight + cap)
            canvas.drawRoundRect(bar, radius, radius, paint)
        }
        paint.shader = null
    }
}

/** A glowing trace with a soft fill and a bright head. */
class SparkLineView(
    context: Context,
    private var lineColor: Int,
) : View(context) {

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val density = resources.displayMetrics.density
    private val path = Path()
    private val fillPath = Path()
    private val samples = ArrayDeque<Float>()
    private val maxPoints = 14

    fun setColor(color: Int) {
        lineColor = color
        invalidate()
    }

    fun push(value: Float) {
        samples.addLast(value.coerceIn(0f, 1f))
        while (samples.size > maxPoints) samples.removeFirst()
        invalidate()
    }

    /** A gentle resting wave so the card never shows an empty box. */
    fun seed() {
        samples.clear()
        repeat(maxPoints) { index ->
            samples.addLast((0.35f + 0.2f * sin(index * 0.9f)).coerceIn(0f, 1f))
        }
        invalidate()
    }

    fun reset() = seed()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (samples.size < 2 || width <= 0 || height <= 0) return
        val inset = 3f * density
        val usableH = height - inset * 2
        val step = (width - inset) / (samples.size - 1)
        path.reset()
        var lastX = 0f
        var lastY = 0f
        samples.forEachIndexed { index, value ->
            val x = index * step
            val y = inset + usableH * (1f - value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            lastX = x
            lastY = y
        }
        fillPath.set(path)
        fillPath.lineTo(lastX, height.toFloat())
        fillPath.lineTo(0f, height.toFloat())
        fillPath.close()
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Sculpt.withAlpha(lineColor, 0.30f), Sculpt.withAlpha(lineColor, 0f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(fillPath, fillPaint)
        fillPaint.shader = null
        // Glow under the trace.
        stroke.shader = null
        stroke.strokeWidth = 4.5f * density
        stroke.color = Sculpt.withAlpha(lineColor, 0.18f)
        canvas.drawPath(path, stroke)
        stroke.strokeWidth = 1.7f * density
        stroke.shader = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            Sculpt.withAlpha(lineColor, 0.55f), lineColor,
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, stroke)
        stroke.shader = null
        // Bright head.
        fillPaint.color = Sculpt.withAlpha(lineColor, 0.28f)
        canvas.drawCircle(lastX, lastY, 4.8f * density, fillPaint)
        fillPaint.color = lineColor
        canvas.drawCircle(lastX, lastY, 2.2f * density, fillPaint)
    }
}

/** The home-screen footer: a gold-to-cyan wave with a caption. */
class OrbitFooterWave(
    context: Context,
    private val palette: AppAppearance.Palette,
    private val caption: String,
) : View(context) {

    private val wave = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typefaces.medium(context)
        // Latin-only: Persian/Chinese glyphs shatter under letter-spacing.
        letterSpacing = if (AppLanguage.current() != "en") 0f else 0.18f
    }
    private val density = resources.displayMetrics.density
    private val path = Path()
    private var lit = false
    private var running = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            postDelayed(this, 50L)
        }
    }

    fun setLit(value: Boolean) {
        if (lit == value) return
        lit = value
        syncTicker()
        invalidate()
    }

    private fun syncTicker() {
        val shouldRun = lit && isAttachedToWindow && windowVisibility == VISIBLE
        if (shouldRun == running) return
        running = shouldRun
        removeCallbacks(ticker)
        if (running) post(ticker)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        syncTicker()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        syncTicker()
    }

    override fun onDetachedFromWindow() {
        running = false
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val accent = if (lit) palette.connected else palette.primary
        val phase = if (lit) (SystemClock.uptimeMillis() % 4_000L) / 4_000f * (2f * Math.PI.toFloat()) else 0f
        val midY = height * 0.42f
        val amplitude = (if (lit) 5.5f else 2.2f) * density
        path.reset()
        val points = 48
        for (i in 0..points) {
            val t = i / points.toFloat()
            val x = width * t
            val envelope = sin(t * Math.PI.toFloat())
            val y = midY + amplitude * envelope *
                (sin(t * 6.2f + phase) * 0.7f + sin(t * 13f - phase * 1.6f) * 0.3f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val shader = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(
                Sculpt.withAlpha(palette.primary, 0f),
                Sculpt.withAlpha(palette.primary, if (lit) 0.55f else 0.30f),
                Sculpt.withAlpha(accent, if (lit) 0.95f else 0.45f),
                Sculpt.withAlpha(palette.primary, if (lit) 0.55f else 0.30f),
                Sculpt.withAlpha(palette.primary, 0f),
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP,
        )
        if (lit) {
            wave.shader = null
            wave.strokeWidth = 4.5f * density
            wave.color = Sculpt.withAlpha(accent, 0.14f)
            canvas.drawPath(path, wave)
        }
        wave.strokeWidth = 1.5f * density
        wave.shader = shader
        canvas.drawPath(path, wave)
        wave.shader = null

        text.textSize = 8.5f * density
        text.color = Sculpt.withAlpha(palette.faint, if (lit) 0.95f else 0.7f)
        canvas.drawText(caption, width / 2f, height * 0.92f, text)
    }
}
