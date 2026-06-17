<template>
  <section class="panel">
    <div class="page-shell user-page-shell">
      <div class="hero">
        <div class="hero-copy">
          <span class="hero-badge">留言反馈</span>
          <h2>留言反馈</h2>
          <p>欢迎提交功能建议、使用问题或改进意见，我们会认真查看每一条反馈并持续优化使用体验。</p>
        </div>

        <div class="hero-side">
          <div class="hero-stat">
            <span class="hero-stat__label">反馈类型</span>
            <strong>建议 / 问题</strong>
          </div>
          <div class="hero-stat soft">
            <span class="hero-stat__label">处理态度</span>
            <strong>认真查看</strong>
          </div>
        </div>
      </div>

      <el-card shadow="never" class="card">
        <div class="card-head">
          <div>
            <div class="card-title">提交你的反馈</div>
            <p class="card-desc">请尽量填写清楚问题背景、建议内容和联系方式，便于我们更快跟进处理。</p>
          </div>
        </div>

        <el-alert v-if="submitError" type="error" :closable="false" :title="submitError" class="submit-error" />

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form">
          <el-form-item label="主题" prop="subject" required>
            <el-input v-model="form.subject" placeholder="例如：功能建议、使用问题、交易体验" />
          </el-form-item>

          <el-form-item label="内容" prop="content" required class="content-item">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="7"
              placeholder="请详细描述你的问题或建议，便于我们更快处理"
            />
          </el-form-item>

          <div class="form-grid">
            <el-form-item label="联系方式" prop="contact" required>
              <el-input v-model="form.contact" placeholder="请填写手机号、微信或 QQ" />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="选填，用于接收回复或处理进度" />
            </el-form-item>
          </div>

          <div class="actions">
            <div class="actions-copy">
              <strong>提交后我们会尽快查看</strong>
              <span>感谢你帮助校园二手平台持续改进校园闲置交易体验。</span>
            </div>
            <el-button class="submit-btn" type="primary" :loading="loading" @click="submit">提交反馈</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { FormInstance } from "element-plus";
import { ElMessage } from "element-plus";
import { apiFeedbackSubmit } from "@/api/feedback";
import { getApiErrorMessage } from "@/utils/apiError";

const loading = ref(false);
const submitError = ref("");

const formRef = ref<FormInstance>();
const form = ref({
  subject: "",
  content: "",
  contact: "",
  email: "",
});

const rules = {
  subject: [{ required: true, message: "请输入主题", trigger: "blur" }],
  content: [{ required: true, message: "请输入内容", trigger: "blur" }],
  contact: [{ required: true, message: "请输入联系方式", trigger: "blur" }],
  email: [{ type: "email", message: "邮箱格式不正确", trigger: "blur" }],
};

async function submit() {
  await formRef.value?.validate();

  loading.value = true;
  submitError.value = "";
  try {
    await apiFeedbackSubmit({
      subject: form.value.subject,
      content: form.value.content,
      contact: form.value.contact,
      email: form.value.email || undefined,
    });

    ElMessage.success("提交成功，感谢反馈");
    form.value = { subject: "", content: "", contact: "", email: "" };
    formRef.value?.clearValidate();
  } catch (error) {
    submitError.value = getApiErrorMessage(error, "提交失败，请稍后重试");
    ElMessage.error(submitError.value);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.panel {
  background: transparent;
}

.hero {
  margin-bottom: 18px;
  padding: 18px 20px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.08), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(246, 250, 255, 0.98) 100%);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(300px, 0.98fr);
  align-items: center;
  gap: 18px;
}

.hero-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.hero-copy h2 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.14;
  font-weight: 800;
}

.hero-copy p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.68;
  max-width: 580px;
}

.hero-side {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
  min-width: 0;
  padding-bottom: 2px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.hero-side::-webkit-scrollbar {
  display: none;
}

.hero-stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: max-content;
  height: 42px;
  padding: 0 12px;
  border-radius: 14px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  background: rgba(239, 246, 255, 0.96);
  white-space: nowrap;
}

.hero-stat.soft {
  border-color: rgba(220, 252, 231, 0.92);
  background: rgba(240, 253, 244, 0.96);
}

.hero-stat__label {
  color: #7b8aa3;
  font-size: 11px;
  line-height: 1;
}

.hero-stat strong {
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1;
  font-weight: 700;
}

.hero-stat.soft strong {
  color: #15803d;
}

.card {
  width: 100%;
  border: none;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.06);
}

.card :deep(.el-card__body) {
  padding: 24px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.card-title {
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
}

.card-desc {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.75;
}

.submit-error {
  margin: 18px 0 0;
}

.form {
  margin-top: 18px;
}

.form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.form :deep(.el-input__wrapper),
.form :deep(.el-textarea__inner) {
  border-radius: 16px;
  background: #fbfdff;
  box-shadow: inset 0 0 0 1px rgba(209, 219, 234, 0.9);
}

.form :deep(.el-input__wrapper) {
  min-height: 46px;
}

.form :deep(.el-textarea__inner) {
  min-height: 170px !important;
  padding-top: 12px;
  line-height: 1.75;
}

.content-item {
  margin-bottom: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 4px;
  padding-top: 18px;
  border-top: 1px solid rgba(226, 232, 240, 0.82);
}

.actions-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.actions-copy strong {
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
}

.actions-copy span {
  margin-top: 4px;
  color: #7b8aa3;
  font-size: 12px;
  line-height: 1.6;
}

.submit-btn {
  min-width: 136px;
  height: 46px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 60%, #22c55e 100%);
  box-shadow: 0 16px 28px rgba(59, 130, 246, 0.2);
  font-weight: 700;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .hero-side {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .panel {
    padding-bottom: 20px;
  }

  .hero {
    padding: 18px;
  }

  .card :deep(.el-card__body) {
    padding: 18px;
  }

  .hero-copy h2,
  .card-title {
    font-size: 24px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .submit-btn {
    width: 100%;
  }
}
</style>

