<template>
  <div class="page">
    <div class="page-title">{{ TEXT.pageTitle }}</div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          :placeholder="TEXT.keywordPlaceholder"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="query.status"
          :placeholder="TEXT.statusPlaceholder"
          clearable
          class="status-select"
        >
          <el-option :label="TEXT.pending" value="PENDING" />
          <el-option :label="TEXT.approved" value="APPROVED" />
          <el-option :label="TEXT.rejected" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">{{ TEXT.search }}</el-button>
        <el-button @click="handleReset">{{ TEXT.reset }}</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" :label="TEXT.applyId" width="90" align="center" />
        <el-table-column prop="userId" :label="TEXT.userId" width="90" align="center" />
        <el-table-column prop="username" :label="TEXT.username" min-width="140" />
        <el-table-column prop="realName" :label="TEXT.realName" width="120" align="center" />
        <el-table-column prop="studentNo" :label="TEXT.studentNo" min-width="140" />
        <el-table-column prop="school" :label="TEXT.school" min-width="180" show-overflow-tooltip />

        <el-table-column :label="TEXT.currentStatus" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="TEXT.createdAt" width="168" align="center">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column :label="TEXT.actions" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" link @click="handleApprove(row)">{{ TEXT.approve }}</el-button>
              <el-button type="danger" link @click="openReject(row)">{{ TEXT.reject }}</el-button>
            </template>
            <el-button type="primary" link @click="openDetail(row)">{{ TEXT.viewDetail }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailDialog.visible" :title="TEXT.detailDialogTitle" width="720px" destroy-on-close>
      <div v-if="detailDialog.data" class="detail-wrap">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ TEXT.applyId }}</span>
            <strong>{{ detailDialog.data.id }}</strong>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ TEXT.userId }}</span>
            <strong>{{ detailDialog.data.userId }}</strong>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ TEXT.username }}</span>
            <strong>{{ detailDialog.data.username || "-" }}</strong>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ TEXT.realName }}</span>
            <strong>{{ detailDialog.data.realName || "-" }}</strong>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ TEXT.studentNo }}</span>
            <strong>{{ detailDialog.data.studentNo || "-" }}</strong>
          </div>
          <div class="detail-item detail-item-full">
            <span class="detail-label">{{ TEXT.school }}</span>
            <strong>{{ detailDialog.data.school || "-" }}</strong>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ TEXT.reviewStatus }}</span>
            <el-tag :type="statusTagType(detailDialog.data.status)" effect="plain">
              {{ statusText(detailDialog.data.status) }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ TEXT.createdAt }}</span>
            <strong>{{ formatTime(detailDialog.data.createdAt) }}</strong>
          </div>
          <div v-if="detailDialog.data.rejectReason" class="detail-item detail-item-full">
            <span class="detail-label">{{ TEXT.rejectReason }}</span>
            <div class="reason-box">{{ detailDialog.data.rejectReason }}</div>
          </div>
          <div class="detail-item detail-item-full">
            <span class="detail-label">{{ TEXT.proofMaterial }}</span>
            <div v-if="detailProofUrl" class="proof-panel">
              <el-image
                class="proof-image"
                :src="detailProofUrl"
                fit="cover"
                :preview-src-list="[detailProofUrl]"
                preview-teleported
              />
              <div class="proof-tip">点击图片可预览大图</div>
            </div>
            <div v-else class="proof-empty">{{ TEXT.noProofMaterial }}</div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialog.visible = false">{{ TEXT.close }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialog.visible" :title="TEXT.rejectDialogTitle" width="560px" destroy-on-close>
      <div class="reject-summary">
        <div>{{ TEXT.applyId }}: {{ rejectDialog.data?.id || "-" }}</div>
        <div>{{ TEXT.username }}: {{ rejectDialog.data?.username || "-" }}</div>
        <div>{{ TEXT.studentNo }}: {{ rejectDialog.data?.studentNo || "-" }}</div>
      </div>

      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="88px">
        <el-form-item :label="TEXT.rejectReason" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="5"
            maxlength="255"
            show-word-limit
            :placeholder="TEXT.rejectReasonPlaceholder"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rejectDialog.visible = false">{{ TEXT.cancel }}</el-button>
        <el-button type="primary" :loading="rejecting" @click="handleRejectSave">
          {{ TEXT.confirmReject }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import {
  apiAdminVerifyApprove,
  apiAdminVerifyPage,
  apiAdminVerifyReject,
  type AdminVerifyStatus,
} from "@/api/verify";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { getApiErrorMessage } from "@/utils/apiError";
import { imgUrl } from "@/utils/img";

const TEXT = {
  pageTitle: "\u8ba4\u8bc1\u5ba1\u6838\u7ba1\u7406",
  keywordPlaceholder: "\u7528\u6237\u540d / \u5b66\u53f7 / \u771f\u5b9e\u59d3\u540d",
  statusPlaceholder: "\u5ba1\u6838\u72b6\u6001",
  search: "\u67e5\u8be2",
  reset: "\u91cd\u7f6e",
  confirm: "\u786e\u5b9a",
  applyId: "\u7533\u8bf7ID",
  userId: "\u7528\u6237ID",
  username: "\u7528\u6237\u540d",
  realName: "\u771f\u5b9e\u59d3\u540d",
  studentNo: "\u5b66\u53f7",
  school: "\u5b66\u6821",
  currentStatus: "\u5f53\u524d\u72b6\u6001",
  reviewStatus: "\u5ba1\u6838\u72b6\u6001",
  createdAt: "\u63d0\u4ea4\u65f6\u95f4",
  actions: "\u64cd\u4f5c",
  approve: "\u901a\u8fc7",
  reject: "\u9a73\u56de",
  viewDetail: "\u67e5\u770b\u8be6\u60c5",
  detailDialogTitle: "\u8ba4\u8bc1\u7533\u8bf7\u8be6\u60c5",
  rejectDialogTitle: "\u9a73\u56de\u8ba4\u8bc1\u7533\u8bf7",
  rejectReason: "\u9a73\u56de\u539f\u56e0",
  proofMaterial: "\u8ba4\u8bc1\u6750\u6599",
  noProofMaterial: "\u672a\u4e0a\u4f20\u8ba4\u8bc1\u6750\u6599",
  rejectReasonPlaceholder: "\u8bf7\u8f93\u5165\u9a73\u56de\u539f\u56e0",
  confirmReject: "\u786e\u8ba4\u9a73\u56de",
  close: "\u5173\u95ed",
  cancel: "\u53d6\u6d88",
  pending: "\u5f85\u5ba1\u6838",
  approved: "\u5df2\u901a\u8fc7",
  rejected: "\u5df2\u9a73\u56de",
  loadListFailed: "\u52a0\u8f7d\u8ba4\u8bc1\u5ba1\u6838\u5217\u8868\u5931\u8d25",
  approveConfirmTitle: "\u63d0\u793a",
  approveSuccess: "\u8ba4\u8bc1\u5df2\u901a\u8fc7",
  approveFailed: "\u8ba4\u8bc1\u5ba1\u6838\u5931\u8d25",
  rejectRequired: "\u8bf7\u8f93\u5165\u9a73\u56de\u539f\u56e0",
  rejectRuleMessage: "\u9a73\u56de\u539f\u56e0\u957f\u5ea6\u9700\u4e3a 2-255 \u4e2a\u5b57\u7b26",
  rejectSuccess: "\u5df2\u9a73\u56de\u8ba4\u8bc1\u7533\u8bf7",
  rejectFailed: "\u9a73\u56de\u8ba4\u8bc1\u7533\u8bf7\u5931\u8d25",
} as const;

type VerifyRow = {
  id: number;
  userId: number;
  username: string;
  school: string;
  studentNo: string;
  realName: string;
  status: AdminVerifyStatus;
  rejectReason: string;
  proofUrl: string;
  createdAt: string;
};

const { runConfirmAction } = useConfirmAction();

const loading = ref(false);
const rejecting = ref(false);
const tableData = ref<VerifyRow[]>([]);
const rejectFormRef = ref<FormInstance>();

const query = reactive({
  keyword: "",
  status: "" as "" | AdminVerifyStatus,
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const detailDialog = reactive({
  visible: false,
  data: null as VerifyRow | null,
});

const rejectDialog = reactive({
  visible: false,
  data: null as VerifyRow | null,
});

const rejectForm = reactive({
  reason: "",
});

const rejectRules: FormRules = {
  reason: [
    { required: true, message: TEXT.rejectRequired, trigger: "blur" },
    { min: 2, max: 255, message: TEXT.rejectRuleMessage, trigger: "blur" },
  ],
};

const detailProofUrl = computed(() => imgUrl(detailDialog.data?.proofUrl || ""));

function normalizeStatus(status: unknown): AdminVerifyStatus {
  const text = String(status || "").toUpperCase();
  if (text === "APPROVED") return "APPROVED";
  if (text === "REJECTED") return "REJECTED";
  return "PENDING";
}

function stringValue(value: unknown) {
  return value == null ? "" : String(value);
}

function numberValue(value: unknown) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function mapRow(item: any): VerifyRow {
  return {
    id: numberValue(item?.id),
    userId: numberValue(item?.userId),
    username: stringValue(item?.username),
    school: stringValue(item?.school),
    studentNo: stringValue(item?.studentNo),
    realName: stringValue(item?.realName),
    status: normalizeStatus(item?.status),
    rejectReason: stringValue(item?.rejectReason),
    proofUrl: stringValue(item?.proofUrl || item?.proof_url || item?.imageUrl || item?.materialUrl),
    createdAt: stringValue(item?.createdAt),
  };
}

function formatTime(value?: string) {
  if (!value) return "-";
  const text = String(value).replace("T", " ");
  return text.length >= 19 ? text.slice(0, 19) : text;
}

function statusText(status: AdminVerifyStatus) {
  if (status === "APPROVED") return TEXT.approved;
  if (status === "REJECTED") return TEXT.rejected;
  return TEXT.pending;
}

function statusTagType(status: AdminVerifyStatus) {
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "warning";
}

async function fetchList() {
  loading.value = true;
  try {
    const data = await apiAdminVerifyPage({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword.trim() || undefined,
      status: query.status || undefined,
    });
    pagination.total = numberValue(data?.total);
    tableData.value = Array.isArray(data?.records) ? data.records.map((item) => mapRow(item)) : [];
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, TEXT.loadListFailed));
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.page = 1;
  fetchList();
}

function handleReset() {
  query.keyword = "";
  query.status = "";
  pagination.page = 1;
  fetchList();
}

function handleSizeChange(size: number) {
  pagination.size = size;
  pagination.page = 1;
  fetchList();
}

function handlePageChange(page: number) {
  pagination.page = page;
  fetchList();
}

function openDetail(row: VerifyRow) {
  detailDialog.data = { ...row };
  detailDialog.visible = true;
}

function openReject(row: VerifyRow) {
  rejectDialog.data = { ...row };
  rejectForm.reason = row.rejectReason || "";
  rejectDialog.visible = true;
}

async function handleApprove(row: VerifyRow) {
  await runConfirmAction({
    title: TEXT.approveConfirmTitle,
    message: `\u786e\u5b9a\u901a\u8fc7\u7528\u6237\u201c${row.username || row.userId}\u201d\u7684\u8ba4\u8bc1\u7533\u8bf7\u5417\uff1f`,
    type: "warning",
    confirmButtonText: TEXT.confirm,
    cancelButtonText: TEXT.cancel,
    successMessage: TEXT.approveSuccess,
    errorMessage: TEXT.approveFailed,
    action: () => apiAdminVerifyApprove(row.id),
    onSuccess: fetchList,
  });
}

async function handleRejectSave() {
  if (!rejectFormRef.value || !rejectDialog.data) return;
  const valid = await rejectFormRef.value.validate().catch(() => false);
  if (!valid) return;

  const reason = rejectForm.reason.trim();
  if (!reason) {
    ElMessage.warning(TEXT.rejectRequired);
    return;
  }

  rejecting.value = true;
  try {
    await runConfirmAction({
      title: TEXT.approveConfirmTitle,
      message: `\u786e\u5b9a\u9a73\u56de\u7533\u8bf7\u201c${rejectDialog.data.id}\u201d\u5417\uff1f`,
      type: "warning",
      confirmButtonText: TEXT.confirm,
      cancelButtonText: TEXT.cancel,
      successMessage: TEXT.rejectSuccess,
      errorMessage: TEXT.rejectFailed,
      action: () =>
        apiAdminVerifyReject({
          id: rejectDialog.data!.id,
          reason,
        }),
      onSuccess: async () => {
        rejectDialog.visible = false;
        rejectForm.reason = "";
        await fetchList();
      },
    });
  } finally {
    rejecting.value = false;
  }
}

onMounted(fetchList);
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

.toolbar,
.table-card {
  border: 1px solid #ddd;
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.keyword-input {
  width: 280px;
}

.status-select {
  width: 140px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.detail-wrap {
  color: #333;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.detail-item {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid #ececec;
  border-radius: 8px;
  background: #fafafa;
}

.detail-item-full {
  grid-column: 1 / -1;
}

.detail-label {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 12px;
}

.detail-item strong {
  color: #222;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.reason-box {
  padding: 10px 12px;
  border: 1px dashed #ccc;
  background: #fff;
  color: #444;
  line-height: 1.6;
}

.proof-panel {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.proof-image {
  width: 280px;
  height: 176px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  overflow: hidden;
}

.proof-tip {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.6;
}

.proof-empty {
  padding: 14px 16px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  color: #9ca3af;
  background: #fff;
  font-size: 13px;
}

.reject-summary {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px dashed #d1d5db;
  background: #fafafa;
  color: #444;
  line-height: 1.8;
}
</style>
