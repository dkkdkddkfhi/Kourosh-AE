package com.kourosh.ae

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Compact vector shield used by the Pro Home horizontal connect action. */
class ProConnectShieldView(
    context: Context,
    private val palette: AppAppearance.Palette,
) : View(context) {
    var state: OrbitDialView.State = OrbitDialView.State.DISCONNECTED
        set(value) {
            if (field == value) return
            field = value
            contentDescription = when (value) {
                OrbitDialView.State.CONNECTED,
                OrbitDialView.State.DEGRADED -> "محافظت فعال"
                OrbitDialView.State.CONNECTING -> "در حال برقراری محافظت"
                OrbitDialView.State.FAILED -> "خطای اتصال"
                OrbitDialView.State.DISCONNECTED -> "اتصال"
            }
            invalidate()
        }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shield = Path()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        contentDescription = "اتصال"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val top = h * 0.12f
        val bottom = h * 0.88f
        val half = w * 0.31f
        val accent = when (state) {
            OrbitDialView.State.CONNECTED -> palette.connected
            OrbitDialView.State.DEGRADED -> palette.amber
            OrbitDialView.State.CONNECTING -> palette.primary
            OrbitDialView.State.FAILED -> palette.danger
            OrbitDialView.State.DISCONNECTED -> palette.primary
        }

        shield.rewind()
        shield.moveTo(cx, top)
        shield.lineTo(cx + half, top + h * 0.18f)
        shield.lineTo(cx + half * 0.82f, h * 0.62f)
        shield.quadTo(cx, bottom, cx, bottom)
        shield.quadTo(cx, bottom, cx - half * 0.82f, h * 0.62f)
        shield.lineTo(cx - half, top + h * 0.18f)
        shield.close()

        fill.color = Sculpt.withAlpha(accent, if (state == OrbitDialView.State.CONNECTED) 0.24f else 0.12f)
        canvas.drawPath(shield, fill)
        stroke.color = Sculpt.withAlpha(accent, 0.92f)
        stroke.strokeWidth = 1.7f * d
        canvas.drawPath(shield, stroke)

        stroke.strokeWidth = 1.8f * d
        when (state) {
            OrbitDialView.State.CONNECTED,
            OrbitDialView.State.DEGRADED -> {
                val check = Path().apply {
                    moveTo(cx - w * 0.18f, h * 0.51f)
                    lineTo(cx - w * 0.04f, h * 0.65f)
                    lineTo(cx + w * 0.21f, h * 0.36f)
                }
                canvas.drawPath(check, stroke)
            }
            OrbitDialView.State.CONNECTING -> {
                canvas.drawCircle(cx, h * 0.51f, w * 0.13f, stroke)
                canvas.drawLine(cx, h * 0.51f, cx, h * 0.39f, stroke)
                canvas.drawLine(cx, h * 0.51f, cx + w * 0.09f, h * 0.57f, stroke)
            }
            OrbitDialView.State.FAILED -> {
                canvas.drawLine(cx - w * 0.14f, h * 0.39f, cx + w * 0.14f, h * 0.63f, stroke)
                canvas.drawLine(cx + w * 0.14f, h * 0.39f, cx - w * 0.14f, h * 0.63f, stroke)
            }
            OrbitDialView.State.DISCONNECTED -> {
                canvas.drawCircle(cx, h * 0.51f, w * 0.08f, stroke)
            }
        }
    }
}
