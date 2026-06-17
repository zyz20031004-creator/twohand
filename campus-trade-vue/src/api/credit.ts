import { http } from "@/api/http";

export interface CreditRecordItem {
  id: number;
  userId: number;
  changeVal: number;
  reason: string;
  bizType?: string;
  bizId?: number;
  remark?: string;
  reviewContent?: string;
  content?: string;
  ratingLevel?: "GOOD" | "NEUTRAL" | "BAD" | string;
  ratingText?: string;
  sourceUserId?: number;
  sourceUserName?: string;
  sourceNickName?: string;
  sourceAvatar?: string;
  createdAt?: string;
}

export interface CreditMyResp {
  score: number;
  records: CreditRecordItem[];
  total: number;
}

export const apiCreditMy = (params: { page: number; size: number }) =>
  http.get<CreditMyResp>("/credit/my", { params });

export const apiCreditMyReviews = (params: { page: number; size: number }) =>
  http.get<CreditMyResp>("/credit/my/reviews", { params });

export interface AdminCreditUserItem {
  id: number;
  username: string;
  name?: string;
  realName?: string;
  school?: string;
  verifyStatus?: string;
  creditScore?: number;
  status?: number;
  updatedAt?: string;
}

export interface AdminCreditPageResp {
  total: number;
  records: AdminCreditUserItem[];
}

export interface AdminCreditLogResp {
  userId: number;
  username?: string;
  name?: string;
  score: number;
  records: Array<CreditRecordItem & { operatorName?: string | null }>;
  total: number;
}

export const apiAdminCreditPage = (params: {
  page: number;
  size: number;
  keyword?: string;
  verifyStatus?: string;
  minScore?: number;
  maxScore?: number;
}) => http.get<AdminCreditPageResp>("/admin/credit/page", { params });

export const apiAdminCreditLogs = (params: {
  userId: number;
  page: number;
  size: number;
}) => http.get<AdminCreditLogResp>("/admin/credit/logs", { params });

export const apiAdminCreditAdjust = (data: {
  userId: number;
  delta: number;
  remark: string;
}) => http.post("/admin/credit/adjust", data);

