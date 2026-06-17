<template>
  <el-container class="admin-layout">
    <el-header class="header">
      <div class="header-left">
        <span class="sys-name">后台管理</span>
      </div>

      <div class="header-right">
        <el-dropdown trigger="hover" placement="bottom-end">
          <div class="user-trigger">
            <el-avatar :size="32" class="avatar" :src="avatarSrc">{{ avatarText }}</el-avatar>
            <span class="admin-name">{{ displayName }}</span>
            <span class="caret">▼</span>
          </div>

          <template #dropdown>
            <div class="dropdown-card">
              <div class="dropdown-item" @click="goProfile">个人信息</div>
              <div class="dropdown-item" @click="goPassword">修改密码</div>
              <div class="dropdown-item danger" @click="logout">退出登录</div>
            </div>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="body">
      <el-aside width="220px" class="aside">
        <el-menu :default-active="activeMenu" router class="menu" :collapse-transition="false">
          <el-menu-item index="/admin/dashboard">
            <span>系统首页</span>
          </el-menu-item>

          <el-sub-menu index="info">
            <template #title>
              <span>信息管理</span>
            </template>

            <el-menu-item index="/admin/categories">分类信息</el-menu-item>
            <el-menu-item index="/admin/products">商品信息</el-menu-item>
            <el-menu-item index="/admin/orders">订单信息</el-menu-item>
            <el-menu-item index="/admin/wanted">求购信息</el-menu-item>
            <el-menu-item index="/admin/notices">公告信息</el-menu-item>
            <el-menu-item index="/admin/addresses">地址信息</el-menu-item>
            <el-menu-item index="/admin/feedbacks">反馈信息</el-menu-item>
            <el-menu-item index="/admin/reports">举报管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="user">
            <template #title>
              <span>用户管理</span>
            </template>

            <el-menu-item v-if="isSuperAdmin" index="/admin/admins">管理员信息</el-menu-item>
            <el-menu-item index="/admin/users">用户信息</el-menu-item>
            <el-menu-item index="/admin/verify">认证审核</el-menu-item>
            <el-menu-item index="/admin/credit">信用管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu v-if="isSuperAdmin" index="system">
            <template #title>
              <span>系统管理</span>
            </template>

            <el-menu-item index="/admin/system">系统设置</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="profileDialogVisible" title="个人信息" width="520px" destroy-on-close>
      <el-form label-width="86px" class="profile-form">
        <el-form-item label="账号">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input v-model="profileForm.role" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="profileForm.name" clearable maxlength="30" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="profileForm.phone" clearable maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" clearable maxlength="60" />
        </el-form-item>
        <el-form-item label="头像地址">
          <el-input v-model="profileForm.avatar" clearable placeholder="可选：/upload/xxx.png 或网络图片地址" />
        </el-form-item>
        <el-form-item label="头像预览">
          <el-avatar :size="48" class="avatar" :src="avatarPreviewSrc">{{ avatarPreviewText }}</el-avatar>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="460px" destroy-on-close>
      <el-form label-width="98px" class="password-form">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPwd" show-password clearable maxlength="64" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPwd" show-password clearable maxlength="16" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPwd" show-password clearable maxlength="16" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPassword" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { apiChangePassword, apiGetMe, apiUpdateMe } from "@/api/user";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { getApiErrorMessage, isReloginError, RELOGIN_ERROR_CODE } from "@/utils/apiError";
import { clearStoredUser, getStoredUser, mergeStoredUser } from "@/utils/auth";
import { imgUrl as resolveImgUrl } from "@/utils/img";
import { resetSessionState } from "@/router";

const route = useRoute();
const router = useRouter();
const { runConfirmAction } = useConfirmAction();

const currentAdmin = reactive({
  username: "",
  name: "",
  role: "ADMIN",
  phone: "",
  email: "",
  avatar: "",
  isSuperAdmin: false,
});

const profileDialogVisible = ref(false);
const passwordDialogVisible = ref(false);
const savingProfile = ref(false);
const savingPassword = ref(false);

const profileForm = reactive({
  username: "",
  role: "",
  name: "",
  phone: "",
  email: "",
  avatar: "",
});

const passwordForm = reactive({
  oldPwd: "",
  newPwd: "",
  confirmPwd: "",
});

const displayName = computed(() => currentAdmin.name || currentAdmin.username || "管理员");
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());
const avatarSrc = computed(() => resolveImgUrl(currentAdmin.avatar));
const avatarPreviewSrc = computed(() => resolveImgUrl(profileForm.avatar || currentAdmin.avatar));
const avatarPreviewText = computed(() => {
  const text = (profileForm.name || profileForm.username || "管").slice(0, 1);
  return text.toUpperCase();
});
const isSuperAdmin = computed(() => currentAdmin.isSuperAdmin || currentAdmin.username.toLowerCase() === "admin");

function applyAdmin(source: any) {
  currentAdmin.username = typeof source?.username === "string" ? source.username : currentAdmin.username;
  currentAdmin.name = typeof source?.name === "string" ? source.name : currentAdmin.name;
  currentAdmin.role = typeof source?.role === "string" ? source.role : currentAdmin.role;
  currentAdmin.phone = typeof source?.phone === "string" ? source.phone : currentAdmin.phone;
  currentAdmin.email = typeof source?.email === "string" ? source.email : currentAdmin.email;
  currentAdmin.avatar = typeof source?.avatar === "string" ? source.avatar : currentAdmin.avatar;
  currentAdmin.isSuperAdmin = source?.isSuperAdmin === true || currentAdmin.username.toLowerCase() === "admin";

  mergeStoredUser({
    username: currentAdmin.username,
    name: currentAdmin.name,
    role: currentAdmin.role,
    phone: currentAdmin.phone,
    email: currentAdmin.email,
    avatar: currentAdmin.avatar,
    isSuperAdmin: currentAdmin.isSuperAdmin,
  });
}

function fillProfileForm() {
  profileForm.username = currentAdmin.username;
  profileForm.role = currentAdmin.role || "ADMIN";
  profileForm.name = currentAdmin.name || "";
  profileForm.phone = currentAdmin.phone || "";
  profileForm.email = currentAdmin.email || "";
  profileForm.avatar = currentAdmin.avatar || "";
}

async function loadCurrentAdmin() {
  const stored = getStoredUser();
  if (stored) applyAdmin(stored);

  try {
    const data: any = await apiGetMe();
    applyAdmin(data);
    profileForm.username = data?.username || profileForm.username;
    profileForm.role = data?.role || profileForm.role || "ADMIN";
    profileForm.name = data?.name || "";
    profileForm.phone = data?.phone || "";
    profileForm.email = data?.email || "";
    profileForm.avatar = data?.avatar || "";
  } catch {
    fillProfileForm();
  }
}

function goProfile() {
  fillProfileForm();
  profileDialogVisible.value = true;
}

function goPassword() {
  passwordForm.oldPwd = "";
  passwordForm.newPwd = "";
  passwordForm.confirmPwd = "";
  passwordDialogVisible.value = true;
}

async function saveProfile() {
  savingProfile.value = true;
  try {
    await apiUpdateMe({
      name: profileForm.name.trim(),
      phone: profileForm.phone.trim(),
      email: profileForm.email.trim(),
      avatar: profileForm.avatar.trim(),
    });
    applyAdmin({
      username: profileForm.username,
      name: profileForm.name,
      role: profileForm.role,
      avatar: profileForm.avatar,
    });
    profileDialogVisible.value = false;
    ElMessage.success("个人信息已更新");
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "保存个人信息失败"));
  } finally {
    savingProfile.value = false;
  }
}

async function savePassword() {
  if (!passwordForm.oldPwd.trim()) {
    ElMessage.warning("请输入原密码");
    return;
  }
  if (passwordForm.newPwd.length < 6 || passwordForm.newPwd.length > 16) {
    ElMessage.warning("新密码长度需在 6 到 16 位之间");
    return;
  }
  if (passwordForm.newPwd !== passwordForm.confirmPwd) {
    ElMessage.warning("两次输入的新密码不一致");
    return;
  }

  savingPassword.value = true;
  try {
    await apiChangePassword({
      oldPwd: passwordForm.oldPwd,
      newPwd: passwordForm.newPwd,
      confirmPwd: passwordForm.confirmPwd,
    });
    // 关闭修改密码对话框
    passwordDialogVisible.value = false;
    // 清空表单数据（安全考虑）
    passwordForm.oldPwd = "";
    passwordForm.newPwd = "";
    passwordForm.confirmPwd = "";
    // 强制退出登录并显示提示弹窗
    await forceLogout("密码已修改，请重新登录！");
  } catch (error: any) {
    // 如果是重新登录错误（code=401），后端已使 session 失效，直接退出
    if (isReloginError(error) || error?.code === RELOGIN_ERROR_CODE) {
      passwordDialogVisible.value = false;
      passwordForm.oldPwd = "";
      passwordForm.newPwd = "";
      passwordForm.confirmPwd = "";
      await forceLogout("密码已修改，请重新登录！");
      return;
    }
    ElMessage.error(getApiErrorMessage(error, "修改密码失败"));
  } finally {
    savingPassword.value = false;
  }
}

/**
 * 强制退出登录：显示提示弹窗，清除本地存储并跳转到登录页
 * 同时重置路由守卫的 session 验证状态
 *
 * @param message - 可选的提示信息，默认显示"密码已修改，请重新登录！"
 */
async function forceLogout(message: string = "密码已修改，请重新登录！") {
  // 显示友好的弹窗提示，告知用户需要重新登录
  await ElMessageBox.alert(message, "提示", {
    // 使用成功类型的图标
    type: "success",
    // 显示确认按钮
    confirmButtonText: "确定",
    // 点击弹窗后执行退出逻辑
    callback: async () => {
      // 清除本地存储的用户信息（localStorage）
      clearStoredUser();
      // 重置 session 验证状态，确保路由守卫会重新验证
      resetSessionState();
      // 跳转到登录页
      await router.push("/auth");
    },
  });
}

async function logout() {
  await runConfirmAction({
    title: "提示",
    message: "确定退出登录吗？",
    type: "warning",
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    action: async () => {
      await forceLogout("退出成功");
    },
    showError: false,
  });
}

const activeMenu = computed(() => route.path);

onMounted(loadCurrentAdmin);
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  background: #f2f2f2;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #ddd;
}

.header-left .sys-name {
  font-size: 18px;
  font-weight: 600;
  color: #222;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #333;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  background: #fff;
}

.avatar {
  border: 1px solid #bbb;
  background: #e9e9e9;
  color: #111;
}

.admin-name {
  font-size: 14px;
}

.caret {
  font-size: 12px;
  color: #666;
}

.dropdown-card {
  width: auto;
  border: 1px solid #333;
  background: #fff;
  padding: 6px 0;
}

.dropdown-item {
  padding: 10px 12px;
  font-size: 14px;
  color: #222;
  border-top: 1px solid #eee;
}

.dropdown-item:first-child {
  border-top: none;
}

.dropdown-item:hover {
  background: #f5f5f5;
}

.dropdown-item.danger {
  color: #111;
  font-weight: 600;
}

.body {
  height: calc(100vh - 60px);
}

.aside {
  background: #fff;
  border-right: 1px solid #ddd;
}

.menu {
  height: 100%;
  border-right: none;
}

.main {
  padding: 16px;
}

.profile-form,
.password-form {
  padding-right: 8px;
}
</style>
