# Bible Nature Wallpaper Android App

Android app that automatically creates a scenic nature wallpaper with a Bible verse overlay and sets it as the phone wallpaper about every 8 hours.

## What it does

- Automatically schedules wallpaper generation about every 8 hours using Android WorkManager.
- Uses a battery-friendly flexible schedule instead of exact alarms, so updates do not need to happen on the dot.
- Uses only scenic nature-style backgrounds: mountains, ocean, forest, night sky, lake, meadow, and waterfall valley.
- Does not use city, building, or urban scenery.
- Avoids repeating Bible verses until the included verse list has been fully used.
- Rotates nature scene themes to avoid repeating the same style too often.
- Saves every generated wallpaper inside the app's history gallery.
- Reschedules the wallpaper worker after phone reboot or app update.
- Includes a manual `Create Wallpaper Now` button for testing.

## Power-saving behavior

The app uses WorkManager with a flexible recurring window and a battery-not-low constraint. This keeps battery drain low because the app only wakes up a few times per day and does not use internet.

Android may still delay background work during Doze, Battery Saver, App Standby, or aggressive manufacturer battery optimization. Because of that, wallpapers update around every 8 hours rather than at an exact time.

## Important Android behavior

The app must be opened once after installation so Android can start the schedule. After that, it should keep running automatically.

## GitHub upload

Upload this full project folder to GitHub. The project is structured as a standard Android Studio Kotlin project.

## Build

Open the project in Android Studio, let Gradle sync, then run on an Android phone or build an APK.

## Main files

- `MainActivity.kt` - app screen and saved wallpaper history gallery
- `WallpaperWorker.kt` - background wallpaper generation and setting
- `WallpaperImageCreator.kt` - scenic nature image generator and verse overlay
- `WallpaperHistoryStore.kt` - saves generated wallpapers and tracks used verses/scenes
- `WallpaperScheduler.kt` - schedules the around-8-hour background worker
- `BootReceiver.kt` - reschedules work after reboot or app update
- `Verse.kt` - Bible verse list
