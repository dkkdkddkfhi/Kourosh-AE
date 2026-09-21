package com.kourosh.ae

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View

/** Elevated connection control used by the five-item home navigation bar. */
class BottomConnectButton(
    context: Context,
    private val palette: AppAppearance.Palette,
) : View(context) {
    var state: OrbitDialView.State = OrbitDialView.State.DISCONNECTED
        set(value) {
            field = value
            contentDescription = when (value) {
                OrbitDialView.State.CONNECTED, OrbitDialView.State.DEGRADED -> "قطع اتصال"
                OrbitDialView.State.CONNECTING -> "در حال اتصال"
                else -> "اتصال"
            }
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2600L
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
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * 0.43f
        val active = state == OrbitDialView.State.CONNECTED || state == OrbitDialView.State.DEGRADED
        val connecting = state == OrbitDialView.State.CONNECTING
        val accent = when {
            active -> palette.connected
            connecting -> palette.amber
            else -> palette.primary
        }

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx - r * 0.28f, cy - r * 0.36f, r * 1.4f,
            intArrayOf(Sculpt.withAlpha(accent, 0.30f), Sculpt.withAlpha(palette.ink, 0.98f)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        paint.setShadowLayer(dp(12f), 0f, dp(5f), Sculpt.withAlpha(accent, 0.70f))
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null
        paint.clearShadowLayer()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2f)
        paint.color = Sculpt.withAlpha(palette.primary, 0.95f)
        canvas.drawCircle(cx, cy, r, paint)
        paint.strokeWidth = dp(1f)
        paint.color = Sculpt.withAlpha(accent, 0.85f)
        canvas.drawCircle(cx, cy, r - dp(5f), paint)

        val ring = r - dp(9f)
        paint.strokeWidth = dp(1.6f)
        paint.color = Sculpt.withAlpha(accent, 0.55f)
        canvas.drawArc(cx - ring, cy - ring, cx + ring, cy + ring, phase * 360f - 70f, 92f, false, paint)
        canvas.drawArc(cx - ring, cy - ring, cx + ring, cy + ring, phase * 360f + 130f, 48f, false, paint)

        // Power glyph: unmistakable and compact inside the elevated control.
        val powerR = r * 0.30f
        paint.strokeWidth = dp(2.6f)
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Sculpt.onGlass(accent)
        canvas.drawArc(cx - powerR, cy - powerR, cx + powerR, cy + powerR, 42f, 276f, false, paint)
        canvas.drawLine(cx, cy - powerR * 1.18f, cx, cy + powerR * 0.08f, paint)
        paint.strokeCap = Paint.Cap.BUTT
        paint.style = Paint.Style.FILL
        if (active) {
            paint.color = Sculpt.withAlpha(palette.mint, 0.82f)
            canvas.drawCircle(cx + r * 0.55f, cy - r * 0.55f, dp(3.2f), paint)
        }
    }

    private fun dp(value: Float): Float = value * density
}
