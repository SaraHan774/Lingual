# 08 — Roadmap & Open Questions

[← 목차로](../../PRD.md)

이 문서는 **앞으로 할 일의 목록**이다. 우선순위·일정은 사용자(제품 오너)가 확정하며, `prd-curator` 에이전트는 항목을 관리할 뿐 스스로 스코프를 결정하지 않는다.

## 현재 단계

- **v0.1 MVP** — 로컬 테스트 / Firebase App Distribution 내부 배포 단계.
- **다음 마일스톤**: v1.0 Play Store 공개 (일정 미정).

## Phase 2 기능 후보 (우선순위 대략순)

### P0 — Play Store 출시 전 필수

1. **Room 마이그레이션 정비**
   현재 destructive rebuild. 실사용자 데이터 손실 방지 위해 v1 → v2 마이그레이션 스크립트 기준 확립 필수. (`07-architecture.md` 참조)
2. **패키지 리네임 `com.august.spiritscribe` → `com.august.lingual`**
   - Firebase Console에 신규 Android 앱 추가 → 새 `google-services.json` 다운로드 & 커밋.
   - `applicationId`, Kotlin 패키지 선언 전체, `firebaseAppDistribution` app id 업데이트.
   - Android Studio **Refactor → Rename**으로 일괄 처리.
   - `SpiritScribeApplication` 클래스명 → `LingualApplication`으로 함께 리네임.
3. **개인정보 처리방침 + 스토어 리스팅 텍스트 (4개 언어)**
   Play Console 제출용. 데이터 안전성 섹션에 "외부 전송 없음" 명시.
4. **Crashlytics 도입**
   최소한의 관측성. 개인정보 전송 없이 스택트레이스만.
5. **앱 아이콘·스플래시 4개 언어 느낌에 맞춘 브랜딩**

### P1 — 사용자 만족도 상승

6. **자동 단어 추출** (→ `04-feature-flashcard.md`)
   ML Kit Entity Extraction 또는 간단한 빈도/난이도 휴리스틱.
7. **학습 세션 모드 (`FlashCardStudy` 라우트 활성화)**
   즐겨찾기/숙련도 낮음 카드 우선, 세션 종료 후 통계.
8. **일기 편집 기능 + 편집 시 번역 무효화 정책** ← [Resolved 2026-04-19] `02-feature-diary.md` AC-E01~E10 참조
9. **TTS 속도·피치 사용자 설정** (→ `06-feature-settings.md`)
10. **검색/필터 (일기 본문 검색)**
11. **Translate Playground 히스토리 영속화** (→ `03-feature-translation.md` Open Questions)
    현재 Playground 는 비영속(ViewModel scope). 사용자가 "어제 찾아봤던 단어" 를 다시 찾을 수 있도록 최근 10~20건 로컬 캐시 제공.
12. **Translate Playground 기반 문장 번역 퀴즈/학습 세션** (→ `03-feature-translation.md` Non-Goals)
    Playground 의 정답 비교 기반 학습 모드. v0.1 에서 의도적으로 보류한 항목.
13. **Diary 탭 원문 언어 필터 (T-02 회수분)** (→ `02-feature-diary.md`, `03-feature-translation.md`)
    T-02 에서 `TranslateBrowseScreen` 을 제거하면서 "원문 언어 FilterChip" 기능이 임시 손실. 03 의 "Diary 탭에 흡수돼도 기능 손실 없음" 주장을 충족하기 위해 `DiaryListScreen` 상단에 4개 언어 FilterChip 행을 추가, `selectedSourceLanguage: AppLanguage?` 상태로 목록을 필터링한다 (null = 전체).

### P2 — 확장성·파워 유저

11. **데이터 내보내기/가져오기 (JSON)**
12. **앱 잠금 (생체 / PIN)**
13. **간격 반복 (SM-2 lite)** — `nextReviewAt` 활성화.
14. **예문 자동 채움** — 원문 일기 문장에서 해당 단어 포함 문장을 추출.
15. **자동 언어 감지** — `MlKitLanguageIdentification` 활용해 작성 시 원문 언어 자동 지정.

### P3 — 장기 검토

16. **번체 중국어(`zh-TW`) 분리**
17. **스페인어·프랑스어 등 언어 확장**
18. **iOS 클라이언트** (데이터 포맷 호환 포함 전면 재설계)
19. **번역 품질 피드백 (👍/👎)** — 로컬 집계만으로 할지 외부 수집할지 결정 필요.

## 전 범위 Open Questions 모음

각 기능 문서에 흩어진 Open Questions를 한눈에 본다. 해결되면 해당 원본 문서의 Open Questions에서 제거하고 수용 기준으로 승격.

| 영역 | 질문 | 원본 문서 |
|------|------|-----------|
| 제품 전략 | 언어 확장 기준은? | `01-overview.md` |
| 제품 전략 | 일기 길이 상한? | `01-overview.md` |
| 제품 전략 | 로컬 DB 내보내기/가져오기 제공? | `01-overview.md` |
| ~~Diary~~ | ~~편집 시 번역 무효화 정책~~ **[Resolved 2026-04-19]** 정책 A+C 채택. AC-E01~E10 확정. | `02-feature-diary.md` |
| Diary | Mood/Tags UI 노출 or 스키마 제거 | `02-feature-diary.md` |
| Diary | 검색/필터 도입 시점 | `02-feature-diary.md` |
| Translation | Stale PENDING 회수 전략 | `03-feature-translation.md` |
| Translation | 품질 피드백 수집 수단 | `03-feature-translation.md` |
| Translation | `zh-TW` 분리 여부 | `03-feature-translation.md` |
| Translation | 자동 언어 감지 여부 | `03-feature-translation.md` |
| FlashCard | ~~단어 추가 UI 위치~~ **확정**: 텍스트 선택 후 컨텍스트 메뉴 방식 | `04-feature-flashcard.md` |
| FlashCard | ~~단어 단위 번역 소스~~ **확정**: ML Kit 단어 단위 직접 호출 | `04-feature-flashcard.md` |
| FlashCard | ~~중복 단어 처리 정책~~ **확정**: 별도 카드로 추가(병합 없음) | `04-feature-flashcard.md` |
| TTS | 탭 이동 시 재생 지속 여부 | `05-feature-tts.md` |
| TTS | 긴 문장 분할 재생 | `05-feature-tts.md` |
| TTS | 속도 설정 저장 범위 | `05-feature-tts.md` |
| Settings | 앱 잠금 범위 | `06-feature-settings.md` |
| Settings | 기본 원문 언어 기본값 정책 | `06-feature-settings.md` |
| Settings | 내보내기 포맷 | `06-feature-settings.md` |
| Architecture | Room 마이그레이션 전략 | `07-architecture.md` |
| Architecture | 관측성(Crashlytics 등) 도입 범위 | `07-architecture.md` |
| Architecture | 테스트 커버리지 목표 | `07-architecture.md` |

## 의사결정 로그

변경된 제품 결정은 여기에 날짜순으로 추가한다. 번복 시 기존 항목을 삭제하지 않고 "~~취소선~~ + 사유" 형식으로 남긴다.

| 날짜 | 결정 | 사유 |
|------|------|------|
| 2026-04-18 | PRD를 TOC 루트 + `docs/prd/` 하위 구조로 분리, `prd-curator` 에이전트가 유지 | 기능 단위 변경의 추적성·리뷰 용이성 확보 |
| 2026-04-18 | FlashCard 카드 추가 UI: 텍스트 선택 후 컨텍스트 메뉴 방식 채택 | 별도 버튼보다 자연스러운 텍스트 선택 인터랙션 |
| 2026-04-18 | 단어 카드 번역: ML Kit 단어 단위 직접 호출 (일기 번역 재사용 안 함) | 단어 카드는 독립 생애주기 필요, 문장 번역에서 단어 추출 불가 |
| 2026-04-18 | 중복 단어: 별도 카드로 추가 (병합 없음) | v0.1은 단순성 우선, Phase 2에서 재검토 |
| 2026-04-19 | 일기 편집 번역 무효화 정책: A+C 채택 (전 언어 PENDING 리셋 → 즉시 재번역). 제목만 변경 시 무효화 없음. | diff 기반 부분 번역은 ML Kit 오프라인 특성상 이점 없고 복잡도만 증가 |
| 2026-04-21 | Translate 탭 전면 재설계 (T-02): 기존 `TranslateBrowseScreen` (DiaryList 와 중복된 일기 목록+필터) 제거 → **Translate Playground** (실시간 프리뷰 번역기 + 단어 카드 저장 + TTS) 로 교체 | "번역" 탭 이름과 실제 가치 사이의 간극 제거, 페르소나 '민지' 의 "사전 찾다 흐름 끊김" pain point 해결, FlashCard 와 자연스러운 연결 |
