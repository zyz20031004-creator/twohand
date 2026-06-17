<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="brand-panel">
        <div class="panel-orb orb-one"></div>
        <div class="panel-orb orb-two"></div>
        <div class="panel-orb orb-three"></div>

        <div class="brand-copy">
          <span class="eyebrow">校园闲置交易平台</span>
          <h1>校园二手交易平台</h1>
          <p class="brand-desc">
            让闲置物品高效流转，打造便捷、安全、可信的校园交易环境
          </p>

          <div class="feature-list">
            <div v-for="feature in featureList" :key="feature.title" class="feature-item">
              <div class="feature-icon">
                <el-icon>
                  <component :is="feature.icon" />
                </el-icon>
              </div>
              <div class="feature-content">
                <h3>{{ feature.title }}</h3>
                <p>{{ feature.text }}</p>
              </div>
            </div>
          </div>

          <div class="tag-row">
            <span v-for="tag in sceneTags" :key="tag" class="scene-tag">
              {{ tag }}
            </span>
          </div>
        </div>

        <div class="brand-showcase" aria-hidden="true">
          <div class="showcase-header">
            <span class="visual-badge">校内高效交易</span>
            <el-icon class="showcase-icon">
              <School />
            </el-icon>
          </div>
          <h3>从教材、数码到宿舍好物，让闲置快速找到下一位主人</h3>
          <p>轻量发布、同校沟通、线下见面更安心，适合校园二手交易场景。</p>

          <div class="showcase-stats">
            <div class="stat-pill">
              <strong>3 步</strong>
              <span>快速发布</span>
            </div>
            <div class="stat-pill">
              <strong>校内</strong>
              <span>真实交流</span>
            </div>
            <div class="stat-pill">
              <strong>便捷</strong>
              <span>高效流转</span>
            </div>
          </div>
        </div>
      </section>

      <section class="form-panel">
        <el-card class="auth-card" shadow="never">
          <div class="auth-card__header">
            <span class="welcome-chip">{{ activeTab === "login" ? "欢迎登录" : "欢迎加入" }}</span>
            <h2>{{ activeTab === "login" ? "开启你的校园好物交换" : "注册后即可发布和交易" }}</h2>
            <p>
              {{ activeTab === "login"
                ? "进入平台后即可查看闲置商品、联系卖家、管理你的交易记录。"
                : "创建账号后即可成为平台用户，体验便捷、安全的校园二手交易服务。" }}
            </p>
          </div>

          <el-tabs v-model="activeTab" class="auth-tabs" stretch>
            <el-tab-pane label="登录" name="login">
              <el-form
                ref="loginFormRef"
                :model="loginForm"
                :rules="loginRules"
                label-position="top"
                class="auth-form"
                autocomplete="off"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input
                    v-model="loginForm.username"
                    name="auth-username"
                    placeholder="请输入用户名"
                    autocomplete="off"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="密码" prop="password">
                  <el-input
                    v-model="loginForm.password"
                    name="auth-password"
                    type="password"
                    show-password
                    placeholder="请输入密码"
                    autocomplete="new-password"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="角色" prop="role">
                  <el-select v-model="loginForm.role" class="role-select" placeholder="请选择角色">
                    <el-option
                      v-for="option in roleOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item class="action-item">
                  <el-button class="submit-btn" type="primary" @click="handleLogin">
                    登录平台
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="注册" name="register">
              <el-form
                ref="registerFormRef"
                :model="registerForm"
                :rules="registerRules"
                label-position="top"
                class="auth-form"
                autocomplete="off"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input
                    v-model="registerForm.username"
                    name="register-username"
                    placeholder="请设置用户名"
                    autocomplete="off"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="密码" prop="password">
                  <el-input
                    v-model="registerForm.password"
                    name="register-password"
                    type="password"
                    show-password
                    placeholder="请设置密码"
                    autocomplete="new-password"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input
                    v-model="registerForm.confirmPassword"
                    name="register-confirm-password"
                    type="password"
                    show-password
                    placeholder="请再次输入密码"
                    autocomplete="new-password"
                    clearable
                  />
                </el-form-item>

                <el-form-item class="action-item">
                  <el-button class="submit-btn" type="primary" @click="handleRegister">
                    注册账号
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <div class="card-footer">
            <el-icon class="footer-icon">
              <CircleCheckFilled />
            </el-icon>
            <span>{{ activeTab === "login" ? "支持普通用户与管理员登录" : "注册成功后默认创建普通用户账号" }}</span>
          </div>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from "vue";
import type { Component } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { ChatDotRound, CircleCheckFilled, Promotion, School } from "@element-plus/icons-vue";
import { loginApi, registerApi } from "@/api/auth";
import { setStoredUser } from "@/utils/auth";

type FeatureItem = {
  title: string;
  text: string;
  icon: Component;
};

const router = useRouter();

const activeTab = ref<"login" | "register">("login");

const loginForm = reactive({
  username: "",
  password: "",
  role: "USER" as "USER" | "ADMIN",
});

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
});

const loginFormRef = ref<FormInstance>();
const registerFormRef = ref<FormInstance>();

function resetLoginCredentials() {
  loginForm.username = "";
  loginForm.password = "";
}

const featureList: FeatureItem[] = [
  {
    title: "学生认证，更安心",
    text: "基于校园场景使用，提升交易可信度。",
    icon: CircleCheckFilled,
  },
  {
    title: "闲置发布，更便捷",
    text: "教材数码与宿舍好物都能快速上架。",
    icon: Promotion,
  },
  {
    title: "在线交流，更高效",
    text: "买卖双方直接沟通，减少等待成本。",
    icon: ChatDotRound,
  },
];

const sceneTags = ["校内实名场景", "闲置高效流转", "同学间安心交易"];

const roleOptions = [
  { label: "普通用户", value: "USER" },
  { label: "管理员", value: "ADMIN" },
];

const loginRules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  role: [{ required: true, message: "请选择角色", trigger: "change" }],
};

const registerRules: FormRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 16, message: "用户名长度 3-10 个字符", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 16, message: "密码长度 6-16 个字符", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认密码", trigger: "blur" },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) callback(new Error("两次密码输入不一致"));
        else callback();
      },
      trigger: "blur",
    },
  ],
};

onMounted(async () => {
  resetLoginCredentials();
  await nextTick();
  resetLoginCredentials();
});

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  const user = await loginApi({
    username: loginForm.username,
    password: loginForm.password,
    role: loginForm.role,
  });

  setStoredUser(user as any);
  ElMessage.success("登录成功");

  if (user.role === "ADMIN") {
    router.push("/admin");
  } else {
    router.push("/user");
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  await registerApi({
    username: registerForm.username,
    password: registerForm.password,
  });

  ElMessage.success("注册成功");

  activeTab.value = "login";
  resetLoginCredentials();
  loginForm.role = "USER";
  registerForm.password = "";
  registerForm.confirmPassword = "";
}
</script>

<style scoped>
.auth-page {
  --primary: #3b82f6;
  --secondary: #22c55e;
  --text-strong: #0f172a;
  --text-main: #1e293b;
  --text-muted: #5b6b84;
  --card-shadow: 0 26px 70px rgba(80, 120, 171, 0.18);
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 20px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% 16%, rgba(59, 130, 246, 0.16), transparent 24%),
    radial-gradient(circle at 88% 84%, rgba(34, 197, 94, 0.14), transparent 20%),
    linear-gradient(135deg, #edf6ff 0%, #f8fbff 42%, #f1fbf5 100%);
}

.auth-shell {
  width: min(1100px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(380px, 420px);
  gap: 24px;
  align-items: center;
}

.brand-panel {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 24px;
  min-height: 560px;
  padding: 34px 36px;
  border-radius: 30px;
  color: var(--text-main);
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.84) 0%, rgba(235, 245, 255, 0.9) 34%, rgba(235, 251, 244, 0.92) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 24px 70px rgba(109, 145, 186, 0.16);
}

.panel-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(6px);
  opacity: 0.8;
  pointer-events: none;
}

.orb-one {
  top: -54px;
  left: -32px;
  width: 220px;
  height: 220px;
  background: rgba(59, 130, 246, 0.18);
}

.orb-two {
  right: 9%;
  top: 11%;
  width: 110px;
  height: 110px;
  background: rgba(34, 197, 94, 0.14);
}

.orb-three {
  right: -36px;
  bottom: 36px;
  width: 170px;
  height: 170px;
  background: rgba(73, 211, 178, 0.16);
}

.brand-copy,
.brand-showcase {
  position: relative;
  z-index: 1;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: var(--primary);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.12);
}

.brand-copy h1 {
  margin: 18px 0 12px;
  font-size: clamp(36px, 4.7vw, 50px);
  line-height: 1.08;
  color: var(--text-strong);
  letter-spacing: -0.03em;
}

.brand-desc {
  max-width: 540px;
  margin: 0;
  font-size: 16px;
  line-height: 1.75;
  color: var(--text-muted);
}

.feature-list {
  display: grid;
  gap: 10px;
  margin-top: 24px;
}

.feature-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 10px 24px rgba(109, 145, 186, 0.08);
  backdrop-filter: blur(10px);
}

.feature-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 12px;
  color: var(--primary);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(34, 197, 94, 0.14));
  font-size: 18px;
}

.feature-content h3 {
  margin: 1px 0 4px;
  font-size: 15px;
  line-height: 1.35;
  color: var(--text-strong);
}

.feature-content p {
  margin: 0;
  font-size: 12px;
  line-height: 1.65;
  color: var(--text-muted);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.scene-tag {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.9);
}

.brand-showcase {
  padding: 20px 22px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 16px 34px rgba(109, 145, 186, 0.1);
  backdrop-filter: blur(14px);
}

.showcase-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.visual-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.showcase-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 13px;
  color: var(--secondary);
  background: rgba(34, 197, 94, 0.12);
  font-size: 20px;
}

.brand-showcase h3 {
  margin: 0;
  font-size: 22px;
  line-height: 1.4;
  color: var(--text-strong);
}

.brand-showcase p {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.75;
  color: var(--text-muted);
}

.showcase-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.stat-pill {
  padding: 12px 10px;
  border-radius: 16px;
  background: rgba(248, 251, 255, 0.94);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.12);
}

.stat-pill strong {
  display: block;
  font-size: 16px;
  color: var(--text-strong);
}

.stat-pill span {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted);
}

.form-panel {
  display: flex;
  justify-content: center;
}

.auth-card {
  width: 100%;
  border: none;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--card-shadow);
  animation: float-card 6s ease-in-out infinite;
}

.auth-card :deep(.el-card__body) {
  padding: 32px 30px 28px;
}

.auth-card__header h2 {
  margin: 14px 0 10px;
  font-size: 28px;
  line-height: 1.3;
  color: var(--text-strong);
}

.auth-card__header p {
  margin: 0 0 26px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-muted);
}

.welcome-chip {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(34, 197, 94, 0.14));
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.auth-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.auth-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: rgba(148, 163, 184, 0.16);
}

.auth-tabs :deep(.el-tabs__item) {
  height: 42px;
  font-size: 16px;
  font-weight: 600;
  color: #7b8aa3;
  transition: color 0.2s ease;
}

.auth-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary);
}

.auth-tabs :deep(.el-tabs__active-bar) {
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--primary), var(--secondary));
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.auth-form :deep(.el-input__wrapper),
.auth-form :deep(.el-select__wrapper) {
  min-height: 48px;
  border-radius: 15px;
  background: #f8fbff;
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.28);
  transition: box-shadow 0.2s ease, transform 0.2s ease, background-color 0.2s ease;
}

.auth-form :deep(.el-input__wrapper:hover),
.auth-form :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.34);
}

.auth-form :deep(.el-input__wrapper.is-focus),
.auth-form :deep(.el-select__wrapper.is-focused) {
  background: #ffffff;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.18);
  transform: translateY(-1px);
}

.auth-form :deep(.el-input__inner),
.auth-form :deep(.el-select__selected-item),
.auth-form :deep(.el-input__inner::placeholder) {
  font-size: 14px;
}

.role-select {
  width: 100%;
}

.action-item {
  margin-top: 8px;
}

.auth-form :deep(.action-item .el-form-item__content) {
  margin-left: 0 !important;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 15px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.03em;
  background: linear-gradient(135deg, #3b82f6 0%, #37a8ea 46%, #22c55e 100%);
  box-shadow: 0 16px 30px rgba(59, 130, 246, 0.24);
  transition: transform 0.22s ease, box-shadow 0.22s ease, filter 0.22s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 36px rgba(59, 130, 246, 0.28);
  filter: saturate(1.04);
}

.submit-btn:active {
  transform: translateY(0);
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
  color: var(--text-muted);
  font-size: 13px;
}

.footer-icon {
  color: var(--secondary);
  font-size: 16px;
  flex: none;
}

@keyframes float-card {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

@media (max-width: 1120px) {
  .auth-shell {
    grid-template-columns: minmax(0, 1fr) minmax(360px, 420px);
    gap: 20px;
  }

  .brand-panel {
    min-height: 530px;
    padding: 32px;
  }
}

@media (max-width: 920px) {
  .auth-page {
    padding: 20px;
  }

  .auth-shell {
    grid-template-columns: 1fr;
    max-width: 760px;
  }

  .brand-panel {
    min-height: auto;
    padding: 30px 26px;
    gap: 20px;
  }

  .form-panel {
    justify-content: stretch;
  }

  .auth-card {
    animation: none;
  }
}

@media (max-width: 640px) {
  .auth-page {
    padding: 16px;
  }

  .brand-panel {
    padding: 26px 20px;
    border-radius: 24px;
  }

  .brand-copy h1 {
    margin-top: 16px;
    font-size: 34px;
  }

  .brand-desc {
    font-size: 15px;
  }

  .showcase-stats {
    grid-template-columns: 1fr;
  }

  .auth-card {
    border-radius: 22px;
  }

  .auth-card :deep(.el-card__body) {
    padding: 28px 22px 24px;
  }

  .auth-card__header h2 {
    font-size: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-card,
  .submit-btn,
  .auth-form :deep(.el-input__wrapper),
  .auth-form :deep(.el-select__wrapper) {
    animation: none;
    transition: none;
  }
}
</style>
