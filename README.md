# Alcohol Tracker — Android

Native Android app: Kotlin, Jetpack Compose, Room. Package `com.mtss.alcoholtracker`.

## Stack

- Kotlin 1.9.24 · Compose BOM 2024.06 · Material3 · single activity
- Room (logs, dry days, saved drinks, reminders) + Preferences DataStore (settings)
- RevenueCat `purchases:8.25.0` for the Pro entitlement
- minSdk 26 · target/compile 34 · AGP 8.2.2 · Gradle 8.5

## Layout

```
app/src/main/java/com/mtss/alcoholtracker/
  data/           Room entities/DAO/DB, SettingsRepository, BackupManager, ProStore
  domain/         AlcoholMath, StatsEngine, presets, units/currency config
  notifications/  daily reminders, boot re-arm, ongoing BAC status card
  ui/             Root, AppViewModel, theme, components, screens
  util/           CSV export, haptics, formatters
```

## Build

```
./gradlew assembleDebug
```

On a path containing `&`, `gradlew.bat` mis-parses; use instead:

```powershell
java -cp gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Note

This repo holds application code only. Localized `strings.xml` files are
generated artifacts — the translation sources and generator live in the
workspace outside this repo and are not committed here.
