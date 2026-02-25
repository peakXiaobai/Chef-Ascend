# API V1 Contract

Base path: `/api/v1`
Content type: `application/json`

Implementation status:
- Implemented:
  - `GET /dishes`
  - `GET /dishes/{dish_id}`
  - `POST /cook-sessions`
  - `GET /cook-sessions/{session_id}`
  - `POST /cook-sessions/{session_id}/steps/{step_no}/start`
  - `POST /cook-sessions/{session_id}/steps/{step_no}/complete`
  - `POST /cook-sessions/{session_id}/timer/pause`
  - `POST /cook-sessions/{session_id}/timer/resume`
  - `POST /cook-sessions/{session_id}/timer/reset`
  - `POST /cook-sessions/{session_id}/complete`
  - `GET /users/{user_id}/cook-records`
  - `GET /admin/dishes` (web console page)
  - `GET /api/v1/admin/categories`
  - `GET /api/v1/admin/dishes`
  - `GET /api/v1/admin/dishes/{dish_id}`
  - `POST /api/v1/admin/dishes`
  - `PUT /api/v1/admin/dishes/{dish_id}`
  - `PATCH /api/v1/admin/dishes/{dish_id}/active`
  - `GET /api/v1/app/android/latest`
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

## 9) Admin Dish Console

- Page: `GET /admin/dishes`
- API base: `/api/v1/admin`

### Admin List

`GET /api/v1/admin/dishes?keyword=&include_inactive=1`

### Admin Detail

`GET /api/v1/admin/dishes/{dish_id}`

### Admin Create

`POST /api/v1/admin/dishes`

```json
{
  "name": "番茄炒蛋",
  "slug": "tomato-scrambled-eggs",
  "description": "经典家常快手菜",
  "difficulty": 1,
  "estimated_total_seconds": 900,
  "cover_image_url": null,
  "is_active": true,
  "category_ids": [1, 2],
  "ingredients": [
    { "name": "鸡蛋", "amount": "3个" },
    { "name": "番茄", "amount": "2个" }
  ],
  "steps": [
    {
      "title": "准备食材",
      "instruction": "鸡蛋打散，番茄切块备用",
      "timer_seconds": 180,
      "remind_mode": "BOTH"
    }
  ]
}
```

### Admin Update

`PUT /api/v1/admin/dishes/{dish_id}`

### Admin Set Active

`PATCH /api/v1/admin/dishes/{dish_id}/active`

```json
{
  "is_active": false
}
```

## 10) Android Update Metadata

`GET /api/v1/app/android/latest`

### Response 200

```json
{
  "platform": "android",
  "version_code": 3,
  "version_name": "0.3.0",
  "file_name": "ChefAscend-debug.apk",
  "file_size_bytes": 25248116,
  "updated_at": "2026-02-24T12:45:11.000Z",
  "download_url": "http://118.196.100.121:3000/downloads/ChefAscend-debug.apk",
  "release_notes": "支持下载进度显示和下载完成自动弹安装"
}
```
