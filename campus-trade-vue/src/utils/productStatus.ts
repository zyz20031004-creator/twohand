export type ProductAuditStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ProductSaleStatus = "ON" | "OFF";
export type ProductDisplayStatusKey = "PENDING" | "REJECTED" | "ON" | "OFF" | "SOLD";

export type ProductStatusSource = {
  id?: number | string;
  status?: unknown;
  saleStatus?: unknown;
  auditStatus?: unknown;
  sold?: unknown;
  isSold?: unknown;
  soldFlag?: unknown;
  orderStatus?: unknown;
};

export type ProductDisplayStatus = {
  key: ProductDisplayStatusKey;
  text: "待审" | "未通过" | "在售" | "已下架" | "已售出";
  tagType: "success" | "info" | "warning" | "danger";
  tagClass: "on" | "off" | "pending" | "rejected";
  auditStatus: ProductAuditStatus;
  saleStatus: ProductSaleStatus;
  isSold: boolean;
};

export type ProductActionKey = "view" | "edit" | "toggle" | "delete";

type ResolveProductDisplayStatusOptions = {
  soldProductIds?: Set<number>;
  isSold?: boolean;
};

const pendingAuditStatuses = new Set(["WAIT", "PENDING", "PEND"]);
const approvedAuditStatuses = new Set(["PASS", "PASSED", "APPROVED", "APPROVE", "SUCCESS"]);
const rejectedAuditStatuses = new Set([
  "REJECT",
  "REJECTED",
  "REFUSED",
  "DENIED",
  "FAILED",
  "FAIL",
  "NOT_PASS",
  "NOTPASSED",
]);
const soldOrderStatuses = new Set(["FINISHED", "COMPLETED", "DONE", "SOLD"]);

function normalizeText(value: unknown) {
  return String(value ?? "").trim().toUpperCase();
}

function readBooleanFlag(value: unknown) {
  if (typeof value === "boolean") return value;
  if (typeof value === "number") return value === 1;

  const normalized = normalizeText(value);
  return normalized === "1" || normalized === "TRUE" || normalized === "Y" || normalized === "YES";
}

export function normalizeProductAuditStatus(auditStatus: unknown): ProductAuditStatus {
  const normalized = normalizeText(auditStatus);

  if (approvedAuditStatuses.has(normalized)) return "APPROVED";
  if (rejectedAuditStatuses.has(normalized)) return "REJECTED";
  if (pendingAuditStatuses.has(normalized)) return "PENDING";
  return "PENDING";
}

export function normalizeProductSaleStatus(status: unknown, saleStatus?: unknown): ProductSaleStatus {
  const normalized = normalizeText(saleStatus ?? status);
  return normalized === "ON" ? "ON" : "OFF";
}

export function resolveProductIsSold(
  product: ProductStatusSource,
  options: ResolveProductDisplayStatusOptions = {}
) {
  if (typeof options.isSold === "boolean") return options.isSold;

  const productId = Number(product.id);
  if (Number.isFinite(productId) && options.soldProductIds?.has(productId)) {
    return true;
  }

  if (readBooleanFlag(product.isSold ?? product.sold ?? product.soldFlag)) {
    return true;
  }

  return soldOrderStatuses.has(normalizeText(product.orderStatus));
}

export function getProductDisplayStatus(
  product: ProductStatusSource,
  options: ResolveProductDisplayStatusOptions = {}
): ProductDisplayStatus {
  const auditStatus = normalizeProductAuditStatus(product.auditStatus);
  const saleStatus = normalizeProductSaleStatus(product.status, product.saleStatus);
  const isSold = auditStatus === "APPROVED" && resolveProductIsSold(product, options);

  if (isSold) {
    return {
      key: "SOLD",
      text: "已售出",
      tagType: "danger",
      tagClass: "off",
      auditStatus,
      saleStatus,
      isSold,
    };
  }

  if (auditStatus === "PENDING") {
    return {
      key: "PENDING",
      text: "待审",
      tagType: "warning",
      tagClass: "pending",
      auditStatus,
      saleStatus,
      isSold,
    };
  }

  if (auditStatus === "REJECTED") {
    return {
      key: "REJECTED",
      text: "未通过",
      tagType: "danger",
      tagClass: "rejected",
      auditStatus,
      saleStatus,
      isSold,
    };
  }

  if (saleStatus === "ON") {
    return {
      key: "ON",
      text: "在售",
      tagType: "success",
      tagClass: "on",
      auditStatus,
      saleStatus,
      isSold,
    };
  }

  return {
    key: "OFF",
    text: "已下架",
    tagType: "info",
    tagClass: "off",
    auditStatus,
    saleStatus,
    isSold,
  };
}

export function getProductActionKeys(
  status: ProductDisplayStatus | ProductDisplayStatusKey
): ProductActionKey[] {
  const key = typeof status === "string" ? status : status.key;
  if (key === "ON" || key === "OFF") {
    return ["view", "edit", "toggle", "delete"];
  }
  if (key === "SOLD") {
    return ["view", "delete"];
  }
  return ["view", "edit", "delete"];
}
