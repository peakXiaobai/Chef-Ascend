export type CookSessionStatus = "IN_PROGRESS" | "COMPLETED" | "ABANDONED";

export interface StartCookSessionInput {
  dishId: number;
  userId?: number;
}

export interface StartCookSessionResponse {
  session_id: number;
  dish_id: number;
  status: CookSessionStatus;
  current_step_no: number;
  started_at: string;
}

export interface CookTimerState {
  remaining_seconds: number;
  is_paused: boolean;
}

export interface CookSessionStateResponse {
  session_id: number;
  status: CookSessionStatus;
  current_step_no: number;
  timer: CookTimerState;
}

export interface CookSessionStepActionResponse {
  session_id: number;
  current_step_no: number;
  status: CookSessionStatus;
}
