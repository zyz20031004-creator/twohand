<template>
  <div class="page-wrap">
    <section class="page-card" v-loading="loading">
      <header class="header-row">
        <div class="header-copy">
          <span class="header-badge">举报记录</span>
          <h2 class="header-title">我的举报</h2>
          <p class="header-desc">查看你提交过的举报记录及处理结果。</p>
        </div>

        <div class="header-metrics">
          <div class="metric-chip">
            <span>举报总数</span>
            <strong>{{ total }} 条</strong>
          </div>
          <div class="metric-chip soft">
            <span>当前页</span>
            <strong>{{ tableData.length }} 条</strong>
          </div>
        </div>
      </header>

      <section class="content-board">
        <div class="board-head">
          <div class="board-copy">
            <h3 class="board-title">举报记录</h3>
            <p class="board-desc">只展示你本人提交过的商品举报，处理进度和管理员反馈会同步更新。</p>
          </div>

          <div class="board-chip">
            <span>分页大小</span>
            <strong>{{ query.size }} 条</strong>
          </div>
        </div>

        <ListDataState
          :loading="loading"
          :empty="tableData.length === 0"
          :error-message="errorMessage"
          empty-text="暂无举报记录，你还没有提交过举报"
          @retry="load"
        >
          <div class="report-list">
            <article v-for="row in tableData" :key="row.id" class="report-card">
              <div class="report-card-left">
                <button v-if="row.hasProduct" type="button" class="cover-btn" @click="goProduct(row.productId)">
                  <img :src="coverUrl(row.productCoverUrl)" alt="" class="cover-image" />
                </button>
                <div v-else class="cover-placeholder">
                  <span>商品状态</span>
                  <strong>已下架</strong>
                </div>
              </div>

              <div class="report-card-main">
                <div class="report-title-row">
                  <button
                    v-if="row.hasProduct"
                    type="button"
                    class="product-link"
                    @click="goProduct(row.productId)"
                  >
                    {{ productTitle(row) }}
                  </button>
                  <span v-else class="product-link is-disabled">{{ productTitle(row) }}</span>
                </div>

                <div class="report-meta">
                  <span class="serial-chip">举报编号 #{{ row.id }}</span>
                  <el-tag :type="statusTagType(row.status)" effect="light" class="state-tag">
                    {{ statusText(row.status) }}
                  </el-tag>
                </div>

                <div class="report-fields">
                  <div class="field-item">
                    <span class="field-label">举报原因</span>
                    <p class="field-value strong-text">{{ displayText(row.reason, "未填写举报原因") }}</p>
                  </div>

                  <div class="field-item">
                    <span class="field-label">补充说明</span>
                    <p class="field-value subdued-text">{{ displayText(row.detail, "无补充说明") }}</p>
                  </div>
                </div>

                <div class="report-time">
                  <span>举报时间 {{ formatTime(row.createdAt) }}</span>
                  <span v-if="row.handledAt">处理时间 {{ formatTime(row.handledAt) }}</span>
                </div>
              </div>

              <div class="report-card-side">
                <div class="report-result">
                  <span class="result-label">处理结果</span>
                  <p class="result-text">{{ handleResultText(row) }}</p>
                </div>

                <div class="report-actions">
                  <el-button
                    v-if="row.hasProduct"
                    class="action-btn view-btn"
                    @click="goProduct(row.productId)"
                  >
                    查看商品
                  </el-button>
                  <div v-else class="action-placeholder">该商品已下架或删除</div>
                </div>
              </div>
            </article>
          </div>
        </ListDataState>

        <div v-if="!loading && !errorMessage && total > 0" class="pager">
          <div class="pager-copy">共 {{ total }} 条举报记录</div>
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="query.size"
            :current-page="query.page"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import ListDataState from "@/components/common/ListDataState.vue";
import { apiMyReportPage, type ReportStatus } from "@/api/report";
import { useAuthUser } from "@/composables/useAuthUser";
import { usePagedQuery } from "@/composables/usePagedQuery";
import { getApiErrorMessage } from "@/utils/apiError";
import { productImgUrl as resolveProductImgUrl } from "@/utils/img";

type Row = {
  id: number;
  productId: number;
  productTitle: string;
  productCoverUrl: string;
  hasProduct: boolean;
  reason: string;
  detail: string;
  status: ReportStatus;
  handleRemark: string;
  handledAt: string;
  createdAt: string;
};

const router = useRouter();
const { requireLogin } = useAuthUser();

const loading = ref(false);
const errorMessage = ref("");
const tableData = ref<Row[]>([]);
const total = ref(0);
const { query, changePage } = usePagedQuery({
  page: 1,
  size: 8,
});

function stringValue(value: unknown) {
  return value == null ? "" : String(value).trim();
}

function numberValue(value: unknown) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function booleanValue(value: unknown) {
  if (typeof value === "boolean") return value;
  const num = Number(value);
  if (Number.isFinite(num)) return num > 0;
  const text = stringValue(value).toLowerCase();
  return text === "true" || text === "yes";
}

function normalizeStatus(value: unknown): ReportStatus {
  const text = stringValue(value).toUpperCase();
  if (text === "VALID") return "VALID";
  if (text === "INVALID") return "INVALID";
  if (text === "HANDLED") return "HANDLED";
  return "PENDING";
}

function mapRow(item: any): Row {
  const title = stringValue(item?.productTitle);
  return {
    id: numberValue(item?.id),
    productId: numberValue(item?.productId),
    productTitle: title,
    productCoverUrl: stringValue(item?.productCoverUrl),
    hasProduct: booleanValue(item?.productExists) || !!title,
    reason: stringValue(item?.reason),
    detail: stringValue(item?.detail),
    status: normalizeStatus(item?.status),
    handleRemark: stringValue(item?.handleRemark),
    handledAt: stringValue(item?.handledAt),
    createdAt: stringValue(item?.createdAt),
  };
}

function statusText(status: ReportStatus) {
  if (status === "VALID") return "举报有效";
  if (status === "INVALID") return "已驳回";
  if (status === "HANDLED") return "已处理";
  return "待处理";
}

function statusTagType(status: ReportStatus) {
  if (status === "VALID") return "success";
  if (status === "INVALID") return "danger";
  if (status === "HANDLED") return "info";
  return "warning";
}

function displayText(value?: string | null, fallback = "-") {
  const text = stringValue(value);
  return text || fallback;
}

function formatTime(value?: string | null) {
  const text = stringValue(value).replace("T", " ");
  if (!text) return "-";
  return text.length >= 19 ? text.slice(0, 19) : text;
}

function coverUrl(url?: string | null) {
  return resolveProductImgUrl(url || undefined);
}

function productTitle(row: Row) {
  return row.hasProduct ? displayText(row.productTitle, `商品 #${row.productId}`) : "该商品已下架或删除";
}

function handleResultText(row: Row) {
  if (row.handleRemark) return row.handleRemark;
  if (row.status === "VALID") return "平台已判定该举报有效，正在继续跟进处理。";
  if (row.status === "INVALID") return "平台已驳回该举报，感谢你的反馈。";
  if (row.status === "HANDLED") return "管理员已完成处理。";
  return "管理员暂未处理";
}

function goProduct(productId: number) {
  if (!productId) return;
  router.push(`/user/hot/product/${productId}`);
}

function handlePageChange(page: number) {
  changePage(page, load);
}

async function load() {
  const userId = requireLogin("请先登录后查看我的举报");
  if (!userId) {
    tableData.value = [];
    total.value = 0;
    errorMessage.value = "";
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  try {
    const res = await apiMyReportPage({
      page: query.page,
      size: query.size,
    });
    tableData.value = Array.isArray(res?.records) ? res.records.map(mapRow) : [];
    total.value = numberValue(res?.total);
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "获取举报记录失败");
    tableData.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
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

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: #3b82f6;
  font-size: 11px;
  font-weight: 700;
}

.header-title {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.14;
  font-weight: 800;
}

.header-desc {
  margin: 0;
  max-width: 560px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.68;
}

.header-metrics {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(191, 219, 254, 0.88);
  background: rgba(239, 246, 255, 0.9);
}

.metric-chip.soft {
  border-color: rgba(220, 252, 231, 0.88);
  background: rgba(240, 253, 244, 0.9);
}

.metric-chip span {
  color: #7b8aa3;
  font-size: 11px;
}

.metric-chip strong {
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.metric-chip.soft strong {
  color: #15803d;
}

.content-board {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid #edf2f7;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.board-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.board-copy {
  min-width: 0;
}

.board-title {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.board-desc {
  margin: 6px 0 0;
  color: #7b8aa3;
  font-size: 13px;
  line-height: 1.65;
}

.board-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(191, 219, 254, 0.88);
  background: rgba(239, 246, 255, 0.92);
  white-space: nowrap;
}

.board-chip span {
  color: #7b8aa3;
  font-size: 11px;
}

.board-chip strong {
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.report-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-card {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) 208px;
  gap: 16px;
  align-items: start;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid #e8eef6;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.04), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(249, 251, 255, 0.98) 100%);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.report-card:hover {
  transform: translateY(-2px);
  border-color: #dbeafe;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.report-card-left,
.report-card-main,
.report-card-side {
  min-width: 0;
}

.cover-btn {
  width: 96px;
  height: 74px;
  padding: 0;
  border: 1px solid rgba(226, 232, 240, 0.88);
  border-radius: 16px;
  background: #f8fbff;
  overflow: hidden;
  cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(248, 250, 252, 0.9);
}

.cover-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.cover-placeholder {
  width: 96px;
  height: 74px;
  border-radius: 16px;
  border: 1px dashed rgba(203, 213, 225, 0.9);
  background: rgba(248, 251, 255, 0.78);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  text-align: center;
}

.cover-placeholder span {
  color: #94a3b8;
  font-size: 10px;
  font-weight: 700;
}

.cover-placeholder strong {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.report-card-main {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 2px;
}

.report-title-row {
  min-width: 0;
}

.product-link {
  padding: 0;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.42;
  text-align: left;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  cursor: pointer;
  transition: color 0.2s ease;
}

.product-link:hover {
  color: #2563eb;
}

.product-link.is-disabled {
  color: #64748b;
  cursor: default;
}

.report-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.serial-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 11px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  background: rgba(248, 251, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.state-tag {
  border-radius: 999px;
  font-weight: 700;
  padding: 0 2px;
}

.report-fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(232, 238, 246, 0.92);
  background: rgba(248, 251, 255, 0.9);
}

.field-item {
  min-width: 0;
}

.field-item + .field-item {
  padding-top: 10px;
  border-top: 1px dashed rgba(203, 213, 225, 0.9);
}

.field-label {
  display: block;
  margin-bottom: 4px;
  color: #7b8aa3;
  font-size: 12px;
  font-weight: 700;
}

.field-value {
  margin: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.66;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.strong-text {
  color: #334155;
  font-weight: 600;
}

.subdued-text {
  color: #64748b;
}

.report-time {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.report-card-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: stretch;
}

.report-result {
  padding: 12px 13px;
  border-radius: 16px;
  border: 1px solid rgba(187, 247, 208, 0.8);
  background: linear-gradient(180deg, rgba(244, 252, 247, 0.96) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.result-label {
  display: inline-block;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.result-text {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  overflow: hidden;
}

.report-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  width: 100%;
  min-width: 104px;
  height: 36px;
  border-radius: 999px;
  font-weight: 700;
}

.view-btn {
  border-color: rgba(191, 219, 254, 0.92);
  color: #2563eb;
  background: rgba(239, 246, 255, 0.92);
}

.action-placeholder {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.88);
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.6;
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

@media (max-width: 1120px) {
  .header-row,
  .board-head {
    flex-direction: column;
  }

  .report-card {
    grid-template-columns: 96px minmax(0, 1fr);
  }

  .report-card-side {
    grid-column: 2;
    flex-direction: row;
    align-items: stretch;
  }

  .report-result {
    flex: 1 1 0;
  }

  .report-actions {
    width: 168px;
    flex: 0 0 168px;
  }
}

@media (max-width: 760px) {
  .page-wrap {
    padding: 10px;
  }

  .page-card,
  .content-board,
  .report-card {
    padding: 16px;
    border-radius: 18px;
  }

  .header-title {
    font-size: 24px;
  }

  .report-card {
    grid-template-columns: 1fr;
  }

  .cover-btn,
  .cover-placeholder {
    width: 100%;
    height: 156px;
  }

  .report-card-side {
    grid-column: auto;
    flex-direction: column;
  }

  .report-actions {
    width: 100%;
    flex-basis: auto;
  }

  .pager {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
