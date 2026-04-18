# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Lingual** is an Android diary app for polyglots. Users write diary entries in one of Korean / English / Japanese / Chinese, and the app auto-translates to the other three languages on-device, reads each translation aloud via TTS, and builds a vocabulary flashcard deck from selected words.

- Public release target: Google Play Store
- Repository/folder name is still `spiritscribe` (legacy); `applicationId` and Kotlin package are still `com.august.spiritscribe` because Firebase (google-services.json, App Distribution) is keyed to it. The user-facing launcher label is `Lingual` (`res/values/strings.xml` → `app_name`).
- A full package rename to `com.august.lingual` requires adding a new Android app in the Firebase Console, downloading a fresh `google-services.json`, updating `firebaseAppDistribution` app id, and refactoring all Kotlin package declarations via Android Studio's Refactor → Rename.

## Architecture

Single-module Android app (`:app`). The legacy `:feature-lab` AR module has been removed. Clean architecture layering:

- `ui/` — Jetpack Compose screens grouped by feature (`diary`, `flashcard`, `translate`, `settings`). Each feature has a `Screen` + `ViewModel`. `@HiltViewModel` + `collectAsStateWithLifecycle` for state.
- `domain/model/` — Domain types (`DiaryEntry`, `Translation`, `WordCard`, `AppLanguage`). Each includes `toDomain()`/`toEntity()` mappers to/from Room entities. JSON fields use explicit `ListSerializer`/`MapSerializer` (not `reified` overloads) to avoid kotlinx.serialization type-inference failures.
- `domain/repository/DiaryRepository` — single interface exposing `Flow`-based observers over entries, translations, and word cards. Default method `targetLanguagesFor(source)` returns the other three `AppLanguage.entries`.
- `data/local/` — Room (`AppDatabase`, version 1, `lingual.db`) with three entities: `DiaryEntryEntity`, `TranslationEntity` (CASCADE delete FK to DiaryEntry, unique `(diaryEntryId, targetLanguage)` index), `WordCardEntity` (SET NULL FK).
- `data/repository/DiaryRepositoryImpl` — `@Singleton`, injects the three DAOs.
- `data/translation/` — `TranslationEngine` interface + `MlKitTranslationEngine` implementation. The engine caches `Translator` instances per `(source, target)` pair and bridges ML Kit's `Task` callbacks to coroutines via `suspendCancellableCoroutine`. `modelVersion = "mlkit-v1"` is persisted with each translation for future migrations.
- `utils/TtsService` — `@Singleton` wrapper around Android `TextToSpeech` that exposes a `StateFlow<TtsState>` (`Idle` / `Playing(language, utteranceId)` / `Error(message)`). Uses `UtteranceProgressListener` to transition back to Idle on completion. `speak()` sets `Locale` from `AppLanguage.toLocale()` and clamps rate/pitch.
- `di/` — `DatabaseModule` provides `AppDatabase` + DAOs, `RepositoryModule` `@Binds` `DiaryRepositoryImpl → DiaryRepository` and `MlKitTranslationEngine → TranslationEngine`.

### Navigation

`Navigation.kt` uses type-safe `@Serializable` routes:
- Bottom nav tabs: `Screen.Diary`, `Screen.Translate`, `Screen.FlashCard`, `Screen.Settings` (plain string routes).
- Detail routes: `WriteDiary`, `DiaryDetail(id)`, `FlashCardStudy` — passed as typed data class / object instances to `navController.navigate()`.
- `MainActivity` hides the bottom bar on detail routes by checking `destination.hasRoute<WriteDiary>() / hasRoute<DiaryDetail>() / hasRoute<FlashCardStudy>()` against `hideBottomNavigationRoutes`.
- `DiaryDetailViewModel` extracts its id via `savedStateHandle.toRoute<DiaryDetail>().id`.

### Write → Translate flow

`WriteDiaryViewModel.save()` (1) creates the `DiaryEntry`, (2) for each of the other three languages upserts a `PENDING` placeholder translation, (3) calls `translationEngine.translate(...)`, (4) upserts the final `SUCCESS` or `ERROR` translation. The detail screen observes the translation flow, so each language tab transitions from spinner → text as models finish.

`DiaryDetailScreen` renders one tab per language (source language marked with `(원문)`), delegates TTS playback to `TtsService`, and exposes a retry button on `ERROR` translations.

## Common Commands

```bash
# Build
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew build

# Install / run on connected device
./gradlew :app:installDebug
adb shell am start -n com.august.spiritscribe/.MainActivity

# Tests
./gradlew :app:test
./gradlew :app:connectedAndroidTest
./gradlew :app:test --tests "com.august.spiritscribe.*"

# Firebase App Distribution (deploy.sh wraps assembleDebug/Release + upload)
./deploy.sh debug "Release notes"
./deploy.sh release "Release notes"
```

## Key Configuration

- `compileSdk = 35`, `minSdk = 28`, `targetSdk = 35`, `ndkVersion = "25.1.8937393"`
- `applicationId = "com.august.spiritscribe"` — do not change without also updating `app/google-services.json` and Firebase App Distribution settings.
- Kotlin JVM target 1.8 (compileOptions + kotlinOptions); build daemon runs Java 21 and warns about the obsolete target — expected.
- `libs.versions.toml` keys: `mlkitTranslate = "17.0.3"`, `mlkitLanguageId = "17.0.6"`.
- `AndroidManifest.xml` declares `com.google.mlkit.vision.DEPENDENCIES = translate` so ML Kit models download automatically after install. Only `INTERNET` permission is requested.

## Data / ML Kit notes

- First translation per language pair downloads a ~30MB model; subsequent calls are offline. The engine calls `downloadModelIfNeeded()` inside `translate()`, so translations are slow on first use.
- `AppLanguage` enum is the single source of truth for language codes (`ko`, `en`, `ja`, `zh`) and display names. ML Kit language tags are resolved via `TranslateLanguage.fromLanguageTag(...)` in `MlKitTranslationEngine`.
- `WordCardEntity` stores per-language translations as a JSON map (`translationsJson`); decoding uses `MapSerializer(String.serializer(), String.serializer())` with explicit serializer parameters — do not replace with reified `Json.encodeToString(value)` or compilation fails.

## Testing

Unit tests live in `app/src/test/`; instrumented tests in `app/src/androidTest/`. Use MockK for Hilt-injected dependencies and `androidx.room.testing` for in-memory Room databases. Database schemas are not exported (`exportSchema = false`) and migrations are not yet set up — version bumps currently require a destructive rebuild.

## Notes for future work

- Word extraction from diary entries is Phase 2 — current UI only supports manual selection; auto-extraction is not implemented.
- TTS honors device-installed voices. On emulators, verify the target Locale's TTS data is downloaded or the speak call emits `TtsState.Error("Language not supported: ...")`.
- `SpiritScribeApplication` class name is legacy; it only calls `ResourceUtils.init(this)`. Safe to rename together with the full package refactor.
