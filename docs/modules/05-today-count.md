# Module 05 - Today Count Display

## Goal

Display how many users cooked a dish today with low latency and strong fallback reliability.

## Functional Scope

- Per-dish today count read
- Increment today count when completion is submitted
- Redis miss fallback to PostgreSQL daily stats

## API (planned)

- Embedded in:
  - `GET /api/v1/dishes`
  - `GET /api/v1/dishes/{dish_id}`
- Optional dedicated endpoint:
  - `GET /api/v1/dishes/{dish_id}/today-count`

## Data Dependencies

- Redis counter key (`chef:today:cook_count:{yyyyMMdd}:{dishId}`)
- PostgreSQL `dish_daily_stats`

## Acceptance Checklist

- Counter increments only on completion submit
- Read path prefers Redis and falls back to DB
- Key TTL keeps memory bounded
- DB remains accurate source for reconciliation
