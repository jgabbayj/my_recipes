# Morsel

Morsel is a native Android recipe manager built with Kotlin and Jetpack Compose. It supports recipe browsing, editing, cook mode, Firebase-backed authentication and storage, and Gemini-assisted parsing for turning free-form recipe text into structured ingredients and steps.

## Features

- Compose UI for dashboard, detail, form, login, username prompt, and cook-mode screens.
- Firebase Authentication and Cloud Firestore integration.
- Local repository layer with default recipes and cached image support.
- Gemini parser for structured recipe extraction.
- Ingredient unit conversion helpers with local tests.
- Material 3 theming and responsive Android navigation.

## Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose and Material 3
- Firebase Auth and Firestore
- AndroidX Navigation 3
- JUnit and kotlinx-coroutines-test

## Local Setup

Requirements: Android Studio or a local Android SDK, JDK 17, and Gradle through the included wrapper.

Firebase configuration is intentionally not committed. Add your local `app/google-services.json` before building Firebase-enabled variants.

```bash
./gradlew test
./gradlew assembleDebug
```

Install a debug build on a connected device or emulator:

```bash
./gradlew installDebug
```

## Project Layout

- `app/src/main/java/com/example/myrecipes/data`: recipe models, repository, Firebase access, Gemini parsing, and conversion helpers.
- `app/src/main/java/com/example/myrecipes/ui`: Compose screens for auth, dashboard, detail, form, and cook mode.
- `app/src/main/java/com/example/myrecipes/theme`: app color, typography, and Material theme definitions.
- `firestore.rules`: Firestore security rules for the project.

## Notes

Generated APKs, local SDK paths, browser test profiles, screenshots, and Firebase config files are ignored so the repository can remain public.
