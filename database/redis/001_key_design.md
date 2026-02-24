# Redis Key Design

## Key: Today Dish Cook Count

- Pattern: `chef:today:cook_count:{yyyyMMdd}:{dishId}`
- Value: integer
- TTL: 259200 seconds (3 days)

Example:
- `chef:today:cook_count:20260224:101 = 53`

## Key: Session Runtime State

- Pattern: `chef:session:state:{sessionId}`
- Type: hash
- Fields:
  - `current_step_no`
  - `remaining_seconds`
  - `is_paused`
  - `updated_at_epoch`
- TTL: 86400 seconds (24 hours)

## Lua Script

Use `scripts/incr_today_count.lua` to atomically increment counter and keep TTL.

Input:
- `KEYS[1]` = counter key
- `ARGV[1]` = ttl seconds

Output:
- incremented count value
