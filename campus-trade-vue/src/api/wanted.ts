import { http } from "@/api/http";

// ✅ 标准命名：apiWantedPage
export const apiWantedPage = (params: {
  page: number;
  size: number;
  status?: "OPEN" | "SOLVED";
  keyword?: string;
}) => http.get("/wanted/page", { params });

/** 记录求购浏览量 */
export const apiIncreaseWantedView = (id: number) => http.post(`/wanted/${id}/view`);

// ✅ 兼容命名：apiGetWantedPage（防止你页面里用的是这个）
export const apiGetWantedPage = apiWantedPage;

// ✅ 兼容默认导出（极少数写法会用到）
export default {
  apiWantedPage,
  apiIncreaseWantedView,
  apiGetWantedPage,
};

/** =========================
 * 管理员：求购管理
 * ========================= */

/** 管理员：求购分页（keyword/status） */
export const apiAdminWantedPage = (params: {
  page: number;
  size: number;
  keyword?: string; // 标题/内容/用户名
  status?: "OPEN" | "SOLVED";
}) => http.get("/admin/wanted/page", { params });

/** 管理员：删除求购 */
export const apiAdminWantedDelete = (id: number) => http.delete(`/admin/wanted/${id}`);

/** 管理员：批量删除 */
export const apiAdminWantedBatchDelete = (ids: number[]) =>
  http.delete("/admin/wanted/batch", { data: { ids } });

/** =========================
 * 用户端：我的求购管理
 * ========================= */

/** 获取我的求购列表 */
export const apiMyWantedPage = (params: {
  page: number;
  size: number;
  userId: number;
  status?: "OPEN" | "SOLVED";
  keyword?: string;
}) => http.get("/wanted/my/page", { params });

/** 创建求购 */
export const apiCreateWanted = (data: {
  title: string;
  content: string;
  imageUrl?: string;
  userId: number;
}) => http.post("/wanted/create", data);

/** 更新求购 */
export const apiUpdateWanted = (id: number, data: {
  title: string;
  content: string;
  imageUrl?: string;
  userId: number;
}) => http.put(`/wanted/update/${id}`, data);

/** 删除求购 */
export const apiDeleteWanted = (id: number, userId: number) => http.delete(`/wanted/delete/${id}`, { params: { userId } });

/** 标记求购为已解决 */
export const apiSolveWanted = (id: number, userId: number) => http.put(`/wanted/solve/${id}`, {}, { params: { userId } });
