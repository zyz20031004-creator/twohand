export type TradeMethod = "MEETUP" | "DELIVERY" | "NEGOTIATE";

export const TRADE_METHOD_OPTIONS: Array<{ label: string; value: TradeMethod }> = [
  { label: "校内当面交易", value: "MEETUP" },
  { label: "送货上门", value: "DELIVERY" },
  { label: "双方协商", value: "NEGOTIATE" },
];

export const MEETUP_LOCATION_OPTIONS = [
  "图书馆门口",
  "食堂门口",
  "教学楼附近",
  "宿舍区楼下",
  "操场附近",
  "校内其他地点",
];

export const DELIVERY_ADDRESS_TEXT = "送货上门";
export const DELIVERY_LOCATION_TEXT = "买家下单后选择收货地址";
export const NEGOTIATE_ADDRESS_TEXT = "双方协商";
export const NEGOTIATE_LOCATION_TEXT = "聊天中确认";

const PHONE_PATTERN = /1[3-9]\d{9}/;

export function cleanLegacyTradeLocation(value?: string | null) {
  return String(value || "")
    .trim()
    .replace(/^历史地址[:：]\s*/, "")
    .trim();
}

export function containsPhone(value?: string | null) {
  return PHONE_PATTERN.test(String(value || ""));
}

export function looksLikeAddressBookText(value?: string | null) {
  const text = String(value || "");
  const pipeCount = (text.match(/[|｜]/g) || []).length;
  return containsPhone(text) || (pipeCount >= 2 && /默认地址|收货人|手机号|电话|宿舍|楼/.test(text));
}

export function inferTradeMethod(value?: string | null): TradeMethod {
  const text = cleanLegacyTradeLocation(value);
  if (text === DELIVERY_ADDRESS_TEXT || text === DELIVERY_LOCATION_TEXT || text.includes("送货上门")) {
    return "DELIVERY";
  }
  if (text === NEGOTIATE_ADDRESS_TEXT || text === NEGOTIATE_LOCATION_TEXT || text.includes("协商") || text.includes("聊天")) {
    return "NEGOTIATE";
  }
  return "MEETUP";
}

export function inferMeetupLocation(value?: string | null) {
  const text = cleanLegacyTradeLocation(value);
  if (MEETUP_LOCATION_OPTIONS.includes(text)) return text;
  if (looksLikeAddressBookText(text) && /宿舍|楼栋|寝室/.test(text)) return "宿舍区楼下";
  return "";
}

export function buildAddressText(method: TradeMethod, meetupLocation = "") {
  if (method === "DELIVERY") return DELIVERY_ADDRESS_TEXT;
  if (method === "NEGOTIATE") return NEGOTIATE_ADDRESS_TEXT;
  return meetupLocation.trim();
}

export function getPublicTradeBadge(value?: string | null) {
  const method = inferTradeMethod(value);
  if (method === "DELIVERY") return "支持送货上门";
  if (method === "NEGOTIATE") return "双方协商";
  return "校内当面交易";
}

export function getTradeDetail(value?: string | null) {
  const method = inferTradeMethod(value);
  if (method === "DELIVERY") {
    return {
      methodText: "送货上门",
      locationText: DELIVERY_LOCATION_TEXT,
      description: "下单后可选择收货地址，具体时间可通过站内聊天确认。",
    };
  }
  if (method === "NEGOTIATE") {
    return {
      methodText: "双方协商",
      locationText: NEGOTIATE_LOCATION_TEXT,
      description: "请通过站内聊天与卖家确认具体交易地点。",
    };
  }

  const locationText = inferMeetupLocation(value) || "校内当面交易";
  return {
    methodText: "校内当面交易",
    locationText,
    description: "",
  };
}

export function validateSafeProductAddressText(value?: string | null) {
  const text = String(value || "").trim();
  if (!text) return "请选择交易方式";
  if (text.length > 100) return "交易地点不能超过 100 个字符";
  if (/^历史地址[:：]/.test(text)) return "交易地点不能包含历史地址前缀";
  if (containsPhone(text)) return "交易地点不能包含手机号";
  if (looksLikeAddressBookText(text)) return "交易地点不能保存联系人和手机号等完整地址簿信息";
  return "";
}
