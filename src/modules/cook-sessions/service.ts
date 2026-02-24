import type { RedisClient } from "../../infrastructure/redis.js";
import type {
  CookSessionStateResponse,
  CookSessionStepActionResponse,
  CookTimerState,
  StartCookSessionInput,
  StartCookSessionResponse
} from "../../types/cook-session.js";
import { CookSessionsRepository, type CookSessionRuntime } from "./repository.js";

const SESSION_STATE_TTL_SECONDS = 24 * 60 * 60;

const buildSessionStateKey = (sessionId: number): string => {
  return `chef:session:state:${sessionId}`;
};

const parseInteger = (value: string | undefined): number | null => {
  if (!value) {
    return null;
  }

  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 0) {
    return null;
  }

  return parsed;
};

interface SessionStateCache {
  currentStepNo: number;
  remainingSeconds: number;
  isPaused: boolean;
}

export class EntityNotFoundError extends Error {}
export class SessionConflictError extends Error {}

export class CookSessionsService {
  constructor(
    private readonly repository: CookSessionsRepository,
    private readonly redis: RedisClient | null,
    private readonly logger: { warn: (message: string, error?: unknown) => void }
  ) {}

  async startSession(input: StartCookSessionInput): Promise<StartCookSessionResponse> {
    const attempt = await this.repository.createSession(input.dishId, input.userId);
    if (attempt.status === "dish_not_found") {
      throw new EntityNotFoundError("Dish not found");
    }

    if (attempt.status === "no_steps") {
      throw new SessionConflictError("Dish has no steps");
    }

    const session = attempt.data;
    if (!session) {
      throw new SessionConflictError("Session creation failed");
    }
    await this.saveSessionState({
      sessionId: session.sessionId,
      currentStepNo: session.currentStepNo,
      remainingSeconds: session.firstStepTimerSeconds,
      isPaused: true
    });

    return {
      session_id: session.sessionId,
      dish_id: session.dishId,
      status: session.status,
      current_step_no: session.currentStepNo,
      started_at: session.startedAt.toISOString()
    };
  }

  async getSessionState(sessionId: number): Promise<CookSessionStateResponse> {
    const runtime = await this.requireRuntime(sessionId);
    const cacheState = await this.readSessionState(sessionId);
    const timer = this.resolveTimerState(runtime, cacheState);

    return {
      session_id: runtime.sessionId,
      status: runtime.status,
      current_step_no: runtime.currentStepNo,
      timer
    };
  }

  async startStep(sessionId: number, stepNo: number): Promise<CookSessionStepActionResponse> {
    const runtime = await this.requireRuntime(sessionId);
    this.ensureMutableSession(runtime);

    if (stepNo !== runtime.currentStepNo) {
      throw new SessionConflictError("Step start must match current step");
    }

    const stepTimer = await this.repository.getStepTimer(sessionId, stepNo);
    if (!stepTimer) {
      throw new SessionConflictError("Step does not exist in session");
    }

    await this.repository.markStepStarted(sessionId, stepNo);
    await this.saveSessionState({
      sessionId,
      currentStepNo: stepNo,
      remainingSeconds: stepTimer.timerSeconds,
      isPaused: false
    });

    return {
      session_id: runtime.sessionId,
      current_step_no: runtime.currentStepNo,
      status: runtime.status
    };
  }

  async completeStep(sessionId: number, stepNo: number): Promise<CookSessionStepActionResponse> {
    const runtime = await this.requireRuntime(sessionId);
    this.ensureMutableSession(runtime);

    if (stepNo !== runtime.currentStepNo) {
      throw new SessionConflictError("Step complete must match current step");
    }

    const marked = await this.repository.markStepCompleted(sessionId, stepNo);
    if (!marked) {
      throw new SessionConflictError("Step does not exist in session");
    }

    const nextStep = await this.repository.getNextStepTimer(sessionId, stepNo);
    const nextStepNo = nextStep?.stepNo ?? stepNo;
    await this.repository.updateCurrentStep(sessionId, nextStepNo);
    await this.repository.refreshSessionElapsed(sessionId);

    await this.saveSessionState({
      sessionId,
      currentStepNo: nextStepNo,
      remainingSeconds: nextStep?.timerSeconds ?? 0,
      isPaused: true
    });

    return {
      session_id: runtime.sessionId,
      current_step_no: nextStepNo,
      status: runtime.status
    };
  }

  async pauseTimer(sessionId: number): Promise<CookSessionStateResponse> {
    return this.updateTimerState(sessionId, "pause");
  }

  async resumeTimer(sessionId: number): Promise<CookSessionStateResponse> {
    return this.updateTimerState(sessionId, "resume");
  }

  async resetTimer(sessionId: number): Promise<CookSessionStateResponse> {
    return this.updateTimerState(sessionId, "reset");
  }

  private async updateTimerState(
    sessionId: number,
    action: "pause" | "resume" | "reset"
  ): Promise<CookSessionStateResponse> {
    const runtime = await this.requireRuntime(sessionId);
    this.ensureMutableSession(runtime);

    const cacheState = await this.readSessionState(sessionId);
    const baseTimer = this.resolveTimerState(runtime, cacheState);

    const nextTimer: CookTimerState =
      action === "pause"
        ? { ...baseTimer, is_paused: true }
        : action === "resume"
          ? { ...baseTimer, is_paused: false }
          : { remaining_seconds: runtime.currentStepTimerSeconds, is_paused: false };

    await this.saveSessionState({
      sessionId,
      currentStepNo: runtime.currentStepNo,
      remainingSeconds: nextTimer.remaining_seconds,
      isPaused: nextTimer.is_paused
    });

    return {
      session_id: runtime.sessionId,
      status: runtime.status,
      current_step_no: runtime.currentStepNo,
      timer: nextTimer
    };
  }

  private async requireRuntime(sessionId: number): Promise<CookSessionRuntime> {
    const runtime = await this.repository.findSessionRuntimeById(sessionId);
    if (!runtime) {
      throw new EntityNotFoundError("Session not found");
    }

    return runtime;
  }

  private ensureMutableSession(runtime: CookSessionRuntime): void {
    if (runtime.status !== "IN_PROGRESS") {
      throw new SessionConflictError("Session is not in progress");
    }
  }

  private resolveTimerState(
    runtime: CookSessionRuntime,
    cacheState: SessionStateCache | null
  ): CookTimerState {
    if (!cacheState || cacheState.currentStepNo !== runtime.currentStepNo) {
      return {
        remaining_seconds: runtime.currentStepTimerSeconds,
        is_paused: true
      };
    }

    return {
      remaining_seconds: cacheState.remainingSeconds,
      is_paused: cacheState.isPaused
    };
  }

  private async readSessionState(sessionId: number): Promise<SessionStateCache | null> {
    if (!this.redis) {
      return null;
    }

    try {
      const key = buildSessionStateKey(sessionId);
      const state = await this.redis.hGetAll(key);

      const currentStepNo = parseInteger(state.current_step_no);
      const remainingSeconds = parseInteger(state.remaining_seconds);
      const isPausedRaw = state.is_paused;
      const isPaused = isPausedRaw === "1";

      if (currentStepNo === null || remainingSeconds === null || (isPausedRaw !== "0" && isPausedRaw !== "1")) {
        return null;
      }

      return {
        currentStepNo,
        remainingSeconds,
        isPaused
      };
    } catch (error) {
      this.logger.warn("Failed to read session state from Redis.", error);
      return null;
    }
  }

  private async saveSessionState(state: {
    sessionId: number;
    currentStepNo: number;
    remainingSeconds: number;
    isPaused: boolean;
  }): Promise<void> {
    if (!this.redis) {
      return;
    }

    try {
      const key = buildSessionStateKey(state.sessionId);
      await this.redis.hSet(key, {
        current_step_no: String(state.currentStepNo),
        remaining_seconds: String(state.remainingSeconds),
        is_paused: state.isPaused ? "1" : "0",
        updated_at_epoch: String(Math.floor(Date.now() / 1000))
      });
      await this.redis.expire(key, SESSION_STATE_TTL_SECONDS);
    } catch (error) {
      this.logger.warn("Failed to save session state to Redis.", error);
      return;
    }
  }
}
