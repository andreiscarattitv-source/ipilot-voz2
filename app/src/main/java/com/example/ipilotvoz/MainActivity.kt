package com.example.ipilotvoz

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var pad: PadView
    private lateinit var mic: Button
    private lateinit var vProp: TextView
    private lateinit var vAnchor: TextView
    private lateinit var vLast: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF3A3A46.toInt()) }

        // Barra superior: menu + logo
        val top = FrameLayout(this)
        val menu = TextView(this).apply {
            text = "☰"; textSize = 22f; setTextColor(0xFFEEEEEE.toInt()); gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF55556A.toInt()) }
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        top.addView(menu, FrameLayout.LayoutParams(dp(46), dp(46)).apply { leftMargin = dp(16); topMargin = dp(14); bottomMargin = dp(8) })
        val title = TextView(this).apply {
            textSize = 28f; typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFFFFFFFF.toInt())
            text = SpannableStringBuilder("▶ i-Pilot Voz").apply {
                setSpan(ForegroundColorSpan(0xFFF5A623.toInt()), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        top.addView(title, FrameLayout.LayoutParams(-2, -2).apply { leftMargin = dp(78); topMargin = dp(18) })
        root.addView(top)

        // Painel circular
        pad = PadView(this).apply {
            onPadTap = { bt ->
                when (bt) {
                    Btn.PROP -> { State.propOn = State.propOn != true
                        Toast.makeText(this@MainActivity, "Estado sincronizado: hélice ${if (State.propOn == true) "ligada" else "desligada"}", Toast.LENGTH_SHORT).show() }
                    Btn.ANCHOR -> { State.anchorOn = !State.anchorOn
                        Toast.makeText(this@MainActivity, "Estado sincronizado: âncora ${if (State.anchorOn) "ativa" else "desativada"}", Toast.LENGTH_SHORT).show() }
                    else -> {}
                }
                refresh()
            }
        }
        root.addView(pad, LinearLayout.LayoutParams(-1, -2))

        // Botão de escuta
        mic = Button(this).apply { setOnClickListener { toggleListening() }; textSize = 16f; typeface = Typeface.DEFAULT_BOLD }
        root.addView(mic, LinearLayout.LayoutParams(-1, dp(56)).apply { setMargins(dp(24), dp(8), dp(24), dp(4)) })
        root.addView(TextView(this).apply {
            text = "Toque na hélice ou na âncora do painel para sincronizar o estado real do motor."
            textSize = 12f; setTextColor(0xFFB0B0C0.toInt()); gravity = Gravity.CENTER; setPadding(dp(24), dp(4), dp(24), 0)
        })
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))

        // Barra inferior (como PROP STATUS / SOG / PROP SPEED do app)
        root.addView(View(this).apply { setBackgroundColor(0xFF1E88E5.toInt()) }, LinearLayout.LayoutParams(-1, dp(4)))
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(dp(8), dp(8), dp(8), dp(12)) }
        fun cell(label: String, w: Float): TextView {
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            col.addView(TextView(this).apply { text = label; textSize = 12f; setTextColor(0xFF555555.toInt()) })
            val v = TextView(this).apply { textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFF222222.toInt()); maxLines = 2 }
            col.addView(v)
            bar.addView(col, LinearLayout.LayoutParams(0, -2, w).apply { setMargins(dp(6), 0, dp(6), 0) })
            return v
        }
        vProp = cell("PROP STATUS", 1f); vAnchor = cell("ÂNCORA", 0.8f); vLast = cell("ÚLTIMO COMANDO", 1.6f)
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)
    }

    private fun toggleListening() {
        if (State.listening) { stopService(Intent(this, VoiceService::class.java)); return }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO, "android.permission.POST_NOTIFICATIONS"), 1)
            return
        }
        if (IPilotAccessibilityService.instance == null)
            Toast.makeText(this, "Ative o serviço de acessibilidade em ☰ > Configurações", Toast.LENGTH_LONG).show()
        ContextCompat.startForegroundService(this, Intent(this, VoiceService::class.java))
    }

    private fun refresh() {
        vProp.text = when (State.propOn) { true -> "LIGADA"; false -> "DESLIGADA"; null -> "?" }
        vAnchor.text = if (State.anchorOn) "ATIVA" else "OFF"
        vLast.text = State.last
        mic.text = if (State.listening) "● OUVINDO — TOQUE PARA PARAR" else "INICIAR ESCUTA POR VOZ"
        mic.setTextColor(0xFF2B2E3A.toInt())
        mic.background = GradientDrawable().apply {
            cornerRadius = dp(28).toFloat(); setColor(if (State.listening) 0xFF7CE08A.toInt() else 0xFFD0D0D0.toInt())
        }
        pad.invalidate()
    }

    override fun onResume() { super.onResume(); State.listener = { refresh() }; refresh() }
    override fun onPause() { State.listener = null; super.onPause() }
}
