import { createClient } from "redis";

export type RedisClient = ReturnType<typeof createClient>;

export const createRedisConnection = async (
  redisUrl: string,
  enabled: boolean,
  logger: { warn: (message: string, error?: unknown) => void }
): Promise<RedisClient | null> => {
  if (!enabled) {
    return null;
  }

  const client = createClient({ url: redisUrl });

  client.on("error", (error) => {
    logger.warn("Redis runtime error", error);
  });

  try {
    await client.connect();
    return client;
  } catch (error) {
    logger.warn("Redis disabled due to connection failure", error);
    return null;
  }
};
