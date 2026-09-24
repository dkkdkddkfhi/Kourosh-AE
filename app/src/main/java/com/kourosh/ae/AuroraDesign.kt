package com.kourosh.ae

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * # Aurora — the Kourosh-AE home language.
 *
 * This file owns everything the new main screen is built from: the corner radii,
 * the type steps, the panel painter, the icon set and the ambient backdrop. It
 * replaced the previous hand-built console, and the rule it was written against is
 * that a surface is never a flat rectangle with a hairline.
 *
 * ## What "nicer" means here, concretely
 *
 * The old home screen was a stack of near-identical cards: same radius, same
 * neutral fill, same 1px outline, all lit by the same `GlassDrawable`. Nothing led
 * the eye. Aurora changes four things:
 *
 *  1. **A real hierarchy of surfaces.** The hero panel is the largest object on the
 *     screen and the only one with a *state-tinted* body ([AuroraPanelDrawable]'s
 *     aurora wash); the cards under it are deliberately quieter — smaller radius,
 *     no tint, a hairline instead of a frame. One loud object, everything else
 *     accompanying it.
 *  2. **Depth from light, not from borders.** Panels carry a top specular, a body
 *     gradient and an inner bottom shadow, so they read as machined glass. On the
 *     light palette the same three layers invert into a soft drop shadow and a
 *     bottom-weighted bevel (see [Sculpt.Lighting]).
 *  3. **A single accent per state, spent in one place.** Gold at rest, amber while
 *     dialling, mint when the tunnel is up, red only on failure — and the accent is
 *     allowed to appear at full strength exactly once per screen (the dial's ring
 *     and its bloom). Everywhere else it is a caption, a dot or a 1dp frame.
 *  4. **Type that carries the reading order.** Micro captions are 8.5sp, uppercase
 *     and tracked; values are large, monospaced and untracked; nothing in between is
 *     used for two different jobs.
 */
object Aurora {

    // ------------------------------------------------------------------ metrics

    /** The hero panel — the one object the eye should land on. */
    const val RADIUS_HERO = 30

    /** An ordinary card in the column under the hero. */
    const val RADIUS_CARD = 22

    /** A recessed group inside a card (the protocol grid's well). */
    const val RADIUS_WELL = 20

    /** A cell inside a well. */
    const val RADIUS_CELL = 16

    /** Pills, chips, badges — always fully rounded. */
    const val RADIUS_PILL = 999

    /** Horizontal page gutter for the whole console. */
    const val GUTTER = 18

    /** Vertical gap between two bands of the console. */
    const val GAP = 12

    // -------------------------------------------------------------------- type

    /**
     * Micro caption: the eyebrow above a value ("EXIT NODE", "DOWNLOAD").
     *
     * 8.5sp with 0.14 tracking. Latin only — [label] zeroes the tracking for
     * Persian and Chinese, whose joined glyphs shatter under letter-spacing.
     */
    const val CAPTION_SP = 8.5f

    /** Body / row text. */
    const val BODY_SP = 13f

    /** A primary readout (an IP, a headline). */
    const val VALUE_SP = 15f

    /** The largest number on the screen (the dial's timer). */
    const val HERO_SP = 17f

    // ------------------------------------------------------------------ colours

    /** Which typeface a label wears. */
    enum class Face { REGULAR, MEDIUM, BOLD, MONO }

    inline fun density(context: Context): Float = context.resources.displayMetrics.density

    fun px(context: Context, value: Int): Int = (value * density(context)).roundToInt()

    fun px(context: Context, value: Float): Int = (value * density(context)).roundToInt()

    /**
     * The one label factory for the home screen.
     *
     * Wraps the per-language rules that the whole app has to obey — the bundled
     * Persian/Chinese faces, the extra line height those faces need — so a new
     * widget cannot forget them and produce a console whose Persian twin is a
     * different height from its English one.
     */
    fun label(
        context: Context,
        text: String = "",
        size: Float,
        color: Int,
        face: Face = Face.REGULAR,
        tracking: Float = 0f,
        gravity: Int = Gravity.START,
    ): TextView = TextView(context).apply {
        this.text = text
        textSize = size * FontChoice.scale(context)
        setTextColor(color)
        this.gravity = gravity
        letterSpacing = if (AppLanguage.current() == "en") tracking else 0f
        typeface = faceOf(context, face)
        if (AppLanguage.current() != "en") setLineSpacing(0f, Typefaces.lineHeightMult())
    }

    fun faceOf(context: Context, face: Face): android.graphics.Typeface = when (face) {
        Face.REGULAR -> Typefaces.regular(context)
        Face.MEDIUM -> Typefaces.medium(context)
        Face.BOLD -> Typefaces.bold(context)
        Face.MONO -> Typefaces.mono(context)
    }

    /**
     * Blend of a palette surface towards a tint — the standard way this screen
     * says "this surface belongs to the connected state" without changing its
     * luminance.
     */
    fun tinted(base: Int, tint: Int, amount: Float): Int = Sculpt.blend(base, tint, amount)

    /** `surface` nudged towards the page, used for the body of a quiet card. */
    fun cardFill(palette: AppAppearance.Palette): Int =
        Sculpt.blend(palette.surface, palette.ink, 0.035f)

    /** The fill of a recessed well: one step *below* the card it sits in. */
    fun wellFill(palette: AppAppearance.Palette): Int =
        if (Sculpt.lighting.elevationDp > 0f) {
            Sculpt.blend(palette.surface, palette.ink, 0.06f)
        } else {
            Sculpt.recess(palette.surface, 0.22f)
        }

    // ----------------------------------------------------------------- drawables

    /**
     * The raised surface of the new console.
     *
     * Four layers, painted in this order: body gradient → top specular → inner
     * bottom shadow → frame. When [lit] the body is washed with [accent] instead of
     * the neutral highlight and the frame is drawn at full strength, which is what
     * makes a panel read as *on* rather than merely outlined.
     *
     * Returns a [StateListDrawable] so any clickable view wearing it sinks on press
     * — the same contract `Sculpt.sculptedBackground` has, kept deliberately so a
     * card can be made tappable later without touching its background.
     */
    fun panel(
        context: Context,
        palette: AppAppearance.Palette,
        radius: Int,
        accent: Int? = null,
        lit: Boolean = false,
        fill: Int = cardFill(palette),
        glow: Boolean = false,
    ): Drawable {
        val density = density(context)
        fun layer(pressed: Boolean) = AuroraPanelDrawable(
            density = density,
            fill = fill,
            radiusDp = radius.toFloat(),
            accent = accent,
            lit = lit,
            pressed = pressed,
            glow = glow,
        )
        return StateListDrawable().apply {
            setEnterFadeDuration(0)
            setExitFadeDuration(140)
            addState(intArrayOf(android.R.attr.state_pressed), layer(true))
            addState(intArrayOf(), layer(false))
        }
    }

    /** Same surface, with a ripple on top. Used for anything that opens a page. */
    fun ripplePanel(
        context: Context,
        palette: AppAppearance.Palette,
        radius: Int,
        accent: Int,
        fill: Int = cardFill(palette),
        glow: Boolean = false,
    ): Drawable {
        val density = density(context)
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius * density
            setColor(Color.WHITE)
        }
        return RippleDrawable(
            ColorStateList.valueOf(Sculpt.withAlpha(accent, 0.18f)),
            panel(context, palette, radius, accent = Sculpt.withAlpha(accent, 0.45f), fill = fill, glow = glow),
            mask,
        )
    }

    /**
     * A recessed well — the container the protocol grid and the chart sit in.
     *
     * Recessed by *lighting*, not by colour: it is the same [AuroraPanelDrawable]
     * with the highlight and shadow swapped, so on the light palette it becomes a
     * pale sunk tray instead of the grey slab a naive `darken()` produced.
     */
    fun well(
        context: Context,
        palette: AppAppearance.Palette,
        radius: Int,
        accent: Int? = null,
        fill: Int = wellFill(palette),
    ): Drawable = AuroraPanelDrawable(
        density = density(context),
        fill = fill,
        radiusDp = radius.toFloat(),
        accent = accent,
        lit = false,
        pressed = true,
        glow = false,
    )

    /** A pill: caption chip, badge, status. */
    fun pill(
        context: Context,
        palette: AppAppearance.Palette,
        accent: Int,
        filled: Boolean = false,
        alpha: Float = 0.14f,
    ): Drawable = AuroraPanelDrawable(
        density = density(context),
        fill = if (filled) Sculpt.blend(palette.surface, accent, alpha) else wellFill(palette),
        radiusDp = RADIUS_PILL.toFloat(),
        accent = Sculpt.withAlpha(accent, if (filled) 0.55f else 0.30f),
        lit = filled,
        pressed = false,
        glow = false,
    )
}

/**
 * The Aurora surface painter.
 *
 * A single drawable that produces both the "raised glass" panel and its recessed
 * twin from one geometry, so a well and the card around it can never disagree about
 * their radius. [pressed] inverts the lighting exactly the way
 * [Sculpt.Lighting.pressedInnerShadow] describes: the specular disappears, the body
 * gradient darkens downwards and the inner shadow moves to the top edge.
 *
 * Everything here is derived from the palette's [Sculpt.Lighting], never from a
 * hard-coded alpha, because the light palette inverts the whole model (see
 * `AppAppearance.PORCELAIN`): a white card is lit by a drop shadow, not by a white
 * highlight, and a hard-coded white specular would put an invisible smudge across
 * every panel.
 */
class AuroraPanelDrawable(
    private val density: Float,
    private val fill: Int,
    private val radiusDp: Float,
    private val accent: Int? = null,
    private val lit: Boolean = false,
    private val pressed: Boolean = false,
    private val glow: Boolean = false,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val shadowRect = RectF()
    private val light = Sculpt.lighting

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        val drop = if (light.elevationDp > 0f && !pressed && glow) light.elevationDp * density else 0f
        val inset = density * 0.6f
        // Room for the drop shadow is reserved out of the drawable's own box, top
        // and bottom, so the fill never starts flush at the top and ends short at
        // the bottom (which makes every centred label sit visibly low in its card).
        val vertical = inset + drop * 0.35f
        rect.set(b.left + inset, b.top + vertical, b.right - inset, b.bottom - vertical)
        if (rect.width() <= 1f || rect.height() <= 1f) return
        val radius = (radiusDp * density).coerceAtMost(minOf(rect.width(), rect.height()) / 2f)

        // 0. outer drop shadow — the light palette's only depth cue.
        if (drop > 0f) {
            paint.style = Paint.Style.FILL
            paint.shader = null
            paint.color = Sculpt.withAlpha(Color.BLACK, light.elevationAlpha * 0.5f)
            shadowRect.set(rect.left, rect.top + drop * 0.5f, rect.right, rect.bottom + drop * 0.9f)
            canvas.drawRoundRect(shadowRect, radius, radius, paint)
        }

        // 1. body — a vertical gradient, tinted by the accent when lit.
        val top = if (lit && accent != null) {
            Sculpt.blend(fill, accent, 0.10f)
        } else {
            Sculpt.lighten(fill, light.topLift)
        }
        val body = if (pressed) {
            intArrayOf(
                Sculpt.blend(fill, Color.BLACK, light.bottomDrop * 1.8f),
                Sculpt.blend(fill, Color.BLACK, light.bottomDrop * 0.5f),
                fill,
            )
        } else {
            intArrayOf(top, fill, Sculpt.blend(fill, Color.BLACK, light.bottomDrop))
        }
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, rect.top, 0f, rect.bottom,
            body, floatArrayOf(0f, 0.58f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, radius, radius, paint)

        // 2. the aurora wash — a soft accent bloom inside the top of the panel.
        //
        // This is what separates the hero from every card under it. It is a radial
        // gradient anchored above the panel's top edge, so the light appears to come
        // from off-screen rather than from a lamp inside the card.
        if (accent != null && (lit || glow)) {
            paint.shader = RadialGradient(
                rect.centerX(), rect.top - rect.height() * 0.10f,
                maxOf(rect.width(), rect.height()) * 0.85f,
                intArrayOf(
                    Sculpt.withAlpha(accent, if (lit) 0.16f else 0.09f),
                    Sculpt.withAlpha(accent, 0f),
                ),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(rect, radius, radius, paint)
        }

        // 3. top specular — the convex-glass cue. Skipped on the light palette,
        // where white-on-white is invisible by definition.
        if (!pressed && light.specular > 0f) {
            paint.shader = RadialGradient(
                rect.left + rect.width() * 0.32f, rect.top,
                maxOf(rect.width(), rect.height()) * 0.95f,
                intArrayOf(
                    Sculpt.withAlpha(Color.WHITE, light.specular * 0.85f),
                    Sculpt.withAlpha(Color.WHITE, 0f),
                ),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(rect, radius, radius, paint)
        }

        // 4. inner shadow at the bottom (or the top when pressed).
        paint.shader = LinearGradient(
            0f, rect.top, 0f, rect.bottom,
            if (pressed) {
                intArrayOf(
                    Sculpt.withAlpha(light.dialInnerShadowColor, light.pressedInnerShadow),
                    Sculpt.withAlpha(light.dialInnerShadowColor, 0f),
                )
            } else {
                intArrayOf(
                    Sculpt.withAlpha(light.dialInnerShadowColor, 0f),
                    Sculpt.withAlpha(light.dialInnerShadowColor, light.innerShadow),
                )
            },
            if (pressed) floatArrayOf(0f, 0.42f) else floatArrayOf(0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null

        // 5. the frame. A lit accent at full strength, otherwise a hairline that
        // fades downwards so the card is anchored at its top edge.
        val frame = accent
        if (frame != null) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (lit) density * 1.25f else density * 1.1f
            paint.shader = LinearGradient(
                0f, rect.top, 0f, rect.bottom,
                intArrayOf(
                    Sculpt.withAlpha(frame, if (lit) 0.92f else 0.42f),
                    Sculpt.withAlpha(frame, if (lit) 0.34f else 0.12f),
                ),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(rect, radius, radius, paint)
            paint.shader = null
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = density
            paint.color = if (light.bevelColor == Color.WHITE) {
                Sculpt.withAlpha(Color.WHITE, light.bevel * 0.55f)
            } else {
                Sculpt.withAlpha(Color.BLACK, light.bevel * 0.9f)
            }
            canvas.drawRoundRect(rect, radius, radius, paint)
        }

        // 6. the bloom — only on the object that is allowed to be loud.
        if (glow && accent != null && Color.alpha(accent) > 40) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = density * 3.2f
            paint.color = Sculpt.withAlpha(accent, 0.16f)
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Drawable", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getPadding(padding: android.graphics.Rect): Boolean = false
}

/**
 * The ambient backdrop: the aurora itself.
 *
 * Replaces the palace photograph the dark theme used to fade in behind the console.
 * That image was 6 MB of APK and, at 14%, it read as texture rather than as light —
 * and it could not exist at all on the porcelain theme, so the two palettes opened
 * on two visually unrelated screens.
 *
 * This does the same job in about 120 lines and no assets: two wide, soft radial
 * washes that drift on a 24-second cycle, tinted by the connection state, over a
 * base gradient that darkens towards the bottom of the page. It is one
 * [View], painted behind everything, and it costs one full-screen software-layer
 * repaint every 16 ms only while it is animating — the drift is slow enough that it
 * is driven at 30 fps by the animator's own duration, not by a render loop.
 *
 * Because it is drawn rather than photographed it exists on *both* palettes: on
 * Porcelain the same washes land as a pale tint over the page, which is what makes
 * the light theme look like the same product with the lights on.
 */
class AuroraBackdrop(
    context: Context,
    private val palette: AppAppearance.Palette,
) : View(context) {

    /** The state accent. Repainted by the console on every state change. */
    private var accent: Int = palette.primary

    /** Secondary wash, kept one step away from [accent] so the two read as light. */
    private var accent2: Int = palette.neonViolet

    /** Connected state makes the whole wash brighter. */
    private var lit = false

    private var phase = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dark = Sculpt.lighting.elevationDp <= 0f

    private val drift = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 24_000L
        repeatCount = android.animation.ValueAnimator.INFINITE
        addUpdateListener {
            phase = it.animatedFraction
            invalidate()
        }
    }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setState(accentColor: Int, secondary: Int, bright: Boolean) {
        if (accent == accentColor && accent2 == secondary && lit == bright) return
        accent = accentColor
        accent2 = secondary
        lit = bright
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!drift.isStarted) drift.start()
    }

    override fun onDetachedFromWindow() {
        drift.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Base: the page colour, deepening towards the bottom. On the dark palette
        // the canvas is already near-black; on the light one the wash is subtle
        // enough to keep the page feeling like paper.
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            if (dark) {
                intArrayOf(
                    Sculpt.blend(palette.canvas, accent, 0.05f),
                    palette.canvas,
                    Sculpt.blend(palette.canvas, Color.BLACK, 0.35f),
                )
            } else {
                intArrayOf(
                    Sculpt.blend(palette.canvas, palette.surface, 0.8f),
                    palette.canvas,
                    Sculpt.blend(palette.canvas, accent, 0.05f),
                )
            },
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)

        // Two drifting washes. The radii are deliberately larger than the screen so
        // the edges of the blobs are never visible — a visible edge would read as a
        // circle drawn on the background rather than as light.
        val breath = 0.5f + 0.5f * kotlin.math.sin(phase * TWO_PI)
        val strength = if (lit) 1f else 0.55f
        blob(
            canvas,
            x = w * (0.18f + 0.10f * breath),
            y = h * (0.18f + 0.05f * (1f - breath)),
            radius = w * (1.15f + 0.12f * breath),
            color = accent,
            alpha = (if (dark) 0.16f else 0.11f) * strength,
        )
        blob(
            canvas,
            x = w * (0.88f - 0.10f * breath),
            y = h * (0.52f + 0.06f * breath),
            radius = w * 1.05f,
            color = accent2,
            alpha = (if (dark) 0.13f else 0.08f) * strength,
        )
    }

    private fun blob(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int, alpha: Float) {
        paint.shader = RadialGradient(
            x, y, radius,
            intArrayOf(
                Sculpt.withAlpha(color, alpha),
                Sculpt.withAlpha(color, alpha * 0.45f),
                Sculpt.withAlpha(color, 0f),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(x, y, radius, paint)
    }

    private companion object {
        const val TWO_PI = 6.2831855f
    }
}

/**
 * The home screen's icon set, drawn as vectors.
 *
 * The previous console used text glyphs — `⌂`, `▥`, `▤`, `⚙`, `☰`. They are
 * whatever the device's font happens to draw for those code points: on some ROMs
 * they are boxed, on others they are emoji-coloured, and none of them sit on the
 * same optical baseline. Twelve short [Path]s replace all of it, cost no asset, scale
 * with the view and take the palette's colour at draw time, so an icon can be
 * repainted for a state change without touching its geometry.
 */
enum class AuroraIcon {
    HOME,
    STATS,
    LOGS,
    SETTINGS,
    MENU,
    POWER,
    REFRESH,
    SHIELD,
    GLOBE,
    BOLT,
    LAYERS,
    LINK,
    GAUGE,
    ONION,
}

/** A vector icon tinted by the palette. [stroke] is in dp. */
class AuroraIconView(
    context: Context,
    private var icon: AuroraIcon,
    private var color: Int,
    private var strokeDp: Float = 1.7f,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /** Some glyphs read better as a filled shape (the shield's tick). */
    private var filled = false

    fun setIcon(next: AuroraIcon) {
        if (icon == next) return
        icon = next
        invalidate()
    }

    fun setColor(next: Int) {
        if (color == next) return
        color = next
        invalidate()
    }

    fun setStrokeDp(next: Float) {
        strokeDp = next
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val s = minOf(w, h)
        val cx = w / 2f
        val cy = h / 2f
        paint.color = color
        paint.strokeWidth = strokeDp * density * (s / (24f * density)).coerceIn(0.75f, 1.6f)
        paint.style = Paint.Style.STROKE
        // Every path below is authored on a 24x24 grid and scaled to the view, so
        // one geometry serves a 16dp dock glyph and a 26dp hero glyph.
        val k = s / 24f
        fun x(v: Float) = cx + (v - 12f) * k
        fun y(v: Float) = cy + (v - 12f) * k

        when (icon) {
            AuroraIcon.HOME -> {
                val path = Path().apply {
                    moveTo(x(4f), y(11.4f))
                    lineTo(x(12f), y(4.6f))
                    lineTo(x(20f), y(11.4f))
                    lineTo(x(20f), y(19f))
                    lineTo(x(14.6f), y(19f))
                    lineTo(x(14.6f), y(14f))
                    lineTo(x(9.4f), y(14f))
                    lineTo(x(9.4f), y(19f))
                    lineTo(x(4f), y(19f))
                    close()
                }
                canvas.drawPath(path, paint)
            }
            AuroraIcon.STATS -> {
                canvas.drawLine(x(5f), y(19f), x(5f), y(12f), paint)
                canvas.drawLine(x(12f), y(19f), x(12f), y(6f), paint)
                canvas.drawLine(x(19f), y(19f), x(19f), y(14.5f), paint)
                canvas.drawLine(x(3.6f), y(20.4f), x(20.4f), y(20.4f), paint)
            }
            AuroraIcon.LOGS -> {
                canvas.drawLine(x(5f), y(7f), x(19f), y(7f), paint)
                canvas.drawLine(x(5f), y(12f), x(19f), y(12f), paint)
                canvas.drawLine(x(5f), y(17f), x(14f), y(17f), paint)
            }
            AuroraIcon.SETTINGS -> {
                val ring = Path().apply {
                    addCircle(cx, cy, 3.1f * k, Path.Direction.CW)
                }
                canvas.drawPath(ring, paint)
                for (index in 0 until 8) {
                    val a = (index * 45f) * (Math.PI / 180f)
                    val cosA = kotlin.math.cos(a).toFloat()
                    val sinA = kotlin.math.sin(a).toFloat()
                    canvas.drawLine(
                        cx + cosA * 5.4f * k, cy + sinA * 5.4f * k,
                        cx + cosA * 7.6f * k, cy + sinA * 7.6f * k,
                        paint,
                    )
                }
            }
            AuroraIcon.MENU -> {
                canvas.drawLine(x(5f), y(8f), x(19f), y(8f), paint)
                canvas.drawLine(x(5f), y(12.5f), x(16f), y(12.5f), paint)
                canvas.drawLine(x(5f), y(17f), x(19f), y(17f), paint)
            }
            AuroraIcon.POWER -> {
                val arc = RectF(x(4.6f), y(4.9f), x(19.4f), y(19.7f))
                canvas.drawArc(arc, 118f, 304f, false, paint)
                canvas.drawLine(cx, y(4.2f), cx, y(10.6f), paint)
            }
            AuroraIcon.REFRESH -> {
                val arc = RectF(x(4.8f), y(4.8f), x(19.2f), y(19.2f))
                canvas.drawArc(arc, 40f, 280f, false, paint)
                val head = Path().apply {
                    moveTo(x(15.2f), y(2.9f))
                    lineTo(x(19.6f), y(5.4f))
                    lineTo(x(15.9f), y(8.4f))
                }
                canvas.drawPath(head, paint)
            }
            AuroraIcon.SHIELD -> {
                val path = Path().apply {
                    moveTo(x(12f), y(3.6f))
                    lineTo(x(19.4f), y(6.6f))
                    lineTo(x(19.4f), y(12.4f))
                    cubicTo(x(19.4f), y(17.2f), x(16.2f), y(20f), x(12f), y(21.2f))
                    cubicTo(x(7.8f), y(20f), x(4.6f), y(17.2f), x(4.6f), y(12.4f))
                    lineTo(x(4.6f), y(6.6f))
                    close()
                }
                canvas.drawPath(path, paint)
                if (filled) {
                    val tick = Path().apply {
                        moveTo(x(8.6f), y(12.2f))
                        lineTo(x(11.2f), y(14.8f))
                        lineTo(x(15.6f), y(9.6f))
                    }
                    canvas.drawPath(tick, paint)
                }
            }
            AuroraIcon.GLOBE -> {
                canvas.drawCircle(cx, cy, 8f * k, paint)
                canvas.drawLine(x(4f), cy, x(20f), cy, paint)
                val meridian = RectF(cx - 3.6f * k, y(4f), cx + 3.6f * k, y(20f))
                canvas.drawOval(meridian, paint)
            }
            AuroraIcon.BOLT -> {
                val path = Path().apply {
                    moveTo(x(13.2f), y(3.2f))
                    lineTo(x(6.2f), y(13.4f))
                    lineTo(x(11.4f), y(13.4f))
                    lineTo(x(10.8f), y(20.8f))
                    lineTo(x(17.8f), y(10.6f))
                    lineTo(x(12.6f), y(10.6f))
                    close()
                }
                canvas.drawPath(path, paint)
            }
            AuroraIcon.LAYERS -> {
                val top = Path().apply {
                    moveTo(x(12f), y(3.8f))
                    lineTo(x(20.4f), y(8.2f))
                    lineTo(x(12f), y(12.6f))
                    lineTo(x(3.6f), y(8.2f))
                    close()
                }
                canvas.drawPath(top, paint)
                canvas.drawLine(x(3.6f), y(12.6f), x(12f), y(17f), paint)
                canvas.drawLine(x(20.4f), y(12.6f), x(12f), y(17f), paint)
                canvas.drawLine(x(3.6f), y(16.4f), x(12f), y(20.8f), paint)
                canvas.drawLine(x(20.4f), y(16.4f), x(12f), y(20.8f), paint)
            }
            AuroraIcon.LINK -> {
                canvas.drawCircle(x(9f), y(9f), 3.9f * k, paint)
                canvas.drawCircle(x(15f), y(15f), 3.9f * k, paint)
                canvas.drawLine(cx - 1.4f * k, cy + 1.4f * k, cx + 1.4f * k, cy - 1.4f * k, paint)
            }
            AuroraIcon.GAUGE -> {
                val arc = RectF(x(3.8f), y(6.4f), x(20.2f), y(22.8f))
                canvas.drawArc(arc, 190f, 160f, false, paint)
                canvas.drawLine(cx, y(13.4f), x(15.6f), y(9.4f), paint)
                canvas.drawCircle(cx, y(13.4f), 1.5f * k, paint)
            }
            AuroraIcon.ONION -> {
                canvas.drawCircle(cx, cy, 8.4f * k, paint)
                canvas.drawCircle(cx, cy, 5.4f * k, paint)
                canvas.drawCircle(cx, cy, 2.4f * k, paint)
            }
        }
    }

    /**
     * Filled variant flag. Only the shield currently uses it — a shield with a tick
     * is the app's "the tunnel is up" mark, and an unfilled shield with a tick inside
     * is unreadable at 20dp.
     */
    fun setFilled(next: Boolean) {
        if (filled == next) return
        filled = next
        invalidate()
    }
}

private const val TWO_PI = 6.2831855f
