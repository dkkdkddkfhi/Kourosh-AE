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
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The Aurora widget set: the pieces the home console is assembled from.
 *
 * Every class here is view-based and hand-drawn. The app ships no Compose runtime and
 * no Material components, and a 49 MB APK is already almost entirely native
 * libraries — so "nicer" has to come from drawing, not from a dependency.
 */

// ---------------------------------------------------------------------------- tile

/**
 * One live rate: caption, value, and a meter that fills with the sample.
 *
 * Replaces the old waveform tile. The waveform was a good idea executed three times
 * over — three identical sparklines in three accents is a dashboard, not a control
 * panel, and the eye had no way to know which of the three mattered. Here each tile
 * carries a *meter* instead: a single horizontal bar whose length IS the value, which
 * is readable in peripheral vision in a way a 16dp sparkline never was. The
 * fine-grained history now lives in exactly one place, the traffic chart above them.
 *
 * The accent is spent on the caption, the meter's fill and its glow; the number
 * itself stays ink so the three tiles can be compared without colour getting in the
 * way.
 */
class AuroraStatTile(
    context: Context,
    private val palette: AppAppearance.Palette,
    caption: String,
    private val accent: Int,
    private val accentText: Int,
    onClick: () -> Unit,
) : LinearLayout(context) {

    private val valueView: TextView
    private val unitView: TextView
    private val row: LinearLayout
    private val meter: MeterView

    init {
        orientation = VERTICAL
        background = Aurora.ripplePanel(
            context, palette, Aurora.RADIUS_CELL,
            accent = accent,
            fill = Sculpt.blend(palette.surface, palette.ink, 0.028f),
        )
        setPadding(Aurora.px(context, 11), Aurora.px(context, 10), Aurora.px(context, 11), Aurora.px(context, 11))
        isClickable = true
        isFocusable = true
        contentDescription = caption
        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onClick()
        }

        val head = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(View(context).apply {
            background = Sculpt.sculptedBackground(
                Aurora.density(context), accent, Aurora.RADIUS_PILL,
            )
        }, LayoutParams(Aurora.px(context, 5), Aurora.px(context, 5)).apply {
            rightMargin = Aurora.px(context, 6)
        })
        head.addView(
            Aurora.label(
                context, caption, Aurora.CAPTION_SP, Sculpt.withAlpha(accentText, 0.95f),
                face = Aurora.Face.MEDIUM, tracking = 0.12f,
            ),
            LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        addView(head, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
        }
        valueView = Aurora.label(
            context, "0", 19f, palette.ink,
            face = Aurora.Face.MONO,
        )
        unitView = Aurora.label(
            context, "B/S", 8.5f, Sculpt.withAlpha(palette.faint, 0.95f),
            face = Aurora.Face.MEDIUM, tracking = 0.08f,
        )
        row.addView(valueView)
        row.addView(unitView, LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            leftMargin = Aurora.px(context, 3)
            bottomMargin = Aurora.px(context, 3)
        })
        addView(row, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = Aurora.px(context, 2) })

        meter = MeterView(context, palette, accent)
        addView(meter, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            Aurora.px(context, 4),
        ).apply { topMargin = Aurora.px(context, 8) })
    }

    private companion object {
        /** Designed size for the number; anything that fits keeps exactly this. */
        const val VALUE_MAX_SP = 19f
        /** Floor for the shrink — still readable while it is animating. */
        const val VALUE_MIN_SP = 11f
    }

    /** [value] is pre-scaled for display; [unit] is its suffix, e.g. "MB/S". */
    fun setValue(value: String, unit: String) {
        valueView.text = value
        unitView.text = unit
        fitValue()
    }

    /**
     * Shrinks the number until it and its unit share the tile.
     *
     * A rate can read `10240` KB/S on a busy tunnel in a tile that is a third of the
     * screen wide, and with a raised system font scale the digits overflow and push
     * the unit out of the tile. Measured against the real row width so density and
     * font scale resolve themselves; values that already fit keep the design size.
     */
    private fun fitValue() {
        row.post {
            val rowWidth = row.width
            if (rowWidth <= 0) return@post
            val gap = (unitView.layoutParams as? LayoutParams)?.leftMargin ?: 0
            val unitWidth = if (unitView.width > 0) unitView.width
                else unitView.paint.measureText(unitView.text.toString()).toInt()
            val available = rowWidth - unitWidth - gap
            var size = VALUE_MAX_SP
            while (size >= VALUE_MIN_SP) {
                valueView.textSize = size * FontChoice.scale(context)
                if (valueView.paint.measureText(valueView.text.toString()).toInt() <= available) break
                size -= 1f
            }
        }
    }

    /** Feeds the meter. [sample] is 0..1 against the tile's own full scale. */
    fun push(sample: Float) = meter.setLevel(sample)

    fun resetWave() = meter.setLevel(0f)

    /**
     * The connected theme's "undim".
     *
     * The tiles idle at 72% and come fully up with the tunnel. Deliberately gentler
     * than the old 55%: at 55% the idle screen looked disabled, which is the wrong
     * signal for a switch that is only waiting for a tap.
     */
    fun dim(active: Boolean) {
        animate().alpha(if (active) 1f else 0.72f).setDuration(320L).start()
    }
}

/**
 * The meter under a tile's number.
 *
 * A recessed track with a lit fill whose *width* is the value, animated with a
 * catch-up tween so a burst of traffic reads as movement rather than as a jump cut.
 * Drawn by hand because a `ProgressBar` would arrive with the platform's own
 * minimum-height and tint rules, and this control has to be 4dp tall.
 */
private class MeterView(
    context: Context,
    private val palette: AppAppearance.Palette,
    private val accent: Int,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var level = 0f
    private var shown = 0f
    private var animator: ValueAnimator? = null

    fun setLevel(value: Float) {
        val target = value.coerceIn(0f, 1f)
        if (abs(target - level) < 0.001f) return
        level = target
        animator?.cancel()
        animator = ValueAnimator.ofFloat(shown, target).apply {
            duration = 420L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                shown = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        if (h <= 0f || width <= 0) return
        val radius = h / 2f
        rect.set(0f, 0f, width.toFloat(), h)
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = if (Sculpt.lighting.elevationDp > 0f) {
            Sculpt.withAlpha(Color.BLACK, 0.07f)
        } else {
            Sculpt.withAlpha(Color.WHITE, 0.08f)
        }
        canvas.drawRoundRect(rect, radius, radius, paint)
        if (shown <= 0.005f) return
        val filled = (width * shown).coerceAtLeast(h)
        paint.shader = LinearGradient(
            0f, 0f, filled, 0f,
            intArrayOf(Sculpt.withAlpha(accent, 0.55f), accent),
            null, Shader.TileMode.CLAMP,
        )
        rect.set(0f, 0f, filled, h)
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null
        // The head of the bar gets a small bloom, which is what makes a full meter
        // read as "busy" from across the room.
        paint.color = Sculpt.withAlpha(accent, 0.35f)
        canvas.drawCircle(filled - radius, h / 2f, radius * 1.6f, paint)
    }
}

// --------------------------------------------------------------------------- chart

/**
 * The live traffic chart: download over upload, filled, smoothed, scrolling.
 *
 * This is the one place in the app that keeps traffic *history*, and it exists
 * because a number cannot show shape. Two rates rendered as two filled areas make the
 * difference between a tunnel that is idle and one that is stuttering legible
 * instantly — which is the question a user opens this screen to answer.
 *
 * Implementation notes that matter: the series are Bezier-smoothed
 * (Catmull-Rom control points) rather than joined with straight segments, so a 256-point
 * history does not look like a bar chart; both fills use a vertical gradient that
 * fades to transparent at the baseline, which is what stops the two areas from
 * fighting where they overlap; and the samples live in fixed arrays that are written
 * in place — no allocation per frame, which matters because this repaints on every
 * traffic tick.
 */
class AuroraTrafficChart(
    context: Context,
    private val palette: AppAppearance.Palette,
) : View(context) {

    private var downColor = palette.mint
    private var upColor = palette.violet

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val downPath = Path()
    private val upPath = Path()
    private val downArea = Path()
    private val upArea = Path()
    private val rect = RectF()

    private val rx = FloatArray(POINTS)
    private val tx = FloatArray(POINTS)
    private var head = 0
    private var filled = 0

    init {
        // No software layer: everything here is a stroked or filled path, which the
        // hardware pipeline draws natively. The blurred shapes on this screen are the
        // ones that ask for software, and they are separate views.
        contentDescription = "نمودار زنده ترافیک"
    }

    fun setColors(down: Int, up: Int) {
        downColor = down
        upColor = up
        invalidate()
    }

    /** [rx] and [tx] are 0..1 against the chart's full scale. */
    fun push(rxSample: Float, txSample: Float) {
        rx[head] = rxSample.coerceIn(0f, 1f)
        tx[head] = txSample.coerceIn(0f, 1f)
        head = (head + 1) % POINTS
        if (filled < POINTS) filled++
        invalidate()
    }

    fun reset() {
        rx.fill(0f)
        tx.fill(0f)
        head = 0
        filled = 0
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Baseline rules: two hairlines, the upper one at the chart's full scale and
        // the lower one at the floor. They give the moving areas a plane to sit on.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = if (Sculpt.lighting.elevationDp > 0f) {
            Sculpt.withAlpha(Color.BLACK, 0.06f)
        } else {
            Sculpt.withAlpha(Color.WHITE, 0.07f)
        }
        canvas.drawLine(0f, h - density, w, h - density, paint)
        canvas.drawLine(0f, h * 0.5f, w, h * 0.5f, paint)

        if (filled < 2) return

        build(uplink = false)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Sculpt.withAlpha(downColor, 0.30f), Sculpt.withAlpha(downColor, 0f)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawPath(downArea, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density * 1.7f
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = downColor
        canvas.drawPath(downPath, paint)

        build(uplink = true)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Sculpt.withAlpha(upColor, 0.26f), Sculpt.withAlpha(upColor, 0f)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawPath(upArea, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density * 1.7f
        paint.color = upColor
        canvas.drawPath(upPath, paint)
        paint.strokeJoin = Paint.Join.MITER

        // The leading dot, on the newest download sample.
        val newest = (head - 1 + POINTS) % POINTS
        val x = w - density * 2f
        val y = h - rx[newest] * h * 0.86f - density * 2f
        paint.style = Paint.Style.FILL
        paint.color = Sculpt.withAlpha(Color.WHITE, 0.9f)
        canvas.drawCircle(x, y, density * 2.1f, paint)
        paint.color = Sculpt.withAlpha(downColor, 0.55f)
        canvas.drawCircle(x, y, density * 4.2f, paint)
    }

    /**
     * Builds one series.
     *
     * Oldest sample is on the left, newest on the right, so the chart scrolls the way
     * it is read. Points are laid out across the full width regardless of how full the
     * history is — with [filled] growing from zero, so the chart fills up from the left
     * after a reset instead of starting as a stretched two-point line.
     */
    private fun build(uplink: Boolean) {
        val source = if (uplink) tx else rx
        val line = if (uplink) upPath else downPath
        val area = if (uplink) upArea else downArea
        val w = width.toFloat()
        val h = height.toFloat()
        val step = w / (POINTS - 1)
        val baseline = h - density

        fun valueAt(index: Int): Float = source[(head - filled + index + POINTS * 2) % POINTS]

        val count = filled
        val pointsX = FloatArray(count)
        val pointsY = FloatArray(count)
        for (index in 0 until count) {
            pointsX[index] = index * step
            pointsY[index] = baseline - valueAt(index) * h * 0.86f
        }

        line.reset()
        line.moveTo(pointsX[0], pointsY[0])
        for (index in 0 until count - 1) {
            // Catmull-Rom → cubic control points: each segment is shaped by its
            // neighbours, which removes the visible corners a polyline leaves on a
            // burst of traffic.
            val p0x = pointsX[(index - 1).coerceAtLeast(0)]
            val p0y = pointsY[(index - 1).coerceAtLeast(0)]
            val p1x = pointsX[index]
            val p1y = pointsY[index]
            val p2x = pointsX[index + 1]
            val p2y = pointsY[index + 1]
            val p3x = pointsX[(index + 2).coerceAtMost(count - 1)]
            val p3y = pointsY[(index + 2).coerceAtMost(count - 1)]
            line.cubicTo(
                p1x + (p2x - p0x) / 6f, p1y + (p2y - p0y) / 6f,
                p2x - (p3x - p1x) / 6f, p2y - (p3y - p1y) / 6f,
                p2x, p2y,
            )
        }

        area.set(line)
        area.lineTo(pointsX[count - 1], baseline)
        area.lineTo(pointsX[0], baseline)
        area.close()
    }

    private companion object {
        /** History depth. 56 points at one sample per traffic tick ≈ 4 minutes. */
        const val POINTS = 56
    }
}

// --------------------------------------------------------------------------- trace

/**
 * The compact single-series trace used inside cards (the exit node, the header).
 *
 * Filled under the line rather than stroked above a baseline: at 24dp tall a filled
 * shape reads at a glance and a 1.6dp polyline does not.
 */
class AuroraMiniTrace(
    context: Context,
    palette: AppAppearance.Palette,
    private var color: Int,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val area = Path()

    private val samples = FloatArray(POINTS)
    private var head = 0
    private var filled = 0

    init {
        // Seeded flat-but-alive, so the card never shows an empty box on a cold start
        // and the shape has somewhere to move from.
        seed()
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setColor(next: Int) {
        color = next
        invalidate()
    }

    fun push(sample: Float) {
        samples[head] = sample.coerceIn(0f, 1f)
        head = (head + 1) % POINTS
        if (filled < POINTS) filled++
        invalidate()
    }

    /**
     * A gentle idle shape — not data, just a surface that is not empty.
     *
     * A flat line at zero reads as a broken widget on a cold start, and it gives the
     * real samples nothing to rise out of. The wave is deterministic, so every launch
     * looks the same.
     */
    fun seed() {
        for (index in 0 until POINTS) {
            samples[index] = 0.18f + 0.16f * (0.5f + 0.5f * sin(index * 0.55f))
        }
        filled = POINTS
        head = 0
        invalidate()
    }

    fun reset() = seed()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || filled < 2) return
        val step = w / (POINTS - 1)
        val baseline = h - density * 0.5f
        path.reset()
        for (index in 0 until POINTS) {
            val value = samples[(head + index) % POINTS]
            val x = index * step
            val y = baseline - value * h * 0.88f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        area.set(path)
        area.lineTo(w, baseline)
        area.lineTo(0f, baseline)
        area.close()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Sculpt.withAlpha(color, 0.35f), Sculpt.withAlpha(color, 0f)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawPath(area, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density * 1.5f
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = color
        canvas.drawPath(path, paint)
    }

    private companion object {
        const val POINTS = 26
    }
}

// ---------------------------------------------------------------------------- rail

/**
 * The transport picker.
 *
 * ## Why a grid of pills instead of the old segmented rail
 *
 * The previous control was one recessed well with six labels and a lit thumb sliding
 * between them, plus a hairline grid drawn in `onDraw`. It worked, but at six
 * transports the cells were text-only and every option looked equally weighted —
 * including the one that was selected, which was indicated by a slightly brighter
 * fill. For the single most consequential choice on the screen that is too quiet.
 *
 * Each transport is now its own pill with a glyph, and selection is stated four ways
 * at once: an accent gradient fill, a lit frame, the glyph taking the accent, and the
 * label going to full ink. Unselected pills sink into the well. The grid still owns
 * the geometry — [rowCount] is still derived, so the console can size the band — and
 * the API is unchanged: pick an index, get a callback.
 */
class AuroraProtocolRail(
    context: Context,
    private val palette: AppAppearance.Palette,
    private val labels: List<String>,
    private val perRow: Int = 3,
    private val glyphs: List<AuroraIcon> = emptyList(),
    private val onPick: (Int) -> Unit,
) : LinearLayout(context) {

    private val cells = mutableListOf<Cell>()
    private var selectedIndex = 0

    /** How many rows the labels need at [perRow] columns. */
    val rowCount: Int = if (labels.isEmpty()) 1 else (labels.size + perRow - 1) / perRow

    private val columns: Int = perRow.coerceAtMost(labels.size.coerceAtLeast(1))

    private inner class Cell(
        val container: FrameLayout,
        val icon: AuroraIconView,
        val text: TextView,
    )

    init {
        orientation = VERTICAL
        background = Aurora.well(context, palette, Aurora.RADIUS_WELL)
        setPadding(Aurora.px(context, 6), Aurora.px(context, 6), Aurora.px(context, 6), Aurora.px(context, 6))

        var index = 0
        for (row in 0 until rowCount) {
            val line = LinearLayout(context).apply { orientation = HORIZONTAL }
            for (column in 0 until columns) {
                if (index >= labels.size) {
                    line.addView(
                        View(context),
                        LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
                    )
                    continue
                }
                val cell = buildCell(index)
                cells.add(cell)
                line.addView(
                    cell.container,
                    LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                        if (column > 0) leftMargin = Aurora.px(context, 6)
                    },
                )
                index++
            }
            addView(line, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                if (row > 0) topMargin = Aurora.px(context, 6)
            })
        }
        paintSelection(animate = false)
    }

    private fun buildCell(index: Int): Cell {
        val container = FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            contentDescription = labels[index]
            setOnClickListener {
                if (!isEnabled) return@setOnClickListener
                performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onPick(index)
            }
        }
        val inner = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            isDuplicateParentStateEnabled = true
        }
        val icon = AuroraIconView(
            context,
            glyphs.getOrElse(index) { AuroraIcon.SHIELD },
            palette.faint,
            strokeDp = 1.5f,
        )
        inner.addView(icon, LayoutParams(Aurora.px(context, 17), Aurora.px(context, 17)).apply {
            rightMargin = Aurora.px(context, 7)
        })
        val text = Aurora.label(
            context, labels[index], 12f, palette.faint,
            face = Aurora.Face.MEDIUM, tracking = 0.04f,
            gravity = Gravity.CENTER_VERTICAL,
        ).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        inner.addView(text, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(inner, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))
        return Cell(container, icon, text)
    }

    /** Applies the selected look to every cell. */
    private fun paintSelection(animate: Boolean) {
        cells.forEachIndexed { index, cell ->
            val on = index == selectedIndex
            val accent = palette.primary
            val fill = if (on) {
                Sculpt.blend(palette.surface, accent, 0.14f)
            } else {
                Sculpt.blend(palette.surface, palette.ink, 0.02f)
            }
            cell.container.background = if (on) {
                Sculpt.sculptedRipple(
                    Aurora.density(context), fill, Aurora.RADIUS_CELL, accent,
                    accent = Sculpt.withAlpha(accent, 0.60f),
                )
            } else {
                Sculpt.sculptedRipple(
                    Aurora.density(context), fill, Aurora.RADIUS_CELL, palette.faint,
                    accent = Sculpt.withAlpha(palette.ink, 0.08f),
                )
            }
            cell.icon.setColor(if (on) Sculpt.onGlass(accent) else Sculpt.withAlpha(palette.faint, 0.9f))
            cell.text.setTextColor(if (on) palette.ink else palette.faint)
            if (animate) {
                cell.container.scaleX = if (on) 0.96f else 1f
                cell.container.scaleY = if (on) 0.96f else 1f
                cell.container.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(220L)
                    .setInterpolator(DecelerateInterpolator(1.6f))
                    .start()
            }
        }
    }

    /** Moves the selection. [animate] adds the settle on the newly selected cell. */
    fun select(index: Int, animate: Boolean) {
        if (index !in labels.indices || index == selectedIndex) {
            if (index in labels.indices) {
                selectedIndex = index
                paintSelection(animate = false)
            }
            return
        }
        selectedIndex = index
        paintSelection(animate)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        cells.forEach { it.container.isEnabled = enabled }
        // The lock is stated on the whole control, not per cell: a tunnel is up, so
        // every transport is equally unavailable until it comes down.
        animate().alpha(if (enabled) 1f else 0.55f).setDuration(220L).start()
    }
}

// ---------------------------------------------------------------------------- wave

/**
 * The strip that closes the screen.
 *
 * Two animated sine layers under a caption. It carries no data — the previous footer
 * tried to imply activity and, being flat grey at idle, looked like a rendering
 * artefact. This one is honest about being decoration: it always moves, it takes the
 * state's accent, and it sits under a caption that states the same fact the header
 * does. It is the only ambient motion on the screen, which is what keeps the dial
 * feeling like the centre of attention.
 */
class AuroraWaveStrip(
    context: Context,
    private val palette: AppAppearance.Palette,
    caption: String,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private var lit = false
    private var phase = 0f
    private var accent = palette.primary

    private val captionPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Sculpt.withAlpha(palette.faint, 0.9f)
        textAlign = Paint.Align.CENTER
        textSize = 8.5f * density * FontChoice.scale(context)
        typeface = Typefaces.medium(context)
        letterSpacing = if (AppLanguage.current() == "en") 0.16f else 0f
    }

    private val captionText = caption

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2_600L
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            phase = it.animatedFraction
            invalidate()
        }
    }

    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * Connected state. Brightens the trace and doubles its amplitude — the strip is
     * part of the "connected is a lighting state" rule, so it has to come up with the
     * tunnel and go flat the moment it drops.
     */
    fun setLit(value: Boolean) {
        if (lit == value) return
        lit = value
        invalidate()
    }

    fun setAccent(color: Int) {
        if (accent == color) return
        accent = color
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val mid = h * 0.42f
        val amplitude = h * (if (lit) 0.30f else 0.16f)

        val layers = 2
        for (layer in 0 until layers) {
            val direction = if (layer == 0) 1f else -1f
            val offset = phase * direction * TWO_PI + layer * 1.7f
            val alpha = if (lit) (0.85f - layer * 0.35f) else (0.42f - layer * 0.20f)
            paint.strokeWidth = density * (if (layer == 0) 1.5f else 1.1f)
            paint.color = Sculpt.withAlpha(
                if (layer == 0) accent else Sculpt.blend(accent, palette.ink, 0.5f),
                alpha.coerceAtLeast(0.06f),
            )
            path.reset()
            val steps = 48
            for (index in 0..steps) {
                val t = index / steps.toFloat()
                val y = mid + sin(t * TWO_PI * 1.6f + offset) * amplitude * (0.55f + 0.45f * sin(t * Math.PI).toFloat())
                if (index == 0) path.moveTo(t * w, y) else path.lineTo(t * w, y)
            }
            canvas.drawPath(path, paint)
        }

        captionPaint.color = Sculpt.withAlpha(if (lit) accent else palette.faint, if (lit) 0.95f else 0.85f)
        canvas.drawText(captionText, w / 2f, h - density * 2f, captionPaint)
    }

    private companion object {
        const val TWO_PI = 6.2831855f
    }
}

// ---------------------------------------------------------------------------- dock

/**
 * The elevated connect control in the navigation dock.
 *
 * On the home screen the dial is the control; this is the same action reachable from
 * everywhere else in the app, and its job is to state the tunnel's state in one
 * glance from any scroll position. It is a filled disc with a lit rim and a soft
 * bloom, and it is deliberately *not* a smaller copy of the dial — a miniature dial
 * competing with the real one would be the loudest thing in the dock.
 */
class AuroraDockButton(
    context: Context,
    private val palette: AppAppearance.Palette,
) : View(context) {

    var state: AuroraDialView.State = AuroraDialView.State.DISCONNECTED
        set(value) {
            field = value
            contentDescription = when (value) {
                AuroraDialView.State.CONNECTED, AuroraDialView.State.DEGRADED -> "قطع اتصال"
                AuroraDialView.State.CONNECTING -> "در حال اتصال"
                else -> "اتصال"
            }
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2_600L
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            phase = it.animatedFraction
            invalidate()
        }
    }

    init {
        isClickable = true
        isFocusable = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        contentDescription = "اتصال"
    }

    private fun accent(): Int = when (state) {
        AuroraDialView.State.CONNECTED -> palette.connected
        AuroraDialView.State.DEGRADED -> palette.amber
        AuroraDialView.State.CONNECTING -> palette.amber
        AuroraDialView.State.FAILED -> palette.danger
        AuroraDialView.State.DISCONNECTED -> palette.primary
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * 0.42f
        if (r <= 0f) return
        val accent = accent()
        val active = state == AuroraDialView.State.CONNECTED || state == AuroraDialView.State.DEGRADED

        // bloom
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy - r * 0.2f, r * 1.65f,
            intArrayOf(
                Sculpt.withAlpha(accent, if (active) 0.30f else 0.18f),
                Sculpt.withAlpha(accent, 0f),
            ),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 1.6f, paint)

        // body
        paint.shader = LinearGradient(
            0f, cy - r, 0f, cy + r,
            intArrayOf(
                Sculpt.lighten(palette.surface, lightLift() + 0.05f),
                Sculpt.blend(palette.surface, accent, 0.10f),
                Sculpt.darken(palette.surface, 0.22f),
            ),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)

        // rim
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density * 1.6f
        paint.color = Sculpt.withAlpha(accent, 0.95f)
        canvas.drawCircle(cx, cy, r, paint)
        paint.strokeWidth = density
        paint.color = Sculpt.withAlpha(accent, 0.35f)
        canvas.drawCircle(cx, cy, r - density * 4f, paint)

        // a single travelling arc while the tunnel is being dialled
        if (state == AuroraDialView.State.CONNECTING) {
            val ring = r - density * 6f
            bounds.set(cx - ring, cy - ring, cx + ring, cy + ring)
            paint.strokeWidth = density * 2f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Sculpt.withAlpha(accent, 0.9f)
            canvas.drawArc(bounds, phase * 360f, 84f, false, paint)
            paint.strokeCap = Paint.Cap.BUTT
        }

        // power glyph
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = density * 2.6f
        paint.color = Sculpt.onGlass(accent)
        val glyph = r * 0.42f
        bounds.set(cx - glyph, cy - glyph, cx + glyph, cy + glyph)
        canvas.drawArc(bounds, 118f, 304f, false, paint)
        canvas.drawLine(cx, cy - glyph * 1.30f, cx, cy - glyph * 0.14f, paint)
        paint.strokeCap = Paint.Cap.BUTT
        paint.style = Paint.Style.FILL

        if (active) {
            paint.color = Sculpt.onGlass(palette.mint)
            canvas.drawCircle(cx + r * 0.62f, cy - r * 0.62f, density * 3.4f, paint)
        }
    }

    private fun lightLift(): Float = Sculpt.lighting.dialBodyLift
}

/**
 * One entry in the navigation dock.
 *
 * Icon over label, with the selected entry sitting on a lit pill. The previous dock
 * gave every entry the same treatment and marked the current one by nothing at all —
 * the bar answered "where am I" only for the centre button.
 */
class AuroraNavItem(
    context: Context,
    private val palette: AppAppearance.Palette,
    icon: AuroraIcon,
    title: String,
    onClick: () -> Unit,
) : LinearLayout(context) {

    private val iconView: AuroraIconView
    private val label: TextView
    private val content: LinearLayout

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        contentDescription = title
        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onClick()
        }

        content = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            isDuplicateParentStateEnabled = true
        }
        iconView = AuroraIconView(context, icon, palette.muted, strokeDp = 1.7f)
        content.addView(iconView, LayoutParams(Aurora.px(context, 21), Aurora.px(context, 21)))
        label = Aurora.label(
            context, title, 8.5f, Sculpt.withAlpha(palette.muted, 0.9f),
            face = Aurora.Face.MEDIUM, tracking = 0.10f, gravity = Gravity.CENTER,
        ).apply { maxLines = 1 }
        content.addView(label, LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = Aurora.px(context, 3) })
        addView(content, LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
    }

    /** Paints the current-page state. */
    fun setActive(active: Boolean) {
        val accent = palette.primary
        content.background = if (active) {
            Aurora.pill(context, palette, accent, filled = true, alpha = 0.16f)
        } else {
            null
        }
        content.setPadding(
            Aurora.px(context, if (active) 12 else 0),
            Aurora.px(context, if (active) 5 else 0),
            Aurora.px(context, if (active) 12 else 0),
            Aurora.px(context, if (active) 5 else 0),
        )
        iconView.setColor(if (active) Sculpt.onGlass(accent) else palette.muted)
        label.setTextColor(if (active) Sculpt.onGlass(accent) else Sculpt.withAlpha(palette.muted, 0.9f))
        contentDescription = label.text
    }
}

// ---------------------------------------------------------------------------- hero

/**
 * The hero panel — the state-tinted surface the dial sits in.
 *
 * A [LinearLayout] with one extra responsibility: it repaints its own background from
 * the connection state, so the whole panel takes the accent when the tunnel comes up.
 * Keeping it in the view (rather than rebuilding the drawable from the Activity on
 * every broadcast) means the transition is guarded here, in one place, and the
 * `CONNECTING` state — which broadcasts a dozen times per attempt — cannot restart an
 * animation.
 */
class AuroraHeroPanel(
    context: Context,
    private val palette: AppAppearance.Palette,
) : LinearLayout(context) {

    private var accent = palette.primary
    private var lit = false

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        clipChildren = false
        clipToPadding = false
        paint()
    }

    fun setAccent(color: Int, bright: Boolean) {
        if (accent == color && lit == bright) return
        accent = color
        lit = bright
        paint()
    }

    private fun paint() {
        background = Aurora.panel(
            context,
            palette,
            Aurora.RADIUS_HERO,
            accent = Sculpt.withAlpha(accent, if (lit) 0.75f else 0.42f),
            lit = lit,
            glow = lit,
        )
    }
}
