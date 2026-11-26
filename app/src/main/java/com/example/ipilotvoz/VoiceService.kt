package com.example.ipilotvoz

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import androidx.core.app.ServiceCompat
import java.util.Locale

class VoiceService : Service(), RecognitionListener {
    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val h = Handler(Looper.getMainLooper())
    private var running = false

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("voz", "Comando de voz", NotificationManager.IMPORTANCE_LOW))
        ServiceCompat.startForeground(this, 1, notif("Ouvindo... diga \"piloto, direita 10\""),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        if (!running) {
            running = true
            State.listening = true; State.changed()
            tts = TextToSpeech(this) { tts?.language = Locale("pt", "BR") }
            listen()
        }
        return START_STICKY
    }

    private fun notif(text: String) = Notification.Builder(this, "voz")
        .setContentTitle("i-Pilot Voz").setContentText(text)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build()

    private fun listen() {
        if (!running) return
        sr?.destroy()
        sr = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        sr?.startListening(intent)
    }

    override fun onResults(b: Bundle?) {
        val list = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        val wake = Cfg.wake(this)
        for (s in list) {
            val cmd = CommandParser.parse(s, wake) ?: continue
            val msgs = IPilotAccessibilityService.instance?.execute(cmd) ?: listOf("Ative o serviço de acessibilidade")
            State.last = msgs.joinToString(", "); State.changed()
            getSystemService(NotificationManager::class.java).notify(1, notif("\"$s\" → ${State.last}"))
            msgs.forEachIndexed { idx, m ->
                tts?.speak(m, if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "fb$idx")
            }
            break
        }
        h.postDelayed({ listen() }, 300)
    }

    override fun onError(e: Int) {
        h.postDelayed({ listen() }, if (e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1000 else 400)
    }

    override fun onDestroy() {
        running = false; State.listening = false; State.changed()
        h.removeCallbacksAndMessages(null)
        sr?.destroy(); tts?.shutdown(); super.onDestroy()
    }

    override fun onReadyForSpeech(p: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(v: Float) {}
    override fun onBufferReceived(b: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(b: Bundle?) {}
    override fun onEvent(t: Int, b: Bundle?) {}
}
