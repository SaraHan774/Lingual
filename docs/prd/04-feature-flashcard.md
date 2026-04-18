# 04 — Feature: FlashCard (단어장)

[← 목차로](../../PRD.md) · **status: shipped (manual) / planned (auto-extract)**

## Goal

사용자가 일기에서 익힌 단어/표현을 **플래시카드 덱**으로 축적하고, 숙련도 기반 복습을 통해 장기 기억으로 넘기도록 돕는다. 각 카드는 4개 언어 번역과 선택적 예문을 담아 한 카드에서 다국어 학습이 완결되도록 한다.

## Non-Goals

- 간격 반복 알고리즘(SM-2, Anki 방식)의 정교한 구현 — v0.1은 0–3 스케일의 단순 숙련도만.
- 학습 세션 UI("스터디 모드") — v0.1은 카드 리스트 + 탭하여 뜻 보기만.
- 카드 공유/마켓 — 개인 덱 전용.

## User Stories

- **추가 (수동)**: 사용자로서, 일기 상세에서 단어를 선택해 카드로 저장할 수 있다. *(현재 UI 미구현 — Open Question)*
- **학습**: 사용자로서, 단어장 탭에서 카드 목록을 보고 탭하면 뜻이 공개되며 숙련도 Lv 0–3을 매길 수 있다.
- **즐겨찾기**: 사용자로서, 하트 아이콘으로 카드를 즐겨찾기 하여 우선 복습할 수 있다.
- **발음**: 사용자로서, 카드의 각 언어 번역을 TTS로 들을 수 있다. *(상세 화면 동일 로직 — Open Question)*

## UX Flow

`FlashCardScreen` 기본 상태.

```
[FlashCard Tab]
  └─ 비어 있음 메시지: "아직 단어 카드가 없습니다. 일기에서 단어를 추가해 보세요."
  └─ 카드 있음: LazyColumn
        └─ 카드 탭 → revealed 토글
              ├─ 미공개: "탭하여 뜻 보기"
              └─ 공개: 원문 외 3개 언어 번역 + Lv 0~3 Chip (숙련도 업데이트)
```

상세 학습 세션(`FlashCardStudy` 라우트)은 네비게이션에 정의되어 있으나 현재 구현 미연결 — `08-roadmap.md` Phase 2 참조.

## Data Model

`WordCardEntity` + `WordCard` (domain).

| 필드 | 타입 | 의미 |
|------|------|------|
| `id` | String | UUID |
| `sourceEntryId` | String? | 출처 일기 ID. FK SET NULL (일기 삭제 시 카드는 유지) |
| `word` | String | 학습 대상 단어/표현 |
| `sourceLanguage` | AppLanguage | 원문 언어 |
| `translations` | Map<AppLanguage, String> (JSON) | 최대 4개 언어 번역 |
| `exampleSentences` | Map<AppLanguage, String>? (JSON) | 선택적 예문 |
| `masteryLevel` | Int (0–3) | 숙련도 |
| `nextReviewAt` | Long? | 다음 복습 시각 (현재 미사용) |
| `reviewCount` | Int | 복습 횟수 |
| `isFavorite` | Boolean | 즐겨찾기 |

JSON 필드는 **반드시** 명시적 `MapSerializer(String.serializer(), String.serializer())`를 사용한다. `Json.encodeToString(value)` reified 오버로드를 쓰면 컴파일이 깨진다 (CLAUDE.md 참조).

## Acceptance Criteria

- [x] 카드를 수동으로 저장하면 단어장 탭에서 즉시 보인다 (Flow 구독).
- [x] 카드를 탭하면 번역이 공개되고, Lv 0–3 Chip으로 숙련도를 업데이트하면 `reviewCount`가 1 증가한다.
- [x] 즐겨찾기 토글은 아이콘이 즉시 반영되고 영속화된다.
- [ ] **일기 상세에서 단어 선택 → 카드 생성** UI가 구현된다. *(현재 미구현)*
- [ ] **카드 삭제/편집** UI가 구현된다.
- [ ] 자동 단어 추출(Phase 2): 일기 저장 시 빈도/난이도 기준으로 후보 단어를 제안한다.

## Phase 2 후보

`08-roadmap.md`에서 관리. 요약:

1. **자동 단어 추출** — ML Kit Entity Extraction 또는 형태소 분석기로 후보 단어를 뽑아 사용자 승인 후 저장.
2. **학습 세션 모드** — `FlashCardStudy` 라우트 활성화. 즐겨찾기/숙련도 낮음 카드 우선, 세션 종료 후 요약.
3. **간격 반복** — `nextReviewAt`을 실제로 활용. SM-2 lite.
4. **예문 자동 생성** — 원문 일기 문장에서 해당 단어가 포함된 문장을 예문으로 자동 채움.

## Open Questions

- **카드 추가 UI의 위치**: 일기 상세에서 텍스트 선택 → 컨텍스트 메뉴 방식이 자연스러운가, 별도 "+ 단어" 버튼이 나은가?
- **번역 재사용**: 이미 Translation 레코드가 있는 일기에서 단어를 추출할 때, 단어 단위 번역을 다시 ML Kit으로 호출할 것인가, 문장 번역에서 추출할 것인가?
- **카드 중복**: 동일 단어가 여러 일기에서 선택될 때 기존 카드의 `reviewCount`를 늘릴지, 별도 카드로 둘지.
