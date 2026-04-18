package com.august.spiritscribe.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.august.spiritscribe.domain.model.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class TtsState {
    data object Idle : TtsState()
    data class Playing(val language: AppLanguage, val utteranceId: String) : TtsState()
    data class Error(val message: String) : TtsState()
}

@Singleton
class TtsService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (!isReady) _state.value = TtsState.Error("TTS engine not available")
        }?.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    _state.value = TtsState.Idle
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _state.value = TtsState.Error("Playback failed")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _state.value = TtsState.Error("Playback failed ($errorCode)")
                }
            })
        }
    }

    fun speak(
        text: String,
        language: AppLanguage,
        speechRate: Float = 1f,
        pitch: Float = 1f,
    ) {
        val engine = tts ?: run {
            _state.value = TtsState.Error("TTS engine not initialized")
            return
        }
        if (!isReady) {
            _state.value = TtsState.Error("TTS not ready")
            return
        }
        val result = engine.setLanguage(language.toLocale())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _state.value = TtsState.Error("Language not supported: ${language.code}")
            return
        }
        engine.setSpeechRate(speechRate.coerceIn(0.5f, 2f))
        engine.setPitch(pitch.coerceIn(0.5f, 2f))
        val utteranceId = "lingual_${System.currentTimeMillis()}"
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        _state.value = TtsState.Playing(language, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _state.value = TtsState.Idle
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _state.value = TtsState.Idle
    }
}
