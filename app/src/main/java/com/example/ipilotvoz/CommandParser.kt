package com.example.ipilotvoz

import java.text.Normalizer

sealed class Cmd {
    data class Speed(val up: Boolean, val steps: Int) : Cmd()
    data class Turn(val right: Boolean, val degrees: Int) : Cmd()
    object Anchor : Cmd()
    data class Prop(val on: Boolean?) : Cmd()
    object HeadingLock : Cmd()
}

object CommandParser {
    private val tens = mapOf(
        "cinco" to 5, "dez" to 10, "quinze" to 15, "vinte" to 20, "trinta" to 30, "quarenta" to 40,
        "cinquenta" to 50, "sessenta" to 60, "setenta" to 70, "oitenta" to 80, "noventa" to 90,
        "cem" to 100, "cento" to 100
    )
    private val small = mapOf("um" to 1, "uma" to 1, "dois" to 2, "duas" to 2, "tres" to 3, "quatro" to 4)

    private fun norm(s: String) =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    /** Retorna o comando ou null. Com [wake]=true exige a palavra "piloto" antes (ex.: "piloto, direita 10"). */
    fun parse(raw: String, wake: Boolean): Cmd? {
        var t = norm(raw).trim()
        if (wake) {
            val i = t.indexOf("piloto")
            if (i < 0) return null
            t = t.substring(i + 6)
        }
        val w = t.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        fun has(vararg k: String) = w.any { it in k }

        // Graus: soma números por extenso ("vinte e cinco") ou dígitos ("25")
        var deg = 0; var found = false
        for (tok in w) {
            tok.toIntOrNull()?.let { deg += it; found = true }
                ?: tens[tok]?.let { deg += it; found = true }
        }
        val smallN = w.firstNotNullOfOrNull { it.toIntOrNull() ?: small[it] } ?: 1

        return when {
            has("ancora", "ancorar", "ancorado") -> Cmd.Anchor
            has("direita", "boreste", "estibordo") -> Cmd.Turn(true, if (found) deg else 5)
            has("esquerda", "bombordo") -> Cmd.Turn(false, if (found) deg else 5)
            has("velocidade", "acelerar", "desacelerar", "reduzir", "reduza", "diminuir", "diminua", "aumentar", "aumente") -> {
                val down = has("diminuir", "diminua", "reduzir", "reduza", "menos", "desacelerar", "devagar")
                Cmd.Speed(!down, smallN.coerceIn(1, 10))
            }
            has("helice", "propulsor", "motor") && has("parar", "pare", "desligar", "desligue", "ligar", "ligue", "alternar") ->
                Cmd.Prop(if (has("parar", "pare", "desligar", "desligue")) false else if (has("ligar", "ligue")) true else null)
            has("rumo", "norte") && has("travar", "trave", "fixar", "fixe", "bussola") -> Cmd.HeadingLock
            else -> null
        }
    }
}
