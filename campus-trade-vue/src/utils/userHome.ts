import type { Router } from "vue-router";
import { getCurrentUserId } from "@/utils/auth";

export function normalizeUserHomeId(value: unknown) {
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : null;
}

export function resolveUserHomePath(userId: unknown) {
  const normalizedId = normalizeUserHomeId(userId);
  if (!normalizedId) return null;

  const currentUserId = getCurrentUserId();
  if (currentUserId && currentUserId === normalizedId) {
    return "/user/mine/home";
  }
  return `/user/home/${normalizedId}`;
}

export function goToUserHome(router: Router, userId: unknown) {
  const targetPath = resolveUserHomePath(userId);
  if (!targetPath) return Promise.resolve();
  return router.push(targetPath);
}
