import { http } from "./http";

export interface Category {
  id: number;
  name: string;
}

export const apiGetCategoryList = () => http.get<Category[]>("/category/list");

export interface AdminCategoryPageResp {
  list: Category[];
  total: number;
}

export const apiAdminCategoryPage = (params: {
  page: number;
  size: number;
  keyword?: string;
  status?: number;
}) => http.get<AdminCategoryPageResp>("/admin/categories", { params });

export const apiAdminCategoryCreate = (data: { name: string }) =>
  http.post<Category>("/admin/categories", data);

export const apiAdminCategoryUpdate = (id: number, data: { name: string }) =>
  http.put<Category>(`/admin/categories/${id}`, data);

export const apiAdminCategoryDelete = (id: number) =>
  http.delete<void>(`/admin/categories/${id}`);

export const apiAdminCategoryBatchDelete = (ids: number[]) =>
  http.post<void>("/admin/categories/batch-delete", { ids });

