# Delivery Log

## 2026-02-24

- Owner: Codex
- Scope:
  - Defined 5 core modules at architecture/API/data level
  - Added PostgreSQL schema + views/indexes + seed scripts
  - Added Redis key design + atomic today-count Lua script
  - Added init scripts and project structure in README
- Files changed:
  - `README.md`
  - `docs/ARCHITECTURE.md`
  - `docs/COLLABORATION.md`
  - `docs/api/API_V1.md`
  - `docs/data/DB_SCHEMA.md`
  - `docs/data/REDIS_SCHEMA.md`
  - `docs/modules/*.md`
  - `database/postgres/*.sql`
  - `database/redis/001_key_design.md`
  - `database/redis/scripts/incr_today_count.lua`
  - `scripts/init_postgres.sh`
  - `scripts/init_redis.sh`
- Follow-up:
  - Implement API handlers and service layer by module order (01 -> 05)

## 2026-02-24 (Phase 2 - Module 1)

- Owner: Codex
- Scope:
  - Bootstrapped TypeScript + Fastify API service
  - Implemented Module 1 `GET /api/v1/dishes` with filter/sort/pagination
  - Added PostgreSQL + Redis integration (Redis miss fallback to DB values)
  - Updated README with code structure and local startup steps
- Files changed:
  - `.gitignore`
  - `.env.example`
  - `package.json`
  - `tsconfig.json`
  - `src/**`
  - `README.md`
  - `docs/api/API_V1.md`
  - `docs/modules/01-catalog.md`
- Follow-up:
  - Implement Module 2 dish detail endpoint

## 2026-02-24 (Phase 3 - Module 2)

- Owner: Codex
- Scope:
  - Implemented Module 2 `GET /api/v1/dishes/{dish_id}`
  - Added repository query for active dish detail and ordered steps
  - Added Redis today-count override with PostgreSQL fallback
  - Updated API and module docs with implementation status
- Files changed:
  - `src/modules/dishes/repository.ts`
  - `src/modules/dishes/routes.ts`
  - `src/modules/dishes/service.ts`
  - `src/types/dish-detail.ts`
  - `README.md`
  - `docs/api/API_V1.md`
  - `docs/modules/02-dish-detail.md`
- Follow-up:
  - Implement Module 3 cook session and timer endpoints

## 2026-02-24 (Phase 4 - Module 3)

- Owner: Codex
- Scope:
  - Implemented cook session lifecycle endpoints (create/read/start-step/complete-step/timer actions)
  - Added session state machine logic with strict step-order checks
  - Added Redis session-state read/write with PostgreSQL fallback for timer baseline
  - Updated README and API/module docs for module 3
- Files changed:
  - `src/modules/cook-sessions/repository.ts`
  - `src/modules/cook-sessions/service.ts`
  - `src/modules/cook-sessions/routes.ts`
  - `src/types/cook-session.ts`
  - `src/server.ts`
  - `README.md`
  - `docs/api/API_V1.md`
  - `docs/modules/03-cook-mode.md`
- Follow-up:
  - Implement Module 4 completion endpoint and user history endpoint

## 2026-02-24 (Phase 5 - Module 4)

- Owner: Codex
- Scope:
  - Implemented completion endpoint with idempotent record insertion (`session_id` unique)
  - Added session final status transition (`COMPLETED`/`ABANDONED`) on first completion
  - Implemented user cook record history endpoint with pagination
  - Added Redis counter increment on new completion with DB fallback for today count
  - Updated docs and README for module 4 implementation status
- Files changed:
  - `src/modules/cook-records/repository.ts`
  - `src/modules/cook-records/service.ts`
  - `src/modules/cook-records/routes.ts`
  - `src/types/cook-record.ts`
  - `src/server.ts`
  - `README.md`
  - `docs/api/API_V1.md`
  - `docs/modules/04-cook-record.md`
- Follow-up:
  - Implement optional dedicated today-count endpoint (module 5)

## 2026-02-24 (Phase 6 - Android App MVP)

- Owner: Codex
- Scope:
  - Created native Android app project under `apps/android-app` (Kotlin + Jetpack Compose)
  - Implemented catalog, dish detail, cook mode, completion, and records screens
  - Integrated Android app with existing backend APIs via Retrofit
  - Added Android app docs and updated root README structure
- Files changed:
  - `apps/android-app/**`
  - `docs/mobile/ANDROID_APP.md`
  - `README.md`
- Follow-up:
  - Add Android auth flow, offline storage, and background timer notifications

## 2026-02-24 (Phase 7 - Access Logs + Seed Data + CN/EN i18n)

- Owner: Codex
- Scope:
  - Added configurable API access logs (`LOG_LEVEL`) with per-request method, path, status, latency, and IP
  - Added `005_seed_feature_data.sql` to seed end-to-end test data for catalog/detail/cook/completion/records/today-count flows
  - Updated PostgreSQL init script and DB docs to include the new feature seed script
  - Switched Android app to Simplified Chinese default UI with in-app language switch (Chinese/English) in settings
  - Localized catalog/detail/cook/completion/records/settings screens and string resources
- Files changed:
  - `src/server.ts`
  - `src/config/env.ts`
  - `.env.example`
  - `database/postgres/005_seed_feature_data.sql`
  - `scripts/init_postgres.sh`
  - `database/postgres/README.md`
  - `apps/android-app/app/src/main/**`
  - `apps/android-app/app/build.gradle.kts`
- Follow-up:
  - Add automated API tests for seed-data assertions and Android UI locale regression tests

## 2026-02-24 (Phase 8 - Dish DB Visual Admin Console)

- Owner: Codex
- Scope:
  - Added a browser-based dish management console at `/admin/dishes`
  - Added admin APIs for category list, dish list/detail, create/update, and active-state switching
  - Added server-side validation and conflict handling for slug uniqueness and invalid category references
  - Added guarded step update behavior (existing dishes do not allow reducing step count)
  - Updated project docs with admin entrypoint and API contract
- Files changed:
  - `src/modules/admin-dishes/repository.ts`
  - `src/modules/admin-dishes/service.ts`
  - `src/modules/admin-dishes/routes.ts`
  - `src/modules/admin-dishes/ui.ts`
  - `src/server.ts`
  - `README.md`
  - `docs/api/API_V1.md`
- Follow-up:
  - Add auth/permission layer for admin console before exposing to public internet

## 2026-02-24 (Phase 9 - Android In-App Update Check)

- Owner: Codex
- Scope:
  - Added backend endpoint `GET /api/v1/app/android/latest` for APK version metadata and download URL
  - Added update-related env config (`ANDROID_APK_FILENAME`, `ANDROID_APK_VERSION_CODE`, `ANDROID_APK_VERSION_NAME`, `ANDROID_APK_RELEASE_NOTES`)
  - Added Android settings-page update capability: check latest version, show release info, and trigger APK download via DownloadManager
  - Bumped Android app version to `0.2.0` (`versionCode=2`) for upgrade detection
- Files changed:
  - `src/server.ts`
  - `src/config/env.ts`
  - `.env.example`
  - `apps/android-app/app/build.gradle.kts`
  - `apps/android-app/app/src/main/java/com/chefascend/mobile/ChefAscendApp.kt`
  - `apps/android-app/app/src/main/java/com/chefascend/mobile/data/api/ChefApiService.kt`
  - `apps/android-app/app/src/main/java/com/chefascend/mobile/data/model/ApiModels.kt`
  - `apps/android-app/app/src/main/java/com/chefascend/mobile/data/repository/ChefRepository.kt`
  - `apps/android-app/app/src/main/java/com/chefascend/mobile/ui/screens/SettingsScreen.kt`
  - `apps/android-app/app/src/main/res/values/strings.xml`
  - `apps/android-app/app/src/main/res/values-en/strings.xml`
  - `README.md`
  - `docs/api/API_V1.md`
- Follow-up:
  - Add APK signature verification prompt and optional force-update policy field
