# 04 — Feature: FlashCard (단어장)

[← 목차로](../../PRD.md) · **status: in-progress**

> **변경 이력**
> - 2026-04-18 최초 작성 (shipped: 목록/숙련도/즐겨찾기)
> - 2026-04-18 `/ship wordcard-feature` iter 1 — 카드 추가 UI · 카드 삭제 UI를 `in-progress`로 승격. Open Questions 두 건 AC로 확정.

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

### B. 카드 목록 — FlashCardScreen (현재 shipped)

```
[FlashCard Tab]
  └─ 비어 있음: "아직 단어 카드가 없습니다. 일기에서 단어를 추가해 보세요."
  └─ 카드 있음: LazyColumn
        └─ 카드 아이템
              ├─ 왼쪽: 단어 + 출처 언어·숙련도·복습 횟수 레이블
              ├─ 오른쪽: 하트(즐겨찾기) + 휴지통(삭제) 아이콘
              └─ 탭 → revealed 토글
                    ├─ 미공개: "탭하여 뜻 보기"
                    └─ 공개: 3개 언어 번역 + Lv 0–3 Chip
```

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
| `nextReviewAt` | Long? | 다음 복습 시각 (현재 미사용, Phase 2) |
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

빈 상태 메시지:

| 언어 | 표시 문자 |
|------|-----------|
| 한국어 | 아직 단어 카드가 없습니다. 일기에서 단어를 추가해 보세요. |
| English | No word cards yet. Select a word in your diary to get started. |
| 日本語 | 単語カードがありません。日記から単語を選んで追加してみましょう。 |
| 中文 | 还没有单词卡。请在日记中选择单词来添加。 |

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
- [ ] **AC-5 (카드 삭제)**: `FlashCardScreen` 카드 아이템에 삭제 아이콘(휴지통)이 있다. 탭 시 확인 다이얼로그가 표시되고, 확인하면 `repository.deleteWordCard(id)`가 호출되어 목록에서 즉시 제거된다.
- [ ] **AC-6 (삭제 취소)**: 삭제 확인 다이얼로그에서 [취소]를 누르면 카드가 유지된다.
- [ ] **AC-7 (sourceEntryId 연결)**: 저장된 카드의 `sourceEntryId`에 해당 일기의 ID가 정확히 기록된다. 이후 일기가 삭제되어도 카드의 `sourceEntryId`가 NULL이 되고 카드 자체는 유지된다 (FK SET NULL 정책).
- [ ] **AC-8 (언어 코드)**: 카드 저장 시 `sourceLanguage`, `translations` 키는 반드시 `AppLanguage` enum 경유. 하드코딩된 문자열 금지.

### planned (Phase 2)

- [ ] 일기 저장 시 단어 자동 추출 및 사용자 승인 플로우.
- [ ] `FlashCardStudy` 라우트 활성화 — 세션 모드, 즐겨찾기/낮은 숙련도 우선.
- [ ] `nextReviewAt` 활용 — SM-2 lite 간격 반복.
- [ ] 카드 TTS 재생 (각 언어별 단어 발음).
- [ ] 예문 자동 채움 (`exampleSentences` 필드 활성화).
- [ ] 카드 편집 (단어/번역 직접 수정).

---

## Phase 2 후보

`08-roadmap.md`에서 관리. 요약:

1. **자동 단어 추출** — ML Kit Entity Extraction 또는 형태소 분석기로 후보 단어를 뽑아 사용자 승인 후 저장.
2. **학습 세션 모드** — `FlashCardStudy` 라우트 활성화. 세션 종료 후 통계.
3. **간격 반복** — `nextReviewAt`을 실제로 활용. SM-2 lite.
4. **예문 자동 생성** — 원문 일기 문장에서 해당 단어가 포함된 문장을 예문으로 자동 채움.

---

## Open Questions

- **TTS 연동**: 카드 상세(공개 상태)에서 각 언어 번역 단어를 TTS로 들을 수 있어야 하는가? Phase 2에서 다루는 것으로 잠정 결정. 우선순위가 높다면 이번 사이클에 포함 가능.
- **카드 추가 UI 구현 방식**: Compose `SelectionContainer` + `ContextMenuArea` 조합 가능 여부를 실제 구현에서 검증 필요. 텍스트 선택 API가 제한적일 경우 "선택된 텍스트를 TextField에 수동 입력하는 다이얼로그" 방식으로 폴백 허용 여부를 coder가 판단하여 사용자에게 확인.
- ~~**단어 추가 UI 위치**~~ — 컨텍스트 메뉴 방식으로 확정 (2026-04-18).
- ~~**번역 재사용**~~ — ML Kit 단어 단위 직접 호출로 확정 (2026-04-18).
- ~~**카드 중복**~~ — 별도 카드로 추가(병합 없음)로 확정 (2026-04-18).
