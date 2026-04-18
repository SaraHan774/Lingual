# Lingual

A multilingual diary app for polyglots. Write once in one of Korean / English / Japanese / Chinese, get the other three translated on-device, listen to each via TTS, and build a vocabulary deck from the entries you write.

## Features

- Per-entry source language (한국어 / English / 日本語 / 中文)
- Automatic translation to the three other languages (Google ML Kit, on-device, offline after first model download)
- Text-to-speech playback per translation using the system `TextToSpeech` engine
- Type-safe Compose navigation with a bottom bar (Diary / Translate / Flashcards / Settings)
- Flashcard deck backed by Room with mastery levels and review scheduling

## Tech

Kotlin · Jetpack Compose · Room · Hilt · ML Kit Translation 17.0.3 · Android TextToSpeech · Firebase App Distribution

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.august.spiritscribe/.MainActivity
```

Firebase App Distribution:

```bash
./deploy.sh debug "Release notes"
./deploy.sh release "Release notes"
```

## Notes

- `applicationId` remains `com.august.spiritscribe` while Firebase config is keyed to it. The launcher label is `Lingual`.
- First translation per language pair downloads a ~30MB ML Kit model; subsequent calls are offline.
- Android `minSdk = 28`, `targetSdk = 35`.
