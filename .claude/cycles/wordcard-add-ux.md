---
slug: wordcard-add-ux
request: "단어 카드 추가 UX 전면 교체 — 현재 Copy 버튼 납치(클립보드 프로브) 방식 제거. 방안 A(권장): 원문 탭 상단 '+' 아이콘 → TextField 입력 방식으로 전환."
started: "2026-04-18 00:00"
max_iter: 3
stage: done
iter: {prd: 1, prd-review: 1, code: 2, review: 2, qa: 3}
last_verdict: "PASS (stage=qa, iter=3)"
feedback:
  prd: ~
  prd-review: ~
  code: |
    [iter 1 — from prd-reviewer, 2026-04-18]
    1. 로딩 상태: [저장] 탭 후 버튼 비활성화, 이중 탭 방지
    2. 번역 실패: BottomSheet 유지 + 인라인 에러 텍스트 + 버튼 복원 (조용한 에러 저장 금지)
    3. 공백 입력: trim() 후 비어 있으면 [저장] 비활성
    4. 시트 닫기 시 번역 코루틴 취소 (고아 카드 저장 방지)
    5. 아이콘 버튼: 원문 탭에서만 표시, 탭 전환 시 깜박임 없음

    [iter 2 — from qa, 2026-04-18]
    F1 FAIL: 긴 일기 스크롤 시 BookmarkAdd 아이콘이 본문 카드 내부에 배치되어 뷰포트 밖으로 사라짐.
    - 재현: 약 2500자 본문 일기 → 원문 탭 스크롤 → 아이콘 소멸 (재현율 2/2)
    - 기대: PRD AC-1 "항상 표시"
    - 수정 방향: 탭 Row와 같은 Row에 AnimatedVisibility(isSourceTab) { IconButton } 추가 (스크롤 밖 sticky)
    나머지 AC-1/2/3/4, F2/F3/F4/F5 모두 PASS
  review: |
    [iter 1 — from code, 2026-04-18]
    1. dismiss + saveWordCard 코루틴 레이스 가능성 확인
    2. "다시 시도" 버튼 canSave 가드 안전성 확인
    3. 아이콘 버튼이 탭 헤더 대신 본문 카드 내부에 배치됨 (번역 탭 숨김 로직 단순화 목적)

    [iter 2 — from code, 2026-04-18]
    1. AnimatedVisibility 전이 시 TabRow 너비 변화로 탭 indicator 흔들림 가능성
    2. 탭+아이콘 Row가 스크롤 밖에 있어 긴 제목 시 화면 점유 과다 가능성
    3. 접근성 포커스 순서 (탭 → 아이콘 → 본문)
  qa: |
    [iter 1 — from review, 2026-04-18]
    1. [F3 레이스] 저장 중(번역 스피너) 시트 닫기 → 스낵바/고아 카드 재현 여부
    2. [F4 FocusRequester] 저사양 에뮬레이터 + 한국어 IME 반복 열기 crash 여부
    3. [F1 발견성] 긴 일기에서 BookmarkAdd 아이콘이 스크롤로 뷰포트에서 밀리는지
    4. [F2 무응답] "???" 또는 "123" 입력 후 [저장] 탭 시 무응답 확인
    5. [F5 재오픈] 드래그 다운 후 재오픈 시 입력 초기화 확인

    [iter 2 → iter 3 — from review, 2026-04-18]
    1. [핵심] 긴 일기 스크롤 후 BookmarkAdd 아이콘 뷰포트 유지 확인 (F1 재현 실패 확인)
    2. 탭 전환 시 AnimatedVisibility fade 중 탭 인디케이터 흔들림 육안 판단
    3. 작은 화면/긴 제목 시 상단 고정 영역 과다 점유 여부
    4. TalkBack 탐색 순서: 탭 → 아이콘 → 본문
---

<!-- History: append-only -->

### 2026-04-18 — prd (iter 1): PASS → prd-review. 방안 A 확정: 아이콘 버튼+TextField BottomSheet. AC-1/AC-2/User Story/UX Flow A 교체, SelectionContainer/TextToolbar 폐기 결정.
### 2026-04-18 — prd-review (iter 1): PASS → code. CRITICAL 없음, 구현 주안점 5건 전달.
### 2026-04-18 — code (iter 1): PASS → review. WordCardSelectableText/클립보드 프로브 제거, BookmarkAdd 아이콘 버튼 + OutlinedTextField BottomSheet 구현, 빌드 통과.
### 2026-04-18 — review (iter 1): PASS → qa. BLOCKER 없음. F3 레이스, F4 FocusRequester crash 가능성 QA에서 확인 필요.
### 2026-04-18 — qa (iter 2): NEEDS_CODE → code. 12/13 PASS, F1 FAIL: 긴 일기 스크롤 시 BookmarkAdd 아이콘 뷰포트 밖 사라짐. 증거: test-artifacts/qa-wordcard-ux-iter2/26-very-long-scrolled.png
### 2026-04-18 — code (iter 2): PASS → review. BookmarkAdd 아이콘을 탭 헤더 Row(스크롤 밖)로 이전, AnimatedVisibility(isSourceSelected), 빌드 통과.
### 2026-04-18 — review (iter 2): PASS → qa. BLOCKER 없음. F1 위치 이전 확인. Follow-ups: i18n 하드코딩, ViewModel 단위 테스트.
### 2026-04-18 — qa (iter 3): PASS → done. F1 재확인 PASS: 스크롤 후 아이콘 [1208,504] 유지. 탭 인디케이터 흔들림 없음. smoke test 성공. 크래시 없음.
