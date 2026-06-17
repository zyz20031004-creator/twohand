import { http } from "@/api/http";

export const apiAdminProductPage = (params: {
  page: number;
  size: number;
  keyword?: string;
  school?: string;
  auditStatus?: "PENDING" | "APPROVED" | "REJECTED";
  saleStatus?: "ON" | "OFF";
}) => http.get("/admin/product/page", { params });

export const apiAdminProductDetail = (id: number) =>
  http.get(`/admin/product/detail/${id}`);

export const apiAdminProductApprove = (id: number) =>
  http.post(`/admin/product/${id}/approve`);

export const apiAdminProductReject = (id: number, data: { reason: string }) =>
  http.post(`/admin/product/${id}/reject`, data);

export const apiAdminProductOff = (id: number, data: { reason: string }) =>
  http.post(`/admin/product/${id}/off`, data);

export const apiAdminProductOn = (id: number) =>
  http.post(`/admin/product/${id}/on`);

export const apiAdminProductDelete = (id: number) =>
  http.delete(`/admin/product/${id}`);

export const apiAdminProductBatchDelete = (ids: number[]) =>
  http.post("/admin/product/batch-delete", { ids });
