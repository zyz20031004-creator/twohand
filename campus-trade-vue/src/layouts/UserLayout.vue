<template>
  <div class="page">
    <header class="nav">
      <div class="nav-shell">
        <div class="nav-left">
          <button type="button" class="brand" @click="go('/user/hot')">
            <span class="brand-mark">
              <el-icon><Shop /></el-icon>
            </span>
            <span class="brand-copy">
              <strong class="logo">校园二手</strong>
              <span class="logo-sub">校园二手交易平台</span>
            </span>
          </button>
        </div>

        <div class="nav-center">
          <el-menu mode="horizontal" :ellipsis="false" class="menu" :default-active="activeIndex">
            <el-menu-item index="hot" @click="go('/user/hot')">
              <el-icon><Goods /></el-icon>
              <span>热卖专区</span>
            </el-menu-item>
            <el-menu-item index="wanted" @click="go('/user/wanted')">
              <el-icon><Promotion /></el-icon>
              <span>求购专区</span>
            </el-menu-item>
            <el-menu-item index="notice" @click="go('/user/notice')">
              <el-icon><Bell /></el-icon>
              <span>公告通知</span>
            </el-menu-item>
            <el-menu-item index="feedback" @click="go('/user/feedback')">
              <el-icon><EditPen /></el-icon>
              <span>留言反馈</span>
            </el-menu-item>
          </el-menu>
        </div>

        <div class="nav-right">
          <div class="nav-actions">
            <div class="nav-quick-actions">
              <el-button class="chat-btn" @click="go('/user/chat')">
                <el-icon><ChatDotRound /></el-icon>
                <span>聊天消息</span>
              </el-button>
            </div>

            <div class="nav-profile">
              <el-dropdown trigger="hover" popper-class="user-dropdown-popper">
                <div
                  class="user"
                  role="button"
                  tabindex="0"
                  aria-label="进入个人中心"
                  @click="go('mine')"
                  @keydown.enter.prevent="go('mine')"
                  @keydown.space.prevent="go('mine')"
                >
                  <el-avatar :size="42" class="avatar" :src="avatarSrc">{{ avatarText }}</el-avatar>
                  <div class="user-meta">
                    <span class="user-name">{{ currentUser.name || currentUser.username || "用户" }}</span>
                    <span class="user-sub">校园好物买家</span>
                  </div>
                </div>

                <template #dropdown>
                  <div class="dropdown-card">
                    <div class="dropdown-top">
                      <el-avatar :size="44" class="avatar" :src="avatarSrc">{{ avatarText }}</el-avatar>
                      <div class="dropdown-user">
                        <strong>{{ currentUser.name || currentUser.username || "校园用户" }}</strong>
                        <span>进入个人中心管理交易与收藏</span>
                      </div>
                    </div>

                    <div class="dropdown-section">
                      <button type="button" class="menu-link" @click="go('bought')">
                        <span class="menu-label">我买到的</span>
                        <span class="arrow">→</span>
                      </button>

                      <button type="button" class="menu-link" @click="go('sold')">
                        <span class="menu-label">我卖出的</span>
                        <span class="arrow">→</span>
                      </button>

                      <button type="button" class="menu-link" @click="go('fav')">
                        <span class="menu-label">我的收藏</span>
                        <span class="arrow">→</span>
                      </button>
                    </div>

                    <div class="dropdown-divider"></div>

                    <button type="button" class="logout-link" @click="logout">退出登录</button>
                  </div>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <aside class="quick-publish" aria-label="快捷发布">
      <div class="quick-publish-head">
        <span class="quick-publish-eyebrow">常用入口</span>
        <strong class="quick-publish-title">快捷发布</strong>
      </div>

      <div class="quick-publish-list">
        <button
          v-for="item in quickPublishActions"
          :key="item.path"
          type="button"
          class="quick-publish-item"
          @click="openQuickPublish(item.path)"
        >
          <span class="quick-publish-item__icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span class="quick-publish-item__content">
            <span class="quick-publish-item__hint">{{ item.hint }}</span>
            <span class="quick-publish-item__title">{{ item.title }}</span>
          </span>
        </button>
      </div>
    </aside>

    <footer class="footer">
      <div class="footer-inner">校园二手 · 校园好物更高效流转</div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import type { Component } from "vue";
import { computed, onBeforeUnmount, onMounted, reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Bell, ChatDotRound, EditPen, Goods, Promotion, Search, Shop } from "@element-plus/icons-vue";
import { apiGetMe } from "@/api/user";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { clearStoredUser, getStoredUser } from "@/utils/auth";
import { userAvatarUrl } from "@/utils/img";
import { syncSavedUserProfile, USER_PROFILE_UPDATED_EVENT } from "@/utils/userProfile";

const route = useRoute();
const router = useRouter();
const { runConfirmAction } = useConfirmAction();

const currentUser = reactive({
  username: "",
  name: "",
  avatar: "",
});

const minePathMap: Record<string, string> = {
  mine: "mine/home",
  profile: "mine/profile",
  bought: "mine/bought",
  sold: "mine/sold",
  products: "mine/products",
  mywanted: "mine/mywanted",
  address: "mine/address",
  fav: "mine/fav",
  myfeedback: "mine/myfeedback",
  verify: "mine/verify",
  credit: "mine/credit",
};

type QuickPublishAction = {
  title: string;
  hint: string;
  path: string;
  icon: Component;
};

const quickPublishActions: QuickPublishAction[] = [
  { title: "发布商品", hint: "上新闲置", path: "/user/mine/products", icon: Goods },
  { title: "发布求购", hint: "发布需求", path: "/user/mine/mywanted", icon: Search },
];

function go(path: string) {
  if (!path.startsWith("/")) {
    const normalized = minePathMap[path] || path;
    path = "/user/" + normalized;
  }
  router.push(path);
}

function openQuickPublish(path: string) {
  router.push({
    path,
    query: {
      action: "create",
    },
  });
}

const activeIndex = computed(() => {
  const path = route.path;
  if (path.includes("/user/hot")) return "hot";
  if (path.includes("/user/wanted")) return "wanted";
  if (path.includes("/user/notice")) return "notice";
  if (path.includes("/user/feedback")) return "feedback";
  return "hot";
});

const avatarText = computed(() => {
  return (currentUser.name || currentUser.username || "用").slice(0, 1).toUpperCase();
});

const avatarSrc = computed(() => {
  return userAvatarUrl(currentUser.avatar);
});

function applyUserInfo(source: any) {
  currentUser.username = typeof source?.username === "string" ? source.username : "";
  currentUser.name = typeof source?.name === "string" ? source.name : "";
  currentUser.avatar = typeof source?.avatar === "string" ? source.avatar : "";
}

function readStoredUser() {
  return getStoredUser();
}

async function loadCurrentUser() {
  try {
    const profile = await apiGetMe();
    applyUserInfo(profile);
    syncSavedUserProfile(profile, { emit: false });
  } catch {
    const stored = readStoredUser();
    if (stored) applyUserInfo(stored);
  }
}

function handleProfileUpdated(event: Event) {
  const detail = (event as CustomEvent).detail;
  if (detail) applyUserInfo(detail);
}

async function logout() {
  await runConfirmAction({
    title: "退出确认",
    message: "确认退出当前账号吗？",
    type: "warning",
    confirmButtonText: "退出",
    cancelButtonText: "取消",
    action: async () => {
      clearStoredUser();
      await router.push("/auth");
    },
    showError: false,
  });
}

onMounted(() => {
  const stored = readStoredUser();
  if (stored) applyUserInfo(stored);
  loadCurrentUser();
  window.addEventListener(USER_PROFILE_UPDATED_EVENT, handleProfileUpdated as EventListener);
});

onBeforeUnmount(() => {
  window.removeEventListener(USER_PROFILE_UPDATED_EVENT, handleProfileUpdated as EventListener);
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top center, rgba(219, 234, 254, 0.46) 0, rgba(219, 234, 254, 0) 34%),
    linear-gradient(180deg, #f6f9fd 0%, #eef3f8 100%);
  color: #1f2937;
}

.nav {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 16px 24px 0;
  background: linear-gradient(180deg, rgba(246, 249, 253, 0.95) 0%, rgba(246, 249, 253, 0.7) 100%);
  backdrop-filter: blur(10px);
}

.nav-shell {
  max-width: 1280px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 220px) minmax(0, 1fr) minmax(0, 318px);
  align-items: center;
  gap: 22px;
  padding: 12px 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 12px 34px rgba(15, 23, 42, 0.06);
}

.nav-left,
.nav-right {
  display: flex;
  align-items: center;
  min-width: 0;
}

.nav-center {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, #3b82f6 0%, #22c55e 100%);
  color: #fff;
  font-size: 24px;
  box-shadow: 0 14px 28px rgba(59, 130, 246, 0.22);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.logo {
  font-size: 18px;
  line-height: 1.1;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: #0f172a;
}

.logo-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.menu {
  display: flex;
  align-items: center;
  justify-content: center;
  width: auto;
  max-width: 100%;
  min-width: 0;
  flex-wrap: nowrap;
  border-bottom: none !important;
  background: transparent;
  gap: 2px;
}

.menu :deep(.el-menu) {
  border-bottom: none !important;
}

.menu :deep(.el-menu-item) {
  min-width: 0;
  height: 44px;
  margin: 0;
  padding: 0 14px;
  border-radius: 14px;
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
  transition: background-color 0.22s ease, color 0.22s ease, transform 0.22s ease;
}

.menu :deep(.el-menu-item .el-icon) {
  margin-right: 6px;
  font-size: 16px;
}

.menu :deep(.el-menu-item span) {
  white-space: nowrap;
}

.menu :deep(.el-menu-item:hover) {
  color: #2563eb;
  background: rgba(59, 130, 246, 0.08);
  transform: translateY(-1px);
}

.menu :deep(.el-menu-item.is-active) {
  color: #1d4ed8;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.14), rgba(34, 197, 94, 0.12));
}

.menu :deep(.el-menu-item.is-active::after) {
  display: none;
}

.nav-right {
  justify-content: flex-end;
}

.nav-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 18px;
  width: auto;
  max-width: 100%;
  min-width: 0;
}

.nav-quick-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.nav-profile {
  display: flex;
  align-items: center;
  min-width: 0;
}

.chat-btn {
  height: 42px;
  padding: 0 14px;
  border: 1px solid #dbe6f2;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  color: #334155;
  font-weight: 600;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.chat-btn:hover {
  transform: translateY(-1px);
  border-color: #bfdbfe;
  box-shadow: 0 10px 22px rgba(59, 130, 246, 0.12);
}

.chat-btn .el-icon {
  margin-right: 6px;
}

.chat-btn span {
  white-space: nowrap;
}

.user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 6px 14px 6px 6px;
  border-radius: 18px;
  border: 1px solid #dbe6f2;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.user:hover {
  transform: translateY(-1px);
  border-color: #bfdbfe;
  box-shadow: 0 10px 22px rgba(59, 130, 246, 0.12);
}

.avatar {
  border: 1px solid rgba(191, 219, 254, 0.9);
  background: linear-gradient(180deg, #ffffff 0%, #eaf2ff 100%);
  color: #0f172a;
  font-weight: 700;
}

.user-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.user-sub {
  margin-top: 2px;
  font-size: 11px;
  color: #64748b;
}

.dropdown-card {
  width: 240px;
  padding: 12px;
  border-radius: 22px;
  border: 1px solid #e7eef8;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.14);
}

.dropdown-top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 4px 12px;
}

.dropdown-user {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.dropdown-user strong {
  font-size: 15px;
  color: #0f172a;
}

.dropdown-user span {
  margin-top: 3px;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.dropdown-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-link {
  width: 100%;
  min-height: 42px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: #f8fbff;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.menu-link:hover {
  background: #ffffff;
  border-color: #dbe6f2;
  transform: translateY(-1px);
}

.arrow {
  color: #94a3b8;
  font-size: 18px;
  line-height: 1;
}

.dropdown-divider {
  height: 1px;
  margin: 12px 2px 10px;
  background: #e8eef5;
}

.logout-link {
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 14px;
  background: rgba(241, 245, 249, 0.92);
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.logout-link:hover {
  background: #eef2ff;
  color: #1d4ed8;
}

.main {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 16px 30px;
}

.quick-publish {
  position: fixed;
  top: 260px;
  right: 24px;
  z-index: 18;
  width: 152px;
  padding: 12px;
  border-radius: 22px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(14px);
}

.quick-publish-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
  padding: 0 4px;
}

.quick-publish-eyebrow {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.quick-publish-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
}

.quick-publish-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-publish-item {
  width: 100%;
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 251, 255, 0.96) 100%);
  padding: 10px;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.05);
}

.quick-publish-item:hover {
  transform: translateY(-2px);
  border-color: rgba(147, 197, 253, 0.96);
  background: linear-gradient(180deg, #ffffff 0%, #f0f7ff 100%);
  box-shadow: 0 14px 24px rgba(59, 130, 246, 0.12);
}

.quick-publish-item__icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 13px;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.98) 0%, rgba(219, 234, 254, 0.98) 100%);
  color: #2563eb;
  font-size: 17px;
  box-shadow: inset 0 0 0 1px rgba(191, 219, 254, 0.95);
}

.quick-publish-item:nth-child(2) .quick-publish-item__icon {
  background: linear-gradient(180deg, rgba(255, 247, 237, 0.98) 0%, rgba(254, 215, 170, 0.5) 100%);
  color: #c2410c;
  box-shadow: inset 0 0 0 1px rgba(253, 186, 116, 0.85);
}

.quick-publish-item__content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.quick-publish-item__hint {
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.2;
}

.quick-publish-item__title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.25;
}

.footer {
  background: transparent;
}

.footer-inner {
  max-width: 1180px;
  margin: 16px auto 10px;
  padding: 0 16px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7c8ca3;
  font-size: 12px;
}

@media (max-width: 1080px) {
  .nav-shell {
    max-width: 1180px;
    grid-template-columns: minmax(0, 204px) minmax(0, 1fr) minmax(0, 288px);
    gap: 16px;
    padding: 12px 18px;
  }

  .menu :deep(.el-menu-item) {
    padding: 0 12px;
    font-size: 13px;
  }

  .nav-actions {
    gap: 14px;
  }

  .chat-btn {
    padding: 0 13px;
  }

  .quick-publish {
    right: 16px;
    width: 142px;
  }
}

@media (max-width: 900px) {
  .nav {
    padding-top: 10px;
  }

  .nav-shell {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .nav-left,
  .nav-right {
    justify-content: center;
  }

  .nav-actions {
    justify-content: center;
    gap: 14px;
  }

  .menu {
    flex-wrap: wrap;
  }

  .quick-publish {
    display: none;
  }
}

@media (max-width: 640px) {
  .nav {
    padding: 10px 12px 0;
  }

  .nav-shell {
    padding: 14px 12px;
    border-radius: 20px;
  }

  .brand {
    justify-content: center;
  }

  .brand-copy {
    align-items: flex-start;
  }

  .menu :deep(.el-menu-item) {
    margin: 3px;
    padding: 0 10px;
    font-size: 13px;
  }

  .nav-actions {
    flex-direction: column;
    gap: 10px;
  }

  .chat-btn,
  .user {
    width: 100%;
    justify-content: center;
  }

  .main {
    padding: 18px 12px 24px;
  }
}
</style>
