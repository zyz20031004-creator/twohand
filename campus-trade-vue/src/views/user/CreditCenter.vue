<template>
  <div class="page-wrap" v-loading="loading">
    <section class="page-card">
      <header class="page-head">
        <div class="head-copy">
          <h2 class="page-title">信誉积分</h2>
          <p class="page-desc">查看当前信誉积分和历史变动记录。</p>
        </div>
      </header>

      <section class="score-card">
        <span class="score-label">当前信誉积分</span>
        <strong>{{ score }}</strong>
        <p>{{ scoreHint }}</p>
      </section>

      <section class="section-card">
        <div class="section-head">
          <div>
            <h3 class="section-title">信誉记录</h3>
            <p class="section-desc">按时间查看每一次积分变化和对应说明。</p>
          </div>
          <div class="head-chip">
            <span>累计记录</span>
            <strong>{{ total }} 条</strong>
          </div>
        </div>

        <ListDataState
          :loading="loading"
          :empty="records.length === 0"
          :error-message="errorMessage"
          empty-text="暂无信誉记录"
          @retry="loadData"
        >
          <div class="table-shell">
            <el-table :data="records" class="credit-table">
              <el-table-column label="类型" min-width="180">
                <template #default="{ row }">
                  <div class="reason-cell">{{ formatReason(row.reason) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="积分变化" min-width="120" align="center">
                <template #default="{ row }">
                  <span :class="Number(row.changeVal) >= 0 ? 'delta-up' : 'delta-down'">
                    {{ formatDelta(row.changeVal) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="300">
                <template #default="{ row }">
                  <div class="remark-cell">{{ formatRemark(row) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="时间" min-width="170">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </ListDataState>

        <div v-if="!loading && !errorMessage && total > 0" class="pager">
          <div class="pager-copy">共 {{ total }} 条积分记录</div>
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            background
            :total="total"
            layout="prev, pager, next"
            @current-change="loadData"
          />
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { apiCreditMy } from "@/api/credit";
import ListDataState from "@/components/common/ListDataState.vue";
import { getApiErrorMessage } from "@/utils/apiError";

const loading = ref(false);
const errorMessage = ref("");
const score = ref(100);
const records = ref<any[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

const scoreHint = computed(() => {
  if (score.value >= 120) return "当前信用表现稳定，继续保持良好的交易与互动习惯。";
  if (score.value >= 100) return "当前信用状态良好，认证和正常交易会继续帮助你积累信用。";
  if (score.value >= 80) return "继续完成认证、交易和评价，可以逐步提升个人信用表现。";
  return "建议先完善认证资料，并保持规范的交易与互动行为。";
});

function formatReason(reason?: string) {
  if (reason === "ORDER_FINISH" || reason === "ORDER_FINISHED") return "交易完成加分";
  if (reason === "VERIFY_APPROVED") return "认证通过加分";
  if (reason === "ADMIN_ADJUST") return "平台人工调整";
  return reason || "-";
}

function formatRemark(row: any) {
  const reason = String(row?.reason || "").trim().toUpperCase();
  const remark = String(row?.remark || "").trim();
  if (reason === "ORDER_FINISH" || reason === "ORDER_FINISHED" || remark === "order finish reward") {
    return "交易完成加分";
  }
  if (reason === "VERIFY_APPROVED") {
    return "校园认证通过";
  }
  return remark || "暂无备注说明";
}

function formatTime(value?: string) {
  if (!value) return "-";
  const text = String(value).replace("T", " ");
  return text.length >= 19 ? text.slice(0, 19) : text;
}

function formatDelta(value: unknown) {
  const delta = Number(value ?? 0);
  if (!Number.isFinite(delta)) return "0";
  return delta > 0 ? `+${delta}` : String(delta);
}

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await apiCreditMy({ page: page.value, size: size.value });
    score.value = data?.score ?? 100;
    records.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "加载信用记录失败");
    records.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
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
  padding: 22px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.page-head {
  margin-bottom: 16px;
}

.head-copy {
  min-width: 0;
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.14;
  font-weight: 800;
}

.page-desc {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.68;
}

.score-card,
.section-card {
  border: 1px solid #edf2f7;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.score-card {
  margin-bottom: 18px;
  padding: 20px 24px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.08), transparent 30%),
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.score-label {
  display: block;
  color: #7b8aa3;
  font-size: 12px;
  font-weight: 700;
}

.score-card strong {
  display: block;
  margin-top: 12px;
  color: #1d4ed8;
  font-size: 48px;
  line-height: 1;
  font-weight: 800;
}

.score-card p {
  margin: 12px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.68;
}

.section-card {
  padding: 20px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.section-desc {
  margin: 6px 0 0;
  color: #7b8aa3;
  font-size: 13px;
  line-height: 1.68;
}

.head-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 40px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  background: rgba(239, 246, 255, 0.96);
  white-space: nowrap;
}

.head-chip span {
  color: #7b8aa3;
  font-size: 11px;
}

.head-chip strong {
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.table-shell {
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid #e7eef8;
  background: #ffffff;
}

.reason-cell {
  color: #0f172a;
  font-weight: 700;
}

.remark-cell {
  color: #64748b;
  line-height: 1.68;
  white-space: pre-wrap;
  word-break: break-word;
}

.delta-up,
.delta-down {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
}

.delta-up {
  color: #15803d;
  background: rgba(220, 252, 231, 0.9);
}

.delta-down {
  color: #dc2626;
  background: rgba(254, 226, 226, 0.92);
}

.pager {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pager-copy {
  color: #64748b;
  font-size: 12px;
}

:deep(.credit-table) {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: #f8fbff;
  --el-table-row-hover-bg-color: #f8fbff;
}

:deep(.credit-table .el-table__cell) {
  padding: 14px 0;
}

:deep(.credit-table th.el-table__cell) {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

:deep(.credit-table td.el-table__cell) {
  color: #475569;
}

@media (max-width: 760px) {
  .page-wrap {
    padding: 10px;
  }

  .page-card,
  .score-card,
  .section-card {
    padding: 16px;
    border-radius: 18px;
  }

  .page-title {
    font-size: 24px;
  }

  .score-card strong {
    font-size: 42px;
  }

  .section-head,
  .pager {
    flex-direction: column;
    align-items: flex-start;
  }

  .table-shell {
    overflow-x: auto;
  }
}
</style>
