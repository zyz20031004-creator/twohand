<template>
  <div class="page-wrap" v-loading="loading">
    <section class="page-card">
      <header class="page-head">
        <div class="head-copy">
          <h2 class="page-title">学号认证</h2>
          <p class="page-desc">填写学校、学号和姓名后即可提交认证，审核结果会同步更新到当前页面。</p>
        </div>
        <el-tag :type="statusMeta.type" effect="light" class="status-tag">
          {{ statusMeta.text }}
        </el-tag>
      </header>

      <section class="section-card verify-card">
        <div class="section-head">
          <div>
            <h3 class="section-title">提交认证信息</h3>
            <p class="section-desc">{{ statusDescription }}</p>
          </div>
        </div>

        <el-form :model="form" label-position="top" class="form">
          <div class="form-grid">
            <el-form-item label="学校">
              <el-input
                v-model="form.school"
                :disabled="formReadonly"
                placeholder="请输入学校或校区名称"
              />
            </el-form-item>

            <el-form-item label="学号">
              <el-input
                v-model="form.studentNo"
                :disabled="formReadonly"
                placeholder="请输入学号"
              />
            </el-form-item>

            <el-form-item label="真实姓名" class="full-span">
              <el-input
                v-model="form.realName"
                :disabled="formReadonly"
                placeholder="请输入真实姓名"
              />
            </el-form-item>
          </div>

          <div class="upload-panel">
            <div class="upload-head">
              <div>
                <h4 class="upload-title">上传认证材料</h4>
                <p class="upload-desc">请上传能够证明你在校身份的材料，用于校园身份核验。</p>
              </div>
              <div class="support-badges">
                <span>学生证</span>
                <span>校园卡</span>
                <span>教务系统截图</span>
              </div>
            </div>

            <div class="upload-body">
              <div class="material-preview" :class="{ empty: !proofPreviewUrl }">
                <template v-if="proofPreviewUrl">
                  <img :src="proofPreviewUrl" alt="认证材料预览" />
                  <div class="preview-mask">
                    <el-button
                      link
                      type="danger"
                      :icon="Delete"
                      :disabled="formReadonly"
                      @click.stop="removeProof"
                    >
                      删除
                    </el-button>
                  </div>
                </template>
                <template v-else>
                  <el-icon class="preview-icon"><Picture /></el-icon>
                  <span>暂无材料</span>
                </template>
              </div>

              <el-upload
                class="proof-uploader"
                drag
                action="/api/verify/upload"
                :show-file-list="false"
                :disabled="formReadonly"
                :with-credentials="true"
                :before-upload="beforeProofUpload"
                :on-success="handleProofUploadSuccess"
              >
                <div class="upload-drop">
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <strong>点击或拖拽文件到此处上传</strong>
                  <p>支持 JPG / PNG，建议上传清晰照片</p>
                  <small>仅用于校园身份验证，平台将严格保密</small>
                  <span v-if="proofPreviewUrl" class="replace-text">重新上传将替换当前材料</span>
                </div>
              </el-upload>
            </div>
          </div>

          <div class="tips-panel">
            <div class="tips-title">
              <el-icon><InfoFilled /></el-icon>
              <span>审核说明</span>
            </div>
            <div class="tips-grid">
              <div v-for="tip in verifyTips" :key="tip" class="tip-item">{{ tip }}</div>
            </div>
          </div>

          <div class="action-row">
            <el-button
              type="primary"
              class="primary-btn"
              :loading="submitting"
              :disabled="submitDisabled"
              @click="submit"
            >
              {{ submitButtonText }}
            </el-button>
          </div>
        </el-form>
      </section>

      <section class="section-card latest-section">
        <div class="latest-head">
          <div>
            <h3 class="section-title">最近申请</h3>
            <p class="section-desc">这里会显示你最近一次提交的认证信息与当前审核结果。</p>
          </div>
        </div>

        <div v-if="latestApply" class="latest-summary">
          <div class="latest-info">
            <div class="latest-pair">
              <span class="latest-label">学校</span>
              <strong>{{ latestApply.school || "-" }}</strong>
            </div>
            <div class="latest-pair">
              <span class="latest-label">学号</span>
              <strong>{{ latestApply.studentNo || "-" }}</strong>
            </div>
            <div class="latest-pair">
              <span class="latest-label">姓名</span>
              <strong>{{ latestApply.realName || "-" }}</strong>
            </div>
            <div class="latest-pair">
              <span class="latest-label">提交时间</span>
              <strong>{{ formatDate(latestApply.createdAt) }}</strong>
            </div>
          </div>

          <div class="latest-status-panel">
            <span class="latest-label">当前状态</span>
            <el-tag :type="statusMeta.type" effect="light" class="latest-status-tag">
              {{ statusMeta.text }}
            </el-tag>
          </div>
        </div>

        <div v-else class="empty-panel">
          <strong>还没有提交认证</strong>
          <p>补充学校、学号和真实姓名后，即可发起校园身份认证申请。</p>
        </div>

        <div v-if="latestApply?.rejectReason" class="reason-panel">
          <span class="reason-label">驳回原因</span>
          <p>{{ latestApply.rejectReason }}</p>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Delete, InfoFilled, Picture, UploadFilled } from "@element-plus/icons-vue";
import { ElMessage, type UploadProps } from "element-plus";
import { apiVerifyApply, apiVerifyMy, type VerifyApplyItem, type VerifyStatus } from "@/api/verify";

type TagType = "success" | "warning" | "danger" | "info";

const loading = ref(false);
const submitting = ref(false);
const verifyStatus = ref<VerifyStatus>("UNVERIFIED");
const latestApply = ref<VerifyApplyItem | null>(null);
const proofPreviewUrl = ref("");
const proofChanged = ref(false);

const form = reactive({
  school: "",
  studentNo: "",
  realName: "",
});

const verifyTips = [
  "认证通过后可正常发布商品与下单交易",
  "提交后需等待管理员审核",
  "信息有误可重新提交修改申请",
  "认证仅用于校园身份校验",
];

const statusMeta = computed<{ text: string; type: TagType }>(() => {
  if (verifyStatus.value === "VERIFIED") return { text: "已认证", type: "success" };
  if (verifyStatus.value === "PENDING") return { text: "审核中", type: "warning" };
  if (verifyStatus.value === "REJECTED") return { text: "已驳回", type: "danger" };
  return { text: "未认证", type: "info" };
});

const statusDescription = computed(() => {
  if (verifyStatus.value === "VERIFIED") return "你的校园身份已通过审核，如学校、学号或姓名有误，可修改后提交重新审核。";
  if (verifyStatus.value === "PENDING") return "你的认证申请正在审核中，审核期间可查看当前申请信息。";
  if (verifyStatus.value === "REJECTED") return "你的认证申请未通过审核，请根据驳回原因修改后重新提交。";
  return "请填写学校、学号和真实姓名，提交后等待管理员审核。";
});

const submitButtonText = computed(() => {
  if (verifyStatus.value === "VERIFIED") return "申请修改认证信息";
  if (verifyStatus.value === "PENDING") return "审核中";
  if (verifyStatus.value === "REJECTED") return "重新提交认证申请";
  return "提交认证申请";
});

const formReadonly = computed(() => verifyStatus.value === "PENDING");
const submitDisabled = computed(() => formReadonly.value);

async function loadMy() {
  loading.value = true;
  try {
    const data = await apiVerifyMy();
    verifyStatus.value = (data?.verifyStatus || "UNVERIFIED") as VerifyStatus;
    latestApply.value = data?.latestApply || null;
    if (latestApply.value) {
      form.school = latestApply.value.school || "";
      form.studentNo = latestApply.value.studentNo || "";
      form.realName = latestApply.value.realName || "";
      proofPreviewUrl.value = resolveProofUrl(latestApply.value);
      proofChanged.value = false;
    }
  } finally {
    loading.value = false;
  }
}

async function submit() {
  if (submitDisabled.value) return ElMessage.warning("认证申请审核中，请勿重复提交");
  if (!form.school.trim()) return ElMessage.warning("请输入学校");
  if (!form.studentNo.trim()) return ElMessage.warning("请输入学号");
  if (!form.realName.trim()) return ElMessage.warning("请输入真实姓名");

  submitting.value = true;
  try {
    await apiVerifyApply({
      school: form.school.trim(),
      studentNo: form.studentNo.trim(),
      realName: form.realName.trim(),
      proofUrl: proofChanged.value ? proofPreviewUrl.value : undefined,
    });
    ElMessage.success(
      verifyStatus.value === "VERIFIED"
        ? "认证修改申请已提交，请等待管理员审核"
        : "认证申请已提交，请等待管理员审核"
    );
    await loadMy();
  } finally {
    submitting.value = false;
  }
}

const beforeProofUpload: UploadProps["beforeUpload"] = (rawFile) => {
  const validType = ["image/jpeg", "image/png"].includes(rawFile.type);
  if (!validType) {
    ElMessage.warning("请上传 JPG 或 PNG 图片");
    return false;
  }
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.warning("图片大小不能超过 5MB");
    return false;
  }
  return true;
};

const handleProofUploadSuccess: UploadProps["onSuccess"] = (response, uploadFile) => {
  const result = response as any;
  if (result && typeof result === "object" && "code" in result && result.code !== 0 && result.code !== 200) {
    ElMessage.error(result.msg || result.message || "认证材料上传失败");
    return;
  }
  const url = extractUploadUrl(response) || URL.createObjectURL(uploadFile.raw!);
  proofPreviewUrl.value = url;
  proofChanged.value = true;
  ElMessage.success("认证材料已上传");
};

function removeProof() {
  proofPreviewUrl.value = "";
  proofChanged.value = true;
}

function resolveProofUrl(item: VerifyApplyItem) {
  return item.proofUrl || item.fileUrl || item.imageUrl || "";
}

function extractUploadUrl(response: unknown) {
  const data = response as any;
  return data?.data?.url || data?.data?.fileUrl || data?.data || data?.url || data?.fileUrl || "";
}

function formatDate(value?: string) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}

onMounted(loadMy);
</script>

<style scoped>
.page-wrap {
  max-width: 1240px;
  margin: 0 auto;
  padding: 16px;
}

.page-card {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #eef2f7;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.head-copy {
  min-width: 0;
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.16;
  font-weight: 800;
}

.page-desc {
  max-width: 620px;
  margin: 9px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.status-tag {
  flex-shrink: 0;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  font-weight: 700;
  margin-top: 2px;
}

.section-card {
  border: 1px solid #edf2f7;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  padding: 20px 22px 22px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.verify-card {
  position: relative;
  overflow: hidden;
}

.section-head,
.latest-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 750;
}

.section-desc {
  margin: 7px 0 0;
  color: #7b8aa3;
  font-size: 13px;
  line-height: 1.64;
}

.form {
  display: block;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
}

.full-span {
  grid-column: 1 / -1;
}

.form :deep(.el-form-item) {
  margin-bottom: 0;
}

.form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

:deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 14px;
  background: #f8fbff;
  box-shadow: 0 0 0 1px rgba(209, 219, 234, 0.9);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.14);
}

.upload-panel {
  margin-top: 22px;
  padding: 18px;
  border: 1px solid #e4edf8;
  border-radius: 18px;
  background: #fafdff;
}

.upload-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.upload-title {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
  font-weight: 750;
}

.upload-desc {
  margin: 6px 0 0;
  color: #7b8aa3;
  font-size: 13px;
  line-height: 1.6;
}

.support-badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.support-badges span {
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  line-height: 28px;
}

.upload-body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
}

.material-preview {
  position: relative;
  display: flex;
  min-height: 156px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  background: #ffffff;
}

.material-preview.empty {
  flex-direction: column;
  gap: 9px;
  border-style: dashed;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 700;
}

.material-preview img {
  width: 100%;
  height: 100%;
  min-height: 156px;
  object-fit: cover;
}

.preview-icon {
  color: #60a5fa;
  font-size: 32px;
}

.preview-mask {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
}

.proof-uploader {
  min-width: 0;
}

.proof-uploader :deep(.el-upload) {
  width: 100%;
}

.proof-uploader :deep(.el-upload-dragger) {
  display: flex;
  min-height: 156px;
  align-items: center;
  justify-content: center;
  border: 1px dashed #bfdbfe;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fbff 0%, #f2f8ff 100%);
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.proof-uploader :deep(.el-upload-dragger:hover) {
  border-color: #60a5fa;
  background: #eff6ff;
  box-shadow: 0 10px 22px rgba(59, 130, 246, 0.08);
}

.upload-drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  text-align: center;
}

.upload-icon {
  margin-bottom: 8px;
  color: #3b82f6;
  font-size: 34px;
}

.upload-drop strong {
  color: #0f172a;
  font-size: 14px;
}

.upload-drop p,
.upload-drop small {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.replace-text {
  margin-top: 9px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.tips-panel {
  margin-top: 16px;
  padding: 15px;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  background: #f5f9ff;
}

.tips-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 750;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px 12px;
  margin-top: 11px;
}

.tip-item {
  position: relative;
  min-height: 34px;
  padding: 8px 10px 8px 24px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.78);
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
}

.tip-item::before {
  content: "";
  position: absolute;
  left: 11px;
  top: 15px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #60a5fa;
}

.action-row {
  margin-top: 18px;
  display: flex;
  justify-content: flex-start;
}

.primary-btn {
  min-width: 154px;
  height: 42px;
  padding: 0 22px;
  border-radius: 12px;
  font-weight: 700;
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.16);
}

.latest-section {
  margin-top: 18px;
}

.latest-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid #e7eef8;
  background: linear-gradient(180deg, #fafdff 0%, #f8fbff 100%);
}

.latest-info {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.latest-pair {
  min-width: 0;
  padding: 12px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #eef2f7;
}

.latest-label {
  display: block;
  color: #7b8aa3;
  font-size: 12px;
  font-weight: 700;
}

.latest-pair strong {
  display: block;
  margin-top: 7px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 750;
  line-height: 1.55;
  word-break: break-word;
}

.latest-status-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: 10px;
  padding-left: 18px;
  border-left: 1px solid #e7eef8;
}

.latest-status-tag {
  width: fit-content;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  font-weight: 700;
}

.reason-panel {
  margin-top: 14px;
  border-radius: 16px;
  background: #fff1f2;
  padding: 14px;
  border: 1px solid #fecdd3;
}

.reason-label {
  display: block;
  color: #be123c;
  font-size: 12px;
  font-weight: 750;
}

.reason-panel p {
  margin: 8px 0 0;
  color: #9f1239;
  font-size: 13px;
  line-height: 1.68;
}

.empty-panel {
  border: 1px dashed #dbeafe;
  border-radius: 16px;
  background: #f8fbff;
  padding: 16px;
}

.empty-panel strong {
  color: #0f172a;
  font-size: 15px;
}

.empty-panel p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.68;
}

@media (max-width: 980px) {
  .upload-body,
  .latest-summary {
    grid-template-columns: 1fr;
  }

  .latest-info {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .latest-status-panel {
    padding-left: 0;
    padding-top: 14px;
    border-left: none;
    border-top: 1px solid #e7eef8;
  }
}

@media (max-width: 760px) {
  .page-wrap {
    padding: 10px;
  }

  .page-card,
  .section-card {
    padding: 16px;
    border-radius: 18px;
  }

  .page-head,
  .upload-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .support-badges {
    justify-content: flex-start;
  }

  .page-title {
    font-size: 24px;
  }

  .form-grid,
  .latest-info,
  .tips-grid {
    grid-template-columns: 1fr;
  }

  .upload-panel,
  .latest-summary {
    padding: 14px;
  }

  .material-preview,
  .proof-uploader :deep(.el-upload-dragger) {
    min-height: 146px;
  }

  .action-row {
    justify-content: stretch;
  }

  .primary-btn {
    width: 100%;
  }
}
</style>
