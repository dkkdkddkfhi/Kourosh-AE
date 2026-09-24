package com.kourosh.ae

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat

/** Persisted UI font choice. Missing optional files intentionally fall back to system. */
object FontChoice {
    private const val PREF = "ui_font_choice"
    private const val KEY = "family"
    private const val SCALE_KEY = "text_scale"

    /**
     * The one voice the interface speaks in.
     *
     * The family picker is gone: eleven faces, six of which were never shipped, and a
     * sample-paragraph wall that changed the whole console's metrics when it was used.
     * Custom families are not merely unpicked, they are refused — [current] always
     * answers [Family.SYSTEM], so an old install that had chosen Dast Nevis comes back
     * on the system face with no migration step, and no later caller can reintroduce a
     * third-party face by accident. The size control is kept: that one is accessibility,
     * not decoration.
     */
    enum class Family(val key: String, val label: String, val resource: Int?) {
        SYSTEM("system", "System default", null),
        ;

        val available: Boolean get() = resource != null
    }

    /** Always the system face. See [Family]. */
    fun current(context: Context): Family = Family.SYSTEM
    fun scale(context: Context): Float = context.profiled().getFloat(SCALE_KEY, 1f).coerceIn(0.9f, 1.3f)

    fun setScale(context: Context, value: Float) {
        context.profiled().edit().putFloat(SCALE_KEY, value.coerceIn(0.9f, 1.3f)).apply()
    }

    fun regular(context: Context): Typeface {
        val chosen = current(context)
        return chosen.resource?.let { runCatching { ResourcesCompat.getFont(context, it) }.getOrNull() }
            ?: Typeface.create("sans", Typeface.NORMAL)
    }

    fun medium(context: Context): Typeface {
        val chosen = current(context)
        return when (chosen) {
            Family.VAZIRMATN, Family.VAZIRMATN_BOLD -> runCatching { ResourcesCompat.getFont(context, R.font.vazirmatn_bold) }.getOrNull()
            Family.NOTO_SANS, Family.NOTO_SANS_MEDIUM -> runCatching { ResourcesCompat.getFont(context, R.font.noto_sc_medium) }.getOrNull()
            else -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        } ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
}
