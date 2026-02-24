import type { RedisClient } from "../../infrastructure/redis.js";
import type {
  CompleteCookSessionInput,
  CompleteCookSessionResponse,
  UserCookRecordListResponse
} from "../../types/cook-record.js";
import { CookRecordsRepository } from "./repository.js";

const TODAY_COUNT_TTL_SECONDS = 3 * 24 * 60 * 60;
const INCR_TODAY_COUNT_LUA = `
local key = KEYS[1]
local ttl = tonumber(ARGV[1])
local value = redis.call('INCR', key)
if redis.call('TTL', key) < 0 then
  redis.call('EXPIRE', key, ttl)
end
return value
`;

const buildTodayCounterKey = (dishId: number, now = new Date()): string => {
  const year = now.getUTCFullYear();
  const month = String(now.getUTCMonth() + 1).padStart(2, "0");
  const day = String(now.getUTCDate()).padStart(2, "0");
  return `chef:today:cook_count:${year}${month}${day}:${dishId}`;
};

const buildSessionStateKey = (sessionId: number): string => {
  return `chef:session:state:${sessionId}`;
};

const toCountOrNull = (value: unknown): number | null => {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return null;
  }

  return parsed;
};

export class RecordNotFoundError extends Error {}

export class CookRecordsService {
  constructor(
    private readonly repository: CookRecordsRepository,
    private readonly redis: RedisClient | null,
    private readonly logger: { warn: (message: string, error?: unknown) => void }
  ) {}

  async completeSession(input: CompleteCookSessionInput): Promise<CompleteCookSessionResponse> {
    const attempt = await this.repository.completeSession(input);
    if (attempt.status === "session_not_found" || !attempt.data) {
      throw new RecordNotFoundError("Session not found");
    }

    await this.clearSessionState(attempt.data.sessionId);

    const todayCount = attempt.data.isNewRecord
      ? await this.resolveTodayCountAfterNewRecord(attempt.data.dishId)
      : await this.resolveTodayCount(attempt.data.dishId);

    return {
      session_id: attempt.data.sessionId,
      record_id: attempt.data.recordId,
      result: attempt.data.result,
      today_cook_count: todayCount
    };
  }

  async listUserRecords(
    userId: number,
    page: number,
    pageSize: number
  ): Promise<UserCookRecordListResponse> {
    const result = await this.repository.listUserRecords(userId, page, pageSize);
    if (result.status === "user_not_found") {
      throw new RecordNotFoundError("User not found");
    }

    return {
      page,
      page_size: pageSize,
      total: result.total,
      items: result.items
    };
  }

  private async resolveTodayCountAfterNewRecord(dishId: number): Promise<number> {
    const incremented = await this.incrementTodayCount(dishId);
    if (incremented !== null) {
      return incremented;
    }

    return this.repository.getTodayCookCountByDishId(dishId);
  }

  private async resolveTodayCount(dishId: number): Promise<number> {
    const cached = await this.getCachedTodayCount(dishId);
    if (cached !== null) {
      return cached;
    }

    return this.repository.getTodayCookCountByDishId(dishId);
  }

  private async incrementTodayCount(dishId: number): Promise<number | null> {
    if (!this.redis) {
      return null;
    }

    const key = buildTodayCounterKey(dishId);

    try {
      const response = await this.redis.sendCommand([
        "EVAL",
        INCR_TODAY_COUNT_LUA,
        "1",
        key,
        String(TODAY_COUNT_TTL_SECONDS)
      ]);
      return toCountOrNull(response);
    } catch (error) {
      this.logger.warn("Failed to increment Redis today count.", error);
      return null;
    }
  }

  private async getCachedTodayCount(dishId: number): Promise<number | null> {
    if (!this.redis) {
      return null;
    }

    try {
      const value = await this.redis.get(buildTodayCounterKey(dishId));
      if (value === null) {
        return null;
      }

      return toCountOrNull(value);
    } catch (error) {
      this.logger.warn("Failed to read Redis today count.", error);
      return null;
    }
  }

  private async clearSessionState(sessionId: number): Promise<void> {
    if (!this.redis) {
      return;
    }

    try {
      await this.redis.del(buildSessionStateKey(sessionId));
    } catch (error) {
      this.logger.warn("Failed to clear Redis session state.", error);
    }
  }
}
