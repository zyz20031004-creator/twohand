import { http } from "./http";

export interface PageResp<T> {
  records: T[];
  total: number;
  page?: number;
  size?: number;
}

export interface ProductImageItem {
  id?: number;
  productId?: number;
  url: string;
  sort?: number;
}

export interface ProductItem {
  id: number;
  title: string;
  price: number;
  viewCount: number;
  likeCount: number;
  coverUrl?: string;
  images?: ProductImageItem[];
  status?: "ON" | "OFF";
  auditStatus?: "PENDING" | "APPROVED" | "REJECTED";
  createdAt?: string;
  categoryId?: number;
  description?: string;
  addressText?: string;
  favoriteCount?: number;
  schoolName?: string;
}

export const apiGetProductPage = (params: {
  page: number;
  size: number;
  keyword?: string;
  categoryId?: number;
  status?: "ON" | "OFF";
  auditStatus?: "PENDING" | "APPROVED" | "REJECTED";
  sortBy?: "createdAt" | "viewCount" | "likeCount" | "price";
  sortOrder?: "asc" | "desc";
}) =>
  http.get<PageResp<ProductItem>>("/product/page", {
    params,
  });

export interface ProductDetailResp {
  id: number;
  title: string;
  price: number;
  description?: string;
  addressText?: string;
  createdAt?: string;
  viewCount?: number;
  coverUrl?: string;
  images?: ProductImageItem[];
  likeCount: number;
  liked: boolean;
  favoriteCount: number;
  favorited: boolean;
  sellerId?: number;
  sellerName?: string;
  sellerAvatar?: string;
  schoolName?: string;
}

export const apiGetProductDetail = (id: number) =>
  http.get<ProductDetailResp>(`/product/detail/${id}`);

export interface AdminProductDetailResp {
  id: number;
  name?: string;
  title?: string;
  price: number;
  description?: string;
  shipAddress?: string;
  addressText?: string;
  createdAt?: string;
  auditStatus?: "PENDING" | "APPROVED" | "REJECTED";
  auditReason?: string;
  categoryName?: string;
  userId?: number;
  username?: string;
  sellerNickName?: string;
  sellerUsername?: string;
  saleStatus?: "ON" | "OFF";
  status?: "ON" | "OFF";
  coverUrl?: string;
  images?: ProductImageItem[];
  schoolName?: string;
}

export const apiAdminProductDetail = (id: number) =>
  http.get<AdminProductDetailResp>(`/admin/product/detail/${id}`);

export const apiToggleLike = (productId: number) =>
  http.post<{ liked: boolean; likeCount: number }>(
    "/product/like/toggle",
    null,
    { params: { productId } }
  );

export const apiToggleFavorite = (productId: number) =>
  http.post<{ favorited: boolean; favoriteCount: number }>(
    "/product/favorite/toggle",
    null,
    { params: { productId } }
  );

export const apiGetMyProductPage = (params: {
  page: number;
  size: number;
  keyword?: string;
  status?: "ON" | "OFF";
}) =>
  http.get<PageResp<ProductItem>>("/product/my/page", {
    params,
  });

export const apiDeleteProduct = (id: number) =>
  http.delete<void>(`/product/${id}`);

export const apiToggleProduct = (id: number) =>
  http.put<void>(`/product/${id}/toggle`);

export interface ProductSavePayload {
  title: string;
  price: number | string;
  coverUrl?: string;
  images?: Array<string | ProductImageItem>;
  status: "ON" | "OFF";
  categoryId?: number;
  description?: string;
  addressText?: string;
}

export const apiUpdateProduct = (id: number, data: ProductSavePayload) =>
  http.put("/product/" + id, data);

export const apiCreateProduct = (data: ProductSavePayload) =>
  http.post("/product", data);

