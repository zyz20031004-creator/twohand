// src/utils/img.ts
// Centralized static resource URL handling for /upload/** style paths.
// If no env base is provided, keep the path relative so Vite proxy or same-origin deployment can handle it.

const RAW_API_BASE = String(import.meta.env.VITE_API_BASE_URL || "").trim();
const API_BASE = RAW_API_BASE.replace(/\/$/, "");
const SERVER_BASE = API_BASE.replace(/\/api\/?$/, "");
const PRODUCT_PLACEHOLDER_PATH = "/upload/product/product_placeholder.svg";
export const DEFAULT_USER_AVATAR =
  "data:image/svg+xml;utf8," +
  "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 64 64'>" +
  "<circle cx='32' cy='32' r='31' fill='%23f3f4f6' stroke='%23d1d5db'/>" +
  "<circle cx='32' cy='24' r='10' fill='%239ca3af'/>" +
  "<path d='M16 52c0-8.8 7.2-16 16-16s16 7.2 16 16' fill='%239ca3af'/>" +
  "</svg>";

const BLOCKED_PRODUCT_IMAGE_MARKERS = [
  "1772470139664_iphone1.png",
  "1772470150961_iphone2.png",
  "1772470223564_iphone1.png",
  "1772541103200_iphone2.png",
  "1772608234000_book1.png",
  "1772608241751_1772470139664_iphone1.png",
  "1772610361936_1772608241751_1772470139664_iphone1.png",
  "1772610646218_iphone2.png",
  "1772612730195_1772608234000_book1.png",
  "7rhgx5ux5fljh82e.jpg",
  "b1867326e1ec34eba04d7b407d6ec2ddee609171a6feec3ecd32fc87d016c0b4.jpg",
  "1688521133714449.png",
  "r.jpg",
  "r (1).jpg",
];

function normalizeRelativePath(url: string) {
  return url.startsWith("/") ? url : `/${url}`;
}

export function imgUrl(url?: string) {
  const text = typeof url === "string" ? url.trim() : "";
  if (!text) return "";
  if (/^(https?:|data:|blob:)/i.test(text)) return text;
  const path = normalizeRelativePath(text);
  return SERVER_BASE ? `${SERVER_BASE}${path}` : path;
}

export function userAvatarUrl(url?: string) {
  return imgUrl(url) || DEFAULT_USER_AVATAR;
}

export function sanitizeProductImageUrl(url?: string) {
  if (!url) return "";
  const text = String(url).trim();
  if (!text) return "";
  const lower = text.toLowerCase();
  if (/^(blob:|data:|file:)/i.test(lower)) return "";
  if (BLOCKED_PRODUCT_IMAGE_MARKERS.some((marker) => lower.includes(marker))) return "";
  return text.startsWith("upload/") ? `/${text}` : text;
}

export function productPlaceholderUrl() {
  return imgUrl(PRODUCT_PLACEHOLDER_PATH);
}

export function productImgUrl(url?: string) {
  const sanitized = sanitizeProductImageUrl(url);
  return imgUrl(sanitized || PRODUCT_PLACEHOLDER_PATH);
}
