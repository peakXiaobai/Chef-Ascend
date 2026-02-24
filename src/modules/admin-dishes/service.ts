import type { AdminDishIngredient, AdminDishStep, DishRemindMode } from "./repository.js";
import { AdminDishesRepository } from "./repository.js";

const REMIND_MODES: DishRemindMode[] = ["NONE", "SOUND", "VIBRATION", "BOTH"];

interface PgLikeError {
  code?: string;
}

export interface AdminDishUpsertInput {
  name: string;
  slug?: string | null;
  description?: string | null;
  difficulty: number;
  estimated_total_seconds: number;
  cover_image_url?: string | null;
  ingredients?: AdminDishIngredient[];
  category_ids?: number[];
  steps: Array<{
    title: string;
    instruction: string;
    timer_seconds: number;
    remind_mode: DishRemindMode;
  }>;
  is_active?: boolean;
}

export class AdminNotFoundError extends Error {}
export class AdminConflictError extends Error {}

const isPgLikeError = (error: unknown): error is PgLikeError => {
  return typeof error === "object" && error !== null && "code" in error;
};

const normalizeString = (value: string): string => {
  return value.trim();
};

const normalizeOptionalString = (value?: string | null): string | null => {
  if (typeof value !== "string") {
    return null;
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
};

const slugify = (value: string): string => {
  const normalized = value
    .normalize("NFKD")
    .replace(/[^\w\s-]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[\s_-]+/g, "-")
    .replace(/^-+|-+$/g, "");

  if (normalized.length > 0) {
    return normalized;
  }

  return `dish-${Date.now()}`;
};

const parseIngredients = (value: unknown): AdminDishIngredient[] => {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.flatMap((item) => {
    if (typeof item !== "object" || item === null) {
      return [];
    }

    const entry = item as Record<string, unknown>;
    const name = entry.name;
    const amount = entry.amount;
    if (typeof name !== "string" || typeof amount !== "string") {
      return [];
    }

    return [
      {
        name,
        amount
      }
    ];
  });
};

const normalizeIngredients = (value?: AdminDishIngredient[]): AdminDishIngredient[] => {
  if (!value) {
    return [];
  }

  return value
    .map((ingredient) => ({
      name: normalizeString(ingredient.name),
      amount: normalizeString(ingredient.amount)
    }))
    .filter((ingredient) => ingredient.name.length > 0 && ingredient.amount.length > 0);
};

const normalizeSteps = (steps: AdminDishUpsertInput["steps"]): AdminDishStep[] => {
  return steps.map((step, index) => ({
    step_no: index + 1,
    title: normalizeString(step.title),
    instruction: normalizeString(step.instruction),
    timer_seconds: step.timer_seconds,
    remind_mode: step.remind_mode
  }));
};

const normalizeCategoryIds = (value?: number[]): number[] => {
  if (!value || value.length === 0) {
    return [];
  }

  return [...new Set(value)].sort((a, b) => a - b);
};

export class AdminDishesService {
  constructor(private readonly repository: AdminDishesRepository) {}

  async listCategories() {
    return this.repository.listCategories();
  }

  async listDishes(query: { keyword?: string; includeInactive: boolean }) {
    const dishes = await this.repository.listDishes(query);
    return {
      items: dishes.map((dish) => ({
        ...dish,
        updated_at: dish.updated_at.toISOString()
      }))
    };
  }

  async getDishDetail(dishId: number) {
    const detail = await this.repository.findDishDetailById(dishId);
    if (!detail) {
      throw new AdminNotFoundError("菜品不存在");
    }

    return {
      id: detail.id,
      name: detail.name,
      slug: detail.slug,
      description: detail.description,
      difficulty: detail.difficulty,
      estimated_total_seconds: detail.estimated_total_seconds,
      cover_image_url: detail.cover_image_url,
      is_active: detail.is_active,
      category_ids: detail.category_ids,
      ingredients: parseIngredients(detail.ingredients_json),
      steps: detail.steps,
      updated_at: detail.updated_at.toISOString()
    };
  }

  async createDish(input: AdminDishUpsertInput) {
    const normalized = await this.normalizeUpsertInput(input);

    try {
      const dishId = await this.repository.createDish(normalized);
      return this.getDishDetail(dishId);
    } catch (error) {
      this.mapWriteError(error);
    }
  }

  async updateDish(dishId: number, input: AdminDishUpsertInput) {
    const existing = await this.repository.findDishDetailById(dishId);
    if (!existing) {
      throw new AdminNotFoundError("菜品不存在");
    }

    if (input.steps.length < existing.steps.length) {
      throw new AdminConflictError("当前菜品已存在历史步骤，暂不支持减少步骤数量");
    }

    const normalized = await this.normalizeUpsertInput(input, existing.is_active);

    try {
      const updated = await this.repository.updateDish(dishId, normalized);
      if (!updated) {
        throw new AdminNotFoundError("菜品不存在");
      }
      return this.getDishDetail(dishId);
    } catch (error) {
      this.mapWriteError(error);
    }
  }

  async setDishActive(dishId: number, isActive: boolean) {
    const updated = await this.repository.setDishActive(dishId, isActive);
    if (!updated) {
      throw new AdminNotFoundError("菜品不存在");
    }

    return {
      dish_id: dishId,
      is_active: isActive
    };
  }

  private async normalizeUpsertInput(
    input: AdminDishUpsertInput,
    fallbackIsActive = true
  ) {
    const name = normalizeString(input.name);
    if (name.length === 0) {
      throw new AdminConflictError("菜品名称不能为空");
    }

    if (input.steps.length === 0) {
      throw new AdminConflictError("至少需要一个步骤");
    }

    const normalizedSteps = normalizeSteps(input.steps);
    const invalidStep = normalizedSteps.find(
      (step) => step.title.length === 0 || step.instruction.length === 0 || !REMIND_MODES.includes(step.remind_mode)
    );
    if (invalidStep) {
      throw new AdminConflictError("步骤内容不完整或提醒模式非法");
    }

    const categoryIds = normalizeCategoryIds(input.category_ids);
    if (categoryIds.length > 0) {
      const existingCategoryIds = await this.repository.findExistingCategoryIds(categoryIds);
      if (existingCategoryIds.length !== categoryIds.length) {
        throw new AdminConflictError("存在无效的分类 ID");
      }
    }

    const slugSource = normalizeOptionalString(input.slug) ?? name;

    return {
      name,
      slug: slugify(slugSource),
      description: normalizeOptionalString(input.description),
      difficulty: input.difficulty,
      estimatedTotalSeconds: input.estimated_total_seconds,
      coverImageUrl: normalizeOptionalString(input.cover_image_url),
      ingredients: normalizeIngredients(input.ingredients),
      categoryIds,
      steps: normalizedSteps.map((step) => ({
        title: step.title,
        instruction: step.instruction,
        timerSeconds: step.timer_seconds,
        remindMode: step.remind_mode
      })),
      isActive: input.is_active ?? fallbackIsActive
    };
  }

  private mapWriteError(error: unknown): never {
    if (error instanceof AdminConflictError || error instanceof AdminNotFoundError) {
      throw error;
    }

    if (isPgLikeError(error) && error.code === "23505") {
      throw new AdminConflictError("slug 已存在，请修改后重试");
    }

    throw error;
  }
}
