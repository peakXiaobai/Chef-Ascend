# Module 02 - Dish Detail

## Goal

Show full cooking context of one dish before the user starts cook mode.

## Functional Scope

- Dish metadata (name, description, difficulty, estimated time)
- Ingredient list (extensible section; initially kept in JSON text field or future table)
- Ordered step list with timer settings
- Today cook count display

## API (planned)

- `GET /api/v1/dishes/{dish_id}`

## Data Dependencies

- PostgreSQL: `dishes`, `dish_steps`
- Redis: today count key

## Acceptance Checklist

- Step order is deterministic by `step_no`
- Timer value present for each step (`0` allowed)
- Includes today count in response
- Returns 404 for inactive or missing dish

## Implementation Status

- Status: Implemented (API v1)
- Route: `GET /api/v1/dishes/{dish_id}`
- Main files:
  - `src/modules/dishes/routes.ts`
  - `src/modules/dishes/service.ts`
  - `src/modules/dishes/repository.ts`
