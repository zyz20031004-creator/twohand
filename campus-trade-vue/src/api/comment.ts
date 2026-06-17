import { http } from "@/api/http";

export const apiGetProductComments = (params: {
  productId: number;
  currentUserId?: number;
}) => http.get("/comment/product/tree", { params });

export const apiAddProductComment = (data: {
  productId: number;
  userId: number;
  content: string;
  parentId?: number | null;
}) => http.post("/comment/product/add", data);

export const apiDeleteComment = (commentId: number, userId: number) =>
  http.delete(`/comment/product/delete/${commentId}`, {
    params: { userId },
  });

export const apiWantedCommentList = (targetId: number) =>
  http.get("/comment/wanted/list", { params: { targetId } });

export const apiGetWantedComments = (params: {
  targetId: number;
  currentUserId?: number;
}) => http.get("/comment/wanted/tree", { params });

export const apiAddWantedComment = (data: {
  targetId: number;
  userId: number;
  content: string;
  parentId?: number | null;
}) => http.post("/comment/wanted/add", data);

export const apiDeleteWantedComment = (commentId: number, userId: number) =>
  http.delete(`/comment/wanted/delete/${commentId}`, {
    params: { userId },
  });

