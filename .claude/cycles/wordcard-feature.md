# Cycle: wordcard-feature

- **Request**: 단어장 기능을 구현해줘.
- **Started**: 2026-04-18 00:00
- **Max iterations per stage**: 3

## State
- **Current stage**: done
- **Iterations**:
  - prd: 1
  - code: 2
  - review: 2
  - qa: 3
- **Last verdict**: PASS (qa iter 3 — AC-4 회전 replay 없음, AC-1 구두점 차단 확인)

## Accumulated Feedback
<!-- 각 스테이지 에이전트를 호출할 때 실어 보낼 최신 피드백. 해당 스테이지로 다시 돌아올 때마다 덮어쓴다. -->

### For prd
(none)

### For code
QA iter 2 결과 2개 버그 수정 필요:

1. **[P0] AC-4 스낵바 회전 replay 버그**
   - 원인: `DiaryDetailViewModel.kt:68` `MutableStateFlow<Int>` 는 latest 값(1) 보존 → 회전 후 Composition 재생성 시 `LaunchedEffect(savedEvents=1)` 재실행 → 스낵바 중복 노출
   - 수정: `DiaryDetailViewModel.kt:68,176` → `MutableSharedFlow<Unit>(replay=0, extraBufferCapacity=1)`, `emit(Unit)` 사용. `DiaryDetailScreen.kt:76,80` → `LaunchedEffect(Unit) { vm.wordCardSavedEvents.collect { showSnackbar(...) } }`

2. **[P1] AC-1 구두점/빈 단어 저장 방어 누락**
   - 원인: `DiaryDetailViewModel.kt:114` `requestAddWordCard` 가 `word.isBlank()` 만 확인 → 구두점(. , ! 등)만 선택된 경우 통과
   - 수정: `if (word.trim().none { it.isLetter() }) return` 추가

Android 12+ 클립보드 토스트는 기술적 근본 해결이 어렵고 PRD에 명시 없음 → 이번 iter에서는 보류 (F3 minor로 남김).

### For review
code iter 2 수정 사항 리뷰:
1. DiaryDetailViewModel.kt:68-71 — `MutableStateFlow<Int>` → `MutableSharedFlow<Unit>(replay=0, extraBufferCapacity=1)` 교체 적절성 확인.
2. DiaryDetailViewModel.kt:180 — `tryEmit(Unit)` 호출 시점 및 coroutine scope 확인.
3. DiaryDetailScreen.kt:76-84 — `LaunchedEffect(Unit) { collect { showSnackbar } }` 패턴의 수명 주기 안전성 확인.
4. DiaryDetailViewModel.kt:118 — `word.none { it.isLetter() }` 가드 Unicode 정확성 확인 (한/영/일/중 통과, 구두점 차단).
5. 빌드 성공 확인됨. 다른 코드 변경 없음.

### For qa
review iter 2 PASS. code iter 2 버그 패치 검증:
1. **AC-4 회전 replay 수정 확인** — 단어 카드 저장 → 스낵바 소멸 → 화면 회전 → 스낵바 재노출 없어야 PASS. (이전 qa iter 2에서 FAIL, SharedFlow 교체로 수정됨)
2. **AC-1 구두점 저장 차단 확인** — 본문에서 "." / ", " / "!?" 기호만 선택 후 Copy → BottomSheet가 열리지 않아야 PASS. (이전 qa iter 2에서 FAIL, `word.none{it.isLetter()}` 가드 추가됨)
3. **AC-1 정상 입력** — 한/영/일/중 실문자 단어 선택 → BottomSheet 정상 노출 및 저장.
4. AC-5/6/7 은 이전 iter에서 이미 PASS — 재확인 선택.

스크린샷 최소화 규칙 동일: 꼭 필요한 단계에서만, 3장 이상 연속 캡처 금지.

## History
<!-- 호출 결과 append-only. -->
- 2026-04-18 prd iter 1: PASS. `docs/prd/04-feature-flashcard.md` 갱신 (status: in-progress, AC-1~AC-8 확정, Open Questions 3건 결정). `PRD.md` TOC 라벨 갱신. `08-roadmap.md` 의사결정 로그 추가.
- 2026-04-18 code iter 1: PASS. 빌드 성공. DiaryDetailScreen 텍스트 선택→카드추가BottomSheet, FlashCardScreen 삭제 다이얼로그 구현. AC-1~AC-8 완료.
- 2026-04-18 review iter 1: PASS. BLOCKER 없음. MINOR 이슈: F1(TextToolbar UX), F2(StateFlow<Int> one-shot), F3(all-or-nothing 번역). QA에서 실기기 확인 필요.
- 2026-04-18 qa iter 1: BLOCKED_HUMAN. QA 에이전트가 스크린샷 이미지 크기 초과로 VERDICT 반환 실패. 수동 QA 또는 /compact 후 재개 필요.
- 2026-04-18 qa iter 2: NEEDS_CODE. AC-5/6/7 PASS. AC-4 스낵바 replay(StateFlow→SharedFlow 필요), AC-1 구두점 저장 방어 누락 FAIL. next=code.
- 2026-04-18 code iter 2: PASS. 스낵바 SharedFlow 교체(DiaryDetailViewModel:68,180 / DiaryDetailScreen:76-84), 구두점 방어(DiaryDetailViewModel:118). 빌드 성공. next=review.
- 2026-04-18 review iter 2: PASS. BLOCKER 없음. SharedFlow 교체 및 구두점 가드 정확성 확인. next=qa.
- 2026-04-18 qa iter 3: PASS. AC-4 회전 replay 없음(SharedFlow 검증), AC-1 구두점 차단 확인(letter 가드). 전 시나리오 PASS. 사이클 완료.
