# Chef Ascend Android App

This is the native Android client (Jetpack Compose) for Chef Ascend.

## Features in this phase

- Dish catalog list
- Dish detail view
- Cook mode with step actions and countdown
- Completion submit (success/failed)
- User cook record list

## API base URL

Configured in `app/build.gradle.kts`:

- `BuildConfig.API_BASE_URL`

Default:

- `http://118.196.100.121:3000/`

## How to run

1. Open `apps/android-app` in Android Studio.
2. Let Gradle sync dependencies.
3. Run on Android device/emulator.

## Notes

- This project allows cleartext HTTP for early stage testing.
- In production, switch to HTTPS and remove cleartext config.
