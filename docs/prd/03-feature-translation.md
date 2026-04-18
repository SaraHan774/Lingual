# 03 — Feature: Translation (번역)

[← 목차로](../../PRD.md) · **status: shipped (v0.1)**

## Goal

일기 원문을 4개 언어 중 나머지 3개 언어로 **오프라인·비동기·실패 허용** 방식으로 번역한다. 번역 결과는 언어별 독립적 상태 머신을 가지며, 사용자는 일부 언어가 실패해도 다른 언어 결과를 읽을 수 있어야 한다.

## Non-Goals

- 클라우드 번역(Google Translate API, DeepL 등) — 프라이버시 원칙 위반.
- 문장 단위 정렬(alignment) 표시 — v0.1은 전체 문단 단위 결과만.
- 사용자 정의 용어집/고유명사 처리 — 엔진에 맡김.

## Supported Language Pairs

`AppLanguage` enum이 단일 출처 (`domain/model/AppLanguage.kt`).

| Code | 표시명 | Locale | 용도 |
|------|--------|--------|------|
| `ko` | 한국어 | `Locale.KOREAN` | source / target |
| `en` | English | `Locale.US` | source / target |
| `ja` | 日本語 | `Locale.JAPANESE` | source / target |
| `zh` | 中文 | `Locale.SIMPLIFIED_CHINESE` | source / target (간체 기준) |

총 번역 방향은 4 × 3 = 12쌍. 각 쌍은 독립 ML Kit `Translator` 인스턴스로 처리하고 엔진 내부 캐시로 재사용한다.

## UX: Translate 탭 (탐색)

`TranslateBrowseScreen`은 **일기 목록을 원문 언어로 필터링**해서 훑는 뷰다. 번역 결과를 별도로 편집하는 화면이 아니다. 상세로 진입하면 `DiaryDetailScreen`에서 실제 번역을 본다.

- FilterChip: `전체` + `한국어` / `English` / `日本語` / `中文`
- 항목 탭 → `DiaryDetail(id)`

## TranslationSummary 집계 로직

`DiaryListViewModel`이 `(DiaryEntry, List<Translation>)` 스트림을 소비해 각 항목의 `TranslationSummary`를 파생한다.

우선순위 (높음 → 낮음):
1. 번역 레코드 없음 → `Empty`
2. 하나 이상 `ERROR` → `HasError`
3. 하나 이상 `PENDING` (ERROR 없음) → `InProgress(completed, total)`
4. 전부 `SUCCESS` → `AllDone`

이 집계는 `DiaryListScreen`의 `TranslationStatusBadge`와 `DiaryDetailScreen`의 탭 인디케이터 양쪽에서 소비한다. 집계 변경 시 두 화면 모두 영향을 받으므로 동시에 검토해야 한다.

## 상태 머신

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

## Acceptance Criteria

- [x] 일기 저장 직후 `(diaryEntryId, targetLanguage)` 3쌍의 PENDING 레코드가 <500ms 내에 생성된다.
- [x] 동일 언어쌍을 두 번째로 번역할 때는 모델 다운로드 없이 수행된다 (인스턴스 캐시 히트).
- [x] 네트워크가 끊긴 상태에서 이미 다운로드된 언어쌍은 정상 번역된다.
- [x] 네트워크가 끊긴 상태에서 **첫 번째** 호출이면 ERROR 상태로 기록되고 사용자에게 재시도 옵션이 제공된다.
- [x] `modelVersion` 필드가 모든 SUCCESS 레코드에 기록된다.
- [ ] 번역 ERROR 비율이 주간 2% 미만. (메트릭 수집 수단은 미정 — Open Questions)

## Error Handling

- ML Kit이 반환하는 `MlKitException` 메시지를 그대로 `errorMessage`에 저장한다. 사용자에게는 "번역 실패. 재시도" 수준으로만 노출.
- 모델 다운로드 중 취소/앱 종료 시 PENDING 상태가 남을 수 있다 → 앱 재시작 시 PENDING 레코드를 재실행할 것인지가 Open Question.

## Open Questions

- **Stale PENDING 회수**: 번역 도중 앱이 죽으면 PENDING이 유령처럼 남는다. 앱 시작 시 모든 PENDING을 큐에 다시 넣을 것인가, 타임아웃 정책을 둘 것인가?
- **품질 피드백 루프**: 사용자가 번역 품질을 평가(👍/👎)할 수단이 필요한가? 평가는 로컬로만 쓸 것인가 익명 집계를 보낼 것인가?
- **번체/간체 중국어**: 현재 `zh`는 간체로 통일. 대만·홍콩 사용자 피드백 시 `zh-TW` 분리 필요.
- **언어 자동 감지**: 작성 화면에서 사용자가 원문 언어를 수동 지정하는데, 모델이 자동 감지하도록 바꿀 것인가?
