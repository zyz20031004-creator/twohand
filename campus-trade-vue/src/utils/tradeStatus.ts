export type StatusTone = "primary" | "success" | "warning" | "danger" | "info";
export type OrderStatusKey = "UNPAID" | "PAID" | "FINISHED" | "CANCELLED" | "UNKNOWN";
export type OrderActionKey = "pay" | "finish" | "detail" | "contact" | "cancel" | "remove";

export type OrderStatusMeta = {
  key: OrderStatusKey;
  text: string;
  tone: StatusTone;
  className: string;
};

const ORDER_STATUS_META: Record<Exclude<OrderStatusKey, "UNKNOWN">, OrderStatusMeta> = {
  UNPAID: {
    key: "UNPAID",
    text: "待支付",
    tone: "warning",
    className: "is-unpaid",
  },
  PAID: {
    key: "PAID",
    text: "已支付",
    tone: "primary",
    className: "is-paid",
  },
  FINISHED: {
    key: "FINISHED",
    text: "已完成",
    tone: "success",
    className: "is-finished",
  },
  CANCELLED: {
    key: "CANCELLED",
    text: "已取消",
    tone: "info",
    className: "is-cancelled",
  },
};

function normalizeOrderStatus(status: unknown): OrderStatusKey {
  const normalized = String(status ?? "").trim().toUpperCase();
  if (normalized === "UNPAID") return "UNPAID";
  if (normalized === "PAID") return "PAID";
  if (normalized === "FINISHED") return "FINISHED";
  if (normalized === "CANCELLED") return "CANCELLED";
  return "UNKNOWN";
}

export function getOrderStatusMeta(status: unknown): OrderStatusMeta {
  const key = normalizeOrderStatus(status);
  if (key !== "UNKNOWN") {
    return ORDER_STATUS_META[key];
  }
  return {
    key,
    text: String(status ?? "-").trim() || "-",
    tone: "info",
    className: "is-default",
  };
}

export function getOrderStatusText(status: unknown) {
  return getOrderStatusMeta(status).text;
}

export function getBoughtOrderActionKeys(status: unknown, isExpired = false): OrderActionKey[] {
  const meta = getOrderStatusMeta(status);
  if (meta.key === "UNPAID" && !isExpired) {
    return ["pay", "detail", "contact", "cancel"];
  }
  if (meta.key === "PAID") {
    return ["finish", "detail", "contact"];
  }
  if (meta.key === "FINISHED" || meta.key === "CANCELLED") {
    return ["detail", "contact", "remove"];
  }
  return ["detail", "contact"];
}

export function getSoldOrderActionKeys(status: unknown): OrderActionKey[] {
  const meta = getOrderStatusMeta(status);
  if (meta.key === "FINISHED" || meta.key === "CANCELLED") {
    return ["detail", "contact", "remove"];
  }
  return ["detail", "contact"];
}

export function getCountdownTone(kind: "payment" | "expired"): StatusTone {
  return kind === "payment" ? "warning" : "danger";
}
