# PostgreSQL Scripts

Run order:
1. `001_schema.sql`
2. `002_views_and_indexes.sql`
3. `003_seed_demo.sql` (optional base demo)
4. `005_seed_feature_data.sql` (optional module test data)
5. `004_rebuild_daily_stats.sql` (optional reconciliation)

## Manual Execution

```bash
psql "$DATABASE_URL" -f database/postgres/001_schema.sql
psql "$DATABASE_URL" -f database/postgres/002_views_and_indexes.sql
psql "$DATABASE_URL" -f database/postgres/003_seed_demo.sql
psql "$DATABASE_URL" -f database/postgres/005_seed_feature_data.sql
psql "$DATABASE_URL" -f database/postgres/004_rebuild_daily_stats.sql
```

## Notes

- Scripts are idempotent where possible (`IF NOT EXISTS`, `ON CONFLICT`).
- Seed file is safe to rerun.
- Production should use migration files per change rather than modifying existing base scripts.
