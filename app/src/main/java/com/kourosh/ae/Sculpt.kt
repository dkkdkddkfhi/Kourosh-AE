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
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Shared drawing helpers for the Orbit visual language.
 *
 * A "sculpted" control is not one colour — it is four layers, exactly as the
 * approved mock described them:
 *   1. a specular highlight near the top-left (convex glass),
 *   2. a body gradient,
 *   3. a one-pixel light line along the top edge,
 *   4. an inner shadow at the bottom that creates depth.
 * Pressing inverts 3 and 4 so the surface genuinely sinks instead of only
 * shrinking.
 *
 * The old implementation faked this with a plain [GradientDrawable], which can
 * only do a linear body gradient — no radial specular, no inner shadow. That is
 * why the shipped buttons looked flat next to the HTML preview. [GlassDrawable]
 * below draws all four layers, and every surface in the app goes through it.
 */
object Sculpt {

    /**
     * How a raised surface is lit.
     *
     * The dark palette builds depth with a white specular highlight on the top
     * edge and a black inner shadow at the bottom. On a white card both of those
     * disappear — white-on-white is invisible, and a black inner shadow on white
     * reads as dirt rather than depth.
     *
     * So the light palette inverts the model instead of recolouring it:
     *   - the body gradient runs the other way (the surface is brightest where
     *     the light hits it, which on a light page is the top, but the *step* is
     *     much smaller because there is less headroom above white),
     *   - the specular highlight is dropped ([specular] = 0),
     *   - the inner bottom shadow is nearly dropped, and depth comes from a real
     *     outer drop shadow ([elevationDp]) instead,
     *   - the bevel line along the top edge becomes a darker hairline at the
     *     bottom, because on light surfaces the *shadowed* edge is what the eye
     *     reads as an edge.
     *
     * Held as a single mutable field set once by [AppAppearance.load], because
     * the 26 call sites of [sculptedBackground] do not have a palette in scope
     * and should not each have to be told which theme is active.
     */
    data class Lighting(
        /** Body gradient: how much lighter the top is than the fill. */
        val topLift: Float,
        /** Body gradient: how much darker the bottom is than the fill. */
        val bottomDrop: Float,
        /** Radial white specular at the top-left. 0 disables it. */
        val specular: Float,
        /** Inner shadow at the bottom of a raised surface. */
        val innerShadow: Float,
        /** Inner shadow at the top of a pressed surface. */
        val pressedInnerShadow: Float,
        /** Bevel line alpha on a raised surface. */
        val bevel: Float,
        /** Bevel line alpha on a pressed surface. */
        val pressedBevel: Float,
        /** Colour of the bevel line — white on dark, black on light. */
        val bevelColor: Int,
        /** Outer drop shadow radius in dp. 0 disables it (dark palette). */
        val elevationDp: Float,
        /** Outer drop shadow opacity. */
        val elevationAlpha: Float,
        /** Default hairline when a caller passes neither accent nor stroke. */
        val defaultOutline: Int,
        /** Fallback fill tint for recessed wells. */
        val recessOutline: Int,
        // --- the dial only ---------------------------------------------------
        // The dial is a hand-drawn glass disc rather than a rounded rect, so it
        // has its own set of alphas. They are here and not in the view because
        // the whole point of Lighting is that one object decides how depth is
        // faked, and a second set of literals inside the dial is how a theme
        // drifts out of sync.
        /** Drop shadow opacity under the idle dial. */
        val dialShadowAlpha: Float,
        /** Radial specular on the glass disc. */
        val dialSpecular: Float,
        /** Travelling sheen band, connected state only. */
        val dialSheen: Float,
        /** Inner shadow at the bottom of the disc. */
        val dialInnerShadow: Float,
        /** Colour of that inner shadow. */
        val dialInnerShadowColor: Int,
        /** Bevel edge alpha at the top of the disc. */
        val dialEdgeStrong: Float,
        /** Bevel edge alpha at the bottom of the disc. */
        val dialEdgeSoft: Float,
        /** Multiplier applied to every [Sculpt.recess] depth. */
        val recessScale: Float,
        /** Dial body gradient: lift at the top. */
        val dialBodyLift: Float,
        /** Dial body gradient: drop at the bottom. */
        val dialBodyDrop: Float,
        /**
         * How the timer digits are shifted away from the accent so they read on
         * the glass. Positive lightens (dark palette: a pale mint glowing on
         * black), negative darkens (light palette: a deep teal on white). This is
         * the largest text in the app, so it does not get to be approximate.
         */
        val dialTextShift: Float,
    )

    val DARK_LIGHTING = Lighting(
        topLift = 0.09f,
        bottomDrop = 0.09f,
        specular = 0.13f,
        innerShadow = 0.30f,
        pressedInnerShadow = 0.45f,
        bevel = 0.22f,
        pressedBevel = 0.05f,
        bevelColor = Color.WHITE,
        elevationDp = 0f,
        elevationAlpha = 0f,
        defaultOutline = Color.argb(28, 255, 255, 255),
        recessOutline = Color.argb(20, 255, 255, 255),
        dialShadowAlpha = 0.65f,
        dialSpecular = 0.16f,
        dialSheen = 0.085f,
        dialInnerShadow = 0.30f,
        dialInnerShadowColor = Color.BLACK,
        dialEdgeStrong = 0.24f,
        dialEdgeSoft = 0.05f,
        recessScale = 1f,
        dialBodyLift = 0.11f,
        dialBodyDrop = 0.16f,
        dialTextShift = 0.55f,
    )

    /**
     * Light-palette lighting. Numbers, not vibes:
     * a white card can only go 0% brighter, so [topLift] is tiny and the visible
     * separation is carried by [elevationDp] — an 8dp shadow at 22% under a
     * white card on a `#EEF1F4` page, which is the same figure the HTML preview
     * used (`0 6px 16px -8px rgba(17,26,31,.20)`).
     */
    val LIGHT_LIGHTING = Lighting(
        topLift = 0.02f,
        bottomDrop = 0.05f,
        specular = 0f,
        innerShadow = 0.05f,
        pressedInnerShadow = 0.14f,
        bevel = 0.05f,
        pressedBevel = 0.10f,
        bevelColor = Color.BLACK,
        // 6dp, not the preview's 8: the shadow has to be reserved out of the
        // view's own box (see GlassDrawable.draw), and every dp of blur costs two
        // dp of card height. 6dp at 24% is the same visual weight as 8dp at 20%
        // and gives the layout 4dp back.
        elevationDp = 6f,
        elevationAlpha = 0.24f,
        defaultOutline = Color.argb(28, 17, 26, 31),
        recessOutline = Color.argb(22, 17, 26, 31),
        // A light dial cannot be lit by adding white — the disc is already near
        // white. Depth comes from a soft outer shadow (0.18, well below the dark
        // model's 0.65 so it reads as paper, not soot) and a black bevel that is
        // strong at the BOTTOM. The travelling sheen is kept but halved: on a
        // light disc it is a subtle wipe rather than a glint.
        dialShadowAlpha = 0.18f,
        dialSpecular = 0.05f,
        dialSheen = 0.04f,
        dialInnerShadow = 0.07f,
        dialInnerShadowColor = Color.BLACK,
        dialEdgeStrong = 0.06f,
        dialEdgeSoft = 0.14f,
        // 0.22: the deepest call site asks for 0.30, which on white becomes
        // darken(0.066) = #EEEEEE — the canvas grey. So the deepest well on the
        // light palette is exactly "as dark as the page", and the shallower ones
        // land between that and white. Nothing sinks below the page, which is
        // what stops a light theme looking like it has holes in it.
        recessScale = 0.22f,
        dialBodyLift = 0.03f,
        dialBodyDrop = 0.06f,
        // darken(mint #0E9C82, 0.30) = #0A6D5B: 6.9:1 on the white dial face.
        dialTextShift = -0.30f,
    )

    /**
     * The active lighting model. Written once per Activity by
     * [AppAppearance.load]; read on every draw.
     *
     * `@Volatile` because views draw on the main thread but the palette is
     * loaded in `onCreate` — the write must be visible without a fence
     * assumption, and a stale read would draw dark-model highlights on a white
     * card for one frame.
     */
    @Volatile
    var lighting: Lighting = DARK_LIGHTING

    /** Alpha-blend [overlay] onto [base]. Used to fake translucency on opaque views. */
    fun blend(base: Int, overlay: Int, alpha: Float): Int {
        val a = alpha.coerceIn(0f, 1f)
        val r = ((Color.red(base) * (1 - a)) + (Color.red(overlay) * a)).roundToInt()
        val g = ((Color.green(base) * (1 - a)) + (Color.green(overlay) * a)).roundToInt()
        val b = ((Color.blue(base) * (1 - a)) + (Color.blue(overlay) * a)).roundToInt()
        return Color.rgb(r, g, b)
    }

    fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))

    /** Lift a colour towards white — the highlight edge of a bevel. */
    fun lighten(color: Int, amount: Float): Int = blend(color, Color.WHITE, amount)

    /** Push a colour towards black — the shadow edge of a bevel. */
    fun darken(color: Int, amount: Float): Int = blend(color, Color.BLACK, amount)

    /**
     * A surface that should read as sunk *below* [base] — the transport rail's
     * well, the recessed halves of the cards, a pressed cell.
     *
     * Why this is not just `darken`: the call sites were written against the dark
     * palette, where `surface` is `#0B1116` and darkening it 30% lands on a near
     * black well that reads as depth. Applying the same 30% to a white card gives
     * `#B3B3B3` — a mid grey slab in the middle of a white page, which reads as a
     * disabled area rather than a well. On a light palette the same *perceptual*
     * step needs a much smaller number, so [Lighting.recessScale] carries it and
     * each call site keeps stating its intent ("sink this a lot" / "a little").
     */
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
     * The standard Kourosh-AE raised glass surface. [radius] is in dp.
     *
     * [accent] is a lit outline used for active states and wins over [stroke].
     * [pressed] forces the recessed lighting for callers that manage their own
     * state; everyone else gets a state list, so any clickable view using this
     * background genuinely sinks on touch instead of only scaling.
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
        fun layer(down: Boolean) = GlassDrawable(
            density = density,
            fill = fill,
            radiusDp = radius.toFloat(),
            stroke = outline,
            strokeWidthDp = strokeWidth * 1.1f,
            pressed = down,
            glow = accent,
        )
        if (pressed) return layer(true)
        // StateListDrawable, not a bare GlassDrawable: this is what gives every
        // button in the app the "press = sink inwards" behaviour for free. Views
        // that are not clickable simply never enter state_pressed and always
        // render the raised layer.
        return StateListDrawable().apply {
            setEnterFadeDuration(0)
            setExitFadeDuration(140)
            addState(intArrayOf(android.R.attr.state_pressed), layer(true))
            addState(intArrayOf(), layer(false))
        }
    }

    /** A recessed well: the inverse lighting, used for the transport rail track. */
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

    /**
     * Wrap a sculpted surface in a ripple so touch feedback survives.
     *
     * selectableItemBackground draws nothing over a custom drawable on some OEM
     * skins, so the ripple is explicit and always has a mask.
     */
    fun sculptedRipple(
        density: Float,
        fill: Int,
        radius: Int,
        rippleColor: Int,
        accent: Int? = null,
    ): RippleDrawable {
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius * density
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
 * The four-layer glass surface from the approved mock, drawn by hand.
 *
 * Layer order matches CSS paint order in the preview:
 *   body gradient → radial specular → inner bottom shadow → bevel stroke.
 * [pressed] swaps the vertical lighting and moves the inner shadow to the top,
 * which is what makes a press read as "sunk in" rather than "faded".
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
    private val shadowRect = RectF()
    private val clip = Path()
    private val light = Sculpt.lighting

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        val strokeWidth = (strokeWidthDp * density).coerceAtLeast(1f)
        val inset = strokeWidth / 2f
        // An outer drop shadow needs room to fall into, or it is clipped by the
        // view's own bounds. Reserve it from the drawable's box rather than
        // asking every call site for padding.
        val drop = if (light.elevationDp > 0f && !pressed) light.elevationDp * density else 0f
        // The reserve is symmetric, top and bottom, even though the shadow only
        // falls downwards. An asymmetric reserve was the first attempt and it
        // looked wrong for a reason that is obvious in hindsight: the fill ended
        // 4dp above the view's bottom edge while starting flush at the top, so
        // every row's text — centred by the view's own padding — sat visibly low
        // inside its own card. Losing a couple of dp at the top costs nothing.
        val vertical = inset + drop * 0.35f
        rect.set(
            b.left + inset,
            b.top + vertical,
            b.right - inset,
            b.bottom - vertical,
        )
        // A pill radius (999dp in the mock) has to clamp to half the height or
        // drawRoundRect produces a lens shape on short views.
        val radius = (radiusDp * density).coerceAtMost(minOf(rect.width(), rect.height()) / 2f)

        // 0. outer drop shadow — the light palette's ONLY depth cue.
        //
        // Drawn first and underneath everything, offset downwards. The shadow is
        // what separates a white card from a near-white page; without it the
        // light theme is flat rectangles on flat background. Skipped entirely on
        // the dark palette (elevationDp = 0), where the bevel does this job.
        //
        // Two implementations, because Paint.setShadowLayer() for anything other
        // than text is only supported by the hardware-accelerated pipeline from
        // API 28, and minSdk here is 26. On 26/27 it is silently ignored — no
        // crash, just no shadow — which would ship a flat light theme to
        // Android 8.x. So below 28 the shadow is stacked by hand from a few
        // expanding round rects, which every API level can draw.
        if (drop > 0f) {
            paint.style = Paint.Style.FILL
            paint.shader = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                paint.color = fill
                paint.setShadowLayer(
                    drop,
                    0f,
                    drop * 0.45f,
                    Sculpt.withAlpha(Color.BLACK, light.elevationAlpha),
                )
                canvas.drawRoundRect(rect, radius, radius, paint)
                paint.clearShadowLayer()
            } else {
                // Three rings, widest and faintest first, each offset down by a
                // fraction of the blur radius. Alpha is divided across the rings
                // so the stack lands near elevationAlpha rather than tripling it.
                val rings = 3
                for (i in rings downTo 1) {
                    val spread = drop * (i / rings.toFloat())
                    paint.color = Sculpt.withAlpha(
                        Color.BLACK,
                        light.elevationAlpha * 0.45f / i,
                    )
                    shadowRect.set(
                        rect.left - spread * 0.35f,
                        rect.top - spread * 0.10f,
                        rect.right + spread * 0.35f,
                        rect.bottom + spread * 0.75f,
                    )
                    canvas.drawRoundRect(shadowRect, radius + spread * 0.3f, radius + spread * 0.3f, paint)
                }
            }
        }

        // 1. body gradient
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, rect.top, 0f, rect.bottom,
            if (pressed) {
                intArrayOf(
                    Sculpt.darken(fill, light.bottomDrop * 2.4f),
                    Sculpt.darken(fill, light.bottomDrop * 0.7f),
                    fill,
                )
            } else {
                intArrayOf(
                    Sculpt.lighten(fill, light.topLift),
                    fill,
                    Sculpt.darken(fill, light.bottomDrop),
                )
            },
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, radius, radius, paint)

        // 2. specular highlight, top-left — the "convex glass" cue.
        // Disabled on light palettes: a white highlight on a white card is
        // invisible, and turning it dark would read as a smudge, not a highlight.
        if (!pressed && light.specular > 0f) {
            paint.shader = RadialGradient(
                rect.left + rect.width() * 0.30f,
                rect.top,
                maxOf(rect.width(), rect.height()) * 0.95f,
                intArrayOf(
                    Sculpt.withAlpha(Color.WHITE, light.specular),
                    Sculpt.withAlpha(Color.WHITE, 0f),
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(rect, radius, radius, paint)
        }

        // 3. inner shadow — bottom when raised, top when pressed
        val shadowStops = if (pressed) {
            intArrayOf(
                Sculpt.withAlpha(Color.BLACK, light.pressedInnerShadow),
                Sculpt.withAlpha(Color.BLACK, 0f),
            )
        } else {
            intArrayOf(
                Sculpt.withAlpha(Color.BLACK, 0f),
                Sculpt.withAlpha(Color.BLACK, light.innerShadow),
            )
        }
        paint.shader = LinearGradient(
            0f, rect.top, 0f, rect.bottom,
            shadowStops,
            if (pressed) floatArrayOf(0f, 0.45f) else floatArrayOf(0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null

        // 4. bevel: a light line on the top edge (dark palette), or a darker
        // hairline fading downwards (light palette). Same geometry, opposite
        // colour: on a light surface it is the shadowed edge that reads as an
        // edge, so bevelColor is black there and the gradient is inverted.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        val bevelAlpha = if (pressed) light.pressedBevel else light.bevel
        paint.shader = LinearGradient(
            0f, rect.top, 0f, rect.bottom,
            if (light.bevelColor == Color.WHITE) {
                intArrayOf(
                    Sculpt.withAlpha(light.bevelColor, bevelAlpha),
                    Sculpt.withAlpha(light.bevelColor, bevelAlpha * 0.18f),
                )
            } else {
                intArrayOf(
                    Sculpt.withAlpha(light.bevelColor, bevelAlpha * 0.18f),
                    Sculpt.withAlpha(light.bevelColor, bevelAlpha),
                )
            },
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null

        // outline / lit accent ring
        paint.color = stroke
        canvas.drawRoundRect(rect, radius, radius, paint)

        // a lit control also gets a soft outer bloom, like the mock's box-shadow
        glow?.let { color ->
            if (Color.alpha(color) < 40) return@let
            clip.reset()
            paint.color = Sculpt.withAlpha(color, 0.22f)
            paint.strokeWidth = strokeWidth * 2.4f
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Drawable", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getPadding(padding: Rect): Boolean = false
}
