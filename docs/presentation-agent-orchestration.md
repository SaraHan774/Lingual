# Cycle File 기반 Multi-Agent Orchestration
### AI 에이전트가 서로 협업해 기능을 만드는 방법

---

## 목차

1. [배경 — 왜 만들었나?](#1-배경)
2. [시스템 전체 구조](#2-전체-구조)
3. [핵심 개념: Cycle File](#3-cycle-file)
4. [5개 전문 에이전트](#4-전문-에이전트)
5. [VERDICT 프로토콜](#5-verdict-프로토콜)
6. [실전 예시: flashcard-ui-ux 사이클](#6-실전-예시)
7. [Compaction 안전성](#7-compaction-안전성)
8. [사용 방법](#8-사용-방법)
9. [Q&A](#9-qa)

---

## 1. 배경

### 문제

```
"일기에 태그 기능 추가해줘"
 → Claude가 바로 코드를 짜기 시작
 → PRD 없이 구현 → 요구사항 어긋남 → 재작업
 → 리뷰 없이 머지 → 버그 발생
 → 세션이 길어지면 앞 내용을 잊어버림
```

### 해결 목표

- **역할 분리**: 기획·검토·구현·리뷰·QA 를 각각 전문 에이전트에게 위임
- **상태 보존**: 세션이 끊겨도, 대화가 압축(compaction)되어도 작업이 이어짐
- **품질 게이트**: 각 단계가 다음 단계로 넘기기 전에 반드시 검증

---

## 2. 전체 구조

```
사용자
  │
  │  /ship <기능 설명>
  ▼
오케스트레이터 (메인 Claude)
  │   ┌────────────────────────────────────┐
  │   │  .claude/cycles/<slug>.md          │
  │   │  ← 단일 진실 공급원 (SSoT)          │
  │   └────────────────────────────────────┘
  │
  ├─── Task ──▶ prd-curator   (PRD 작성/갱신)
  │                │ VERDICT
  ├─── Task ──▶ prd-reviewer  (UX 품질 검토)
  │                │ VERDICT
  ├─── Task ──▶ coder         (코드 구현)
  │                │ VERDICT
  ├─── Task ──▶ code-reviewer (diff 리뷰)
  │                │ VERDICT
  └─── Task ──▶ qa-tester     (E2E 검증)
                   │ VERDICT → PASS → 완료!
```

**핵심 규칙**: 오케스트레이터는 직접 코드를 짜거나 판단하지 않는다.  
오직 **Cycle File을 읽고** → **에이전트를 호출하고** → **VERDICT를 파싱해 분기**한다.

---

## 3. Cycle File

### 위치

```
.claude/cycles/
├── flashcard-ui-ux.md    ← 완료된 사이클
├── wordcard-feature.md   ← 완료된 사이클
└── wordcard-add-ux.md    ← 진행 중
```

### 파일 구조

```markdown
# Cycle: flashcard-ui-ux

- **Request**: 단어장 화면 UI/UX 개선 (필터 칩, 숙련도 시각화 등)
- **Started**: 2026-04-18 00:00
- **Max iterations per stage**: 3

## State
- **Current stage**: done          ← 오케스트레이터가 매 iteration 여기서 읽음
- **Iterations**:
  - prd: 2  / prd-review: 2 / code: 1 / review: 1 / qa: 1
- **Last verdict**: PASS (stage=qa, iter=1)

## Accumulated Feedback
### For prd          ← prd-reviewer가 back-prop한 피드백
### For code         ← prd-reviewer가 code에 전달할 주안점
...

## History           ← append-only 이력
### 2026-04-18 — prd (iter 1)
- status: PASS → next: prd-review
...
```

### 왜 파일인가?

| 저장 위치 | 세션 종료 후 | 대화 압축(compaction) 후 |
|---|---|---|
| Claude 메모리(대화) | ❌ 사라짐 | ❌ 요약으로 대체됨 |
| **Cycle File (파일)** | ✅ 유지 | ✅ 유지 |

---

## 4. 전문 에이전트

각 에이전트는 `.claude/agents/` 아래 마크다운 파일로 정의된다.

### 에이전트 역할 분담

```
┌─────────────────────────────────────────────────────────┐
│ prd-curator                                             │
│ • PRD.md + docs/prd/ 갱신                               │
│ • "무엇을, 왜" 확정 (Acceptance Criteria 구체화)          │
│ • 코드에서 읽히는 것은 PRD에 쓰지 않음                    │
└─────────────────────────────────────────────────────────┘
         │ PASS → next: prd-review
         ▼
┌─────────────────────────────────────────────────────────┐
│ prd-reviewer                                            │
│ • 언어 학습 앱 UX 관점에서 PRD 검토                       │
│ • CRITICAL 발견 시 prd로 back-propagate                  │
└─────────────────────────────────────────────────────────┘
         │ PASS → next: code
         ▼
┌─────────────────────────────────────────────────────────┐
│ coder  (model: claude-opus)                             │
│ • PRD의 AC를 코드로 구현                                  │
│ • ./gradlew assembleDebug 빌드 확인까지                   │
│ • E2E 검증은 qa-tester에게 위임                           │
└─────────────────────────────────────────────────────────┘
         │ PASS → next: review
         ▼
┌─────────────────────────────────────────────────────────┐
│ code-reviewer                                           │
│ • diff 리뷰: BLOCKER / MAJOR / MINOR 분류                │
│ • 아키텍처·보안·성능 집중 검토                             │
└─────────────────────────────────────────────────────────┘
         │ PASS → next: qa
         ▼
┌─────────────────────────────────────────────────────────┐
│ qa-tester                                               │
│ • 실기기/에뮬레이터 E2E 검증                              │
│ • AC별 통과/실패 판정                                     │
└─────────────────────────────────────────────────────────┘
         │ PASS → DONE 🎉
```

### 에이전트 정의 예시 (prd-curator.md 헤더)

```markdown
---
name: prd-curator
description: PRD SSoT 유지 전문 에이전트
tools: Read, Write, Edit, Glob, Grep, Bash
---
```

- `tools`: 에이전트가 사용할 수 있는 도구를 제한 (최소 권한 원칙)
- `model`: coder만 `claude-opus`로 설정 (복잡한 구현 작업)
- 에이전트는 **stateless**: 필요한 모든 입력은 프롬프트에 명시

---

## 5. VERDICT 프로토콜

### 에이전트가 응답 맨 마지막에 방출

````markdown
```verdict
status: PASS | NEEDS_SPEC | NEEDS_CODE | NEEDS_REVIEW | BLOCKED_HUMAN
next_stage: prd | prd-review | code | review | qa | done | human
iteration: 2
cycle_file: .claude/cycles/flashcard-ui-ux.md
feedback: |
  다음 스테이지가 처리해야 할 내용.
  AC-10 "복습 예정" 필터와 nextReviewAt 충돌 해결 필요.
refs:
  - docs/prd/04-feature-flashcard.md#data-model
```
````

### 전이 매트릭스

| 에이전트 | 방출 가능 status | next_stage |
|---|---|---|
| prd-curator | PASS, BLOCKED_HUMAN | prd-review / human |
| prd-reviewer | PASS, **NEEDS_SPEC** | code / **prd** |
| coder | PASS, NEEDS_SPEC, NEEDS_UX | review / prd / prd-review |
| code-reviewer | PASS, **NEEDS_CODE** | qa / **code** |
| qa-tester | PASS, NEEDS_CODE, NEEDS_SPEC | **done** / code / prd |

### Back-Propagation (핵심!)

```
qa-tester: "NEEDS_CODE" 방출
    ↓
오케스트레이터: cycle file의 "For code" 섹션에 feedback 기록
    ↓
coder 재호출 (iter 2): feedback을 받아 수정
    ↓
code-reviewer 재호출 → qa-tester 재호출
    ↓
PASS → DONE
```

---

## 6. 실전 예시

### flashcard-ui-ux 사이클 전체 흐름

```
요청: "단어장 화면 UI/UX 개선 — 필터칩, 숙련도 시각화, 통계 배너 등"
slug: flashcard-ui-ux
```

#### 타임라인

```
[prd iter 1]  PASS → prd-review
              AC-9~AC-15 신규, UX Flow B 전면 개정

[prd-review iter 1]  NEEDS_SPEC → prd (back-prop!)
    ★ CRITICAL: nextReviewAt "Phase 2 미사용" vs
                AC-10 "복습 예정 필터" 충돌!
    권고 4건: reviewCount 즉각 갱신, 언어 색상 팔레트, 자동접힘...

[prd iter 2]  PASS → prd-review
    옵션A 채택: 숙련도 선택 시 nextReviewAt 자동 설정
    AC-16 신규, AC-11/13/15 수정

[prd-review iter 2]  PASS → code
    구현 주안점 5건 전달

[code iter 1]  PASS → review
    FlashCardViewModel.kt, FlashCardScreen.kt 수정
    빌드 통과

[review iter 1]  PASS → qa
    BLOCKER 없음, MINOR 4건

[qa iter 1]  PASS → DONE 🎉
    AC-5/9~16 전부 AVD Pixel 9 Pro(API 35) E2E 통과
```

#### 결과 통계

```
총 iteration: prd=2, prd-review=2, code=1, review=1, qa=1
back-prop 횟수: 1 (prd-review→prd)
소요 스테이지: 7회 에이전트 호출
```

---

## 7. Compaction 안전성

### 문제: 대화가 길어지면 Claude는 이전 내용을 잊는다

```
[세션 초반]
오케스트레이터: "prd PASS, 다음은 code"

... 대화 100턴 ...

[compaction 발생]
오케스트레이터: "??? 어디까지 했더라?"
```

### 해결: 매 iteration 파일을 처음부터 읽는다

```python
# 오케스트레이터의 매 iteration 첫 번째 동작
cycle_state = Read(".claude/cycles/<slug>.md")  # 항상 파일이 SSoT

current_stage = cycle_state["Current stage"]  # 기억 아닌 파일에서
iterations    = cycle_state["Iterations"]
feedback      = cycle_state["Accumulated Feedback"]

# 대화 컨텍스트와 어긋나면 → 파일을 따른다
```

### Compaction 시나리오

```
review PASS 직후 → compaction 발생
    ↓
다음 턴: 오케스트레이터 사이클 파일 재로드
    ↓
Current stage = "qa" 확인
    ↓
qa-tester 정상 호출 → 이어서 진행
```

### 보너스: 세션 재개

```bash
# 세션이 끊겨도
/ship --resume flashcard-ui-ux

# 파일에서 상태 승계 → 저장된 stage부터 재개
# 사이클 파일이 있는 한 언제든 이어서 진행
```

---

## 8. 사용 방법

### 기본 명령

```bash
# 새 기능 시작
/ship 일기에 태그 기능 추가

# 진행 중 사이클 목록
/ship --list

# 사이클 재개 (세션 단절 후)
/ship --resume diary-tags
```

### 파일 구조

```
.claude/
├── agents/
│   ├── prd-curator.md      # 에이전트 정의 (역할, 도구, 규칙)
│   ├── prd-reviewer.md
│   ├── coder.md
│   ├── code-reviewer.md
│   └── qa-tester.md
├── skills/
│   └── ship/
│       └── SKILL.md        # 오케스트레이터 로직
├── cycles/
│   ├── flashcard-ui-ux.md  # 완료된 사이클 (done)
│   └── wordcard-add-ux.md  # 진행 중
└── settings.local.json
```

### 안전 장치

| 상황 | 동작 |
|---|---|
| 같은 스테이지에 4번째 진입 | `BLOCKED_HUMAN` → 사람 개입 요청 |
| VERDICT 블록 없음 | 에이전트에게 한 번만 재요청 |
| 파일 쓰기 실패 | 다음 스테이지 진행 금지 |
| 사용자가 중단 요청 | 즉시 멈춤, 파일 보존, 나중에 resume 가능 |

---

## 9. Q&A

### 자주 묻는 질문

**Q. 에이전트가 잘못된 VERDICT를 내면?**  
A. 오케스트레이터가 YAML 파싱 실패 시 한 번만 재요청 (iteration 카운트 미포함). 두 번 실패하면 BLOCKED_HUMAN.

**Q. 사이클 중간에 요구사항이 바뀌면?**  
A. `/ship --resume <slug>` 로 재개하되, prd-curator부터 다시 돌리는 게 안전. 사이클 파일의 Accumulated Feedback을 수동으로 수정한 뒤 resume도 가능.

**Q. 에이전트끼리 직접 대화하나?**  
A. 아니다. 오케스트레이터가 중개자다. 에이전트 A의 결과(VERDICT의 feedback)를 오케스트레이터가 사이클 파일에 기록하고, 다음 에이전트를 호출할 때 프롬프트에 실어 보낸다.

**Q. 모든 기능에 /ship을 써야 하나?**  
A. 아니다. 간단한 버그 수정은 직접 구현. `/ship`은 "PRD가 필요할 만큼 복잡하거나, 품질 게이트가 필요한" 기능에 쓴다.

**Q. coder만 claude-opus를 쓰는 이유?**  
A. 구현이 가장 복잡하고 파일 조작이 많다. 다른 에이전트는 sonnet으로 충분하고 비용 절감도 된다.

---

## 정리

```
┌─────────────────────────────────────────────────────┐
│  핵심 아이디어 3가지                                  │
│                                                     │
│  1. 역할 분리                                        │
│     기획·UX검토·구현·리뷰·QA = 각각 전문 에이전트     │
│                                                     │
│  2. 파일이 SSoT                                      │
│     세션·compaction에 무관하게 상태 보존              │
│                                                     │
│  3. VERDICT 프로토콜                                  │
│     구조화된 출력으로 자동 분기 + back-propagation    │
└─────────────────────────────────────────────────────┘
```

### 참고 파일

- 오케스트레이터 로직: `.claude/skills/ship/SKILL.md`
- 에이전트 정의: `.claude/agents/*.md`
- 실제 사이클: `.claude/cycles/flashcard-ui-ux.md`
