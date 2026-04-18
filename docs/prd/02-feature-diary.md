# 02 — Feature: Diary (일기)

[← 목차로](../../PRD.md) · **status: shipped (v0.1)**

## Goal

사용자가 자신에게 가장 편한 언어로 **마찰 없이** 일기를 작성하게 한다. 저장과 동시에 나머지 3개 언어 번역 파이프라인을 비동기로 트리거해, 사용자가 목록/상세 화면에서 번역 결과를 자연스럽게 소비할 수 있게 한다.

## Non-Goals

- 부유한 텍스트 에디터(마크다운, 이미지 삽입, 첨부) — v0.1은 순수 텍스트.
- 카테고리/폴더/검색 — v0.1은 단일 시간순 목록.
- 작성 중 자동 번역 프리뷰 — 저장 시점에만 번역.

## User Stories

- **작성**: 사용자로서, 일기 작성 화면에서 제목과 본문을 입력하고 원문 언어를 선택해 저장할 수 있다.
- **목록**: 사용자로서, 과거에 쓴 일기를 시간 역순으로 훑으며 제목·발췌·원문 언어를 한눈에 확인할 수 있다.
- **상세**: 사용자로서, 일기 상세에서 원문과 3개 번역을 탭으로 전환하며 읽을 수 있고, 각 탭에서 TTS 재생이 가능하다.
- **재시도**: 사용자로서, 번역이 실패했을 때 해당 탭에서 버튼 한 번으로 재번역할 수 있다.

## UX Flow

```
[Diary Tab]
  └─ DiaryListScreen ─── FAB("쓰기") ──→ [WriteDiaryScreen]
        │                                     │
        │  항목 탭                             │  저장
        ▼                                     ▼
  [DiaryDetailScreen]  ←──── navigation ─── (뒤로)
        ├─ 원문 탭 (원문 언어, 배지 "(원문)")
        ├─ 번역 탭 × 3 (PENDING=스피너 / SUCCESS=텍스트 / ERROR=재시도 버튼)
        └─ 각 탭 하단에 TTS 재생 버튼
```

상세 내비게이션 규칙은 `Navigation.kt`의 `WriteDiary`, `DiaryDetail(id)` 타입 세이프 라우트 참조.
상세/쓰기 화면에서는 하단 내비게이션 바가 숨겨진다 (`hideBottomNavigationRoutes`).

## Data Model

| Entity | 핵심 필드 | 비고 |
|--------|----------|------|
| `DiaryEntryEntity` | `id`, `title`, `content`, `sourceLanguage`, `mood?`, `tags`(JSON), `createdAt`, `updatedAt` | `sourceLanguage`는 `AppLanguage.code` 문자열. |
| `TranslationEntity` | `id`, `diaryEntryId`(FK CASCADE), `targetLanguage`, `translatedContent`, `translationStatus`, `errorMessage?`, `translatedAt`, `modelVersion?` | `(diaryEntryId, targetLanguage)` UNIQUE. |

도메인 매핑: `DiaryEntry.toEntity()` / `DiaryEntryEntity.toDomain()` (`domain/model/DiaryEntry.kt`).

## Write → Translate 파이프라인

`WriteDiaryViewModel.save()`는 아래 순서를 지킨다.

1. `DiaryEntry`를 생성하고 Room에 insert.
2. 원문 언어를 제외한 3개 언어 각각에 대해 `Translation`을 **PENDING** 상태로 upsert(placeholder).
3. 각 언어별로 `TranslationEngine.translate(source, target, content)`를 호출.
4. 완료 시 **SUCCESS**(혹은 실패 시 **ERROR** + `errorMessage`)로 upsert.

이 순서가 깨지면 상세 화면이 스피너 → 텍스트 전환을 못 한다. 수정 시 `DiaryDetailScreen`의 Flow 구독 로직과 짝을 맞출 것.

## Acceptance Criteria

- [x] 저장 직후 상세 화면에 진입하면 3개 번역 탭이 모두 PENDING 스피너로 시작한다.
- [x] 각 언어 번역은 완료되는 대로 독립적으로 SUCCESS 상태로 전환된다(all-or-nothing 아님).
- [x] ERROR 탭에서 "재시도" 버튼을 누르면 해당 언어만 재번역된다.
- [x] 일기 삭제 시 연결된 모든 Translation이 CASCADE로 함께 삭제된다.
- [x] 앱을 강제 종료 후 재실행해도 PENDING 상태로 남아있던 번역은 다시 실행되거나(추후) 에러로 마감된다.

## Open Questions

- **편집 기능**: 현재 작성 후 수정 UX가 없다. 본문 수정 시 기존 번역을 무효화할 것인가, 아니면 diff 기반으로 부분 재번역할 것인가?
- **Mood/Tags UI**: 엔티티에는 `mood`, `tags`가 있으나 작성 화면에 노출되어 있지 않음. 사용하지 않을 것이라면 스키마에서 제거하거나, 쓸 것이라면 입력 UI가 필요.
- **로컬 검색**: 일기 수가 수백 개로 늘었을 때 필터/검색이 필요한가?
