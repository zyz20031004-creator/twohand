export type UserRole = "USER" | "ADMIN" | string;

export type StoredUser = {
  id?: number | string;
  role?: UserRole;
  [key: string]: unknown;
};

const USER_KEY = "user";

function readRawUser(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(USER_KEY);
}

function parseUser(raw: string | null): StoredUser | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed === "object") {
      return parsed as StoredUser;
    }
  } catch {
    // ignore parse error
  }
  return null;
}

export function getStoredUser(): StoredUser | null {
  const raw = readRawUser();
  const user = parseUser(raw);
  if (!user && raw && typeof window !== "undefined") {
    window.localStorage.removeItem(USER_KEY);
  }
  return user;
}

export function clearStoredUser() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(USER_KEY);
}

export function setStoredUser(user: StoredUser | null) {
  if (typeof window === "undefined") return;
  if (!user) {
    clearStoredUser();
    return;
  }
  window.localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function mergeStoredUser(patch: Partial<StoredUser>) {
  const current = getStoredUser() || {};
  setStoredUser({
    ...current,
    ...patch,
  });
}

export function getCurrentUserId(): number | null {
  const user = getStoredUser();
  const id = Number(user?.id);
  return Number.isFinite(id) && id > 0 ? id : null;
}

export function getCurrentUserRole(): UserRole | null {
  const role = getStoredUser()?.role;
  if (typeof role !== "string" || !role.trim()) return null;
  return role;
}

export function isCurrentSuperAdmin(): boolean {
  const user = getStoredUser();
  return user?.isSuperAdmin === true || String(user?.username || "").toLowerCase() === "admin";
}

export function isLoggedIn(): boolean {
  return getCurrentUserId() !== null;
}

export function isAuthenticated(): boolean {
  return isLoggedIn() && getCurrentUserRole() !== null;
}
