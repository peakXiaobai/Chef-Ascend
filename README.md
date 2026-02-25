# Chef Ascend

Chef Ascend is a cooking companion focused on step-by-step execution with timer reminders.

Core product goals:
- Dish catalog browsing
- Dish detail view
- Cook mode with per-step timers/reminders
- Completion record tracking
- Today cook count display

## Delivery Progress

### Spec & Data Layer

- [x] Module 1: Catalog spec + schema
- [x] Module 2: Dish detail spec + schema
- [x] Module 3: Cook mode spec + schema
- [x] Module 4: Completion record spec + schema
- [x] Module 5: Today count spec + schema + Redis Lua

### API Implementation

- [x] Module 1: Catalog API (`GET /api/v1/dishes`)
- [x] Module 2: Dish detail API (`GET /api/v1/dishes/:dish_id`)
- [x] Module 3: Cook mode API (`/api/v1/cook-sessions/*`)
- [x] Module 4: Completion record API (`/api/v1/cook-sessions/:id/complete`, `/api/v1/users/:id/cook-records`)
- [x] Admin dish management API + web UI (`/admin/dishes`, `/api/v1/admin/*`)
- [ ] Module 5: Dedicated today count endpoint (optional)

### Android App Implementation

- [x] App scaffold (`Jetpack Compose`)
- [x] Catalog page
- [x] Dish detail page
- [x] Cook mode page (step/timer actions)
- [x] Completion submit + records page
- [x] Settings page with Chinese/English language switch (default Chinese)

## Runtime Architecture

- API service (TypeScript + Fastify)
- PostgreSQL as source of truth
- Redis for today counters and session runtime cache

## Repository Structure

```text
Chef-Ascend/
├── README.md
├── package.json
├── tsconfig.json
├── .env.example
├── apps/
│   └── android-app/                # native Android client (Jetpack Compose)
├── docs/
│   ├── ARCHITECTURE.md              # system architecture and request flow
│   ├── COLLABORATION.md             # conventions for multi-model collaboration
│   ├── DELIVERY_LOG.md              # phase-by-phase delivery report
│   ├── mobile/
│   │   └── ANDROID_APP.md           # android app architecture and setup
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
│   │   ├── 005_seed_feature_data.sql # module-level test data
│   │   └── README.md                # db init/run guide
│   └── redis/
│       ├── 001_key_design.md        # redis key conventions
│       └── scripts/
│           └── incr_today_count.lua # atomic increment + expire script
├── src/
│   ├── config/
│   │   └── env.ts                   # env parsing and validation
│   ├── infrastructure/
│   │   ├── postgres.ts              # pg connection pool
│   │   └── redis.ts                 # redis connection helper
│   ├── modules/
│   │   ├── cook-records/
│   │   │   ├── repository.ts        # completion record SQL logic
│   │   │   ├── routes.ts            # completion and history routes
│   │   │   └── service.ts           # idempotent completion + redis counter sync
│   │   ├── cook-sessions/
│   │   │   ├── repository.ts        # cook mode session SQL logic
│   │   │   ├── routes.ts            # /api/v1/cook-sessions routes
│   │   │   └── service.ts           # session state machine + redis timer state
│   │   ├── admin-dishes/
│   │   │   ├── repository.ts        # dish/category/step admin SQL logic
│   │   │   ├── routes.ts            # /admin/dishes + /api/v1/admin routes
│   │   │   ├── service.ts           # admin payload validation + conflict handling
│   │   │   └── ui.ts                # embedded web console page
│   │   └── dishes/
│   │       ├── repository.ts        # catalog SQL query logic
│   │       ├── routes.ts            # /api/v1/dishes route
│   │       └── service.ts           # redis merge + response shaping
│   ├── types/
│   │   ├── catalog.ts               # catalog domain types
│   │   ├── cook-record.ts           # completion record domain types
│   │   ├── cook-session.ts          # cook session domain types
│   │   └── dish-detail.ts           # detail domain types
│   └── server.ts                    # app bootstrap
└── scripts/
    ├── init_postgres.sh             # run postgres scripts in order
    └── init_redis.sh                # load/test redis lua script
```

## Local Setup

1) Install dependencies:

```bash
npm install
```

2) Copy env file:

```bash
cp .env.example .env
```

3) Initialize PostgreSQL and Redis (after setting env):

```bash
bash scripts/init_postgres.sh
bash scripts/init_redis.sh
```

4) Start API:

```bash
npm run dev
```

## Database Initialization (Server)

When deployed to your server, run:

```bash
bash scripts/init_postgres.sh
bash scripts/init_redis.sh
```

Required env vars:
- `DATABASE_URL` (for PostgreSQL)
- `REDIS_URL` (for Redis)
- `LOG_LEVEL` (recommended `info` in production)
- `ANDROID_APK_FILENAME` (default `ChefAscend-debug.apk`)
- `ANDROID_APK_VERSION_CODE` (android update check version code)
- `ANDROID_APK_VERSION_NAME` (android update check version name)
- `ANDROID_APK_RELEASE_NOTES` (optional release notes shown in app)

## Dish Admin Console

- Open in browser: `http://localhost:3000/admin/dishes`
- Core capabilities:
  - dish list and DB-like visual table
  - create/edit dish base info
  - category linking
  - ingredient and step management
  - active/inactive switch
- Current security model: no auth layer yet; deploy behind trusted network or reverse-proxy auth before production internet exposure.

## Implemented Endpoints

Module 1:

```bash
curl "http://localhost:3000/api/v1/dishes?page=1&page_size=20&sort=popular_today"
```

Module 2:

```bash
curl "http://localhost:3000/api/v1/dishes/101"
```

Module 3:

```bash
curl -X POST "http://localhost:3000/api/v1/cook-sessions" \
  -H "content-type: application/json" \
  -d '{"dish_id":101,"user_id":1}'
```

Module 4:

```bash
curl -X POST "http://localhost:3000/api/v1/cook-sessions/1/complete" \
  -H "content-type: application/json" \
  -d '{"user_id":1,"result":"SUCCESS","rating":5}'
```

```bash
curl "http://localhost:3000/api/v1/users/1/cook-records?page=1&page_size=20"
```

Android update metadata:

```bash
curl "http://localhost:3000/api/v1/app/android/latest"
```

## Next Build Order

1. Module 5: optional dedicated today-count endpoint
2. Add auth, monitoring, and deployment pipeline
3. Add Android app login, offline cache, and background timer reminder
