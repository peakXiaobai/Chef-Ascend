import type { FastifyInstance, FastifyReply } from "fastify";
import { z } from "zod";

import { AdminConflictError, AdminDishesService, AdminNotFoundError } from "./service.js";
import { renderAdminDishPage } from "./ui.js";

const dishIdParamsSchema = z.object({
  dish_id: z.coerce.number().int().positive()
});

const listQuerySchema = z.object({
  keyword: z.string().trim().min(1).max(120).optional(),
  include_inactive: z.coerce.number().int().min(0).max(1).default(1)
});

const remindModeSchema = z.enum(["NONE", "SOUND", "VIBRATION", "BOTH"]);

const ingredientSchema = z.object({
  name: z.string().trim().min(1).max(100),
  amount: z.string().trim().min(1).max(100)
});

const stepSchema = z.object({
  title: z.string().trim().min(1).max(120),
  instruction: z.string().trim().min(1).max(3000),
  timer_seconds: z.coerce.number().int().min(0).max(86_400),
  remind_mode: remindModeSchema
});

const upsertDishBodySchema = z.object({
  name: z.string().trim().min(1).max(120),
  slug: z.string().trim().max(160).optional().nullable(),
  description: z.string().trim().max(4000).optional().nullable(),
  difficulty: z.coerce.number().int().min(1).max(5),
  estimated_total_seconds: z.coerce.number().int().positive().max(86_400),
  cover_image_url: z.string().trim().max(2000).optional().nullable(),
  ingredients: z.array(ingredientSchema).max(120).optional(),
  category_ids: z.array(z.coerce.number().int().positive()).max(60).optional(),
  steps: z.array(stepSchema).min(1).max(120),
  is_active: z.boolean().optional()
});

const setActiveBodySchema = z.object({
  is_active: z.boolean()
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

const mapErrorToReply = (reply: FastifyReply, error: unknown) => {
  if (error instanceof AdminNotFoundError) {
    return reply.code(404).send({ message: error.message });
  }

  if (error instanceof AdminConflictError) {
    return reply.code(409).send({ message: error.message });
  }

  throw error;
};

export const registerAdminDishRoutes = async (
  app: FastifyInstance,
  service: AdminDishesService
): Promise<void> => {
  app.get("/admin", async (_request, reply) => {
    return reply.redirect("/admin/dishes");
  });

  app.get("/admin/dishes", async (_request, reply) => {
    reply.header("content-type", "text/html; charset=utf-8");
    return reply.send(renderAdminDishPage());
  });

  app.get("/api/v1/admin/categories", async () => {
    return service.listCategories();
  });

  app.get("/api/v1/admin/dishes", async (request, reply) => {
    const parsed = listQuerySchema.safeParse(request.query);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    return service.listDishes({
      keyword: parsed.data.keyword,
      includeInactive: parsed.data.include_inactive === 1
    });
  });

  app.get("/api/v1/admin/dishes/:dish_id", async (request, reply) => {
    const parsed = dishIdParamsSchema.safeParse(request.params);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    try {
      return await service.getDishDetail(parsed.data.dish_id);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.post("/api/v1/admin/dishes", async (request, reply) => {
    const parsed = upsertDishBodySchema.safeParse(request.body);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    try {
      const created = await service.createDish(parsed.data);
      return reply.code(201).send(created);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.put("/api/v1/admin/dishes/:dish_id", async (request, reply) => {
    const paramsParsed = dishIdParamsSchema.safeParse(request.params);
    if (!paramsParsed.success) {
      return sendValidationError(reply, buildValidationIssues(paramsParsed.error.issues));
    }

    const bodyParsed = upsertDishBodySchema.safeParse(request.body);
    if (!bodyParsed.success) {
      return sendValidationError(reply, buildValidationIssues(bodyParsed.error.issues));
    }

    try {
      const updated = await service.updateDish(paramsParsed.data.dish_id, bodyParsed.data);
      return reply.code(200).send(updated);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.patch("/api/v1/admin/dishes/:dish_id/active", async (request, reply) => {
    const paramsParsed = dishIdParamsSchema.safeParse(request.params);
    if (!paramsParsed.success) {
      return sendValidationError(reply, buildValidationIssues(paramsParsed.error.issues));
    }

    const bodyParsed = setActiveBodySchema.safeParse(request.body);
    if (!bodyParsed.success) {
      return sendValidationError(reply, buildValidationIssues(bodyParsed.error.issues));
    }

    try {
      const updated = await service.setDishActive(paramsParsed.data.dish_id, bodyParsed.data.is_active);
      return reply.code(200).send(updated);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });
};
