---
slug: translate-tab-redesign
request: "번역 탭(Translate)을 재설계한다. 현재 번역 탭의 존재 이유가 불명확함 — 일기 작성 시 자동 번역되고 DiaryDetail에서 탭으로 확인 가능한데, 별도 Translate 탭의 역할이 중복/모호. prd-curator와 prd-reviewer를 통해 (1) 번역 탭이 제공해야 할 고유 가치를 정의하고 (2) 일기 플로우와 겹치지 않는 새 화면으로 탈바꿈시킬 것. 후보 방향(참고용, 확정 아님): 실시간 프리뷰 번역기 / 학습용 문장 번역 연습 / 외부 텍스트 번역 유틸리티 / 단어장과 연동된 번역 히스토리 등. PRD 단계에서 방향을 확정한 뒤 구현."
started: "2026-04-21 00:30"
max_iter: 3
stage: done
iter: {prd: 2, prd-review: 2, code: 1, review: 1, qa: 1}
last_verdict: "PASS (stage=qa, iter=1)"
feedback:
  prd: |
    [iter 1 — from prd-review, 2026-04-21]
    CRITICAL 1건 — AC-T02-24 의 [+] 버튼 표시-동작 불일치. 카드 안에 위치하면서 "전 Success 일괄 저장" 동작은 사용자 멘탈 모델과 충돌하며 "내가 저장한 적 없는 언어가 왜 카드에 있지?" 라는 신뢰 깨짐을 유발한다.

    필수 수정 (옵션 A 권장):
    - docs/prd/03-feature-translation.md "UX: Translate Playground" → 화면 레이아웃 ASCII 다이어그램에서 카드 헤더의 [+] 제거. 입력 영역과 결과 카드 사이(또는 화면 최하단)에 풀폭 [단어 카드로 저장 (N개 언어 포함)] 버튼을 추가. N은 현재 Success 상태인 대상 언어 수.
    - "단어 카드로 저장 액션 (카드의 [+] 버튼)" 섹션을 "단어 카드로 저장 액션 (풀폭 버튼)" 으로 제목·본문 갱신. [+] 가 카드별이라는 표현 전부 제거.
    - AC-T02-19 → 카드별 우측 상단 액션을 [♪] TTS 만으로 축소. [+] 제거.
    - AC-T02-20 → 풀폭 버튼 탭 시 동작으로 재작성. 활성화 조건: "최소 1개 대상 언어가 Success 상태". 비활성: 모두 Idle/Loading/Error 일 때 (alpha 0.38).
    - AC-T02-24 → "버튼 위치는 카드 외부 풀폭이며 항상 모든 Success 결과를 일괄 저장한다. 카드별 개별 저장 개념은 v0.1에 없다." 로 단순화.
    - 신규 i18n 키 추가: translate_playground_save_button_label = "단어 카드로 저장 (%1$d개 언어)" / "Save as word card (%1$d languages)".

    권고 (CRITICAL 아님, 시간 여유 있으면 함께 반영):
    - AC-T02-15 — 원문 언어 변경 시 150ms 짧은 디바운스로 ML Kit 호출 폭증 방지.
    - AC-T02-21 — 저장 성공 스낵바에 "FlashCard 보기" Action 버튼 추가(라우팅 Screen.FlashCard).
    - AC-T02-04 — 180자 초과 시 글자수 카운터 색상 error 로 변경 (200자 도달 직전 친절한 경고).
    - 신규 AC-T02-28 — Empty State 1줄 안내(첫 사용 시 ML Kit 모델 다운로드 약 30MB 안내).
    - 신규 AC-T02-29 — TTS 단일 채널: 카드 A 재생 중 카드 B [♪] 탭 시 A 정지 후 B 재생.
    - 08-roadmap.md P1 — "Diary 탭 원문 언어 필터 (T-02 회수분)" 한 줄 추가. 03 의 "Diary 탭에 흡수돼도 기능 손실 없음" 주장과 정합성 맞추기 위해.

    중점 검토 지점 결론(참고): 200자 상한 적정 / 디바운스 500ms 적정 / alpha 0.5 outdated 마커 유지 / 원문==대상 충돌 정책 충분 / 저장 후 입력 유지 정책 유지 / TranslateBrowse 제거 시 필터 손실은 로드맵 P1 회수.
  prd-review: ~
  code: |
    [iter 0 — from prd-review iter 2, 2026-04-21]
    iter 1 의 CRITICAL (AC-T02-24 [+] 표시-동작 불일치) 해소 확인. 표시(풀폭 단일 저장 버튼 + N개 라벨)와 동작(N개 Success 언어 일괄 저장)이 1:1 일치하는 단일 멘탈 모델로 통일. 권고 5건 + 로드맵 P1 #13 모두 반영. 새 CRITICAL 없음.

    coder 가 구현 중 지켜야 할 UX 디테일 (11개):
    1) 풀폭 저장 버튼: imePadding + 결과 영역 verticalScroll. successCount >= 1 시 enabled, 그 외 alpha 0.38.
    2) translate_playground_save_button_label 포맷 인자에 successCount 정확히 전달. 작은 화면에서 maxLines=1 + Ellipsis 또는 두 줄 허용 결정.
    3) AC-T02-19 — 카드 우측 상단에 [♪] TTS 만. 카드별 [+] 절대 추가 금지(iter 1 CRITICAL 핵심).
    4) AC-T02-21 "FlashCard 보기" Action — navController.navigate(Screen.FlashCard.route) { launchSingleTop = true } 로 백스택 유지.
    5) AC-T02-28 Empty State 헬퍼는 결과 카드 영역 "하단" 배치. Loading/Success/Error 한 번이라도 진입하면 영구 숨김.
    6) AC-T02-29 TTS 단일 채널 — 카드 A 재생 중 카드 B [♪] 탭 시 stop() 후 speak(). UtteranceProgressListener onDone/onError 비동기 주의.
    7) AC-T02-15 — 원문 언어 변경 시 150ms 디바운스(타이핑 500ms 와 별개 채널).
    8) AC-T02-04 — 180자 이상 카운터 colorScheme.error, 미만 onSurfaceVariant. 200자 take(200) 차단.
    9) 모든 UI 문자열 stringResource. values/strings.xml + values-en/strings.xml 동시 추가. 기존 translate_playground_save_card_cd 추가 금지.
    10) AC-T02-Q01 logcat QA 마커 5종 — BuildConfig.DEBUG 가드 + Log.d("QA", ...).
    11) AC-T02-R01 TranslateBrowseScreen.kt + ViewModel 삭제. AC-T02-R02 strings 의 translate_browse_* 3종은 deprecated 주석으로 남김. AC-T02-R03 09-feature-i18n.md 미수정.
  review: |
    [iter 0 — from code iter 1, 2026-04-21]
    구현 완료. 빌드 게이트 OK (./gradlew :app:assembleDebug BUILD SUCCESSFUL).

    변경 파일:
    - 신규: app/src/main/java/com/august/spiritscribe/ui/translate/TranslatePlaygroundScreen.kt
    - 신규: app/src/main/java/com/august/spiritscribe/ui/translate/TranslatePlaygroundViewModel.kt
    - 수정: app/src/main/java/com/august/spiritscribe/Navigation.kt
    - 수정: app/src/main/res/values/strings.xml (translate_playground_* 16개 + translate_browse_* 3개 deprecated 주석)
    - 수정: app/src/main/res/values-en/strings.xml
    - 삭제: ui/translate/TranslateBrowseScreen.kt + TranslateBrowseViewModel.kt

    code-reviewer 우선 검토 지점 8건 (coder 보고에서):
    1) toggleSpeak() — TtsService.speak() 호출 직후 ttsService.state.value 동기 read
    2) triggerTranslate() 메모 히트 시 Loading 단계 스킵, cancelAllJobs() race
    3) _resultsContext outdated 판정 (Pair 비교, 빈 입력 가드)
    4) saveWordCard() successByLang Map 구성 (Loading/Error/Idle 빠지는지)
    5) AC-T02-19 카드 헤더 [♪] 만 ([+] 절대 없음)
    6) AC-T02-04 200자 take(200) + 180자 카운터 색상
    7) AC-T02-Q01 5종 logcat 마커 (DEBUG 가드, 본문 미노출)
    8) Navigation.kt onNavigateToFlashCard launchSingleTop=true 백스택 유지
  qa: |
    [iter 0 — from review iter 1, 2026-04-21]
    Approve with comments. BLOCKER/MAJOR 없음. 빌드 OK.

    qa-tester 우선 점검 AC 15건 (실기 시나리오):
    - AC-T02-04 200자/180자 카운터 색
    - AC-T02-07 500ms 디바운스 3개 병렬
    - AC-T02-09/10/11 Success/Error 독립성 + 재시도
    - AC-T02-12 outdated alpha 0.5
    - AC-T02-13 입력 삭제 Idle
    - AC-T02-14 메모 히트 (logcat translateTrigger 있되 translateResult 없음)
    - AC-T02-15/E07 원문 언어 150ms + 동일 언어 no-op
    - AC-T02-17/18 지우기 버튼
    - AC-T02-19 카드 [♪] 만 [+] 부재 (CRITICAL)
    - AC-T02-20/21 풀폭 저장 + 스낵바 Action "FlashCard 보기" 백스택 유지
    - AC-T02-23 동일 단어 2회 저장 경고 없음
    - AC-T02-28 첫 사용 헬퍼 + 영구 숨김
    - AC-T02-29 TTS 단일 채널
    - AC-T02-Q01/Q02 logcat 5종 마커
    - AC-T02-R01/R02 파일 삭제 + strings 키 deprecated

    후속 사이클 권고 (이번 차단 아님):
    - MINOR F5 outdated 저장 정책 PRD-curator 협의
    - ViewModel 단위 테스트
    - 09-feature-i18n.md AC-i18n-03 갱신 (AC-T02-R03 의도적 보류)
---

<!-- History: append-only -->
### 2026-04-21 — prd (iter 1): PASS → prd-review. Translate Playground (실시간 프리뷰 + 카드 저장 + TTS) 방향 확정, AC 31개 + 기존 T-01 Superseded 처리.
### 2026-04-21 — prd-review (iter 1): NEEDS_SPEC → prd. CRITICAL 1건 — AC-T02-24 [+] 버튼이 카드 안에 위치하면서 "전 Success 일괄 저장" 동작은 표시-동작 불일치. 옵션 A(풀폭 버튼) 권장.
### 2026-04-21 — prd (iter 2): PASS → prd-review. 풀폭 저장 버튼으로 [+] 의미론 충돌 해소, 권고 5건도 함께 반영(AC-T02-04/15/21/28/29 + 로드맵 P1 #13).
### 2026-04-21 — prd-review (iter 2): PASS → code. 표시-동작 일관성 회복, AC-T02-28/29 모순 없음, 새 CRITICAL 없음. 11개 UX 디테일 coder 위임.
### 2026-04-21 — code (iter 1): PASS → review. TranslatePlaygroundScreen+ViewModel 신설, Navigation 교체, strings 16개(KO+EN) 추가, TranslateBrowse 2파일 삭제. 빌드 게이트 OK.
### 2026-04-21 — review (iter 1): PASS → qa. BLOCKER/MAJOR 없음. MINOR F5 (outdated 저장) 후속 사이클로 분리. 8개 우선 검토 지점 통과.
### 2026-04-21 — qa (iter 1): PASS → done. emulator-5554 에서 15개 우선 AC 모두 PASS (AC-T02-09/10/11 Error 분기는 정적 검증, 모델 캐시 상태). 25분 세션 무크래시.
