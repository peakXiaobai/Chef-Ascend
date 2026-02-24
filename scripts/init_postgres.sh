#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${DATABASE_URL:-}" ]]; then
  echo "ERROR: DATABASE_URL is not set"
  exit 1
fi

psql "$DATABASE_URL" -f database/postgres/001_schema.sql
psql "$DATABASE_URL" -f database/postgres/002_views_and_indexes.sql

if [[ "${SKIP_SEED:-0}" != "1" ]]; then
  psql "$DATABASE_URL" -f database/postgres/003_seed_demo.sql
fi

if [[ "${REBUILD_DAILY_STATS:-0}" == "1" ]]; then
  psql "$DATABASE_URL" -f database/postgres/004_rebuild_daily_stats.sql
fi

echo "PostgreSQL initialization complete."
