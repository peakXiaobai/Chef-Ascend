import { Pool } from "pg";

export const createPostgresPool = (databaseUrl: string): Pool => {
  return new Pool({
    connectionString: databaseUrl,
    max: 20,
    idleTimeoutMillis: 30_000,
    connectionTimeoutMillis: 5_000
  });
};
