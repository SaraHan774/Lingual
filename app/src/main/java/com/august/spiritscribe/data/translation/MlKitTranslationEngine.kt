package com.august.spiritscribe.data.translation

import com.august.spiritscribe.domain.model.AppLanguage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MlKitTranslationEngine @Inject constructor() : TranslationEngine {

    override val modelVersion: String = "mlkit-translate-v1"

    private val translators = mutableMapOf<Pair<String, String>, Translator>()
    private val mutex = Mutex()
    private val preparedLanguages = mutableSetOf<AppLanguage>()

    override suspend fun translate(
        text: String,
        from: AppLanguage,
        to: AppLanguage,
    ): Result<String> = runCatching {
        if (from == to) return@runCatching text
        if (text.isBlank()) return@runCatching ""

        val translator = getOrCreateTranslator(from, to)
        translator.ensureDownloaded().getOrThrow()
        translator.translateAsync(text)
    }

    override suspend fun prepareLanguage(language: AppLanguage): Result<Unit> = runCatching {
        mutex.withLock {
            if (language in preparedLanguages) return@runCatching
        }
        val translator = getOrCreateTranslator(AppLanguage.KOREAN, language)
        translator.ensureDownloaded().getOrThrow()
        mutex.withLock { preparedLanguages.add(language) }
    }

    private suspend fun getOrCreateTranslator(from: AppLanguage, to: AppLanguage): Translator {
        val key = from.code to to.code
        mutex.withLock {
            translators[key]?.let { return it }
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(from.toMlKitTag())
                .setTargetLanguage(to.toMlKitTag())
                .build()
            val translator = Translation.getClient(options)
            translators[key] = translator
            return translator
        }
    }

    private suspend fun Translator.ensureDownloaded(): Result<Unit> = runCatching {
        val conditions = DownloadConditions.Builder().build()
        suspendCancellableCoroutine<Unit> { cont ->
            downloadModelIfNeeded(conditions)
                .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
                .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
        }
    }

    private suspend fun Translator.translateAsync(text: String): String =
        suspendCancellableCoroutine { cont ->
            translate(text)
                .addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
                .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
        }

    private fun AppLanguage.toMlKitTag(): String = when (this) {
        AppLanguage.KOREAN -> TranslateLanguage.KOREAN
        AppLanguage.ENGLISH -> TranslateLanguage.ENGLISH
        AppLanguage.JAPANESE -> TranslateLanguage.JAPANESE
        AppLanguage.CHINESE -> TranslateLanguage.CHINESE
    }
}
