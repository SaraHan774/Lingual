---
name: retro
description: "완료된 /ship 사이클을 분석해 파이프라인의 구조적 결함만 식별하고, 에이전트·스킬·CLAUDE.md 를 실제로 수정하기 위한 **실행 가능한 DIRECTIVE 블록**을 생성한다. 사람용 리포트가 아니라 다음 턴 Claude 가 프롬프트로 재수신해 그대로 Edit 할 수 있는 형식. 사용 시점: 사용자가 `/retro <slug>`, `/retro --all`, 또는 인자 없이 `/retro` 를 입력할 때."
---

# /retro — Post-Ship Pipeline Retrospective

너는 이 스킬이 호출되는 동안 **분석기** 역할만 한다. 파일을 수정하지 않는다. 사이클 파일을 읽어 구조적 결함을 식별하고, 다음 턴 Claude 가 그대로 실행할 수 있는 **DIRECTIVE 블록**으로 출력한다. 사용자가 출력 말미의 트리거 문구를 새 프롬프트로 제출하면 그때 실제 Edit 이 수행된다.

## 인수

- `/retro` — `.claude/cycles/` 에서 `stage: done` 인 가장 최근 사이클 한 개.
- `/retro <slug>` — 지정 사이클.
- `/retro --all` — `.claude/cycles/` 의 모든 `done` 사이클 집계 분석 (재발 임계 판정용).

사이클 파일이 없거나 slug 가 유효하지 않으면 `ls .claude/cycles/` 결과를 보여주고 종료.

## 분석 파이프라인

### 1) 입력 로드

- 대상 사이클 파일을 `Read` 로 로드. YAML 프론트매터의 `slug`, `request`, `iter`, `feedback.<stage>`, `last_verdict` 와 `<!-- History -->` 하단 이력을 파싱.
- `--all` 모드: `Glob` 로 `.claude/cycles/*.md` → `stage: done` 인 파일만 선별.
- **CLAUDE.md 동시 로드** — "Non-obvious rules" 섹션의 각 규칙을 bullet 단위로 추출. 이 목록이 명문 규칙 위반 판정의 기준이 된다.

### 2) 결함 추출

각 사이클의 `feedback.<stage>` 블록과 이력 라인에서 아래를 뽑는다:

- **back-prop 사유** — `NEEDS_<STAGE>` 전이 각각에 대해 원인 한 문장 + 인용.
- **iteration 카운트** — 스테이지별 iter ≥ 2 인 경우 모두 후보.
- **인프라 실패** — `BLOCKED_HUMAN`, VERDICT 파싱 실패, 빌드 게이트 실패(코드 iter 페널티로 증가한 경우).

### 3) 분류

각 후보를 다음 원인 카테고리 중 하나로 분류:

| 코드 | 의미 |
|---|---|
| `RULE_VIOLATION` | CLAUDE.md Non-obvious rules 에 이미 명시된 규칙을 위반 |
| `INTERACTION_ONLY` | 실기 재현으로만 드러나는 UI/상호작용 버그 (회전·스크롤·포커스·TTS·IME 등) |
| `SPEC_GAP` | PRD 에 에러 상태·파괴적 동작·엣지케이스·Open Question 누락 |
| `INFRA_FAIL` | VERDICT 크기·형식, 빌드 게이트, 스크린샷 bridge 등 파이프라인 자체 실패 |
| `OTHER` | 위에 들어가지 않는 일회성 |

분류 증거는 반드시 **사이클 파일의 원문 인용 + 라인 번호** 로 남긴다.

### 4) 신뢰도 판정

| confidence | 조건 |
|---|---|
| **high** | `RULE_VIOLATION` 단발이라도 OK / 또는 어떤 카테고리든 **재발 ≥ 2** (`--all` 모드에서만 재발 판정 가능) |
| **medium** | 재발 미충족이지만 `INFRA_FAIL` 또는 구조적 영향 큼 (예: BLOCKED_HUMAN 유발) |
| **low** | 단발·구조적 영향 작음 → **DIRECTIVE 로 출력하지 않음.** "관찰" 섹션에만 기록. |

**완화 규칙**: `RULE_VIOLATION` 은 단발이라도 high 로 승격. CLAUDE.md 에 이미 써 있는 규칙을 놓친 것은 에이전트 프롬프트에 강제하면 즉시 재발을 막을 수 있기 때문.

### 5) 개선 지점 결정

high / medium 결함마다 **수정 대상 파일**을 결정:

| 결함 성격 | 우선 대상 |
|---|---|
| coder 가 명문 규칙을 놓침 | `.claude/agents/coder.md` — 프리플라이트 체크리스트 또는 해당 규칙 강조 |
| review 가 실기 검증 없이 PASS | `.claude/agents/code-reviewer.md` — 인터랙션 리스크 플래그 섹션 |
| qa-tester 가 VERDICT 크기 초과 | `.claude/agents/qa-tester.md` — 스크린샷은 경로 참조만 강제 |
| prd-curator 가 특정 카테고리 반복 누락 (재발 ≥ 2) | `.claude/agents/prd-curator.md` — 체크리스트 추가 |
| `/ship` 오케스트레이션 자체 결함 | `.claude/skills/ship/SKILL.md` |
| CLAUDE.md 규칙이 있으나 모호 | `CLAUDE.md` Non-obvious rules 보강 |

## DIRECTIVE 블록 스키마

각 high / medium 결함에 대해 아래 블록 하나씩 방출한다. 형식은 고정이며 다음 턴 Claude 가 파싱한다.

~~~directive
id: D<n>
target_file: <.claude/agents/X.md | .claude/skills/ship/SKILL.md | CLAUDE.md>
anchor: "<대상 파일에서 Grep 가능한 고유 문자열 또는 섹션 제목>"
action: append_to_section | insert_after_anchor | replace_anchor
evidence:
  - cycle: .claude/cycles/<slug>.md
    lines: <L1>-<L2>
    quote: |
      <인용 원문>
  # (재발이면 cycle 여러 개 나열)
category: RULE_VIOLATION | INTERACTION_ONLY | SPEC_GAP | INFRA_FAIL | OTHER
rule_violated: "<CLAUDE.md 의 해당 규칙 원문 인용 또는 NEW>"
proposed_text: |
  <target_file 에 실제로 삽입/대체할 텍스트. 기존 파일 톤/형식 유지.>
rationale: "<한 줄 — 이 edit 이 왜 재발을 막는지>"
confidence: high | medium
~~~

- `anchor` 는 Grep 으로 파일에서 1회만 매치되어야 한다. 모호하면 더 긴 문자열 사용.
- `proposed_text` 는 draft — 다음 턴 Claude 가 대상 파일 톤에 맞게 미세 조정할 여지 인정.

## 출력 형식 (고정)

```
# /retro 분석 — <slug 또는 --all>

## 메트릭
- 분석 대상: <사이클 개수>
- 스테이지별 평균 iter: prd=<x> prd-review=<x> code=<x> review=<x> qa=<x>
- BLOCKED_HUMAN: <n>건
- 빌드 게이트 실패: <n>건

## DIRECTIVE (high / medium)
<DIRECTIVE 블록들 — 없으면 "없음">

## 관찰 (low, 지시문 미생성)
- <1줄 요약, 근거 사이클 인용>

## 실행
위 DIRECTIVE 를 적용하려면 다음 프롬프트를 그대로 제출하세요:

> apply retro directives above
```

## 규칙

- **파일을 쓰지 않는다.** 출력만. 실행은 다음 턴.
- **low 는 DIRECTIVE 로 쓰지 않는다.** 샘플 부족 건을 지시문으로 강제하면 과잉 대응이 되어 프롬프트가 비대해진다.
- **명문 규칙 위반은 단발이라도 high** — 완화 규칙. CLAUDE.md 에서 해당 규칙 원문을 그대로 인용해 `rule_violated` 에 넣는다.
- **인용은 반드시 라인 번호와 함께.** 다음 턴 Claude 가 evidence 를 재검증할 수 있어야 한다.
- **docs/prd/, 코드 파일, cycle 파일은 수정 대상 아님.** retro 는 파이프라인(에이전트·스킬·CLAUDE.md)만 다룬다. PRD 누락 패턴은 prd-curator 에이전트의 체크리스트 개선으로 우회한다.
- **확증 없는 일반화 금지.** 단발 SPEC_GAP 은 low 로 관찰만. 재발 ≥ 2 를 확인한 뒤에만 high 승격.

## 다음 턴 트리거 — "apply retro directives above"

사용자가 이 문구로 재진입하면, 그때의 Claude 는:

1. 직전 어시스턴트 메시지에서 모든 `~~~directive ... ~~~` 블록을 파싱.
2. 각 블록에 대해:
   - `target_file` 을 `Read` 로 열고 `anchor` 를 `Grep` 으로 위치 확인.
   - `action` 에 따라 `Edit` 수행:
     - `append_to_section`: anchor 가 속한 섹션 끝에 `proposed_text` 추가
     - `insert_after_anchor`: anchor 바로 다음 줄에 삽입
     - `replace_anchor`: anchor 와 매치되는 구간을 `proposed_text` 로 교체
   - anchor 가 0회 또는 2회 이상 매치되면 해당 DIRECTIVE 는 스킵 + 사용자에게 보고.
3. 적용 결과를 1줄씩 요약 (`D1 applied: <file>`, `D2 skipped: anchor ambiguous`).
4. 어떤 파일도 git commit 하지 않는다.

## 비목표

- 사람용 분석 리포트 생성 (출력은 DIRECTIVE 전용).
- cycle 자체의 버그 분석 (그건 `/ship` 의 qa 스테이지 역할).
- 코드/테스트/PRD 문서 직접 수정.
- 사이클 간 시간·코스트·토큰 메트릭 집계.
- 재발 임계 미충족 건에 대한 추측성 일반화.

## 호출 예시

- `/retro` → 가장 최근 done 사이클 분석. RULE_VIOLATION 1건 발견 → D1(high) 방출.
- `/retro wordcard-feature` → 해당 사이클의 qa iter 3 추적 → StateFlow replay = RULE_VIOLATION(CLAUDE.md Compose one-shot 이벤트 규칙) → `.claude/agents/coder.md` 에 프리플라이트 체크 추가 DIRECTIVE.
- `/retro --all` → 전 done 사이클에서 SPEC_GAP 카테고리 재발 2건 확인되면 prd-curator 체크리스트 DIRECTIVE 방출. 미충족이면 "관찰" 섹션에만.
- `/retro foo` (존재하지 않는 slug) → `ls .claude/cycles/` 로 사용 가능 slug 안내 후 종료.
