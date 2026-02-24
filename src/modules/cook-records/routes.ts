import type { FastifyInstance, FastifyReply } from "fastify";
import { z } from "zod";

import { CookRecordsService, RecordNotFoundError } from "./service.js";

const completeSessionParamsSchema = z.object({
  session_id: z.coerce.number().int().positive()
});

const completeSessionBodySchema = z.object({
  user_id: z.coerce.number().int().positive().optional(),
  result: z.enum(["SUCCESS", "FAILED"]),
  rating: z.coerce.number().int().min(1).max(5).optional(),
  note: z.string().min(1).max(1000).optional()
});

const userIdParamsSchema = z.object({
  user_id: z.coerce.number().int().positive()
});

const listUserRecordsQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  page_size: z.coerce.number().int().min(1).max(100).default(20)
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
  if (error instanceof RecordNotFoundError) {
    return reply.code(404).send({
      message: error.message
    });
  }

  throw error;
};

export const registerCookRecordRoutes = async (
  app: FastifyInstance,
  service: CookRecordsService
): Promise<void> => {
  app.post("/api/v1/cook-sessions/:session_id/complete", async (request, reply) => {
    const paramsParsed = completeSessionParamsSchema.safeParse(request.params);
    if (!paramsParsed.success) {
      return sendValidationError(reply, buildValidationIssues(paramsParsed.error.issues));
    }

    const bodyParsed = completeSessionBodySchema.safeParse(request.body);
    if (!bodyParsed.success) {
      return sendValidationError(reply, buildValidationIssues(bodyParsed.error.issues));
    }

    try {
      const result = await service.completeSession({
        sessionId: paramsParsed.data.session_id,
        userId: bodyParsed.data.user_id,
        result: bodyParsed.data.result,
        rating: bodyParsed.data.rating,
        note: bodyParsed.data.note
      });
      return reply.code(200).send(result);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.get("/api/v1/users/:user_id/cook-records", async (request, reply) => {
    const paramsParsed = userIdParamsSchema.safeParse(request.params);
    if (!paramsParsed.success) {
      return sendValidationError(reply, buildValidationIssues(paramsParsed.error.issues));
    }

    const queryParsed = listUserRecordsQuerySchema.safeParse(request.query);
    if (!queryParsed.success) {
      return sendValidationError(reply, buildValidationIssues(queryParsed.error.issues));
    }

    try {
      const result = await service.listUserRecords(
        paramsParsed.data.user_id,
        queryParsed.data.page,
        queryParsed.data.page_size
      );
      return reply.code(200).send(result);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });
};
