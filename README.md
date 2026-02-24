# Chef Ascend

Chef Ascend is a cooking companion product focused on guided execution, not just recipe reading.

Primary product goals:
- Dish catalog browsing
- Dish detail with structured steps
- Cook mode with per-step timer and reminder
- Completion record tracking
- Today cook count display

## Current Delivery Scope (Phase 1)

This repository currently delivers:
- Product and engineering documentation for the 5 core modules
- Database schema and seed scripts (PostgreSQL)
- Redis key planning and atomic counter script
- Initialization scripts for later server deployment

## Core Modules

- [x] Module 1: Catalog (list/filter/sort)
- [x] Module 2: Dish Detail (ingredients/steps/time)
- [x] Module 3: Cook Mode (session, step timer, reminder state)
- [x] Module 4: Completion Record (success/fail, rating, note)
- [x] Module 5: Today Count Display (Redis fast path + DB fallback)

> Note: In this phase, the modules are completed at the architecture/data/API contract level. Business code implementation can now proceed module by module.

## Planned Runtime Architecture

- App service: API + timer orchestration
- PostgreSQL: source of truth (dishes, steps, sessions, records, daily stats)
- Redis: low-latency counters and temporary cook mode state

## Repository Structure

```text
Chef-Ascend/
├── README.md
├── docs/
│   ├── ARCHITECTURE.md              # system architecture and request flow
│   ├── COLLABORATION.md             # conventions for multi-model collaboration
│   ├── DELIVERY_LOG.md              # phase-by-phase delivery report
│   ├── api/
│   │   └── API_V1.md                # HTTP contract for 5 core modules
│   ├── data/
│   │   ├── DB_SCHEMA.md             # table-by-table schema explanation
│   │   └── REDIS_SCHEMA.md          # key design and consistency strategy
│   └── modules/
│       ├── 01-catalog.md            # module spec: catalog
│       ├── 02-dish-detail.md        # module spec: dish detail
│       ├── 03-cook-mode.md          # module spec: cook mode
│       ├── 04-cook-record.md        # module spec: completion record
│       └── 05-today-count.md        # module spec: today count display
├── database/
│   ├── postgres/
│   │   ├── 001_schema.sql           # tables, constraints, triggers
│   │   ├── 002_views_and_indexes.sql# views and indexes
│   │   ├── 003_seed_demo.sql        # optional seed data
│   │   ├── 004_rebuild_daily_stats.sql # rebuild daily stats from records
│   │   └── README.md                # db init/run guide
│   └── redis/
│       ├── 001_key_design.md        # redis key conventions
│       └── scripts/
│           └── incr_today_count.lua # atomic increment + expire script
└── scripts/
    ├── init_postgres.sh             # run postgres scripts in order
    └── init_redis.sh                # load/test redis lua script
```

## Database Initialization (Later on Server)

When deployed to your server, run:

```bash
bash scripts/init_postgres.sh
bash scripts/init_redis.sh
```

Required env vars:
- `DATABASE_URL` (for PostgreSQL)
- `REDIS_URL` (for Redis)

## Recommended Build Order (After This Phase)

1. Implement Module 1 + Module 2 read APIs
2. Implement Module 3 session/timer state machine
3. Implement Module 4 completion write path
4. Wire Module 5 today count cache update and fallback query
5. Add auth, monitoring, and deployment pipeline
