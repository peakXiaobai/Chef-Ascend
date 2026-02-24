import type { FastifyInstance, FastifyReply } from "fastify";
import { z } from "zod";

import { DISH_SORT_VALUES } from "../../types/catalog.js";
import type { DishesService } from "./service.js";

const listDishesQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  page_size: z.coerce.number().int().min(1).max(100).default(20),
  category_id: z.coerce.number().int().positive().optional(),
  difficulty: z.coerce.number().int().min(1).max(5).optional(),
  sort: z.enum(DISH_SORT_VALUES).default("popular_today")
});

const dishIdParamsSchema = z.object({
  dish_id: z.coerce.number().int().positive()
});

const buildValidationIssues = (issues: z.ZodIssue[]): Array<{ path: string; message: string }> => {
  return issues.map((issue) => ({
    path: issue.path.join("."),
    message: issue.message
  }));
};

const sendValidationError = (reply: FastifyReply, issues: Array<{ path: string; message: string }>) => {
  return reply.code(400).send({
    message: "Invalid request parameters",
    issues
  });
};

export const registerDishRoutes = async (
  app: FastifyInstance,
  service: DishesService
): Promise<void> => {
  app.get("/api/v1/dishes", async (request, reply) => {
    const parsed = listDishesQuerySchema.safeParse(request.query);

    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    const query = parsed.data;
    const result = await service.listCatalog({
      page: query.page,
      pageSize: query.page_size,
      categoryId: query.category_id,
      difficulty: query.difficulty,
      sort: query.sort
    });

    return reply.code(200).send(result);
  });

  app.get("/api/v1/dishes/:dish_id", async (request, reply) => {
    const parsed = dishIdParamsSchema.safeParse(request.params);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    const detail = await service.getDishDetail(parsed.data.dish_id);
    if (!detail) {
      return reply.code(404).send({
        message: "Dish not found"
      });
    }

    return reply.code(200).send(detail);
  });
};
