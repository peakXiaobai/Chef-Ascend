import { config as loadDotEnv } from "dotenv";
import { z } from "zod";

loadDotEnv();

const envSchema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  PORT: z.coerce.number().int().positive().default(3000),
  HOST: z.string().default("0.0.0.0"),
  LOG_LEVEL: z.enum(["debug", "info", "warn", "error"]).optional(),
  ANDROID_APK_FILENAME: z.string().default("ChefAscend-debug.apk"),
  ANDROID_APK_VERSION_CODE: z.coerce.number().int().positive().default(2),
  ANDROID_APK_VERSION_NAME: z.string().default("0.2.0"),
  ANDROID_APK_RELEASE_NOTES: z.string().optional(),
  DATABASE_URL: z.string().min(1, "DATABASE_URL is required"),
  REDIS_URL: z.string().default("redis://localhost:6379"),
  REDIS_ENABLED: z.enum(["0", "1"]).default("1")
});

const parsed = envSchema.safeParse(process.env);

if (!parsed.success) {
  const issues = parsed.error.issues
    .map((issue) => `${issue.path.join(".")}: ${issue.message}`)
    .join("; ");
  throw new Error(`Invalid environment: ${issues}`);
}

export const env = {
  ...parsed.data,
  redisEnabled: parsed.data.REDIS_ENABLED === "1"
};
