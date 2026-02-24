# Module 01 - Catalog

## Goal

Provide a fast and filterable dish list for users to select what to cook.

## Functional Scope

- List dishes with pagination
- Filter by category and difficulty
- Sort by popularity (today count), latest, or duration
- Return today count per dish

## API (planned)

- `GET /api/v1/dishes`

Query params:
- `page` (default 1)
- `page_size` (default 20)
- `category_id` (optional)
- `difficulty` (optional 1..5)
- `sort` (`popular_today|latest|duration_asc|duration_desc`)

## Data Dependencies

- PostgreSQL: `dishes`, `dish_category_links`, `dish_categories`
- Redis: `chef:today:cook_count:{yyyyMMdd}:{dishId}`

## Acceptance Checklist

- Returns only active dishes
- Stable pagination and sort
- Includes fallback today count when Redis key is missing
- Response latency target <= 300ms (single page)

## Implementation Status

- Status: Implemented (API v1)
- Route: `GET /api/v1/dishes`
- Main files:
  - `src/modules/dishes/routes.ts`
  - `src/modules/dishes/service.ts`
  - `src/modules/dishes/repository.ts`
