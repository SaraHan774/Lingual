# 05 — Feature: TTS (음성 재생)

[← 목차로](../../PRD.md) · **status: shipped (v0.1)**

## Goal

모든 번역 결과와 단어 카드를 **해당 언어의 네이티브 음성으로** 들려준다. 발음은 언어 학습의 핵심이며, 특히 성조(중국어) / 악센트(영어) / 장단음(일본어)을 텍스트만으로 익히기 어렵다.

## Non-Goals

- 합성 음성 커스터마이징(피치/속도 사용자 편집) — v0.1은 엔진 기본값 고정.
- 녹음·재생 비교(사용자 녹음 vs TTS) — Phase 2 후보.
- 클라우드 TTS(네이티브 음성 품질 향상) — 온디바이스 원칙.

## User Stories

- **재생**: 사용자로서, 일기 상세의 각 언어 탭에서 재생 버튼을 눌러 해당 언어 문장을 들을 수 있다.
- **정지**: 사용자로서, 재생 중 다시 누르거나 다른 탭으로 이동하면 재생이 정지된다.
- **오류 안내**: 사용자로서, 기기에 해당 언어 TTS 데이터가 없을 때 명확한 오류 메시지를 받는다(예: "Language not supported: ja").

## 구현 요점

`utils/TtsService.kt` — `@Singleton`, `TextToSpeech` 래퍼.

- **상태 공개**: `StateFlow<TtsState>`
  - `Idle`
  - `Playing(language: AppLanguage, utteranceId: String)`
  - `Error(message: String)`
- **Locale 매핑**: `AppLanguage.toLocale()` 사용. `isLanguageAvailable()` 결과가 `LANG_MISSING_DATA` 또는 `LANG_NOT_SUPPORTED`면 즉시 `Error` 상태.
- **완료 감지**: `UtteranceProgressListener`로 `Playing` → `Idle` 자동 복귀.
- **속도/피치**: 엔진 기본값 유지 (사용자 편집 불가, v0.1 제약).

## Acceptance Criteria

- [x] 4개 언어 각각에 대해 재생이 가능한 기기에서 정상 재생된다.
- [x] 재생 중 동일 언어 버튼 재탭 시 정지 → Idle 복귀.
- [x] 미지원 언어에 대해 `TtsState.Error("Language not supported: xx")`가 노출되고 UI가 에러 문구를 표시한다.
- [x] 앱을 백그라운드로 보내거나 다른 화면으로 이동하면 재생이 중단된다.
- [ ] **사용자 설정 속도**: Settings에서 재생 속도(0.5x–1.5x)를 조절할 수 있다. *(Phase 2)*

## 알려진 제약

- **에뮬레이터 주의**: Android 에뮬레이터는 기본적으로 일부 언어 TTS 데이터가 없어 `Error`가 자주 발생. 실기기 또는 "설정 → 접근성 → TTS" 에서 해당 언어 데이터 설치 필요.
- **중국어 화자 기본값**: 기기에 따라 보통화(표준 중국어) 또는 광둥어 중 하나가 기본. 현재 `Locale.SIMPLIFIED_CHINESE`로 지정하므로 일반적으로 보통화.

## Open Questions

- **재생 중 탭 이동 정책**: 현재 정지된다. 학습 흐름상 계속 재생되는 편이 나을 수 있음 — 사용자 설정으로 둘지 결정 필요.
- **긴 문장 분할 재생**: 긴 일기를 한 번에 재생하면 몰입감이 떨어진다. 문장 단위로 끊어 재생하고 문장별 하이라이트?
- **속도 조절 저장 범위**: 사용자 설정으로 추가 시 언어별로 따로 저장할지 전역으로 하나만 둘지.
