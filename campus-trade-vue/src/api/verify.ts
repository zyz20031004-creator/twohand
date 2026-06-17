import { http } from "@/api/http";

export type VerifyStatus = "UNVERIFIED" | "PENDING" | "VERIFIED" | "REJECTED";
export type AdminVerifyStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface VerifyApplyItem {
  id: number;
  userId: number;
  school: string;
  studentNo: string;
  realName?: string;
  proofUrl?: string;
  fileUrl?: string;
  imageUrl?: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  rejectReason?: string;
  createdAt?: string;
}

export interface VerifyMyResp {
  verifyStatus: VerifyStatus;
  latestApply: VerifyApplyItem | null;
}

export interface VerifyApplyReq {
  school: string;
  studentNo: string;
  realName?: string;
  proofUrl?: string;
}

export const apiVerifyMy = () => http.get<VerifyMyResp>("/verify/my");

export const apiVerifyApply = (data: VerifyApplyReq) =>
  http.post("/verify/apply", data);

export interface AdminVerifyPageItem extends VerifyApplyItem {
  username?: string;
}

export interface AdminVerifyPageResp {
  total: number;
  records: AdminVerifyPageItem[];
}

export const apiAdminVerifyPage = (params: {
  page: number;
  size: number;
  status?: AdminVerifyStatus;
  keyword?: string;
}) => http.get<AdminVerifyPageResp>("/admin/verify/page", { params });

export const apiAdminVerifyApprove = (id: number) =>
  http.post("/admin/verify/approve", { id });

export const apiAdminVerifyReject = (data: { id: number; reason: string }) =>
  http.post("/admin/verify/reject", data);

