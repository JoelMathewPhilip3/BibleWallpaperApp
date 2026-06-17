# Bible Nature Wallpaper Android App

Android app that automatically creates a scenic nature wallpaper with a Bible verse overlay and sets it as the phone wallpaper about every 8 hours.

## What it does

- Automatically schedules wallpaper generation about every 8 hours using Android WorkManager.
- Uses only scenic nature-style backgrounds: mountains, ocean, forest, night sky, lake, meadow, and waterfall valley.
- Does not use city, building, or urban scenery.
- Avoids repeating Bible verses until the included verse list has been fully used.
- Saves every generated wallpaper inside the app's history gallery.
- Reschedules the wallpaper worker after phone reboot or app update.
- Includes a manual `Create Wallpaper Now` button for testing.

## Important Android behavior

The app must be opened once after installation so Android can start the schedule. After that, it should keep running automatically.

Android may delay background work during Doze, App Standby, or aggressive manufacturer battery optimization. For best results, set this app to **Unrestricted** in Android battery settings.

## GitHub upload

Upload this full project folder to GitHub. The project is structured as a standard Android Studio Kotlin project.

## Build

Open the project in Android Studio, let Gradle sync, then run on an Android phone or build an APK.

## Main files

- `MainActivity.kt` - app screen and saved wallpaper history gallery
- `WallpaperWorker.kt` - background wallpaper generation and setting
- `WallpaperImageCreator.kt` - scenic nature image generator and verse overlay
- `WallpaperHistoryStore.kt` - saves generated wallpapers and tracks used verses
- `WallpaperScheduler.kt` - schedules the 8-hour background worker
- `BootReceiver.kt` - reschedules work after reboot or app update
- `Verse.kt` - Bible verse list

- `SceneStyles.kt` - nature-only scene list used for rotation
