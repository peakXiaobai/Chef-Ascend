import type { RedisClient } from "../../infrastructure/redis.js";
import type { CatalogDishItem, CatalogListQuery, CatalogListResponse } from "../../types/catalog.js";
import { DishesRepository } from "./repository.js";

const TODAY_COUNTER_TTL_DAYS = 3;

const buildTodayCounterKey = (dishId: number, now = new Date()): string => {
  const year = now.getUTCFullYear();
  const month = String(now.getUTCMonth() + 1).padStart(2, "0");
  const day = String(now.getUTCDate()).padStart(2, "0");
  return `chef:today:cook_count:${year}${month}${day}:${dishId}`;
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
}

export const TODAY_COUNT_CACHE_DAYS = TODAY_COUNTER_TTL_DAYS;
