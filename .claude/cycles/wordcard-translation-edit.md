---
slug: wordcard-translation-edit
request: "번역 오류 수정 기능 — 단어카드에서 ML Kit 번역이 의도치 않게 잘못된 경우 사용자가 직접 번역을 편집·수정할 수 있는 기능. 단어 학습 관점에서 오역 수정 + 올바른 번역 확인이 핵심 가치."
started: "2026-04-19 00:00"
max_iter: 3
stage: done
iter: {prd: 2, prd-review: 2, code: 1, review: 1, qa: 1}
last_verdict: "PASS (stage=qa, iter=1)"
feedback:
  prd: |
    [iter 1 — from prd-review, 2026-04-19]
    docs/prd/04-feature-flashcard.md의 UX Flow D 및 AC-17~AC-21에 아래 4건을 추가/수정해야 한다.

    [수정 요청 1] AC-19에 에러 상태 추가
    "저장 실패 시 에러 스낵바('번역 수정에 실패했습니다. 다시 시도해 주세요.')를 표시하고
    BottomSheet는 닫지 않는다." 조항을 AC-19에 추가.

    [수정 요청 2] AC-19 또는 UX Flow D에 수정 불가역성 고지 방식 결정 명기
    [저장] 버튼 탭 시 ML Kit 원본 번역이 덮어써지며 v0.1에서 복원 불가인 파괴적 동작임.
    권장: BottomSheet 내 보조 텍스트로 "수정 후 ML Kit 원본 번역을 복원할 수 없습니다" 1줄 경고 표시.
    prd-curator가 UX Flow D의 BottomSheet 구성 항목에 해당 경고 텍스트를 추가하고 AC-19에 반영.

    [수정 요청 3] AC-17 또는 신규 AC에 ML Kit 번역 실패 언어 및 번역 전무 카드 처리 명기
    (a) 번역값이 null 또는 빈 문자열인 언어에서 편집 아이콘 표시 여부를 결정:
        - 허용 시: 저장 버튼 비활성화 조건에서 "비어 있으면 비활성화" 조항을 해당 케이스에서 제외
        - 불허 시: 해당 언어 행 미표시로 명기
    (b) 번역 항목이 하나도 없는 카드의 공개 상태 표시 방식을 결정:
        - 권장안: "번역 없음" 플레이스홀더 행 3개 표시 + 편집 아이콘으로 직접 입력 가능

    [수정 요청 4] AC-20 Open Question 해소
    Open Questions의 "수정 인디케이터 위치" 이슈를 결정 사항으로 확정.
    권장: 공개 상태에서만 표시 (헤더 복잡도 최소화).
  prd-review: ~
  code: ~
  review: ~
  qa: ~
---

<!-- History: append-only -->
### 2026-04-19 — prd (iter 1): PASS → prd-review. AC-17~AC-21 추가, EditTranslationBottomSheet UX Flow D 확정, isTranslationEdited 필드 추가.
### 2026-04-19 — prd-review (iter 1): NEEDS_SPEC → prd. CRITICAL: 에러 상태 누락, 수정 불가역성 미고지, 번역 실패 엣지케이스 3건, Open Question 미확정.
### 2026-04-19 — prd (iter 2): PASS → prd-review. AC-18~AC-23 갱신, 수정 불가역성 경고, 번역 실패 언어 처리, 인디케이터 위치 확정.
### 2026-04-19 — prd-review (iter 2): PASS → code. CRITICAL 없음. 4건 모두 해소 확인.
### 2026-04-19 — code (iter 1): PASS → review. AC-17~23 구현 완료, 빌드 게이트 통과.
### 2026-04-19 — review (iter 1): PASS → qa. BLOCKER 없음. MINOR 4건(scope.launch 중복, !== vs !=, isNullOrBlank 통일, 포커스 타이밍).
### 2026-04-19 — qa (iter 1): PASS → done. AC-17~21 실기 검증 완료. AC-22/23 코드 경로 확인(실기 재현 제한). 크래시 없음.
