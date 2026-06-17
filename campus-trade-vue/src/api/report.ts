import { http } from "@/api/http";

export type ReportStatus = "PENDING" | "VALID" | "INVALID" | "HANDLED";
export type ReportHandleStatus = Exclude<ReportStatus, "PENDING">;

export interface SubmitReportPayload {
  productId: number;
  reason: string;
  detail?: string;
}

export const apiSubmitReport = (data: SubmitReportPayload) =>
  http.post("/report/submit", data);

export interface MyReportItem {
  id: number;
  productId: number;
  productTitle?: string;
  productExists?: boolean | number;
  productCoverUrl?: string;
  reason: string;
  detail?: string;
  status: ReportStatus;
  handleRemark?: string;
  handledAt?: string;
  createdAt?: string;
}

export interface MyReportPageResp {
  records: MyReportItem[];
  total: number;
  page?: number;
  size?: number;
}

export const apiMyReportPage = (params: {
  page: number;
  size: number;
  keyword?: string;
  status?: ReportStatus;
}) => http.get<MyReportPageResp>("/report/my/page", { params });

export interface AdminReportItem {
  id: number;
  reporterId: number;
  reporterName?: string;
  reporterNickName?: string;
  reporterUsername?: string;
  productId: number;
  productTitle?: string;
  reason: string;
  detail?: string;
  status: ReportStatus;
  handleRemark?: string;
  handledBy?: number;
  handlerName?: string;
  handlerNickName?: string;
  handlerUsername?: string;
  handledAt?: string;
  createdAt?: string;
}

export interface AdminReportPageResp {
  records: AdminReportItem[];
  total: number;
  page?: number;
  size?: number;
}

export const apiAdminReportPage = (params: {
  page: number;
  size: number;
  keyword?: string;
  status?: ReportStatus;
}) => http.get<AdminReportPageResp>("/admin/report/page", { params });

export const apiAdminReportHandle = (data: {
  id: number;
  status: ReportHandleStatus;
  handleRemark?: string;
}) => http.post("/admin/report/handle", data);

