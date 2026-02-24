#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${REDIS_URL:-}" ]]; then
  echo "ERROR: REDIS_URL is not set"
  exit 1
fi

SCRIPT_PATH="database/redis/scripts/incr_today_count.lua"
SCRIPT_BODY="$(cat "$SCRIPT_PATH")"

SHA=$(redis-cli -u "$REDIS_URL" SCRIPT LOAD "$SCRIPT_BODY")

echo "Loaded Lua script SHA: $SHA"
echo "Redis initialization complete."
