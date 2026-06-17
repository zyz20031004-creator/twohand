import { http } from "@/api/http";

export const apiNoticePage = (params: { page: number; size: number }) =>
  http.get("/notice/page", { params });

export const apiNoticeDetail = (id: number) =>
  http.get("/notice/detail", { params: { id } });

/** =========================
 * 管理员：公告管理
 * ========================= */

/** 分页查询 */
export const apiAdminNoticePage = (params: {
  page: number;
  size: number;
  keyword?: string; // 标题关键字
  status?: 0 | 1;   // 1展示 0下线
}) => http.get("/admin/notice/page", { params });

/** 新增公告 */
export const apiAdminNoticeCreate = (data: {
  title: string;
  content: string;
  status?: 0 | 1;
}) => http.post("/admin/notice", data);

/** 编辑公告 */
export const apiAdminNoticeUpdate = (id: number, data: {
  title: string;
  content: string;
  status?: 0 | 1;
}) => http.put(`/admin/notice/${id}`, data);

/** 删除公告 */
export const apiAdminNoticeDelete = (id: number) => http.delete(`/admin/notice/${id}`);

/** 批量删除 */
export const apiAdminNoticeBatchDelete = (ids: number[]) =>
  http.delete("/admin/notice/batch", { data: { ids } });

/** 上下线 */
export const apiAdminNoticeSetStatus = (id: number, status: 0 | 1) =>
  http.put(`/admin/notice/${id}/status`, { status });
