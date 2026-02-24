import type { Pool, PoolClient } from "pg";

import type { CookSessionStatus } from "../../types/cook-session.js";

interface CreateCookSessionRow {
  id: number;
  dish_id: number;
  status: CookSessionStatus;
  current_step_no: number;
  started_at: Date;
}

interface StepSummaryRow {
  step_count: string;
  first_step_no: number | null;
  max_step_no: number | null;
  first_timer_seconds: number;
}

interface SessionRuntimeRow {
  session_id: number;
  dish_id: number;
  status: CookSessionStatus;
  current_step_no: number;
  current_step_timer_seconds: number | null;
  max_step_no: number | null;
}

interface StepTimerRow {
  step_no: number;
  timer_seconds_snapshot: number;
}

export interface CreateCookSessionResult {
  sessionId: number;
  dishId: number;
  status: CookSessionStatus;
  currentStepNo: number;
  startedAt: Date;
  firstStepTimerSeconds: number;
}

export interface CookSessionRuntime {
  sessionId: number;
  dishId: number;
  status: CookSessionStatus;
  currentStepNo: number;
  currentStepTimerSeconds: number;
  maxStepNo: number;
}

export interface SessionStepTimer {
  stepNo: number;
  timerSeconds: number;
}

type CreateSessionStatus = "ok" | "dish_not_found" | "no_steps";

export interface CreateSessionAttempt {
  status: CreateSessionStatus;
  data?: CreateCookSessionResult;
}

const toRuntime = (row: SessionRuntimeRow): CookSessionRuntime => {
  return {
    sessionId: row.session_id,
    dishId: row.dish_id,
    status: row.status,
    currentStepNo: row.current_step_no,
    currentStepTimerSeconds: row.current_step_timer_seconds ?? 0,
    maxStepNo: row.max_step_no ?? row.current_step_no
  };
};

export class CookSessionsRepository {
  constructor(private readonly pool: Pool) {}

  async createSession(dishId: number, userId?: number): Promise<CreateSessionAttempt> {
    const client = await this.pool.connect();
    try {
      await client.query("BEGIN");

      const dishExists = await this.findActiveDish(client, dishId);
      if (!dishExists) {
        await client.query("ROLLBACK");
        return { status: "dish_not_found" };
      }

      const stepSummary = await this.getDishStepSummary(client, dishId);
      const stepCount = Number(stepSummary.step_count);
      if (stepCount === 0 || stepSummary.first_step_no === null) {
        await client.query("ROLLBACK");
        return { status: "no_steps" };
      }

      const createSessionResult = await client.query<CreateCookSessionRow>(
        `
        INSERT INTO cook_sessions (user_id, dish_id, status, current_step_no)
        VALUES ($1, $2, 'IN_PROGRESS', $3)
        RETURNING id, dish_id, status, current_step_no, started_at;
        `,
        [userId ?? null, dishId, stepSummary.first_step_no]
      );

      const created = createSessionResult.rows[0];

      await client.query(
        `
        INSERT INTO cook_session_steps (session_id, dish_step_id, step_no, timer_seconds_snapshot)
        SELECT $1, ds.id, ds.step_no, ds.timer_seconds
        FROM dish_steps ds
        WHERE ds.dish_id = $2
        ORDER BY ds.step_no ASC;
        `,
        [created.id, dishId]
      );

      await client.query("COMMIT");

      return {
        status: "ok",
        data: {
          sessionId: created.id,
          dishId: created.dish_id,
          status: created.status,
          currentStepNo: created.current_step_no,
          startedAt: created.started_at,
          firstStepTimerSeconds: stepSummary.first_timer_seconds
        }
      };
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }

  async findSessionRuntimeById(sessionId: number): Promise<CookSessionRuntime | null> {
    const result = await this.pool.query<SessionRuntimeRow>(
      `
      SELECT
        s.id AS session_id,
        s.dish_id,
        s.status,
        s.current_step_no,
        css.timer_seconds_snapshot AS current_step_timer_seconds,
        ms.max_step_no
      FROM cook_sessions s
      LEFT JOIN cook_session_steps css
        ON css.session_id = s.id
       AND css.step_no = s.current_step_no
      LEFT JOIN LATERAL (
        SELECT MAX(step_no)::int AS max_step_no
        FROM cook_session_steps csi
        WHERE csi.session_id = s.id
      ) ms ON TRUE
      WHERE s.id = $1
      LIMIT 1;
      `,
      [sessionId]
    );

    const row = result.rows[0];
    if (!row) {
      return null;
    }

    return toRuntime(row);
  }

  async getStepTimer(sessionId: number, stepNo: number): Promise<SessionStepTimer | null> {
    const result = await this.pool.query<StepTimerRow>(
      `
      SELECT step_no, timer_seconds_snapshot
      FROM cook_session_steps
      WHERE session_id = $1
        AND step_no = $2
      LIMIT 1;
      `,
      [sessionId, stepNo]
    );

    const row = result.rows[0];
    if (!row) {
      return null;
    }

    return {
      stepNo: row.step_no,
      timerSeconds: row.timer_seconds_snapshot
    };
  }

  async getNextStepTimer(sessionId: number, stepNo: number): Promise<SessionStepTimer | null> {
    const result = await this.pool.query<StepTimerRow>(
      `
      SELECT step_no, timer_seconds_snapshot
      FROM cook_session_steps
      WHERE session_id = $1
        AND step_no > $2
      ORDER BY step_no ASC
      LIMIT 1;
      `,
      [sessionId, stepNo]
    );

    const row = result.rows[0];
    if (!row) {
      return null;
    }

    return {
      stepNo: row.step_no,
      timerSeconds: row.timer_seconds_snapshot
    };
  }

  async markStepStarted(sessionId: number, stepNo: number): Promise<boolean> {
    const result = await this.pool.query(
      `
      UPDATE cook_session_steps
      SET
        started_at = NOW(),
        finished_at = NULL,
        elapsed_seconds = NULL,
        reminder_fired = FALSE
      WHERE session_id = $1
        AND step_no = $2
        AND finished_at IS NULL;
      `,
      [sessionId, stepNo]
    );

    return (result.rowCount ?? 0) > 0;
  }

  async markStepCompleted(sessionId: number, stepNo: number): Promise<boolean> {
    const result = await this.pool.query(
      `
      UPDATE cook_session_steps
      SET
        finished_at = NOW(),
        elapsed_seconds = GREATEST(EXTRACT(EPOCH FROM (NOW() - started_at))::int, 0)
      WHERE session_id = $1
        AND step_no = $2
        AND finished_at IS NULL;
      `,
      [sessionId, stepNo]
    );

    return (result.rowCount ?? 0) > 0;
  }

  async updateCurrentStep(sessionId: number, currentStepNo: number): Promise<void> {
    await this.pool.query(
      `
      UPDATE cook_sessions
      SET current_step_no = $2
      WHERE id = $1;
      `,
      [sessionId, currentStepNo]
    );
  }

  async refreshSessionElapsed(sessionId: number): Promise<void> {
    await this.pool.query(
      `
      UPDATE cook_sessions
      SET total_elapsed_seconds = (
        SELECT COALESCE(SUM(css.elapsed_seconds), 0)
        FROM cook_session_steps css
        WHERE css.session_id = $1
          AND css.elapsed_seconds IS NOT NULL
      )
      WHERE id = $1;
      `,
      [sessionId]
    );
  }

  private async findActiveDish(client: PoolClient, dishId: number): Promise<boolean> {
    const result = await client.query<{ id: number }>(
      `
      SELECT id
      FROM dishes
      WHERE id = $1
        AND is_active = TRUE
      LIMIT 1;
      `,
      [dishId]
    );

    return result.rows.length > 0;
  }

  private async getDishStepSummary(client: PoolClient, dishId: number): Promise<StepSummaryRow> {
    const result = await client.query<StepSummaryRow>(
      `
      SELECT
        COUNT(*)::text AS step_count,
        MIN(step_no)::int AS first_step_no,
        MAX(step_no)::int AS max_step_no,
        COALESCE((
          SELECT timer_seconds
          FROM dish_steps
          WHERE dish_id = $1
          ORDER BY step_no ASC
          LIMIT 1
        ), 0)::int AS first_timer_seconds
      FROM dish_steps
      WHERE dish_id = $1;
      `,
      [dishId]
    );

    return result.rows[0];
  }
}
