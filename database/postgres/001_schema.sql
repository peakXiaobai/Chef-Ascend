BEGIN;

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  nickname VARCHAR(64) NOT NULL,
  avatar_url TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dish_categories (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  sort_order INT NOT NULL DEFAULT 100,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dishes (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(160) NOT NULL UNIQUE,
  description TEXT,
  difficulty SMALLINT NOT NULL DEFAULT 1 CHECK (difficulty BETWEEN 1 AND 5),
  estimated_total_seconds INT NOT NULL CHECK (estimated_total_seconds > 0),
  cover_image_url TEXT,
  ingredients_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dish_category_links (
  dish_id BIGINT NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
  category_id BIGINT NOT NULL REFERENCES dish_categories(id) ON DELETE RESTRICT,
  PRIMARY KEY (dish_id, category_id)
);

CREATE TABLE IF NOT EXISTS dish_steps (
  id BIGSERIAL PRIMARY KEY,
  dish_id BIGINT NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
  step_no INT NOT NULL CHECK (step_no > 0),
  title VARCHAR(120) NOT NULL,
  instruction TEXT NOT NULL,
  timer_seconds INT NOT NULL DEFAULT 0 CHECK (timer_seconds >= 0),
  remind_mode VARCHAR(16) NOT NULL DEFAULT 'BOTH'
    CHECK (remind_mode IN ('NONE', 'SOUND', 'VIBRATION', 'BOTH')),
  tips TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (dish_id, step_no)
);

CREATE TABLE IF NOT EXISTS cook_sessions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
  dish_id BIGINT NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT,
  status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS'
    CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
  current_step_no INT NOT NULL DEFAULT 1 CHECK (current_step_no > 0),
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at TIMESTAMPTZ,
  total_elapsed_seconds INT CHECK (total_elapsed_seconds IS NULL OR total_elapsed_seconds >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cook_session_steps (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES cook_sessions(id) ON DELETE CASCADE,
  dish_step_id BIGINT NOT NULL REFERENCES dish_steps(id) ON DELETE RESTRICT,
  step_no INT NOT NULL CHECK (step_no > 0),
  timer_seconds_snapshot INT NOT NULL DEFAULT 0 CHECK (timer_seconds_snapshot >= 0),
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at TIMESTAMPTZ,
  elapsed_seconds INT CHECK (elapsed_seconds IS NULL OR elapsed_seconds >= 0),
  reminder_fired BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE (session_id, step_no)
);

CREATE TABLE IF NOT EXISTS cook_records (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL UNIQUE REFERENCES cook_sessions(id) ON DELETE CASCADE,
  user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
  dish_id BIGINT NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT,
  result VARCHAR(16) NOT NULL CHECK (result IN ('SUCCESS', 'FAILED')),
  rating SMALLINT CHECK (rating BETWEEN 1 AND 5),
  note TEXT,
  cooked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dish_daily_stats (
  stat_date DATE NOT NULL,
  dish_id BIGINT NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
  success_count INT NOT NULL DEFAULT 0 CHECK (success_count >= 0),
  failed_count INT NOT NULL DEFAULT 0 CHECK (failed_count >= 0),
  PRIMARY KEY (stat_date, dish_id)
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_dishes_updated_at ON dishes;
CREATE TRIGGER trg_dishes_updated_at
BEFORE UPDATE ON dishes
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sessions_updated_at ON cook_sessions;
CREATE TRIGGER trg_sessions_updated_at
BEFORE UPDATE ON cook_sessions
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION apply_daily_stats_from_record()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO dish_daily_stats (stat_date, dish_id, success_count, failed_count)
  VALUES (
    NEW.cooked_at::date,
    NEW.dish_id,
    CASE WHEN NEW.result = 'SUCCESS' THEN 1 ELSE 0 END,
    CASE WHEN NEW.result = 'FAILED' THEN 1 ELSE 0 END
  )
  ON CONFLICT (stat_date, dish_id)
  DO UPDATE SET
    success_count = dish_daily_stats.success_count + EXCLUDED.success_count,
    failed_count = dish_daily_stats.failed_count + EXCLUDED.failed_count;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cook_record_daily_stats ON cook_records;
CREATE TRIGGER trg_cook_record_daily_stats
AFTER INSERT ON cook_records
FOR EACH ROW
EXECUTE FUNCTION apply_daily_stats_from_record();

COMMIT;
