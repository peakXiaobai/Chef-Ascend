import Fastify from "fastify";

import { env } from "./config/env.js";
import { createPostgresPool } from "./infrastructure/postgres.js";
import { createRedisConnection } from "./infrastructure/redis.js";
import type { RedisClient } from "./infrastructure/redis.js";
import { DishesRepository } from "./modules/dishes/repository.js";
import { registerDishRoutes } from "./modules/dishes/routes.js";
import { DishesService } from "./modules/dishes/service.js";

const app = Fastify({
  logger: {
    level: env.NODE_ENV === "development" ? "info" : "warn"
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

  const dishesRepository = new DishesRepository(pool);
  const dishesService = new DishesService(dishesRepository, redisConnection, logger);
  await registerDishRoutes(app, dishesService);

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
