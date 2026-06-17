export type NormalizedApiError = {
  code?: number;
  message: string;
  raw: unknown;
};

// 特殊错误码：需要重新登录
export const RELOGIN_ERROR_CODE = 4010;

function isAuthMessage(message: unknown): boolean {
  if (typeof message !== "string") return false;
  const text = message.trim();
  return text.includes("请先登录") || text.includes("登录失效") || text.includes("重新登录");
}

function pickCode(v: unknown): number | undefined {
  const n = Number(v);
  if (Number.isFinite(n)) return n;
  return undefined;
}

export function normalizeApiError(error: unknown, fallbackMessage = "请求失败"): NormalizedApiError {
  const e = error as any;
  const rawCode =
    pickCode(e?.code) ??
    pickCode(e?.status) ??
    pickCode(e?.response?.status) ??
    pickCode(e?.response?.data?.code);

  const message =
    e?.msg ||
    e?.message ||
    e?.response?.data?.msg ||
    e?.response?.data?.message ||
    fallbackMessage;

  const finalMessage = typeof message === "string" && message.trim() ? message : fallbackMessage;
  
  // 处理需要重新登录的情况：code=401（密码修改后）或被识别为认证消息
  let code = rawCode;
  if (code === 401 || isAuthMessage(finalMessage)) {
    code = RELOGIN_ERROR_CODE;
  }

  return {
    code,
    message: finalMessage,
    raw: error,
  };
}

export function getApiErrorMessage(error: unknown, fallbackMessage = "操作失败"): string {
  return normalizeApiError(error, fallbackMessage).message;
}

/**
 * 判断错误是否表示需要重新登录
 */
export function isReloginError(error: unknown): boolean {
  const normalized = normalizeApiError(error);
  return normalized.code === RELOGIN_ERROR_CODE;
}
