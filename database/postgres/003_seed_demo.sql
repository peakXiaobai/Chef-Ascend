BEGIN;

INSERT INTO users (nickname, avatar_url)
VALUES
  ('DemoCookA', NULL),
  ('DemoCookB', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO dish_categories (name, sort_order)
VALUES
  ('Home Style', 10),
  ('Quick Meals', 20),
  ('Beginner Friendly', 30)
ON CONFLICT (name) DO NOTHING;

INSERT INTO dishes (
  name,
  slug,
  description,
  difficulty,
  estimated_total_seconds,
  cover_image_url,
  ingredients_json,
  is_active
)
VALUES
  (
    'Tomato Scrambled Eggs',
    'tomato-scrambled-eggs',
    'Classic quick home-style dish.',
    1,
    900,
    NULL,
    '[{"name":"egg","amount":"3"},{"name":"tomato","amount":"2"},{"name":"salt","amount":"3g"}]'::jsonb,
    TRUE
  ),
  (
    'Stir-fried Cabbage',
    'stir-fried-cabbage',
    'Beginner stir-fry dish with clear step timing.',
    1,
    780,
    NULL,
    '[{"name":"cabbage","amount":"300g"},{"name":"garlic","amount":"2 cloves"},{"name":"soy sauce","amount":"8ml"}]'::jsonb,
    TRUE
  )
ON CONFLICT (slug) DO NOTHING;

INSERT INTO dish_category_links (dish_id, category_id)
SELECT d.id, c.id
FROM dishes d
JOIN dish_categories c ON c.name IN ('Home Style', 'Quick Meals', 'Beginner Friendly')
WHERE d.slug = 'tomato-scrambled-eggs'
ON CONFLICT DO NOTHING;

INSERT INTO dish_category_links (dish_id, category_id)
SELECT d.id, c.id
FROM dishes d
JOIN dish_categories c ON c.name IN ('Home Style', 'Quick Meals')
WHERE d.slug = 'stir-fried-cabbage'
ON CONFLICT DO NOTHING;

INSERT INTO dish_steps (dish_id, step_no, title, instruction, timer_seconds, remind_mode, tips)
SELECT d.id, 1, 'Prepare ingredients', 'Beat eggs, cut tomatoes into wedges.', 180, 'BOTH', 'Keep tomatoes medium chunks.'
FROM dishes d
WHERE d.slug = 'tomato-scrambled-eggs'
ON CONFLICT (dish_id, step_no) DO NOTHING;

INSERT INTO dish_steps (dish_id, step_no, title, instruction, timer_seconds, remind_mode, tips)
SELECT d.id, 2, 'Cook eggs', 'Heat oil, pour eggs, stir until 80 percent done then remove.', 120, 'BOTH', 'Do not overcook eggs.'
FROM dishes d
WHERE d.slug = 'tomato-scrambled-eggs'
ON CONFLICT (dish_id, step_no) DO NOTHING;

INSERT INTO dish_steps (dish_id, step_no, title, instruction, timer_seconds, remind_mode, tips)
SELECT d.id, 3, 'Cook tomato and combine', 'Cook tomato until softened, return eggs, season and mix.', 180, 'BOTH', 'Add a small amount of sugar if tomato is sour.'
FROM dishes d
WHERE d.slug = 'tomato-scrambled-eggs'
ON CONFLICT (dish_id, step_no) DO NOTHING;

INSERT INTO dish_steps (dish_id, step_no, title, instruction, timer_seconds, remind_mode, tips)
SELECT d.id, 1, 'Prep cabbage', 'Wash and cut cabbage into bite-size pieces.', 180, 'BOTH', 'Drain water to avoid splatter.'
FROM dishes d
WHERE d.slug = 'stir-fried-cabbage'
ON CONFLICT (dish_id, step_no) DO NOTHING;

INSERT INTO dish_steps (dish_id, step_no, title, instruction, timer_seconds, remind_mode, tips)
SELECT d.id, 2, 'Stir-fry garlic', 'Heat oil and stir-fry garlic until fragrant.', 30, 'SOUND', 'Low heat to avoid burning garlic.'
FROM dishes d
WHERE d.slug = 'stir-fried-cabbage'
ON CONFLICT (dish_id, step_no) DO NOTHING;

INSERT INTO dish_steps (dish_id, step_no, title, instruction, timer_seconds, remind_mode, tips)
SELECT d.id, 3, 'Stir-fry cabbage', 'Add cabbage and stir-fry on high heat until tender-crisp.', 180, 'BOTH', 'Do not cover the wok.'
FROM dishes d
WHERE d.slug = 'stir-fried-cabbage'
ON CONFLICT (dish_id, step_no) DO NOTHING;

COMMIT;
