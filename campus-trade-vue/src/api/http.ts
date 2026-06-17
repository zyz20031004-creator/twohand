import axios from "axios";
import { ElMessage } from "element-plus";
import { clearStoredUser, getStoredUser } from "@/utils/auth";
import { normalizeApiError, RELOGIN_ERROR_CODE } from "@/utils/apiError";

const JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
const JSON_ACCEPT = "application/json;charset=UTF-8";

const http = axios.create({
  baseURL: "/api",
  timeout: 10000,
  withCredentials: true,
  headers: {
    Accept: JSON_ACCEPT,
    "Content-Type": JSON_CONTENT_TYPE,
  },
});

export { http };

let redirectingToAuth = false;

function redirectToAuth() {
  clearStoredUser();
  if (typeof window === "undefined") return;
  if (redirectingToAuth || window.location.pathname === "/auth") return;
  redirectingToAuth = true;
  window.location.replace("/auth");
}

http.interceptors.request.use((config) => {
  const user = getStoredUser();
  const isFormDataRequest = typeof FormData !== "undefined" && config.data instanceof FormData;

  config.headers = config.headers || {};
  config.headers.Accept = config.headers.Accept || JSON_ACCEPT;
  if (!isFormDataRequest) {
    config.headers["Content-Type"] = config.headers["Content-Type"] || JSON_CONTENT_TYPE;
  }
  if (user?.id) {
    config.headers["X-User-Id"] = user.id;
  }

  if (config.params) {
    config.params = Object.fromEntries(
      Object.entries(config.params).filter(([_, value]) => value !== undefined)
    );
  }

  return config;
});

http.interceptors.response.use(
  (resp) => {
    const res = resp.data;

    if (!res || typeof res !== "object") {
      return res;
    }
    if (!("code" in res)) {
      return res;
    }

    if (res.code === 0 || res.code === 200) {
      return res.data;
    }

    const normalized = normalizeApiError(res);
    // 处理需要重新登录的情况：code=401（密码修改后）或 code=4001（未登录/会话失效）
    if (normalized.code === RELOGIN_ERROR_CODE) {
      redirectToAuth();
      // 不显示错误提示，因为 redirectToAuth 会清除存储并跳转
      // 返回一个特殊的 rejected promise，调用方可以自行处理
      return Promise.reject(normalized);
    }
    ElMessage.error(normalized.message);
    return Promise.reject(normalized);
  },
  (err) => {
    const normalized = normalizeApiError(err, "网络错误");
    if (normalized.code === RELOGIN_ERROR_CODE) redirectToAuth();
    else ElMessage.error(normalized.message);
    return Promise.reject(normalized);
  }
);

declare module "axios" {
  export interface AxiosInstance {
    get<T = any>(url: string, config?: any): Promise<T>;
    post<T = any>(url: string, data?: any, config?: any): Promise<T>;
    put<T = any>(url: string, data?: any, config?: any): Promise<T>;
    delete<T = any>(url: string, config?: any): Promise<T>;
  }
}

