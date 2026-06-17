<template>
  <div class="page-wrap">
    <section class="page-card" v-loading="loading">
      <UserCenterPageHeader
        eyebrow="我卖出的"
        title="跟踪待处理与已完成的售出订单"
        description="梳理售出订单的状态、金额和买家信息，方便快速跟进。"
        :stats="headerStats"
      />

      <UserCenterFilterBar class="section-gap">
        <el-input
          v-model="query.keyword"
          class="search-input"
          clearable
          placeholder="搜索商品标题"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="query.status"
          class="status-select"
          clearable
          placeholder="按订单状态筛选"
          @change="handleFilterChange"
        >
          <el-option label="全部状态" value="" />
          <el-option label="待支付" value="UNPAID" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已完成" value="FINISHED" />
        </el-select>
        <el-button type="primary" class="toolbar-btn" @click="handleSearch">查询</el-button>
        <el-button class="toolbar-btn light-btn" @click="reset">重置</el-button>
      </UserCenterFilterBar>

      <section class="content-board">
        <div class="section-head">
          <div>
            <h3 class="section-title">售出订单列表</h3>
            <p class="section-desc">优先查看标题、状态、金额和买家信息。</p>
          </div>
          <div class="section-chip">当前展示 {{ orderList.length }} 笔</div>
        </div>

        <ListDataState
          :loading="loading"
          :empty="orderList.length === 0"
          :error-message="errorMessage"
          :empty-text="emptyText"
          @retry="load"
        >
          <div class="list-wrap">
            <article v-for="row in orderList" :key="row.id" class="sold-card">
              <section class="card-main">
                <button type="button" class="card-media" @click="goDetail(row.productId)">
                  <el-image v-if="row.coverUrl" class="card-image" :src="toImg(row.coverUrl)" fit="cover">
                    <template #error>
                      <div class="card-placeholder">暂无图片</div>
                    </template>
                  </el-image>
                  <div v-else class="card-placeholder">暂无图片</div>
                </button>

                <div class="card-content">
                  <button type="button" class="card-title" :title="row.productTitle || '未命名商品'" @click="goDetail(row.productId)">
                    {{ row.productTitle || "未命名商品" }}
                  </button>

                  <div class="card-grid">
                    <div
                      v-for="item in buildInfoItems(row)"
                      :key="`${row.id}-${item.label}`"
                      class="meta-item"
                    >
                      <span class="meta-label">{{ item.label }}</span>
                      <span class="meta-value" :class="{ 'is-strong': item.strong, 'is-ellipsis': item.ellipsis }">
                        {{ item.value }}
                      </span>
                    </div>
                  </div>

                  <div class="card-grid timeline-grid">
                    <div
                      v-for="item in buildTimelineItems(row)"
                      :key="`${row.id}-${item.label}`"
                      class="meta-item"
                      :class="{ 'is-muted': item.muted }"
                    >
                      <span class="meta-label">{{ item.label }}</span>
                      <span class="meta-value">{{ item.value }}</span>
                    </div>
                  </div>
                </div>
              </section>

              <footer class="card-footer">
                <div class="footer-info">
                  <div class="footer-status-group">
                    <UserCenterStatusTag class="footer-status-tag" :text="getOrderStatusMeta(row.status).text" :tone="statusTone(row)" />
                    <span v-if="statusNoteText(row)" class="footer-note" :class="{ 'is-danger': row.status === 'CANCELLED' }">
                      {{ statusNoteText(row) }}
                    </span>
                  </div>
                  <div class="footer-amount-group">
                    <span class="footer-amount-label">订单金额</span>
                    <strong class="footer-amount-value">{{ formatAmount(row.amount) }}</strong>
                  </div>
                </div>

                <div class="footer-actions">
                <el-button class="action-btn action-btn--plain" @click="goDetail(row.productId)">查看商品</el-button>
                <el-button
                  class="action-btn action-btn--plain"
                  :disabled="!hasOrderAction(row, 'contact') || !canContact(row)"
                  @click="contactBuyer(row)"
                >
                  联系买家
                </el-button>
                <el-button
                  class="action-btn action-btn--danger action-btn--delete"
                  :disabled="!hasOrderAction(row, 'remove') || !canDelete(row)"
                  @click="removeOrder(row.id)"
                >
                  删除订单
                </el-button>
                </div>
              </footer>
            </article>
          </div>
        </ListDataState>

        <div v-if="!loading && !errorMessage && total > 0" class="pager">
          <div class="pager-copy">共 {{ total }} 笔订单</div>
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
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiCreateOrGetProductSession } from "@/api/chat";
import { apiGetMyOrderPage, apiHideOrder } from "@/api/user";
import ListDataState from "@/components/common/ListDataState.vue";
import UserCenterFilterBar from "@/components/user-center/UserCenterFilterBar.vue";
import UserCenterPageHeader from "@/components/user-center/UserCenterPageHeader.vue";
import UserCenterStatusTag from "@/components/user-center/UserCenterStatusTag.vue";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { usePagedQuery } from "@/composables/usePagedQuery";
import { getApiErrorMessage } from "@/utils/apiError";
import { productImgUrl as resolveProductImgUrl } from "@/utils/img";
import { formatPrice } from "@/utils/price";
import { getOrderStatusMeta, getSoldOrderActionKeys, type OrderActionKey } from "@/utils/tradeStatus";
import { getTradeDetail } from "@/utils/tradeLocation";

type OrderStatus = "UNPAID" | "PAID" | "CANCELLED" | "FINISHED" | string;

type OrderRow = {
  id: number;
  productId?: number;
  orderNo?: string;
  productTitle?: string;
  coverUrl?: string | null;
  buyerId?: number;
  buyerName?: string;
  phone?: string;
  receiver?: string;
  receiveAddress?: string;
  tradeLocation?: string;
  amount?: number | string | null;
  createdAt?: string;
  status: OrderStatus;
};

const router = useRouter();
const { runConfirmAction } = useConfirmAction();

const loading = ref(false);
const errorMessage = ref("");
const orderList = ref<OrderRow[]>([]);
const total = ref(0);

const { query, reset: resetPagedQuery, changePage, search } = usePagedQuery({
  type: "SELL" as const,
  page: 1,
  size: 10,
  keyword: "",
  status: "",
  payType: "",
});

const currentStatusLabel = computed(() => {
  if (query.status) return getOrderStatusMeta(query.status).text;
  return "全部";
});

const headerStats = computed(() => [
  { label: "订单总数", value: total.value },
  { label: "当前筛选", value: currentStatusLabel.value, tone: "success" as const },
]);

const emptyText = computed(() => {
  if (String(query.keyword || "").trim() || query.status) {
    return "没有匹配的售出订单";
  }
  return "暂无售出订单";
});

function toImg(url?: string | null) {
  return resolveProductImgUrl(url || "");
}

function formatTime(value?: string | null) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}

function formatAmount(value: unknown) {
  return formatPrice(value as string | number | null | undefined);
}

function canDelete(row: OrderRow) {
  return row.status === "FINISHED" || row.status === "CANCELLED";
}

function canContact(row: OrderRow) {
  const buyerId = Number(row.buyerId || 0);
  return Number.isFinite(buyerId) && buyerId > 0;
}

function hasOrderAction(row: OrderRow, action: OrderActionKey) {
  return getSoldOrderActionKeys(row.status).includes(action);
}

function statusTone(row: OrderRow) {
  if (row.status === "CANCELLED") return "danger" as const;
  return getOrderStatusMeta(row.status).tone;
}

function statusNoteText(row: OrderRow) {
  if (row.status === "FINISHED") return "交易已完成";
  if (row.status === "CANCELLED") return "订单已取消";
  return "";
}

function buildInfoItems(row: OrderRow) {
  const hasReceiveAddress = row.receiveAddress && row.receiveAddress !== "-" && row.receiveAddress.trim();
  const addressText = hasReceiveAddress
    ? `${row.receiver || ""} ${row.phone || ""} ${row.receiveAddress || ""}`.trim()
    : "";
  const trade = getTradeDetail(row.tradeLocation);
  
  const isDefaultTip = (text: string) => 
    text.includes("下单后可选择收货地址") || 
    text.includes("请通过站内聊天与卖家确认");

  const items: Array<{ label: string; value: string; strong?: boolean; ellipsis?: boolean }> = [
    { label: "买家", value: row.buyerName || "-", strong: true },
    { label: "联系方式", value: row.phone || "-" },
  ];

  if (hasReceiveAddress) {
    items.push({ label: "收货地址", value: addressText });
    items.push({ label: "交易方式", value: "送货上门" });
  } else if (trade.description && !isDefaultTip(trade.description)) {
    items.push({ label: "交易地点", value: trade.description });
  } else if (trade.locationText && !isDefaultTip(trade.locationText)) {
    items.push({ label: "交易地点", value: trade.locationText });
  }
  
  items.push({ label: "订单号", value: row.orderNo || "-", strong: false, ellipsis: true });

  return items;
}

function buildTimelineItems(row: OrderRow) {
  return [{ label: "创建时间", value: formatTime(row.createdAt), muted: !row.createdAt }];
}

function handlePageChange(page: number) {
  changePage(page, load);
}

function handleFilterChange() {
  search(load);
}

function handleSearch() {
  search(load);
}

function reset() {
  resetPagedQuery(load);
}

function goDetail(productId?: number) {
  const id = Number(productId || 0);
  if (!Number.isFinite(id) || id <= 0) {
    ElMessage.warning("未找到商品");
    return;
  }
  router.push(`/user/hot/product/${id}`);
}

async function contactBuyer(row: OrderRow) {
  if (!canContact(row)) {
    ElMessage.warning("买家信息不可用");
    return;
  }
  const buyerId = Number(row.buyerId);
  const productId = Number(row.productId || 0);

  if (!Number.isFinite(productId) || productId <= 0) {
    ElMessage.warning("未找到关联商品");
    return;
  }
  try {
    const session: any = await apiCreateOrGetProductSession({ productId, targetUserId: buyerId });
    const sessionId = Number(session?.sessionId || session?.id);
    if (!Number.isFinite(sessionId) || sessionId <= 0) {
      ElMessage.error("创建聊天会话失败");
      return;
    }
    router.push({ path: "/user/chat", query: { sessionId: String(sessionId), productId: String(productId) } });
  } catch (error: any) {
    ElMessage.error(error?.message || "创建聊天会话失败");
  }
}

async function load() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const result = await apiGetMyOrderPage<{ records: OrderRow[]; total: number }>({ ...query });
    orderList.value = result.records || [];
    total.value = result.total || 0;
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "加载订单失败");
    orderList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

async function removeOrder(id: number) {
  await runConfirmAction({
    title: "删除订单",
    message: "删除后，该订单将不再在当前列表中显示，但不会删除系统中的真实订单记录",
    successMessage: "订单已删除",
    action: () => apiHideOrder(id),
    onSuccess: load,
  });
}

onMounted(() => {
  load();
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
  max-width: 560px;
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

.section-gap :deep(.filter-bar) {
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
}

.section-gap :deep(.main) {
  gap: 8px;
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
  width: 260px;
}

.status-select {
  width: 142px;
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

.content-board {
  margin-top: 14px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid #edf2f7;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.board-head,
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.board-title,
.section-title {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.board-desc,
.section-desc {
  margin: 4px 0 0;
  color: #7b8aa3;
  font-size: 12px;
  line-height: 1.58;
}

.board-chip,
.section-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  background: rgba(239, 246, 255, 0.96);
  white-space: nowrap;
}

.section-chip {
  justify-content: center;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
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

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sold-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 18px 14px;
  border: 1px solid #e6edf5;
  border-radius: 17px;
  background: #fff;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.sold-card:hover {
  transform: translateY(-1px);
  border-color: #dbeafe;
  box-shadow: 0 9px 18px rgba(15, 23, 42, 0.05);
}

.card-main {
  display: grid;
  grid-template-columns: 148px minmax(0, 1fr);
  align-items: start;
  gap: 16px;
}

.card-media {
  position: relative;
  width: 148px;
  height: 112px;
  padding: 0;
  border: 1px solid #edf2f7;
  border-radius: 14px;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  cursor: pointer;
}

.card-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

:deep(.card-image .el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}

.card-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-title {
  margin: 0;
  padding: 0;
  width: 100%;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 17px;
  line-height: 1.45;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px 18px;
}

.timeline-grid {
  gap: 6px 18px;
}

.meta-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  min-width: 0;
}

.meta-item.is-muted .meta-value {
  color: #94a3b8;
}

.meta-label {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
}

.meta-value {
  min-width: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}

.meta-value.is-strong {
  color: #334155;
  font-weight: 600;
}

.meta-value.is-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #eef3f8;
}

.footer-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-width: 0;
}

.footer-status-group {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1 1 auto;
}

.footer-status-tag :deep(.status-tag) {
  min-height: 26px;
  padding: 0 10px;
  font-size: 12px;
}

.footer-note {
  min-width: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.footer-note.is-danger {
  color: #c45d5d;
}

.footer-amount-group {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
}

.footer-amount-label {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.footer-amount-value {
  color: #ea580c;
  font-size: 24px;
  line-height: 1.05;
  font-weight: 800;
  white-space: nowrap;
}

.footer-actions {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(0, 1fr);
  gap: 8px;
  width: 100%;
}

.action-btn {
  margin: 0;
  width: 100%;
  min-width: 0;
  height: 32px;
  border-radius: 9px;
  border-color: #dbe6f2;
  color: #475569;
  background: #ffffff;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 600;
}

.action-btn--plain {
  border-color: #dbe6f2;
  color: #334155;
  background: #ffffff;
}

.danger-btn,
.action-btn--danger {
  border-color: rgba(248, 113, 113, 0.18);
  color: #d36363;
  background: rgba(255, 249, 249, 0.88);
}

.action-btn--delete {
  border-color: rgba(248, 113, 113, 0.12);
  color: #c96b6b;
  background: rgba(255, 250, 250, 0.72);
}

.content-board :deep(.state-wrap) {
  min-height: 180px;
  border-radius: 16px;
  border: 1px dashed #dbe6f2;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.content-board :deep(.el-empty__description p),
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

@media (max-width: 1080px) {
  .card-main {
    grid-template-columns: 140px minmax(0, 1fr);
  }

  .card-media {
    width: 140px;
    height: 106px;
  }
}

@media (max-width: 760px) {
  .page-wrap {
    padding: 10px;
  }

  .page-card,
  .toolbar-panel,
  .content-board,
  .sold-card {
    padding: 16px;
    border-radius: 18px;
  }

  .page-card :deep(.page-header .title) {
    font-size: 22px;
  }

  .header-row {
    flex-direction: column;
  }

  .toolbar-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-right {
    justify-content: flex-start;
  }

  .search-input,
  .status-select,
  .toolbar-btn {
    width: 100%;
    min-width: 0;
  }

  .board-head,
  .section-head,
  .pager {
    display: grid;
    grid-template-columns: 1fr;
  }

  .sold-card {
    padding: 15px 14px 13px;
  }

  .card-main {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .card-media {
    width: 100%;
    height: 176px;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }

  .footer-info {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .footer-status-group,
  .footer-amount-group {
    width: 100%;
  }
}
</style>







