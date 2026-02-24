import type { Pool, PoolClient } from "pg";

import type { CookRecordResult, CompleteCookSessionInput, UserCookRecordItem } from "../../types/cook-record.js";

interface SessionForCompleteRow {
  id: number;
  dish_id: number;
  user_id: number | null;
}

interface CookRecordRow {
  id: number;
  session_id: number;
  dish_id: number;
  result: CookRecordResult;
}

interface CountRow {
  total: string;
}

interface TodayCountRow {
  today_total_count: number;
}

interface UserRecordHistoryRow {
  record_id: number;
  dish_id: number;
  dish_name: string;
  result: CookRecordResult;
  rating: number | null;
  cooked_at: Date;
}

interface CompleteRecordData {
  sessionId: number;
  recordId: number;
  dishId: number;
  result: CookRecordResult;
  isNewRecord: boolean;
}

type CompleteSessionStatus = "ok" | "session_not_found";
type HistoryStatus = "ok" | "user_not_found";

export interface CompleteSessionAttempt {
  status: CompleteSessionStatus;
  data?: CompleteRecordData;
}

export interface UserRecordHistoryResult {
  status: HistoryStatus;
  total: number;
  items: UserCookRecordItem[];
}

const toUserHistoryItem = (row: UserRecordHistoryRow): UserCookRecordItem => {
  return {
    record_id: row.record_id,
    dish_id: row.dish_id,
    dish_name: row.dish_name,
    result: row.result,
    rating: row.rating,
    cooked_at: row.cooked_at.toISOString()
  };
};

const mapSessionStatusByResult = (result: CookRecordResult): "COMPLETED" | "ABANDONED" => {
  return result === "SUCCESS" ? "COMPLETED" : "ABANDONED";
};

export class CookRecordsRepository {
  constructor(private readonly pool: Pool) {}

  async completeSession(input: CompleteCookSessionInput): Promise<CompleteSessionAttempt> {
    const client = await this.pool.connect();
    try {
      await client.query("BEGIN");

      const session = await this.findSessionForUpdate(client, input.sessionId);
      if (!session) {
        await client.query("ROLLBACK");
        return { status: "session_not_found" };
      }

      const existingRecord = await this.findRecordBySessionId(client, input.sessionId);
      if (existingRecord) {
        await client.query("COMMIT");
        return {
          status: "ok",
          data: {
            sessionId: existingRecord.session_id,
            recordId: existingRecord.id,
            dishId: existingRecord.dish_id,
            result: existingRecord.result,
            isNewRecord: false
          }
        };
      }

      const finalSessionStatus = mapSessionStatusByResult(input.result);
      await client.query(
        `
        UPDATE cook_sessions
        SET
          status = $2,
          finished_at = COALESCE(finished_at, NOW()),
          total_elapsed_seconds = (
            SELECT COALESCE(SUM(css.elapsed_seconds), 0)
            FROM cook_session_steps css
            WHERE css.session_id = $1
              AND css.elapsed_seconds IS NOT NULL
          )
        WHERE id = $1;
        `,
        [input.sessionId, finalSessionStatus]
      );

      const createdRecord = await this.insertRecord(client, input, session);
      await client.query("COMMIT");

      return {
        status: "ok",
        data: {
          sessionId: createdRecord.session_id,
          recordId: createdRecord.id,
          dishId: createdRecord.dish_id,
          result: createdRecord.result,
          isNewRecord: true
        }
      };
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }

  async getTodayCookCountByDishId(dishId: number): Promise<number> {
    const result = await this.pool.query<TodayCountRow>(
      `
      SELECT
        COALESCE(success_count, 0) + COALESCE(failed_count, 0) AS today_total_count
      FROM dish_daily_stats
      WHERE stat_date = CURRENT_DATE
        AND dish_id = $1
      LIMIT 1;
      `,
      [dishId]
    );

    return result.rows[0]?.today_total_count ?? 0;
  }

  async listUserRecords(
    userId: number,
    page: number,
    pageSize: number
  ): Promise<UserRecordHistoryResult> {
    const userExists = await this.checkUserExists(userId);
    if (!userExists) {
      return {
        status: "user_not_found",
        total: 0,
        items: []
      };
    }

    const countResult = await this.pool.query<CountRow>(
      `
      SELECT COUNT(*)::text AS total
      FROM cook_records
      WHERE user_id = $1;
      `,
      [userId]
    );

    const offset = (page - 1) * pageSize;
    const dataResult = await this.pool.query<UserRecordHistoryRow>(
      `
      SELECT
        cr.id AS record_id,
        cr.dish_id,
        d.name AS dish_name,
        cr.result,
        cr.rating,
        cr.cooked_at
      FROM cook_records cr
      JOIN dishes d ON d.id = cr.dish_id
      WHERE cr.user_id = $1
      ORDER BY cr.cooked_at DESC, cr.id DESC
      LIMIT $2
      OFFSET $3;
      `,
      [userId, pageSize, offset]
    );

    return {
      status: "ok",
      total: Number(countResult.rows[0]?.total ?? 0),
      items: dataResult.rows.map(toUserHistoryItem)
    };
  }

  private async checkUserExists(userId: number): Promise<boolean> {
    const result = await this.pool.query<{ id: number }>(
      `
      SELECT id
      FROM users
      WHERE id = $1
      LIMIT 1;
      `,
      [userId]
    );

    return result.rows.length > 0;
  }

  private async findSessionForUpdate(
    client: PoolClient,
    sessionId: number
  ): Promise<SessionForCompleteRow | null> {
    const result = await client.query<SessionForCompleteRow>(
      `
      SELECT id, dish_id, user_id
      FROM cook_sessions
      WHERE id = $1
      FOR UPDATE;
      `,
      [sessionId]
    );

    return result.rows[0] ?? null;
  }

  private async findRecordBySessionId(
    client: PoolClient,
    sessionId: number
  ): Promise<CookRecordRow | null> {
    const result = await client.query<CookRecordRow>(
      `
      SELECT id, session_id, dish_id, result
      FROM cook_records
      WHERE session_id = $1
      LIMIT 1;
      `,
      [sessionId]
    );

    return result.rows[0] ?? null;
  }

  private async insertRecord(
    client: PoolClient,
    input: CompleteCookSessionInput,
    session: SessionForCompleteRow
  ): Promise<CookRecordRow> {
    const result = await client.query<CookRecordRow>(
      `
      INSERT INTO cook_records (
        session_id,
        user_id,
        dish_id,
        result,
        rating,
        note
      )
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING id, session_id, dish_id, result;
      `,
      [
        input.sessionId,
        input.userId ?? session.user_id,
        session.dish_id,
        input.result,
        input.rating ?? null,
        input.note ?? null
      ]
    );

    return result.rows[0];
  }
}
