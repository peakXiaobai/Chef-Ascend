import type { RedisClient } from "../../infrastructure/redis.js";
import type { CatalogDishItem, CatalogListQuery, CatalogListResponse } from "../../types/catalog.js";
import type { DishDetailResponse, DishIngredientItem } from "../../types/dish-detail.js";
import { DishesRepository } from "./repository.js";

const TODAY_COUNTER_TTL_DAYS = 3;

const buildTodayCounterKey = (dishId: number, now = new Date()): string => {
  const year = now.getUTCFullYear();
  const month = String(now.getUTCMonth() + 1).padStart(2, "0");
  const day = String(now.getUTCDate()).padStart(2, "0");
  return `chef:today:cook_count:${year}${month}${day}:${dishId}`;
};

const normalizeIngredients = (ingredientsValue: unknown): DishIngredientItem[] => {
  if (!Array.isArray(ingredientsValue)) {
    return [];
  }

  return ingredientsValue.flatMap((item) => {
    if (typeof item !== "object" || item === null) {
      return [];
    }

    const record = item as Record<string, unknown>;
    const name = record.name;
    const amount = record.amount;
    if (typeof name !== "string" || typeof amount !== "string") {
      return [];
    }

    return [{ name, amount }];
  });
};

export class DishesService {
  constructor(
    private readonly repository: DishesRepository,
    private readonly redis: RedisClient | null,
    private readonly logger: { warn: (message: string, error?: unknown) => void }
  ) {}

  async listCatalog(query: CatalogListQuery): Promise<CatalogListResponse> {
    const result = await this.repository.listForCatalog(query);
    const baseItems = result.rows.map((row) => ({
      id: row.id,
      name: row.name,
      difficulty: row.difficulty,
      estimated_total_seconds: row.estimated_total_seconds,
      cover_image_url: row.cover_image_url,
      today_cook_count: row.db_today_count
    }));

    const items = await this.attachRedisTodayCounts(baseItems);

    return {
      page: query.page,
      page_size: query.pageSize,
      total: result.total,
      items
    };
  }

  async getDishDetail(dishId: number): Promise<DishDetailResponse | null> {
    const result = await this.repository.findActiveDetailById(dishId);
    if (!result) {
      return null;
    }

    const todayCount = await this.getTodayCount(result.dish.id, result.dish.db_today_count);
    const ingredients = normalizeIngredients(result.dish.ingredients_json);

    return {
      id: result.dish.id,
      name: result.dish.name,
      description: result.dish.description,
      difficulty: result.dish.difficulty,
      estimated_total_seconds: result.dish.estimated_total_seconds,
      cover_image_url: result.dish.cover_image_url,
      today_cook_count: todayCount,
      ingredients,
      steps: result.steps.map((step) => ({
        step_no: step.step_no,
        title: step.title,
        instruction: step.instruction,
        timer_seconds: step.timer_seconds,
        remind_mode: step.remind_mode
      }))
    };
  }

  private async attachRedisTodayCounts(items: CatalogDishItem[]): Promise<CatalogDishItem[]> {
    if (!this.redis || items.length === 0) {
      return items;
    }

    const keys = items.map((item) => buildTodayCounterKey(item.id));

    try {
      const values = await this.redis.mGet(keys);
      return items.map((item, index) => {
        const value = values[index];
        if (value === null) {
          return item;
        }

        const parsed = Number(value);
        if (!Number.isFinite(parsed) || parsed < 0) {
          return item;
        }

        return {
          ...item,
          today_cook_count: parsed
        };
      });
    } catch (error) {
      this.logger.warn("Failed to read today counts from Redis. Falling back to PostgreSQL.", error);
      return items;
    }
  }

  private async getTodayCount(dishId: number, fallbackValue: number): Promise<number> {
    if (!this.redis) {
      return fallbackValue;
    }

    try {
      const value = await this.redis.get(buildTodayCounterKey(dishId));
      if (value === null) {
        return fallbackValue;
      }

      const parsed = Number(value);
      if (!Number.isFinite(parsed) || parsed < 0) {
        return fallbackValue;
      }

      return parsed;
    } catch (error) {
      this.logger.warn("Failed to read today count from Redis. Falling back to PostgreSQL.", error);
      return fallbackValue;
    }
  }
}

export const TODAY_COUNT_CACHE_DAYS = TODAY_COUNTER_TTL_DAYS;
