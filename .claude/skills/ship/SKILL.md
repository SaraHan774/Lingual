---
name: ship
description: "Lingual 기능의 전체 전달 사이클(PRD → Code → Review → QA)을 하나의 명령으로 돌린다. 각 스테이지 에이전트가 방출하는 VERDICT 블록을 파싱해 PASS 면 다음 스테이지로, NEEDS_<STAGE> 면 이전 스테이지로 back-propagate 한다. 사이클 상태는 `.claude/cycles/<slug>.md` 에 파일로 보관되어 compaction 과 세션 단절에도 안전하며, `/ship --resume <slug>` 로 재개할 수 있다. 사용 시점: 사용자가 `/ship <기능 설명>` 또는 `/ship --resume <slug>` 또는 `/ship --list` 를 입력할 때."
---

# /ship — Feature Delivery Cycle Orchestrator

너(메인 Claude)는 이 스킬이 호출되는 동안 **오케스트레이터** 역할을 한다. 직접 코드를 짜거나 리뷰·QA 를 수행하지 않는다. 오직 아래 5개 에이전트를 `Task` 툴로 호출하고, 각 에이전트가 방출하는 **VERDICT** 블록을 읽어 다음 스테이지로 분기할 뿐이다.

- `prd-curator` — PRD 확정
- `prd-reviewer` — UX 품질 검토 (언어 학습 앱 전문 관점)
- `coder` — 구현
- `code-reviewer` — 리뷰
- `qa-tester` — E2E 검증

## 인수

`/ship` 뒤에 따라오는 자유 텍스트 = 기능 요청. 예: `/ship 일기에 태그 기능 추가`.

**특수 형태:**

- `/ship --resume <slug>` — 기존 사이클 이어서 실행. `.claude/cycles/<slug>.md` 가 존재해야 하며, 없으면 오류 메시지 + `ls .claude/cycles/` 결과로 사용 가능한 slug 안내.
- `/ship --list` — `.claude/cycles/` 를 훑어 `Current stage != done` 인 진행 중 사이클 목록을 slug / 현재 stage / iteration 수 / 마지막 업데이트 시각으로 표시. 이 모드는 에이전트 호출 없이 리스트만 반환하고 종료.

요청이 비어 있으면 한 번 되묻는다("어떤 기능을 ship 할까요? 또는 `--list` 로 진행 중 사이클을 볼 수 있습니다."). 이후 사용자가 답하면 진행.

## 사이클 상수

- `MAX_ITERATIONS_PER_STAGE = 3` — 같은 스테이지에 네 번째로 재진입해야 하면 `BLOCKED_HUMAN` 으로 에스컬레이트한다.
- `STAGE_ORDER = [prd, prd-review, code, review, qa]` — PASS 시 기본 전진 순서.
- `DONE` 상태: qa 에서 PASS 가 나오면 사이클 종료.

## 워크플로우

### 0) 사이클 초기화 또는 재개

**경로 A — 신규 요청 (`/ship <text>`)**

1. **slug 생성**: 요청 텍스트에서 짧은 영문 slug 를 만든다(예: "일기에 태그 기능 추가" → `diary-tags`). 한국어·특수문자는 제거, 3~5 단어, kebab-case. 이미 `.claude/cycles/<slug>.md` 가 존재하면 뒤에 `-2`, `-3` 붙인다.
2. **cycle file 작성**: `.claude/cycles/<slug>.md` 에 아래 템플릿을 쓴다.
   YAML 프론트매터(`---` 블록)가 기계 판독 상태, 그 아래 마크다운이 사람 판독 이력이다.

```markdown
---
slug: <slug>
request: "<원본 요청>"
started: "<YYYY-MM-DD HH:MM>"
max_iter: 3
stage: prd
iter: {prd: 0, prd-review: 0, code: 0, review: 0, qa: 0}
last_verdict: ~
feedback:
  prd: ~
  prd-review: ~
  code: ~
  review: ~
  qa: ~
---

<!-- History: append-only -->
```

3. **사용자에게 한 줄 통지**: "사이클 시작: `.claude/cycles/<slug>.md`" — 사용자가 진행 상황을 볼 수 있는 파일 위치를 알려준다.

**경로 B — 재개 (`/ship --resume <slug>`)**

1. `.claude/cycles/<slug>.md` 를 `Read` 한다. 없으면 오류 + `ls .claude/cycles/` 결과 반환 후 종료.
2. YAML 프론트매터의 `stage` 가 `done` 이면 "이미 완료된 사이클입니다" 로 종료(재실행이 필요하면 신규 `/ship` 호출 권장).
3. `stage`, `iter`, `feedback`, `last_verdict` 를 SSoT 로 그대로 승계 — **대화 컨텍스트의 기억으로 덮어쓰지 않는다.** (이전 세션의 기억이 있더라도, 파일이 우선.)
4. 사용자에게 한 줄 통지: "사이클 재개: `<slug>` — stage=`<current>`, iter=prd:<n>/code:<n>/review:<n>/qa:<n>".
5. 아래 루프로 진입.

### 1) 스테이지 실행 루프

아래 루프를 `current_stage == done` 이 되거나 `BLOCKED_HUMAN` 이 나올 때까지 반복한다.

> **Compaction 안전성 원칙**: 대화 컨텍스트의 기억(직전 verdict, 직전 stage 등)은 compaction 으로 언제든 요약·유실될 수 있다. 따라서 **매 iteration 은 사이클 파일을 처음 보는 것처럼 시작한다.** "내가 방금 뭘 했는지" 를 기억에 의존해 판단하지 않는다.

1. **사이클 파일 재로드 (compaction 안전)**: iteration 시작 시 맨 먼저 `.claude/cycles/<slug>.md` 를 `Read` 로 **다시 읽는다.** 파일 상단 `---` 블록(YAML 프론트매터)에서 `stage`, `iter`, `feedback`, `last_verdict` 를 읽는다. 대화 컨텍스트와 어긋나면 **파일을 따른다** — 파일이 SSoT 이다.
2. **가드 체크**: 재로드된 `iter.<current_stage>` 가 `max_iter` 를 초과하면 즉시 루프 종료 + 사용자에게 에스컬레이션 메시지(아래 "종료" 참조).
3. **카운트 증가**: `iter.<current_stage>` 를 +1 하고 **즉시** 사이클 파일의 YAML `iter:` 필드를 `Edit` 로 반영.
4. **에이전트 호출**: `Task` 툴로 해당 스테이지의 에이전트를 호출한다. 프롬프트에 반드시 다음을 포함:
   - **맥락 블록**: "당신은 `/ship` 사이클 컨텍스트에서 호출되었습니다. cycle file: `.claude/cycles/<slug>.md` (iteration <N>). 원본 요청: <원본>."
   - **누적 피드백**: YAML 프론트매터의 `feedback.<stage>` 필드 전체(여러 번 back-prop 된 경우 모든 항목이 시간순으로 쌓여 있다. 가장 최근 항목이 맨 아래).
   - **출력 요구**: "응답 맨 마지막에 VERDICT 블록을 반드시 포함하세요. 스키마는 에이전트 정의의 'VERDICT 블록' 섹션을 따릅니다."
5. **VERDICT 파싱**: 에이전트 응답의 마지막 ```verdict ... ``` 블록을 찾아 YAML 로 파싱한다. 블록이 없거나 스키마가 깨지면 **그 에이전트를 한 번만** "VERDICT 블록을 형식에 맞게 다시 출력해 주세요" 라고 재호출한다(재호출은 iteration 카운트에 포함하지 않는다). 두 번 실패하면 `BLOCKED_HUMAN` 으로 종료.
5-a. **빌드 게이트 (code 스테이지 PASS 직후에만)**: coder 가 `PASS` 를 방출하면, 다음 스테이지(review)로 넘기기 **전에** 오케스트레이터가 직접 `./gradlew :app:assembleDebug` 를 실행한다.
   - **성공**: 정상적으로 step 6 로 진행.
   - **실패**: 빌드 출력을 feedback 으로 삼아 YAML `feedback.code` 에 append 하고, `stage` 를 `code` 로 유지한 채 `iter.code` 를 +1. 이 실패는 coder iteration 카운트에 포함된다(false PASS 방출에 대한 페널티). 이후 루프 맨 위로 돌아가 coder 재호출.
6. **즉시 영속화 (compaction 안전)**: VERDICT 파싱 **직후, 다음 Task 호출 전에 반드시 먼저** 사이클 파일을 `Edit` 로 갱신한다. 수정 대상은 모두 YAML 프론트매터(`---` 블록) 안의 필드다:
   - `stage: <old>` → `stage: <next_stage>` 로 교체 (BLOCKED_HUMAN 이면 `stage: human`).
   - `iter:` 의 해당 스테이지 카운트를 +1 된 값으로 교체. 예: `{prd: 1, prd-review: 0, ...}`.
   - `last_verdict: ~` (또는 이전 값) → `last_verdict: "<status> (stage=<stage>, iter=<N>)"` 로 교체.
   - `NEEDS_<STAGE>` 인 경우 `feedback.<target-stage>:` 필드에 새 항목을 **추가(append)** 한다. 기존 값이 `~` 이면 block scalar 로 교체하고, 이미 block scalar 이면 맨 아래에 빈 줄 + 새 항목을 붙인다:
     ```yaml
       <target-stage>: |
         [iter <N> — from <current-stage>, <YYYY-MM-DD>]
         <verdict.feedback 내용>
     ```
   - `<!-- History -->` 마크다운 블록 맨 아래에 한 줄 이력을 추가(append):
     `### <YYYY-MM-DD> — <stage> (iter <N>): <status> → <next_stage>. <feedback 1줄 요약>`
   - 이 파일 쓰기가 실패하면 다음 스테이지로 **절대 진행하지 않는다** — 실패 보고 후 BLOCKED_HUMAN.
7. **루프 제어**: `status: BLOCKED_HUMAN` 이면 종료. 아니면 루프 맨 위(Step 1, 재로드)로 돌아간다 — in-memory 변수를 이월하지 않고 파일에서 다시 읽어 진행한다.

### 2) 종료

루프 종료 사유별로 사용자에게 보고한다.

- **`done` (qa PASS)**: "사이클 완료. 총 iteration: prd=<n> code=<n> review=<n> qa=<n>. 상세: `.claude/cycles/<slug>.md`". 사이클 파일의 맨 위 `Current stage` 를 `done` 으로 마크.
- **`BLOCKED_HUMAN`**: 어느 스테이지에서 왜 멈췄는지 + 마지막 에이전트의 feedback 요약 + 사용자가 결정해야 할 구체 질문. 사이클 파일은 그대로 두고, 사용자가 답하면 재개할 수 있도록 안내.
- **iteration 초과**: "`<stage>` 가 3회 연속 수렴하지 못했습니다. 마지막 피드백: <...>. 사람이 개입해야 합니다." — 역시 사이클 파일은 보존.

## VERDICT 블록 스키마 (에이전트가 방출, 오케스트레이터가 파싱)

각 에이전트는 응답 **맨 마지막**에 다음 형태의 fenced block 을 출력해야 한다:

~~~verdict
status: PASS | NEEDS_SPEC | NEEDS_UX | NEEDS_CODE | NEEDS_REVIEW | NEEDS_QA | BLOCKED_HUMAN
next_stage: prd | prd-review | code | review | qa | done | human
iteration: <에이전트가 호출 프롬프트에서 받은 iteration 숫자>
cycle_file: .claude/cycles/<slug>.md
feedback: |
  <다음 스테이지가 처리해야 할 내용을 한국어 자유 텍스트로. 파일/라인/AC 인용을 포함.>
refs:
  - <file:line 또는 docs/prd/xx.md#anchor 형식, 여러 개 가능>
~~~

- 언어: 한국어
- `status` 는 현재 스테이지의 결과, `next_stage` 는 어디로 가야 하는지.
- `PASS` 면 `next_stage` 는 기본 전진 값(예: prd→code, code→review, review→qa, qa→done).
- `NEEDS_<STAGE>` 면 `next_stage` 는 그 소문자(`prd`/`code`/`review`/`qa`).
- `BLOCKED_HUMAN` 이면 `next_stage: human` 으로 고정.

## 스테이지별 전이 매트릭스 (참고)

| 스테이지 | 방출 가능 status | 매핑된 next_stage |
|---|---|---|
| prd-curator | PASS, BLOCKED_HUMAN | PASS→prd-review, BLOCKED→human |
| prd-reviewer | PASS, NEEDS_SPEC, BLOCKED_HUMAN | PASS→code, NEEDS_SPEC→prd, BLOCKED→human |
| coder | PASS, NEEDS_SPEC, NEEDS_UX, BLOCKED_HUMAN | PASS→review, NEEDS_SPEC→prd, NEEDS_UX→prd-review, BLOCKED→human |
| code-reviewer | PASS, NEEDS_CODE, NEEDS_SPEC, BLOCKED_HUMAN | PASS→qa, NEEDS_CODE→code, NEEDS_SPEC→prd, BLOCKED→human |
| qa-tester | PASS, NEEDS_CODE, NEEDS_SPEC, BLOCKED_HUMAN | PASS→done, NEEDS_CODE→code, NEEDS_SPEC→prd, BLOCKED→human |

## 오케스트레이터가 지켜야 할 규칙

- **사이클 파일이 SSoT. 대화 컨텍스트는 캐시일 뿐.** compaction 은 언제든 요약을 교체할 수 있으므로, 판단은 항상 방금 읽은 사이클 파일 내용을 근거로. in-memory 상태와 파일이 어긋나면 파일을 따른다.
- **매 응답 끝에 anchor 줄을 남긴다.** compaction 이 요약을 만들 때 최소한 slug 와 stage 가 보존되도록 마지막 줄에 `[cycle: <slug>, stage: <current>, iter: <n>]` 형태의 한 줄을 출력한다. 이 한 줄이 있어야 사용자(와 compaction 후의 자기 자신) 가 어디를 재개해야 할지 즉시 안다.
- **에이전트의 역할을 대신하지 말 것.** 빌드·리뷰·테스트 결과를 네가 직접 판단하지 않는다. VERDICT 만 믿는다.
- **에이전트는 stateless 전제.** 필요한 모든 입력은 사이클 파일과 프롬프트에 명시. 이전 대화를 암묵적으로 기억한다고 가정하지 않는다.
- **파괴적 git 작업은 절대 자동화하지 말 것.** 이 스킬은 로컬 파일 쓰기와 Task 호출만 한다. commit/push/merge 는 사이클이 끝난 뒤 사용자가 명시 요청해야 실행.
- **사용자가 중단 요청하면 즉시 멈춘다.** 사이클 파일은 보존. `/ship --resume <slug>` 로 언제든 재개 가능.
- **진행 상황 보고 간결하게.** 각 스테이지 전이마다 사용자에게는 한 줄 — `[<stage> iter <N>] <status> → <next>` — 이면 충분. 상세는 사이클 파일에서 보게 한다. 이 줄 다음에 위의 anchor 줄을 추가.

## 호출 예시

- `/ship 일기에 태그 기능 추가` → slug=`diary-tags` → prd 부터 시작.
- `/ship` → 사용자에게 "어떤 기능을 ship 할까요?" 되묻기.
- `/ship --list` → `.claude/cycles/` 의 진행 중 사이클 목록만 보여주고 종료.
- `/ship --resume diary-tags` → 기존 사이클 파일 승계 → 저장된 `Current stage` 부터 재개. 세션이 바뀌었거나 compaction 이후에도 안전.
- 사이클 도중 qa 가 `NEEDS_CODE` 방출 → coder 재호출(iter 2) → 수정 후 review 재호출 → qa 재호출. qa 가 또 `NEEDS_CODE` 방출 → coder iter 3. 또 `NEEDS_CODE` → 다음번이 iter 4 이므로 `BLOCKED_HUMAN` 으로 종료 + 사용자 개입 요청. 사용자 수동 수정 후 `/ship --resume <slug>` 로 이어서 qa 부터 재실행 가능.
- **Compaction 시나리오**: review 가 `PASS` 를 막 돌려준 직후 compaction 발생 → 다음 턴에 오케스트레이터는 자기 기억 대신 사이클 파일을 재로드 → `Current stage=qa` 를 확인하고 qa 호출로 정상 진행.
