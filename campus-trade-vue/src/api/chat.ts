import { http } from "@/api/http";

export const apiSendMessage = (data: {
  sessionId: number;
  content: string;
}) => http.post("/chat/send", data);

export const apiGetChatHistory = (params: {
  sessionId: number;
  page?: number;
  size?: number;
}) => http.get("/chat/history", { params });

export const apiGetContacts = (params: {
  userId: number;
}) => http.get("/chat/contacts", { params });

export const apiMarkAsRead = (params: {
  sessionId: number;
}) => http.put("/chat/read", null, { params });

export const apiGetUnreadCount = (params: {
  userId: number;
}) => http.get("/chat/unread/count", { params });

export const apiDeleteConversation = (sessionId: number) =>
  http.post("/chat/delete", null, { params: { sessionId } });

export const apiGetChatProduct = (params: {
  productId: number;
  sessionId?: number;
}) => http.get("/chat/product", { params });

export const apiCreateOrGetProductSession = (data: { productId: number; targetUserId?: number }) =>
  http.post("/chat/session/product", data);

export const apiUpdateChatProductPrice = (data: {
  productId: number;
  price: number | string;
}) => http.post("/chat/product/price", data);

