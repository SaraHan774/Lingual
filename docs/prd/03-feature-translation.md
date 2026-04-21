# 03 — Feature: Translation (번역)

[← 목차로](../../PRD.md) · **status: shipped (엔진) · in-progress (Translate 탭 재설계 T-02)**

> **변경 이력**
> - 초기 shipped: ML Kit 온디바이스 번역 엔진, PENDING/SUCCESS/ERROR 상태 머신.
> - 2026-04-19 `/ship translate-browse-viewmodel` — Translate 탭 전용 `TranslateBrowseViewModel` 분리 (T-01).
> - 2026-04-21 `/ship translate-tab-redesign` iter 1 — **Translate 탭 전면 재설계 (T-02)**. 기존 "일기 목록 + 원문 언어 필터" 가 `DiaryListScreen` 과 중복/모호하므로 제거. **실시간 프리뷰 번역기 (Translate Playground)** 로 대체. 학습자가 일기 저장 전후와 무관하게 짧은 문장/구/단어를 타이핑하면 나머지 3개 언어로 즉시 번역해 보여 주고, 결과를 단어 카드로 한 번에 저장할 수 있게 한다. T-01 AC 는 `Superseded` 로 표기하되 구현체는 재설계 과정에서 안전하게 철거한다.
> - 2026-04-21 `/ship translate-tab-redesign` iter 2 — prd-review NEEDS_SPEC 반영. (1) 카드별 [+] 버튼 → 입력 영역 아래 **풀폭 단일 저장 버튼** 으로 모델 통일 (AC-T02-19/20/21/24, UX 다이어그램, "단어 카드로 저장 액션" 섹션). (2) 신규 i18n 키 `translate_playground_save_button_label`("단어 카드로 저장 (N개 언어 포함)") 추가. (3) 권고 반영: AC-T02-04 180자 이상 카운터 색상 error, AC-T02-15 원문 언어 변경 150 ms 디바운스, AC-T02-21 스낵바 "FlashCard 보기" Action 버튼, 신규 AC-T02-28 Empty State 모델 다운로드 안내, 신규 AC-T02-29 TTS 단일 채널 정책. (4) `08-roadmap.md` P1 — "Diary 탭 원문 언어 필터 회수" 1줄 추가.

## Goal

**두 가지 층위의 번역 가치를 제공한다.**

1. **일기 번역 (shipped, 배경 파이프라인)** — 일기 원문을 4개 언어 중 나머지 3개 언어로 **오프라인·비동기·실패 허용** 방식으로 번역한다. 번역 결과는 언어별 독립적 상태 머신을 가지며, 일부 언어가 실패해도 다른 언어 결과를 읽을 수 있다. `DiaryDetailScreen` 에서 탭으로 소비.
2. **Translate Playground (T-02, in-progress)** — 일기와 무관하게 짧은 텍스트(단어/구/문장)를 **타이핑 중 즉시 3개 언어로 프리뷰** 번역해 보여 주고, 결과를 **단어 카드로 저장** 하거나 **TTS 로 발음** 을 들을 수 있는 학습 유틸리티. 일기 작성 흐름을 끊지 않고도 빠르게 표현을 시험할 수 있게 한다.

## Non-Goals

- 클라우드 번역(Google Translate API, DeepL 등) — 프라이버시 원칙 위반.
- 문장 단위 정렬(alignment) 표시 — v0.1은 전체 문단 단위 결과만.
- 사용자 정의 용어집/고유명사 처리 — 엔진에 맡김.
- **Translate Playground 히스토리 영속화 — v0.1 미포함.** Playground 결과는 ViewModel 생명주기 동안만 유지되고, 앱 재시작 시 사라진다. 사용자가 남기고 싶으면 "단어 카드로 저장" 액션을 명시적으로 취해야 한다. (Phase 2 — `08-roadmap.md` 항목 후보.)
- **Playground 에서의 긴 문단 번역 — 스코프 외.** Playground 입력은 최대 200자로 제한하며, 일기 수준의 문단은 Diary 탭에서 작성·저장해야 한다. (이유: 일기 번역과의 경계를 명확히 해 중복을 제거.)
- **문장 번역 퀴즈·학습 세션 — Phase 2.** `08-roadmap.md` P1 후보로 보류.

## Supported Language Pairs

`AppLanguage` enum이 단일 출처 (`domain/model/AppLanguage.kt`).

| Code | 표시명 | Locale | 용도 |
|------|--------|--------|------|
| `ko` | 한국어 | `Locale.KOREAN` | source / target |
| `en` | English | `Locale.US` | source / target |
| `ja` | 日本語 | `Locale.JAPANESE` | source / target |
| `zh` | 中文 | `Locale.SIMPLIFIED_CHINESE` | source / target (간체 기준) |

총 번역 방향은 4 × 3 = 12쌍. 각 쌍은 독립 ML Kit `Translator` 인스턴스로 처리하고 엔진 내부 캐시로 재사용한다.

## 재설계 배경 (T-02)

**문제**: 기존 `TranslateBrowseScreen` 은 `DiaryListScreen` 과 기능이 중복된다.

- 두 화면 모두 `repository.observeAllWithTranslations()` 를 구독해 같은 일기 카드 목록을 그린다.
- 차별 포인트는 "원문 언어 FilterChip" 하나뿐인데, 이는 Diary 탭에 흡수돼도 기능 손실이 없다.
- 번역 결과 자체는 두 화면 어디에서도 직접 보이지 않는다 — 사용자가 번역을 보려면 항상 `DiaryDetailScreen` 까지 이동해야 한다.
- "번역" 이라는 탭 이름이 **기대치와 실제 제공 가치 사이의 간극** 을 만든다: 사용자는 "번역 도구" 를 기대하지만 실제로는 "필터 있는 일기 목록" 을 만난다.

**해법**: Translate 탭을 **번역 유틸리티** 로 재정의한다. 일기와 독립된 짧은 텍스트 번역을 지원하고, 타겟 페르소나(01-overview: "민지 — 사전을 찾으며 쓰다 흐름이 끊긴다") 의 실제 pain point 를 정면으로 해결한다. 단어 카드 저장을 곁들여 FlashCard 탭과도 자연스럽게 연결된다.

**제거 대상**: 기존 `TranslateBrowseScreen` / `TranslateBrowseViewModel` 및 관련 문자열 리소스(`translate_browse_*`).

## UX: Translate Playground (T-02 신규)

`TranslatePlaygroundScreen` — 상단에 원문 언어 선택, 가운데 원문 입력 TextField, 그 아래에 3개 대상 언어 카드가 세로로 나열되는 단일 화면 구조.

### 화면 레이아웃

```
┌──────────────────────────────────────────┐
│ TopAppBar: "번역 연습" / "Translate"     │
├──────────────────────────────────────────┤
│ [원문 언어: 한국어 ▾]   [지우기 ✕]       │  ← 원문 언어 드롭다운 + 입력 초기화
├──────────────────────────────────────────┤
│ ┌────────────────────────────────────┐   │
│ │ 번역할 단어·구·문장을 입력하세요     │   │  ← OutlinedTextField, maxLines=4
│ │                          (0 / 200) │   │  ← 글자수 카운터 오른쪽 하단
│ └────────────────────────────────────┘   │
├──────────────────────────────────────────┤
│ ┌──────────────────────────────────────┐ │
│ │ [단어 카드로 저장 (3개 언어 포함)]    │ │  ← 풀폭 단일 저장 버튼 (Filled Button)
│ └──────────────────────────────────────┘ │
├──────────────────────────────────────────┤
│ ┌─ English ───────────────────── [♪] ─┐ │  ← 대상 언어 카드 × 3
│ │ Today I had a quiet evening.        │ │  ← 번역 결과 / 스피너 / 에러 문구
│ │                                     │ │
│ └─────────────────────────────────────┘ │
│ ┌─ 日本語 ─────────────────────── [♪] ─┐ │
│ │ 今日は静かな夜を過ごした。           │ │
│ └─────────────────────────────────────┘ │
│ ┌─ 中文 ────────────────────────── [♪] ─┐│
│ │ [스피너] 번역 중…                    │ │
│ └─────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

- **원문 언어 드롭다운**: 4개 언어 중 하나. 기본값은 최근 사용한 일기의 `sourceLanguage`, 없으면 시스템 로케일에 대응하는 `AppLanguage`, 둘 다 없으면 `KOREAN`.
- **지우기 버튼**: 입력 TextField 를 비우고 3개 결과 카드 상태를 `Idle` 로 리셋.
- **풀폭 저장 버튼**: 입력 영역과 결과 카드 목록 사이에 위치한 단일 Filled Button. 라벨은 `R.string.translate_playground_save_button_label` 포맷 문자열로 "단어 카드로 저장 (N개 언어 포함)" / "Save as word card (N languages)". `N` 은 현재 `Success` 상태인 대상 언어 수. 활성화 조건: `N >= 1`. 비활성화: 모든 카드가 `Idle`/`Loading`/`Error` 일 때 alpha 0.38. 카드별 [+] 버튼은 v0.1 에 존재하지 않는다.
- **대상 언어 카드 3개**: 원문 언어를 제외한 3개 언어. 카드 내부에 언어 이름, 번역 결과 텍스트, **TTS 재생 버튼(♪) 만** 카드 우측 상단에 배치. 카드 자체에 저장 버튼은 없다.

### 번역 트리거 정책 — 디바운스 타이핑

- 사용자가 타이핑을 멈추고 **500ms** 가 지나면 자동으로 3개 언어 번역을 병렬 호출한다.
- 같은 텍스트·같은 원문 언어에 대해서는 캐시 히트(이미 성공한 결과가 있으면) 시 재호출하지 않는다.
- 입력 중 글자수가 바뀌는 순간 기존 결과 카드는 "outdated" 마커를 위해 **흐리게(alpha 0.5)** 표시한다 — 번역이 다시 완료되면 원래 불투명도로 복귀.
- 빈 입력(`trim().isEmpty()`) 일 때 3개 카드는 `Idle` — 플레이스홀더 문구("결과가 여기에 표시됩니다")만 표시.

### Playground 상태 머신 (카드당)

```
[Idle (입력 전/비어 있음)]
     │ 사용자가 타이핑 + 500ms 경과
     ▼
[Loading (스피너 + "번역 중…")]
     │ ML Kit translate() 성공
     ▼
[Success (번역 텍스트 + ♪ + +)]

[Loading] ──실패──▶ [Error (실패 메시지 + "다시 시도" 버튼)]
```

- 각 카드는 서로 독립. 한 언어가 ERROR 여도 다른 언어는 SUCCESS 로 갈 수 있다 (일기 번역 파이프라인과 동일한 실패 허용 원칙).
- `Error` 카드의 "다시 시도" 버튼은 해당 언어만 재호출 (디바운스 재시작 아님).

### 단어 카드로 저장 액션 (풀폭 버튼)

- 풀폭 저장 버튼은 **결과 카드 목록 위쪽**(입력 영역과 카드 목록 사이) 에 단 하나만 존재한다. 카드별 개별 [+] 버튼은 **v0.1 에 없다** — 사용자가 "어느 언어 카드를 저장한다" 가 아니라 "이 단어를 (현재 번역된 N개 언어와 함께) 한 장의 카드로 저장한다" 는 단일 멘탈 모델을 제공한다.
- 버튼 라벨은 `R.string.translate_playground_save_button_label` 포맷 문자열을 사용해 현재 `Success` 상태인 대상 언어 개수 `N` 을 노출한다.
  - 한국어: `단어 카드로 저장 (%1$d개 언어 포함)`
  - English: `Save as word card (%1$d languages)`
- 활성화 조건: 최소 1개 대상 언어가 `Success` 상태. 비활성: 모든 대상 언어가 `Idle`/`Loading`/`Error` 일 때 (alpha 0.38, `enabled=false`).
- 버튼 탭 시:
  1. 현재 원문 텍스트(`inputText.trim()`) = `word`, 현재 원문 언어 = `sourceLanguage`.
  2. `translations` Map 은 **현재 `Success` 상태인 모든 대상 언어** 의 번역 결과로 채운다. `Loading`/`Error`/`Idle` 인 언어는 포함하지 않는다 (빈 값 저장 방지).
  3. `repository.addWordCard(card)` 호출 → FlashCard 탭 목록 즉시 갱신.
  4. 성공 시 스낵바: "단어 카드가 저장되었습니다." / "Word card saved." — **Action 버튼**으로 "FlashCard 보기" / "View" 를 노출하고, 탭 시 `Screen.FlashCard` 로 이동한다 (현재 화면은 pop 하지 않음 — 사용자가 뒤로 돌아오기 쉽게).
  5. 실패 시 에러 스낵바: "단어 카드 저장에 실패했습니다. 다시 시도해 주세요." / "Failed to save word card. Please try again."
- 저장 후 `inputText`·`results`·`sourceLanguage` 는 변경되지 않는다 (사용자가 연속으로 시험·저장 가능).
- 동일 단어 중복 저장은 **허용** (FlashCard PRD 결정 사항 준수 — 별도 카드로 추가).
- 단어 카드 저장 시 `sourceEntryId = null`. (Playground 는 특정 일기와 연결되지 않음.)

### TTS 재생 액션 (카드의 [♪] 버튼)

- [♪] 버튼은 카드 상태가 `Success` 일 때만 활성화된다.
- [♪] 탭 시 기존 `TtsService` 를 통해 해당 언어 번역 텍스트를 재생. 재생 중 다시 누르면 정지.
- 기기에 해당 Locale TTS 데이터가 없으면 `TtsState.Error` 를 스낵바로 표시: "해당 언어 TTS 데이터가 설치되어 있지 않습니다." (05-feature-tts.md 의 오류 안내 규칙 준수.)

### 원문 언어 == 대상 언어 방지

- 사용자가 원문 언어 드롭다운을 바꾸면 대상 언어 카드 3개도 자동 재계산(`AppLanguage.entries - source`). 결과 카드 상태는 모두 `Idle` 로 리셋하고 기존 번역 결과는 폐기.

## UX: Translate Tab 진입점

바텀 탭 네비게이션의 "번역" 탭은 유지된다 (`Screen.Translate`, 경로 `"translate"`). 아이콘은 `Icons.Filled.Translate` 그대로. 탭 탭 시 `TranslatePlaygroundScreen` 이 열린다.

기존 `TranslateBrowseScreen` 은 완전 제거한다 — 라우트는 유지하되 composable 바인딩을 새 화면으로 교체.

## Data Model

**Translate Playground 는 영속 데이터를 갖지 않는다.** 모든 상태는 `TranslatePlaygroundViewModel` 내부 `StateFlow` 로만 관리되며, ViewModel 이 소멸하면 입력과 결과가 함께 사라진다. 사용자가 명시적으로 풀폭 "단어 카드로 저장" 버튼을 탭했을 때만 `WordCardEntity` 가 Room 에 기록된다.

| ViewModel State | 타입 | 초기값 |
|------|------|--------|
| `sourceLanguage` | `StateFlow<AppLanguage>` | 기본값 계산 결과 (아래 참조) |
| `inputText` | `StateFlow<String>` | `""` |
| `results` | `StateFlow<Map<AppLanguage, PlaygroundResult>>` | 3개 대상 언어 모두 `PlaygroundResult.Idle` |

```kotlin
sealed class PlaygroundResult {
    data object Idle : PlaygroundResult()
    data object Loading : PlaygroundResult()
    data class Success(val text: String) : PlaygroundResult()
    data class Error(val message: String) : PlaygroundResult()
}
```

### 원문 언어 초기값 결정

```
기본값 우선순위:
1. 최근에 저장된 DiaryEntry.sourceLanguage (SharedPreferences 아님 — Flow로 1회 조회)
2. 시스템 로케일 → AppLanguage.fromCode(Locale.getDefault().language)
3. AppLanguage.KOREAN
```

## 상태 머신 (일기 번역 — 기존 shipped)

```
 저장 시 생성    모델 로드/번역 중    번역 성공              번역 실패
 ─────────────▶ [PENDING] ────────▶ [SUCCESS]     또는    [ERROR + errorMessage]
                                        ▲                         │
                                        └────── 사용자 재시도 ◀───┘
```

| 상태 | DB 값 | UI | 허용되는 다음 상태 |
|------|-------|----|-------------------|
| PENDING | `translation_status = "pending"` | 스피너 | SUCCESS, ERROR |
| SUCCESS | `"success"` | 번역 텍스트 + TTS 버튼 | (재저장 시) PENDING |
| ERROR | `"error"` + `errorMessage` | 에러 문구 + "재시도" 버튼 | PENDING |

## 엔진 계층

- **인터페이스**: `data/translation/TranslationEngine.kt`
- **구현체**: `MlKitTranslationEngine` (`@Singleton`)
  - `(source, target)` 쌍별 `Translator` 인스턴스 캐시.
  - `downloadModelIfNeeded()`를 `translate()` 내부에서 호출 → 최초 호출 시 ~30MB 다운로드 발생.
  - ML Kit의 `Task` 콜백을 `suspendCancellableCoroutine`으로 코루틴화.
  - 영속화 시 `modelVersion = "mlkit-v1"`을 기록해 향후 엔진 교체 시 마이그레이션 기준으로 사용.

엔진 교체가 필요하면 `TranslationEngine`의 인터페이스만 지키고 `RepositoryModule`의 `@Binds`를 교체한다. 기존 SUCCESS 데이터는 `modelVersion` 값으로 구분해 재번역 대상을 결정한다.

Translate Playground 는 **동일한 `TranslationEngine` 인터페이스** 를 재사용한다. 별도 엔진·캐시를 만들지 않는다.

## Acceptance Criteria

### 번역 엔진 (shipped — 변경 없음)
- [x] 일기 저장 직후 `(diaryEntryId, targetLanguage)` 3쌍의 PENDING 레코드가 <500ms 내에 생성된다.
- [x] 동일 언어쌍을 두 번째로 번역할 때는 모델 다운로드 없이 수행된다 (인스턴스 캐시 히트).
- [x] 네트워크가 끊긴 상태에서 이미 다운로드된 언어쌍은 정상 번역된다.
- [x] 네트워크가 끊긴 상태에서 **첫 번째** 호출이면 ERROR 상태로 기록되고 사용자에게 재시도 옵션이 제공된다.
- [x] `modelVersion` 필드가 모든 SUCCESS 레코드에 기록된다.
- [ ] 번역 ERROR 비율이 주간 2% 미만. (메트릭 수집 수단은 미정 — Open Questions)

### TranslateBrowseViewModel 분리 (T-01) — **Superseded by T-02**

> **Superseded (2026-04-21, translate-tab-redesign)**: T-01 이 도입한 `TranslateBrowseScreen`/`TranslateBrowseViewModel` 는 T-02 재설계에서 전체 제거된다. 아래 AC 는 역사적 기록을 위해 남기되, 신규 구현 기준이 아니다. T-02 의 AC-T02-R01 이 제거 방식을 명시한다.

- [x] ~~`TranslateBrowseScreen`은 `DiaryListViewModel`을 주입받지 않는다. 전용 `TranslateBrowseViewModel`만 사용한다.~~ (Superseded — 화면 자체가 제거됨.)
- [x] ~~`TranslateBrowseViewModel`은 `@HiltViewModel`로 선언되어 Hilt가 주입한다.~~ (Superseded.)
- [x] ~~언어 필터(`selectedLanguage`)의 초기값은 `null`(전체)이다.~~ (Superseded.)
- [x] ~~FilterChip 선택 시 `selectLanguage()` 호출 → `filteredEntries` 재계산.~~ (Superseded.)
- [x] ~~탭 이동 후 복귀 시 선택된 언어 필터 유지.~~ (Superseded.)

### Translate Playground (T-02, in-progress)

#### 진입·레이아웃

- [ ] **AC-T02-01**: 바텀 탭 "번역" 을 탭하면 `TranslatePlaygroundScreen` 이 열린다. `Screen.Translate` 의 라우트(`"translate"`) 와 아이콘(`Icons.Filled.Translate`) 은 유지한다.
- [ ] **AC-T02-02**: 화면 상단부터 순서대로 (1) TopAppBar (title = `R.string.translate_playground_title`), (2) 원문 언어 드롭다운 + 지우기 버튼 Row, (3) 원문 입력 `OutlinedTextField` (placeholder = `R.string.translate_playground_input_hint`, 우측 하단 글자수 카운터 `current/200`), (4) 3개 대상 언어 결과 카드 세로 나열 을 구성한다.
- [ ] **AC-T02-03**: 원문 언어 드롭다운에는 `AppLanguage.entries` 4개가 모두 선택지로 표시되고, 레이블은 `AppLanguage.displayName` 을 그대로 사용한다 (KO="한국어", EN="English", JA="日本語", ZH="中文"). 하드코딩된 언어 코드/문자열 금지.
- [ ] **AC-T02-04**: 원문 입력 TextField 의 최대 글자수는 **200**. 글자수 카운터는 `current/200` 형태로 `R.string.translate_playground_char_counter` 포맷 문자열로 표시. 초과 입력은 물리적으로 차단(입력이 들어가지 않음 — `onValueChange` 에서 `newText.take(200)` 로 트림). 글자수가 **180 자 이상** 이면 카운터 텍스트 색을 `MaterialTheme.colorScheme.error` 로 전환해 한도 임박을 시각적으로 경고하고, 180 자 미만이면 기본 색(`onSurfaceVariant`) 으로 복귀한다.

#### 원문 언어 초기값

- [ ] **AC-T02-05**: Playground 최초 진입 시 `sourceLanguage` 는 다음 우선순위로 결정된다: (1) 가장 최근에 저장된 `DiaryEntry.sourceLanguage` — `DiaryRepository` 에 1회성 suspend 조회 또는 Flow `.first()` 로 획득, (2) `AppLanguage.fromCode(Locale.getDefault().language)` 가 non-null 이면 그 값, (3) `AppLanguage.KOREAN`. 조회 중에는 드롭다운이 빈 상태가 아닌 fallback(KOREAN) 으로 시작하고, 조회 완료 시 값이 갱신된다.
- [ ] **AC-T02-06**: 탭을 다른 탭으로 이동했다가 Translate 탭으로 복귀해도 사용자가 선택한 `sourceLanguage`, `inputText`, `results` 는 유지된다 (ViewModel 이 Activity scope 에 묶여 있으므로). 단, 앱 프로세스가 종료되면 상태는 초기화된다 (Non-Goals 에 따라 영속화 없음).

#### 타이핑 → 자동 번역 (디바운스)

- [ ] **AC-T02-07**: 사용자가 원문 입력 TextField 에 타이핑한 후 **500ms** 동안 추가 입력이 없으면, 현재 `inputText.trim()` 이 빈 문자열이 아닐 때 원문 언어를 제외한 3개 대상 언어에 대해 `TranslationEngine.translate()` 를 **병렬로** 호출한다. 구현: `snapshotFlow { inputText.value }.debounce(500).collect { ... }` 또는 `StateFlow.debounce(500)` 동등 패턴.
- [ ] **AC-T02-08**: 번역 호출 시작 순간 해당 언어 카드 상태는 `PlaygroundResult.Loading` 으로 전환되고, UI 는 카드 내부에 `CircularProgressIndicator` + "번역 중…" 텍스트를 표시한다.
- [ ] **AC-T02-09**: 번역 성공 시 해당 언어 카드 상태는 `PlaygroundResult.Success(text)` 로 전환되고, 번역 텍스트가 카드에 표시된다. 카드의 [♪] TTS 버튼이 활성화되고, 화면 풀폭 저장 버튼의 `successCount` 가 1 증가해 라벨·활성 상태가 갱신된다.
- [ ] **AC-T02-10**: 번역 실패 시 해당 언어 카드 상태는 `PlaygroundResult.Error(message)` 로 전환되고, 카드 내부에 실패 문구 + "다시 시도" 버튼 (`R.string.translate_playground_retry`) 이 표시된다. "다시 시도" 버튼 탭 시 해당 언어만 재호출된다 (`inputText` 재타이핑 없이).
- [ ] **AC-T02-11**: 3개 대상 언어는 서로 독립적이다 — 한 언어의 `Error` 가 다른 언어의 `Success` 전환을 막지 않는다. 각 언어는 자기 자신의 `Job` 을 갖는다.
- [ ] **AC-T02-12**: 사용자가 결과가 표시된 상태에서 원문을 수정하면, 기존 결과 카드들은 즉시 **alpha 0.5** 로 흐리게 표시된다 ("outdated" 시각 피드백). 다음 디바운스 경계(500ms)에서 번역이 재개되어 `Loading` → `Success/Error` 로 전환될 때 alpha 가 1.0 으로 복귀한다.
- [ ] **AC-T02-13**: 원문 입력이 `trim().isEmpty()` 이면 3개 카드는 `Idle` 상태로 전환되고 플레이스홀더 문구(`R.string.translate_playground_placeholder`) 가 표시된다. 이미 진행 중이던 번역 Job 은 취소된다.
- [ ] **AC-T02-14**: 동일 `(inputText, sourceLanguage, targetLanguage)` 조합에 대해 직전 결과가 `Success` 로 캐시되어 있으면, 디바운스 재트리거 시 ML Kit 호출을 생략하고 기존 `Success` 상태를 유지한다. (사용자가 값을 바꿨다 돌린 경우를 위한 간단 메모이제이션 — ViewModel 내부 `Map<Triple<lang, lang, text>, String>` 한 벌.)

#### 원문 언어 변경

- [ ] **AC-T02-15**: 원문 언어 드롭다운에서 다른 언어를 선택하면 (1) `sourceLanguage` 가 즉시 갱신되고, (2) 기존 3개 대상 언어 카드의 결과가 모두 `Idle` 로 리셋되며, (3) 현재 `inputText` 가 비어 있지 않으면 짧은 **150 ms 디바운스** 후 새 대상 언어 3개에 대해 번역을 트리거한다. 사용자가 드롭다운을 빠르게 여러 번 바꾸는 경우 ML Kit 호출 폭증을 방지하기 위함이며, 타이핑 디바운스(500 ms) 와 달리 짧게 잡아 체감 지연을 최소화한다.
- [ ] **AC-T02-16**: 원문 언어가 `X` 이면 대상 언어 카드는 `AppLanguage.entries - X` 3개. 예: X=KO 이면 EN/JA/ZH 카드, X=EN 이면 KO/JA/ZH 카드. 카드 순서는 `AppLanguage.entries` 의 선언 순서 (KO, EN, JA, ZH) 에서 원문 언어만 건너뛴 순서를 따른다.

#### 지우기 버튼

- [ ] **AC-T02-17**: 상단 "지우기 ✕" (`R.string.translate_playground_clear_cd`) 버튼 탭 시 `inputText = ""`, 3개 카드 상태 모두 `Idle` 로 리셋. 진행 중 번역 Job 은 취소. 드롭다운의 `sourceLanguage` 는 유지.
- [ ] **AC-T02-18**: "지우기" 버튼은 `inputText.isNotEmpty()` 일 때만 활성화(enabled=true)되고, 빈 상태에서는 비활성(enabled=false, alpha 0.38).

#### 단어 카드 저장 (풀폭 버튼)

- [ ] **AC-T02-19**: 각 결과 카드 우측 상단에는 **[♪] TTS 버튼만** 표시된다. 카드별 [+] 저장 버튼은 v0.1 에 존재하지 않는다 (사용자 멘탈 모델 일관성: "단어 카드 저장" 은 카드 단위가 아닌 입력 단위 액션).
- [ ] **AC-T02-20**: 입력 영역과 결과 카드 목록 사이에 **풀폭 Filled Button** (`Modifier.fillMaxWidth().padding(horizontal = 16.dp)`, contentDescription = `R.string.translate_playground_save_button_label`) 한 개를 배치한다. 라벨은 `stringResource(R.string.translate_playground_save_button_label, successCount)` — `successCount` 는 현재 `results` 중 `PlaygroundResult.Success` 인 대상 언어 수. 활성화 조건: `successCount >= 1`. 비활성: `successCount == 0` (모든 카드가 `Idle`/`Loading`/`Error`) 일 때 `enabled=false`, alpha 0.38.
- [ ] **AC-T02-21**: 풀폭 저장 버튼 탭 시 다음 `WordCard` 가 `repository.addWordCard()` 로 저장된다:
  - `id` = 신규 UUID
  - `sourceEntryId` = `null`
  - `word` = 현재 `inputText.trim()`
  - `sourceLanguage` = 현재 `sourceLanguage`
  - `translations` = **`Success` 상태인 모든 대상 언어의 번역 텍스트를 담은 Map**. `Loading`/`Error`/`Idle` 상태의 언어는 `translations` 에 포함하지 않는다(빈 값으로 저장 방지).
  - `exampleSentences` = `null`
  - `masteryLevel` = `0`
  - `nextReviewAt` = `null`
  - `reviewCount` = `0`
  - `isFavorite` = `false`
  - `createdAt` = `System.currentTimeMillis()`
  - `updatedAt` = `System.currentTimeMillis()`
  - `isTranslationEdited` = `false`

  저장 성공 시 스낵바 (`R.string.translate_playground_save_success` = "단어 카드가 저장되었습니다." / "Word card saved.") 가 표시되고, **Action 버튼** "FlashCard 보기" / "View" (`R.string.translate_playground_save_success_action`) 가 함께 노출된다. Action 탭 시 `Screen.FlashCard` 로 navigate 하되 현재 Playground 화면은 백스택에 유지한다 (사용자가 곧장 돌아와 다음 단어 시험 가능). 화면은 그대로 유지된다(입력·결과 리셋하지 않음).
- [ ] **AC-T02-22**: 저장 실패 시 에러 스낵바 (`R.string.translate_playground_save_failure`) 가 표시된다. 입력·결과 상태는 변경되지 않는다.
- [ ] **AC-T02-23**: 동일 단어가 이미 덱에 있더라도 병합 없이 별도 카드로 저장된다 (FlashCard PRD 의 중복 처리 결정 사항 준수). 사용자에게는 별도 경고를 표시하지 않는다.
- [ ] **AC-T02-24**: 저장 버튼은 **카드 외부 풀폭** 위치이며, 항상 현재 `Success` 상태인 모든 대상 언어 결과를 **일괄 저장** 한다. 카드별 개별 저장 개념은 v0.1 에 존재하지 않는다. 사용자가 어느 카드를 보고 있든 동일한 한 장의 `WordCard` 가 생성된다.

#### TTS 재생 ([♪] 버튼)

- [ ] **AC-T02-25**: 각 결과 카드 우측에 [♪] 아이콘 버튼 (`Icons.AutoMirrored.Filled.VolumeUp` 등, contentDescription = `R.string.translate_playground_tts_cd`) 이 표시된다. 상태가 `Success` 일 때만 활성화.
- [ ] **AC-T02-26**: [♪] 탭 시 `TtsService.speak(text, language)` 가 호출된다. 재생 중에 다시 누르면 `TtsService.stop()` 호출. 재생 상태는 기존 `TtsService.state: StateFlow<TtsState>` 를 구독해 아이콘 토글로 반영한다 (기존 `DiaryDetailScreen` 과 동일 패턴).
- [ ] **AC-T02-27**: TTS 데이터 미설치 Locale 이면 `TtsState.Error(message)` 가 방출되고, 화면은 스낵바로 "해당 언어 TTS 데이터가 설치되어 있지 않습니다." (`R.string.translate_playground_tts_unavailable`) 를 표시. 재생은 시작되지 않는다.
- [ ] **AC-T02-29**: **TTS 단일 채널 정책** — 어느 시점에든 동시에 재생되는 TTS 는 한 카드뿐이다. 카드 A 가 재생 중일 때 사용자가 카드 B 의 [♪] 를 탭하면 (1) 카드 A 의 재생을 즉시 정지(`TtsService.stop()`), (2) 카드 B 의 텍스트로 새 재생을 시작한다. 두 카드의 음성이 겹치지 않아야 한다. 재생 상태 아이콘은 `TtsService.state` 의 현재 발화 언어를 기준으로 단 하나의 카드에서만 "재생 중" 으로 표시된다.

#### 기존 TranslateBrowse 제거

- [ ] **AC-T02-R01**: `TranslateBrowseScreen.kt`, `TranslateBrowseViewModel.kt` 파일을 삭제한다. `Navigation.kt` 의 `composable(Screen.Translate.route) { ... }` 블록은 `TranslatePlaygroundScreen` 을 호출하도록 교체한다.
- [ ] **AC-T02-R02**: `values/strings.xml` 과 `values-en/strings.xml` 에서 더 이상 참조되지 않는 `translate_browse_title`, `translate_browse_empty`, `translate_browse_source_lang` 키 3종은 **제거하지 않고 주석으로 deprecated 표기** 후 남긴다. 이유: 키 제거는 다른 브랜치 충돌 유발 가능 → 한 사이클 뒤 정리. 코드 내 실제 참조는 전부 제거되어 `R.string.translate_browse_*` 가 어디서도 호출되지 않음을 Grep 으로 확인한다.
- [ ] **AC-T02-R03**: `docs/prd/09-feature-i18n.md` 의 AC-i18n-03 (TranslateBrowseScreen) 은 이 사이클에서는 변경하지 않는다. Playground 의 i18n 은 이 문서의 "UI 문자열 리소스" 섹션으로 커버된다. 09 문서 정합성 갱신은 후속 정리 사이클에 위임.

#### 에러·엣지 케이스

- [ ] **AC-T02-E01**: 번역 중 ML Kit 첫 모델 다운로드가 네트워크 불량으로 실패하면 해당 언어 카드만 `Error` 로 전환된다 (다른 두 언어가 성공하면 그대로 표시). 에러 문구는 `R.string.translate_playground_error_generic` = "번역에 실패했습니다." — 사용자에게 기술적 세부는 노출하지 않는다.
- [ ] **AC-T02-E02**: 번역 중 사용자가 화면을 빠져나갔다가 돌아오면, ViewModel 이 살아 있으므로 상태는 유지된다. 단, 진행 중이던 `Job` 이 `viewModelScope` 에 묶여 있으므로 앱 프로세스가 죽기 전까지는 취소되지 않는다.
- [ ] **AC-T02-E03**: 원문 입력이 한 글자만 있어도(예: "안") 번역이 시도된다 — 최소 글자수 제약 없음. 단, ML Kit 이 비정상 응답(예: 원문 동일 반환) 을 줘도 `Success` 로 간주하고 그대로 표시한다. (품질 판정은 엔진에 위임.)
- [ ] **AC-T02-E04**: 입력이 비어 있는 상태에서 풀폭 저장 버튼 / 카드 [♪] 버튼은 어차피 모든 카드가 `Idle` 이므로 비활성(`successCount == 0`). 추가 가드 불필요.
- [ ] **AC-T02-E05**: 매우 긴 입력(200자 상한 도달) 에서 풀폭 저장 / 카드 [♪] / 번역 동작은 200자 이하 입력과 동일하게 동작한다. TTS 는 200자 정도는 한 번에 재생되지만, 기기별 차이가 있어도 허용한다. (Phase 2 에서 긴 문단 분할 재생 검토 — 05 문서 참조.)
- [ ] **AC-T02-E06**: 번역 성공 후 사용자가 입력을 완전히 지워 `Idle` 로 돌아간 경우, 기존 번역 결과 메모이제이션은 유지된다 — 같은 단어를 다시 입력하면 캐시 히트로 즉시 `Success`.
- [ ] **AC-T02-E07**: 원문 언어 드롭다운에서 현재 선택된 언어를 다시 선택하면 아무 동작 없음 (no-op). 결과를 리셋하지 않는다.
- [ ] **AC-T02-28**: **Empty State 모델 다운로드 안내** — 입력이 비어 있어 3개 카드가 모두 `Idle` 인 첫 화면 상태(혹은 "지우기" 직후)에서, 카드 목록 영역 하단(또는 첫 카드 위) 에 1 줄 헬퍼 텍스트를 `R.string.translate_playground_first_use_hint` 로 표시한다. 한국어: "처음 사용하는 언어쌍은 약 30 MB 모델 다운로드가 필요해요 (Wi-Fi 권장)." / English: "First-time language pairs need a ~30 MB model download (Wi-Fi recommended)." 카드가 한 번이라도 `Loading`/`Success`/`Error` 로 진입하면 헬퍼 텍스트는 숨긴다(반복 노출 방지). 텍스트 스타일은 `MaterialTheme.typography.bodySmall`, color = `onSurfaceVariant`.

#### 언어 코드 일관성 / i18n

- [ ] **AC-T02-I01**: 모든 언어 코드 참조는 `AppLanguage` enum 경유. 하드코딩된 `"ko"`/`"en"`/`"ja"`/`"zh"` 문자열 금지. ML Kit 호출은 기존 `TranslationEngine.translate()` 를 통해 이뤄지므로 자연히 준수된다.
- [ ] **AC-T02-I02**: 모든 UI 문자열은 `stringResource(R.string.*)` 경유. 하드코딩된 한국어 리터럴 금지. 신규 키는 `values/strings.xml` 과 `values-en/strings.xml` 에 동시 추가한다 (규칙: `translate_playground_*` 네임스페이스).
- [ ] **AC-T02-I03**: 스낵바·플레이스홀더·에러 문구는 아래 UI 문자열 리소스 표의 KO/EN 두 언어 모두 정의된다. JA/ZH 는 Phase 2 (09-feature-i18n Non-Goals 준수).

#### QA 관측성 (logcat 마커)

- [ ] **AC-T02-Q01**: 다음 상태 전이에 `BuildConfig.DEBUG` 가드 아래 `Log.d("QA", ...)` 마커를 삽입한다:
  - `Playground:inputChanged:len=<N>` — 입력 변경 시마다
  - `Playground:translateTrigger:source=<code>:targets=<codes>` — 디바운스 경계 돌파 후 번역 시작
  - `Playground:translateResult:target=<code>:status=<success|error>:elapsedMs=<N>` — 카드당 결과 확정
  - `Playground:cardSaved:word=<firstN>:langs=<codes>` — 단어 카드 저장 성공
  - `Playground:ttsStart:lang=<code>` / `Playground:ttsEnd:lang=<code>`
- [ ] **AC-T02-Q02**: QA 테스터는 위 마커를 `adb logcat -s QA` 로 확인해 E2E 검증 가능하다. 스크린샷 없이 로그만으로 "타이핑→디바운스→번역→저장" 경로가 재현 가능해야 한다.

### 파괴적 동작 / 안전

- [ ] **AC-T02-S01**: Playground 는 기존 일기·번역·단어 카드를 **수정하지 않는다**. 유일한 쓰기 액션은 단어 카드 "추가"(insert). 기존 데이터 덮어쓰기 경로 없음.
- [ ] **AC-T02-S02**: 기존 `TranslateBrowseScreen` 제거 자체는 사용자의 영속 데이터(일기·번역·카드) 에 영향을 주지 않는다. 기능 철거일 뿐이며 별도 확인 다이얼로그는 불필요. 단, 사이클 종료 시 CLAUDE.md 또는 `08-roadmap.md` 의 의사결정 로그에 "Translate 탭 재설계" 기록을 남긴다.

### UI 문자열 리소스 (신규)

| 키 | 한국어 (values/) | English (values-en/) |
|---|---|---|
| `translate_playground_title` | 번역 연습 | Translate |
| `translate_playground_input_hint` | 번역할 단어·구·문장을 입력하세요 | Enter a word, phrase, or sentence |
| `translate_playground_char_counter` | %1$d / %2$d | %1$d / %2$d |
| `translate_playground_source_language_label` | 원문 언어 | Source language |
| `translate_playground_clear_cd` | 지우기 | Clear |
| `translate_playground_placeholder` | 결과가 여기에 표시됩니다 | Results will appear here |
| `translate_playground_loading` | 번역 중… | Translating… |
| `translate_playground_retry` | 다시 시도 | Retry |
| `translate_playground_error_generic` | 번역에 실패했습니다 | Translation failed |
| `translate_playground_save_button_label` | 단어 카드로 저장 (%1$d개 언어 포함) | Save as word card (%1$d languages) |
| `translate_playground_save_success` | 단어 카드가 저장되었습니다. | Word card saved. |
| `translate_playground_save_success_action` | FlashCard 보기 | View |
| `translate_playground_save_failure` | 단어 카드 저장에 실패했습니다. 다시 시도해 주세요. | Failed to save word card. Please try again. |
| `translate_playground_tts_cd` | 발음 듣기 | Play pronunciation |
| `translate_playground_tts_unavailable` | 해당 언어 TTS 데이터가 설치되어 있지 않습니다. | TTS data for this language is not installed. |
| `translate_playground_first_use_hint` | 처음 사용하는 언어쌍은 약 30 MB 모델 다운로드가 필요해요 (Wi-Fi 권장). | First-time language pairs need a ~30 MB model download (Wi-Fi recommended). |

## Error Handling

- ML Kit이 반환하는 `MlKitException` 메시지를 그대로 `errorMessage`에 저장한다(일기 번역). 사용자에게는 "번역 실패. 재시도" 수준으로만 노출.
- Playground 에서는 예외 메시지를 사용자에게 노출하지 않고 `R.string.translate_playground_error_generic` 로 일반화한다. (개인정보·네트워크 정보 등이 메시지에 섞여 들어올 가능성 차단.)
- 모델 다운로드 중 취소/앱 종료 시 PENDING 상태가 남을 수 있다 → 앱 재시작 시 PENDING 레코드를 재실행할 것인지가 Open Question.

## Open Questions

- **Stale PENDING 회수**: 번역 도중 앱이 죽으면 PENDING이 유령처럼 남는다. 앱 시작 시 모든 PENDING을 큐에 다시 넣을 것인가, 타임아웃 정책을 둘 것인가?
- **품질 피드백 루프**: 사용자가 번역 품질을 평가(👍/👎)할 수단이 필요한가? 평가는 로컬로만 쓸 것인가 익명 집계를 보낼 것인가?
- **번체/간체 중국어**: 현재 `zh`는 간체로 통일. 대만·홍콩 사용자 피드백 시 `zh-TW` 분리 필요.
- **언어 자동 감지**: 작성 화면에서 사용자가 원문 언어를 수동 지정하는데, 모델이 자동 감지하도록 바꿀 것인가? (Playground 에도 동일 질문 적용 — 현재는 드롭다운 수동 선택.)
- **Playground 히스토리 영속화**: 현재는 비영속. 사용자가 "어제 찾아봤던 단어" 를 다시 찾고 싶을 때를 위해 최근 10~20건 로컬 캐시를 제공할 가치가 있는가? Phase 2 검토.
- **Playground 상한(200자) 적정성**: 학습자의 실제 입력 길이 분포를 관찰해야 한다. 일기로 흘러갈 수준(수백 자)이면 Diary 탭으로 유도하고, 구·문장 레벨이면 200자로 충분할 것.
- **[Resolved 2026-04-21]** Translate 탭 방향: 실시간 프리뷰 번역기(Playground) + 단어 카드 저장 로 확정. 기존 `TranslateBrowseScreen` 은 T-02 에서 제거.
