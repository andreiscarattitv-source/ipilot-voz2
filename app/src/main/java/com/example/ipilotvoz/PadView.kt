package com.example.ipilotvoz

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot

/** Desenha o painel circular no estilo do i-Pilot. Coordenadas de projeto: 720 de largura, origem no centro do círculo. */
class PadView(c: Context) : View(c) {
    var onPadTap: ((Btn) -> Unit)? = null

    private class Sec(val id: Btn?, val a0: Float, val sweep: Float, val r0: Float, val r1: Float)
    private val secs = listOf(
        Sec(Btn.PLUS, 229f, 82f, 150f, 258f), Sec(Btn.MINUS, 49f, 82f, 150f, 258f),
        Sec(Btn.LEFT, 139f, 82f, 150f, 258f), Sec(Btn.RIGHT, -41f, 82f, 150f, 258f),
        Sec(null, 212f, 38f, 285f, 390f), Sec(Btn.NORTH, 251f, 38f, 285f, 390f), Sec(Btn.ANCHOR, 291f, 38f, 285f, 390f)
    )
    private val green = 0xFF7CE08A.toInt()
    private val ink = 0xFF6F6F6F.toInt()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { pathEffect = CornerPathEffect(16f) }
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2B2E3A.toInt(); pathEffect = CornerPathEffect(30f) }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; style = Paint.Style.STROKE; strokeWidth = 9f; strokeCap = Paint.Cap.ROUND }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 62f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val grad = LinearGradient(0f, -420f, 0f, 420f, 0xFFE4E4E4.toInt(), 0xFFB8B8B8.toInt(), Shader.TileMode.CLAMP)
    private var s = 1f; private var cx = 0f; private var cy = 0f

    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w); setMeasuredDimension(width, (width * 1.02f).toInt())
    }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { s = w / 720f; cx = w / 2f; cy = 415f * s }

    private fun sector(a0: Float, sweep: Float, r0: Float, r1: Float) = Path().apply {
        arcTo(RectF(-r1, -r1, r1, r1), a0, sweep); arcTo(RectF(-r0, -r0, r0, r0), a0 + sweep, -sweep); close()
    }
    private fun paintFill(active: Boolean) = fill.apply {
        if (active) { shader = null; color = green } else shader = grad
    }

    override fun onDraw(cv: Canvas) {
        cv.translate(cx, cy); cv.scale(s, s)
        cv.drawPath(sector(190f, 160f, 250f, 430f), dark)
        cv.drawCircle(0f, 0f, 300f, dark)
        secs.forEach { cv.drawPath(sector(it.a0, it.sweep, it.r0, it.r1), paintFill(it.id == Btn.ANCHOR && State.anchorOn)) }
        cv.drawCircle(0f, 0f, 150f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2B2E3A.toInt() })
        cv.drawCircle(0f, 0f, 135f, paintFill(State.propOn == true))

        // + e −
        cv.drawRect(-30f, -208f, 30f, -192f, icon); cv.drawRect(-8f, -232f, 8f, -168f, icon)
        cv.drawRect(-30f, 192f, 30f, 208f, icon)
        // setas
        cv.drawPath(Path().apply { moveTo(-238f, 0f); lineTo(-180f, -38f); lineTo(-180f, 38f); close() }, icon)
        cv.drawPath(Path().apply { moveTo(238f, 0f); lineTo(180f, -38f); lineTo(180f, 38f); close() }, icon)
        // N
        cv.drawText("N", 0f, -355f, txt)
        cv.drawPath(Path().apply { moveTo(0f, -338f); lineTo(-17f, -312f); lineTo(17f, -312f); close() }, icon)
        cv.drawRect(-5f, -312f, 5f, -288f, icon)
        // âncora
        val ax = 225f; val ay = -275f
        cv.drawCircle(ax, ay - 38f, 11f, stroke)
        cv.drawLine(ax, ay - 27f, ax, ay + 32f, stroke)
        cv.drawLine(ax - 18f, ay - 14f, ax + 18f, ay - 14f, stroke)
        cv.drawArc(RectF(ax - 42f, ay - 24f, ax + 42f, ay + 36f), 20f, 140f, false, stroke)
        // coelho (modo rápido do app; só desenho)
        val rx = -230f; val ry = -280f
        cv.drawOval(RectF(rx - 28f, ry - 6f, rx + 22f, ry + 22f), icon)
        cv.drawCircle(rx + 22f, ry - 14f, 11f, icon)
        cv.drawOval(RectF(rx + 13f, ry - 44f, rx + 22f, ry - 16f), icon)
        cv.drawOval(RectF(rx + 22f, ry - 42f, rx + 31f, ry - 16f), icon)
        // hélice
        cv.drawOval(RectF(-100f, -38f, -14f, 8f), icon)
        cv.drawOval(RectF(14f, -8f, 100f, 38f), icon)
        cv.drawCircle(0f, 0f, 26f, Paint(stroke).apply { strokeWidth = 14f; color = if (State.propOn == true) 0xFF2E7D32.toInt() else ink })
    }

    private fun hit(x: Float, y: Float): Btn? {
        val dx = (x - cx) / s; val dy = (y - cy) / s
        val r = hypot(dx, dy)
        if (r < 140f) return Btn.PROP
        val a = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360) % 360).toFloat()
        return secs.firstOrNull {
            r in it.r0..it.r1 && ((a - (it.a0 + 360) % 360 + 360) % 360) <= it.sweep
        }?.id
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) hit(e.x, e.y)?.let { onPadTap?.invoke(it) }
        return true
    }
}
