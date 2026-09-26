package com.kourosh.ae

import android.content.Context
import android.graphics.Color
import com.kourosh.ae.profiled

/**
 * The Kourosh-AE 3.0 "imperial holo" palettes: one dark, one light, chosen by
 * the user. Nothing here is derived from the wallpaper.
 *
 *   - [ORBIT]     : night navy canvas, ornate gold frames, neon cyan state.
 *   - [PORCELAIN] : ivory canvas, deep gold frames, teal state.
 *
 * Gold is the brand and frame colour ([primary], [neonBlue]). Cyan is the
 * "tunnel is up" colour ([connected], [mint]). Violet is upload.
 *
 * Every accent has a `...Text` sibling that clears 4.5:1 for letters on the
 * palette's cards and page. On [ORBIT] the vivid accents already clear it; on
 * [PORCELAIN] the text siblings are darkened. Vivid accents are for shapes.
 *
 * Depth is carried by [Sculpt.Lighting] on the palette, so every surface in the
 * app changes lighting model together. [load] installs it before any drawing.
 */
object AppAppearance {

    /** Which palette the user picked. Persisted in the shared "settings" store. */
    enum class Mode(val key: String, val enLabel: String, val description: String) {
        DARK("dark", "Dark", "The original console — black glass and neon"),
        LIGHT("light", "Light — theme", "Porcelain — grey page, white cards"),
        ;

        /** Localized at call time so a language switch refreshes the label. */
        val label: String get() = Strings.t(enLabel)

        companion object {
            fun from(key: String?): Mode = entries.firstOrNull { it.key == key } ?: DARK
        }
    }

    const val PREF_KEY = "theme_mode"

    data class Palette(
        /** page background */
        val canvas: Int,
        /** raised card fill */
        val surface: Int,
        /** recessed / secondary card fill */
        val surfaceVariant: Int,
        /** primary text */
        val ink: Int,
        /** secondary text */
        val muted: Int,
        /** hairline borders */
        val divider: Int,
        /** the brand accent: imperial gold */
        val primary: Int,
        /** text and glyphs drawn on a surface filled with [primary] */
        val primaryContainer: Int,
        /** background of a selected row */
        val selectedSurface: Int,
        /** the "tunnel is up" accent: neon cyan */
        val connected: Int,
        val connectedContainer: Int,
        /** tertiary text */
        val faint: Int,
        /** download accent */
        val mint: Int,
        /** upload accent */
        val violet: Int,
        /** in-progress / speed accent */
        val amber: Int,
        /** failure accent */
        val danger: Int,
        /** Accents dark enough to be read as letters on this palette. */
        val primaryText: Int = primary,
        val connectedText: Int = connected,
        val mintText: Int = mint,
        val violetText: Int = violet,
        val amberText: Int = amber,
        val dangerText: Int = danger,
        /** Failure text on the connection headline. */
        val error: Int = danger,
        /**
         * Fixed frame accents for the home-screen cards. Shape colours only,
         * painted regardless of connection state.
         */
        val neonBlue: Int,
        val neonViolet: Int,
        /** How sculpted surfaces are lit on this palette. */
        val lighting: Sculpt.Lighting = Sculpt.DARK_LIGHTING,
    )

    /** Night scene: navy-black page, ornate gold, neon cyan. */
    val ORBIT = Palette(
        canvas = 0xFF05080D.toInt(),
        surface = 0xFF0B121B.toInt(),
        surfaceVariant = 0xFF101A26.toInt(),
        ink = 0xFFF3E6C4.toInt(),
        muted = 0xFFB9AC86.toInt(),
        divider = 0xFF3A2F18.toInt(),
        primary = 0xFFE3B856.toInt(),
        primaryContainer = 0xFF120D03.toInt(),
        selectedSurface = 0xFF0D1C28.toInt(),
        connected = 0xFF22D3EE.toInt(),
        connectedContainer = 0xFF06303A.toInt(),
        faint = 0xFF8C8263.toInt(),
        mint = 0xFF22D3EE.toInt(),
        violet = 0xFFB45CFF.toInt(),
        amber = 0xFFF5C451.toInt(),
        danger = 0xFFFF5A5F.toInt(),
        error = 0xFFFF9A94.toInt(),
        neonBlue = 0xFFD4A64A.toInt(),
        neonViolet = 0xFF22D3EE.toInt(),
        lighting = Sculpt.DARK_LIGHTING,
    )

    /**
     * Daylight: ivory page, warm white cards, deep gold frames, teal state.
     * Text values clear 4.5:1 on the card, the page and a selected row.
     */
    val PORCELAIN = Palette(
        canvas = 0xFFF3ECDD.toInt(),
        surface = 0xFFFFFCF5.toInt(),
        surfaceVariant = 0xFFF7EFDF.toInt(),
        ink = 0xFF1A1508.toInt(),
        muted = 0xFF5B4F35.toInt(),
        divider = 0xFFE2D3AE.toInt(),
        primary = 0xFFB0852B.toInt(),
        primaryContainer = 0xFF1A1508.toInt(),
        selectedSurface = 0xFFF5E7C4.toInt(),
        connected = 0xFF0891B2.toInt(),
        connectedContainer = 0xFFDDF2F7.toInt(),
        faint = 0xFF675B40.toInt(),
        mint = 0xFF0891B2.toInt(),
        violet = 0xFF7C3AED.toInt(),
        amber = 0xFFB7791F.toInt(),
        danger = 0xFFDC2626.toInt(),
        primaryText = 0xFF76560F.toInt(),
        connectedText = 0xFF0B6A82.toInt(),
        mintText = 0xFF0B6A82.toInt(),
        violetText = 0xFF6D28D9.toInt(),
        amberText = 0xFF7A4803.toInt(),
        dangerText = 0xFFB91C1C.toInt(),
        error = 0xFFB91C1C.toInt(),
        neonBlue = 0xFFB0852B.toInt(),
        neonViolet = 0xFF0891B2.toInt(),
        lighting = Sculpt.LIGHT_LIGHTING,
    )

    fun mode(context: Context): Mode = Mode.from(
        context.profiled()
            .getString(PREF_KEY, null)
    )

    fun setMode(context: Context, mode: Mode) {
        context.profiled()
            .edit()
            .putString(PREF_KEY, mode.key)
            .apply()
    }

    fun palette(mode: Mode): Palette = when (mode) {
        Mode.DARK -> ORBIT
        Mode.LIGHT -> PORCELAIN
    }

    /**
     * The palette for this session, and the only place [Sculpt.lighting] is set.
     */
    fun load(context: Context): Palette = palette(mode(context)).also {
        Sculpt.lighting = it.lighting
    }

    /** Callers use this to pick system-bar icon colour and XML theme. */
    fun isNight(context: Context): Boolean = mode(context) == Mode.DARK

    /** Perceived brightness test, used where only a colour is in hand. */
    fun isDark(color: Int): Boolean =
        (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000 < 140
}
