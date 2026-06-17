import { http } from "@/api/http";

/** =========================
 * 管理员：地址管理
 * ========================= */

/** 管理员：地址分页（keyword：联系人/电话/地址/用户名/用户ID） */
export const apiAdminAddressPage = (params: {
  page: number;
  size: number;
  keyword?: string;
}) => http.get("/admin/address/page", { params });

/** 管理员：删除地址（软删） */
export const apiAdminAddressDelete = (id: number) => http.delete(`/admin/address/${id}`);

/** 管理员：批量删除地址（软删） */
export const apiAdminAddressBatchDelete = (ids: number[]) =>
  http.delete("/admin/address/batch", { data: { ids } });
