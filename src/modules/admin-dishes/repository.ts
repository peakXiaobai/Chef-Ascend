import type { Pool, PoolClient } from "pg";

export type DishRemindMode = "NONE" | "SOUND" | "VIBRATION" | "BOTH";

export interface AdminDishCategory {
  id: number;
  name: string;
  sort_order: number;
}

export interface AdminDishListItem {
  id: number;
  name: string;
  slug: string;
  difficulty: number;
  estimated_total_seconds: number;
  is_active: boolean;
  updated_at: Date;
  step_count: number;
  today_cook_count: number;
  categories: string[];
}

export interface AdminDishIngredient {
  name: string;
  amount: string;
}

export interface AdminDishStep {
  step_no: number;
  title: string;
  instruction: string;
  timer_seconds: number;
  remind_mode: DishRemindMode;
}

export interface AdminDishDetail {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  difficulty: number;
  estimated_total_seconds: number;
  cover_image_url: string | null;
  ingredients_json: unknown;
  is_active: boolean;
  updated_at: Date;
  category_ids: number[];
  steps: AdminDishStep[];
}

export interface UpsertDishInput {
  name: string;
  slug: string;
  description: string | null;
  difficulty: number;
  estimatedTotalSeconds: number;
  coverImageUrl: string | null;
  ingredients: AdminDishIngredient[];
  categoryIds: number[];
  steps: Array<{
    title: string;
    instruction: string;
    timerSeconds: number;
    remindMode: DishRemindMode;
  }>;
  isActive: boolean;
}

interface AdminDishListRow {
  id: number;
  name: string;
  slug: string;
  difficulty: number;
  estimated_total_seconds: number;
  is_active: boolean;
  updated_at: Date;
  step_count: number;
  today_cook_count: number;
  categories: string[] | null;
}

interface AdminDishDetailRow {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  difficulty: number;
  estimated_total_seconds: number;
  cover_image_url: string | null;
  ingredients_json: unknown;
  is_active: boolean;
  updated_at: Date;
}

interface CategoryIdRow {
  category_id: number;
}

export class AdminDishesRepository {
  constructor(private readonly pool: Pool) {}

  async listCategories(): Promise<AdminDishCategory[]> {
    const result = await this.pool.query<AdminDishCategory>(
      `
      SELECT id, name, sort_order
      FROM dish_categories
      ORDER BY sort_order ASC, id ASC;
      `
    );

    return result.rows;
  }

  async findExistingCategoryIds(categoryIds: number[]): Promise<number[]> {
    if (categoryIds.length === 0) {
      return [];
    }

    const result = await this.pool.query<{ id: number }>(
      `
      SELECT id
      FROM dish_categories
      WHERE id = ANY($1::bigint[])
      ORDER BY id ASC;
      `,
      [categoryIds]
    );

    return result.rows.map((row) => row.id);
  }

  async listDishes(query: { keyword?: string; includeInactive: boolean }): Promise<AdminDishListItem[]> {
    const conditions: string[] = [];
    const params: unknown[] = [];

    if (!query.includeInactive) {
      conditions.push("d.is_active = TRUE");
    }

    if (query.keyword) {
      params.push(`%${query.keyword}%`);
      const index = params.length;
      conditions.push(`(d.name ILIKE $${index} OR d.slug ILIKE $${index})`);
    }

    const whereSql = conditions.length > 0 ? `WHERE ${conditions.join(" AND ")}` : "";

    const result = await this.pool.query<AdminDishListRow>(
      `
      SELECT
        d.id,
        d.name,
        d.slug,
        d.difficulty,
        d.estimated_total_seconds,
        d.is_active,
        d.updated_at,
        COALESCE(v.today_total_count, 0)::int AS today_cook_count,
        COALESCE(step_stats.step_count, 0)::int AS step_count,
        COALESCE(category_stats.categories, '{}'::text[]) AS categories
      FROM dishes d
      LEFT JOIN v_dish_today_counts v ON v.dish_id = d.id
      LEFT JOIN LATERAL (
        SELECT COUNT(*)::int AS step_count
        FROM dish_steps ds
        WHERE ds.dish_id = d.id
      ) step_stats ON TRUE
      LEFT JOIN LATERAL (
        SELECT ARRAY_AGG(c.name ORDER BY c.sort_order ASC, c.id ASC) AS categories
        FROM dish_category_links dcl
        JOIN dish_categories c ON c.id = dcl.category_id
        WHERE dcl.dish_id = d.id
      ) category_stats ON TRUE
      ${whereSql}
      ORDER BY d.updated_at DESC, d.id DESC;
      `,
      params
    );

    return result.rows.map((row) => ({
      id: row.id,
      name: row.name,
      slug: row.slug,
      difficulty: row.difficulty,
      estimated_total_seconds: row.estimated_total_seconds,
      is_active: row.is_active,
      updated_at: row.updated_at,
      step_count: row.step_count,
      today_cook_count: row.today_cook_count,
      categories: row.categories ?? []
    }));
  }

  async findDishDetailById(dishId: number): Promise<AdminDishDetail | null> {
    const dishResult = await this.pool.query<AdminDishDetailRow>(
      `
      SELECT
        id,
        name,
        slug,
        description,
        difficulty,
        estimated_total_seconds,
        cover_image_url,
        ingredients_json,
        is_active,
        updated_at
      FROM dishes
      WHERE id = $1
      LIMIT 1;
      `,
      [dishId]
    );

    const dish = dishResult.rows[0];
    if (!dish) {
      return null;
    }

    const categoryResult = await this.pool.query<CategoryIdRow>(
      `
      SELECT category_id
      FROM dish_category_links
      WHERE dish_id = $1
      ORDER BY category_id ASC;
      `,
      [dishId]
    );

    const stepResult = await this.pool.query<AdminDishStep>(
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
      ...dish,
      category_ids: categoryResult.rows.map((row) => row.category_id),
      steps: stepResult.rows
    };
  }

  async createDish(input: UpsertDishInput): Promise<number> {
    return this.withTransaction(async (client) => {
      const dishId = await this.insertDish(client, input);
      await this.replaceCategoryLinks(client, dishId, input.categoryIds);
      await this.upsertSteps(client, dishId, input.steps);
      return dishId;
    });
  }

  async updateDish(dishId: number, input: UpsertDishInput): Promise<boolean> {
    return this.withTransaction(async (client) => {
      const updated = await this.updateDishBase(client, dishId, input);
      if (!updated) {
        return false;
      }

      await this.replaceCategoryLinks(client, dishId, input.categoryIds);
      await this.upsertSteps(client, dishId, input.steps);
      return true;
    });
  }

  async setDishActive(dishId: number, isActive: boolean): Promise<boolean> {
    const result = await this.pool.query(
      `
      UPDATE dishes
      SET is_active = $2
      WHERE id = $1;
      `,
      [dishId, isActive]
    );

    return (result.rowCount ?? 0) > 0;
  }

  private async withTransaction<T>(handler: (client: PoolClient) => Promise<T>): Promise<T> {
    const client = await this.pool.connect();
    try {
      await client.query("BEGIN");
      const result = await handler(client);
      await client.query("COMMIT");
      return result;
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }

  private async insertDish(client: PoolClient, input: UpsertDishInput): Promise<number> {
    const result = await client.query<{ id: number }>(
      `
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
      VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8)
      RETURNING id;
      `,
      [
        input.name,
        input.slug,
        input.description,
        input.difficulty,
        input.estimatedTotalSeconds,
        input.coverImageUrl,
        JSON.stringify(input.ingredients),
        input.isActive
      ]
    );

    return result.rows[0].id;
  }

  private async updateDishBase(
    client: PoolClient,
    dishId: number,
    input: UpsertDishInput
  ): Promise<boolean> {
    const result = await client.query(
      `
      UPDATE dishes
      SET
        name = $2,
        slug = $3,
        description = $4,
        difficulty = $5,
        estimated_total_seconds = $6,
        cover_image_url = $7,
        ingredients_json = $8::jsonb,
        is_active = $9
      WHERE id = $1;
      `,
      [
        dishId,
        input.name,
        input.slug,
        input.description,
        input.difficulty,
        input.estimatedTotalSeconds,
        input.coverImageUrl,
        JSON.stringify(input.ingredients),
        input.isActive
      ]
    );

    return (result.rowCount ?? 0) > 0;
  }

  private async replaceCategoryLinks(
    client: PoolClient,
    dishId: number,
    categoryIds: number[]
  ): Promise<void> {
    await client.query(
      `
      DELETE FROM dish_category_links
      WHERE dish_id = $1;
      `,
      [dishId]
    );

    if (categoryIds.length === 0) {
      return;
    }

    await client.query(
      `
      INSERT INTO dish_category_links (dish_id, category_id)
      SELECT $1, c.id
      FROM UNNEST($2::bigint[]) AS c(id)
      ON CONFLICT DO NOTHING;
      `,
      [dishId, categoryIds]
    );
  }

  private async upsertSteps(
    client: PoolClient,
    dishId: number,
    steps: UpsertDishInput["steps"]
  ): Promise<void> {
    for (const [index, step] of steps.entries()) {
      const stepNo = index + 1;
      const updateResult = await client.query(
        `
        UPDATE dish_steps
        SET
          title = $3,
          instruction = $4,
          timer_seconds = $5,
          remind_mode = $6
        WHERE dish_id = $1
          AND step_no = $2;
        `,
        [dishId, stepNo, step.title, step.instruction, step.timerSeconds, step.remindMode]
      );

      if ((updateResult.rowCount ?? 0) > 0) {
        continue;
      }

      await client.query(
        `
        INSERT INTO dish_steps (dish_id, step_no, title, instruction, timer_seconds, remind_mode)
        VALUES ($1, $2, $3, $4, $5, $6);
        `,
        [dishId, stepNo, step.title, step.instruction, step.timerSeconds, step.remindMode]
      );
    }
  }
}
