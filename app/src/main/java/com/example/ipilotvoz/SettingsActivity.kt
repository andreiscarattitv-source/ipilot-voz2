package com.example.ipilotvoz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.widget.*

class SettingsActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }
        fun tv(t: String) = TextView(this).apply { text = t; textSize = 15f; setPadding(0, 20, 0, 8) }
        fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }
        fun edit(v: String, num: Boolean = true) = EditText(this).apply {
            setText(v); if (num) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        root.addView(TextView(this).apply { text = "Configurações"; textSize = 24f })
        root.addView(tv("Comandos: \"piloto, aumentar velocidade\", \"piloto, diminuir velocidade\", \"piloto, âncora\", " +
            "\"piloto, direita 10\", \"piloto, esquerda 25 graus\", \"piloto, ligar hélice\", \"piloto, desligar hélice\"."))
        root.addView(btn("1. Permitir microfone e notificações") {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO, "android.permission.POST_NOTIFICATIONS"), 1)
        })
        root.addView(btn("2. Ativar acessibilidade (i-Pilot Voz)") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })

        root.addView(Switch(this).apply {
            text = "Exigir palavra \"piloto\" antes do comando"; isChecked = Cfg.wake(this@SettingsActivity)
            setOnCheckedChangeListener { _, v -> Cfg.setWake(this@SettingsActivity, v) }
        })
        root.addView(Switch(this).apply {
            text = "Ao desativar a âncora, tocar no botão âncora do i-Pilot (desligue se o i-Pilot já desativa sozinho ao receber outro comando)"
            isChecked = Cfg.tapAnchorOff(this@SettingsActivity)
            setOnCheckedChangeListener { _, v -> Cfg.setTapAnchorOff(this@SettingsActivity, v) }
        })

        root.addView(tv("Graus que o i-Pilot gira por toque na seta (confira no seu motor):"))
        val deg = edit(Cfg.degPerTap(this).toString()); root.addView(deg)
        root.addView(tv("Palavras do nome do pacote do app i-Pilot (separadas por vírgula):"))
        val pkg = edit(Cfg.pkgHints(this).joinToString(","), false); root.addView(pkg)

        root.addView(tv("Calibração dos botões (% da tela: X e Y). Toque em \"Testar\" e troque para o i-Pilot em 5 s; um ponto verde mostra onde o toque acontece."))
        val fields = mutableMapOf<Btn, Pair<EditText, EditText>>()
        Btn.values().forEach { bt ->
            val (x, y) = Cfg.pos(this, bt)
            val ex = edit("%.1f".format(x * 100)); val ey = edit("%.1f".format(y * 100))
            fields[bt] = ex to ey
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(TextView(this).apply { text = bt.label; layoutParams = LinearLayout.LayoutParams(0, -2, 2f) })
            row.addView(ex.apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
            row.addView(ey.apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
            row.addView(Button(this).apply { text = "Testar"; setOnClickListener {
                save(deg, pkg, fields)
                Toast.makeText(context, "Troque para o i-Pilot (5 s)", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({ IPilotAccessibilityService.instance?.repeatTap(bt, 1) }, 5000)
            } })
            root.addView(row)
        }
        root.addView(btn("Salvar") { save(deg, pkg, fields); Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show() })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun save(deg: EditText, pkg: EditText, f: Map<Btn, Pair<EditText, EditText>>) {
        Cfg.setDegPerTap(this, deg.text.toString().toIntOrNull() ?: 5)
        Cfg.setPkgHints(this, pkg.text.toString())
        f.forEach { (bt, e) ->
            val x = e.first.text.toString().toFloatOrNull(); val y = e.second.text.toString().toFloatOrNull()
            if (x != null && y != null) Cfg.setPos(this, bt, x / 100f, y / 100f)
        }
    }

    override fun onPause() { super.onPause() }
}
