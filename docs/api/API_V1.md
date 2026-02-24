# API V1 Contract

Base path: `/api/v1`
Content type: `application/json`

Implementation status:
- Implemented: `GET /dishes`, `GET /dishes/{dish_id}`
- Planned: other endpoints in this document

## 1) List Dishes

`GET /dishes`

### Query
- `page` integer, default `1`
- `page_size` integer, default `20`, max `100`
- `category_id` integer, optional
- `difficulty` integer `1..5`, optional
- `sort` enum: `popular_today|latest|duration_asc|duration_desc`

### Response 200

```json
{
  "page": 1,
  "page_size": 20,
  "total": 120,
  "items": [
    {
      "id": 101,
      "name": "Tomato Scrambled Eggs",
      "difficulty": 1,
      "estimated_total_seconds": 900,
      "cover_image_url": "https://...",
      "today_cook_count": 52
    }
  ]
}
```

## 2) Dish Detail

`GET /dishes/{dish_id}`

### Response 200

```json
{
  "id": 101,
  "name": "Tomato Scrambled Eggs",
  "description": "Simple home-style dish",
  "difficulty": 1,
  "estimated_total_seconds": 900,
  "cover_image_url": "https://...",
  "today_cook_count": 52,
  "ingredients": [
    { "name": "egg", "amount": "3" },
    { "name": "tomato", "amount": "2" }
  ],
  "steps": [
    {
      "step_no": 1,
      "title": "Prep",
      "instruction": "Beat eggs and cut tomato",
      "timer_seconds": 180,
      "remind_mode": "BOTH"
    }
  ]
}
```

## 3) Start Cook Session

`POST /cook-sessions`

### Request

```json
{
  "dish_id": 101,
  "user_id": 1001
}
```

### Response 201

```json
{
  "session_id": 90001,
  "dish_id": 101,
  "status": "IN_PROGRESS",
  "current_step_no": 1,
  "started_at": "2026-02-24T09:20:00Z"
}
```

## 4) Get Session State

`GET /cook-sessions/{session_id}`

### Response 200

```json
{
  "session_id": 90001,
  "status": "IN_PROGRESS",
  "current_step_no": 2,
  "timer": {
    "remaining_seconds": 45,
    "is_paused": false
  }
}
```

## 5) Step Start / Complete

- `POST /cook-sessions/{session_id}/steps/{step_no}/start`
- `POST /cook-sessions/{session_id}/steps/{step_no}/complete`

### Common Response 200

```json
{
  "session_id": 90001,
  "current_step_no": 3,
  "status": "IN_PROGRESS"
}
```

## 6) Timer Operations

- `POST /cook-sessions/{session_id}/timer/pause`
- `POST /cook-sessions/{session_id}/timer/resume`
- `POST /cook-sessions/{session_id}/timer/reset`

### Response 200

```json
{
  "session_id": 90001,
  "timer": {
    "remaining_seconds": 120,
    "is_paused": true
  }
}
```

## 7) Complete Session (Record)

`POST /cook-sessions/{session_id}/complete`

### Request

```json
{
  "user_id": 1001,
  "result": "SUCCESS",
  "rating": 5,
  "note": "easy and tasty"
}
```

### Response 200

```json
{
  "session_id": 90001,
  "record_id": 70001,
  "result": "SUCCESS",
  "today_cook_count": 53
}
```

## 8) User Record History

`GET /users/{user_id}/cook-records?page=1&page_size=20`

### Response 200

```json
{
  "page": 1,
  "page_size": 20,
  "total": 18,
  "items": [
    {
      "record_id": 70001,
      "dish_id": 101,
      "dish_name": "Tomato Scrambled Eggs",
      "result": "SUCCESS",
      "rating": 5,
      "cooked_at": "2026-02-24T09:32:00Z"
    }
  ]
}
```

## Error Codes

- `400` invalid request params
- `404` dish/session/user not found
- `409` state conflict (duplicate complete, invalid step order)
- `500` unexpected server error
