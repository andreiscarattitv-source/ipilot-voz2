package com.example.ipilotvoz

import android.os.Handler
import android.os.Looper

/** Estado compartilhado entre serviços e tela. propOn = null significa "desconhecido". */
object State {
    @Volatile var anchorOn = false
    @Volatile var propOn: Boolean? = null
    @Volatile var listening = false
    @Volatile var last = "—"
    @Volatile var listener: (() -> Unit)? = null
    private val h = Handler(Looper.getMainLooper())
    fun changed() { h.post { listener?.invoke() } }
}
