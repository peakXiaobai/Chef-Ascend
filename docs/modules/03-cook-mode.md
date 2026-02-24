# Module 03 - Cook Mode

## Goal

Guide users step by step with timer-aware cooking execution.

## Functional Scope

- Start cook session from a dish
- Move through steps with explicit progression
- Manage timer state (start, pause, resume, reset)
- Persist progress safely

## API (planned)

- `POST /api/v1/cook-sessions`
- `GET /api/v1/cook-sessions/{session_id}`
- `POST /api/v1/cook-sessions/{session_id}/steps/{step_no}/start`
- `POST /api/v1/cook-sessions/{session_id}/steps/{step_no}/complete`
- `POST /api/v1/cook-sessions/{session_id}/timer/pause`
- `POST /api/v1/cook-sessions/{session_id}/timer/resume`
- `POST /api/v1/cook-sessions/{session_id}/timer/reset`

## Data Dependencies

- PostgreSQL: `cook_sessions`, `cook_session_steps`, `dish_steps`
- Redis: `chef:session:state:{sessionId}` (runtime timer cache)

## Acceptance Checklist

- Session cannot advance when already completed/abandoned
- Step completion must be in order (`step_no` monotonic)
- Timer state survives brief reconnect via Redis
- Final session elapsed time stored in PostgreSQL
