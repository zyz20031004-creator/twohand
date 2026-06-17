import { http } from "@/api/http";

export const apiFeedbackSubmit = (data: {
  subject: string;
  content: string;
  contact: string;
  email?: string;
  userId?: number;
}) => http.post("/feedback/submit", data);

/** =========================
 * 管理员：反馈管理
 * ========================= */

/** 管理员：反馈分页 */
export const apiAdminFeedbackPage = (params: {
  page: number;
  size: number;
  keyword?: string;
}) => http.get("/admin/feedback/page", { params });

/** 管理员：回复反馈 */
export const apiAdminFeedbackReply = (id: number, data: { reply: string; repliedBy?: number }) =>
  http.put(`/admin/feedback/${id}/reply`, data);

/** 管理员：删除反馈 */
export const apiAdminFeedbackDelete = (id: number) => http.delete(`/admin/feedback/${id}`);

/** 管理员：批量删除反馈 */
export const apiAdminFeedbackBatchDelete = (ids: number[]) =>
  http.delete("/admin/feedback/batch", { data: { ids } });

/** =========================
 * 用户端：我的反馈管理
 * ========================= */

/** 获取我的反馈列表 */
export const apiMyFeedbackPage = (params: {
  page: number;
  size: number;
  userId: number;
  status?: "OPEN" | "CLOSED";
  keyword?: string;
}) => http.get("/feedback/my/page", { params });

/** 删除我的反馈 */
export const apiDeleteMyFeedback = (id: number, userId: number) => http.delete(`/feedback/delete/${id}`, { params: { userId } });
