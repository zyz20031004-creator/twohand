import { http } from "@/api/http";

export const apiCreateOrder = (productId: number, addressId?: number) =>
  http.post("/order/create", null, { params: { productId, addressId } });

export const apiAdminOrderPage = (params: {
  page: number;
  size: number;
  keyword?: string;
  status?: string;
  payType?: string;
}) => http.get("/admin/order/page", { params });

export const apiAdminOrderPayTypeStats = (params: {
  keyword?: string;
  status?: string;
  payType?: string;
}) => http.get("/admin/order/payTypeStats", { params });

export const apiAdminOrderDelete = (id: number) => http.delete(`/admin/order/${id}`);

export const apiAdminOrderBatchDelete = (ids: number[]) =>
  http.post("/admin/order/batch-delete", { ids });

export const apiAdminOrderCancel = (id: number) =>
  http.post(`/admin/order/${id}/cancel`);

