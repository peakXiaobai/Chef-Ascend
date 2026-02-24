BEGIN;

CREATE INDEX IF NOT EXISTS idx_dishes_active_diff_time
  ON dishes (is_active, difficulty, estimated_total_seconds);

CREATE INDEX IF NOT EXISTS idx_dishes_created_at
  ON dishes (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_dish_category_links_category
  ON dish_category_links (category_id, dish_id);

CREATE INDEX IF NOT EXISTS idx_dish_steps_dish_step
  ON dish_steps (dish_id, step_no);

CREATE INDEX IF NOT EXISTS idx_cook_sessions_user_started
  ON cook_sessions (user_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_cook_sessions_dish_started
  ON cook_sessions (dish_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_cook_records_user_cooked
  ON cook_records (user_id, cooked_at DESC);

CREATE INDEX IF NOT EXISTS idx_cook_records_dish_cooked
  ON cook_records (dish_id, cooked_at DESC);

CREATE INDEX IF NOT EXISTS idx_daily_stats_dish_date
  ON dish_daily_stats (dish_id, stat_date DESC);

CREATE OR REPLACE VIEW v_dish_today_counts AS
SELECT
  d.id AS dish_id,
  COALESCE(s.success_count, 0) AS today_success_count,
  COALESCE(s.failed_count, 0) AS today_failed_count,
  COALESCE(s.success_count, 0) + COALESCE(s.failed_count, 0) AS today_total_count
FROM dishes d
LEFT JOIN dish_daily_stats s
  ON s.dish_id = d.id
 AND s.stat_date = CURRENT_DATE;

COMMIT;
