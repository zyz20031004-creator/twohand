import { DEFAULT_USER_AVATAR, imgUrl as resolveImgUrl } from "@/utils/img";

export type CommentUserLike = {
  userId?: number | null;
  nickname?: string | null;
  nickName?: string | null;
  name?: string | null;
  username?: string | null;
  userName?: string | null;
  avatar?: string | null;
  avatarUrl?: string | null;
  avatar_url?: string | null;
};

export const DEFAULT_COMMENT_AVATAR = DEFAULT_USER_AVATAR;

export function normalizeDisplayText(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}

export function normalizePositiveNumber(value: unknown) {
  const numeric = Number(value);
  return Number.isFinite(numeric) && numeric > 0 ? numeric : 0;
}

export function getCommentUserName(user?: CommentUserLike | null) {
  const candidates = [
    user?.nickname,
    user?.nickName,
    user?.name,
  ].map(normalizeDisplayText);
  const resolved = candidates.find(Boolean);
  if (resolved) return resolved;
  return "校园用户";
}

export function getCommentUserAvatar(user?: CommentUserLike | null) {
  const avatar = [
    user?.avatar,
    user?.avatarUrl,
    user?.avatar_url,
  ].map(normalizeDisplayText).find(Boolean);
  return avatar ? resolveImgUrl(avatar) || DEFAULT_COMMENT_AVATAR : DEFAULT_COMMENT_AVATAR;
}

export function formatCommentTime(value?: unknown) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}

export function handleCommentAvatarError(event: Event) {
  const target = event.target as HTMLImageElement | null;
  if (!target) return;
  if (target.dataset.fallbackApplied === "true") return;
  target.dataset.fallbackApplied = "true";
  target.src = DEFAULT_COMMENT_AVATAR;
}
