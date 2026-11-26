package com.example.ipilotvoz

import android.content.Context

/** Botões do i-Pilot e posição padrão (fração da tela), tirada do print enviado. Ajustável na tela de calibração. */
enum class Btn(val dx: Float, val dy: Float, val label: String) {
    PLUS(0.50f, 0.426f, "Aumentar velocidade (+)"),
    MINUS(0.50f, 0.676f, "Diminuir velocidade (−)"),
    LEFT(0.23f, 0.555f, "Seta esquerda"),
    RIGHT(0.78f, 0.555f, "Seta direita"),
    ANCHOR(0.81f, 0.378f, "Âncora"),
    PROP(0.50f, 0.555f, "Hélice (liga/desliga)"),
    NORTH(0.50f, 0.314f, "N (travar rumo)")
}

object Cfg {
    private fun p(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    fun pos(c: Context, b: Btn) = p(c).getFloat("x_${b.name}", b.dx) to p(c).getFloat("y_${b.name}", b.dy)
    fun setPos(c: Context, b: Btn, x: Float, y: Float) =
        p(c).edit().putFloat("x_${b.name}", x).putFloat("y_${b.name}", y).apply()
    fun degPerTap(c: Context) = p(c).getInt("degTap", 5).coerceAtLeast(1)
    fun setDegPerTap(c: Context, v: Int) = p(c).edit().putInt("degTap", v).apply()
    fun wake(c: Context) = p(c).getBoolean("wake", true)
    fun setWake(c: Context, v: Boolean) = p(c).edit().putBoolean("wake", v).apply()
    fun tapAnchorOff(c: Context) = p(c).getBoolean("anchorOffTap", true)
    fun setTapAnchorOff(c: Context, v: Boolean) = p(c).edit().putBoolean("anchorOffTap", v).apply()
    fun pkgHints(c: Context): List<String> =
        p(c).getString("pkg", "ipilot,minnkota,minkota,johnson")!!.lowercase().split(",").map { it.trim() }.filter { it.isNotEmpty() }
    fun setPkgHints(c: Context, v: String) = p(c).edit().putString("pkg", v).apply()
}
