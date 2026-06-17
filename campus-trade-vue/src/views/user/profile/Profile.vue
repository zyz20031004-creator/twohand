<template>
  <div class="page-wrap" v-loading="loading">
    <section class="page-card">
      <header class="profile-hero">
        <el-upload
          class="avatar-uploader"
          action="/api/upload"
          :show-file-list="false"
          :on-success="handleUploadSuccess"
        >
          <img :src="avatarPreviewSrc" class="avatar-preview" />
        </el-upload>

        <div class="hero-copy">
          <div class="hero-top">
            <div class="hero-heading">
              <span class="hero-badge">个人资料</span>
              <h2 class="hero-title">{{ displayName }}</h2>
              <p class="hero-subtitle">@{{ form.username || "未设置账号" }}</p>
            </div>

            <div class="hero-tags">
              <el-tag :type="verifyTagType" effect="light" class="hero-tag">{{ verifyTagText }}</el-tag>
              <div class="hero-chip">
                <span>信誉积分</span>
                <strong>{{ creditScore }}</strong>
              </div>
            </div>
          </div>

          <p class="hero-desc">完善头像、昵称和联系方式，让个人主页展示更完整，交易沟通也更顺畅。</p>

          <div class="hero-meta">
            <div class="hero-meta-item">
              <span class="meta-label">手机号</span>
              <strong>{{ form.phone || "待补充" }}</strong>
            </div>
            <div class="hero-meta-item">
              <span class="meta-label">邮箱</span>
              <strong>{{ form.email || "待补充" }}</strong>
            </div>
          </div>

          <p class="hero-note">{{ verifyGuideText }}</p>
        </div>
      </header>

      <section class="section-card form-card">
        <div class="section-head">
          <h3 class="section-title">编辑资料</h3>
          <p class="section-desc">保存后会同步更新导航栏、个人空间和交易联系场景中的展示信息。</p>
        </div>

        <div class="form-grid">
          <div class="field-block readonly">
            <span class="field-label">用户名</span>
            <el-input v-model="form.username" disabled />
          </div>

          <div class="field-block">
            <span class="field-label">昵称</span>
            <el-input v-model="form.name" placeholder="请输入昵称" />
          </div>

          <div class="field-block">
            <span class="field-label">手机号</span>
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </div>

          <div class="field-block">
            <span class="field-label">邮箱</span>
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </div>

          <div class="field-block full">
            <span class="field-label">头像链接</span>
            <el-input
              v-model="form.avatar"
              placeholder="可选：粘贴网络图片地址或站内上传图片地址"
            />
          </div>
        </div>

        <div class="action-row">
          <el-button class="secondary-btn" @click="pwdVisible = true">修改密码</el-button>
          <el-button type="primary" class="primary-btn" @click="save">保存资料</el-button>
        </div>
      </section>
    </section>

    <el-dialog v-model="pwdVisible" title="修改密码" width="560px" align-center>
      <div class="dialog-form">
        <div class="dialog-row">
          <span class="dialog-label">原密码</span>
          <el-input v-model="pwd.oldPwd" type="password" show-password />
        </div>
        <div class="dialog-row">
          <span class="dialog-label">新密码</span>
          <el-input v-model="pwd.newPwd" type="password" show-password />
        </div>
        <div class="dialog-row">
          <span class="dialog-label">确认密码</span>
          <el-input v-model="pwd.confirmPwd" type="password" show-password />
        </div>
      </div>

      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="changePwd">确定修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { apiChangePassword, apiGetMe, apiUpdateMe } from "@/api/user";
import { apiVerifyMy } from "@/api/verify";
import { apiCreditMy } from "@/api/credit";
import { clearStoredUser } from "@/utils/auth";
import { userAvatarUrl } from "@/utils/img";
import { syncSavedUserProfile } from "@/utils/userProfile";
import { isReloginError, RELOGIN_ERROR_CODE } from "@/utils/apiError";
import { resetSessionState } from "@/router";

type ProfileFormState = {
  id: number;
  username: string;
  role: string;
  name: string;
  phone: string;
  email: string;
  avatar: string;
};

function createProfileForm(): ProfileFormState {
  return {
    id: 0,
    username: "",
    role: "",
    name: "",
    phone: "",
    email: "",
    avatar: "",
  };
}

const router = useRouter();
const loading = ref(false);
const PROFILE_FALLBACK_PATH = "/user/mine/home";

const form = reactive<ProfileFormState>(createProfileForm());
const savedForm = reactive<ProfileFormState>(createProfileForm());
const savedAvatar = ref("");
const tempAvatar = ref("");

const pwdVisible = ref(false);
const pwd = reactive({
  oldPwd: "",
  newPwd: "",
  confirmPwd: "",
});

const verifyStatus = ref<"UNVERIFIED" | "PENDING" | "VERIFIED" | "REJECTED">("UNVERIFIED");
const creditScore = ref(100);

const displayName = computed(() => form.name || form.username || "校园用户");
const normalizedSavedAvatar = computed(() => normalizeText(savedAvatar.value));
const normalizedFormAvatar = computed(() => normalizeText(form.avatar));
const hasUnsavedAvatarChange = computed(() => normalizedFormAvatar.value !== normalizedSavedAvatar.value);
const avatarPreviewSource = computed(() => {
  const temp = normalizeText(tempAvatar.value);
  if (temp && temp === normalizedFormAvatar.value) return temp;
  if (hasUnsavedAvatarChange.value) return normalizedFormAvatar.value;
  return normalizedSavedAvatar.value;
});
const avatarPreviewSrc = computed(() => userAvatarUrl(avatarPreviewSource.value));

const verifyTagText = computed(() => {
  if (verifyStatus.value === "VERIFIED") return "已认证";
  if (verifyStatus.value === "PENDING") return "审核中";
  if (verifyStatus.value === "REJECTED") return "已驳回";
  return "未认证";
});

const verifyTagType = computed(() => {
  if (verifyStatus.value === "VERIFIED") return "success";
  if (verifyStatus.value === "PENDING") return "warning";
  if (verifyStatus.value === "REJECTED") return "danger";
  return "info";
});

const verifyGuideText = computed(() => {
  if (verifyStatus.value === "VERIFIED") return "你的校园身份已通过审核，可正常展示认证标识。";
  if (verifyStatus.value === "PENDING") return "认证申请正在审核中，请耐心等待结果更新。";
  if (verifyStatus.value === "REJECTED") return "认证申请未通过，可调整信息后重新提交。";
  return "完成学号认证后，可提升账号可信度并解锁更多能力。";
});

async function load() {
  loading.value = true;
  try {
    const [res, verifyResp, creditResp] = await Promise.all([
      apiGetMe(),
      apiVerifyMy(),
      apiCreditMy({ page: 1, size: 1 }),
    ]);
    applyLoadedProfile(res);
    verifyStatus.value = (verifyResp?.verifyStatus || "UNVERIFIED") as any;
    creditScore.value = Number(creditResp?.score ?? 100);
    syncSavedUserProfile(res, { emit: false });
  } finally {
    loading.value = false;
  }
}

async function save() {
  form.avatar = normalizedFormAvatar.value;
  await apiUpdateMe({
    name: form.name,
    phone: form.phone,
    email: form.email,
    avatar: form.avatar,
  });
  saveCurrentProfile();
  syncSavedUserProfile(form);
  ElMessage.success("保存成功");
  goBackAfterSave();
}

async function changePwd() {
  try {
    await apiChangePassword({
      oldPwd: pwd.oldPwd,
      newPwd: pwd.newPwd,
      confirmPwd: pwd.confirmPwd,
    });
    // 成功但需要重新登录：后端会返回 code=401
    pwdVisible.value = false;
    pwd.oldPwd = "";
    pwd.newPwd = "";
    pwd.confirmPwd = "";
    ElMessage.success("密码修改成功，请重新登录");
    await forceLogout();
  } catch (error: any) {
    // 如果是重新登录错误（code=401），后端已使 session 失效，直接退出
    if (isReloginError(error) || error?.code === RELOGIN_ERROR_CODE) {
      pwdVisible.value = false;
      pwd.oldPwd = "";
      pwd.newPwd = "";
      pwd.confirmPwd = "";
      ElMessage.warning("密码修改成功，请重新登录");
      await forceLogout();
      return;
    }
    // 其他错误，向上抛出或显示提示
    const msg = error?.msg || error?.message || "修改密码失败";
    ElMessage.error(msg);
  }
}

/**
 * 强制退出登录：清除本地存储并跳转到登录页
 * 同时重置路由守卫的 session 验证状态
 */
async function forceLogout() {
  // 清除本地存储的用户信息
  clearStoredUser();
  // 重置 session 验证状态，确保路由守卫会重新验证
  resetSessionState();
  // 跳转到登录页
  await router.push("/auth");
}

function normalizeText(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}

function applyProfileForm(target: ProfileFormState, source: any) {
  target.id = Number(source?.id ?? 0);
  target.username = typeof source?.username === "string" ? source.username : "";
  target.role = typeof source?.role === "string" ? source.role : "";
  target.name = typeof source?.name === "string" ? source.name : "";
  target.phone = typeof source?.phone === "string" ? source.phone : "";
  target.email = typeof source?.email === "string" ? source.email : "";
  target.avatar = normalizeText(source?.avatar);
}

function saveCurrentProfile() {
  applyProfileForm(savedForm, form);
  savedAvatar.value = savedForm.avatar;
  form.avatar = savedForm.avatar;
  tempAvatar.value = "";
}

function applyLoadedProfile(source: any) {
  applyProfileForm(form, source);
  saveCurrentProfile();
}

function handleUploadSuccess(res: any) {
  const url = normalizeText(res?.data || res?.url || res);
  if (url) {
    form.avatar = url;
    tempAvatar.value = url;
    ElMessage.success("头像上传成功，请保存资料后同步到全站");
    return;
  }
  ElMessage.error("头像上传失败");
}

function goBackAfterSave() {
  if (typeof window === "undefined") {
    router.replace(PROFILE_FALLBACK_PATH);
    return;
  }

  const currentPath = window.location.pathname + window.location.search + window.location.hash;
  const backPath = typeof window.history.state?.back === "string" ? window.history.state.back : "";

  if (backPath && backPath !== currentPath) {
    router.back();
    return;
  }

  router.replace(PROFILE_FALLBACK_PATH);
}

onMounted(load);
onBeforeUnmount(() => {
  tempAvatar.value = "";
});
</script>

<style scoped>
.page-wrap {
  max-width: none;
  margin: 0 auto;
  padding: 8px 0 14px;
}

.page-card {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #eef2f7;
  border-radius: 24px;
  padding: 28px 30px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.profile-hero {
  display: flex;
  align-items: flex-start;
  gap: 28px;
  margin-bottom: 28px;
  padding: 6px 2px 26px;
  border-bottom: 1px solid #edf2f7;
}

.avatar-uploader {
  width: fit-content;
  flex-shrink: 0;
}

:deep(.avatar-uploader .el-upload) {
  border: none;
  border-radius: 30px;
  cursor: pointer;
  overflow: hidden;
}

.avatar-preview {
  width: 144px;
  height: 144px;
  border-radius: 30px;
  border: 1px dashed rgba(147, 197, 253, 0.9);
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  object-fit: cover;
  display: block;
}

.hero-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  padding-top: 4px;
}

.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.hero-heading {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.hero-title {
  margin: 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.08;
  font-weight: 800;
}

.hero-subtitle {
  margin: 0;
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
}

.hero-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  padding-top: 4px;
}

.hero-tag {
  height: 34px;
  border-radius: 999px;
  padding: 0 14px;
  font-weight: 700;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(239, 246, 255, 0.96);
  border: 1px solid rgba(191, 219, 254, 0.9);
}

.hero-chip span {
  color: #7b8aa3;
  font-size: 12px;
}

.hero-chip strong {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  max-width: 680px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.72;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-meta-item {
  min-width: 200px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid #e7eef8;
  background: #f8fbff;
}

.meta-label {
  display: block;
  color: #7b8aa3;
  font-size: 12px;
  font-weight: 700;
}

.hero-meta-item strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.55;
  word-break: break-word;
}

.hero-note {
  margin: 0;
  max-width: 720px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.72;
}

.section-card {
  border: 1px solid #edf2f7;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  padding: 26px 28px;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.04);
}

.section-head {
  margin-bottom: 22px;
}

.section-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.3;
}

.section-desc {
  margin: 8px 0 0;
  max-width: 620px;
  color: #7b8aa3;
  font-size: 14px;
  line-height: 1.72;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px 18px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-block.full {
  grid-column: 1 / -1;
}

.field-label {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.readonly .field-label {
  color: #64748b;
}

.action-row {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid #edf2f7;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.primary-btn,
.secondary-btn {
  height: 44px;
  padding: 0 20px;
  border-radius: 12px;
  font-weight: 700;
}

.primary-btn {
  min-width: 140px;
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.16);
}

.secondary-btn {
  border-color: #dbe6f2;
  color: #64748b;
  background: #ffffff;
}

.dialog-form {
  display: grid;
  gap: 14px;
  padding: 4px 0;
}

.dialog-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.dialog-label {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  min-height: 46px;
  border-radius: 14px;
  background: #f8fbff;
  box-shadow: 0 0 0 1px rgba(209, 219, 234, 0.9);
}

:deep(.el-input__inner) {
  font-size: 14px;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

:deep(.el-input.is-disabled .el-input__wrapper) {
  background: #f4f7fb;
  box-shadow: 0 0 0 1px rgba(226, 232, 240, 0.95);
}

@media (max-width: 900px) {
  .hero-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-tags {
    justify-content: flex-start;
    padding-top: 0;
  }
}

@media (max-width: 760px) {
  .page-wrap {
    padding: 8px 0 10px;
  }

  .page-card,
  .section-card {
    padding: 18px;
    border-radius: 18px;
  }

  .profile-hero {
    flex-direction: column;
    gap: 20px;
    margin-bottom: 22px;
    padding: 6px 0 22px;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-meta-item {
    width: 100%;
  }

  .form-grid,
  .dialog-row {
    grid-template-columns: 1fr;
  }

  .action-row {
    flex-direction: column;
  }

  .primary-btn,
  .secondary-btn {
    width: 100%;
  }
}
</style>

