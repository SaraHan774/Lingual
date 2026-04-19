---
slug: flashcard-ui-ux
request: "단어장(FlashCard) 화면 UI/UX 개선: 필터 칩, 숙련도 Chip 레이블, 숙련도 시각화, 통계 배너, Empty state, 삭제 버튼 조건 표시, 언어 AssistChip 배지"
started: "2026-04-18 00:00"
max_iter: 3
stage: done
iter: {prd: 2, prd-review: 2, code: 1, review: 1, qa: 1}
last_verdict: "PASS (stage=qa, iter=1)"
feedback:
  prd: |
    [iter 1 — from prd-reviewer, 2026-04-18]
    [필수 — CRITICAL]
    1. nextReviewAt 충돌 해결 (Data Model 섹션 + AC-10):
       "현재 미사용, Phase 2"로 표시된 nextReviewAt과 AC-10("복습 예정" 필터)이 충돌.
       옵션 A (권장): v0.1에서 숙련도 선택 시 고정 간격으로 nextReviewAt 설정.
         masteryLevel 0→+1일, 1→+3일, 2→+7일, 3→+30일. "현재 미사용" 표시 제거.
       옵션 B: AC-10을 planned로 내리고 UX Flow B에서 "복습 예정" 칩 제거.
    [권고 — NEEDS_REVISION]
    2. AC-11에 "숙련도 Chip 선택 즉시 서브헤더의 reviewCount UI 갱신" 추가.
    3. AC-15에 언어별 AssistChip 색상 팔레트 최소 정의 (KO=파랑, EN=녹색, JA=빨강, ZH=주황 계열).
    4. UX Flow B 공개 섹션에 "숙련도 Chip 선택 후 카드 자동 접힘 여부" 명시.
    5. AC-13 통계 배너에 "복습 예정 K개" 포함 여부 결정 또는 Open Questions 기재.
  prd-review: ~
  code: |
    [iter 1 — from prd-reviewer, 2026-04-18]
    AC-16 + AC-10: 숙련도 선택 → nextReviewAt 저장 후 DueForReview 필터 Flow 체인 즉시 반응 확인
    AC-13 배너: N=0 시 배너 숨김을 구현 재량으로 처리
    AC-15 언어 배지: 다크 테마 명암비 WCAG 4.5:1 이상 확인
    AC-12 숙련도 시각화: masteryLevel 0=모두 빈, 3=모두 채움 매핑 일관성
  review: |
    [iter 1 — from code, 2026-04-18]
    1. filteredCards Flow 체인: updateMastery → nextReviewAt 저장 → DueForReview 필터 즉시 반영 (combine 기반)
    2. stats Flow: cards 단독 파생, 즐겨찾기/숙련도/삭제 시 즉시 갱신
    3. MasteryDots 비활성 스타일: surfaceVariant 채움 원 (outline 대신) — 다크 테마 가독성 우선
    4. LanguageBadge: Surface+Text(code 대문자) — AssistChip 대신 클릭 불가 배지
    5. 삭제 아이콘: revealed 블록 하단 우측으로 이동
  qa: |
    [iter 1 — from review, 2026-04-18]
    1. 필터 전환 즉시 반영 (All↔Favorites↔DueForReview), 숙련도 선택 직후 DueForReview 반응
    2. 숙련도 선택 → reviewCount +1 즉시 UI 반영 (AC-11)
    3. 통계 배너 갱신 (N/M/K) — 카드 추가/삭제/즐겨찾기/숙련도 선택 후
    4. 다크 테마 LanguageBadge 4색 대비 WCAG 확인
    5. revealed=false 삭제 아이콘 비노출, revealed=true에서만 노출 (AC-5)
    6. 숙련도 선택 후 카드 자동 접힘 없음, 카드 바깥 탭 시 접힘은 정상
---

<!-- History: append-only -->

### 2026-04-18 — prd (iter 1): PASS → prd-review. AC-9~AC-15 신규 추가, AC-5 수정, UX Flow B 전면 개정.
### 2026-04-18 — prd-review (iter 1): NEEDS_SPEC → prd. CRITICAL: nextReviewAt "Phase 2 미사용" vs AC-10 "복습 예정" 필터 충돌. 권고 4건.
### 2026-04-18 — prd (iter 2): PASS → prd-review. 옵션A 채택(nextReviewAt 자동 설정), AC-16 신규, AC-11/13/15 수정.
### 2026-04-18 — prd-review (iter 2): PASS → code. CRITICAL 없음, 구현 주안점 5건 전달.
### 2026-04-18 — code (iter 1): PASS → review. FlashCardViewModel.kt, FlashCardScreen.kt 수정. AC-5/9~16 구현 완료, 빌드 통과.
### 2026-04-18 — review (iter 1): PASS → qa. BLOCKER 없음, MINOR 4건 (revealedMap hoisting, MasteryDots background, FilterChipRow 다국어 폭, masteryLabels strings.xml 이관).
### 2026-04-18 — qa (iter 1): PASS → done. AC-5/9~16 모두 AVD Pixel 9 Pro(API 35) E2E 통과. 스크린샷: test-artifacts/qa-flashcard-ui-20260418-235716/
