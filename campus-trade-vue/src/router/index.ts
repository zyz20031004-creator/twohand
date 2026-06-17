import { createRouter, createWebHistory } from "vue-router";
import {
  clearStoredUser,
  getCurrentUserRole,
  getStoredUser,
  isAuthenticated,
  isCurrentSuperAdmin,
  setStoredUser,
} from "@/utils/auth";

const AUTH_PATH = "/auth";
const ADMIN_HOME = "/admin";
const USER_HOME = "/user";

// 导出这些变量，允许其他组件（如 AdminLayout）重置认证状态
export let sessionChecked = false;
export let pendingSessionCheck: Promise<boolean> | null = null;

/**
 * 重置 session 验证状态
 * 在密码修改成功退出登录后调用，确保路由守卫重新验证
 */
export function resetSessionState() {
  sessionChecked = false;
  pendingSessionCheck = null;
}

function isAdminRole(role: string | null) {
  return role === "ADMIN" || role === "SUPER_ADMIN";
}

async function ensureSessionValid() {
  const storedUser = getStoredUser();
  if (!storedUser?.id || !storedUser?.role) {
    sessionChecked = false;
    return false;
  }

  if (sessionChecked) {
    return true;
  }

  if (!pendingSessionCheck) {
    pendingSessionCheck = fetch("/api/user/me", {
      credentials: "include",
      headers: {
        "X-User-Id": String(storedUser.id),
      },
    })
      .then(async (response) => {
        const payload = await response.json().catch(() => null);
        const code = Number((payload as any)?.code);
        const data = (payload as any)?.data;

        if (response.ok && (code === 0 || code === 200) && data && typeof data === "object") {
          setStoredUser({
            ...storedUser,
            ...data,
          });
          sessionChecked = true;
          return true;
        }

        clearStoredUser();
        sessionChecked = false;
        return false;
      })
      .catch(() => false)
      .finally(() => {
        pendingSessionCheck = null;
      });
  }

  return pendingSessionCheck;
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/auth", name: "Auth", component: () => import("../views/Auth.vue") },
    {
      path: "/user",
      name: "UserLayout",
      component: () => import("../layouts/UserLayout.vue"),
      children: [
        { path: "", redirect: "/user/hot" },
        { path: "hot", component: () => import("../views/user/Hot.vue") },
        {
          path: "hot/product/:id",
          name: "ProductDetail",
          component: () => import("../views/user/product/ProductDetail.vue"),
        },
        {
          path: "home/:id",
          name: "UserPublicCenter",
          component: () => import("../views/user/UserPublicCenter.vue"),
        },
        {
          path: "order/confirm/:productId",
          name: "ConfirmOrder",
          component: () => import("../views/user/order/ConfirmOrder.vue"),
        },
        { path: "wanted", component: () => import("../views/user/Wanted.vue") },
        { path: "notice", component: () => import("../views/user/Notice.vue") },
        { path: "feedback", component: () => import("../views/user/Feedback.vue") },
        { path: "chat", component: () => import("../views/user/Chat.vue") },
        {
          path: "mine",
          component: () => import("@/views/user/profile/Mine.vue"),
          children: [
            { path: "", redirect: "/user/mine/home" },
            { path: "home", component: () => import("@/views/user/profile/MineHome.vue") },
            { path: "profile", component: () => import("@/views/user/profile/Profile.vue") },
            { path: "bought", component: () => import("@/views/user/profile/MyBought.vue") },
            { path: "sold", component: () => import("@/views/user/profile/MySold.vue") },
            { path: "products", component: () => import("@/views/user/profile/MyProducts.vue") },
            { path: "mywanted", component: () => import("@/views/user/profile/MyWanted.vue") },
            { path: "address", component: () => import("@/views/user/profile/MyAddress.vue") },
            { path: "fav", component: () => import("@/views/user/profile/MyFavorites.vue") },
            { path: "myfeedback", component: () => import("@/views/user/profile/MyFeedback.vue") },
            { path: "myreport", component: () => import("@/views/user/profile/MyReport.vue") },
            { path: "verify", component: () => import("@/views/user/UserVerify.vue") },
            { path: "credit", component: () => import("@/views/user/CreditCenter.vue") },
          ],
        },
        { path: "order", alias: "orders", redirect: "/user/mine/bought" },
        { path: "profile", redirect: "/user/mine/profile" },
        { path: "bought", redirect: "/user/mine/bought" },
        { path: "sold", redirect: "/user/mine/sold" },
        { path: "products", redirect: "/user/mine/products" },
        { path: "mywanted", redirect: "/user/mine/mywanted" },
        { path: "address", redirect: "/user/mine/address" },
        { path: "fav", redirect: "/user/mine/fav" },
        { path: "myfeedback", redirect: "/user/mine/myfeedback" },
        { path: "myreport", redirect: "/user/mine/myreport" },
        { path: "verify", redirect: "/user/mine/verify" },
        { path: "credit", redirect: "/user/mine/credit" },
      ],
    },
    {
      path: "/admin",
      name: "AdminLayout",
      component: () => import("../layouts/AdminLayout.vue"),
      children: [
        { path: "", redirect: "/admin/dashboard" },
        { path: "dashboard", component: () => import("../views/admin/Dashboard.vue") },
        { path: "categories", component: () => import("../views/admin/category/CategoryManage.vue") },
        { path: "products", component: () => import("../views/admin/product/ProductManage.vue") },
        { path: "orders", component: () => import("../views/admin/order/OrderManage.vue") },
        { path: "wanted", component: () => import("../views/admin/wanted/WantedManage.vue") },
        { path: "notices", component: () => import("../views/admin/notice/NoticeManage.vue") },
        { path: "addresses", component: () => import("../views/admin/address/AddressManage.vue") },
        { path: "feedbacks", component: () => import("../views/admin/feedback/FeedbackManage.vue") },
        { path: "reports", component: () => import("../views/admin/report/ReportManage.vue") },
        { path: "admins", component: () => import("../views/admin/admin/AdminManage.vue") },
        { path: "users", component: () => import("../views/admin/user/UserManage.vue") },
        { path: "verify", component: () => import("../views/admin/verify/VerifyManage.vue") },
        { path: "credit", component: () => import("../views/admin/credit/CreditManage.vue") },
        { path: "system", component: () => import("../views/admin/system/SystemSetting.vue") },
      ],
    },
    { path: "/", redirect: AUTH_PATH },
  ],
});

router.beforeEach(async (to) => {
  const isLogin = isAuthenticated();
  const needsAuth = to.path.startsWith("/admin") || to.path.startsWith("/user");

  if (!isLogin) {
    sessionChecked = false;
    return needsAuth ? AUTH_PATH : true;
  }

  const sessionValid = await ensureSessionValid();
  if (!sessionValid) {
    return to.path === AUTH_PATH ? true : AUTH_PATH;
  }

  const role = getCurrentUserRole();

  if (to.path === AUTH_PATH) {
    return isAdminRole(role) ? ADMIN_HOME : USER_HOME;
  }

  if (to.path.startsWith("/admin") && !isAdminRole(role)) return USER_HOME;
  if (to.path.startsWith("/user") && isAdminRole(role)) return ADMIN_HOME;

  if ((to.path.startsWith("/admin/admins") || to.path.startsWith("/admin/system")) && !isCurrentSuperAdmin()) {
    return ADMIN_HOME;
  }

  if (to.path === "/") return isAdminRole(role) ? ADMIN_HOME : USER_HOME;

  return true;
});

export default router;
