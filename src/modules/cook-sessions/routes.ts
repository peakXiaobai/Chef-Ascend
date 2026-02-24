import type { FastifyInstance, FastifyReply } from "fastify";
import { z } from "zod";

import {
  CookSessionsService,
  EntityNotFoundError,
  SessionConflictError
} from "./service.js";

const createSessionBodySchema = z.object({
  dish_id: z.coerce.number().int().positive(),
  user_id: z.coerce.number().int().positive().optional()
});

const sessionIdParamsSchema = z.object({
  session_id: z.coerce.number().int().positive()
});

const sessionStepParamsSchema = z.object({
  session_id: z.coerce.number().int().positive(),
  step_no: z.coerce.number().int().positive()
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
  if (error instanceof EntityNotFoundError) {
    return reply.code(404).send({ message: error.message });
  }

  if (error instanceof SessionConflictError) {
    return reply.code(409).send({ message: error.message });
  }

  throw error;
};

export const registerCookSessionRoutes = async (
  app: FastifyInstance,
  service: CookSessionsService
): Promise<void> => {
  app.post("/api/v1/cook-sessions", async (request, reply) => {
    const parsed = createSessionBodySchema.safeParse(request.body);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    try {
      const session = await service.startSession({
        dishId: parsed.data.dish_id,
        userId: parsed.data.user_id
      });
      return reply.code(201).send(session);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.get("/api/v1/cook-sessions/:session_id", async (request, reply) => {
    const parsed = sessionIdParamsSchema.safeParse(request.params);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    try {
      const session = await service.getSessionState(parsed.data.session_id);
      return reply.code(200).send(session);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.post("/api/v1/cook-sessions/:session_id/steps/:step_no/start", async (request, reply) => {
    const parsed = sessionStepParamsSchema.safeParse(request.params);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    try {
      const result = await service.startStep(parsed.data.session_id, parsed.data.step_no);
      return reply.code(200).send(result);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.post("/api/v1/cook-sessions/:session_id/steps/:step_no/complete", async (request, reply) => {
    const parsed = sessionStepParamsSchema.safeParse(request.params);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    try {
      const result = await service.completeStep(parsed.data.session_id, parsed.data.step_no);
      return reply.code(200).send(result);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.post("/api/v1/cook-sessions/:session_id/timer/pause", async (request, reply) => {
    const parsed = sessionIdParamsSchema.safeParse(request.params);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    try {
      const result = await service.pauseTimer(parsed.data.session_id);
      return reply.code(200).send(result);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.post("/api/v1/cook-sessions/:session_id/timer/resume", async (request, reply) => {
    const parsed = sessionIdParamsSchema.safeParse(request.params);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    try {
      const result = await service.resumeTimer(parsed.data.session_id);
      return reply.code(200).send(result);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });

  app.post("/api/v1/cook-sessions/:session_id/timer/reset", async (request, reply) => {
    const parsed = sessionIdParamsSchema.safeParse(request.params);
    if (!parsed.success) {
      return sendValidationError(reply, buildValidationIssues(parsed.error.issues));
    }

    try {
      const result = await service.resetTimer(parsed.data.session_id);
      return reply.code(200).send(result);
    } catch (error) {
      return mapErrorToReply(reply, error);
    }
  });
};
