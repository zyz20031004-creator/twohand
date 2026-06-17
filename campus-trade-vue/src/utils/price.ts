const YUAN_SYMBOL = "\u00a5";
const PRICE_UNIT_PATTERN = /(?:\u65e5\u5143|\u4eba\u6c11\u5e01|\u5143|\uffe5|\u00a5|\u5186)/g;
const CURRENCY_CODE_PATTERN = new RegExp(`${["J", "P", "Y"].join("")}|${["C", "N", "Y"].join("")}`, "gi");

export function normalizePriceNumber(value: unknown) {
  if (value === null || value === undefined || value === "") return 0;

  const raw = String(value)
    .replace(PRICE_UNIT_PATTERN, "")
    .replace(CURRENCY_CODE_PATTERN, "")
    .replace(/[^\d.-]/g, "")
    .trim();
  const num = Number(raw);

  return Number.isFinite(num) ? num : 0;
}

export function formatPrice(value: unknown): string {
  const num = normalizePriceNumber(value);
  return Number.isInteger(num) ? `${YUAN_SYMBOL}${num}` : `${YUAN_SYMBOL}${num.toFixed(2)}`;
}

export function formatPriceInput(value: unknown): string {
  const num = normalizePriceNumber(value);
  return Number.isInteger(num) ? String(num) : num.toFixed(2);
}
