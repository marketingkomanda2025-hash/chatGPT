# Floating Dock for Android (prototype)

An Android WebView shell around the published Floating Dock visual prototype.

- Website: https://marketingkomanda2025-hash.github.io/chatGPT/floating-dock/
- Android 8.0+ (min SDK 26), target SDK 34, compile SDK 35.
- Portrait mode and native keyboard.
- Android speech recognizer triggered by the web page's microphone button.
- Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9.

## Build using GitHub Actions

On GitHub, open **Actions → Build Floating Dock APK**, open the newest successful run,
and download the artifact **FloatingDock-debug-APK**. Extract the ZIP to obtain
`app-debug.apk`. The APK is signed with a temporary DEBUG signing key.
For future updates, preserve a signing key; an APK built on a different runner
may not update over the previously installed one.

To build locally, install Android SDK Platform 35 and Gradle 8.9, then run:

```sh
cd floating-dock-android
gradle assembleDebug
```

The APK will be under `app/build/outputs/apk/debug/app-debug.apk`.

## Notes

The application loads the remote GitHub Pages UI and therefore needs internet.
The conversational bot on this page is simulated, not an external AI API.
This is an experimental build, not ready for store publication. The app's
WebView JavaScript bridge is constrained to the specific trusted prototype URL.

## Hryundel test notifications (0.2)

The web chat displays the button **🐽 Пуш через 20 секунд**.
On an Android device, tap it, grant the notification permission on
Android 13+, and leave the app. Android's inexact alarm schedules one
local notification in approximately 20 seconds (the operating system
may delay it in battery saving / idle modes). Tapping the notification
opens the Hryundel chat.

- Uses `POST_NOTIFICATIONS` runtime permission on Android 13+.
- Uses `AlarmManager.setAndAllowWhileIdle` without requesting exact alarms.
- The receiver is private (`android:exported="false"`).
- No Firebase, push server, tracking, or recurring notifications.
- This is a test-only version; remote push requires a separate backend.
- The new debug APK may have a different signing key from the old build.
  If Android refuses an in-place update, uninstall Floating Dock 0.1
  before installing 0.2. Clearing the app loses its local data.
