package com.august.spiritscribe.ui.translate

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.august.spiritscribe.BuildConfig
import com.august.spiritscribe.data.translation.TranslationEngine
import com.august.spiritscribe.domain.model.AppLanguage
import com.august.spiritscribe.domain.model.WordCard
import com.august.spiritscribe.domain.repository.DiaryRepository
import com.august.spiritscribe.utils.TtsService
import com.august.spiritscribe.utils.TtsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

sealed class PlaygroundResult {
    data object Idle : PlaygroundResult()
    data object Loading : PlaygroundResult()
    data class Success(val text: String) : PlaygroundResult()
    data class Error(val message: String) : PlaygroundResult()
}

sealed class PlaygroundEvent {
    /** AC-T02-21 — 단어 카드 저장 성공. UI 가 스낵바 표시 + Action 으로 FlashCard 로 이동. */
    data object WordCardSaved : PlaygroundEvent()

    /** AC-T02-22 — 단어 카드 저장 실패. */
    data object WordCardSaveFailed : PlaygroundEvent()

    /** AC-T02-27 — 해당 Locale TTS 데이터 미설치. */
    data object TtsUnavailable : PlaygroundEvent()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class TranslatePlaygroundViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val translationEngine: TranslationEngine,
    private val ttsService: TtsService,
) : ViewModel() {

    private val _sourceLanguage = MutableStateFlow(defaultSourceLanguage())
    val sourceLanguage: StateFlow<AppLanguage> = _sourceLanguage.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _results = MutableStateFlow(initialResults(_sourceLanguage.value))
    val results: StateFlow<Map<AppLanguage, PlaygroundResult>> = _results.asStateFlow()

    /** AC-T02-28 — 카드가 한 번이라도 Loading/Success/Error 로 진입하면 영구 false. */
    private val _showFirstUseHint = MutableStateFlow(true)
    val showFirstUseHint: StateFlow<Boolean> = _showFirstUseHint.asStateFlow()

    /**
     * AC-T02-12 — 현재 [results] 가 어떤 (source, text) 에 대해 산출되었는지의 스냅샷.
     * UI 가 현재 [inputText] / [sourceLanguage] 와 비교해 다르면 카드를 alpha 0.5 (outdated) 로 표시한다.
     * 디바운스 경계 통과 후 새 트리거가 시작되면 갱신된다.
     */
    private val _resultsContext = MutableStateFlow(_sourceLanguage.value to "")
    val resultsContext: StateFlow<Pair<AppLanguage, String>> = _resultsContext.asStateFlow()

    val ttsState: StateFlow<TtsState> = ttsService.state

    private val _events = MutableSharedFlow<PlaygroundEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<PlaygroundEvent> = _events.asSharedFlow()

    /**
     * 동일 (source, target, text) 트리플의 Success 결과 메모이제이션 (AC-T02-14, E06).
     * 입력을 지웠다 다시 쳐도 즉시 Success 로 복귀할 수 있게 해 사용자 인지를 줄인다.
     */
    private val memo = mutableMapOf<Triple<AppLanguage, AppLanguage, String>, String>()

    /** 카드별 Translation Job. 새 디바운스가 트리거되면 직전 Job 들을 모두 취소한다. */
    private val translationJobs = mutableMapOf<AppLanguage, Job>()

    /**
     * 타이핑 디바운스 채널 (500 ms). source 와 text 둘 중 하나만 바뀌어도 트리거되는데,
     * source 변경 즉시 reset() 에서 results 가 모두 Idle 로 가므로 이 채널은 텍스트 변경에만 반응시켜도 충분.
     * source 변경은 별도 짧은 디바운스(150 ms — AC-T02-15) 로 처리한다.
     */
    private val typingTrigger = MutableStateFlow(_inputText.value)
    private val sourceChangeTrigger = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    init {
        // AC-T02-05 — 가장 최근 일기의 sourceLanguage 를 초기값으로 채택. Flow 1회 조회.
        viewModelScope.launch {
            val recent = runCatching {
                repository.observeAll().firstOrNull()?.firstOrNull()?.sourceLanguage
            }.getOrNull()
            if (recent != null && recent != _sourceLanguage.value) {
                _sourceLanguage.value = recent
                _results.value = initialResults(recent)
            }
        }

        // 타이핑 → 500 ms 디바운스 (AC-T02-07).
        viewModelScope.launch {
            typingTrigger
                .debounce(500)
                .distinctUntilChanged()
                .collect { text ->
                    triggerTranslate(text.trim(), _sourceLanguage.value)
                }
        }

        // 원문 언어 변경 → 150 ms 디바운스 (AC-T02-15).
        viewModelScope.launch {
            sourceChangeTrigger
                .debounce(150)
                .collect {
                    triggerTranslate(_inputText.value.trim(), _sourceLanguage.value)
                }
        }
    }

    fun updateInput(value: String) {
        // AC-T02-04 — 200자 차단. take(200) 으로 트림.
        val clipped = if (value.length > MAX_INPUT_LEN) value.take(MAX_INPUT_LEN) else value
        if (clipped == _inputText.value) return
        _inputText.value = clipped
        if (BuildConfig.DEBUG) Log.d("QA", "Playground:inputChanged:len=${clipped.length}")

        // AC-T02-12 — 입력 변경 시 기존 Success/Error 카드를 outdated 표시할 수 있게 alpha 처리는
        // UI 측 결정. ViewModel 은 결과 자체는 다음 디바운스 결과로 갱신될 때까지 그대로 유지한다.
        // 빈 문자열로 변경된 경우는 즉시 Idle 로 복귀하고 진행 Job 을 취소 (AC-T02-13).
        if (clipped.trim().isEmpty()) {
            cancelAllJobs()
            _results.value = initialResults(_sourceLanguage.value)
            _resultsContext.value = _sourceLanguage.value to ""
        }
        typingTrigger.value = clipped
    }

    fun updateSourceLanguage(language: AppLanguage) {
        // AC-T02-E07 — 동일 언어 재선택은 no-op.
        if (language == _sourceLanguage.value) return
        _sourceLanguage.value = language
        cancelAllJobs()
        _results.value = initialResults(language)
        _resultsContext.value = language to ""
        // 빈 입력이면 아무것도 트리거하지 않는다 (AC-T02-15 의 (3) 가드).
        if (_inputText.value.trim().isNotEmpty()) {
            sourceChangeTrigger.tryEmit(Unit)
        }
    }

    fun clear() {
        // AC-T02-17 — 입력만 비우고 sourceLanguage 는 유지. 진행 중 Job 취소.
        if (_inputText.value.isEmpty()) return
        cancelAllJobs()
        _inputText.value = ""
        typingTrigger.value = ""
        _results.value = initialResults(_sourceLanguage.value)
        _resultsContext.value = _sourceLanguage.value to ""
    }

    fun retry(target: AppLanguage) {
        // AC-T02-10 — 단일 언어 재호출. 디바운스 재시작 아님.
        val source = _sourceLanguage.value
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        if (target == source) return
        translationJobs[target]?.cancel()
        translationJobs[target] = launchTranslateOne(text, source, target)
    }

    fun saveWordCard() {
        val source = _sourceLanguage.value
        val word = _inputText.value.trim()
        if (word.isEmpty()) return
        val successByLang = _results.value
            .mapNotNull { (lang, res) -> if (res is PlaygroundResult.Success) lang to res.text else null }
            .toMap()
        if (successByLang.isEmpty()) return
        val now = System.currentTimeMillis()
        val card = WordCard(
            id = UUID.randomUUID().toString(),
            sourceEntryId = null,
            word = word,
            sourceLanguage = source,
            translations = successByLang,
            exampleSentences = emptyMap(),
            masteryLevel = 0,
            nextReviewAt = null,
            reviewCount = 0,
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
            isTranslationEdited = false,
        )
        viewModelScope.launch {
            runCatching { repository.addWordCard(card) }
                .onSuccess {
                    if (BuildConfig.DEBUG) {
                        val firstN = word.take(8)
                        val codes = successByLang.keys.joinToString(",") { it.code }
                        Log.d("QA", "Playground:cardSaved:word=$firstN:langs=$codes")
                    }
                    _events.tryEmit(PlaygroundEvent.WordCardSaved)
                }
                .onFailure {
                    _events.tryEmit(PlaygroundEvent.WordCardSaveFailed)
                }
        }
    }

    fun toggleSpeak(target: AppLanguage) {
        val res = _results.value[target] as? PlaygroundResult.Success ?: return
        val current = ttsService.state.value
        // AC-T02-29 — 단일 채널: 다른 카드 재생 중이면 정지 후 새 카드 시작.
        // 동일 카드 재생 중이면 정지.
        if (current is TtsState.Playing) {
            ttsService.stop()
            if (current.language == target) {
                if (BuildConfig.DEBUG) Log.d("QA", "Playground:ttsEnd:lang=${target.code}")
                return
            }
        }
        ttsService.speak(res.text, target)
        // speak() 내부에서 Locale 미지원이면 TtsState.Error 가 즉시 방출된다.
        val newState = ttsService.state.value
        if (newState is TtsState.Error) {
            _events.tryEmit(PlaygroundEvent.TtsUnavailable)
        } else if (BuildConfig.DEBUG) {
            Log.d("QA", "Playground:ttsStart:lang=${target.code}")
        }
    }

    private fun triggerTranslate(text: String, source: AppLanguage) {
        if (text.isEmpty()) {
            cancelAllJobs()
            _results.value = initialResults(source)
            _resultsContext.value = source to ""
            return
        }
        val targets = AppLanguage.entries.filter { it != source }
        if (BuildConfig.DEBUG) {
            val targetCodes = targets.joinToString(",") { it.code }
            Log.d("QA", "Playground:translateTrigger:source=${source.code}:targets=$targetCodes")
        }
        // AC-T02-28 — 카드가 Loading 으로 진입하므로 헬퍼 영구 숨김.
        if (_showFirstUseHint.value) _showFirstUseHint.value = false
        // 직전 Job 모두 취소 — 입력이 바뀌었으므로 stale 결과를 막는다.
        cancelAllJobs()
        // outdated 판정용 컨텍스트 갱신 — 카드들이 이 (source, text) 에 대한 결과를 보여주게 됨.
        _resultsContext.value = source to text
        targets.forEach { target ->
            // AC-T02-14 / E06 — 메모 히트면 호출 생략하고 즉시 Success 유지.
            val cached = memo[Triple(source, target, text)]
            if (cached != null) {
                _results.update { it + (target to PlaygroundResult.Success(cached)) }
                return@forEach
            }
            translationJobs[target] = launchTranslateOne(text, source, target)
        }
    }

    private fun launchTranslateOne(
        text: String,
        source: AppLanguage,
        target: AppLanguage,
    ): Job = viewModelScope.launch {
        _results.update { it + (target to PlaygroundResult.Loading) }
        val started = System.currentTimeMillis()
        val result = translationEngine.translate(text, source, target)
        val elapsed = System.currentTimeMillis() - started
        result
            .onSuccess { translated ->
                memo[Triple(source, target, text)] = translated
                _results.update { it + (target to PlaygroundResult.Success(translated)) }
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "QA",
                        "Playground:translateResult:target=${target.code}:status=success:elapsedMs=$elapsed",
                    )
                }
            }
            .onFailure {
                _results.update { it + (target to PlaygroundResult.Error(GENERIC_ERROR_KEY)) }
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "QA",
                        "Playground:translateResult:target=${target.code}:status=error:elapsedMs=$elapsed",
                    )
                }
            }
    }

    private fun cancelAllJobs() {
        translationJobs.values.forEach { it.cancel() }
        translationJobs.clear()
    }

    /**
     * AC-T02-05 의 (2)·(3) — 시스템 Locale 이 4개 언어 중 하나면 그것, 아니면 KOREAN.
     * (1) 가장 최근 저장 일기의 sourceLanguage 는 init {} 에서 비동기로 갱신.
     */
    private fun defaultSourceLanguage(): AppLanguage =
        AppLanguage.fromCode(Locale.getDefault().language) ?: AppLanguage.KOREAN

    private fun initialResults(source: AppLanguage): Map<AppLanguage, PlaygroundResult> =
        AppLanguage.entries.filter { it != source }.associateWith { PlaygroundResult.Idle }

    companion object {
        const val MAX_INPUT_LEN = 200
        const val WARN_INPUT_LEN = 180

        /**
         * UI 가 [PlaygroundResult.Error.message] 를 직접 사용하지 않고 항상 일반 문구
         * (R.string.translate_playground_error_generic) 로 노출하기 위한 마커.
         * (AC-T02-E01: 사용자에게 기술 세부 노출 금지.)
         */
        const val GENERIC_ERROR_KEY = "generic"
    }
}
