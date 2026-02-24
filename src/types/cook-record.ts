export type CookRecordResult = "SUCCESS" | "FAILED";

export interface CompleteCookSessionInput {
  sessionId: number;
  userId?: number;
  result: CookRecordResult;
  rating?: number;
  note?: string;
}

export interface CompleteCookSessionResponse {
  session_id: number;
  record_id: number;
  result: CookRecordResult;
  today_cook_count: number;
}

export interface UserCookRecordItem {
  record_id: number;
  dish_id: number;
  dish_name: string;
  result: CookRecordResult;
  rating: number | null;
  cooked_at: string;
}

export interface UserCookRecordListResponse {
  page: number;
  page_size: number;
  total: number;
  items: UserCookRecordItem[];
}
