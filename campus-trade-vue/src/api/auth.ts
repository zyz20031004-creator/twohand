import { http } from "@/api/http";

export type Role = "USER" | "ADMIN";

export interface RegisterReq {
  username: string;
  password: string;
}

export interface LoginReq {
  username: string;
  password: string;
  role?: Role; // 你后端支持传 role 校验
}

export interface UserResp {
  id: number;
  username: string;
  role: Role;
}

export function registerApi(data: RegisterReq) {
  return http.post<UserResp>("/auth/register", data);
}

export function loginApi(data: LoginReq) {
  return http.post<UserResp>("/auth/login", data);
}

