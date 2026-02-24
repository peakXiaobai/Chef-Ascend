export const DISH_SORT_VALUES = [
  "popular_today",
  "latest",
  "duration_asc",
  "duration_desc"
] as const;

export type DishSort = (typeof DISH_SORT_VALUES)[number];

export interface CatalogListQuery {
  page: number;
  pageSize: number;
  categoryId?: number;
  difficulty?: number;
  sort: DishSort;
}

export interface CatalogDishRow {
  id: number;
  name: string;
  difficulty: number;
  estimated_total_seconds: number;
  cover_image_url: string | null;
  db_today_count: number;
}

export interface CatalogDishItem {
  id: number;
  name: string;
  difficulty: number;
  estimated_total_seconds: number;
  cover_image_url: string | null;
  today_cook_count: number;
}

export interface CatalogListResponse {
  page: number;
  page_size: number;
  total: number;
  items: CatalogDishItem[];
}
