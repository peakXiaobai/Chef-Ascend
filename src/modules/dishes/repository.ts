import type { Pool } from "pg";

import type { CatalogDishRow, CatalogListQuery, DishSort } from "../../types/catalog.js";
import type { DishDetailRow, DishDetailStepRow } from "../../types/dish-detail.js";

interface CatalogRowsResult {
  rows: CatalogDishRow[];
  total: number;
}

interface DishDetailRowsResult {
  dish: DishDetailRow;
  steps: DishDetailStepRow[];
}

const ORDER_BY: Record<DishSort, string> = {
  popular_today: "COALESCE(v.today_total_count, 0) DESC, d.created_at DESC",
  latest: "d.created_at DESC",
  duration_asc: "d.estimated_total_seconds ASC, d.created_at DESC",
  duration_desc: "d.estimated_total_seconds DESC, d.created_at DESC"
};

const buildWhereClause = (query: CatalogListQuery) => {
  const conditions: string[] = ["d.is_active = TRUE"];
  const params: Array<number> = [];

  if (query.difficulty !== undefined) {
    params.push(query.difficulty);
    conditions.push(`d.difficulty = $${params.length}`);
  }

  if (query.categoryId !== undefined) {
    params.push(query.categoryId);
    conditions.push(
      `EXISTS (
        SELECT 1
        FROM dish_category_links dcl
        WHERE dcl.dish_id = d.id
          AND dcl.category_id = $${params.length}
      )`
    );
  }

  return {
    whereSql: conditions.join(" AND "),
    params
  };
};

export class DishesRepository {
  constructor(private readonly pool: Pool) {}

  async listForCatalog(query: CatalogListQuery): Promise<CatalogRowsResult> {
    const { whereSql, params } = buildWhereClause(query);
    const orderSql = ORDER_BY[query.sort];

    const countResult = await this.pool.query<{ total: string }>(
      `
      SELECT COUNT(*)::text AS total
      FROM dishes d
      WHERE ${whereSql};
      `,
      params
    );

    const pageParams = [...params];
    pageParams.push(query.pageSize);
    const limitIndex = pageParams.length;

    pageParams.push((query.page - 1) * query.pageSize);
    const offsetIndex = pageParams.length;

    const dataResult = await this.pool.query<CatalogDishRow>(
      `
      SELECT
        d.id,
        d.name,
        d.difficulty,
        d.estimated_total_seconds,
        d.cover_image_url,
        COALESCE(v.today_total_count, 0)::int AS db_today_count
      FROM dishes d
      LEFT JOIN v_dish_today_counts v ON v.dish_id = d.id
      WHERE ${whereSql}
      ORDER BY ${orderSql}, d.id ASC
      LIMIT $${limitIndex}
      OFFSET $${offsetIndex};
      `,
      pageParams
    );

    return {
      rows: dataResult.rows,
      total: Number(countResult.rows[0]?.total ?? 0)
    };
  }

  async findActiveDetailById(dishId: number): Promise<DishDetailRowsResult | null> {
    const dishResult = await this.pool.query<DishDetailRow>(
      `
      SELECT
        d.id,
        d.name,
        d.description,
        d.difficulty,
        d.estimated_total_seconds,
        d.cover_image_url,
        d.ingredients_json,
        COALESCE(v.today_total_count, 0)::int AS db_today_count
      FROM dishes d
      LEFT JOIN v_dish_today_counts v ON v.dish_id = d.id
      WHERE d.id = $1
        AND d.is_active = TRUE
      LIMIT 1;
      `,
      [dishId]
    );

    const dish = dishResult.rows[0];
    if (!dish) {
      return null;
    }

    const stepsResult = await this.pool.query<DishDetailStepRow>(
      `
      SELECT
        step_no,
        title,
        instruction,
        timer_seconds,
        remind_mode
      FROM dish_steps
      WHERE dish_id = $1
      ORDER BY step_no ASC;
      `,
      [dishId]
    );

    return {
      dish,
      steps: stepsResult.rows
    };
  }
}
