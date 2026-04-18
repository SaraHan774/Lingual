# Lingual — Product Requirements Document (PRD)

> **Single Source of Truth.** 이 문서는 Lingual 앱의 제품 요구사항 목차(Table of Contents)입니다. 기능별 상세는 `docs/prd/` 하위 문서에서 관리됩니다. 기능 추가/변경/삭제 시 `prd-curator` 에이전트가 이 목차와 해당 하위 문서를 **함께** 갱신합니다.
>
> **Maintained by:** [`.claude/agents/prd-curator.md`](./.claude/agents/prd-curator.md)
> **Supported locales:** 한국어 · English · 日本語 · 中文
> **Last indexed:** 2026-04-18

---

## 한눈에 보기

Lingual은 **polyglot(다국어 학습자)을 위한 일기 앱**이다. 사용자는 한국어 / English / 日本語 / 中文 중 하나로 일기를 작성하고, 앱은 나머지 3개 언어로 **온디바이스** 번역·음성 재생하며, 선택한 단어를 플래시카드 덱으로 축적해 학습을 돕는다.

- **배포 목표:** Google Play Store
- **현재 단계:** v0.1 MVP — Diary / Translate / FlashCard / Settings 4개 탭
- **핵심 제약:** 번역은 오프라인(ML Kit) 원칙, 최초 모델 다운로드(~30MB/언어)만 네트워크 필요

---

## 목차 (Table of Contents)

### 1. 제품 정의

- [01 — Overview & Vision](./docs/prd/01-overview.md)
  제품 비전, 타겟 페르소나, 범위, 성공 지표, 비기능 요구 요약.

### 2. 기능 명세 (Feature Specs)

- [02 — Diary (일기)](./docs/prd/02-feature-diary.md) · **status: shipped**
  일기 작성, 목록, 상세 조회. 작성 시 3개 언어 번역 파이프라인을 트리거.
- [03 — Translation (번역)](./docs/prd/03-feature-translation.md) · **status: shipped**
  ML Kit 온디바이스 번역 엔진, 언어쌍 캐싱, PENDING/SUCCESS/ERROR 상태 머신.
- [04 — FlashCard (단어장)](./docs/prd/04-feature-flashcard.md) · **status: shipped (manual) / planned (auto-extract)**
  단어 카드 학습, 숙련도 0–3, 즐겨찾기. 자동 단어 추출은 Phase 2.
- [05 — TTS (음성 재생)](./docs/prd/05-feature-tts.md) · **status: shipped**
  `TextToSpeech` 기반 4개 언어 음성 재생, 재생 상태 StateFlow 공개.
- [06 — Settings (설정)](./docs/prd/06-feature-settings.md) · **status: shipped (read-only)**
  번역 엔진·TTS·앱 정보 안내. 사용자 편집 가능 설정은 Phase 2.

### 3. 시스템 & 로드맵

- [07 — Architecture & NFR](./docs/prd/07-architecture.md)
  Clean Architecture 계층, Hilt DI, Room 스키마, Compose 내비게이션, 빌드 타겟.
- [08 — Roadmap & Open Questions](./docs/prd/08-roadmap.md)
  Phase 2 기능 후보, Play Store 공개 준비 항목, 패키지 리네임(com.august.lingual) 절차.

---

## 문서 변경 프로토콜

1. 기능 요청이 들어오면 **`prd-curator` 에이전트를 먼저 호출**한다.
2. 에이전트는 관련 하위 문서의 Goal / User Stories / Acceptance Criteria / Open Questions를 갱신한다.
3. 신규 기능일 경우 `docs/prd/NN-feature-<slug>.md`로 새 문서를 생성하고 이 목차에 한 줄 추가한다.
4. 상태 라벨은 `planned` → `in-progress` → `shipped` 순으로만 전진한다. 역방향으로 내릴 때는 사용자 확인 필수.
5. 구현(코드 변경)은 PRD 갱신 **이후** 별도 단계로 수행한다.
