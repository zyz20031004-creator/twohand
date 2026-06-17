import { http } from "@/api/http";

/**
 * @deprecated 旧商品详情接口封装，仅保留兼容历史引用。
 * 新代码请使用 "@/api/product" 中的 apiGetProductDetail、apiToggleLike、apiToggleFavorite。
 * 本文件中的 userId 参数旧写法不再作为身份依据。
 */

/** 商品详情（带 coverUrl、卖家、是否已赞/收藏、收藏数等） */
export const apiGetProductDetail = (id: number, userId?: number) =>
  http.get(`/product/detail/${id}`, {
    params: { userId },
  });

/** 点赞/取消点赞（返回 liked + likeCount） */
export const apiToggleLike = (productId: number, userId: number) =>
  http.post(`/product/like/toggle`, null, {
    params: { productId, userId },
  });

/** 收藏/取消收藏（返回 favorited + favoriteCount） */
export const apiToggleFavorite = (productId: number, userId: number) =>
  http.post(`/product/favorite/toggle`, null, {
    params: { productId, userId },
  });
