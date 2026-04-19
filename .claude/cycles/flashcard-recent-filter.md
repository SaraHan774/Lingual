---
slug: flashcard-recent-filter
request: "FlashCard 필터 칩에 \"최근 추가 (7일 내)\" 옵션 추가. 기존 All/Favorites/DueForReview 와 동일한 FilterChipRow 패턴 재사용. 7일 기준은 WordCard.createdAt 사용."
started: "2026-04-19 20:34"
max_iter: 3
stage: done
iter: {prd: 1, prd-review: 1, code: 1, review: 1, qa: 1}
last_verdict: "PASS (stage=qa, iter=1)"
feedback:
  prd: ~
  prd-review: ~
  code: |
    [iter 1 — from prd-review, 2026-04-19]
    CRITICAL 없음. 구현 진행 가능. coder 에게 전달할 UX 주안점:

    1. [좁은 화면 레이아웃 — 중요] 기존 `FilterChipRow` 는 `Row + spacedBy(8.dp)` 로 고정 가로 배치다. 4번째 칩 "최근 추가" 추가 시 ≤360dp 화면(일본어 전각 레이블 포함) 에서 overflow 위험. 구현 시 `LazyRow` 로 변경하여 수평 스크롤 가능하게 할 것. 스크롤바 없이 자연스러운 swipe, 기본 화면(≥400dp)에서는 4개 모두 노출되도록 chip padding 유지.

    2. [Empty State 다음 액션 유도] "최근 추가" 필터 Empty State 문구 4개 언어에 다음 행동 제시를 덧붙일 것. 한국어 예: "최근 7일 내에 추가된 카드가 없습니다. 일기를 작성하고 단어 카드를 추가해 보세요." — 타 언어도 동일 패턴(영어: "Write a diary entry and add some word cards." / 일본어: "日記を書いて単語カードを追加してみましょう。" / 중국어: "写一篇日记并添加单词卡。").

    3. [필터 상태 생명주기] `filterState` 를 `SavedStateHandle` 로 복원하지 말 것. ViewModel 내부 `MutableStateFlow` 로만 관리하여 프로세스 재생성 시 기본값 `All` 로 리셋되도록 구현.

    4. [UX 원칙 일반] 칩 selected=filled, 비선택=outlined. AC-25 상호 배타성 준수. Flow 재방출 시점의 `System.currentTimeMillis()` 기준 평가(AC-24).

    QA 마커 권고: `Log.d("QA", "filter:RecentlyAdded selected")`, `Log.d("QA", "filter:RecentlyAdded results=<N>")` 를 debug 빌드 전용 삽입.

    PRD curator 구현 스코프 요약(iter 1):
    - `FlashCardFilter` enum (ui/flashcard/FlashCardViewModel.kt:22) 에 `RecentlyAdded` 추가.
    - `FlashCardViewModel` 필터 when 분기에 `card.createdAt >= System.currentTimeMillis() - 7L*24*60*60*1000L` 추가.
    - `FilterChipRow` (FlashCardScreen.kt:235) 에 네 번째 `FilterChip` 추가 → LazyRow 로 변경 검토.
    - Empty State 분기 (FlashCardScreen.kt:264~) 에 `RecentlyAdded` 케이스 추가.
    - 데이터 모델/Room 스키마 변경 없음 — `WordCard.createdAt` 이미 존재.
  review: ~
  qa: ~
---

<!-- History: append-only -->
### 2026-04-19 — prd (iter 1): PASS → prd-review. FlashCard "최근 추가 (7일 내)" 필터 칩 AC-9/14/24/25 추가, 4개 언어 레이블/Empty State 반영.
### 2026-04-19 — prd-review (iter 1): PASS → code. CRITICAL 없음. UX 주안점 3개(LazyRow, Empty State 액션 유도, 필터 상태 비영속) coder 에 전달.
### 2026-04-19 — code (iter 1): PASS → review. FlashCardFilter.RecentlyAdded 추가, Row→LazyRow, 4 로케일 strings.xml, QA 마커+buildConfig 활성화. 빌드 PASS.
### 2026-04-19 — review (iter 1): PASS → qa. BLOCKER/MAJOR 없음. F1/F2 는 Follow-up 이관(스코프 밖). QA 검증 포인트 7개 전달.
### 2026-04-19 — qa (iter 1): PASS → done. 7개 시나리오 전부 PASS. AC-9/14/24/25 완전 충족. Pixel 9 Pro AVD, ko-KR, 스크린샷 3장+QA 로그 증빙.
