# Module 04 - Completion Record

## Goal

Record final cooking outcome for user history and analytics.

## Functional Scope

- Submit completion result (success/fail)
- Optional rating and notes
- Query user history

## API (planned)

- `POST /api/v1/cook-sessions/{session_id}/complete`
- `GET /api/v1/users/{user_id}/cook-records`

## Data Dependencies

- PostgreSQL: `cook_sessions`, `cook_records`
- Trigger side effect: `dish_daily_stats` increment

## Acceptance Checklist

- Completion API is idempotent by `session_id`
- Record stores `dish_id`, `user_id`, result, timestamp
- Session status transitions to `COMPLETED` or `ABANDONED`
- History query supports pagination by `cooked_at desc`

## Implementation Status

- Status: Implemented (API v1)
- Routes:
  - `POST /api/v1/cook-sessions/{session_id}/complete`
  - `GET /api/v1/users/{user_id}/cook-records`
- Main files:
  - `src/modules/cook-records/routes.ts`
  - `src/modules/cook-records/service.ts`
  - `src/modules/cook-records/repository.ts`
