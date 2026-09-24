package com.kourosh.ae

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * The cards under the hero: the exit node and the three slot occupants.
 *
 * They share one shape — a tinted glyph tile, a two-line column, a badge pill — and
 * one rule: **the accent is spent on the glyph and the badge, never on the body of the
 * card**. In the previous design every card was lit by a border that was either
 * always on (the chain/split/MIM slot) or keyed to the connection state (the exit
 * node), so a row of three cards had three different lighting rules and none of them
 * meant anything. Here a card is quiet until it is *armed*, and when it is armed the
 * whole card takes its accent at once — glyph, frame and badge together.
 */

/**
 * The exit node: where the tunnel is actually leaving from.
 *
 * Deliberately the plainest card on the screen. It reports one fact the core
 * measured, so it has no state to dramatise; the only accent on it is the flag tile
 * and the live trace, and both take the mint of "traffic is flowing through here".
 */
class AuroraExitCard(
    context: Context,
    private val palette: AppAppearance.Palette,
    onClick: () -> Unit,
) : LinearLayout(context) {

    private val flagView: TextView
    private val keyView: TextView
    private val ipView: TextView
    private val locView: TextView
    private val trace: AuroraMiniTrace

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = Aurora.ripplePanel(
            context, palette, Aurora.RADIUS_CARD,
            accent = palette.primary,
            fill = Aurora.cardFill(palette),
        )
        setPadding(px(12), px(11), px(14), px(11))
        isClickable = true
        isFocusable = true
        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onClick()
        }

        // The flag sits in its own tinted tile: on a device with no emoji font the slot
        // is still a visible, deliberate shape rather than a hole in the layout.
        flagView = Aurora.label(context, "\uD83C\uDF10", 18f, palette.ink, gravity = Gravity.CENTER)
        flagView.background = Aurora.panel(
            context, palette, 14,
            accent = Sculpt.withAlpha(palette.primary, 0.35f),
            lit = false,
            fill = Sculpt.blend(palette.surface, palette.primary, 0.07f),
        )
        addView(flagView, LayoutParams(px(42), px(42)))

        val column = LinearLayout(context).apply { orientation = VERTICAL }
        keyView = Aurora.label(
            context, Strings.t("EXIT NODE"), Aurora.CAPTION_SP,
            Sculpt.withAlpha(palette.faint, 0.95f),
            face = Aurora.Face.MEDIUM, tracking = 0.14f,
        )
        ipView = Aurora.label(
            context, Strings.t("not tunnelled"), Aurora.VALUE_SP, palette.ink,
            face = Aurora.Face.MONO,
        ).apply {
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
        }
        locView = Aurora.label(
            context, Strings.t("tap to refresh"), 10.5f,
            Sculpt.withAlpha(palette.faint, 0.9f),
        )
        column.addView(keyView)
        column.addView(
            ipView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = px(3) },
        )
        column.addView(
            locView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = px(1) },
        )
        addView(column, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = px(11)
        })

        trace = AuroraMiniTrace(context, palette, palette.mint)
        addView(trace, LayoutParams(px(50), px(26)))
    }

    /**
     * @param address raw address from the trace endpoint; IPv4 or IPv6
     * @param countryCode two-letter code, or null when unknown
     * @param tunnelled whether the tunnel is up, which changes the caption
     * @param measuring the exit is being read from inside a live tunnel
     * @param note caption to show while measuring, when the caller has a better one
     */
    fun render(
        address: String,
        countryCode: String?,
        tunnelled: Boolean,
        measuring: Boolean = false,
        note: String? = null,
    ) {
        keyView.text = Strings.t(if (tunnelled) "EXIT NODE" else "YOUR IP")
        if (address.isBlank() || address == UNAVAILABLE) {
            if (measuring) {
                // The native path reports the exit from inside the tunnel a second or
                // two after connect. Offering "retry" there would invite the user to
                // retry something that is simply not finished yet.
                ipView.text = Strings.t(MEASURING)
                ipView.textSize = 13f * FontChoice.scale(context)
                locView.text = note ?: Strings.t("reading from inside the tunnel")
                flagView.text = "\uD83C\uDF10"
                contentDescription = note ?: "در حال خواندن آدرس خروج از درون تونل"
                return
            }
            ipView.text = Strings.t(UNAVAILABLE)
            ipView.textSize = Aurora.VALUE_SP * FontChoice.scale(context)
            locView.text = Strings.t("tap to retry")
            flagView.text = "\uD83C\uDF10"
            contentDescription = "آی‌پی در دسترس نیست، برای تلاش مجدد بزنید"
            return
        }
        val fit = IpFormatter.fit(address)
        ipView.text = fit.text
        ipView.textSize = when (fit.step) {
            IpFormatter.Step.V4 -> Aurora.VALUE_SP * FontChoice.scale(context)
            IpFormatter.Step.V6 -> 12.5f * FontChoice.scale(context)
            IpFormatter.Step.V6_LONG -> 11f * FontChoice.scale(context)
        }
        flagView.text = IpFormatter.flag(countryCode)
        val country = countryCode?.trim()?.uppercase().orEmpty()
        locView.text = when {
            country.isNotEmpty() && tunnelled -> Strings.tf("%s · tunnelled", country)
            country.isNotEmpty() -> country
            tunnelled -> Strings.t("tunnelled")
            else -> Strings.t("not tunnelled")
        }
        contentDescription = "${keyView.text}: ${fit.full}${if (country.isNotEmpty()) "، $country" else ""}"
    }

    fun pushSample(value: Float) = trace.push(value)

    fun resetSpark() = trace.reset()

    private fun px(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private companion object {
        const val UNAVAILABLE = "IP unavailable"
        const val MEASURING = "measuring…"
    }
}

/**
 * Shared body of the three slot cards (chain, masque-over-masque, smart split).
 *
 * All three are the same object with different words: a glyph, a title, a one-line
 * explanation and an ON/OFF badge. They share one renderer so their lighting can
 * never drift apart — which is exactly what happened in the previous design, where
 * each card had grown its own idea of what "lit" meant.
 */
abstract class AuroraSlotCard(
    context: Context,
    protected val palette: AppAppearance.Palette,
    title: String,
    icon: AuroraIcon,
    protected val accent: Int,
    protected val accentText: Int,
) : LinearLayout(context) {

    protected val titleView: TextView
    protected val subtitleView: TextView
    protected val badgeView: TextView
    protected val glyph: AuroraIconView
    private val glyphTile: LinearLayout

    /** Why the card cannot be used, shown in place of the normal subtitle. */
    protected var unavailableReason: String? = null

    /**
     * Whether the feature applies to the selected transport at all.
     *
     * Kept separate from [unavailableReason] because "cannot be changed right now"
     * and "does not apply here" are different facts: a live chained tunnel is locked
     * but very much armed, and the badge has to keep saying so.
     */
    protected var applicable = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        // Vertical padding only pads within the fixed height the console gives this
        // row; it must not add height.
        setPadding(px(11), px(6), px(13), px(6))
        isClickable = true
        isFocusable = true

        glyphTile = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            isDuplicateParentStateEnabled = true
        }
        glyph = AuroraIconView(context, icon, accentText, strokeDp = 1.7f)
        glyphTile.addView(glyph, LayoutParams(px(19), px(19)))
        addView(glyphTile, LayoutParams(px(36), px(36)))

        val column = LinearLayout(context).apply { orientation = VERTICAL }
        titleView = Aurora.label(
            context, title, 11f, palette.muted,
            face = Aurora.Face.MEDIUM, tracking = 0.10f,
        ).apply { setSingleLine(true) }
        subtitleView = Aurora.label(context, "", 9.5f, palette.muted).apply {
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
        }
        column.addView(titleView)
        column.addView(
            subtitleView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = if (AppLanguage.current() == "en") px(1) else px(3) },
        )
        addView(column, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = px(11)
            if (AppLanguage.current() != "en") rightMargin = px(12)
        })

        badgeView = Aurora.label(
            context, "", Aurora.CAPTION_SP, palette.faint,
            face = Aurora.Face.MEDIUM, tracking = 0.12f, gravity = Gravity.CENTER,
        ).apply {
            setPadding(
                px(if (AppLanguage.current() == "en") 9 else 10),
                px(if (AppLanguage.current() == "en") 4 else 6),
                px(if (AppLanguage.current() == "en") 9 else 10),
                px(if (AppLanguage.current() == "en") 4 else 6),
            )
        }
        addView(badgeView, LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
    }

    /**
     * Paints the whole card from one decision: is it armed and does it apply here?
     *
     * [lit] drives the fill, the frame, the glyph tile and the badge together. The
     * frame is never "always on" — the previous design lit every card's border
     * unconditionally to keep the row from looking unbounded, which cost the only
     * signal the border had.
     */
    protected fun paint(lit: Boolean) {
        val fill = if (lit) {
            Sculpt.blend(palette.surface, accent, 0.13f)
        } else {
            Aurora.cardFill(palette)
        }
        background = Sculpt.sculptedRipple(
            resources.displayMetrics.density, fill, Aurora.RADIUS_CARD, accent,
            accent = Sculpt.withAlpha(accent, if (lit) 0.70f else 0.30f),
        )
        titleView.setTextColor(if (lit) palette.ink else palette.muted)
        glyphTile.background = if (lit) {
            Aurora.panel(
                context, palette, 12,
                accent = Sculpt.withAlpha(accent, 0.5f),
                lit = true,
                fill = Sculpt.blend(palette.surface, accent, 0.14f),
            )
        } else {
            Aurora.panel(
                context, palette, 12,
                accent = Sculpt.withAlpha(palette.ink, 0.10f),
                lit = false,
                fill = Aurora.wellFill(palette),
            )
        }
        glyph.setColor(if (lit) Sculpt.onGlass(accent) else Sculpt.withAlpha(palette.faint, 0.95f))
        badgeView.setTextColor(if (lit) accentText else palette.faint)
        badgeView.background = Aurora.pill(
            context, palette, if (lit) accent else palette.faint,
            filled = lit,
            alpha = 0.14f,
        )
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        // A disabled row is unavailable, not invisible: 0.5 rather than the old 0.45,
        // and the badge keeps carrying the reason.
        alpha = if (enabled) 1f else 0.55f
    }

    protected fun px(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}

/**
 * `PSIPHON OVER WARP` / `TOR OVER WARP` — one tunnel inside another.
 *
 * Unchanged semantics from the previous build, including the distinction that
 * matters most: [setUnavailable] with `applicable = true` means the chain is live but
 * locked, so the card keeps reporting CHAINED instead of claiming it does not apply.
 */
class AuroraChainCard(
    context: Context,
    palette: AppAppearance.Palette,
    private val onToggle: (Boolean) -> Unit,
) : AuroraSlotCard(
    context, palette,
    title = Strings.t("PSIPHON OVER WARP"),
    icon = AuroraIcon.LINK,
    accent = palette.violet,
    accentText = palette.violetText,
) {

    private var armed = false

    /** The transport being wrapped, for every string this card shows. */
    private var innerName: String = "PSIPHON"

    /** How the outer transport is chosen, in words. Set from settings. */
    private var outerSummary: String = "auto transport"

    init {
        setOnClickListener {
            // isEnabled=false blocks a touch click, but a focus-based activation from
            // a keyboard or remote still arrives, and flipping state there would leave
            // the card and the config disagreeing.
            if (!isEnabled) return@setOnClickListener
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            setArmed(!armed)
            onToggle(armed)
        }
        setArmed(false)
    }

    /** Marks the card unavailable and says why. */
    fun setUnavailable(reason: String?, applicable: Boolean = reason == null) {
        unavailableReason = reason
        this.applicable = applicable
        isEnabled = reason == null
        setArmed(armed)
    }

    /** Names the transport this card wraps, e.g. "Psiphon" or "Tor". */
    fun setInner(name: String) {
        innerName = name
        titleView.text = Strings.tf("%s OVER WARP", innerName.uppercase())
        setArmed(armed)
    }

    /** How the outer leg is chosen, e.g. "auto transport" or "via WoW". */
    fun setOuterSummary(summary: String) {
        outerSummary = summary
        setArmed(armed)
    }

    /** Paints the armed look. Does not notify [onToggle]. */
    fun setArmed(value: Boolean) {
        armed = value
        val lit = value && applicable
        titleView.text = Strings.tf("%s OVER WARP", innerName.uppercase())
        subtitleView.text = unavailableReason ?: if (value) {
            Strings.tf("armed %s inside WARP, %s", innerLabel(), outerSummary)
        } else {
            Strings.t("for when neither exit IP is accepted")
        }
        badgeView.text = when {
            !applicable -> Strings.t("N/A")
            value -> Strings.t("CHAINED")
            else -> Strings.t("OFF")
        }
        paint(lit)
        contentDescription = when {
            !applicable -> "${innerLabel()} روی WARP در دسترس نیست: $unavailableReason"
            value && unavailableReason != null -> "${innerLabel()} روی WARP فعال است، $unavailableReason"
            value -> "${innerLabel()} روی WARP فعال است"
            else -> "${innerLabel()} روی WARP خاموش است"
        }
    }

    fun isArmed(): Boolean = armed

    private fun innerLabel(): String =
        if (AppLanguage.current() == "en") {
            innerName.take(1) + innerName.drop(1).lowercase()
        } else {
            innerName
        }
}

/**
 * Masque-over-Masque: the same shape as the chain card, both hops MASQUE.
 *
 * OFF by default — two stacked handshakes are measurably slower than one, and the
 * user arms it for the networks where a single layer is not enough.
 */
class AuroraMimCard(
    context: Context,
    palette: AppAppearance.Palette,
    private val onToggle: (Boolean) -> Unit,
) : AuroraSlotCard(
    context, palette,
    title = Strings.t("MASQUE OVER MASQUE"),
    icon = AuroraIcon.LAYERS,
    accent = palette.violet,
    accentText = palette.violetText,
) {

    private var armed = false

    init {
        setOnClickListener {
            if (!isEnabled) return@setOnClickListener
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            setArmed(!armed)
            onToggle(armed)
        }
        setArmed(false)
    }

    fun setUnavailable(reason: String?, applicable: Boolean = reason == null) {
        unavailableReason = reason
        this.applicable = applicable
        isEnabled = reason == null
        setArmed(armed)
    }

    /** Paints the armed look. Does not notify [onToggle]. */
    fun setArmed(value: Boolean) {
        armed = value
        val lit = value && applicable
        // States the effect, not the mechanism: the outer hop is what the network
        // sees, which is the entire point of the second hop.
        subtitleView.text = unavailableReason ?: if (value) {
            Strings.t("two hops — the network sees the outer one")
        } else {
            Strings.t("chain a second masque hop inside the first")
        }
        badgeView.text = when {
            !applicable -> Strings.t("N/A")
            value -> Strings.t("CHAINED")
            else -> Strings.t("OFF")
        }
        paint(lit)
        contentDescription = when {
            !applicable -> "Masque over Masque unavailable: $unavailableReason"
            value && unavailableReason != null -> "Masque over Masque is on, $unavailableReason"
            value -> "Masque over Masque is on"
            else -> "Masque over Masque is off"
        }
    }

    fun isArmed(): Boolean = armed
}

/**
 * Smart Split: Iranian sites direct, everything blocked through the node.
 *
 * The subtitle never names the fragment profile the probe settled on — that is a
 * measurement, not a preference (see [SmartSplit]), so the line states the effect and
 * only mentions that a measurement *exists*.
 */
class AuroraSplitCard(
    context: Context,
    palette: AppAppearance.Palette,
    private val onToggle: (Boolean) -> Unit,
) : AuroraSlotCard(
    context, palette,
    title = Strings.t("SMART SPLIT"),
    icon = AuroraIcon.GAUGE,
    accent = palette.mint,
    accentText = palette.mintText,
) {

    private var enabledState = false

    /** What the measurement says, in the user's terms — never a profile name. */
    private var tuningSummary: String = ""

    init {
        setOnClickListener {
            if (!isEnabled) return@setOnClickListener
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            setSplitEnabled(!enabledState)
            onToggle(enabledState)
        }
        setSplitEnabled(false)
    }

    fun setUnavailable(reason: String?, applicable: Boolean = reason == null) {
        unavailableReason = reason
        this.applicable = applicable
        isEnabled = reason == null
        setSplitEnabled(enabledState)
    }

    /** Sets the tuning line, e.g. "tuned for this network". Repaints immediately. */
    fun setTuningSummary(summary: String) {
        tuningSummary = summary
        setSplitEnabled(enabledState)
    }

    /** Paints the on/off look. Does not notify [onToggle]. */
    fun setSplitEnabled(value: Boolean) {
        enabledState = value
        val lit = value && applicable
        // The effect, not the mechanism — and it stays true whichever fragment
        // profile the probe settles on.
        subtitleView.text = unavailableReason ?: when {
            !value -> Strings.t("everything through the node")
            tuningSummary.isNotEmpty() -> Strings.tf("local sites direct · %s", tuningSummary)
            else -> Strings.t("local sites direct, blocked sites via node")
        }
        badgeView.text = when {
            !applicable -> Strings.t("N/A")
            value -> Strings.t("ON")
            else -> Strings.t("OFF")
        }
        paint(lit)
        contentDescription = when {
            !applicable -> "Smart Split unavailable: $unavailableReason"
            value && unavailableReason != null -> "Smart Split is on, $unavailableReason"
            value -> "Smart Split is on"
            else -> "Smart Split is off"
        }
    }

    fun isSplitEnabled(): Boolean = enabledState
}
