BEGIN;

-- Ensure deterministic demo users for all module tests.
INSERT INTO users (nickname, avatar_url)
SELECT v.nickname, NULL
FROM (
  VALUES
    ('ChefSeedUserA'),
    ('ChefSeedUserB')
) AS v(nickname)
WHERE NOT EXISTS (
  SELECT 1
  FROM users u
  WHERE u.nickname = v.nickname
);

-- Seed one in-progress session for cook mode testing.
WITH target_dish AS (
  SELECT id AS dish_id
  FROM dishes
  WHERE slug = 'tomato-scrambled-eggs'
  LIMIT 1
),
target_user AS (
  SELECT id AS user_id
  FROM users
  WHERE nickname = 'ChefSeedUserA'
  LIMIT 1
),
seed_session AS (
  INSERT INTO cook_sessions (
    user_id,
    dish_id,
    status,
    current_step_no,
    started_at,
    created_at,
    updated_at
  )
  SELECT
    u.user_id,
    d.dish_id,
    'IN_PROGRESS',
    2,
    '2026-02-24T10:00:00Z'::timestamptz,
    NOW(),
    NOW()
  FROM target_dish d
  CROSS JOIN target_user u
  WHERE NOT EXISTS (
    SELECT 1
    FROM cook_sessions s
    WHERE s.user_id = u.user_id
      AND s.dish_id = d.dish_id
      AND s.started_at = '2026-02-24T10:00:00Z'::timestamptz
  )
  RETURNING id, dish_id
),
resolved_session AS (
  SELECT id, dish_id
  FROM seed_session
  UNION ALL
  SELECT s.id, s.dish_id
  FROM cook_sessions s
  JOIN target_dish d ON d.dish_id = s.dish_id
  JOIN target_user u ON u.user_id = s.user_id
  WHERE s.started_at = '2026-02-24T10:00:00Z'::timestamptz
  LIMIT 1
)
INSERT INTO cook_session_steps (session_id, dish_step_id, step_no, timer_seconds_snapshot)
SELECT
  rs.id,
  ds.id,
  ds.step_no,
  ds.timer_seconds
FROM resolved_session rs
JOIN dish_steps ds ON ds.dish_id = rs.dish_id
ON CONFLICT (session_id, step_no) DO NOTHING;

UPDATE cook_session_steps css
SET
  started_at = '2026-02-24T10:00:00Z'::timestamptz,
  finished_at = '2026-02-24T10:03:00Z'::timestamptz,
  elapsed_seconds = 180,
  reminder_fired = TRUE
WHERE css.session_id IN (
  SELECT s.id
  FROM cook_sessions s
  JOIN dishes d ON d.id = s.dish_id
  WHERE s.started_at = '2026-02-24T10:00:00Z'::timestamptz
    AND d.slug = 'tomato-scrambled-eggs'
)
  AND css.step_no = 1;

UPDATE cook_session_steps css
SET
  started_at = '2026-02-24T10:03:30Z'::timestamptz,
  finished_at = NULL,
  elapsed_seconds = NULL,
  reminder_fired = FALSE
WHERE css.session_id IN (
  SELECT s.id
  FROM cook_sessions s
  JOIN dishes d ON d.id = s.dish_id
  WHERE s.started_at = '2026-02-24T10:00:00Z'::timestamptz
    AND d.slug = 'tomato-scrambled-eggs'
)
  AND css.step_no = 2;

-- Seed completed SUCCESS session and record.
WITH target_dish AS (
  SELECT id AS dish_id
  FROM dishes
  WHERE slug = 'tomato-scrambled-eggs'
  LIMIT 1
),
target_user AS (
  SELECT id AS user_id
  FROM users
  WHERE nickname = 'ChefSeedUserA'
  LIMIT 1
),
seed_session AS (
  INSERT INTO cook_sessions (
    user_id,
    dish_id,
    status,
    current_step_no,
    started_at,
    finished_at,
    total_elapsed_seconds,
    created_at,
    updated_at
  )
  SELECT
    u.user_id,
    d.dish_id,
    'COMPLETED',
    3,
    '2026-02-24T11:00:00Z'::timestamptz,
    '2026-02-24T11:16:00Z'::timestamptz,
    960,
    NOW(),
    NOW()
  FROM target_dish d
  CROSS JOIN target_user u
  WHERE NOT EXISTS (
    SELECT 1
    FROM cook_sessions s
    WHERE s.user_id = u.user_id
      AND s.dish_id = d.dish_id
      AND s.started_at = '2026-02-24T11:00:00Z'::timestamptz
  )
  RETURNING id, dish_id, user_id
),
resolved_session AS (
  SELECT id, dish_id, user_id
  FROM seed_session
  UNION ALL
  SELECT s.id, s.dish_id, s.user_id
  FROM cook_sessions s
  JOIN target_dish d ON d.dish_id = s.dish_id
  JOIN target_user u ON u.user_id = s.user_id
  WHERE s.started_at = '2026-02-24T11:00:00Z'::timestamptz
  LIMIT 1
)
INSERT INTO cook_session_steps (session_id, dish_step_id, step_no, timer_seconds_snapshot)
SELECT
  rs.id,
  ds.id,
  ds.step_no,
  ds.timer_seconds
FROM resolved_session rs
JOIN dish_steps ds ON ds.dish_id = rs.dish_id
ON CONFLICT (session_id, step_no) DO NOTHING;

UPDATE cook_session_steps css
SET
  started_at = '2026-02-24T11:00:00Z'::timestamptz + (css.step_no - 1) * INTERVAL '5 minutes',
  finished_at = '2026-02-24T11:00:00Z'::timestamptz + (css.step_no - 1) * INTERVAL '5 minutes' + INTERVAL '3 minutes',
  elapsed_seconds = GREATEST(css.timer_seconds_snapshot, 60),
  reminder_fired = TRUE
WHERE css.session_id IN (
  SELECT s.id
  FROM cook_sessions s
  JOIN dishes d ON d.id = s.dish_id
  WHERE s.started_at = '2026-02-24T11:00:00Z'::timestamptz
    AND d.slug = 'tomato-scrambled-eggs'
);

UPDATE cook_sessions s
SET total_elapsed_seconds = (
  SELECT COALESCE(SUM(css.elapsed_seconds), 0)
  FROM cook_session_steps css
  WHERE css.session_id = s.id
)
WHERE s.started_at = '2026-02-24T11:00:00Z'::timestamptz;

INSERT INTO cook_records (session_id, user_id, dish_id, result, rating, note, cooked_at)
SELECT
  s.id,
  s.user_id,
  s.dish_id,
  'SUCCESS',
  5,
  'seed:module4-success',
  NOW() - INTERVAL '15 minutes'
FROM cook_sessions s
WHERE s.started_at = '2026-02-24T11:00:00Z'::timestamptz
ON CONFLICT (session_id) DO NOTHING;

-- Seed completed FAILED session and record.
WITH target_dish AS (
  SELECT id AS dish_id
  FROM dishes
  WHERE slug = 'stir-fried-cabbage'
  LIMIT 1
),
target_user AS (
  SELECT id AS user_id
  FROM users
  WHERE nickname = 'ChefSeedUserB'
  LIMIT 1
),
seed_session AS (
  INSERT INTO cook_sessions (
    user_id,
    dish_id,
    status,
    current_step_no,
    started_at,
    finished_at,
    total_elapsed_seconds,
    created_at,
    updated_at
  )
  SELECT
    u.user_id,
    d.dish_id,
    'ABANDONED',
    2,
    '2026-02-24T12:00:00Z'::timestamptz,
    '2026-02-24T12:09:00Z'::timestamptz,
    540,
    NOW(),
    NOW()
  FROM target_dish d
  CROSS JOIN target_user u
  WHERE NOT EXISTS (
    SELECT 1
    FROM cook_sessions s
    WHERE s.user_id = u.user_id
      AND s.dish_id = d.dish_id
      AND s.started_at = '2026-02-24T12:00:00Z'::timestamptz
  )
  RETURNING id, dish_id, user_id
),
resolved_session AS (
  SELECT id, dish_id, user_id
  FROM seed_session
  UNION ALL
  SELECT s.id, s.dish_id, s.user_id
  FROM cook_sessions s
  JOIN target_dish d ON d.dish_id = s.dish_id
  JOIN target_user u ON u.user_id = s.user_id
  WHERE s.started_at = '2026-02-24T12:00:00Z'::timestamptz
  LIMIT 1
)
INSERT INTO cook_session_steps (session_id, dish_step_id, step_no, timer_seconds_snapshot)
SELECT
  rs.id,
  ds.id,
  ds.step_no,
  ds.timer_seconds
FROM resolved_session rs
JOIN dish_steps ds ON ds.dish_id = rs.dish_id
ON CONFLICT (session_id, step_no) DO NOTHING;

UPDATE cook_session_steps css
SET
  started_at = '2026-02-24T12:00:00Z'::timestamptz + (css.step_no - 1) * INTERVAL '3 minutes',
  finished_at = CASE WHEN css.step_no = 1 THEN '2026-02-24T12:03:00Z'::timestamptz ELSE NULL END,
  elapsed_seconds = CASE WHEN css.step_no = 1 THEN GREATEST(css.timer_seconds_snapshot, 60) ELSE NULL END,
  reminder_fired = CASE WHEN css.step_no = 1 THEN TRUE ELSE FALSE END
WHERE css.session_id IN (
  SELECT s.id
  FROM cook_sessions s
  JOIN dishes d ON d.id = s.dish_id
  WHERE s.started_at = '2026-02-24T12:00:00Z'::timestamptz
    AND d.slug = 'stir-fried-cabbage'
);

UPDATE cook_sessions s
SET total_elapsed_seconds = (
  SELECT COALESCE(SUM(css.elapsed_seconds), 0)
  FROM cook_session_steps css
  WHERE css.session_id = s.id
)
WHERE s.started_at = '2026-02-24T12:00:00Z'::timestamptz;

INSERT INTO cook_records (session_id, user_id, dish_id, result, rating, note, cooked_at)
SELECT
  s.id,
  s.user_id,
  s.dish_id,
  'FAILED',
  NULL,
  'seed:module4-failed',
  NOW() - INTERVAL '10 minutes'
FROM cook_sessions s
WHERE s.started_at = '2026-02-24T12:00:00Z'::timestamptz
ON CONFLICT (session_id) DO NOTHING;

-- Guarantee visible today-count demo numbers.
INSERT INTO dish_daily_stats (stat_date, dish_id, success_count, failed_count)
SELECT CURRENT_DATE, d.id, 1, 0
FROM dishes d
WHERE d.slug = 'tomato-scrambled-eggs'
ON CONFLICT (stat_date, dish_id)
DO UPDATE SET
  success_count = GREATEST(dish_daily_stats.success_count, 1);

INSERT INTO dish_daily_stats (stat_date, dish_id, success_count, failed_count)
SELECT CURRENT_DATE, d.id, 0, 1
FROM dishes d
WHERE d.slug = 'stir-fried-cabbage'
ON CONFLICT (stat_date, dish_id)
DO UPDATE SET
  failed_count = GREATEST(dish_daily_stats.failed_count, 1);

COMMIT;
