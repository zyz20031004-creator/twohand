<template>
  <div class="mine-layout" v-loading="loading">
    <aside class="side card">
      <div class="side-user" @click="goTab('home')">
        <el-avatar :size="46" class="avatar" :src="avatarSrc">{{ avatarText }}</el-avatar>
        <div class="side-meta">
          <div class="side-name">{{ displayName }}</div>
          <div class="side-sub">个人中心</div>
        </div>
      </div>

      <el-menu
        class="side-menu"
        :default-active="activeTab"
        :default-openeds="['trade', 'account']"
        :collapse-transition="false"
        @select="onMenuSelect"
      >
        <el-menu-item index="home">我的主页</el-menu-item>

        <el-sub-menu index="trade">
          <template #title>我的交易</template>
          <el-menu-item index="products">我发布的</el-menu-item>
          <el-menu-item index="sold">我卖出的</el-menu-item>
          <el-menu-item index="bought">我买到的</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="fav">我的收藏</el-menu-item>

        <el-sub-menu index="account">
          <template #title>账户设置</template>
          <el-menu-item index="profile">个人资料</el-menu-item>
          <el-menu-item index="verify">学号认证</el-menu-item>
          <el-menu-item index="credit">信誉积分</el-menu-item>
          <el-menu-item index="address">我的地址</el-menu-item>
          <el-menu-item index="mywanted">我的求购</el-menu-item>
          <el-menu-item index="myfeedback">我的反馈</el-menu-item>
          <el-menu-item index="myreport">我的举报</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <section class="content-host">
      <router-view />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { apiGetMe } from "@/api/user";
import { getStoredUser } from "@/utils/auth";
import { userAvatarUrl } from "@/utils/img";
import { syncSavedUserProfile, USER_PROFILE_UPDATED_EVENT } from "@/utils/userProfile";

const route = useRoute();
const router = useRouter();
const loading = ref(false);

const user = reactive({
  username: "",
  name: "",
  avatar: "",
});

const tabKeys = new Set([
  "home",
  "products",
  "sold",
  "bought",
  "fav",
  "profile",
  "verify",
  "credit",
  "address",
  "mywanted",
  "myfeedback",
  "myreport",
]);

const activeTab = computed(() => {
  const seg = route.path.split("/").filter(Boolean)[2];
  return seg && tabKeys.has(seg) ? seg : "home";
});

const displayName = computed(() => user.name || user.username || "用户");
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());

const avatarSrc = computed(() => {
  return userAvatarUrl(user.avatar);
});

function tabPath(tab: string) {
  return `/user/mine/${tab}`;
}

function goTab(tab: string) {
  router.push(tabPath(tab));
}

function onMenuSelect(index: string) {
  if (tabKeys.has(index)) {
    goTab(index);
  }
}

function applyUser(source: any) {
  user.username = typeof source?.username === "string" ? source.username : "";
  user.name = typeof source?.name === "string" ? source.name : "";
  user.avatar = typeof source?.avatar === "string" ? source.avatar : "";
}

function readCachedUser() {
  return getStoredUser();
}

async function loadUser() {
  loading.value = true;
  try {
    const profile = await apiGetMe();
    applyUser(profile);
    syncSavedUserProfile(profile, { emit: false });
  } catch {
    const cached = readCachedUser();
    if (cached) applyUser(cached);
  } finally {
    loading.value = false;
  }
}

function handleProfileUpdated(event: Event) {
  const detail = (event as CustomEvent).detail;
  if (detail) applyUser(detail);
}

onMounted(() => {
  const cached = readCachedUser();
  if (cached) applyUser(cached);
  loadUser();
  window.addEventListener(USER_PROFILE_UPDATED_EVENT, handleProfileUpdated as EventListener);
});

onBeforeUnmount(() => {
  window.removeEventListener(USER_PROFILE_UPDATED_EVENT, handleProfileUpdated as EventListener);
});
</script>

<style scoped>
.mine-layout {
  max-width: 1360px;
  margin: 0 auto;
  padding: 16px;
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 14px;
}

.card {
  background: #fff;
  border: 1px solid #e9edf4;
  border-radius: 20px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.side {
  padding: 14px;
  position: sticky;
  top: 14px;
  align-self: start;
}

.side-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e7ecf4;
  border-radius: 14px;
  cursor: pointer;
  margin-bottom: 10px;
  transition: all 0.2s ease;
}

.side-user:hover {
  border-color: #d4def1;
  background: #f8fbff;
}

.avatar {
  border: 1px solid #d7deea;
}

.side-meta {
  min-width: 0;
}

.side-name {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #94a3b8;
}

.side-menu {
  border-right: none;
  background: transparent;
}

.side-menu :deep(.el-menu) {
  border-right: none;
  background: transparent;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  border-radius: 12px;
  margin-bottom: 6px;
  color: #334155;
  font-size: 15px;
  font-weight: 600;
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background: #f4f7fd;
  color: #1d4ed8;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: #edf2ff;
  color: #1d4ed8;
}

.side-menu :deep(.el-sub-menu .el-menu-item) {
  margin-left: 8px;
  height: 38px;
  line-height: 38px;
  font-size: 14px;
  color: #475569;
}

.side-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background: #eef4ff;
  color: #1d4ed8;
}

.content-host {
  min-height: 640px;
  min-width: 0;
}

.content-host :deep(.page-wrap),
.content-host :deep(.mine-home) {
  max-width: none;
  margin: 0;
  padding-top: 0;
}

.content-host :deep(.wrap) {
  max-width: none;
  margin: 0;
  padding: 0;
}

@media (max-width: 900px) {
  .mine-layout {
    grid-template-columns: 1fr;
  }

  .side {
    position: static;
  }
}
</style>
