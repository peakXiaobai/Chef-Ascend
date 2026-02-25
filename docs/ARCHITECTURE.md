# Architecture

## 1) System Components

- API Service
  - Handles dish catalog/detail read APIs
  - Handles cook session lifecycle and completion flow
  - Writes/reads PostgreSQL and Redis
  - Provides Android update metadata API
- APK Release Center (static host, e.g. `:18080`)
  - Hosts APK binaries for multiple apps
  - Hosts `latest.json` metadata per app/channel
- PostgreSQL
  - Durable source of truth for dish metadata, steps, sessions, records, daily stats
- Redis
  - Fast read path for today dish cook counts
  - Temporary runtime state for in-progress timers

## 2) Request Flow

### Catalog and Detail
1. Client requests dish list/detail.
2. API fetches core data from PostgreSQL.
3. API fetches `today_count` from Redis; if key miss, fallback to `dish_daily_stats` in PostgreSQL.
4. API returns merged response.

### Cook Mode
1. Client starts cook session for a dish.
2. API creates `cook_sessions` row and pre-creates step execution rows in `cook_session_steps`.
3. During cooking, client updates current step/timer progress.
4. API stores durable progress in PostgreSQL and hot runtime state in Redis.

### Completion
1. Client submits completion result.
2. API updates session status and inserts `cook_records`.
3. Trigger updates `dish_daily_stats` in PostgreSQL.
4. API runs Redis Lua script to increment today counter atomically.

### Android Update
1. Mobile client requests `GET /api/v1/app/android/latest`.
2. API returns version metadata and release download URL.
3. APK binary is downloaded from the dedicated APK Release Center.

## 3) Consistency Strategy

- Source of truth for analytics is PostgreSQL (`cook_records`, `dish_daily_stats`).
- Redis is acceleration cache and short-lived runtime state.
- On Redis miss, always fallback to PostgreSQL.
- If Redis update fails, the request can still succeed because durable write already exists in PostgreSQL.

## 4) Non-Functional Baselines

- API idempotency for completion endpoint (avoid double submit)
- Timer state update with optimistic checks (`current_step_no` + status)
- Index coverage for dish browse and record history queries
- Minimal auditability via timestamps on all mutable entities
