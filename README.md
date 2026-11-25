# Internet Radio

Android app with a live Czech radio station list from Radio Browser and background playback.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK 34 installed.
The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds the APK after a push or via Actions -> Build Internet Radio APK -> Run workflow.

Note: live internet radio needs an internet connection. The app currently does not download or cache music for offline playback.
