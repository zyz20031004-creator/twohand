import { http } from "@/api/http";
import type { PageResp, ProductItem } from "@/api/product";

export const apiAdminPage = (params: { page: number; size: number; keyword?: string }) =>
  http.get("/admin/admin/page", { params });

export const apiAdminCreate = (data: {
  username: string;
  password: string;
  name?: string;
  phone?: string;
  email?: string;
  avatar?: string;
  status?: number;
}) => http.post("/admin/admin", data);

export const apiAdminUpdate = (
  id: number,
  data: { name?: string; phone?: string; email?: string; avatar?: string; status?: number; password?: string }
) => http.put(`/admin/admin/${id}`, data);

export const apiAdminDelete = (id: number) => http.delete(`/admin/admin/${id}`);

export const apiAdminBatchDelete = (ids: number[]) =>
  http.delete("/admin/admin/batch", { data: { ids } });

export const apiAdminUserPage = (params: { page: number; size: number; keyword?: string }) =>
  http.get("/admin/user/page", { params });

export const apiAdminUserCreate = (data: {
  username: string;
  password: string;
  name?: string;
  phone?: string;
  email?: string;
  avatar?: string;
}) => http.post("/admin/user", data);

export const apiAdminUserUpdate = (
  id: number,
  data: { status: 0 | 1 }
) => http.put(`/admin/user/${id}`, data);

export const apiAdminUserDelete = (id: number) => http.delete(`/admin/user/${id}`);

export const apiAdminUserBatchDelete = (ids: number[]) =>
  http.delete("/admin/user/batch", { data: { ids } });

export const apiAdminUserChangeStatus = (id: number, status: 0 | 1) =>
  http.put(`/admin/user/${id}/status`, { status });

export const apiAdminUserBatchChangeStatus = (ids: number[], status: 0 | 1) =>
  http.put("/admin/user/batch/status", { ids, status });

export const apiGetMe = () => http.get("/user/me");

export const apiUpdateMe = (data: {
  name?: string;
  phone?: string;
  email?: string;
  avatar?: string;
}) => http.put("/user/me", data);

export const apiChangePassword = (data: {
  oldPwd: string;
  newPwd: string;
  confirmPwd: string;
}) => http.post("/user/changePassword", data);

export type UserPublicProfileResp = {
  id: number;
  userId?: number;
  nickname?: string;
  name?: string;
  avatar?: string;
  school?: string;
  verifyStatus?: "UNVERIFIED" | "PENDING" | "VERIFIED" | "REJECTED" | string;
  creditScore?: number;
  publishedCount?: number;
};

export type UserPublicProductItem = ProductItem & {
  displayStatus?: "ON" | "OFF" | "SOLD";
};

export type UserPublicOrderLite = {
  id: number;
  buyerId?: number;
  buyerName?: string;
  buyerAvatar?: string;
  sellerId?: number;
  sellerName?: string;
  sellerAvatar?: string;
};

export type UserPublicCreditResp = {
  score: number;
  total: number;
  records: Array<{
    id: number;
    userId: number;
    changeVal: number;
    reason?: string;
    bizType?: string;
    bizId?: number;
    sourceUserId?: number;
    sourceUserName?: string;
    sourceNickName?: string;
    sourceAvatar?: string;
    sourceRoleText?: string;
    ratingLevel?: "GOOD" | "NEUTRAL" | "BAD" | string;
    ratingText?: string;
    reviewContent?: string;
    content?: string;
    remark?: string;
    createdAt?: string;
  }>;
  buyOrders?: UserPublicOrderLite[];
  sellOrders?: UserPublicOrderLite[];
};

export const apiGetUserPublicProfile = (id: number) =>
  http.get<UserPublicProfileResp>(`/user/profile/${id}`);

export const apiGetUserPublicProducts = (
  id: number,
  params: {
    page: number;
    size: number;
    status?: "ON" | "OFF" | "SOLD";
  }
) => http.get<PageResp<UserPublicProductItem>>(`/user/profile/${id}/products`, { params });

export const apiGetUserPublicCredit = (
  id: number,
  params: {
    page: number;
    size: number;
    filter?: "POSITIVE" | "NEGATIVE";
  }
) => http.get<UserPublicCreditResp>(`/user/profile/${id}/credit`, { params });

export const apiGetMyOrderPage = <T>(params: {
  type: "BUY" | "SELL";
  page: number;
  size: number;
  keyword?: string;
  status?: string;
  payType?: string;
}) => http.get<T>("/order/myPage", { params });

export const apiPayOrder = (id: number, payType: "WECHAT" | "ALIPAY" = "WECHAT") =>
  http.post("/order/pay", null, { params: { id, payType } });

export const apiCancelOrder = (id: number) =>
  http.post("/order/cancel", null, { params: { id } });

export const apiFinishOrder = (
  id: number,
  data: {
    review?: string;
  }
) =>
  http.post("/order/finish", null, {
    params: {
      id,
      review: data.review?.trim() || undefined,
    },
  });

export const apiHideOrder = (id: number) =>
  http.post("/order/hide", null, { params: { id } });

export const apiDeleteOrder = apiHideOrder;

export const apiGetMyFavoritePage = <T>(params: {
  page: number;
  size: number;
  keyword?: string;
}) => http.get<T>("/favorite/myPage", { params });

export const apiCancelFavorite = (productId: number) =>
  http.post("/favorite/cancel", null, { params: { productId } });

export const apiCancelFavoriteBatch = (ids: number[]) =>
  http.post("/favorite/cancelBatch", ids);

export type UserAddressItem = {
  id: number;
  contactName?: string;
  contactPhone?: string;
  addressText?: string;
  isDefault?: number;
  status?: number;
};

export const apiGetMyAddressList = () =>
  http.get<UserAddressItem[]>("/address/my/list");

export const apiCreateAddress = (data: any) =>
  http.post("/address/create", data);

export const apiUpdateAddress = (id: number, data: any) =>
  http.put(`/address/update/${id}`, data);

export const apiDeleteAddress = (id: number) =>
  http.post(`/address/delete/${id}`);

export const apiSetDefaultAddress = (id: number) =>
  http.post(`/address/default/${id}`);




