<template>
  <div class="page-wrap">
    <section class="page-card" v-loading="loading">
      <UserCenterPageHeader
        eyebrow="我的收藏"
        title="把感兴趣的商品都放在这里"
        description="查看已收藏商品，快速回到详情页继续浏览，或及时清理下架和失效收藏。"
        :stats="headerStats"
      >
        <template #actions>
          <el-button class="header-action" @click="goExplore">去看看商品</el-button>
        </template>
      </UserCenterPageHeader>

      <UserCenterFilterBar class="section-gap">
        <el-input
          v-model="query.keyword"
          class="search-input"
          clearable
          placeholder="搜索收藏商品标题"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" class="status-select" clearable placeholder="按商品状态筛选">
          <el-option
            v-for="item in STATUS_OPTIONS"
            :key="item.value || 'all'"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" class="toolbar-btn" @click="handleSearch">查询</el-button>
        <el-button class="toolbar-btn light-btn" @click="reset">重置</el-button>

        <template #actions>
          <el-button class="toolbar-btn batch-btn" :disabled="selectedIds.length === 0" @click="batchCancel">
            {{ batchButtonText }}
          </el-button>
        </template>
      </UserCenterFilterBar>

      <section class="content-board">
        <div class="section-head">
          <div class="board-copy">
            <h3 class="section-title">收藏商品</h3>
            <p class="section-desc">更适合浏览的收藏卡列表，优先查看在售商品，也能顺手清理下架和失效内容。</p>
          </div>
          <div class="section-chip">当前显示 {{ filteredFavorites.length }} 条</div>
        </div>

        <ListDataState :loading="loading" :empty="false" :error-message="errorMessage" @retry="load">
          <div v-if="filteredFavorites.length" class="favorite-list">
            <article
              v-for="item in filteredFavorites"
              :key="item.favoriteId"
              class="favorite-card"
              :class="{ 'is-invalid': item.isInvalid, 'is-off': item.filterKey === 'OFF' }"
            >
              <div class="cover-wrap">
                <el-checkbox
                  class="select-check"
                  :model-value="isSelected(item.productId)"
                  :disabled="item.productId <= 0"
                  @change="handleSelectionChange(item.productId, $event)"
                />

                <button type="button" class="cover-button" @click="openProduct(item.productId)">
                  <el-image v-if="item.coverUrl" class="cover-image" :src="toImg(item.coverUrl)" fit="cover">
                    <template #error>
                      <div class="cover-placeholder">暂无图片</div>
                    </template>
                  </el-image>
                  <div v-else class="cover-placeholder">暂无图片</div>
                </button>
              </div>

              <div class="card-body">
                <div class="card-head">
                  <button type="button" class="card-title" :title="item.title" @click="openProduct(item.productId)">
                    {{ item.title }}
                  </button>
                  <UserCenterStatusTag :text="item.statusText" :tone="item.statusTone" size="sm" />
                </div>

                <div class="price-row">
                  <span class="price-label">当前价格</span>
                  <strong class="price-value">{{ item.priceText }}</strong>
                </div>

                <p class="status-note" :class="{ 'is-alert': item.isInvalid }">
                  {{ item.statusReason }}
                </p>

                <div class="meta-list">
                  <div class="meta-item">
                    <span class="meta-label">收藏时间</span>
                    <span class="meta-value">{{ item.favoriteTimeText }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">商品编号</span>
                    <span class="meta-value">#{{ item.productId > 0 ? item.productId : "--" }}</span>
                  </div>
                </div>
              </div>

              <div class="card-actions">
                <el-button type="primary" class="action-btn action-btn--view" @click="openProduct(item.productId)">
                  查看商品
                </el-button>
                <el-button class="action-btn action-btn--ghost" @click="toggleSelected(item.productId, !isSelected(item.productId))">
                  {{ isSelected(item.productId) ? "已选中" : "选择收藏" }}
                </el-button>
                <el-button class="action-btn action-btn--danger" @click="cancelOne(item.productId)">
                  取消收藏
                </el-button>
              </div>
            </article>
          </div>

          <div v-else class="empty-panel">
            <el-empty :description="emptyText">
              <template #description>
                <div class="empty-copy">
                  <p class="empty-title">{{ emptyText }}</p>
                  <p class="empty-desc">{{ emptyDescription }}</p>
                </div>
              </template>
              <el-button type="primary" class="empty-action" @click="handleEmptyAction">
                {{ hasFilters ? "重置筛选" : "去看看商品" }}
              </el-button>
            </el-empty>
          </div>
        </ListDataState>

        <div v-if="!loading && !errorMessage && total > 0" class="pager">
          <div class="pager-copy">共 {{ total }} 件收藏</div>
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
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { apiCancelFavorite, apiCancelFavoriteBatch, apiGetMyFavoritePage } from "@/api/user";
import ListDataState from "@/components/common/ListDataState.vue";
import UserCenterFilterBar from "@/components/user-center/UserCenterFilterBar.vue";
import UserCenterPageHeader from "@/components/user-center/UserCenterPageHeader.vue";
import UserCenterStatusTag from "@/components/user-center/UserCenterStatusTag.vue";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { usePagedQuery } from "@/composables/usePagedQuery";
import { getApiErrorMessage } from "@/utils/apiError";
import { productImgUrl as resolveProductImgUrl } from "@/utils/img";
import { formatPrice } from "@/utils/price";
import { getProductDisplayStatus } from "@/utils/productStatus";

type StatusFilterKey = "" | "ON" | "OFF" | "INVALID";
type FavoriteStatusTone = "success" | "info" | "danger";

type FavoriteRow = {
  id?: number | string;
  productId?: number | string;
  title?: string | null;
  price?: number | string | null;
  productStatus?: unknown;
  auditStatus?: unknown;
  soldFlag?: unknown;
  createdAt?: string | null;
  coverUrl?: string | null;
};

type FavoriteCardItem = {
  favoriteId: number;
  productId: number;
  title: string;
  coverUrl: string;
  priceText: string;
  statusText: "在售" | "已下架" | "已失效";
  statusTone: FavoriteStatusTone;
  filterKey: Exclude<StatusFilterKey, "">;
  statusReason: string;
  favoriteTimeText: string;
  isInvalid: boolean;
};

type PageResp<T> = {
  records: T[];
  total: number;
};

const STATUS_OPTIONS = [
  { label: "全部", value: "" as StatusFilterKey },
  { label: "在售", value: "ON" as StatusFilterKey },
  { label: "已下架", value: "OFF" as StatusFilterKey },
  { label: "已失效", value: "INVALID" as StatusFilterKey },
];

const router = useRouter();
const { runConfirmAction } = useConfirmAction();

const loading = ref(false);
const errorMessage = ref("");
const favoriteList = ref<FavoriteCardItem[]>([]);
const total = ref(0);
const selectedIds = ref<number[]>([]);

const { query, search, reset: resetPagedQuery, changePage } = usePagedQuery({
  page: 1,
  size: 10,
  keyword: "",
  status: "" as StatusFilterKey,
});

const currentStatusLabel = computed(() => {
  return STATUS_OPTIONS.find((item) => item.value === query.status)?.label || "全部";
});

const headerStats = computed(() => [
  { label: "收藏总数", value: total.value },
  { label: "当前筛选", value: currentStatusLabel.value, tone: statusToneByFilter(query.status) },
]);

const filteredFavorites = computed(() => {
  if (!query.status) return favoriteList.value;
  return favoriteList.value.filter((item) => item.filterKey === query.status);
});

const hasFilters = computed(() => {
  return Boolean(String(query.keyword || "").trim() || query.status);
});

const emptyText = computed(() => {
  return hasFilters.value ? "未找到相关收藏商品" : "你还没有收藏商品";
});

const emptyDescription = computed(() => {
  return hasFilters.value
    ? "换个关键词试试，或者重置筛选后再继续浏览。"
    : "先去逛逛校园好物，把感兴趣的商品收藏起来吧。";
});

const batchButtonText = computed(() => {
  return selectedIds.value.length > 0 ? `取消已选（${selectedIds.value.length}）` : "取消已选";
});

watch(
  filteredFavorites,
  (items) => {
    const visibleIds = new Set(items.map((item) => item.productId));
    selectedIds.value = selectedIds.value.filter((id) => visibleIds.has(id));
  },
  { immediate: true }
);

function statusToneByFilter(value: StatusFilterKey) {
  if (value === "ON") return "success" as const;
  if (value === "INVALID") return "danger" as const;
  return "info" as const;
}

function formatTime(value?: string | null) {
  if (!value) return "暂无记录";
  return String(value).replace("T", " ").slice(0, 19);
}

function mapFavoriteStatus(row: FavoriteRow) {
  const status = getProductDisplayStatus({
    id: row.productId,
    status: row.productStatus,
    auditStatus: row.auditStatus,
    soldFlag: row.soldFlag,
  });

  if (status.key === "ON") {
    return {
      filterKey: "ON" as const,
      statusText: "在售" as const,
      statusTone: "success" as const,
      statusReason: "商品仍在售，可继续查看详情。",
      isInvalid: false,
    };
  }

  if (status.key === "OFF") {
    return {
      filterKey: "OFF" as const,
      statusText: "已下架" as const,
      statusTone: "info" as const,
      statusReason: "商品当前已下架，可保留收藏或及时清理。",
      isInvalid: false,
    };
  }

  if (status.key === "SOLD") {
    return {
      filterKey: "INVALID" as const,
      statusText: "已失效" as const,
      statusTone: "danger" as const,
      statusReason: "商品已售出，建议清理收藏。",
      isInvalid: true,
    };
  }

  if (status.key === "REJECTED") {
    return {
      filterKey: "INVALID" as const,
      statusText: "已失效" as const,
      statusTone: "danger" as const,
      statusReason: "商品状态异常或未通过审核，建议清理收藏。",
      isInvalid: true,
    };
  }

  return {
    filterKey: "INVALID" as const,
    statusText: "已失效" as const,
    statusTone: "danger" as const,
    statusReason: "商品暂不可浏览或交易，可稍后再看。",
    isInvalid: true,
  };
}

function normalizeFavoriteRow(row: FavoriteRow): FavoriteCardItem {
  const productId = Number(row.productId || 0);
  const favoriteId = Number(row.id || productId || Date.now());
  const statusMeta = mapFavoriteStatus(row);

  return {
    favoriteId,
    productId,
    title: String(row.title || "未命名商品"),
    coverUrl: String(row.coverUrl || ""),
    priceText: formatPrice(row.price),
    statusText: statusMeta.statusText,
    statusTone: statusMeta.statusTone,
    filterKey: statusMeta.filterKey,
    statusReason: statusMeta.statusReason,
    favoriteTimeText: formatTime(row.createdAt),
    isInvalid: statusMeta.isInvalid,
  };
}

function toImg(url?: string | null) {
  return resolveProductImgUrl(url || "");
}

function isSelected(productId: number) {
  return selectedIds.value.includes(productId);
}

function toggleSelected(productId: number, checked: unknown) {
  if (!Number.isFinite(productId) || productId <= 0) return;

  const next = new Set(selectedIds.value);
  if (Boolean(checked)) {
    next.add(productId);
  } else {
    next.delete(productId);
  }
  selectedIds.value = Array.from(next);
}

function handleSelectionChange(productId: number, checked: unknown) {
  toggleSelected(productId, checked);
}

function handleSearch() {
  selectedIds.value = [];
  search(load);
}

function reset() {
  selectedIds.value = [];
  resetPagedQuery(load);
}

function handlePageChange(page: number) {
  selectedIds.value = [];
  changePage(page, load);
}

function openProduct(productId: number) {
  const id = Number(productId || 0);
  if (!Number.isFinite(id) || id <= 0) {
    ElMessage.warning("未找到商品信息");
    return;
  }
  router.push(`/user/hot/product/${id}`);
}

function goExplore() {
  router.push("/user/hot");
}

function handleEmptyAction() {
  if (hasFilters.value) {
    reset();
    return;
  }
  goExplore();
}

async function load() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const res = await apiGetMyFavoritePage<PageResp<FavoriteRow>>({
      page: query.page,
      size: query.size,
      keyword: String(query.keyword || ""),
    });

    favoriteList.value = (res.records || []).map(normalizeFavoriteRow);
    total.value = Number(res.total || 0);
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "加载收藏失败");
    favoriteList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

async function cancelOne(productId: number) {
  await apiCancelFavorite(productId);
  selectedIds.value = selectedIds.value.filter((id) => id !== productId);
  ElMessage.success("已取消收藏");
  await load();
}

async function batchCancel() {
  if (!selectedIds.value.length) return;

  await runConfirmAction({
    title: "取消收藏",
    message: "确定取消所选收藏吗？",
    successMessage: "已取消所选收藏",
    action: async () => {
      await apiCancelFavoriteBatch(selectedIds.value);
      selectedIds.value = [];
    },
    onSuccess: load,
  });
}

onMounted(() => {
  void load();
});
</script>

<style scoped>
.page-wrap {
  max-width: 1240px;
  margin: 0 auto;
  padding: 16px;
}

.page-card {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #edf2f7;
  border-radius: 22px;
  padding: 18px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.section-gap {
  margin-top: 12px;
}

.page-card :deep(.page-header) {
  gap: 16px;
}

.page-card :deep(.page-header .copy) {
  gap: 6px;
}

.page-card :deep(.page-header .eyebrow) {
  min-height: 28px;
  padding: 0 10px;
}

.page-card :deep(.page-header .title) {
  font-size: 24px;
  line-height: 1.18;
  font-weight: 700;
}

.page-card :deep(.page-header .description) {
  max-width: 580px;
  font-size: 12px;
  line-height: 1.6;
}

.page-card :deep(.page-header .aside) {
  gap: 10px;
}

.page-card :deep(.page-header .stats) {
  gap: 8px;
}

.page-card :deep(.page-header .stat-chip) {
  min-height: 36px;
  padding: 0 10px;
  border-radius: 12px;
}

.page-card :deep(.page-header .stat-chip span) {
  font-size: 11px;
}

.page-card :deep(.page-header .stat-chip strong) {
  font-size: 12px;
}

.header-action {
  height: 36px;
  border-radius: 10px;
  border-color: #dbe6f2;
  color: #334155;
  background: #ffffff;
}

.section-gap :deep(.filter-bar) {
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
}

.section-gap :deep(.main) {
  gap: 8px;
}

.search-input {
  width: 280px;
}

.status-select {
  width: 156px;
}

.toolbar-btn {
  height: 36px;
  border-radius: 10px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 700;
}

.light-btn {
  border-color: #dbe6f2;
  color: #475569;
  background: #ffffff;
}

.batch-btn {
  border-color: rgba(248, 113, 113, 0.18);
  color: #cc6b6b;
  background: rgba(255, 250, 250, 0.76);
}

.content-board {
  margin-top: 14px;
  min-width: 0;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid #edf2f7;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.board-copy {
  min-width: 0;
}

.section-title {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
  line-height: 1.3;
  font-weight: 700;
}

.section-desc {
  margin: 4px 0 0;
  color: #8b98ab;
  font-size: 12px;
  line-height: 1.55;
}

.section-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(191, 219, 254, 0.85);
  background: rgba(248, 250, 255, 0.98);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.favorite-card {
  display: grid;
  grid-template-columns: 168px minmax(0, 1fr) 148px;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #e6edf5;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.favorite-card:hover {
  transform: translateY(-1px);
  border-color: #dbeafe;
  box-shadow: 0 9px 18px rgba(15, 23, 42, 0.05);
}

.favorite-card.is-off {
  border-color: rgba(203, 213, 225, 0.88);
}

.favorite-card.is-invalid {
  background: linear-gradient(180deg, #ffffff 0%, #fcfcfd 100%);
  border-color: rgba(226, 232, 240, 0.95);
}

.cover-wrap {
  position: relative;
}

.select-check {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  min-height: 28px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.cover-button {
  position: relative;
  width: 168px;
  height: 124px;
  padding: 0;
  border: 1px solid #edf2f7;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  cursor: pointer;
}

.cover-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

:deep(.cover-image .el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.favorite-card.is-invalid :deep(.cover-image .el-image__inner) {
  filter: grayscale(1);
  opacity: 0.72;
}

.cover-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}

.card-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  margin: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.45;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.favorite-card.is-invalid .card-title {
  color: #334155;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.price-label {
  color: #8b98ab;
  font-size: 12px;
}

.price-value {
  color: #ea580c;
  font-size: 28px;
  line-height: 1.05;
  font-weight: 800;
}

.status-note {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.55;
}

.status-note.is-alert {
  color: #b85b5b;
}

.meta-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.meta-label {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.meta-value {
  min-width: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-self: stretch;
  justify-content: center;
}

.action-btn {
  margin: 0;
  width: 100%;
  height: 36px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
}

.action-btn--ghost {
  border-color: #dbe6f2;
  color: #334155;
  background: #ffffff;
}

.action-btn--danger {
  border-color: rgba(248, 113, 113, 0.18);
  color: #cf6b6b;
  background: rgba(255, 250, 250, 0.78);
}

.empty-panel {
  border: 1px dashed #dbe6f2;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.empty-panel :deep(.el-empty) {
  padding: 38px 16px;
}

.empty-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.empty-title {
  margin: 0;
  color: #334155;
  font-size: 15px;
  font-weight: 700;
}

.empty-desc {
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.6;
}

.empty-action {
  height: 36px;
  border-radius: 10px;
}

.content-board :deep(.state-wrap) {
  min-height: 220px;
  border-radius: 18px;
  border: 1px dashed #dbe6f2;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.content-board :deep(.el-result__subtitle) {
  color: #94a3b8;
}

.pager {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pager-copy {
  color: #64748b;
  font-size: 12px;
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

@media (max-width: 1180px) {
  .favorite-card {
    grid-template-columns: 152px minmax(0, 1fr) 138px;
  }

  .cover-button {
    width: 152px;
    height: 116px;
  }
}

@media (max-width: 900px) {
  .favorite-card {
    grid-template-columns: 144px minmax(0, 1fr);
  }

  .card-actions {
    grid-column: 1 / -1;
    flex-direction: row;
  }
}

@media (max-width: 760px) {
  .page-wrap {
    padding: 10px;
  }

  .page-card {
    padding: 16px;
    border-radius: 18px;
  }

  .page-card :deep(.page-header .title) {
    font-size: 22px;
  }

  .search-input,
  .status-select,
  .toolbar-btn {
    width: 100%;
    min-width: 0;
  }

  .section-head,
  .pager {
    display: grid;
    grid-template-columns: 1fr;
  }

  .favorite-card {
    grid-template-columns: 1fr;
    padding: 14px;
  }

  .cover-button {
    width: 100%;
    height: 188px;
  }

  .card-head,
  .meta-list,
  .card-actions {
    grid-template-columns: 1fr;
  }

  .card-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .meta-list {
    grid-template-columns: 1fr;
  }

  .card-actions {
    flex-direction: column;
  }
}
</style>
