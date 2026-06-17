import { getStoredUser, mergeStoredUser, type StoredUser } from "@/utils/auth";

export const USER_PROFILE_UPDATED_EVENT = "user-profile-updated";

type SavedUserProfilePatch = Partial<
  Pick<StoredUser, "id" | "role"> & {
    username: string;
    name: string;
    avatar: string;
    phone: string;
    email: string;
  }
>;

function normalizeText(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}

function pickUserProfilePatch(source: any): SavedUserProfilePatch {
  const patch: SavedUserProfilePatch = {};
  if (!source || typeof source !== "object") return patch;

  if (source.id !== undefined && source.id !== null && source.id !== "") {
    patch.id = source.id as StoredUser["id"];
  }
  if (typeof source.role === "string" && source.role.trim()) patch.role = source.role;
  if (typeof source.username === "string") patch.username = source.username;
  if (typeof source.name === "string") patch.name = source.name;
  if (typeof source.avatar === "string") patch.avatar = normalizeText(source.avatar);
  if (typeof source.phone === "string") patch.phone = source.phone;
  if (typeof source.email === "string") patch.email = source.email;

  return patch;
}

export function syncSavedUserProfile(source: any, options: { emit?: boolean } = {}) {
  const patch = pickUserProfilePatch(source);
  if (Object.keys(patch).length === 0) {
    return getStoredUser();
  }

  mergeStoredUser(patch);
  const nextUser = getStoredUser();

  if (options.emit !== false && typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(USER_PROFILE_UPDATED_EVENT, { detail: nextUser }));
  }

  return nextUser;
}
