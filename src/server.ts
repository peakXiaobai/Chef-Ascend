import Fastify from "fastify";
import { createReadStream } from "node:fs";
import { access } from "node:fs/promises";
import path from "node:path";

import { env } from "./config/env.js";
import { createPostgresPool } from "./infrastructure/postgres.js";
import { createRedisConnection } from "./infrastructure/redis.js";
import type { RedisClient } from "./infrastructure/redis.js";
import { AdminDishesRepository } from "./modules/admin-dishes/repository.js";
import { registerAdminDishRoutes } from "./modules/admin-dishes/routes.js";
import { AdminDishesService } from "./modules/admin-dishes/service.js";
import { CookRecordsRepository } from "./modules/cook-records/repository.js";
import { registerCookRecordRoutes } from "./modules/cook-records/routes.js";
import { CookRecordsService } from "./modules/cook-records/service.js";
import { CookSessionsRepository } from "./modules/cook-sessions/repository.js";
import { registerCookSessionRoutes } from "./modules/cook-sessions/routes.js";
import { CookSessionsService } from "./modules/cook-sessions/service.js";
import { DishesRepository } from "./modules/dishes/repository.js";
import { registerDishRoutes } from "./modules/dishes/routes.js";
import { DishesService } from "./modules/dishes/service.js";

const logLevel = env.LOG_LEVEL ?? (env.NODE_ENV === "development" ? "debug" : "info");

const app = Fastify({
  logger: {
    level: logLevel
  }
});

const pool = createPostgresPool(env.DATABASE_URL);
let redisConnection: RedisClient | null = null;

const logger = {
  warn: (message: string, error?: unknown) => {
    if (error) {
      app.log.warn({ err: error }, message);
      return;
    }
    app.log.warn(message);
  }
};

const shutdown = async (): Promise<void> => {
  app.log.info("Shutting down server...");
  await app.close();
  if (redisConnection) {
    await redisConnection.quit();
  }
  await pool.end();
};

const bootstrap = async (): Promise<void> => {
  await pool.query("SELECT 1");

  redisConnection = await createRedisConnection(env.REDIS_URL, env.redisEnabled, logger);

  app.get("/healthz", async () => {
    return {
      status: "ok",
      redis: redisConnection ? "connected" : "disabled_or_unavailable"
    };
  });

  app.addHook("onResponse", (request, reply, done) => {
    request.log.info(
      {
        method: request.method,
        url: request.url,
        statusCode: reply.statusCode,
        responseTimeMs: Number(reply.elapsedTime.toFixed(2)),
        remoteAddress: request.ip
      },
      "access"
    );
    done();
  });

  app.get("/downloads/:filename", async (request, reply) => {
    const params = request.params as { filename?: string };
    const filename = params.filename ?? "";
    if (!/^[A-Za-z0-9._-]+$/.test(filename)) {
      return reply.code(400).send({ message: "Invalid filename" });
    }

    const filePath = path.join(process.cwd(), "releases", filename);
    try {
      await access(filePath);
    } catch {
      return reply.code(404).send({ message: "File not found" });
    }

    reply.header("Content-Type", "application/vnd.android.package-archive");
    reply.header("Content-Disposition", `attachment; filename=\"${filename}\"`);
    return reply.send(createReadStream(filePath));
  });

  const dishesRepository = new DishesRepository(pool);
  const dishesService = new DishesService(dishesRepository, redisConnection, logger);
  await registerDishRoutes(app, dishesService);

  const adminDishesRepository = new AdminDishesRepository(pool);
  const adminDishesService = new AdminDishesService(adminDishesRepository);
  await registerAdminDishRoutes(app, adminDishesService);

  const cookSessionsRepository = new CookSessionsRepository(pool);
  const cookSessionsService = new CookSessionsService(cookSessionsRepository, redisConnection, logger);
  await registerCookSessionRoutes(app, cookSessionsService);

  const cookRecordsRepository = new CookRecordsRepository(pool);
  const cookRecordsService = new CookRecordsService(cookRecordsRepository, redisConnection, logger);
  await registerCookRecordRoutes(app, cookRecordsService);

  process.on("SIGINT", async () => {
    await shutdown();
    process.exit(0);
  });

  process.on("SIGTERM", async () => {
    await shutdown();
    process.exit(0);
  });

  await app.listen({ port: env.PORT, host: env.HOST });
  app.log.info(`Chef Ascend API listening on ${env.HOST}:${env.PORT}`);
};

bootstrap().catch(async (error) => {
  app.log.error({ err: error }, "Server startup failed");
  await pool.end();
  process.exit(1);
});
