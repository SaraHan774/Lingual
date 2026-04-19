---
slug: wordcard-feature
request: "단어장 기능을 구현해줘."
started: "2026-04-18 00:00"
max_iter: 3
stage: done
iter: {prd: 1, prd-review: 0, code: 2, review: 2, qa: 3}
last_verdict: "PASS (stage=qa, iter=3)"
feedback:
  prd: ~
  prd-review: ~
  code: |
    [iter 1 — from qa, 2026-04-18]
    QA iter 2 결과 2개 버그 수정 필요:
    1. [P0] AC-4 스낵바 회전 replay 버그
       DiaryDetailViewModel.kt:68 MutableStateFlow<Int> → MutableSharedFlow<Unit>(replay=0, extraBufferCapacity=1)
       DiaryDetailScreen.kt:76,80 → LaunchedEffect(Unit) { vm.wordCardSavedEvents.collect { showSnackbar(...) } }
    2. [P1] AC-1 구두점/빈 단어 저장 방어 누락
       DiaryDetailViewModel.kt:114 → if (word.trim().none { it.isLetter() }) return 추가
    Android 12+ 클립보드 토스트는 근본 해결 어려움 + PRD 미명시 → 보류 (F3 minor).
  review: |
    [iter 1 — from code, 2026-04-18]
    code iter 2 수정 사항 리뷰:
    1. DiaryDetailViewModel.kt:68-71 — StateFlow→SharedFlow 교체 적절성
    2. DiaryDetailViewModel.kt:180 — tryEmit(Unit) 호출 시점 및 coroutine scope
    3. DiaryDetailScreen.kt:76-84 — LaunchedEffect(Unit) collect 패턴 수명 주기 안전성
    4. DiaryDetailViewModel.kt:118 — word.none{it.isLetter()} 가드 Unicode 정확성 (한/영/일/중 통과, 구두점 차단)
    5. 빌드 성공 확인됨. 다른 코드 변경 없음.
  qa: |
    [iter 1 — from review, 2026-04-18]
    review iter 2 PASS. code iter 2 버그 패치 검증:
    1. AC-4 회전 replay 수정 확인 — 저장→스낵바 소멸→회전→스낵바 재노출 없어야 PASS
    2. AC-1 구두점 저장 차단 확인 — "." / ", " / "!?" 기호만 선택 후 BottomSheet가 열리지 않아야 PASS
    3. AC-1 정상 입력 — 한/영/일/중 실문자 단어 선택→BottomSheet 정상 노출 및 저장
    4. AC-5/6/7 은 이전 iter에서 이미 PASS — 재확인 선택
---

<!-- History: append-only -->

### 2026-04-18 — prd (iter 1): PASS → prd-review. docs/prd/04-feature-flashcard.md 갱신 (AC-1~AC-8 확정, Open Questions 3건 결정).
### 2026-04-18 — code (iter 1): PASS → review. DiaryDetailScreen 텍스트 선택→카드추가BottomSheet, FlashCardScreen 삭제 다이얼로그 구현. AC-1~AC-8 완료.
### 2026-04-18 — review (iter 1): PASS → qa. BLOCKER 없음. MINOR: F1(TextToolbar UX), F2(StateFlow one-shot), F3(all-or-nothing 번역).
### 2026-04-18 — qa (iter 1): BLOCKED_HUMAN. 스크린샷 이미지 크기 초과로 VERDICT 반환 실패. 수동 QA 또는 /compact 후 재개 필요.
### 2026-04-18 — qa (iter 2): NEEDS_CODE → code. AC-5/6/7 PASS. AC-4 스낵바 replay(StateFlow→SharedFlow 필요), AC-1 구두점 저장 방어 누락 FAIL.
### 2026-04-18 — code (iter 2): PASS → review. 스낵바 SharedFlow 교체(DiaryDetailViewModel:68,180 / DiaryDetailScreen:76-84), 구두점 방어(DiaryDetailViewModel:118). 빌드 성공.
### 2026-04-18 — review (iter 2): PASS → qa. BLOCKER 없음. SharedFlow 교체 및 구두점 가드 정확성 확인.
### 2026-04-18 — qa (iter 3): PASS → done. AC-4 회전 replay 없음(SharedFlow 검증), AC-1 구두점 차단 확인(letter 가드). 전 시나리오 PASS.
