package com.example.ipilotvoz

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlin.math.roundToInt

class IPilotAccessibilityService : AccessibilityService() {
    companion object { @Volatile var instance: IPilotAccessibilityService? = null }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun later(ms: Long, f: () -> Unit) { if (ms <= 0) f() else handler.postDelayed({ f() }, ms) }

    /** Executa o comando e devolve as frases a serem faladas. */
    fun execute(cmd: Cmd): List<String> {
        val pkg = rootInActiveWindow?.packageName?.toString() ?: ""
        if (Cfg.pkgHints(this).none { pkg.lowercase().contains(it) })
            return listOf("Abra o i-Pilot na tela")
        val msgs = mutableListOf<String>()
        var delay = 0L

        // Âncora ativa + qualquer instrução => desativa a âncora primeiro
        if (State.anchorOn) {
            State.anchorOn = false
            msgs += "Âncora desativado"
            if (Cfg.tapAnchorOff(this)) { repeatTap(Btn.ANCHOR, 1); delay = 1000L }
            if (cmd is Cmd.Anchor) { State.changed(); return msgs }
        }

        when (cmd) {
            is Cmd.Speed -> {
                later(delay) { repeatTap(if (cmd.up) Btn.PLUS else Btn.MINUS, cmd.steps) }
                msgs += if (cmd.up) "Aumentando velocidade" else "Diminuindo velocidade"
            }
            is Cmd.Turn -> {
                val n = (cmd.degrees / Cfg.degPerTap(this).toFloat()).roundToInt().coerceIn(1, 36)
                later(delay) { repeatTap(if (cmd.right) Btn.RIGHT else Btn.LEFT, n) }
                msgs += "${if (cmd.right) "Direita" else "Esquerda"} ${cmd.degrees} graus"
            }
            Cmd.Anchor -> {
                later(delay) { repeatTap(Btn.ANCHOR, 1) }
                State.anchorOn = true
                msgs += "Âncora ativado"
            }
            is Cmd.Prop -> {
                val want = cmd.on ?: (State.propOn != true)
                if (State.propOn == want) {
                    msgs += "Hélice já está ${if (want) "ligada" else "desligada"}"
                } else {
                    later(delay) { repeatTap(Btn.PROP, 1) }
                    State.propOn = want
                    msgs += if (want) "Hélice ligada" else "Hélice desligada"
                }
            }
            Cmd.HeadingLock -> { later(delay) { repeatTap(Btn.NORTH, 1) }; msgs += "Rumo travado" }
        }
        State.changed()
        return msgs
    }

    private fun realMetrics(): DisplayMetrics {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(dm)
        return dm
    }

    fun repeatTap(b: Btn, times: Int, i: Int = 0) {
        if (i >= times) return
        val dm = realMetrics()
        val (fx, fy) = Cfg.pos(this, b)
        val x = fx * dm.widthPixels; val y = fy * dm.heightPixels
        showDot(x, y)
        val path = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60)).build()
        val next = { handler.postDelayed({ repeatTap(b, times, i + 1) }, 300) }
        dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { next() }
            override fun onCancelled(d: GestureDescription?) { next() }
        }, null)
    }

    /** Ponto verde sobre o local do toque (janela de sobreposição de acessibilidade, não intercepta toques). */
    private fun showDot(x: Float, y: Float) {
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val size = (56 * resources.displayMetrics.density).toInt()
            val v = object : View(this) {
                val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00E676.toInt() }
                val ringP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 5f }
                override fun onDraw(c: Canvas) {
                    val r = width / 2f - 4f
                    c.drawCircle(width / 2f, height / 2f, r, fillP)
                    c.drawCircle(width / 2f, height / 2f, r, ringP)
                }
            }
            val lp = WindowManager.LayoutParams(
                size, size, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START; this.x = (x - size / 2).toInt(); this.y = (y - size / 2).toInt() }
            wm.addView(v, lp)
            v.animate().alpha(0f).setStartDelay(250).setDuration(500).withEndAction {
                try { wm.removeView(v) } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {}
    }
}
