# CLAUDE.md

## Project

**Lingual** — polyglot을 위한 Android 일기 앱. KO/EN/JA/ZH 중 하나로 작성하면 나머지 3개 언어로 온디바이스 번역, TTS 재생, 단어카드 생성. 배포 타겟: Google Play.

리포지토리명·`applicationId`·Kotlin 패키지는 모두 `com.august.spiritscribe` 로 남아 있다(레거시, Firebase 키가 묶여 있음). 런처 라벨은 `strings.xml` → `app_name = "Lingual"`. 패키지 rename 은 Firebase 재설정 + `google-services.json` 교체가 동반되므로 가볍게 건드리지 않는다.

## Architecture

단일 모듈 `:app`. Clean arch 3 레이어.

- `ui/` — Compose + `@HiltViewModel` + `collectAsStateWithLifecycle`. 기능별 `Screen` + `ViewModel` 쌍.
- `domain/` — 모델(`DiaryEntry`, `Translation`, `WordCard`, `AppLanguage`) + `toDomain()`/`toEntity()` 매퍼 + `DiaryRepository` 인터페이스.
- `data/` — Room (`AppDatabase` v1, `lingual.db`), `DiaryRepositoryImpl`, `TranslationEngine` 인터페이스 + `MlKitTranslationEngine`.
- `di/` — `DatabaseModule`(DB/DAO), `RepositoryModule`(`@Binds` 구현체 ↔ 인터페이스).

**Navigation** — 타입 안정 `@Serializable` 라우트. 바텀탭 4개(`Diary/Translate/FlashCard/Settings`), 디테일 라우트(`WriteDiary`/`DiaryDetail(id)`/`FlashCardStudy`)는 `hideBottomNavigationRoutes` 로 바텀바 숨김. id 추출은 `savedStateHandle.toRoute<DiaryDetail>().id`.

**Write → Translate 파이프라인** — `WriteDiaryViewModel.save()` 가 엔트리 저장 → 나머지 3개 언어에 `PENDING` 플레이스홀더 upsert → `translationEngine.translate()` → 결과를 `SUCCESS`/`ERROR` 로 upsert. `DiaryDetailScreen` 은 Flow 구독으로 탭별 스피너 → 텍스트 전이. 실패 탭에는 재시도 버튼.

## Feature workflow (MANDATORY)

기능 추가·수정·삭제가 포함된 모든 작업 — "분석 후 구현" 요청 포함 — 은 아래 순서를 반드시 따른다.

1. **prd-curator** → PRD.md + docs/prd/ 갱신
2. **prd-reviewer** → UX 관점 검토 (CRITICAL 없어야 통과)
3. **coder** → 구현
4. **code-reviewer** → diff 리뷰
5. **qa-tester** → E2E 검증

`/ship <기능>` 스킬이 이 사이클을 자동으로 실행한다. 가능하면 `/ship` 을 사용할 것.

## Non-obvious rules

- **언어 코드는 반드시 `AppLanguage` enum 경유.** 하드코딩된 `"ko"`/`"en"`/`"ja"`/`"zh"` 금지. ML Kit 변환은 `TranslateLanguage.fromLanguageTag(...)`.
- **kotlinx.serialization 의 reified 오버로드 금지.** `ListSerializer`/`MapSerializer(String.serializer(), String.serializer())` 를 명시해야 컴파일된다. 예: `WordCardEntity.translationsJson`.
- **FK 의도**: `TranslationEntity` → `CASCADE` (일기 삭제 시 번역 삭제), `WordCardEntity` → `SET NULL`.
- **권한**: `INTERNET` 만. 추가 권한은 PRD 근거 필요. ML Kit 모델 자동 다운로드를 위해 `com.google.mlkit.vision.DEPENDENCIES = translate` 를 manifest 에 선언.
- **ML Kit**: 언어쌍당 첫 호출은 ~30MB 모델 다운로드로 느림. 이후 오프라인 가능. `Translator` 는 `(source, target)` 키로 캐시되고 `suspendCancellableCoroutine` 으로 `Task` 콜백을 코루틴으로 bridge.
- **TTS**: `@Singleton TtsService` 가 `StateFlow<TtsState>` (`Idle`/`Playing`/`Error`) 노출. `AppLanguage.toLocale()` 로 Locale 지정. 기기에 TTS 데이터 미설치 Locale 은 `Error("Language not supported: ...")`.
- **Room**: `exportSchema = false`. 마이그레이션 미설정 → version bump 는 destructive rebuild 를 전제로 한다.
- **Compose one-shot 이벤트**: `MutableSharedFlow<Unit>(replay=0, extraBufferCapacity=1)` + `LaunchedEffect(Unit) { collect { ... } }` 패턴 사용. `StateFlow<Int>` + increment 방식은 Composition 재생성(화면 회전 등) 시 마지막 값으로 LaunchedEffect 가 재실행되어 replay 버그 발생.

## Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug && adb shell am start -n com.august.spiritscribe/.MainActivity
./gradlew :app:test --tests "com.august.spiritscribe.*"
./gradlew :app:connectedAndroidTest
./deploy.sh debug|release "Release notes"   # Firebase App Distribution
```

## Config pins

- `compileSdk=35`, `minSdk=28`, `targetSdk=35`, `ndkVersion=25.1.8937393`, Kotlin JVM target `1.8`.
- `libs.versions.toml`: `mlkitTranslate=17.0.3`, `mlkitLanguageId=17.0.6`.

## Testing

- 단위: `app/src/test/`, 계측: `app/src/androidTest/`.
- Hilt 의존성은 MockK 로 대체. Room 은 `androidx.room.testing` in-memory DB.

## Status

- 단어 자동 추출은 Phase 2. 현재 UI 는 수동 선택만.
