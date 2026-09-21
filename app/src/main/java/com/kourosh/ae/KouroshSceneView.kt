package com.kourosh.ae

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/** Native Kourosh backdrop: gradients, depth rings, columns, particles and gold geometry. */
class KouroshSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1f) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    // Reused on every frame. Creating Paint/Path objects inside onDraw caused
    // avoidable GC pressure while the live scene was animating at 30 fps.
    private val aurora = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ribbon = Path()
    private val shield = Path()
    private val particles = Array(36) { PointF(Random.nextFloat(), Random.nextFloat()) }
    private var phase = 0f
    private var running = false
    private val frame = object : Runnable {
        override fun run() {
            if (!running || !isShown) return
            phase += 0.008f
            invalidate()
            postDelayed(this, 33L)
        }
    }

    init { isClickable = false }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); running = true; post(frame) }
    override fun onDetachedFromWindow() { running = false; removeCallbacks(frame); super.onDetachedFromWindow() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        bg.shader = LinearGradient(0f, 0f, 0f, h, Color.rgb(2, 12, 18), Color.rgb(4, 5, 7), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, bg); bg.shader = null
        val cx = w * .58f; val cy = h * .34f
        // Slow aurora ribbons add depth without a bitmap or a static scene.
        aurora.strokeWidth = dp(18f)
        aurora.color = Color.argb(32, 34, 211, 197)
        ribbon.rewind()
        ribbon.moveTo(-dp(30f), h * .28f)
        ribbon.cubicTo(w * .22f, h * (.20f + .03f * sin(phase)), w * .48f, h * (.42f + .02f * sin(phase + 1f)), w + dp(30f), h * .27f)
        c.drawPath(ribbon, aurora)
        aurora.color = Color.argb(22, 124, 245, 228)
        aurora.strokeWidth = dp(9f)
        c.drawPath(ribbon, aurora)
        // Low sun and concentric royal seal.
        fill.color = Color.argb(30, 246, 217, 139)
        c.drawCircle(cx, cy, dp(116f), fill)
        fill.color = Color.argb(18, 124, 245, 228)
        c.drawCircle(cx + sin(phase) * dp(6f), cy, dp(164f), fill)
        gold.color = Color.argb(110, 246, 217, 139)
        gold.strokeWidth = dp(1f)
        for (i in 0..3) c.drawCircle(cx, cy, dp(74f + i * 13f), gold)
        c.drawLine(cx - dp(150f), cy, cx + dp(150f), cy, gold)
        c.drawLine(cx, cy - dp(150f), cx, cy + dp(150f), gold)
        // Faravahar-inspired geometric wings, deliberately vector-drawn.
        gold.strokeWidth = dp(2f)
        val wingY = dp(54f)
        c.drawArc(RectF(cx - dp(156f), wingY - dp(22f), cx, wingY + dp(30f)), 190f, 160f, false, gold)
        c.drawArc(RectF(cx, wingY - dp(22f), cx + dp(156f), wingY + dp(30f)), 190f, 160f, false, gold)
        for (i in 0..5) {
            val offset = dp(18f + i * 22f)
            c.drawLine(cx - offset, wingY + dp(8f), cx - offset - dp(20f), wingY + dp(18f + i * 2f), gold)
            c.drawLine(cx + offset, wingY + dp(8f), cx + offset + dp(20f), wingY + dp(18f + i * 2f), gold)
        }
        // Stylised Cyrus profile: crown, nose, beard and robe, all vector strokes.
        val px = w * .17f; val py = h * .35f
        gold.color = Color.argb(72, 246, 217, 139); gold.strokeWidth = dp(2f)
        c.drawOval(RectF(px - dp(30f), py - dp(42f), px + dp(18f), py + dp(24f)), gold)
        c.drawLine(px - dp(28f), py - dp(36f), px + dp(18f), py - dp(36f), gold)
        c.drawLine(px - dp(22f), py - dp(52f), px + dp(12f), py - dp(52f), gold)
        c.drawLine(px - dp(20f), py - dp(52f), px - dp(28f), py - dp(36f), gold)
        c.drawLine(px + dp(12f), py - dp(52f), px + dp(18f), py - dp(36f), gold)
        c.drawLine(px + dp(18f), py - dp(5f), px + dp(33f), py + dp(2f), gold)
        c.drawArc(RectF(px - dp(6f), py + dp(8f), px + dp(30f), py + dp(44f)), 15f, 150f, false, gold)
        c.drawLine(px - dp(26f), py + dp(28f), px - dp(62f), py + dp(98f), gold)
        c.drawLine(px + dp(10f), py + dp(24f), px + dp(58f), py + dp(98f), gold)
        // Minimal shield emblem in the center.
        shield.rewind()
        shield.moveTo(cx, cy - dp(54f)); shield.lineTo(cx + dp(42f), cy - dp(34f)); shield.lineTo(cx + dp(34f), cy + dp(34f)); shield.lineTo(cx, cy + dp(58f)); shield.lineTo(cx - dp(34f), cy + dp(34f)); shield.lineTo(cx - dp(42f), cy - dp(34f)); shield.close()
        fill.color = Color.argb(180, 8, 7, 5); c.drawPath(shield, fill)
        gold.strokeWidth = dp(2f); c.drawPath(shield, gold)
        // Abstract Persepolis columns and mountain horizon.
        gold.strokeWidth = dp(1f); gold.color = Color.argb(65, 212, 166, 74)
        val horizon = h * .70f
        c.drawLine(0f, horizon, w, horizon, gold)
        for (i in 0..4) {
            val x = w * (.08f + i * .24f); val top = horizon - dp(55f + (i % 2) * 28f)
            c.drawRect(x, top, x + dp(14f), horizon, gold)
            c.drawLine(x - dp(5f), top, x + dp(19f), top, gold)
        }
        // Gold dust with a slow, low-cost drift.
        fill.color = Color.argb(145, 246, 217, 139)
        particles.forEachIndexed { i, p ->
            val x = p.x * w + sin(phase + i) * dp(2f)
            val y = p.y * h + sin(phase * .7f + i) * dp(2f)
            c.drawCircle(x, y, dp(if (i % 5 == 0) 1.2f else .55f), fill)
        }
        // Cool ground reflection keeps the scene cinematic behind the controls.
        bg.shader = LinearGradient(0f, h * .55f, 0f, h, Color.argb(0, 3, 15, 19), Color.argb(220, 2, 4, 6), Shader.TileMode.CLAMP)
        c.drawRect(0f, h * .52f, w, h, bg); bg.shader = null
        // Fade under the scene so the live controls remain the visual priority.
        bg.shader = LinearGradient(0f, h * .46f, 0f, h, Color.argb(0, 4, 5, 6), Color.argb(250, 4, 5, 6), Shader.TileMode.CLAMP)
        c.drawRect(0f, h * .42f, w, h, bg); bg.shader = null
    }
    private fun dp(v: Float) = v * resources.displayMetrics.density
}
