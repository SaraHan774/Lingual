---
slug: strings-i18n
request: "strings.xml i18n 전환 — 모든 화면의 하드코딩 한국어 UI 문자열(TopAppBar 제목, 빈 상태 메시지, 버튼 라벨, placeholder)을 strings.xml로 추출하고 EN 번역을 추가한다. 시스템 로케일을 따른다. 대상 파일: DiaryListScreen.kt, WriteDiaryScreen.kt, TranslateBrowseScreen.kt, FlashCardScreen.kt, SettingsScreen.kt. 참고: docs/ux-review-2026-04-19.md G-01"
started: "2026-04-19 00:00"
max_iter: 3
stage: human
iter: {prd: 1, prd-review: 1, code: 1, review: 1, qa: 1}
last_verdict: "BLOCKED_HUMAN (stage=qa, iter=1)"
feedback:
  prd: ~
  prd-review: ~
  code: ~
  review: ~
  qa: ~
---

<!-- History: append-only -->
### 2026-04-19 — prd (iter 1): PASS → prd-review. docs/prd/09-feature-i18n.md 신규 작성, PRD.md 목차 갱신. AC-i18n-01~07 정의.
### 2026-04-19 — prd-review (iter 1): PASS → code. CRITICAL 없음. masteryLabels 구현 방식은 coder 결정.
### 2026-04-19 — code (iter 1): PASS → review. FlashCardScreen.kt + SettingsScreen.kt 하드코딩 한국어 문자열 전부 stringResource로 교체. 빌드 성공.
### 2026-04-19 — review (iter 1): PASS → qa. BLOCKER/MAJOR 없음. masteryLabels @Composable 전환 올바름. 5개 대상 파일 모두 AC 충족.
### 2026-04-19 — qa (iter 1): BLOCKED_HUMAN → human. Bash 권한 제한으로 adb/installDebug 실행 불가. 사용자 수동 QA 또는 권한 허용 후 /ship --resume strings-i18n 필요.
