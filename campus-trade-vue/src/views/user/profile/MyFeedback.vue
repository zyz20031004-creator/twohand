<template>
  <div class="page-wrap">
    <section class="page-card" v-loading="loading">
      <header class="header-row">
        <div class="header-copy">
          <span class="header-badge">反馈中心</span>
          <h2 class="header-title">我的反馈</h2>
          <p class="header-desc">查看你提交的问题、建议和处理进度，方便持续跟进平台回复与历史记录。</p>
        </div>

        <div class="header-metrics">
          <div class="metric-chip">
            <span>反馈总数</span>
            <strong>{{ total }} 条</strong>
          </div>
          <div class="metric-chip soft">
            <span>当前筛选</span>
            <strong>{{ currentStatusLabel }}</strong>
          </div>
        </div>
      </header>

      <div class="toolbar-panel">
        <div class="toolbar-left">
          <el-input
            v-model="query.keyword"
            placeholder="搜索反馈标题"
            clearable
            class="search-input"
            @keyup.enter="load"
          />
          <el-select
            v-model="query.status"
            placeholder="全部状态"
            clearable
            class="filter-select"
            @change="handleFilterChange"
          >
            <el-option label="处理中" value="OPEN" />
            <el-option label="已完成" value="CLOSED" />
          </el-select>
          <el-button type="primary" class="toolbar-btn" @click="load">查询</el-button>
          <el-button class="toolbar-btn light-btn" @click="reset">重置</el-button>
        </div>

        <div class="toolbar-right">
          <el-button type="primary" class="toolbar-btn primary-btn" @click="openCreate">我要反馈</el-button>
        </div>
      </div>

      <section class="content-board">
        <div class="board-head">
          <div>
            <h3 class="board-title">反馈记录</h3>
            <p class="board-desc">这里会记录你提交给平台的所有反馈，以及对应的处理状态和平台回复。</p>
          </div>
          <div class="board-chip">
            <span>当前页</span>
            <strong>{{ tableData.length }} 条</strong>
          </div>
        </div>

        <ListDataState
          :loading="loading"
          :empty="tableData.length === 0"
          :error-message="errorMessage"
          empty-text="还没有反馈记录，遇到问题或有建议时可以随时提交"
          @retry="load"
        >
          <div class="feedback-list">
            <article v-for="row in tableData" :key="row.id" class="feedback-card">
              <div class="card-head">
                <div class="title-wrap">
                  <div class="title-line">
                    <h3 class="feedback-title">{{ getDisplayText(row.subject, "未命名反馈") }}</h3>
                    <el-tag :type="getStatusTagType(row.status)" effect="light" class="state-tag">
                      {{ getStatusText(row.status) }}
                    </el-tag>
                  </div>

                  <div class="meta-line">
                    <span class="meta-item">提交时间 {{ row.createdAt || "-" }}</span>
                    <span class="meta-item">联系方式 {{ row.contact || "-" }}</span>
                    <span v-if="row.email" class="meta-item">邮箱 {{ row.email }}</span>
                  </div>
                </div>
              </div>

              <div class="feedback-body">
                <div class="content-panel">
                  <span class="panel-label">反馈内容</span>
                  <p>{{ getDisplayText(row.content, "暂无反馈内容") }}</p>
                </div>

                <div class="content-panel reply-panel">
                  <span class="panel-label">平台回复</span>
                  <p>{{ getReplyText(row.reply) }}</p>
                </div>
              </div>

              <div class="action-row">
                <el-button class="action-btn view-btn" @click="openDetail(row)">查看详情</el-button>
                <el-button class="action-btn danger-btn" @click="removeOne(row.id)">删除</el-button>
              </div>
            </article>
          </div>
        </ListDataState>

        <div v-if="!loading && !errorMessage && total > 0" class="pager">
          <div class="pager-copy">共 {{ total }} 条反馈记录</div>
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

      <el-dialog v-model="detail.visible" title="反馈详情" width="760px" align-center>
        <div class="detail-card">
          <div class="detail-row">
            <span class="detail-label">主题</span>
            <div class="detail-value">{{ getDisplayText(detail.row?.subject, "-") }}</div>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态</span>
            <div class="detail-value">
              <el-tag :type="getStatusTagType(detail.row?.status)" effect="light">
                {{ getStatusText(detail.row?.status) }}
              </el-tag>
            </div>
          </div>
          <div class="detail-row">
            <span class="detail-label">内容</span>
            <div class="detail-block">{{ getDisplayText(detail.row?.content, "-") }}</div>
          </div>
          <div class="detail-row">
            <span class="detail-label">平台回复</span>
            <div class="detail-block reply-detail">{{ getReplyText(detail.row?.reply) }}</div>
          </div>
        </div>

        <template #footer>
          <el-button @click="detail.visible = false">关闭</el-button>
        </template>
      </el-dialog>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { apiMyFeedbackPage, apiDeleteMyFeedback } from "@/api/feedback";
import ListDataState from "@/components/common/ListDataState.vue";
import { useAuthUser } from "@/composables/useAuthUser";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { usePagedQuery } from "@/composables/usePagedQuery";
import { getApiErrorMessage } from "@/utils/apiError";

const router = useRouter();
const { getUserId, requireLogin } = useAuthUser();
const { runConfirmAction } = useConfirmAction();

type Row = {
  id: number;
  subject: string;
  content: string;
  contact: string;
  email?: string;
  status: "OPEN" | "CLOSED";
  reply?: string;
  createdAt: string;
};

const DISPLAY_TEXT_MAP: Record<string, string> = {
  "Suggest adding categories": "建议增加分类",
  "Hope to add more product categories for easy searching": "希望增加更多商品分类，方便查找",
  "No reply yet": "平台暂未回复，请耐心等待处理。",
  "Waiting for reply": "平台暂未回复，请耐心等待处理。",
  "Issue resolved": "问题已处理完成。",
};

const loading = ref(false);
const errorMessage = ref("");
const tableData = ref<Row[]>([]);
const total = ref(0);
const { query, reset: resetPagedQuery, changePage, search } = usePagedQuery({
  keyword: "",
  status: "" as "" | "OPEN" | "CLOSED",
  page: 1,
  size: 8,
});

const currentStatusLabel = computed(() => {
  if (query.status === "OPEN") return "处理中";
  if (query.status === "CLOSED") return "已完成";
  return "全部";
});

function getCurrentUserId(): number | null {
  return getUserId();
}

function getStatusText(status?: Row["status"]) {
  return status === "CLOSED" ? "已完成" : "处理中";
}

function getStatusTagType(status?: Row["status"]) {
  return status === "CLOSED" ? "success" : "warning";
}

function getDisplayText(value?: string | null, fallback = "-") {
  const text = String(value || "").trim();
  if (!text) return fallback;
  return DISPLAY_TEXT_MAP[text] || text;
}

function getReplyText(value?: string | null) {
  return getDisplayText(value, "平台暂未回复，请耐心等待处理。");
}

function reset() {
  resetPagedQuery(load);
}

function handlePageChange(page: number) {
  changePage(page, load);
}

function handleFilterChange() {
  search(load);
}

async function load() {
  const currentUserId = getCurrentUserId();
  if (!currentUserId) {
    tableData.value = [];
    total.value = 0;
    errorMessage.value = "";
    ElMessage.warning("请先登录后查看我的反馈");
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  try {
    const res = await apiMyFeedbackPage({
      page: query.page,
      size: query.size,
      userId: currentUserId,
      status: query.status || undefined,
      keyword: (String(query.keyword || "").trim() || undefined) as string | undefined,
    });
    tableData.value = res?.records || [];
    total.value = res?.total || 0;
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "获取数据失败");
    tableData.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  router.push("/user/feedback");
}

function openDetail(row: Row) {
  detail.row = row;
  detail.visible = true;
}

async function removeOne(id: number) {
  const currentUserId = requireLogin("请先登录后再操作");
  if (!currentUserId) return;

  await runConfirmAction({
    title: "提示",
    message: "确认删除该反馈吗？",
    successMessage: "已删除",
    errorMessage: "删除失败",
    action: () => apiDeleteMyFeedback(id, currentUserId),
    onSuccess: load,
  });
}

const detail = reactive<{ visible: boolean; row: Row | null }>({
  visible: false,
  row: null,
});

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
  gap: 8px;
  min-width: 0;
}

.header-badge {
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

.header-title {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.14;
  font-weight: 800;
}

.header-desc {
  margin: 0;
  max-width: 720px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.72;
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
  min-height: 40px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  background: rgba(239, 246, 255, 0.96);
}

.metric-chip.soft {
  border-color: rgba(220, 252, 231, 0.92);
  background: rgba(240, 253, 244, 0.96);
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

.toolbar-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid #edf2f7;
  border-radius: 20px;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fbff 100%);
  margin-bottom: 18px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-input {
  width: 290px;
}

.filter-select {
  width: 160px;
}

.toolbar-btn {
  height: 40px;
  border-radius: 12px;
  font-weight: 700;
}

.primary-btn {
  padding: 0 18px;
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.16);
}

.light-btn {
  border-color: #dbe6f2;
  color: #475569;
  background: #ffffff;
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
  line-height: 1.68;
}

.board-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  background: rgba(239, 246, 255, 0.96);
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

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feedback-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid #e8eef6;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.05), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(249, 251, 255, 0.98) 100%);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.feedback-card:hover {
  transform: translateY(-2px);
  border-color: #dbeafe;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.card-head {
  min-width: 0;
}

.title-wrap {
  min-width: 0;
}

.title-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.feedback-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.4;
}

.state-tag {
  border-radius: 999px;
  font-weight: 700;
  padding: 0 2px;
}

.meta-line {
  margin-top: 10px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 11px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  background: rgba(248, 251, 255, 0.95);
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.feedback-body {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.content-panel {
  padding: 15px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fbff 100%);
  border: 1px solid #edf2f7;
}

.reply-panel {
  background: linear-gradient(180deg, #f8fffb 0%, #f2fcf5 100%);
  border-color: rgba(187, 247, 208, 0.9);
}

.panel-label {
  display: inline-block;
  margin-bottom: 8px;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.reply-panel .panel-label {
  color: #15803d;
}

.content-panel p {
  margin: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-word;
}

.action-row {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.82);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  height: 36px;
  min-width: 92px;
  border-radius: 999px;
  border-color: #dbe6f2;
  color: #475569;
  background: #ffffff;
}

.view-btn {
  border-color: rgba(191, 219, 254, 0.92);
  color: #2563eb;
  background: rgba(239, 246, 255, 0.92);
}

.danger-btn {
  border-color: rgba(248, 113, 113, 0.28);
  color: #dc2626;
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

.detail-card {
  display: grid;
  gap: 14px;
  padding: 4px 0;
}

.detail-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.detail-label {
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.8;
}

.detail-value,
.detail-block {
  color: #475569;
  font-size: 14px;
  line-height: 1.72;
}

.detail-block {
  padding: 14px 15px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #edf2f7;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-detail {
  background: linear-gradient(180deg, #f8fffb 0%, #f2fcf5 100%);
  border-color: rgba(187, 247, 208, 0.9);
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-radius: 14px;
  background: #f8fbff;
  box-shadow: 0 0 0 1px rgba(209, 219, 234, 0.9);
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

@media (max-width: 1100px) {
  .header-row {
    flex-direction: column;
  }
}

@media (max-width: 900px) {
  .toolbar-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-right {
    justify-content: flex-start;
  }

  .feedback-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .page-wrap {
    padding: 10px;
  }

  .page-card,
  .toolbar-panel,
  .content-board,
  .feedback-card {
    padding: 16px;
    border-radius: 18px;
  }

  .header-title {
    font-size: 24px;
  }

  .search-input,
  .filter-select,
  .primary-btn {
    width: 100%;
  }

  .detail-row,
  .pager {
    grid-template-columns: 1fr;
  }
}
</style>
