<template>
  <div class="page-wrap">
    <section class="page-card" v-loading="loading">
      <UserCenterPageHeader
        eyebrow="我买到的"
        title="跟踪你的购买订单"
        description="梳理购买订单的状态、金额和联系信息，方便快速查看和处理。"
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
          <div class="board-copy">
            <h3 class="section-title">购买记录</h3>
            <p class="section-desc">优先查看商品状态、金额、卖家信息与下单进度。</p>
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
            <article v-for="row in orderList" :key="row.id" class="bought-card">
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
                    <span v-if="showCountdown(row)" class="footer-note" :class="`is-${countdownTone(row)}`">
                      {{ countdownText(row) }}
                    </span>
                  </div>
                  <div class="footer-amount-group">
                    <span class="footer-amount-label">{{ amountLabel(row) }}</span>
                    <strong class="footer-amount-value">{{ formatAmount(row.amount) }}</strong>
                  </div>
                </div>

                <div class="footer-actions">
                <el-button
                  v-if="hasOrderAction(row, 'pay')"
                  type="primary"
                  class="action-btn action-btn--primary"
                  :disabled="isPaymentExpired(row)"
                  @click="pay(row.id)"
                >
                  立即支付
                </el-button>
                <el-button
                  v-if="hasOrderAction(row, 'finish')"
                  type="success"
                  class="action-btn action-btn--success"
                  @click="finish(row)"
                >
                  确认收货
                </el-button>
                <el-button v-if="hasOrderAction(row, 'detail')" class="action-btn action-btn--plain" @click="goDetail(row.productId)">
                  查看商品
                </el-button>
                <el-button
                  v-if="hasOrderAction(row, 'contact')"
                  class="action-btn action-btn--plain"
                  :disabled="!canContactSeller(row)"
                  @click="contactSeller(row)"
                >
                  联系卖家
                </el-button>
                <el-button
                  v-if="hasOrderAction(row, 'cancel')"
                  class="action-btn action-btn--danger"
                  @click="cancel(row.id)"
                >
                  取消订单
                </el-button>
                <el-button
                  v-if="hasOrderAction(row, 'remove')"
                  class="action-btn action-btn--danger action-btn--delete"
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
import { computed, h, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElInput, ElMessage, ElMessageBox, ElRadio, ElRadioGroup } from "element-plus";
import { apiCreateOrGetProductSession } from "@/api/chat";
import { apiCancelOrder, apiFinishOrder, apiGetMyOrderPage, apiHideOrder, apiPayOrder } from "@/api/user";
import ListDataState from "@/components/common/ListDataState.vue";
import UserCenterFilterBar from "@/components/user-center/UserCenterFilterBar.vue";
import UserCenterPageHeader from "@/components/user-center/UserCenterPageHeader.vue";
import UserCenterStatusTag from "@/components/user-center/UserCenterStatusTag.vue";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { usePagedQuery } from "@/composables/usePagedQuery";
import { getApiErrorMessage } from "@/utils/apiError";
import { productImgUrl as resolveProductImgUrl } from "@/utils/img";
import { formatPrice } from "@/utils/price";
import { getBoughtOrderActionKeys, getCountdownTone, getOrderStatusMeta, type OrderActionKey } from "@/utils/tradeStatus";
import { getTradeDetail } from "@/utils/tradeLocation";

type OrderStatus = "UNPAID" | "PAID" | "CANCELLED" | "FINISHED" | string;
type PayType = "WECHAT" | "ALIPAY";
type ReviewLevel = "GOOD" | "NEUTRAL" | "BAD";

type OrderRow = {
  id: number;
  productId?: number;
  orderNo?: string;
  productTitle?: string | null;
  coverUrl?: string | null;
  sellerId?: number;
  sellerName?: string;
  phone?: string;
  receiver?: string;
  receiveAddress?: string;
  tradeLocation?: string;
  amount?: number | string | null;
  createdAt?: string;
  paidAt?: string | null;
  cancelledAt?: string | null;
  finishedAt?: string | null;
  payType?: string | null;
  status: OrderStatus;
};

const PAYMENT_TIMEOUT_MS = 30 * 60 * 1000;
const AUTO_RECEIVE_TIMEOUT_MS = 7 * 24 * 60 * 60 * 1000;
const router = useRouter();
const { runConfirmAction } = useConfirmAction();

const refreshTick = ref(Date.now());
let timer: ReturnType<typeof setInterval> | null = null;

const loading = ref(false);
const errorMessage = ref("");
const expiredOrderRefreshing = ref(false);
const orderList = ref<OrderRow[]>([]);
const total = ref(0);

const { query, reset: resetPagedQuery, changePage, search } = usePagedQuery({
  type: "BUY" as const,
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
    return "没有匹配的购买订单";
  }
  return "暂无购买记录";
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

function parseTimeMs(value?: string | null) {
  if (!value) return Number.NaN;
  const raw = String(value).trim();
  if (!raw) return Number.NaN;

  const directTime = new Date(raw).getTime();
  if (!Number.isNaN(directTime)) return directTime;

  const normalized = raw.replace("T", " ").replace(/\.\d+$/, "").replace(/-/g, "/");
  return new Date(normalized).getTime();
}

function formatMinuteSecond(ms: number) {
  const totalSeconds = Math.max(Math.floor(ms / 1000), 0);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}分${String(seconds).padStart(2, "0")}秒`;
}

function getPaymentRemainMs(row: OrderRow) {
  const createdTime = parseTimeMs(row.createdAt);
  if (!Number.isFinite(createdTime)) return Number.NaN;
  return createdTime + PAYMENT_TIMEOUT_MS - refreshTick.value;
}

function getAutoReceiveRemainMs(row: OrderRow) {
  const paidTime = parseTimeMs(row.paidAt);
  if (!Number.isFinite(paidTime)) return Number.NaN;
  return paidTime + AUTO_RECEIVE_TIMEOUT_MS - refreshTick.value;
}

function formatAutoReceiveRemain(ms: number) {
  const totalHours = Math.ceil(Math.max(ms, 0) / (60 * 60 * 1000));
  const days = Math.floor(totalHours / 24);
  const hours = totalHours % 24;
  if (days > 0 && hours > 0) return `${days}天${hours}小时`;
  if (days > 0) return `${days}天`;
  if (hours > 0) return `${hours}小时`;
  return "1小时内";
}

function isPaymentExpired(row: OrderRow) {
  if (row.status !== "UNPAID") return false;
  const remain = getPaymentRemainMs(row);
  return Number.isFinite(remain) && remain <= 0;
}

function paymentCountdownText(row: OrderRow) {
  const remain = getPaymentRemainMs(row);
  if (!Number.isFinite(remain)) return "支付倒计时同步中";
  return "请在 " + formatMinuteSecond(remain) + " 内完成支付";
}

function autoReceiveCountdownText(row: OrderRow) {
  const remain = getAutoReceiveRemainMs(row);
  if (!Number.isFinite(remain)) return "付款后 7 天将自动确认收货";
  if (remain <= 0) return "即将自动确认收货";
  return "还剩 " + formatAutoReceiveRemain(remain) + " 自动确认收货";
}

function showCountdown(row: OrderRow) {
  if (row.status === "UNPAID") return true;
  if (row.status === "PAID") return true;
  return isTimedOutCancelled(row);
}

function countdownText(row: OrderRow) {
  if (row.status === "PAID") return autoReceiveCountdownText(row);
  if (row.status === "UNPAID") {
    return isPaymentExpired(row) ? "订单状态同步中" : paymentCountdownText(row);
  }
  if (isTimedOutCancelled(row)) return "超时未支付，系统已自动取消";
  return "";
}

function countdownTone(row: OrderRow) {
  if (row.status === "PAID") return "warning";
  if (row.status === "UNPAID") {
    return isPaymentExpired(row) ? getCountdownTone("expired") : getCountdownTone("payment");
  }
  return getCountdownTone("expired");
}

function isTimedOutCancelled(row: OrderRow) {
  if (row.status !== "CANCELLED" || row.paidAt) return false;
  const createdTime = parseTimeMs(row.createdAt);
  if (!Number.isFinite(createdTime)) return false;
  return createdTime + PAYMENT_TIMEOUT_MS <= refreshTick.value;
}

function hasExpiredUnpaidOrders() {
  return orderList.value.some((row) => row.status === "UNPAID" && isPaymentExpired(row));
}

async function refreshExpiredOrdersIfNeeded() {
  if (expiredOrderRefreshing.value || loading.value || !hasExpiredUnpaidOrders()) {
    return;
  }
  expiredOrderRefreshing.value = true;
  try {
    await load();
  } finally {
    expiredOrderRefreshing.value = false;
  }
}

function reviewLevelText(level: ReviewLevel) {
  if (level === "GOOD") return "好评";
  if (level === "NEUTRAL") return "中评";
  return "差评";
}

function buildReview(level: ReviewLevel, content: string) {
  const text = content.trim();
  const prefix = reviewLevelText(level);
  return text ? prefix + ": " + text : prefix;
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

function canContactSeller(row: OrderRow) {
  const sellerId = Number(row.sellerId || 0);
  return Number.isFinite(sellerId) && sellerId > 0;
}

function hasOrderAction(row: OrderRow, action: OrderActionKey) {
  return getBoughtOrderActionKeys(row.status, isPaymentExpired(row)).includes(action);
}

function statusTone(row: OrderRow) {
  if (row.status === "CANCELLED") return "danger" as const;
  return getOrderStatusMeta(row.status).tone;
}

function amountLabel(row: OrderRow) {
  return row.paidAt ? "实付金额" : "订单金额";
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

  const items = [
    { label: "卖家", value: row.sellerName || "-", strong: true, ellipsis: true },
    { label: "联系方式", value: row.phone || "暂无联系方式" },
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
  return [
    { label: "下单时间", value: formatTime(row.createdAt), muted: !row.createdAt },
    { label: "支付时间", value: formatTime(row.paidAt), muted: !row.paidAt },
  ];
}

async function contactSeller(row: OrderRow) {
  if (!canContactSeller(row)) {
    ElMessage.warning("卖家信息不可用");
    return;
  }

  const sellerId = Number(row.sellerId);
  const productId = Number(row.productId || 0);

  if (!Number.isFinite(productId) || productId <= 0) {
    ElMessage.warning("未找到关联商品");
    return;
  }
  try {
    const session: any = await apiCreateOrGetProductSession({ productId, targetUserId: sellerId });
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

async function pay(id: number) {
  const payType = await choosePayType();
  if (!payType) return;

  try {
    const result: any = await apiPayOrder(id, payType);
    ElMessage.success(String(result.payTypeText || payType) + " 支付成功");
    await load();
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "支付失败"));
    await load();
  }
}

async function cancel(id: number) {
  await runConfirmAction({
    title: "提示",
    message: "确定取消这条订单吗？",
    successMessage: "已取消",
    action: () => apiCancelOrder(id),
    onSuccess: load,
  });
}

async function finish(row: OrderRow) {
  const form = reactive({
    level: "GOOD" as ReviewLevel,
    review: "",
  });

  try {
    await ElMessageBox({
      title: "确认收货",
      showCancelButton: true,
      closeOnClickModal: false,
      confirmButtonText: "确认",
      cancelButtonText: "取消",
      beforeClose: (action: string, _instance: unknown, done: () => void) => {
        if (action !== "confirm") {
          done();
          return;
        }
        if (form.review.trim().length > 120) {
          ElMessage.warning("评价内容不能超过 120 个字符");
          return;
        }
        if (form.level === "BAD" && !form.review.trim()) {
          ElMessage.warning("选择差评时请填写问题说明");
          return;
        }
        done();
      },
      message: () =>
        h("div", { class: "finish-dialog" }, [
          h("div", { class: "finish-tip" }, "确认收货后可选填写评价。"),
          h("div", { class: "finish-line" }, "商品：" + (row.productTitle || "-")),
          h("div", { class: "finish-line" }, "金额：" + formatAmount(row.amount)),
          h("div", { class: "finish-level" }, [
            h("span", { class: "finish-level-label" }, "评价"),
            h(
              ElRadioGroup,
              {
                modelValue: form.level,
                "onUpdate:modelValue": (value: string | number | boolean | undefined) => {
                  form.level = value === "BAD" ? "BAD" : value === "NEUTRAL" ? "NEUTRAL" : "GOOD";
                },
              },
              () => [
                h(ElRadio, { label: "GOOD" }, () => "好评"),
                h(ElRadio, { label: "NEUTRAL" }, () => "中评"),
                h(ElRadio, { label: "BAD" }, () => "差评"),
              ],
            ),
          ]),
          h(ElInput, {
            modelValue: form.review,
            type: "textarea",
            rows: 3,
            maxlength: 120,
            showWordLimit: true,
            placeholder: "可选填写评价详情",
            "onUpdate:modelValue": (value: string) => {
              form.review = value;
            },
          }),
        ]),
    });

    const review = buildReview(form.level, form.review);
    await apiFinishOrder(row.id, { review });
    ElMessage.success("订单已完成");
    await load();
  } catch (error) {
    if (error === "cancel" || error === "close") return;
    ElMessage.error(getApiErrorMessage(error, "确认收货失败"));
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

async function choosePayType(): Promise<PayType | null> {
  const payType = ref<PayType>("WECHAT");

  try {
    await ElMessageBox({
      title: "选择支付方式",
      showCancelButton: true,
      confirmButtonText: "确认支付",
      cancelButtonText: "取消",
      message: () =>
        h("div", { class: "pay-dialog" }, [
          h("div", { class: "pay-tip" }, "请选择支付方式，超时订单将自动取消。"),
          h(
            ElRadioGroup,
            {
              modelValue: payType.value,
              "onUpdate:modelValue": (value: string | number | boolean | undefined) => {
                payType.value = value === "ALIPAY" ? "ALIPAY" : "WECHAT";
              },
            },
            () => [
              h(ElRadio, { label: "WECHAT" }, () => "微信支付"),
              h(ElRadio, { label: "ALIPAY" }, () => "支付宝"),
            ],
          ),
        ]),
    });
    return payType.value;
  } catch {
    return null;
  }
}

onMounted(() => {
  void load();
  timer = setInterval(() => {
    refreshTick.value = Date.now();
    void refreshExpiredOrdersIfNeeded();
  }, 1000);
});

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
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
  min-width: 0;
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

.board-copy {
  min-width: 0;
}

.board-title,
.section-title {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
  line-height: 1.3;
  font-weight: 700;
}

.board-desc,
.section-desc {
  margin: 4px 0 0;
  color: #8b98ab;
  font-size: 12px;
  line-height: 1.55;
}

.board-chip,
.section-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(191, 219, 254, 0.85);
  background: rgba(248, 250, 255, 0.98);
  white-space: nowrap;
}

.section-chip {
  justify-content: center;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.board-chip span {
  color: #8b98ab;
  font-size: 12px;
}

.board-chip strong {
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 700;
}

.order-card {
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  gap: 16px;
  border: 1px solid #e6edf5;
  border-radius: 16px;
  background: #fff;
  padding: 16px 18px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.order-card:hover {
  transform: translateY(-1px);
  border-color: #dbeafe;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.07);
}

.order-card + .order-card {
  margin-top: 14px;
}

.cover-wrap {
  flex: 0 0 124px;
  width: 124px;
  height: 124px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #edf2f7;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  cursor: pointer;
  position: relative;
  align-self: center;
}

.cover-status-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 2;
}

.cover-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
}

:deep(.cover-image .el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 13px;
}

.order-main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.title-row {
  min-width: 0;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  flex-wrap: nowrap;
}

.goods-title {
  margin: 0;
  flex: 1 1 auto;
  min-width: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
  cursor: pointer;
}

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bought-card {
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

.bought-card:hover {
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

.footer-note.is-warning {
  color: #b45309;
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
  height: 34px;
  border-radius: 9px;
  border-color: #dbe6f2;
  color: #475569;
  background: #ffffff;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 600;
}

.action-btn--primary {
  font-weight: 700;
  box-shadow: none;
}

.action-btn--success {
  font-weight: 700;
  box-shadow: none;
}

.action-btn--plain {
  border-color: #dbe6f2;
  color: #334155;
  background: #ffffff;
  box-shadow: none;
}

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
  margin-top: 4px;
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

:deep(.pay-dialog),
:deep(.finish-dialog) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.pay-tip),
:deep(.finish-tip) {
  color: #666;
}

:deep(.finish-tip),
:deep(.finish-line) {
  line-height: 1.6;
}

:deep(.finish-level) {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

:deep(.finish-level-label) {
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
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

@media (max-width: 1120px) {
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
  .bought-card {
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

  .bought-card {
    padding: 15px 14px 13px;
  }

  .card-main {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .card-media {
    width: 100%;
    height: 184px;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }

}
</style>








