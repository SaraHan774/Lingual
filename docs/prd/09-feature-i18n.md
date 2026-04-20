# 09 — UI 국제화 (i18n) — strings.xml 전환

[← 목차로](../../PRD.md)

> **참고**: UX Review 2026-04-19 G-01 (P0) — "TopAppBar titles & UI labels are Korean-only. Core contradiction: app targets polyglots but UI is monolingual."

## Goal

모든 화면에 하드코딩된 한국어 UI 문자열(TopAppBar 제목, 빈 상태 메시지, 버튼 라벨, placeholder, contentDescription)을 `strings.xml` 로 추출하고, `values-en/strings.xml` 에 영어 번역을 추가한다. 시스템 로케일에 따라 앱 UI 언어가 자동으로 전환된다.

## Non-Goals

- JA / ZH UI 번역 추가 (Phase 2)
- 앱 내 언어 설정 UI (Settings 탭에서 UI 언어를 수동 선택 — Phase 2, UX Review S-01)
- DiaryDetailScreen 문자열 추출 (이번 스코프 외 — 대상 파일 5개에 집중)
- 일기 콘텐츠·번역 결과 자체의 언어 변경 (ML Kit 번역 영역, 이번 스코프 외)

## 대상 파일

| 파일 | 하드코딩 문자열 (현황) |
|------|----------------------|
| `DiaryListScreen.kt` | TopAppBar "내 일기", EmptyState "아직 작성된 일기가 없습니다.…", 제목없음 "제목 없음", FAB contentDescription "새 일기 작성", 번역 중 "번역 중 %d/%d", 번역 실패 "번역 실패" |
| `WriteDiaryScreen.kt` | TopAppBar "새 일기" / "일기 편집", 뒤로 "뒤로", 언어 레이블 "작성 언어", 제목 필드 "제목", 내용 필드 placeholder "오늘 있었던 일을 적어보세요", 버튼 "저장하고 번역하기" / "저장", 저장 중 "저장 중…", 다이얼로그 제목/본문/버튼 |
| `TranslateBrowseScreen.kt` | TopAppBar "번역 탐색", FilterChip "전체", EmptyState "조건에 맞는 일기가 없습니다.", 원문 "원문: %s", 제목없음 "제목 없음" |
| `FlashCardScreen.kt` | TopAppBar "단어장", FilterChip "전체" / "즐겨찾기" / "복습 예정", EmptyState "아직 단어 카드가 없습니다.…" / "즐겨찾기한 카드가 없습니다." / "복습 예정인 카드가 없습니다.", StatsBanner "카드 %d개 · 즐겨찾기 %d개 · 오늘 복습 %d개", 숙련도 레이블 "모름" / "어려움" / "보통" / "완벽", "탭하여 뜻 보기", 삭제 다이얼로그, 즐겨찾기 contentDescription, 삭제 contentDescription, 복습횟수 "복습 %d회" |
| `SettingsScreen.kt` | TopAppBar "설정", SectionTitle "번역 엔진" / "TTS" / "앱 정보", 각 설명 텍스트 |

## User Stories

- **AS** a non-Korean polyglot user (EN/JA/ZH system locale), **I WANT** the app UI to appear in my system language, **SO THAT** the app doesn't feel exclusive to Korean speakers.
- **AS** a Korean user, **I WANT** the app to still show Korean labels when my system locale is KO, **SO THAT** there's no regression.

## UX Flow

1. 사용자가 기기 시스템 언어를 영어로 설정한 채 앱을 실행한다.
2. 모든 대상 화면의 TopAppBar 제목, 버튼 라벨, 빈 상태 메시지가 영어로 표시된다.
3. 시스템 언어가 한국어면 기존과 동일하게 한국어로 표시된다.
4. JA/ZH 시스템 로케일이면 fallback으로 한국어(기본값) 표시 — Phase 2에서 JA/ZH 리소스 추가.

## Data Model / 구현 노트

- `values/strings.xml` — 기본(KO). 기존 파일에 신규 키 추가.
- `values-en/strings.xml` — 영어 번역. 기존 파일에 신규 키 추가.
- 각 Composable 에서 `stringResource(R.string.<key>)` 또는 `pluralStringResource(R.plurals.<key>, count)` 로 교체.
- 복수형(번역 중 %d/%d, 카드 %d개 등)은 `String.format` 또는 `stringResource(id, arg1, arg2)` 인수형 사용.
- `masteryLabels` 리스트는 `stringArrayResource(R.array.flashcard_mastery_labels)` 로 추출.
- `contentDescription = null` 인 아이콘 중 **상호작용 요소**에만 contentDescription 을 strings.xml 에 추가.

## Acceptance Criteria

### AC-i18n-01: DiaryListScreen
- [ ] TopAppBar 제목이 KO: "내 일기", EN: "My Diary" 로 로케일에 따라 표시된다.
- [ ] EmptyState 메시지가 로케일에 따라 KO/EN 으로 표시된다.
- [ ] 제목 없는 일기 아이템의 "제목 없음" fallback 텍스트가 로케일 대응된다.
- [ ] FAB contentDescription이 strings.xml 에서 온다.
- [ ] "번역 중 N/M", "번역 실패" 텍스트가 strings.xml 에서 온다.

### AC-i18n-02: WriteDiaryScreen
- [ ] TopAppBar 제목이 신규 모드: KO "새 일기" / EN "New Diary", 편집 모드: KO "일기 편집" / EN "Edit Diary".
- [ ] 뒤로 아이콘 contentDescription이 strings.xml 에서 온다.
- [ ] "작성 언어" 레이블이 로케일 대응된다.
- [ ] 제목 필드 label "제목" → strings.xml.
- [ ] 내용 필드 placeholder "오늘 있었던 일을 적어보세요" → strings.xml 로케일 대응.
- [ ] 저장 버튼: "저장하고 번역하기" / "저장" / "저장 중…" 이 strings.xml 에서 온다.
- [ ] 나가기 확인 다이얼로그(제목·본문·확인·취소 버튼)가 strings.xml 에서 온다.

### AC-i18n-03: TranslateBrowseScreen
- [ ] TopAppBar 제목 KO "번역 탐색" / EN "Browse Translations".
- [ ] FilterChip "전체" → strings.xml (DiaryList와 동일 키 공유 가능).
- [ ] EmptyState "조건에 맞는 일기가 없습니다." → strings.xml.
- [ ] "원문: %s" 포맷 문자열 → strings.xml.
- [ ] "제목 없음" → DiaryListScreen 와 동일 키 공유.

### AC-i18n-04: FlashCardScreen
- [ ] TopAppBar 제목 KO "단어장" / EN "Flashcards".
- [ ] FilterChip "전체" → 공용 키(translate_browse 와 공유 가능), "즐겨찾기" / "복습 예정" → strings.xml.
- [ ] EmptyState 3종 메시지(All / Favorites / DueForReview)가 strings.xml 에서 온다.
- [ ] StatsBanner 포맷 문자열이 strings.xml 에서 온다.
- [ ] 숙련도 레이블 4종("모름"/"어려움"/"보통"/"완벽")이 string-array 또는 개별 키로 strings.xml 에서 온다.
- [ ] "탭하여 뜻 보기" → strings.xml.
- [ ] 삭제 확인 다이얼로그(제목·본문 포맷·확인·취소)가 strings.xml 에서 온다.
- [ ] 즐겨찾기·삭제 contentDescription → strings.xml.
- [ ] "복습 %d회" → strings.xml.

### AC-i18n-05: SettingsScreen
- [ ] TopAppBar "설정" → strings.xml.
- [ ] SectionTitle 3종("번역 엔진" / "TTS" / "앱 정보") → strings.xml.
- [ ] 각 설명 본문 텍스트(번역 엔진 설명, 모델 다운로드 안내, TTS 설명, 앱 버전 문구, 언어 지원 안내) → strings.xml.

### AC-i18n-06: 에러/엣지 케이스
- [ ] JA/ZH 시스템 로케일: `values-ja`·`values-zh` 폴더 없음 → KO fallback 표시 (Android 기본 동작, 별도 코드 불필요).
- [ ] 빌드에서 `R.string.*` 참조 누락 시 컴파일 에러로 즉시 감지 — 런타임 크래시 없음.
- [ ] 기존 `flashcard_*` 키 중 이번 범위에 영향받는 키가 있으면 양쪽 `values/` 와 `values-en/` 모두 갱신.

### AC-i18n-07: 내부 일관성
- [ ] "전체" (all), "취소" (cancel), "저장" (save), "제목 없음" (no title) 같이 여러 화면에 등장하는 동일 개념은 **공용 키** 하나로 통일.
- [ ] 신규 키 명명 규칙: `<screen_prefix>_<element>` (예: `diary_list_title`, `write_diary_placeholder_content`).

## Open Questions (결정됨)

- **JA/ZH 번역 타이밍**: Phase 2. 이번 PR 에선 KO + EN 만.
- **앱 내 언어 오버라이드 설정**: Phase 2 (Settings S-01). 이번엔 시스템 로케일 그대로.
- **복수형 처리**: `번역 중 N/M` 은 카운트 2개라 plurals 불필요, `stringResource(id, n, m)` 인수형으로 처리.

## Status

`in-progress`
