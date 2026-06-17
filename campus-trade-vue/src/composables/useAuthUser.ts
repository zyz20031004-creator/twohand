import { ElMessage } from "element-plus";
import { getCurrentUserId, getCurrentUserRole, getStoredUser, isLoggedIn } from "@/utils/auth";

export function useAuthUser() {
  function getUser() {
    return getStoredUser();
  }

  function getUserId() {
    return getCurrentUserId();
  }

  function getRole() {
    return getCurrentUserRole();
  }

  function hasLogin() {
    return isLoggedIn();
  }

  function requireLogin(message = "请先登录"): number | null {
    const id = getCurrentUserId();
    if (!id) {
      ElMessage.warning(message);
      return null;
    }
    return id;
  }

  return {
    getUser,
    getUserId,
    getRole,
    hasLogin,
    requireLogin,
  };
}
