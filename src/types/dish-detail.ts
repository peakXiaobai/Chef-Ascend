export type DishRemindMode = "NONE" | "SOUND" | "VIBRATION" | "BOTH";

export interface DishIngredientItem {
  name: string;
  amount: string;
}

export interface DishDetailRow {
  id: number;
  name: string;
  description: string | null;
  difficulty: number;
  estimated_total_seconds: number;
  cover_image_url: string | null;
  ingredients_json: unknown;
  db_today_count: number;
}

export interface DishDetailStepRow {
  step_no: number;
  title: string;
  instruction: string;
  timer_seconds: number;
  remind_mode: DishRemindMode;
}

export interface DishDetailResponse {
  id: number;
  name: string;
  description: string | null;
  difficulty: number;
  estimated_total_seconds: number;
  cover_image_url: string | null;
  today_cook_count: number;
  ingredients: DishIngredientItem[];
  steps: DishDetailStepRow[];
}
