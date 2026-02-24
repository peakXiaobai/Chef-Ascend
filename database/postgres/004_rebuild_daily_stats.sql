BEGIN;

TRUNCATE TABLE dish_daily_stats;

INSERT INTO dish_daily_stats (stat_date, dish_id, success_count, failed_count)
SELECT
  cooked_at::date AS stat_date,
  dish_id,
  SUM(CASE WHEN result = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
  SUM(CASE WHEN result = 'FAILED' THEN 1 ELSE 0 END) AS failed_count
FROM cook_records
GROUP BY cooked_at::date, dish_id;

COMMIT;
