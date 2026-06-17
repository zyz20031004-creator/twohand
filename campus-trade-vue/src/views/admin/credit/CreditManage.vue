<template>
  <div class="page">
    <div class="page-title">信用管理</div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 用户ID"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.verifyStatus" placeholder="认证状态" clearable class="status-select">
          <el-option label="未认证" value="UNVERIFIED" />
          <el-option label="审核中" value="PENDING" />
          <el-option label="已认证" value="VERIFIED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-input-number v-model="query.minScore" :min="0" :max="200" :step="1" class="score-input" />
        <span class="range-separator">至</span>
        <el-input-number v-model="query.maxScore" :min="0" :max="200" :step="1" class="score-input" />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="用户ID" width="90" align="center" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="name" label="昵称" width="130" align="center" />
        <el-table-column prop="realName" label="真实姓名" width="120" align="center" />
        <el-table-column prop="school" label="学校" min-width="180" show-overflow-tooltip />

        <el-table-column label="当前信誉分" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="scoreTagType(row.creditScore)" effect="plain">
              {{ row.creditScore }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="认证状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="verifyTagType(row.verifyStatus)" effect="plain">
              {{ verifyText(row.verifyStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最近更新时间" width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openLogs(row)">查看记录</el-button>
            <el-button type="warning" link @click="openAdjust(row)">调整信用</el-button>
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

    <el-dialog v-model="adjustDialog.visible" title="调整信用分" width="540px" destroy-on-close>
      <div class="adjust-summary">
        <div>用户ID：{{ adjustDialog.user?.id || "-" }}</div>
        <div>用户名：{{ adjustDialog.user?.username || "-" }}</div>
        <div>当前信用分：{{ adjustDialog.user?.creditScore ?? "-" }}</div>
      </div>

      <el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-width="90px">
        <el-form-item label="调整分值" prop="delta">
          <el-input-number
            v-model="adjustForm.delta"
            :min="-200"
            :max="200"
            :step="1"
            class="adjust-input"
          />
          <div class="field-tip">支持正数加分、负数减分，不能为 0。</div>
        </el-form-item>
        <el-form-item label="调整原因" prop="remark">
          <el-input
            v-model="adjustForm.remark"
            type="textarea"
            :rows="5"
            maxlength="64"
            show-word-limit
            placeholder="请输入调整原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="adjustDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="handleAdjustSave">确认调整</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logDrawer.visible" title="信用变动记录" size="760px">
      <div class="drawer-head">
        <div class="drawer-user">
          <div class="drawer-name">{{ logDrawer.user?.name || logDrawer.user?.username || "用户" }}</div>
          <div class="drawer-meta">
            <span>用户ID：{{ logDrawer.user?.id || "-" }}</span>
            <span>用户名：{{ logDrawer.user?.username || "-" }}</span>
            <span>当前信用分：{{ logDrawer.score }}</span>
          </div>
        </div>
      </div>

      <div v-loading="logDrawer.loading" class="drawer-body">
        <el-empty v-if="!logDrawer.loading && logDrawer.records.length === 0" description="暂无信用记录" />

        <template v-else>
          <el-table :data="logDrawer.records" border stripe>
            <el-table-column label="变动分值" width="110" align="center">
              <template #default="{ row }">
                <span :class="Number(row.changeVal) >= 0 ? 'delta-up' : 'delta-down'">
                  {{ formatDelta(row.changeVal) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="原因" width="140" align="center">
              <template #default="{ row }">{{ formatReason(row.reason) }}</template>
            </el-table-column>
            <el-table-column label="备注" min-width="240">
              <template #default="{ row }">
                <div class="remark-cell">{{ formatRemark(row) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作人" width="120" align="center">
              <template #default="{ row }">{{ row.operatorName || "-" }}</template>
            </el-table-column>
            <el-table-column label="时间" width="170" align="center">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>

          <div class="pager drawer-pager">
            <el-pagination
              v-model:current-page="logDrawer.page"
              v-model:page-size="logDrawer.size"
              :total="logDrawer.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="handleLogSizeChange"
              @current-change="handleLogPageChange"
            />
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import {
  apiAdminCreditAdjust,
  apiAdminCreditLogs,
  apiAdminCreditPage,
  type AdminCreditUserItem,
} from "@/api/credit";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { getApiErrorMessage } from "@/utils/apiError";

type VerifyStatus = "UNVERIFIED" | "PENDING" | "VERIFIED" | "REJECTED";

type CreditRow = AdminCreditUserItem & {
  id: number;
  username: string;
  name: string;
  realName: string;
  school: string;
  verifyStatus: VerifyStatus;
  creditScore: number;
  updatedAt: string;
};

type CreditLogRow = {
  id: number;
  userId: number;
  changeVal: number;
  reason: string;
  bizType?: string;
  bizId?: number;
  remark?: string;
  operatorName?: string | null;
  createdAt?: string;
};

const { runConfirmAction } = useConfirmAction();

const loading = ref(false);
const adjusting = ref(false);
const tableData = ref<CreditRow[]>([]);
const adjustFormRef = ref<FormInstance>();

const query = reactive({
  keyword: "",
  verifyStatus: "" as "" | VerifyStatus,
  minScore: undefined as number | undefined,
  maxScore: undefined as number | undefined,
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const adjustDialog = reactive({
  visible: false,
  user: null as CreditRow | null,
});

const adjustForm = reactive({
  delta: 0,
  remark: "",
});

const logDrawer = reactive({
  visible: false,
  loading: false,
  user: null as CreditRow | null,
  score: 100,
  records: [] as CreditLogRow[],
  total: 0,
  page: 1,
  size: 10,
});

const adjustRules: FormRules = {
  delta: [
    {
      validator: (_rule, value, callback) => {
        if (typeof value !== "number" || !Number.isFinite(value) || value === 0) {
          callback(new Error("请输入有效的调整分值"));
          return;
        }
        callback();
      },
      trigger: "change",
    },
  ],
  remark: [
    { required: true, message: "请输入调整原因", trigger: "blur" },
    { min: 2, max: 64, message: "调整原因长度需在 2 到 64 个字符之间", trigger: "blur" },
  ],
};

function numberValue(value: unknown, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

function stringValue(value: unknown) {
  return value == null ? "" : String(value);
}

function normalizeVerifyStatus(status: unknown): VerifyStatus {
  const text = String(status || "").toUpperCase();
  if (text === "PENDING") return "PENDING";
  if (text === "VERIFIED") return "VERIFIED";
  if (text === "REJECTED") return "REJECTED";
  return "UNVERIFIED";
}

function mapRow(item: any): CreditRow {
  return {
    id: numberValue(item?.id),
    username: stringValue(item?.username),
    name: stringValue(item?.name),
    realName: stringValue(item?.realName),
    school: stringValue(item?.school),
    verifyStatus: normalizeVerifyStatus(item?.verifyStatus),
    creditScore: numberValue(item?.creditScore, 100),
    updatedAt: stringValue(item?.updatedAt),
  };
}

function mapLog(item: any): CreditLogRow {
  return {
    id: numberValue(item?.id),
    userId: numberValue(item?.userId),
    changeVal: numberValue(item?.changeVal),
    reason: stringValue(item?.reason),
    bizType: stringValue(item?.bizType) || undefined,
    bizId: item?.bizId == null ? undefined : numberValue(item?.bizId),
    remark: stringValue(item?.remark) || undefined,
    operatorName: stringValue(item?.operatorName) || null,
    createdAt: stringValue(item?.createdAt) || undefined,
  };
}

function formatTime(value?: string) {
  if (!value) return "-";
  const text = String(value).replace("T", " ");
  return text.length >= 19 ? text.slice(0, 19) : text;
}

function verifyText(status: VerifyStatus) {
  if (status === "VERIFIED") return "已认证";
  if (status === "PENDING") return "审核中";
  if (status === "REJECTED") return "已驳回";
  return "未认证";
}

function verifyTagType(status: VerifyStatus) {
  if (status === "VERIFIED") return "success";
  if (status === "PENDING") return "warning";
  if (status === "REJECTED") return "danger";
  return "info";
}

function scoreTagType(score: number) {
  if (score >= 120) return "success";
  if (score >= 80) return "warning";
  return "danger";
}

function formatDelta(value: unknown) {
  const delta = numberValue(value);
  return delta > 0 ? `+${delta}` : String(delta);
}

function formatReason(reason?: string) {
  if (reason === "ORDER_FINISH" || reason === "ORDER_FINISHED") return "交易完成加分";
  if (reason === "VERIFY_APPROVED") return "认证通过";
  if (reason === "ADMIN_ADJUST") return "人工调整";
  return reason || "-";
}

function formatRemark(row: CreditLogRow) {
  const reason = String(row?.reason || "").trim().toUpperCase();
  const remark = String(row?.remark || "").trim();
  if (reason === "ORDER_FINISH" || reason === "ORDER_FINISHED" || remark === "order finish reward") {
    return "交易完成加分";
  }
  if (reason === "VERIFY_APPROVED") {
    return "校园认证通过";
  }
  return remark || "-";
}

async function fetchList() {
  loading.value = true;
  try {
    if (
      typeof query.minScore === "number" &&
      typeof query.maxScore === "number" &&
      query.minScore > query.maxScore
    ) {
      ElMessage.warning("最低分不能大于最高分");
      loading.value = false;
      return;
    }

    const data = await apiAdminCreditPage({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword.trim() || undefined,
      verifyStatus: query.verifyStatus || undefined,
      minScore: query.minScore,
      maxScore: query.maxScore,
    });
    pagination.total = numberValue(data?.total);
    tableData.value = Array.isArray(data?.records) ? data.records.map(mapRow) : [];
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "加载信用列表失败"));
  } finally {
    loading.value = false;
  }
}

async function loadLogs() {
  if (!logDrawer.user) return;
  logDrawer.loading = true;
  try {
    const data = await apiAdminCreditLogs({
      userId: logDrawer.user.id,
      page: logDrawer.page,
      size: logDrawer.size,
    });
    logDrawer.score = numberValue(data?.score, logDrawer.user.creditScore);
    logDrawer.total = numberValue(data?.total);
    logDrawer.records = Array.isArray(data?.records) ? data.records.map(mapLog) : [];
    if (logDrawer.user) {
      logDrawer.user.creditScore = logDrawer.score;
      logDrawer.user.username = stringValue(data?.username) || logDrawer.user.username;
      logDrawer.user.name = stringValue(data?.name) || logDrawer.user.name;
    }
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "加载信用记录失败"));
    logDrawer.records = [];
    logDrawer.total = 0;
  } finally {
    logDrawer.loading = false;
  }
}

function handleSearch() {
  pagination.page = 1;
  fetchList();
}

function handleReset() {
  query.keyword = "";
  query.verifyStatus = "";
  query.minScore = undefined;
  query.maxScore = undefined;
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

function openLogs(row: CreditRow) {
  logDrawer.user = { ...row };
  logDrawer.score = row.creditScore;
  logDrawer.page = 1;
  logDrawer.size = 10;
  logDrawer.visible = true;
  loadLogs();
}

function handleLogSizeChange(size: number) {
  logDrawer.size = size;
  logDrawer.page = 1;
  loadLogs();
}

function handleLogPageChange(page: number) {
  logDrawer.page = page;
  loadLogs();
}

function openAdjust(row: CreditRow) {
  adjustDialog.user = { ...row };
  adjustForm.delta = 0;
  adjustForm.remark = "";
  adjustDialog.visible = true;
}

async function handleAdjustSave() {
  if (!adjustFormRef.value || !adjustDialog.user) return;
  const valid = await adjustFormRef.value.validate().catch(() => false);
  if (!valid) return;

  const delta = Number(adjustForm.delta);
  const remark = adjustForm.remark.trim();
  if (!delta) {
    ElMessage.warning("调整分值不能为 0");
    return;
  }
  if (!remark) {
    ElMessage.warning("请输入调整原因");
    return;
  }

  adjusting.value = true;
  try {
    await runConfirmAction({
      title: "提示",
      message: `确定将用户「${adjustDialog.user.username}」的信用分调整 ${formatDelta(delta)} 吗？`,
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      successMessage: "信用分调整成功",
      errorMessage: "信用分调整失败",
      action: () =>
        apiAdminCreditAdjust({
          userId: adjustDialog.user!.id,
          delta,
          remark,
        }),
      onSuccess: async () => {
        adjustDialog.visible = false;
        await fetchList();
        if (logDrawer.visible && logDrawer.user && logDrawer.user.id === adjustDialog.user!.id) {
          await loadLogs();
        }
      },
    });
  } finally {
    adjusting.value = false;
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
  width: 240px;
}

.status-select {
  width: 140px;
}

.score-input {
  width: 120px;
}

.range-separator {
  color: #666;
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.adjust-summary {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px dashed #d1d5db;
  background: #fafafa;
  color: #444;
  line-height: 1.8;
}

.adjust-input {
  width: 180px;
}

.field-tip {
  margin-top: 8px;
  color: #666;
  font-size: 12px;
  line-height: 1.6;
}

.drawer-head {
  margin-bottom: 16px;
}

.drawer-name {
  color: #222;
  font-size: 18px;
  font-weight: 600;
}

.drawer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 8px;
  color: #666;
  font-size: 13px;
}

.drawer-body {
  min-height: 220px;
}

.drawer-pager {
  margin-top: 12px;
}

.remark-cell {
  color: #444;
  line-height: 1.6;
  word-break: break-word;
}

.delta-up,
.delta-down {
  display: inline-flex;
  min-width: 62px;
  justify-content: center;
  font-weight: 600;
}

.delta-up {
  color: #1f8b4c;
}

.delta-down {
  color: #c45656;
}
</style>
