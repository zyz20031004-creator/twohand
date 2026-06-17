<template>
  <div class="page">
    <div class="page-title">系统设置</div>

    <el-card shadow="never" class="form-card" v-loading="loading">
      <div class="form-head">
        <div class="form-head-title">基础配置</div>
        <div class="form-head-desc">轻量维护平台基础信息，不包含复杂运维监控。</div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="setting-form"
      >
        <el-form-item label="平台名称" prop="siteName">
          <el-input
            v-model="form.siteName"
            maxlength="100"
            show-word-limit
            placeholder="请输入平台名称"
          />
        </el-form-item>

        <el-form-item label="平台副标题" prop="siteSubtitle">
          <el-input
            v-model="form.siteSubtitle"
            maxlength="255"
            show-word-limit
            placeholder="请输入平台副标题"
          />
        </el-form-item>

        <el-form-item label="首页公告标题" prop="noticeTitle">
          <el-input
            v-model="form.noticeTitle"
            maxlength="100"
            show-word-limit
            placeholder="请输入首页公告标题"
          />
        </el-form-item>

        <el-form-item label="首页公告内容" prop="noticeContent">
          <el-input
            v-model="form.noticeContent"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="请输入首页公告内容"
          />
        </el-form-item>

        <el-form-item label="商品审核开关" prop="productAuditEnabled">
          <div class="switch-wrap">
            <el-switch v-model="form.productAuditEnabled" />
            <span class="field-tip">
              当前先支持后台保存，商品发布流程联动可后续补充。
            </span>
          </div>
        </el-form-item>

        <el-form-item label="最多上传图片数" prop="maxUploadCount">
          <div class="number-wrap">
            <el-input-number v-model="form.maxUploadCount" :min="1" :max="20" :step="1" />
            <span class="field-tip">当前先作为系统配置保存，前台上传限制暂未统一联动。</span>
          </div>
        </el-form-item>

        <el-form-item label="平台联系方式" prop="contactInfo">
          <el-input
            v-model="form.contactInfo"
            maxlength="255"
            show-word-limit
            placeholder="请输入平台联系方式"
          />
        </el-form-item>

        <el-form-item label="平台说明" prop="siteDesc">
          <el-input
            v-model="form.siteDesc"
            type="textarea"
            :rows="6"
            maxlength="4000"
            show-word-limit
            placeholder="请输入平台说明"
          />
        </el-form-item>
      </el-form>

      <div class="action-row">
        <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { apiAdminSystemConfigGet, apiAdminSystemConfigSave } from "@/api/system";
import { getApiErrorMessage } from "@/utils/apiError";

const loading = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();

const form = reactive({
  siteName: "",
  siteSubtitle: "",
  noticeTitle: "",
  noticeContent: "",
  productAuditEnabled: true,
  maxUploadCount: 6,
  contactInfo: "",
  siteDesc: "",
});

const rules: FormRules = {
  siteName: [
    { required: true, message: "请输入平台名称", trigger: "blur" },
    { min: 2, max: 100, message: "平台名称长度为 2-100 个字符", trigger: "blur" },
  ],
  maxUploadCount: [
    {
      validator: (_rule, value, callback) => {
        const count = Number(value);
        if (!Number.isFinite(count) || count < 1 || count > 20) {
          callback(new Error("最多上传图片数需在 1 到 20 之间"));
          return;
        }
        callback();
      },
      trigger: "change",
    },
  ],
};

function applyConfig(data: any) {
  form.siteName = data?.siteName || "";
  form.siteSubtitle = data?.siteSubtitle || "";
  form.noticeTitle = data?.noticeTitle || "";
  form.noticeContent = data?.noticeContent || "";
  form.productAuditEnabled = Boolean(data?.productAuditEnabled ?? true);
  form.maxUploadCount = Number(data?.maxUploadCount ?? 6);
  form.contactInfo = data?.contactInfo || "";
  form.siteDesc = data?.siteDesc || "";
}

async function loadConfig() {
  loading.value = true;
  try {
    const data = await apiAdminSystemConfigGet();
    applyConfig(data);
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "加载系统设置失败"));
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  saving.value = true;
  try {
    const data = await apiAdminSystemConfigSave({
      siteName: form.siteName.trim(),
      siteSubtitle: form.siteSubtitle.trim(),
      noticeTitle: form.noticeTitle.trim(),
      noticeContent: form.noticeContent.trim(),
      productAuditEnabled: form.productAuditEnabled,
      maxUploadCount: Number(form.maxUploadCount),
      contactInfo: form.contactInfo.trim(),
      siteDesc: form.siteDesc.trim(),
    });
    applyConfig(data);
    ElMessage.success("系统设置已保存");
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "保存系统设置失败"));
  } finally {
    saving.value = false;
  }
}

onMounted(loadConfig);
</script>

<style scoped>
.page {
  padding: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #222;
}

.form-card {
  border: 1px solid #ddd;
}

.form-head {
  margin-bottom: 18px;
}

.form-head-title {
  font-size: 16px;
  font-weight: 600;
  color: #222;
}

.form-head-desc {
  margin-top: 6px;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
}

.setting-form {
  max-width: 840px;
}

.switch-wrap,
.number-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.field-tip {
  color: #666;
  font-size: 12px;
  line-height: 1.6;
}

.action-row {
  padding-left: 120px;
  padding-top: 8px;
}
</style>
