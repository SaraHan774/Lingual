# 02 — Feature: Diary (일기)

[← 목차로](../../PRD.md) · **status: shipped (v0.1)**

## Goal

사용자가 자신에게 가장 편한 언어로 **마찰 없이** 일기를 작성하게 한다. 저장과 동시에 나머지 3개 언어 번역 파이프라인을 비동기로 트리거해, 사용자가 목록/상세 화면에서 번역 결과를 자연스럽게 소비할 수 있게 한다.

## Non-Goals

- 부유한 텍스트 에디터(마크다운, 이미지 삽입, 첨부) — v0.1은 순수 텍스트.
- 카테고리/폴더/검색 — v0.1은 단일 시간순 목록.
- 작성 중 자동 번역 프리뷰 — 저장 시점에만 번역.

## User Stories

- **작성**: 사용자로서, 일기 작성 화면에서 제목과 본문을 입력하고 원문 언어를 선택해 저장할 수 있다.
- **목록**: 사용자로서, 과거에 쓴 일기를 시간 역순으로 훑으며 제목·발췌·원문 언어를 한눈에 확인할 수 있다.
- **상세**: 사용자로서, 일기 상세에서 원문과 3개 번역을 탭으로 전환하며 읽을 수 있고, 각 탭에서 TTS 재생이 가능하다.
- **재시도**: 사용자로서, 번역이 실패했을 때 해당 탭에서 버튼 한 번으로 재번역할 수 있다.
- **편집**: 사용자로서, 일기 상세 화면에서 편집 버튼을 눌러 기존 내용을 수정하고 저장할 수 있다. 저장하면 수정된 본문으로 번역이 자동으로 다시 시작된다.

## UX Flow

```
[Diary Tab]
  └─ DiaryListScreen ─── FAB("쓰기") ──→ [WriteDiaryScreen (신규 모드)]
        │                                     │
        │  항목 탭                             │  저장
        ▼                                     ▼
  [DiaryDetailScreen]  ←──── navigation ─── (뒤로)
        ├─ 원문 탭 (원문 언어, 배지 "(원문)")
        ├─ 번역 탭 × 3 (PENDING=스피너 / SUCCESS=텍스트 / ERROR=재시도 버튼)
        ├─ 각 탭 하단에 TTS 재생 버튼
        └─ 상단 액션 바: 편집 아이콘 버튼
                │
                ▼
          [WriteDiaryScreen (편집 모드)]
                │  저장 (본문 변경 시 번역 전부 PENDING 리셋 → 즉시 재번역)
                ▼
          [DiaryDetailScreen]  (동일 entryId, popBackStack)
```

### 편집 진입 규칙

- `DiaryDetailScreen` 상단 액션 바에 편집 아이콘(`Icons.Filled.Edit`)을 배치한다.
- `WriteDiary` 라우트를 편집 모드로 재사용한다. 현재 코드에서 `WriteDiary`는 `data object`(파라미터 없음)이므로, **`data class WriteDiary(val entryId: String? = null)`로 변경이 필요하다.** `entryId`가 null이면 신규 작성 모드, non-null이면 편집 모드.
- `WriteDiaryViewModel`은 `SavedStateHandle`에서 `entryId`를 읽어 기존 엔트리를 로드하고, `save()` 시 새 UUID 생성 대신 기존 id로 update한다.

### 번역 무효화 정책 (결정됨 2026-04-19)

**정책 A + C 채택: PENDING 전체 리셋 → 즉시 재번역.**

본문(`content`) 또는 원문 언어(`sourceLanguage`)가 변경된 경우:

1. 기존 3개 언어 Translation 레코드를 모두 `PENDING`으로 리셋(translatedContent 비움).
2. 즉시 `translateAll()`을 호출해 재번역을 트리거.
3. 편집 후 상세 화면에서는 신규 작성 직후와 동일하게 스피너 → 텍스트 전이가 보인다.

근거: 기존 `WriteDiaryViewModel.translateAll()` 파이프라인을 그대로 재사용 가능. diff 기반 부분 번역(B 후보)은 ML Kit 오프라인 번역의 특성상 이점이 없고 복잡도만 증가.

**예외**: 제목(`title`)만 변경된 경우는 번역 무효화 없이 저장만 한다. 번역은 본문 기준이므로.

상세 내비게이션 규칙은 `Navigation.kt`의 `WriteDiary`, `DiaryDetail(id)` 타입 세이프 라우트 참조.
상세/쓰기 화면에서는 하단 내비게이션 바가 숨겨진다 (`hideBottomNavigationRoutes`).

## 번역 상태 표시 UI

### DiaryListScreen — TranslationStatusBadge

각 목록 항목에 `TranslationSummary` 기반의 인라인 배지를 표시한다.
ViewModel(`DiaryListViewModel`)에서 `ERROR > PENDING > AllDone` 우선순위로 집계한다.

| `TranslationSummary` | 아이콘 | 텍스트 | 컬러 토큰 |
|---|---|---|---|
| `AllDone` | 없음 (배지 미표시) | 없음 (배지 미표시) | — |
| `InProgress(completed, total)` | HourglassEmpty | "번역 중 {completed}/{total}" | `secondary` |
| `HasError` | Error | "번역 실패" | `error` |
| `Empty` | (없음) | (없음) | — |

> **결정됨 (2026-04-18)**: `AllDone` 배지 제거(선택지 A 채택). 이상 상태(PENDING/ERROR)만 배지 표시. `TranslationSummary.AllDone` 분기는 `Unit`으로 처리.

### DiaryDetailScreen — 탭 헤더 상태 인디케이터

현재(shipped): 탭 레이블은 언어 이름(원문은 "(원문)" 접미)만 표시한다. 번역 상태를 확인하려면 탭을 직접 눌러야 한다.

탭 레이블 옆에 소형 상태 아이콘을 추가해, 탭 전환 없이 각 언어의 번역 상태를 한눈에 파악할 수 있게 한다. (shipped)

| 탭 상태 | 인디케이터 | 구현 세부 |
|---|---|---|
| 원문 탭 | 인디케이터 영역 미생성 | 의도적으로 공간 미할당 |
| SUCCESS | 빈 Box 10dp | 공간 예약만 (아이콘 없음) |
| PENDING | `CircularProgressIndicator` 8dp, strokeWidth 1.5dp | HourglassEmpty 아이콘 대신 스피너 채택 |
| ERROR | `Icons.Filled.Error` 10dp, error 컬러 | — |

### DiaryDetailScreen — LanguagePanel 본문

| 상태 | 표시 내용 |
|---|---|
| 원문 탭 | 원문 텍스트 + TTS 버튼 + 단어 선택(텍스트 선택 → 단어카드 추가) |
| PENDING | CircularProgressIndicator + "번역 중..." |
| SUCCESS | 번역 텍스트 + TTS 버튼 |
| ERROR | "번역 실패: {errorMessage}" + "다시 시도" 버튼 |

## Data Model

| Entity | 핵심 필드 | 비고 |
|--------|----------|------|
| `DiaryEntryEntity` | `id`, `title`, `content`, `sourceLanguage`, `mood?`, `tags`(JSON), `createdAt`, `updatedAt` | `sourceLanguage`는 `AppLanguage.code` 문자열. |
| `TranslationEntity` | `id`, `diaryEntryId`(FK CASCADE), `targetLanguage`, `translatedContent`, `translationStatus`, `errorMessage?`, `translatedAt`, `modelVersion?` | `(diaryEntryId, targetLanguage)` UNIQUE. |

도메인 매핑: `DiaryEntry.toEntity()` / `DiaryEntryEntity.toDomain()` (`domain/model/DiaryEntry.kt`).

## Write → Translate 파이프라인

`WriteDiaryViewModel.save()`는 아래 순서를 지킨다.

1. `DiaryEntry`를 생성하고 Room에 insert.
2. 원문 언어를 제외한 3개 언어 각각에 대해 `Translation`을 **PENDING** 상태로 upsert(placeholder).
3. 각 언어별로 `TranslationEngine.translate(source, target, content)`를 호출.
4. 완료 시 **SUCCESS**(혹은 실패 시 **ERROR** + `errorMessage`)로 upsert.

이 순서가 깨지면 상세 화면이 스피너 → 텍스트 전환을 못 한다. 수정 시 `DiaryDetailScreen`의 Flow 구독 로직과 짝을 맞출 것.

## Acceptance Criteria

### 작성 / 저장
- [x] 저장 직후 상세 화면에 진입하면 3개 번역 탭이 모두 PENDING 스피너로 시작한다.
- [x] 각 언어 번역은 완료되는 대로 독립적으로 SUCCESS 상태로 전환된다(all-or-nothing 아님).
- [x] ERROR 탭에서 "재시도" 버튼을 누르면 해당 언어만 재번역된다.
- [x] 일기 삭제 시 연결된 모든 Translation이 CASCADE로 함께 삭제된다.
- [x] 앱을 강제 종료 후 재실행해도 PENDING 상태로 남아있던 번역은 다시 실행되거나(추후) 에러로 마감된다.

### DiaryListScreen 번역 상태 배지 (shipped)
- [x] 번역 중인 일기는 목록에서 "번역 중 X/Y" 배지로 표시된다.
- [x] 하나 이상 ERROR인 일기는 목록에서 "번역 실패" 배지로 표시된다.
- [x] ERROR와 PENDING이 동시에 존재하면 "번역 실패" 배지가 우선 표시된다.
- [x] 번역 레코드가 없는 일기(원문 언어 = 전체 언어 등 예외 케이스)는 배지를 표시하지 않는다.

### DiaryDetailScreen 탭 상태 인디케이터 (shipped)
- [x] 상세 화면 진입 시 탭 레이블 옆에 PENDING/ERROR 상태 인디케이터가 표시되어, 탭을 누르지 않아도 각 언어의 번역 상태를 알 수 있다.
- [x] SUCCESS 탭은 인디케이터 없이 언어 이름만 표시한다(정상 상태는 노이즈 없이).
- [x] PENDING 인디케이터는 번역 완료 시 자동으로 사라진다(Flow 실시간 반영).

### 일기 편집 (status: planned)

**AC-E01.** `DiaryDetailScreen` 상단 액션 바에 편집 아이콘 버튼이 노출된다.

**AC-E02.** 편집 버튼을 누르면 `WriteDiaryScreen`이 편집 모드로 열린다. 기존 제목·본문·원문 언어가 미리 채워져 있다.

**AC-E03.** `WriteDiary` 라우트는 `data class WriteDiary(val entryId: String? = null)` 형태로, `entryId`가 null이면 신규 작성, non-null이면 편집 모드로 동작한다. `hideBottomNavigationRoutes`는 변경 없이 유지된다.

**AC-E04.** 편집 화면에서 저장 버튼 비활성 조건: 본문이 비어 있거나 이미 저장 중인 경우. 저장 진행 중에는 버튼이 비활성화되며 원형 진행 인디케이터(CircularProgressIndicator)가 함께 표시된다. (신규 작성과 동일한 시각 피드백)

**AC-E05.** 저장 시 **본문 또는 원문 언어가 초기 로드값과 실제로 다른 경우**: 기존 3개 언어 Translation을 모두 PENDING으로 리셋하고, 즉시 재번역(`translateAll()`)을 트리거한다. 편집 후 상세 화면에서 탭들이 스피너 상태로 시작해 번역 완료 시 순차적으로 텍스트로 전환된다. **예외**: 수정 후 원래 값으로 되돌린 경우(최종 저장값 = 초기 로드값)에는 번역 무효화 없이 `updatedAt`만 갱신한다.

**AC-E06.** 저장 시 **제목만 변경된 경우**: 기존 Translation 레코드를 건드리지 않는다. 상세 화면의 번역 탭 상태는 편집 전과 동일하다.

**AC-E07.** 편집 저장 완료 후 `WriteDiaryScreen`에서 `popBackStack()`으로 `DiaryDetailScreen`으로 복귀한다. 복귀 후 화면은 변경된 제목·본문을 즉시 반영한다(Flow 구독).

**AC-E08.** 편집 화면에서 "뒤로" 또는 취소 시: 제목·본문·원문 언어 중 하나라도 초기 로드값과 다를 때는 확인 다이얼로그("저장하지 않고 나가시겠습니까? [취소] [나가기]")를 표시한다. 사용자가 [나가기]를 선택하거나 아무것도 변경하지 않은 경우에는 다이얼로그 없이 즉시 `DiaryDetailScreen`으로 복귀한다. 어느 경우든 변경 사항은 저장되지 않으며 기존 번역 상태는 유지된다.

**AC-E09.** `DiaryEntryEntity.updatedAt` 필드는 편집 저장 시점의 타임스탬프로 갱신된다. `createdAt`은 변경되지 않는다.

**AC-E10.** 편집 모드에서 `WriteDiaryViewModel`은 `SavedStateHandle`에서 `entryId`를 읽어 기존 엔트리를 로드한다. 로드 실패(ID 불일치 등) 시 편집 화면 진입 즉시 스낵바로 에러를 표시하고 자동으로 `DiaryDetailScreen`으로 `popBackStack`한다(사용자가 저장을 시도할 때까지 기다리지 않음).

## Open Questions

- **[Resolved 2026-04-18] AllDone 배지 제거**: 선택지 A 채택. `TranslationSummary.AllDone`은 배지를 렌더링하지 않는다(`Unit`). PENDING/ERROR 이상 상태만 배지 표시.
- **[Resolved 2026-04-19] 편집 기능 + 번역 무효화 정책**: 정책 A+C 채택. 본문/원문 언어 변경 시 전 언어 PENDING 리셋 → 즉시 재번역. 제목만 변경 시 번역 무효화 없음. `WriteDiary` 라우트를 `data class WriteDiary(val entryId: String? = null)`로 변경해 편집 모드 재사용.
- **Mood/Tags UI**: 엔티티에는 `mood`, `tags`가 있으나 작성 화면에 노출되어 있지 않음. 사용하지 않을 것이라면 스키마에서 제거하거나, 쓸 것이라면 입력 UI가 필요.
- **로컬 검색**: 일기 수가 수백 개로 늘었을 때 필터/검색이 필요한가?
- **HasError + Pending 복합 표시**: 일부 언어는 ERROR, 다른 언어는 아직 PENDING인 상황에서 현재 ViewModel은 ERROR를 우선 표시한다. 더 세분화된 표시(예: "번역 실패 + 번역 중 1/3")가 필요한가? Phase 2 논의 대상.
