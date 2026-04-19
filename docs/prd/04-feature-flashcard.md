# 04 — Feature: FlashCard (단어장)

[← 목차로](../../PRD.md) · **status: in-progress**

> **변경 이력**
> - 2026-04-18 최초 작성 (shipped: 목록/숙련도/즐겨찾기)
> - 2026-04-18 `/ship wordcard-feature` iter 1 — 카드 추가 UI · 카드 삭제 UI를 `in-progress`로 승격. Open Questions 두 건 AC로 확정.
> - 2026-04-18 `/ship flashcard-ui-ux` iter 1 — FlashCard 화면 UI/UX 개선 7건 AC-9~AC-15로 추가. UX Flow B 갱신.
> - 2026-04-18 `/ship flashcard-ui-ux` iter 2 — [CRITICAL] nextReviewAt을 v0.1에서 활성화(masteryLevel별 고정 간격). 데이터 모델 "미사용" 표시 제거. AC-10 조건 구체화, AC-11 reviewCount 갱신 명시, AC-13 오늘 복습 예정 수치 추가, AC-15 색상 팔레트 정의, AC-16(신규) 숙련도 선택 시 nextReviewAt 자동 설정 추가. UX Flow B에 카드 자동 접힘 없음 결정 사항 추가.

---

## Goal

사용자가 일기에서 익힌 단어/표현을 **플래시카드 덱**으로 축적하고, 숙련도 기반 복습을 통해 장기 기억으로 넘기도록 돕는다. 각 카드는 4개 언어 번역을 담아 한 카드에서 다국어 학습이 완결되도록 한다.

---

## Non-Goals

- 간격 반복 알고리즘(SM-2, Anki 방식)의 정교한 구현 — v0.1은 0–3 스케일의 단순 숙련도만.
- 학습 세션 UI(`FlashCardStudy` 라우트 연결) — Phase 2.
- 카드 공유/마켓 — 개인 덱 전용.
- 자동 단어 추출 — Phase 2. 현재는 수동 선택만.
- 카드 편집(단어/번역 직접 수정) — Phase 2.

---

## User Stories

1. **추가 (수동)**: 사용자로서, 일기 상세 화면에서 텍스트를 롱프레스(또는 선택)하여 "단어 카드로 저장" 옵션을 통해 카드를 만들 수 있다. 카드 저장 시 해당 단어가 ML Kit으로 나머지 3개 언어로 자동 번역되어 채워진다.
2. **목록**: 사용자로서, FlashCard 탭에서 내가 저장한 카드 목록을 최신순으로 볼 수 있다.
3. **학습 (플립)**: 사용자로서, 카드를 탭하면 나머지 3개 언어 번역이 공개되고, 숙련도 Lv 0–3 중 하나를 선택해 복습 기록을 남길 수 있다.
4. **즐겨찾기**: 사용자로서, 하트 아이콘으로 카드를 즐겨찾기 하여 우선 복습할 수 있다.
5. **삭제**: 사용자로서, 카드 목록에서 카드를 삭제하여 덱을 관리할 수 있다.
6. **발음**: 사용자로서, 카드에 저장된 단어를 TTS로 들을 수 있다. *(Phase 2 후보)*

---

## UX Flow

### A. 카드 추가 — DiaryDetailScreen

```
[DiaryDetailScreen] — 언어 탭(원문 탭 활성)
  └─ 본문 텍스트를 롱프레스 → 시스템 텍스트 선택 핸들 표시
        └─ 선택 완료 후 상단 컨텍스트 메뉴에 "카드 추가" 항목 노출
              └─ 탭 → AddWordCardBottomSheet (또는 다이얼로그) 표시
                    ├─ 선택된 단어 표시 (편집 불가, 확인용)
                    ├─ 번역 중 스피너 (ML Kit 호출 진행)
                    └─ [저장] 버튼 → 번역 완료 후 WordCard 저장
                          └─ 스낵바: "단어 카드가 저장되었습니다."
```

> **결정된 사항 (2026-04-18):** 텍스트 선택 후 컨텍스트 메뉴 방식 채택. 별도 "+ 단어" 버튼은 사용하지 않는다. 컨텍스트 메뉴는 Compose `SelectionContainer` + `ContextMenuArea` 패턴 또는 `BasicTextField`의 `onTextLayout` 콜백으로 구현한다.

### B. 카드 목록 — FlashCardScreen

```
[FlashCard Tab]
  ├─ 통계 배너: "카드 N개 · 즐겨찾기 M개"
  ├─ 필터 칩 행: [전체] [즐겨찾기] [복습 예정]
  │     └─ 선택된 칩: filled 스타일, 나머지: outlined 스타일
  │     └─ "복습 예정" = nextReviewAt ≤ now 조건
  ├─ 비어 있음: 아이콘 + 안내 텍스트 조합 (Empty State UI)
  │     └─ 필터에 따라 안내 문구 분기
  │           ├─ 전체 비어 있음: "아직 단어 카드가 없습니다. 일기에서 단어를 추가해 보세요."
  │           ├─ 즐겨찾기 비어 있음: "즐겨찾기한 카드가 없습니다."
  │           └─ 복습 예정 비어 있음: "복습 예정인 카드가 없습니다."
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
> **결정된 사항 (2026-04-18):** 필터 상태는 `FlashCardViewModel` 내 `filterState: StateFlow<FlashCardFilter>` 로 관리한다. `FlashCardFilter`는 `All | Favorites | DueForReview` enum. DB 쿼리 변경 없이 ViewModel에서 Flow를 필터링하거나, 각 필터에 맞는 repository 메서드를 호출한다.
> **결정된 사항 (2026-04-18):** 숙련도 Chip 선택 후 카드는 자동으로 접히지 않는다. 사용자가 동일 카드에서 연속으로 숙련도를 수정하거나 번역 내용을 계속 볼 수 있도록 공개(revealed) 상태를 유지한다.

### C. 카드 삭제

```
[FlashCardScreen] — 카드 아이템
  └─ 휴지통 아이콘 탭
        └─ AlertDialog: "이 단어 카드를 삭제할까요?"
              ├─ [삭제] → repository.deleteWordCard(id) → 목록에서 즉시 제거 (Flow)
              └─ [취소] → 다이얼로그 닫기
```

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

JSON 직렬화 규칙: `MapSerializer(String.serializer(), String.serializer())`를 반드시 명시. reified 오버로드 금지 (CLAUDE.md 참조).

### 단어 번역 소스 결정

> **결정된 사항 (2026-04-18):** 단어 카드 저장 시 **ML Kit을 단어 단위로 직접 호출**한다. 일기 레벨의 `TranslationEntity`와는 별도 호출. 이유: 일기 번역은 전체 문장 맥락 번역이므로 단어 단위 번역값을 추출하기 어렵고, 단어 카드는 독립적 생애주기를 가져야 하기 때문. 첫 호출 시 모델 다운로드가 완료되어 있다면 단어 번역은 네트워크 없이 3초 이내 응답해야 한다.

### 중복 단어 처리

> **결정된 사항 (2026-04-18):** 동일 단어(대소문자/공백 정규화 기준)가 이미 덱에 있을 때, **별도 카드로 추가**한다. `reviewCount` 공유나 병합은 Phase 2에서 검토. v0.1은 단순성 우선.

---

## 다국어 UI 문자열 예시

카드 추가 컨텍스트 메뉴 레이블:

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 단어 카드 추가 |
| English | Add to flashcards |
| 日本語 | 単語カードに追加 |
| 中文 | 添加到单词卡 |

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

---

## Acceptance Criteria

### shipped (구현 완료)

- [x] 카드를 수동으로 저장하면 단어장 탭에서 즉시 보인다 (Flow 구독).
- [x] 카드를 탭하면 번역이 공개되고, Lv 0–3 Chip으로 숙련도를 업데이트하면 `reviewCount`가 1 증가한다.
- [x] 즐겨찾기 토글은 아이콘이 즉시 반영되고 영속화된다.
- [x] `FlashCardViewModel`이 `repository.observeAllWordCards()` Flow를 구독하여 StateFlow로 노출한다.
- [x] `DiaryRepository` 인터페이스에 `addWordCard`, `updateWordCard`, `deleteWordCard`, `observeAllWordCards`, `observeWordCardsForEntry`, `observeDueWordCards` 가 선언되어 있다.

### in-progress (이번 사이클 구현 대상)

- [ ] **AC-1 (카드 추가 진입)**: `DiaryDetailScreen`의 원문 탭 본문에서 텍스트를 선택하면 "단어 카드 추가" 액션이 컨텍스트 메뉴 또는 액션 버튼으로 노출된다.
- [ ] **AC-2 (카드 생성 시트)**: "단어 카드 추가" 액션 실행 시 선택된 단어와 번역 진행 상태를 보여주는 BottomSheet(또는 AlertDialog)가 표시된다. 번역이 완료되면 [저장] 버튼이 활성화된다.
- [ ] **AC-3 (번역 자동 채움)**: 카드 저장 시 `TranslationEngine.translate(word, sourceLanguage, targetLanguage)`를 나머지 3개 언어에 대해 병렬 호출하여 `WordCard.translations`에 채운다. 첫 모델 다운로드 이후 동일 언어쌍은 네트워크 없이 3초 이내 응답.
- [ ] **AC-4 (저장 완료 피드백)**: 카드 저장 완료 후 스낵바 "단어 카드가 저장되었습니다."가 표시된다. 동일 단어 중복 저장 시에도 별도 카드로 저장되며 동일 스낵바가 표시된다.
- [ ] **AC-5 (카드 삭제)**: `FlashCardScreen` 카드 아이템의 삭제 아이콘(휴지통)은 **카드가 공개(revealed) 상태일 때만** 표시된다. 탭 시 확인 다이얼로그가 표시되고, 확인하면 `repository.deleteWordCard(id)`가 호출되어 목록에서 즉시 제거된다.
- [ ] **AC-6 (삭제 취소)**: 삭제 확인 다이얼로그에서 [취소]를 누르면 카드가 유지된다.
- [ ] **AC-7 (sourceEntryId 연결)**: 저장된 카드의 `sourceEntryId`에 해당 일기의 ID가 정확히 기록된다. 이후 일기가 삭제되어도 카드의 `sourceEntryId`가 NULL이 되고 카드 자체는 유지된다 (FK SET NULL 정책).
- [ ] **AC-8 (언어 코드)**: 카드 저장 시 `sourceLanguage`, `translations` 키는 반드시 `AppLanguage` enum 경유. 하드코딩된 문자열 금지.
- [ ] **AC-9 (필터 칩)**: `FlashCardScreen` 상단에 "전체 / 즐겨찾기 / 복습 예정" 3개 필터 칩이 가로 행으로 배치된다. 선택된 칩은 filled 스타일, 나머지는 outlined 스타일이다. `FlashCardViewModel`은 `filterState: StateFlow<FlashCardFilter>` (`All | Favorites | DueForReview`)를 노출하며, 칩 선택 시 해당 필터로 즉시 목록이 갱신된다.
- [ ] **AC-10 (필터 — 복습 예정)**: "복습 예정" 필터는 `nextReviewAt != null && nextReviewAt ≤ currentTimeMillis()` 조건을 만족하는 카드만 표시한다. `nextReviewAt`이 null인 카드(숙련도를 한 번도 선택하지 않은 신규 카드)는 "복습 예정" 필터에서 제외된다.
- [ ] **AC-11 (숙련도 레이블)**: 카드 공개 상태의 숙련도 선택 Chip 레이블이 `Lv 0`=모름, `Lv 1`=어려움, `Lv 2`=보통, `Lv 3`=완벽 으로 표시된다. 현재 선택된 레벨의 Chip은 filled 스타일, 나머지는 outlined 스타일이다. 숙련도 Chip 선택 즉시 카드 서브헤더의 `reviewCount` UI가 갱신된다(+1 반영 즉시 표시, Flow 구독 경유).
- [ ] **AC-12 (숙련도 시각화)**: 카드 헤더에 `masteryLevel` 0–3을 시각적으로 표현하는 요소(도트 3개 또는 진행 바)가 표시된다. 레벨 0은 모든 요소 비활성, 레벨 3은 모든 요소 활성화된 상태로 한눈에 숙련도를 파악할 수 있다.
- [ ] **AC-13 (통계 배너)**: `FlashCardScreen` 상단(필터 칩 위)에 "카드 N개 · 즐겨찾기 M개 · 오늘 복습 K개" 형태의 통계 배너가 표시된다. N은 전체 카드 수, M은 즐겨찾기 카드 수, K는 `nextReviewAt != null && nextReviewAt ≤ currentTimeMillis()` 조건의 카드 수이며, 카드 추가/삭제/즐겨찾기 토글/숙련도 선택 시 즉시 갱신된다. K가 0이면 "오늘 복습 0개" 도 표시한다(조건 달성 시 사용자 인지 가능).
- [ ] **AC-14 (Empty State)**: 카드 목록이 비어 있을 때 텍스트만 표시하지 않고, 관련 아이콘 + 안내 텍스트 조합의 Empty State UI를 표시한다. 필터별 안내 문구가 다르다: 전체 비어 있음 → "아직 단어 카드가 없습니다. 일기에서 단어를 추가해 보세요.", 즐겨찾기 비어 있음 → "즐겨찾기한 카드가 없습니다.", 복습 예정 비어 있음 → "복습 예정인 카드가 없습니다."
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

### planned (Phase 2)

- [ ] 일기 저장 시 단어 자동 추출 및 사용자 승인 플로우.
- [ ] `FlashCardStudy` 라우트 활성화 — 세션 모드, 즐겨찾기/낮은 숙련도 우선.
- [ ] `nextReviewAt` 간격 고도화 — SM-2 lite 알고리즘으로 개인화된 간격 반복. v0.1의 고정 간격(AC-16)을 대체.
- [ ] 카드 TTS 재생 (각 언어별 단어 발음).
- [ ] 예문 자동 채움 (`exampleSentences` 필드 활성화).
- [ ] 카드 편집 (단어/번역 직접 수정).

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
- **카드 추가 UI 구현 방식**: Compose `SelectionContainer` + `ContextMenuArea` 조합 가능 여부를 실제 구현에서 검증 필요. 텍스트 선택 API가 제한적일 경우 "선택된 텍스트를 TextField에 수동 입력하는 다이얼로그" 방식으로 폴백 허용 여부를 coder가 판단하여 사용자에게 확인.
- ~~**단어 추가 UI 위치**~~ — 컨텍스트 메뉴 방식으로 확정 (2026-04-18).
- ~~**번역 재사용**~~ — ML Kit 단어 단위 직접 호출로 확정 (2026-04-18).
- ~~**카드 중복**~~ — 별도 카드로 추가(병합 없음)로 확정 (2026-04-18).
