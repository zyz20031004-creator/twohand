<template>
  <div class="page-wrap">
    <section class="page-card" v-loading="loading">
      <header class="header-row">
        <div class="header-copy">
          <span class="header-badge">求购管理</span>
          <h2 class="header-title">我的求购</h2>
          <p class="header-desc">管理你发布的求购信息与处理进度，方便随时筛选、补充说明和更新当前状态。</p>
        </div>

        <div class="header-metrics">
          <div class="metric-chip">
            <span>求购数量</span>
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
            placeholder="搜索求购标题"
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
            <el-option label="未解决" value="OPEN" />
            <el-option label="已解决" value="SOLVED" />
          </el-select>
          <el-button type="primary" class="toolbar-btn" @click="load">查询</el-button>
          <el-button class="toolbar-btn light-btn" @click="reset">重置</el-button>
        </div>

        <div class="toolbar-right">
          <el-button type="primary" class="toolbar-btn primary-btn" @click="openCreate">发布求购</el-button>
        </div>
      </div>

      <section class="content-board">
        <div class="board-head">
          <div>
            <h3 class="board-title">求购记录</h3>
            <p class="board-desc">在这里查看你的求购需求、状态进度和操作入口，方便管理每一条待购信息。</p>
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
          empty-text="还没有求购记录，去发布一条求购信息吧"
          @retry="load"
        >
          <div class="wanted-list">
            <article v-for="row in tableData" :key="row.id" class="wanted-card">
              <div class="card-top">
                <div class="title-wrap">
                  <div class="title-line">
                    <h3 class="wanted-title">{{ row.title || "未命名求购" }}</h3>
                    <el-tag :type="row.status === 'SOLVED' ? 'success' : 'primary'" effect="light" class="state-tag">
                      {{ row.status === "OPEN" ? "未解决" : "已解决" }}
                    </el-tag>
                  </div>

                  <div class="meta-line">
                    <span class="meta-item">发布时间 {{ row.createdAt || "-" }}</span>
                    <span class="meta-item">{{ row.status === "OPEN" ? "等待回应中" : "已完成处理" }}</span>
                  </div>
                </div>

                <div v-if="row.imageUrl" class="cover-wrap">
                  <img :src="toImg(row.imageUrl)" class="cover-image" />
                </div>
              </div>

              <div class="content-panel">
                <el-collapse v-model="collapseActiveMap[row.id]" class="content-collapse">
                  <el-collapse-item name="content">
                    <template #title>
                      <span class="collapse-title">查看求购说明</span>
                    </template>
                    <p class="wanted-content">{{ row.content || "暂无求购说明" }}</p>
                  </el-collapse-item>
                </el-collapse>
              </div>

              <div class="action-row">
                <el-button class="action-btn accent-btn" @click="markSolved(row)" :disabled="row.status === 'SOLVED'">
                  {{ row.status === "SOLVED" ? "已解决" : "标记解决" }}
                </el-button>
                <el-button class="action-btn" @click="openEdit(row)">编辑</el-button>
                <el-button class="action-btn danger-btn" @click="removeOne(row.id)">删除</el-button>
              </div>
            </article>
          </div>
        </ListDataState>

        <div v-if="!loading && !errorMessage && total > 0" class="pager">
          <div class="pager-copy">共 {{ total }} 条求购记录</div>
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

      <el-dialog
        v-model="dlg.visible"
        :title="dlg.mode === 'create' ? '发布求购' : '编辑求购'"
        width="760px"
        align-center
      >
        <div class="dialog-form">
          <div class="dialog-row">
            <span class="dialog-label">标题</span>
            <el-input v-model="dlg.form.title" placeholder="请输入求购标题" />
          </div>

          <div class="dialog-row textarea-row">
            <span class="dialog-label">内容</span>
            <el-input
              v-model="dlg.form.content"
              type="textarea"
              :rows="6"
              placeholder="补充你的求购需求、预算或成色要求"
            />
          </div>

          <div class="dialog-row upload-row">
            <span class="dialog-label">图片</span>
            <el-upload
              class="uploader"
              action="/api/upload"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
            >
              <img v-if="dlg.form.imageUrl" :src="toImg(dlg.form.imageUrl)" class="upload-img" />
              <div v-else class="upload-placeholder">上传参考图</div>
            </el-upload>
          </div>
        </div>

        <template #footer>
          <el-button @click="dlg.visible = false">取消</el-button>
          <el-button type="primary" @click="submit">确定</el-button>
        </template>
      </el-dialog>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import {
  apiCreateWanted,
  apiDeleteWanted,
  apiMyWantedPage,
  apiSolveWanted,
  apiUpdateWanted,
} from "@/api/wanted";
import ListDataState from "@/components/common/ListDataState.vue";
import { useAuthUser } from "@/composables/useAuthUser";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { usePagedQuery } from "@/composables/usePagedQuery";
import { getApiErrorMessage } from "@/utils/apiError";
import { imgUrl as resolveImgUrl } from "@/utils/img";

type Row = {
  id: number;
  title: string;
  content: string;
  imageUrl?: string;
  status: "OPEN" | "SOLVED";
  createdAt: string;
};

const route = useRoute();
const router = useRouter();
const { getUserId, requireLogin } = useAuthUser();
const { runConfirmAction } = useConfirmAction();
const loading = ref(false);
const errorMessage = ref("");
const tableData = ref<Row[]>([]);
const total = ref(0);
const collapseActiveMap = reactive<Record<number, string[]>>({});

const { query, reset: resetPagedQuery, changePage, search } = usePagedQuery({
  keyword: "",
  status: "" as "" | "OPEN" | "SOLVED",
  page: 1,
  size: 8,
});

const currentStatusLabel = computed(() => {
  if (query.status === "OPEN") return "未解决";
  if (query.status === "SOLVED") return "已解决";
  return "全部";
});

const dlg = reactive({
  visible: false,
  mode: "create" as "create" | "edit",
  editId: 0,
  form: {
    title: "",
    content: "",
    imageUrl: "",
  },
});

function hasCreateAction(action: unknown) {
  if (Array.isArray(action)) return action.includes("create");
  return action === "create";
}

function clearCreateActionQuery() {
  const nextQuery = { ...route.query };
  delete nextQuery.action;
  void router.replace({ path: route.path, query: nextQuery });
}

function getCurrentUserId() {
  return getUserId();
}

function toImg(url?: string) {
  return resolveImgUrl(url);
}

function syncCollapseState(rows: Row[]) {
  const nextIds = new Set(rows.map((row) => row.id));

  for (const row of rows) {
    if (!collapseActiveMap[row.id]) {
      collapseActiveMap[row.id] = [];
    }
  }

  Object.keys(collapseActiveMap).forEach((key) => {
    const id = Number(key);
    if (!nextIds.has(id)) {
      delete collapseActiveMap[id];
    }
  });
}

async function load() {
  const currentUserId = getCurrentUserId();
  if (!currentUserId) {
    tableData.value = [];
    total.value = 0;
    errorMessage.value = "";
    ElMessage.warning("请先登录后查看我的求购");
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  try {
    const res = await apiMyWantedPage({
      page: query.page,
      size: query.size,
      userId: currentUserId,
      status: query.status || undefined,
      keyword: String(query.keyword || "").trim() || undefined,
    });
    tableData.value = res?.records || [];
    total.value = res?.total || 0;
    syncCollapseState(tableData.value);
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "获取数据失败");
    tableData.value = [];
    total.value = 0;
    syncCollapseState([]);
  } finally {
    loading.value = false;
  }
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

function resetDialogForm() {
  dlg.form.title = "";
  dlg.form.content = "";
  dlg.form.imageUrl = "";
}

function openCreate() {
  if (!requireLogin("请先登录后再发布求购")) return;
  dlg.mode = "create";
  dlg.editId = 0;
  resetDialogForm();
  dlg.visible = true;
}

watch(
  () => route.query.action,
  (action) => {
    if (!hasCreateAction(action)) return;
    openCreate();
    clearCreateActionQuery();
  },
  { immediate: true },
);

function openEdit(row: Row) {
  dlg.mode = "edit";
  dlg.editId = row.id;
  dlg.form.title = row.title;
  dlg.form.content = row.content;
  dlg.form.imageUrl = row.imageUrl || "";
  dlg.visible = true;
}

function handleUploadSuccess(response: any) {
  const url = response?.data || response?.url || response;
  if (typeof url === "string" && url.trim()) {
    dlg.form.imageUrl = url;
    ElMessage.success("图片上传成功");
    return;
  }
  ElMessage.error("图片上传失败");
}

async function submit() {
  const currentUserId = requireLogin("请先登录后再操作");
  if (!currentUserId) return;

  const title = dlg.form.title.trim();
  const content = dlg.form.content.trim();
  if (!title) return ElMessage.warning("请输入标题");
  if (!content) return ElMessage.warning("请输入内容");

  loading.value = true;
  try {
    const payload = {
      title,
      content,
      imageUrl: dlg.form.imageUrl || undefined,
      userId: currentUserId,
    };

    if (dlg.mode === "create") {
      await apiCreateWanted(payload);
      ElMessage.success("发布成功");
    } else {
      await apiUpdateWanted(dlg.editId, payload);
      ElMessage.success("修改成功");
    }

    dlg.visible = false;
    await load();
  } catch (error) {
    const err = error as any;
    if (err?.code === 4001) {
      ElMessage.warning("请先完成学号认证");
      router.push("/user/verify");
      return;
    }
    ElMessage.error(getApiErrorMessage(error, "操作失败"));
  } finally {
    loading.value = false;
  }
}

async function markSolved(row: Row) {
  const currentUserId = requireLogin("请先登录后再操作");
  if (!currentUserId) return;

  await runConfirmAction({
    title: "提示",
    message: "确认将这条求购标记为已解决吗？",
    successMessage: "已标记为解决",
    errorMessage: "操作失败",
    action: () => apiSolveWanted(row.id, currentUserId),
    onSuccess: load,
  });
}

async function removeOne(id: number) {
  const currentUserId = requireLogin("请先登录后再操作");
  if (!currentUserId) return;

  await runConfirmAction({
    title: "提示",
    message: "确认删除这条求购吗？",
    successMessage: "已删除",
    errorMessage: "操作失败",
    action: () => apiDeleteWanted(id, currentUserId),
    onSuccess: load,
  });
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
  max-width: 660px;
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
  min-height: 42px;
  padding: 0 12px;
  border-radius: 14px;
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
  width: 280px;
}

.filter-select {
  width: 150px;
}

.toolbar-btn {
  height: 40px;
  border-radius: 12px;
  font-weight: 700;
}

.primary-btn {
  padding: 0 18px;
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
  min-height: 40px;
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

.wanted-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.wanted-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid #e8eef6;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.wanted-card:hover {
  transform: translateY(-2px);
  border-color: #dbeafe;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.card-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: flex-start;
}

.title-wrap {
  min-width: 0;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.wanted-title {
  margin: 0;
  flex: 1;
  min-width: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.state-tag {
  border-radius: 999px;
  font-weight: 700;
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
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  background: rgba(248, 251, 255, 0.95);
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.cover-wrap {
  width: 136px;
  height: 96px;
  border-radius: 14px;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  flex-shrink: 0;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.content-panel {
  margin-top: 14px;
  padding: 10px 15px;
  border-radius: 16px;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fbff 100%);
  border: 1px solid #edf2f7;
}

.content-collapse {
  border-top: none;
  border-bottom: none;
  background: transparent;
}

.content-panel :deep(.el-collapse-item__wrap) {
  border-bottom: none;
  background: transparent;
}

.content-panel :deep(.el-collapse-item__header) {
  min-height: 32px;
  padding: 0;
  border-bottom: none;
  background: transparent;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
}

.content-panel :deep(.el-collapse-item__arrow) {
  margin-right: 4px;
  color: #94a3b8;
}

.content-panel :deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

.collapse-title {
  display: inline-flex;
  align-items: center;
}

.wanted-content {
  margin: 8px 0 2px;
  color: #475569;
  font-size: 14px;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-word;
}

.action-row {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  height: 36px;
  min-width: 92px;
  border-radius: 10px;
  border-color: #dbe6f2;
  color: #475569;
  background: #ffffff;
}

.accent-btn {
  border-color: rgba(187, 247, 208, 0.92);
  color: #15803d;
  background: rgba(240, 253, 244, 0.9);
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

.dialog-row.textarea-row,
.dialog-row.upload-row {
  align-items: flex-start;
}

.dialog-label {
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.uploader {
  width: fit-content;
}

:deep(.uploader .el-upload) {
  border: none;
}

.upload-img,
.upload-placeholder {
  width: 116px;
  height: 116px;
  border-radius: 22px;
  border: 1px dashed rgba(147, 197, 253, 0.9);
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
}

.upload-img {
  object-fit: cover;
  display: block;
}

.upload-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 14px;
  background: #f8fbff;
  box-shadow: 0 0 0 1px rgba(209, 219, 234, 0.9);
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-textarea__inner:focus) {
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
}

@media (max-width: 760px) {
  .page-wrap {
    padding: 10px;
  }

  .page-card,
  .toolbar-panel,
  .content-board,
  .wanted-card {
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

  .card-top,
  .dialog-row,
  .pager {
    grid-template-columns: 1fr;
  }

  .cover-wrap {
    width: 100%;
    height: 180px;
  }
}
</style>
