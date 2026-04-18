# 07 — Architecture & Non-Functional Requirements

[← 목차로](../../PRD.md)

이 문서는 **제품 관점에서 알아야 할 시스템 구조**만 다룬다. 빌드 명령·로컬 셋업 절차는 `CLAUDE.md` / `CLAUDE.local.md`의 책임이며 중복하지 않는다.

## 플랫폼 & 빌드 타겟

| 항목 | 값 | 비고 |
|------|----|------|
| Platform | Android only | iOS/웹 out of scope (v1) |
| `compileSdk` | 35 | |
| `minSdk` | 28 | Android 9 Pie. ML Kit Translate 호환 하한 고려. |
| `targetSdk` | 35 | Play Store 정책 추종 |
| Language | Kotlin | JVM target 1.8 (빌드 데몬은 Java 21) |
| UI | Jetpack Compose + Material 3 | |
| Package (code) | `com.august.spiritscribe` | **레거시.** 리네임은 `08-roadmap.md` 참조 |
| App label | `Lingual` | `res/values/strings.xml` → `app_name` |

## 아키텍처 레이어

```
┌─────────────────────────────────────────────────────┐
│ ui/                                                 │
│  └─ {diary, translate, flashcard, settings}         │
│     └─ Screen (Compose) + ViewModel (@HiltViewModel)│
│                                                     │
│                  collectAsStateWithLifecycle         │
│                          ▼                          │
│ domain/                                             │
│  ├─ model/         DiaryEntry, Translation,         │
│  │                 WordCard, AppLanguage            │
│  └─ repository/    DiaryRepository (interface)      │
│                                                     │
│                    Flow<T>                          │
│                          ▼                          │
│ data/                                               │
│  ├─ repository/    DiaryRepositoryImpl (@Singleton) │
│  ├─ local/         Room: AppDatabase, DAOs,         │
│  │                 Entities (lingual.db, v1)        │
│  └─ translation/   TranslationEngine (interface)    │
│                    MlKitTranslationEngine           │
│                                                     │
│ utils/             TtsService (@Singleton)          │
│ di/                DatabaseModule, RepositoryModule │
└─────────────────────────────────────────────────────┘
```

**원칙**

- UI는 ViewModel을 통해서만 도메인을 건드린다. Composable 안에서 DAO/Repository를 직접 부르지 않는다.
- 도메인 모델과 Room 엔티티는 분리되어 있고, 각 모델 파일 하단에 `toDomain()` / `toEntity()` 매퍼를 둔다.
- 도메인 인터페이스(`DiaryRepository`, `TranslationEngine`)에만 UI가 의존하고, 구현체는 `di/` 모듈에서 `@Binds`로 주입한다.
- JSON 직렬화는 `ListSerializer` / `MapSerializer`를 **명시적으로** 사용한다. reified 오버로드는 쓰지 않는다(컴파일 실패 사례 있음 — `CLAUDE.md` 참조).

## Navigation

`Navigation.kt` — 타입 세이프 `@Serializable` 라우트.

| 경로 | 타입 | 화면 | 바텀바 표시 |
|------|------|------|-------------|
| `Screen.Diary` | 문자열 | DiaryListScreen | O |
| `Screen.Translate` | 문자열 | TranslateBrowseScreen | O |
| `Screen.FlashCard` | 문자열 | FlashCardScreen | O |
| `Screen.Settings` | 문자열 | SettingsScreen | O |
| `WriteDiary` | data object | WriteDiaryScreen | X |
| `DiaryDetail(id)` | data class | DiaryDetailScreen | X |
| `FlashCardStudy` | data object | *(미구현, Phase 2)* | X |

상세 라우트에서 바텀바를 숨기는 판단은 `hideBottomNavigationRoutes` + `destination.hasRoute<...>()` 조합.

## 데이터 저장소

- **Room**: `AppDatabase`, version 1, file `lingual.db`. 마이그레이션 미설정, `exportSchema = false`. 스키마 변경 시 현재는 **파괴적 재빌드**가 필요하다. v1 출시 전에 마이그레이션 전략 확립 필요 (→ roadmap).
- **테이블 3개**
  - `DiaryEntryEntity`
  - `TranslationEntity` — `(diaryEntryId, targetLanguage)` UNIQUE, CASCADE on delete.
  - `WordCardEntity` — `sourceEntryId` SET NULL on delete.
- **외부 저장소 없음**: SharedPreferences·DataStore 미사용(설정이 read-only이므로). Phase 2에서 사용자 설정 도입 시 DataStore 권장.

## 의존성 주입 (Hilt)

- `@HiltAndroidApp` — `SpiritScribeApplication` (이름은 레거시).
- `DatabaseModule` — `AppDatabase` + 3개 DAO 제공.
- `RepositoryModule` — `DiaryRepositoryImpl → DiaryRepository`, `MlKitTranslationEngine → TranslationEngine` 바인딩.
- 모든 ViewModel은 `@HiltViewModel`.

## 비기능 요구사항 (NFR)

| 카테고리 | 요구사항 | 검증 방법 |
|----------|---------|-----------|
| 오프라인 | 모델 다운로드 이후 네트워크 없이 번역/TTS 동작 | 기내 모드 수동 테스트 |
| 프라이버시 | 일기·번역·단어가 외부로 전송되지 않음 | 네트워크 캡처, 정적 분석 (`INTERNET` 권한만 선언) |
| 응답성 | 저장 후 PENDING 표시 <500ms, 단문 번역 <3s (모델 로드 후) | 수동 계측 / 향후 벤치 |
| 접근성 | 모든 상호작용 요소에 `contentDescription` | Accessibility Scanner |
| 국제화 | UI 문자열의 언어 일관성 (`AppLanguage.displayName`을 단일 출처로) | 수동 QA |

## 외부 서비스

- **ML Kit Translate** (`17.0.3`) — 번역.
- **ML Kit Language ID** (`17.0.6`) — 설치되어 있으나 현재 활성 사용처 미확인. 자동 감지(Open Question)에 사용할 수 있음.
- **Firebase App Distribution** — QA 배포 전용. 앱 ID는 `com.august.spiritscribe` 레거시에 바인딩.

## 권한 & 매니페스트

- 선언된 권한: **`INTERNET`** 만. (모델 다운로드용)
- `AndroidManifest.xml`의 `com.google.mlkit.vision.DEPENDENCIES = translate` 메타데이터로 설치 시 자동 모델 다운로드 트리거.

## 보안 경계

- 데이터는 전부 앱 프라이빗 저장소(Room DB). 외부 저장소에 쓰지 않는다.
- 앱 잠금(생체/PIN)은 Phase 2. 현재는 OS 잠금 외 보호막 없음.
- Firebase App Distribution 자격증명은 체크인하지 않는다 (`FIREBASE_SECURITY_GUIDE.md` 참조).

## Open Questions

- **마이그레이션 전략**: v1.0 출시 전 Room 마이그레이션을 반드시 정비해야 한다. 현재 destructive rebuild는 실사용자 데이터 손실이므로 배포 불가.
- **관측성**: 현재 애널리틱스/크래시 리포트 없음. Play Store 출시 시 최소한 Crashlytics 도입 여부 결정 필요.
- **테스트 인프라**: 단위 테스트·계측 테스트 뼈대만 있고 실질적 커버리지 불명.
