# Redis Design

## Purpose

Redis is used for:
1. Low-latency today count reads
2. Temporary cook session timer state

PostgreSQL remains the durable source of truth.

## Key Conventions

Prefix: `chef:`

### 1) Today Cook Count

- Key: `chef:today:cook_count:{yyyyMMdd}:{dishId}`
- Type: string integer
- Example: `chef:today:cook_count:20260224:101 -> 53`
- TTL: expire after 3 days to bound memory

### 2) Session Runtime State

- Key: `chef:session:state:{sessionId}`
- Type: hash
- Fields:
  - `current_step_no`
  - `remaining_seconds`
  - `is_paused`
  - `updated_at_epoch`
- TTL: 24h (extended on updates)

### 3) Optional Detail Cache

- Key: `chef:dish:detail:{dishId}`
- Type: string (JSON)
- TTL: 10m
- Invalidate on dish/step update

## Consistency Rules

- On completion submit:
  1. Write PostgreSQL record first.
  2. Increment Redis today counter using Lua script.
- On today count read:
  1. Try Redis key.
  2. If miss, query `dish_daily_stats` in PostgreSQL.

## Failure Handling

- If Redis increment fails, request still succeeds if DB write succeeds.
- Reconciliation job can rebuild Redis counters from `dish_daily_stats`.
