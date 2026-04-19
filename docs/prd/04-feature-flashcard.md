# 04 — Feature: FlashCard (단어장)

[← 목차로](../../PRD.md) · **status: in-progress**

> **변경 이력**
> - 2026-04-18 최초 작성 (shipped: 목록/숙련도/즐겨찾기)
> - 2026-04-18 `/ship wordcard-feature` iter 1 — 카드 추가 UI · 카드 삭제 UI를 `in-progress`로 승격. Open Questions 두 건 AC로 확정.
> - 2026-04-18 `/ship flashcard-ui-ux` iter 1 — FlashCard 화면 UI/UX 개선 7건 AC-9~AC-15로 추가. UX Flow B 갱신.
> - 2026-04-18 `/ship flashcard-ui-ux` iter 2 — [CRITICAL] nextReviewAt을 v0.1에서 활성화(masteryLevel별 고정 간격). 데이터 모델 "미사용" 표시 제거. AC-10 조건 구체화, AC-11 reviewCount 갱신 명시, AC-13 오늘 복습 예정 수치 추가, AC-15 색상 팔레트 정의, AC-16(신규) 숙련도 선택 시 nextReviewAt 자동 설정 추가. UX Flow B에 카드 자동 접힘 없음 결정 사항 추가.
> - 2026-04-18 `/ship wordcard-add-ux` iter 1 — 카드 추가 UX를 아이콘 버튼 + TextField BottomSheet 방식(방안 A)으로 전면 교체. `SelectionContainer` + `WordCardTextToolbar` 방식(Copy 납치 + 클립보드 프로브) 폐기. AC-1/AC-2 재정의, UX Flow A 전면 교체, Open Questions "카드 추가 UI 구현 방식" 항목 확정 처리.
> - 2026-04-19 `/ship wordcard-translation-edit` iter 1 — 번역 수정 기능을 Phase 2에서 v0.1 in-progress로 승격. Non-Goals에서 분리(단어 자체 수정은 Phase 2 유지). AC-17~AC-21 신규 추가. UX Flow D(번역 수정) 추가. 데이터 모델에 `isTranslationEdited` 필드 추가. Open Questions 2건 추가.
> - 2026-04-19 `/ship wordcard-translation-edit` iter 2 — prd-reviewer NEEDS_SPEC 피드백 4건 반영: AC-19 에러 스낵바 + BottomSheet 유지 조항 추가; UX Flow D 및 AC-19에 파괴적 덮어쓰기 경고 텍스트 추가; 번역 실패/빈 값 언어 처리 AC-22(신규)·AC-23(신규) 추가 및 AC-18 [저장] 비활성화 조건 예외 명기; 수정 인디케이터 위치를 공개 상태 전용으로 확정하여 AC-20 갱신 + Open Questions 항목 해소.
> - 2026-04-19 `/ship flashcard-recent-filter` iter 1 — 필터 칩에 "최근 추가 (7일 내)" 옵션 신규 추가. `FlashCardFilter` enum 에 `RecentlyAdded` 케이스 추가. AC-9 갱신(4개 칩), AC-24(신규 — 7일 경계 계산 규칙), AC-25(신규 — 필터 상호 배타성 명시). UX Flow B, Empty State 문구(4개 언어), 필터 칩 레이블 테이블, 데이터 모델 테이블(`createdAt`/`updatedAt` 명시) 갱신.

---

## Goal

사용자가 일기에서 익힌 단어/표현을 **플래시카드 덱**으로 축적하고, 숙련도 기반 복습을 통해 장기 기억으로 넘기도록 돕는다. 각 카드는 4개 언어 번역을 담아 한 카드에서 다국어 학습이 완결되도록 한다.

---

## Non-Goals

- 간격 반복 알고리즘(SM-2, Anki 방식)의 정교한 구현 — v0.1은 0–3 스케일의 단순 숙련도만.
- 학습 세션 UI(`FlashCardStudy` 라우트 연결) — Phase 2.
- 카드 공유/마켓 — 개인 덱 전용.
- 자동 단어 추출 — Phase 2. 현재는 수동 선택만.
- 카드 단어(`word` 필드) 직접 수정 — Phase 2.
- ML Kit 원본 번역 복원 기능(수정 전 번역값으로 되돌리기) — Phase 2.
- 번역 수정 이력 관리 — Phase 2.

---

## User Stories

1. **추가 (수동)**: 사용자로서, 일기 상세 화면의 원문 탭에서 "단어 카드 추가" 아이콘 버튼을 탭하고, BottomSheet 내 TextField에 단어/표현을 직접 입력하여 카드를 만들 수 있다. 카드 저장 시 해당 단어가 ML Kit으로 나머지 3개 언어로 자동 번역되어 채워진다.
2. **목록**: 사용자로서, FlashCard 탭에서 내가 저장한 카드 목록을 최신순으로 볼 수 있다.
3. **학습 (플립)**: 사용자로서, 카드를 탭하면 나머지 3개 언어 번역이 공개되고, 숙련도 Lv 0–3 중 하나를 선택해 복습 기록을 남길 수 있다.
4. **즐겨찾기**: 사용자로서, 하트 아이콘으로 카드를 즐겨찾기 하여 우선 복습할 수 있다.
5. **삭제**: 사용자로서, 카드 목록에서 카드를 삭제하여 덱을 관리할 수 있다.
6. **번역 수정**: 사용자로서, 카드의 특정 언어 번역이 오역이거나 맥락에 맞지 않을 때, 해당 번역을 직접 편집하여 정확한 번역으로 수정할 수 있다. 수정된 번역은 학습 화면과 복습에서 일관되게 반영된다.
7. **발음**: 사용자로서, 카드에 저장된 단어를 TTS로 들을 수 있다. *(Phase 2 후보)*

---

## UX Flow

### A. 카드 추가 — DiaryDetailScreen

```
[DiaryDetailScreen] — 원문 탭 활성
  └─ 탭 헤더 우측(또는 본문 상단 우측) "단어 카드 추가" 아이콘 버튼 (항상 표시)
        └─ 탭 → AddWordCardBottomSheet 열림
              ├─ 제목: "단어 카드 추가"
              ├─ TextField: 단어/표현 직접 입력 (키보드 자동 오픈, hint: "단어 또는 표현 입력")
              ├─ 원문 언어 표시 레이블 (예: "원문: 한국어")
              ├─ [저장] 버튼 — TextField가 비어 있으면 비활성화(disabled)
              │     └─ 탭 시:
              │           ├─ 번역 진행 스피너 (ML Kit 3개 언어 병렬 호출)
              │           ├─ 번역 완료 → WordCard 저장
              │           └─ 저장 완료 → BottomSheet 닫힘 + 스낵바: "단어 카드가 저장되었습니다."
              └─ [취소] 또는 시트 드래그 다운 → BottomSheet 닫힘 (저장 없음)
```

> **결정된 사항 (2026-04-18, wordcard-add-ux):** 아이콘 버튼 + TextField 입력 방식(방안 A) 채택. 이전 `SelectionContainer` + `WordCardTextToolbar`(Copy 납치 + 클립보드 프로브) 방식은 발견 불가능성·클립보드 오염·Copy 버튼 납치 문제로 폐기.
> **결정된 사항 (2026-04-18, wordcard-add-ux):** "단어 카드 추가" 아이콘 버튼은 **원문 탭에서만** 표시한다. 번역 탭에서는 표시하지 않는다. 이유: 카드의 `sourceLanguage`는 원문 언어로 저장되며, 번역 탭 텍스트를 카드 원문으로 저장하면 언어 메타데이터가 모호해진다.
> **결정된 사항 (2026-04-18, wordcard-add-ux):** `SelectionContainer` 전체 제거. 복사 기능은 Android 시스템 기본 텍스트 선택 UI로 대체(별도 `CompositionLocalProvider` 불필요).

### B. 카드 목록 — FlashCardScreen

```
[FlashCard Tab]
  ├─ 통계 배너: "카드 N개 · 즐겨찾기 M개"
  ├─ 필터 칩 행: [전체] [즐겨찾기] [복습 예정] [최근 추가]
  │     └─ 선택된 칩: filled 스타일, 나머지: outlined 스타일
  │     └─ 한 번에 하나의 필터만 선택 가능(단일 선택, 상호 배타적)
  │     └─ "복습 예정" = nextReviewAt ≤ now 조건
  │     └─ "최근 추가" = createdAt ≥ (now - 7일) 조건 (7일 내 생성된 카드만)
  ├─ 비어 있음: 아이콘 + 안내 텍스트 조합 (Empty State UI)
  │     └─ 필터에 따라 안내 문구 분기
  │           ├─ 전체 비어 있음: "아직 단어 카드가 없습니다. 일기에서 단어를 추가해 보세요."
  │           ├─ 즐겨찾기 비어 있음: "즐겨찾기한 카드가 없습니다."
  │           ├─ 복습 예정 비어 있음: "복습 예정인 카드가 없습니다."
  │           └─ 최근 추가 비어 있음: "최근 7일 내에 추가된 카드가 없습니다."
  └─ 카드 있음: LazyColumn
        └─ 카드 아이템
              ├─ 헤더 행
              │     ├─ 단어 텍스트
              │     ├─ 숙련도 시각 표시 (도트 3개 또는 진행 바, masteryLevel 0–3)
              │     └─ 출처 언어 AssistChip 배지 (컬러 소형 Chip)
              ├─ 서브헤더: 복습 횟수 레이블
              ├─ 오른쪽: 하트(즐겨찾기) 아이콘
              └─ 탭 → revealed 토글
                    ├─ 미공개: "탭하여 뜻 보기"
                    └─ 공개:
                          ├─ 3개 언어 번역 (각 번역 옆 언어 AssistChip 배지)
                          ├─ 숙련도 Chip 행: "모름" / "어려움" / "보통" / "완벽"
                          │     └─ 현재 선택 레벨: filled 스타일, 나머지: outlined 스타일
                          └─ 휴지통(삭제) 아이콘 — 공개 상태에서만 표시
```

> **결정된 사항 (2026-04-18):** 삭제 버튼은 카드가 공개(revealed) 상태일 때만 표시한다. 미공개 상태에서의 실수 탭 방지 목적.
> **결정된 사항 (2026-04-18):** 숙련도 레이블은 `Lv 0`=모름, `Lv 1`=어려움, `Lv 2`=보통, `Lv 3`=완벽 로 표시한다.
> **결정된 사항 (2026-04-18):** 필터 상태는 `FlashCardViewModel` 내 `filterState: StateFlow<FlashCardFilter>` 로 관리한다. DB 쿼리 변경 없이 ViewModel에서 Flow를 필터링하거나, 각 필터에 맞는 repository 메서드를 호출한다.
> **결정된 사항 (2026-04-19, flashcard-recent-filter):** `FlashCardFilter` enum 에 `RecentlyAdded` 케이스를 추가하여 `All | Favorites | DueForReview | RecentlyAdded` 4개 상태로 확장한다. 필터는 단일 선택(상호 배타적)이며, `RecentlyAdded` 는 `observeAllWordCards()` 의 Flow 를 `ViewModel` 단에서 `card.createdAt >= (System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)` 로 필터링한다. 별도 DAO 쿼리를 추가하지 않는다(목록 크기가 작을 것으로 가정, v0.1).
> **결정된 사항 (2026-04-18):** 숙련도 Chip 선택 후 카드는 자동으로 접히지 않는다. 사용자가 동일 카드에서 연속으로 숙련도를 수정하거나 번역 내용을 계속 볼 수 있도록 공개(revealed) 상태를 유지한다.

### C. 카드 삭제

```
[FlashCardScreen] — 카드 아이템
  └─ 휴지통 아이콘 탭
        └─ AlertDialog: "이 단어 카드를 삭제할까요?"
              ├─ [삭제] → repository.deleteWordCard(id) → 목록에서 즉시 제거 (Flow)
              └─ [취소] → 다이얼로그 닫기
```

### D. 번역 수정 — FlashCardScreen (카드 공개 상태)

```
[FlashCardScreen] — 카드 아이템 (revealed 상태)
  └─ 각 번역 항목 행
        ├─ [언어 배지] [번역 텍스트]   ← 기존 표시 (번역 실패/null인 경우: "번역 없음 — 탭하여 직접 입력")
        └─ 오른쪽: "편집" 아이콘 버튼 (pencil icon)  ← 번역 실패 언어도 편집 아이콘 표시
              └─ 탭 → EditTranslationBottomSheet 열림
                    ├─ 제목: "<언어 이름> 번역 수정" (예: "영어 번역 수정")
                    ├─ 현재 번역값이 채워진 TextField (전체 선택 상태로 포커스)
                    │     └─ 번역 실패/null인 경우 빈 TextField로 열림 (hint: "번역을 직접 입력하세요")
                    ├─ 수정됨 인디케이터: 해당 번역이 이미 사용자 수정본이면
                    │     "사용자 수정" 배지 또는 안내 문구 표시
                    ├─ 경고 보조 텍스트: "수정 후 ML Kit 원본 번역을 복원할 수 없습니다"
                    │     (항상 표시 — 파괴적 덮어쓰기 사전 고지)
                    ├─ [저장] 버튼
                    │     └─ 비활성화 조건:
                    │           ├─ 기존 번역이 있는 경우: TextField가 기존 번역과 동일하거나 빈 값이면 비활성화
                    │           └─ 번역 실패/null인 경우: TextField가 빈 값이면 비활성화 (동일값 조건 제외)
                    │     └─ 활성화 시 탭:
                    │           ├─ repository.updateWordCard(card.copy(translations=..., isTranslationEdited=true)) 호출
                    │           ├─ 성공: BottomSheet 닫힘 + 스낵바 "번역이 수정되었습니다."
                    │           └─ 실패: 에러 스낵바 "번역 수정에 실패했습니다. 다시 시도해 주세요." + BottomSheet 유지
                    └─ [취소] 또는 드래그 다운 → BottomSheet 닫힘 (변경 없음)

[FlashCardScreen] — 카드 아이템 (revealed 상태) — 번역 3개 전부 실패한 카드
  └─ 번역 항목 3행 모두 "번역 없음 — 탭하여 직접 입력" 플레이스홀더로 표시
        └─ 각 행 오른쪽: 편집 아이콘 버튼 (동일 흐름)
```

> **결정된 사항 (2026-04-19, wordcard-translation-edit):** 번역 수정 진입점은 카드 공개(revealed) 상태의 각 번역 항목 행에 위치한 편집 아이콘 버튼이다. 미공개 상태에서는 편집 아이콘이 표시되지 않는다.
> **결정된 사항 (2026-04-19, wordcard-translation-edit):** 수정 대상은 `translations` Map의 특정 언어 값만이다. `word`(원문 단어) 수정은 Phase 2 스코프이며 이 흐름에서 다루지 않는다.
> **결정된 사항 (2026-04-19, wordcard-translation-edit):** 사용자가 한 번이라도 번역을 수정한 카드에는 `isTranslationEdited = true` 플래그를 세워 UI에서 인디케이터로 표시한다. 어느 언어를 수정했는지 언어별 세분화 플래그는 Phase 2.
> **결정된 사항 (2026-04-19, wordcard-translation-edit):** ML Kit 원본 번역값 보존(복원 기능)은 Phase 2. v0.1은 덮어쓰기만 지원한다.
> **결정된 사항 (2026-04-19, wordcard-translation-edit iter 2):** 번역 실패/null인 언어도 편집 아이콘을 표시하여 사용자가 직접 입력할 수 있다. 이 경우 [저장] 비활성화의 "기존 번역과 동일" 조건은 적용하지 않는다(비교할 원본이 없으므로). 빈 값 비활성화 조건만 적용.
> **결정된 사항 (2026-04-19, wordcard-translation-edit iter 2):** BottomSheet 내에 "수정 후 ML Kit 원본 번역을 복원할 수 없습니다" 경고 보조 텍스트를 항상 표시하여 파괴적 덮어쓰기를 사전 고지한다.
> **결정된 사항 (2026-04-19, wordcard-translation-edit iter 2):** 저장 실패 시 에러 스낵바를 표시하고 BottomSheet은 닫지 않는다.
> **결정된 사항 (2026-04-19, wordcard-translation-edit iter 2):** "수정됨" 배지는 공개(revealed) 상태에서만 표시한다. 카드 헤더(미공개 상태 포함)에는 표시하지 않는다. 이유: 헤더 복잡도 최소화, 학습 흐름 자연스러움.

---

## Data Model

`WordCardEntity` (data layer) ↔ `WordCard` (domain layer). 변경 없음.

| 필드 | 타입 | 의미 |
|------|------|------|
| `id` | String | UUID |
| `sourceEntryId` | String? | 출처 일기 ID. FK SET NULL (일기 삭제 시 카드는 유지) |
| `word` | String | 학습 대상 단어/표현 |
| `sourceLanguage` | AppLanguage | 원문 언어 |
| `translations` | Map<AppLanguage, String> (JSON) | 최대 4개 언어 번역 |
| `exampleSentences` | Map<AppLanguage, String>? (JSON) | 선택적 예문 (Phase 2까지 미사용) |
| `masteryLevel` | Int (0–3) | 숙련도 |
| `nextReviewAt` | Long? | 다음 복습 시각. masteryLevel 선택 시 고정 간격으로 자동 설정: Lv 0 → +1일, Lv 1 → +3일, Lv 2 → +7일, Lv 3 → +30일. null이면 아직 숙련도가 한 번도 기록되지 않은 상태 |
| `reviewCount` | Int | 복습 횟수 |
| `isFavorite` | Boolean | 즐겨찾기 |
| `createdAt` | Long | 카드 생성 시각(epoch millis). 목록 정렬 기준이며 "최근 추가 (7일 내)" 필터 경계 계산에도 사용 |
| `updatedAt` | Long | 카드 마지막 수정 시각(epoch millis). 숙련도 선택/즐겨찾기 토글/번역 수정 시 갱신 |
| `isTranslationEdited` | Boolean | 사용자가 번역을 한 번이라도 수정했으면 `true`. 기본값 `false`. ML Kit 자동 번역과 사용자 수정 번역을 UI에서 구분하는 데 사용 |

JSON 직렬화 규칙: `MapSerializer(String.serializer(), String.serializer())`를 반드시 명시. reified 오버로드 금지 (CLAUDE.md 참조).

> **스키마 변경 주의 (2026-04-19):** `isTranslationEdited` 필드 추가는 `WordCardEntity`에 새 컬럼을 필요로 한다. Room `exportSchema = false`이므로 `version` bump 시 destructive rebuild가 발생한다. 기존 카드 데이터가 초기화되므로, 마이그레이션 스크립트(`ALTER TABLE word_cards ADD COLUMN isTranslationEdited INTEGER NOT NULL DEFAULT 0`)를 작성하거나 개발 단계임을 확인 후 rebuild를 선택한다. v0.1 MVP이므로 destructive rebuild 허용 — 단, 구현 단계에서 명시적으로 확인할 것.

### 단어 번역 소스 결정

> **결정된 사항 (2026-04-18):** 단어 카드 저장 시 **ML Kit을 단어 단위로 직접 호출**한다. 일기 레벨의 `TranslationEntity`와는 별도 호출. 이유: 일기 번역은 전체 문장 맥락 번역이므로 단어 단위 번역값을 추출하기 어렵고, 단어 카드는 독립적 생애주기를 가져야 하기 때문. 첫 호출 시 모델 다운로드가 완료되어 있다면 단어 번역은 네트워크 없이 3초 이내 응답해야 한다.

### 중복 단어 처리

> **결정된 사항 (2026-04-18):** 동일 단어(대소문자/공백 정규화 기준)가 이미 덱에 있을 때, **별도 카드로 추가**한다. `reviewCount` 공유나 병합은 Phase 2에서 검토. v0.1은 단순성 우선.

---

## 다국어 UI 문자열 예시

카드 추가 아이콘 버튼 접근성 레이블 (contentDescription):

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 단어 카드 추가 |
| English | Add to flashcards |
| 日本語 | 単語カードに追加 |
| 中文 | 添加到单词卡 |

BottomSheet 제목 및 TextField hint:

| 구분 | 한국어 | English | 日本語 | 中文 |
|------|--------|---------|--------|------|
| BottomSheet 제목 | 단어 카드 추가 | Add Word Card | 単語カードを追加 | 添加单词卡 |
| TextField hint | 단어 또는 표현 입력 | Enter a word or phrase | 単語やフレーズを入力 | 输入单词或短语 |

빈 상태 메시지 (전체 필터):

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 아직 단어 카드가 없습니다. 일기에서 단어를 추가해 보세요. |
| English | No word cards yet. Select a word in your diary to get started. |
| 日本語 | 単語カードがありません。日記から単語を選んで追加してみましょう。 |
| 中文 | 还没有单词卡。请在日记中选择单词来添加。 |

빈 상태 메시지 (즐겨찾기 필터):

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 즐겨찾기한 카드가 없습니다. |
| English | No favorite cards yet. |
| 日本語 | お気に入りのカードがありません。 |
| 中文 | 还没有收藏的卡片。 |

빈 상태 메시지 (복습 예정 필터):

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 복습 예정인 카드가 없습니다. |
| English | No cards due for review. |
| 日本語 | 復習予定のカードはありません。 |
| 中文 | 没有待复习的卡片。 |

빈 상태 메시지 (최근 추가 필터):

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 최근 7일 내에 추가된 카드가 없습니다. |
| English | No cards added in the last 7 days. |
| 日本語 | 過去7日以内に追加されたカードはありません。 |
| 中文 | 最近7天内没有添加的卡片。 |

숙련도 레이블:

| 레벨 | 한국어 | English | 日本語 | 中文 |
|------|--------|---------|--------|------|
| 0 | 모름 | Don't know | わからない | 不会 |
| 1 | 어려움 | Hard | 難しい | 困难 |
| 2 | 보통 | Okay | まあまあ | 还行 |
| 3 | 완벽 | Perfect | 完璧 | 完美 |

필터 칩 레이블:

| 구분 | 한국어 | English | 日本語 | 中文 |
|------|--------|---------|--------|------|
| 전체 | 전체 | All | すべて | 全部 |
| 즐겨찾기 | 즐겨찾기 | Favorites | お気に入り | 收藏 |
| 복습 예정 | 복습 예정 | Due | 復習予定 | 待复习 |
| 최근 추가 | 최근 추가 | Recent | 最近追加 | 最近添加 |

> 주: "최근 추가" 칩은 내부적으로 "7일 내" 를 의미하나, 칩 레이블 자체는 공간 절약을 위해 "최근 추가 / Recent / 最近追加 / 最近添加" 를 사용한다. 기간 정보는 Empty State 메시지와 PRD 정의로 보완한다.

번역 수정 편집 아이콘 접근성 레이블 (contentDescription):

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 번역 수정 |
| English | Edit translation |
| 日本語 | 翻訳を編集 |
| 中文 | 编辑翻译 |

번역 수정 BottomSheet 제목 (언어명은 런타임 치환):

| 구분 | 한국어 | English | 日本語 | 中文 |
|------|--------|---------|--------|------|
| BottomSheet 제목 | {언어명} 번역 수정 | Edit {language} translation | {言語名}の翻訳を編集 | 编辑{语言}翻译 |
| TextField hint (번역 실패 시) | 번역을 직접 입력하세요 | Enter translation manually | 翻訳を直接入力してください | 请直接输入翻译 |
| 파괴적 덮어쓰기 경고 보조 텍스트 | 수정 후 ML Kit 원본 번역을 복원할 수 없습니다 | Original ML Kit translation cannot be restored after editing | 編集後はML Kitの元の翻訳に戻すことができません | 编辑后无法恢复ML Kit原始翻译 |
| 저장 버튼 | 저장 | Save | 保存 | 保存 |
| 취소 버튼 | 취소 | Cancel | キャンセル | 取消 |
| 저장 완료 스낵바 | 번역이 수정되었습니다. | Translation updated. | 翻訳を更新しました。 | 翻译已更新。 |
| 저장 실패 에러 스낵바 | 번역 수정에 실패했습니다. 다시 시도해 주세요. | Failed to update translation. Please try again. | 翻訳の更新に失敗しました。もう一度お試しください。 | 翻译更新失败，请重试。 |

번역 실패 플레이스홀더 (번역 없는 언어 행에 표시):

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 번역 없음 — 탭하여 직접 입력 |
| English | No translation — tap to enter manually |
| 日本語 | 翻訳なし — タップして直接入力 |
| 中文 | 无翻译 — 点击直接输入 |

언어명 (BottomSheet 제목 치환용):

| AppLanguage | 한국어 표시 | English 표시 | 日本語 표시 | 中文 표시 |
|-------------|------------|-------------|------------|----------|
| KO | 한국어 | Korean | 韓国語 | 韩语 |
| EN | 영어 | English | 英語 | 英语 |
| JA | 일본어 | Japanese | 日本語 | 日语 |
| ZH | 중국어 | Chinese | 中国語 | 中文 |

사용자 수정 인디케이터 (`isTranslationEdited = true`인 카드에 표시):

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 수정됨 |
| English | Edited |
| 日本語 | 編集済み |
| 中文 | 已编辑 |

---

## Acceptance Criteria

### shipped (구현 완료)

- [x] 카드를 수동으로 저장하면 단어장 탭에서 즉시 보인다 (Flow 구독).
- [x] 카드를 탭하면 번역이 공개되고, Lv 0–3 Chip으로 숙련도를 업데이트하면 `reviewCount`가 1 증가한다.
- [x] 즐겨찾기 토글은 아이콘이 즉시 반영되고 영속화된다.
- [x] `FlashCardViewModel`이 `repository.observeAllWordCards()` Flow를 구독하여 StateFlow로 노출한다.
- [x] `DiaryRepository` 인터페이스에 `addWordCard`, `updateWordCard`, `deleteWordCard`, `observeAllWordCards`, `observeWordCardsForEntry`, `observeDueWordCards` 가 선언되어 있다.

### in-progress (이번 사이클 구현 대상)

- [ ] **AC-1 (카드 추가 진입)**: `DiaryDetailScreen` 원문 탭 활성 시 탭 헤더 또는 본문 상단 우측에 "단어 카드 추가" 아이콘 버튼이 **항상** 표시된다. 번역 탭 활성 시에는 해당 버튼이 숨겨진다. `SelectionContainer` / `WordCardTextToolbar` / 클립보드 프로브 코드를 사용하지 않는다.
- [ ] **AC-2 (카드 생성 시트)**: 아이콘 버튼 탭 시 `AddWordCardBottomSheet`가 열리며, 내부에 단어/표현 직접 입력을 위한 `TextField`(키보드 자동 오픈), 원문 언어 표시 레이블, [저장] 버튼이 포함된다. TextField가 비어 있으면 [저장] 버튼은 비활성화된다. [저장] 버튼 탭 시 ML Kit 번역 진행 스피너가 표시되고 번역 완료 후 카드가 저장된다.
- [ ] **AC-3 (번역 자동 채움)**: 카드 저장 시 `TranslationEngine.translate(word, sourceLanguage, targetLanguage)`를 나머지 3개 언어에 대해 병렬 호출하여 `WordCard.translations`에 채운다. 첫 모델 다운로드 이후 동일 언어쌍은 네트워크 없이 3초 이내 응답.
- [ ] **AC-4 (저장 완료 피드백)**: 카드 저장 완료 후 스낵바 "단어 카드가 저장되었습니다."가 표시된다. 동일 단어 중복 저장 시에도 별도 카드로 저장되며 동일 스낵바가 표시된다.
- [ ] **AC-5 (카드 삭제)**: `FlashCardScreen` 카드 아이템의 삭제 아이콘(휴지통)은 **카드가 공개(revealed) 상태일 때만** 표시된다. 탭 시 확인 다이얼로그가 표시되고, 확인하면 `repository.deleteWordCard(id)`가 호출되어 목록에서 즉시 제거된다.
- [ ] **AC-6 (삭제 취소)**: 삭제 확인 다이얼로그에서 [취소]를 누르면 카드가 유지된다.
- [ ] **AC-7 (sourceEntryId 연결)**: 저장된 카드의 `sourceEntryId`에 해당 일기의 ID가 정확히 기록된다. 이후 일기가 삭제되어도 카드의 `sourceEntryId`가 NULL이 되고 카드 자체는 유지된다 (FK SET NULL 정책).
- [ ] **AC-8 (언어 코드)**: 카드 저장 시 `sourceLanguage`, `translations` 키는 반드시 `AppLanguage` enum 경유. 하드코딩된 문자열 금지.
- [ ] **AC-9 (필터 칩)**: `FlashCardScreen` 상단에 "전체 / 즐겨찾기 / 복습 예정 / 최근 추가" 4개 필터 칩이 가로 행으로 배치된다. 선택된 칩은 filled 스타일, 나머지는 outlined 스타일이다. `FlashCardViewModel`은 `filterState: StateFlow<FlashCardFilter>` (`All | Favorites | DueForReview | RecentlyAdded`)를 노출하며, 칩 선택 시 해당 필터로 즉시 목록이 갱신된다. 기본값은 `All`. 기존 필터 칩의 UI 패턴(`FilterChipRow` 동일 컴포저블)을 재사용하며, 새 칩을 기존 칩 뒤에 append 하는 방식으로 추가한다(순서: 전체 → 즐겨찾기 → 복습 예정 → 최근 추가).
- [ ] **AC-10 (필터 — 복습 예정)**: "복습 예정" 필터는 `nextReviewAt != null && nextReviewAt ≤ currentTimeMillis()` 조건을 만족하는 카드만 표시한다. `nextReviewAt`이 null인 카드(숙련도를 한 번도 선택하지 않은 신규 카드)는 "복습 예정" 필터에서 제외된다.
- [ ] **AC-11 (숙련도 레이블)**: 카드 공개 상태의 숙련도 선택 Chip 레이블이 `Lv 0`=모름, `Lv 1`=어려움, `Lv 2`=보통, `Lv 3`=완벽 으로 표시된다. 현재 선택된 레벨의 Chip은 filled 스타일, 나머지는 outlined 스타일이다. 숙련도 Chip 선택 즉시 카드 서브헤더의 `reviewCount` UI가 갱신된다(+1 반영 즉시 표시, Flow 구독 경유).
- [ ] **AC-12 (숙련도 시각화)**: 카드 헤더에 `masteryLevel` 0–3을 시각적으로 표현하는 요소(도트 3개 또는 진행 바)가 표시된다. 레벨 0은 모든 요소 비활성, 레벨 3은 모든 요소 활성화된 상태로 한눈에 숙련도를 파악할 수 있다.
- [ ] **AC-13 (통계 배너)**: `FlashCardScreen` 상단(필터 칩 위)에 "카드 N개 · 즐겨찾기 M개 · 오늘 복습 K개" 형태의 통계 배너가 표시된다. N은 전체 카드 수, M은 즐겨찾기 카드 수, K는 `nextReviewAt != null && nextReviewAt ≤ currentTimeMillis()` 조건의 카드 수이며, 카드 추가/삭제/즐겨찾기 토글/숙련도 선택 시 즉시 갱신된다. K가 0이면 "오늘 복습 0개" 도 표시한다(조건 달성 시 사용자 인지 가능).
- [ ] **AC-14 (Empty State)**: 카드 목록이 비어 있을 때 텍스트만 표시하지 않고, 관련 아이콘 + 안내 텍스트 조합의 Empty State UI를 표시한다. 필터별 안내 문구가 다르다: 전체 비어 있음 → "아직 단어 카드가 없습니다. 일기에서 단어를 추가해 보세요.", 즐겨찾기 비어 있음 → "즐겨찾기한 카드가 없습니다.", 복습 예정 비어 있음 → "복습 예정인 카드가 없습니다.", 최근 추가 비어 있음 → "최근 7일 내에 추가된 카드가 없습니다."
- [ ] **AC-15 (언어 배지)**: 카드 헤더에 출처 언어(`sourceLanguage`)를 소형 컬러 AssistChip으로 표시한다. 공개 상태의 번역 목록에서도 각 번역 항목 옆에 해당 언어의 AssistChip 배지가 표시된다. 언어별 칩 색상은 아래 팔레트를 기준으로 `AppLanguage` 값에 따라 일관되게 적용한다.
  - KO: 파랑 계열 (예: `Color(0xFF1565C0)` 배경, 흰 텍스트)
  - EN: 녹색 계열 (예: `Color(0xFF2E7D32)` 배경, 흰 텍스트)
  - JA: 빨강 계열 (예: `Color(0xFFC62828)` 배경, 흰 텍스트)
  - ZH: 주황 계열 (예: `Color(0xFFE65100)` 배경, 흰 텍스트)
  - 정확한 색상 값은 구현 시 Material Design 토큰과 조화롭게 조정 가능하나, 계열 매핑(KO=파랑/EN=녹색/JA=빨강/ZH=주황)은 변경 불가.

- [ ] **AC-16 (숙련도 선택 시 nextReviewAt 자동 설정)**: 숙련도 Chip 선택 시 `nextReviewAt`이 아래 고정 간격으로 자동 계산되어 `WordCard`에 저장된다.
  - Lv 0 (모름): 현재 시각 + 1일
  - Lv 1 (어려움): 현재 시각 + 3일
  - Lv 2 (보통): 현재 시각 + 7일
  - Lv 3 (완벽): 현재 시각 + 30일
  - 계산 기준: `System.currentTimeMillis() + intervalDays * 24 * 60 * 60 * 1000L`

- [ ] **AC-17 (번역 수정 진입)**: 카드 공개(revealed) 상태의 각 번역 항목 행 오른쪽에 편집 아이콘 버튼(pencil icon)이 표시된다. 카드 미공개 상태에서는 편집 아이콘이 표시되지 않는다.

- [ ] **AC-18 (번역 수정 시트)**: 편집 아이콘 버튼 탭 시 `EditTranslationBottomSheet`가 열린다. 시트에는 (1) 대상 언어명이 포함된 제목(예: "영어 번역 수정"), (2) 현재 번역값이 전체 선택 상태로 채워진 `TextField`(키보드 자동 오픈; 번역 실패/null인 경우 빈 TextField에 hint "번역을 직접 입력하세요"로 표시), (3) "수정 후 ML Kit 원본 번역을 복원할 수 없습니다" 경고 보조 텍스트(항상 표시), (4) [저장] 버튼, (5) [취소] 버튼이 포함된다. [저장] 버튼 비활성화 조건: 기존 번역이 있는 경우 TextField가 기존 번역과 동일하거나 비어 있으면 비활성화; 번역 실패/null인 경우 빈 값이면 비활성화(동일값 조건 미적용).

- [ ] **AC-19 (번역 수정 저장)**: [저장] 버튼 탭 시 해당 언어 번역만 수정된 `translations` Map을 가진 카드가 `repository.updateWordCard()`로 저장된다. `isTranslationEdited`가 `true`로 갱신된다. 저장 성공 시 BottomSheet이 닫히고 스낵바 "번역이 수정되었습니다."가 표시된다. 카드 목록의 해당 번역 텍스트가 즉시 갱신된다 (Flow 구독 경유). 저장 실패 시 에러 스낵바 "번역 수정에 실패했습니다. 다시 시도해 주세요."를 표시하고 BottomSheet은 닫지 않는다(사용자가 재시도 가능한 상태 유지).

- [ ] **AC-20 (수정 인디케이터)**: `isTranslationEdited = true`인 카드에는 카드 공개(revealed) 상태에서만 "수정됨" 배지(텍스트 또는 아이콘)가 표시된다. 카드 헤더(미공개 상태 포함)에는 표시하지 않는다. 해당 배지는 사용자가 ML Kit 자동 번역이 아닌 직접 수정한 번역이 있음을 인지할 수 있도록 한다. 이유: 헤더 복잡도 최소화 및 학습 흐름 자연스러움.

- [ ] **AC-21 (언어 코드 일관성)**: 번역 수정 저장 시 `translations` Map 키는 반드시 `AppLanguage` enum 경유. 하드코딩된 문자열 금지. `BottomSheet` 내 언어 이름 표시는 앱 UI 언어(한국어 기준 v0.1)에 따라 현지화된다.

- [ ] **AC-22 (번역 실패 언어 편집 가능)**: `translations` Map에 특정 언어의 값이 null 또는 빈 문자열인 경우(ML Kit 번역 실패), 해당 언어 행에도 편집 아이콘 버튼을 표시하고 "번역 없음 — 탭하여 직접 입력" 플레이스홀더 텍스트를 노출한다. 사용자는 편집 아이콘을 통해 직접 번역을 입력하고 저장할 수 있다. 이 경우 AC-18의 [저장] 비활성화 조건 중 "기존 번역과 동일" 조항은 적용하지 않는다(비교 원본이 없으므로).

- [ ] **AC-23 (3개 언어 번역 전부 실패 카드 표시)**: `translations` Map의 3개 언어 값이 모두 null 또는 빈 문자열인 카드(번역 완전 실패 카드)도 카드 목록에 정상 표시된다. 공개 상태에서는 3개 언어 번역 행 모두 "번역 없음 — 탭하여 직접 입력" 플레이스홀더로 표시되며, 각 행에 편집 아이콘이 표시되어 사용자가 직접 입력 가능하다.

- [ ] **AC-24 (최근 추가 필터 — 7일 경계 계산)**: "최근 추가" 필터는 `WordCard.createdAt` 기준 **현재 시각으로부터 7일 이내** 에 생성된 카드만 표시한다. 경계 계산 규칙:
  - 기준값: `val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L` (= 7일 = 604,800,000 ms)
  - 필터 조건: `card.createdAt >= sevenDaysAgo` (하한 경계 포함, inclusive)
  - 정렬: 기존 목록 정렬 기준(`createdAt DESC`)을 유지
  - 시점 계산은 UI 렌더 시점이 아닌 ViewModel 에서 Flow 결합 시점의 `System.currentTimeMillis()` 를 사용하며, Flow 가 재방출(즐겨찾기 토글·숙련도 선택 등)될 때마다 재평가된다. 사용자가 FlashCard 탭에 장시간 머물며 자정을 넘겨도 현재 카드가 즉시 사라질 필요는 없다(다음 Flow 재방출 시 반영).
  - 타임존: `System.currentTimeMillis()` 의 UTC epoch millis 기준. 현지 자정 기반 캘린더 경계가 아닌 롤링 168시간 윈도우.

- [ ] **AC-25 (필터 상호 배타성)**: 4개 필터 칩 중 항상 **정확히 하나** 만 선택 상태(`filled`)이다. 사용자가 다른 칩을 탭하면 이전 선택은 자동으로 해제(`outlined`)되고 `filterState` 는 새 값으로 교체된다. 현재 선택된 칩을 다시 탭하면 상태는 변경되지 않는다(토글로 `All` 로 되돌아가지 않음 — 기본값 `All` 로의 복귀는 "전체" 칩 탭으로만 수행).

### planned (Phase 2)

- [ ] 일기 저장 시 단어 자동 추출 및 사용자 승인 플로우.
- [ ] `FlashCardStudy` 라우트 활성화 — 세션 모드, 즐겨찾기/낮은 숙련도 우선.
- [ ] `nextReviewAt` 간격 고도화 — SM-2 lite 알고리즘으로 개인화된 간격 반복. v0.1의 고정 간격(AC-16)을 대체.
- [ ] 카드 TTS 재생 (각 언어별 단어 발음).
- [ ] 예문 자동 채움 (`exampleSentences` 필드 활성화).
- [ ] 카드 단어(`word` 필드) 직접 수정.
- [ ] 번역 수정 이력 관리 — 언어별 수정 여부 세분화 플래그, 수정 시각 기록.
- [ ] ML Kit 원본 번역 복원 — 수정 전 번역값으로 되돌리기.

---

## Phase 2 후보

`08-roadmap.md`에서 관리. 요약:

1. **자동 단어 추출** — ML Kit Entity Extraction 또는 형태소 분석기로 후보 단어를 뽑아 사용자 승인 후 저장.
2. **학습 세션 모드** — `FlashCardStudy` 라우트 활성화. 세션 종료 후 통계.
3. **간격 반복 고도화** — v0.1 고정 간격(AC-16)을 SM-2 lite 알고리즘으로 대체. 개인화된 복습 일정.
4. **예문 자동 생성** — 원문 일기 문장에서 해당 단어가 포함된 문장을 예문으로 자동 채움.

---

## Open Questions

- **TTS 연동**: 카드 상세(공개 상태)에서 각 언어 번역 단어를 TTS로 들을 수 있어야 하는가? Phase 2에서 다루는 것으로 잠정 결정. 우선순위가 높다면 이번 사이클에 포함 가능.
- **Room 스키마 마이그레이션 전략**: `isTranslationEdited` 컬럼 추가 시 `ALTER TABLE` 마이그레이션 스크립트를 작성할 것인가, v0.1 MVP 특성상 destructive rebuild를 허용할 것인가? 현재 PRD는 destructive rebuild를 허용하는 방향으로 기술했으나, QA 단계에서 기존 카드 데이터 유실 여부를 명시적으로 확인해야 한다.
- ~~**수정 인디케이터 위치**~~ — 공개(revealed) 상태에서만 표시로 확정 (2026-04-19, wordcard-translation-edit iter 2). 헤더 복잡도 최소화 및 학습 흐름 자연스러움이 이유. AC-20 반영 완료.
- ~~**카드 추가 UI 구현 방식**~~ — 아이콘 버튼 + TextField BottomSheet 방식(방안 A)으로 확정 (2026-04-18, wordcard-add-ux). `SelectionContainer` / `WordCardTextToolbar` / 클립보드 프로브 전면 제거.
- ~~**단어 추가 UI 위치**~~ — 원문 탭 헤더(또는 본문 상단 우측) 아이콘 버튼으로 확정 (2026-04-18, wordcard-add-ux).
- ~~**번역 재사용**~~ — ML Kit 단어 단위 직접 호출로 확정 (2026-04-18).
- ~~**카드 중복**~~ — 별도 카드로 추가(병합 없음)로 확정 (2026-04-18).
