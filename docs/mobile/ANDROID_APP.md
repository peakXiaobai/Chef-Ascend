# Android App Plan and Structure

## Scope

Current Android app implementation covers:

1. Catalog page (`GET /api/v1/dishes`)
2. Dish detail page (`GET /api/v1/dishes/{dish_id}`)
3. Cook mode page (session/timer/step actions)
4. Completion submit (`POST /api/v1/cook-sessions/{session_id}/complete`)
5. User record history (`GET /api/v1/users/{user_id}/cook-records`)

## Code Location

- Android app root: `apps/android-app`
- Compose UI screens: `apps/android-app/app/src/main/java/com/chefascend/mobile/ui/screens`
- API client: `apps/android-app/app/src/main/java/com/chefascend/mobile/data/api`
- Data repository: `apps/android-app/app/src/main/java/com/chefascend/mobile/data/repository`

## Navigation

- `catalog` -> `detail/{dishId}`
- `detail/{dishId}` -> `cook/{dishId}/{sessionId}`
- `cook/{dishId}/{sessionId}` -> `completion/{sessionId}/{recordId}/{result}/{todayCount}`
- any screen -> `records`

## Runtime Config

- API base URL: `BuildConfig.API_BASE_URL`
- Default user id for MVP: `BuildConfig.DEFAULT_USER_ID`

Both are currently defined in `apps/android-app/app/build.gradle.kts`.

## Next Improvements

1. Add local persistence (Room) for last used dishes and records
2. Add push/local notification for timer completion in background
3. Add login/user switch instead of fixed user id
4. Add retry/backoff and unified error UI
5. Add instrumentation and UI tests
