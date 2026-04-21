---
slug: tab-color-indicator
request: "DiaryDetailScreen 언어 탭 색상 인디케이터 — SecondaryScrollableTabRow 각 탭 앞에 AppLanguage별 색상 점(8dp 원)을 추가한다. FlashCardScreen의 languageBadgeColor 함수를 공통 위치(예: ui/theme/LanguageColors.kt)로 이동해 DiaryDetailScreen과 FlashCardScreen이 함께 재사용한다. 참고: docs/ux-review-2026-04-19.md DD-01, F-01"
started: "2026-04-19 00:00"
max_iter: 3
stage: qa
iter: {prd: 2, prd-review: 1, code: 1, review: 1, qa: 1}
last_verdict: "PASS (stage=review, iter=1)"
feedback:
  prd: ~
  prd-review: ~
  code: ~
  review: ~
  qa: ~
---

<!-- History: append-only -->
### 2026-04-19 — prd (iter 2): PASS → prd-review. PRD docs/prd/02-feature-diary.md에 DD-01 AC-DD-01~05 완비 확인, docs/prd/04-feature-flashcard.md에 F-01 색상 팔레트 정의 확인.
### 2026-04-19 — prd-review (iter 1): PASS → code. CRITICAL 없음. 색상 팔레트 가독성 OK, AC 구체성 OK, 엣지케이스 OK.
### 2026-04-19 — code (iter 1): PASS → review. LanguageColors.kt 신규, FlashCardScreen 공통 import, DiaryDetailScreen TabLabel 색상 점 추가. 빌드 성공(12 executed).
### 2026-04-19 — review (iter 1): PASS → qa. Approve with comments. BLOCKER 없음. NIT 2건(import 빈줄, contentDescription).
