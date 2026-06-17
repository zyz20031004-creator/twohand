<template>
  <div class="page">
    <div class="page-title">订单信息管理</div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="订单编号/商品/买家/卖家/电话/地址/交易地点"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="订单状态" clearable class="status-select" @change="handleSearch">
          <el-option label="待支付" value="UNPAID" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已完成" value="FINISHED" />
        </el-select>
        <el-select v-model="query.payType" placeholder="支付方式" clearable class="paytype-select" @change="handleSearch">
          <el-option label="微信支付" value="WECHAT" />
          <el-option label="支付宝" value="ALIPAY" />
        </el-select>

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>

        <div class="spacer" />

        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>

      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-label">微信支付</div>
          <div class="stat-value">{{ payTypeStats.wechat }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">支付宝</div>
          <div class="stat-value">{{ payTypeStats.alipay }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">未支付/未记录</div>
          <div class="stat-value">{{ payTypeStats.unset }}</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="订单ID" width="90" align="center" />
        <el-table-column prop="productTitle" label="商品名称" min-width="160" />

        <el-table-column label="商品图片" width="120" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.coverUrl"
              :src="imgUrl(row.coverUrl)"
              style="width: 88px; height: 44px; border: 1px solid #eee"
              fit="cover"
              preview-teleported
              :preview-src-list="[imgUrl(row.coverUrl)]"
            />
            <div v-else class="img-ph">暂无</div>
          </template>
        </el-table-column>

        <el-table-column prop="orderNo" label="订单编号" min-width="180" />

        <el-table-column label="订单金额" width="110" align="center">
          <template #default="{ row }">{{ formatPrice(row.amount) }}</template>
        </el-table-column>

        <el-table-column prop="createdAt" label="下单时间" width="170" align="center">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column prop="paidAt" label="支付时间" width="170" align="center">
          <template #default="{ row }">{{ fmt(row.paidAt) }}</template>
        </el-table-column>

        <el-table-column prop="finishedAt" label="完成时间" width="170" align="center">
          <template #default="{ row }">{{ fmt(row.finishedAt) }}</template>
        </el-table-column>

        <el-table-column label="支付方式" width="110" align="center">
          <template #default="{ row }">{{ payTypeText(row.payType) }}</template>
        </el-table-column>

        <el-table-column prop="buyerName" label="买家" width="120" align="center" />
        <el-table-column prop="sellerName" label="卖家" width="120" align="center" />
        <el-table-column prop="receiveAddress" label="收货地址" min-width="220" />
        <el-table-column prop="tradeLocation" label="交易地点" min-width="220" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系方式" width="140" align="center" />
        <el-table-column prop="receiver" label="收货人" width="120" align="center" />

        <el-table-column label="订单状态" width="120" align="center">
          <template #default="{ row }">
            <span :class="statusClass(row.status)">{{ statusText(row.status) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="warning"
              link
              :disabled="row.status === 'CANCELLED' || row.status === 'FINISHED'"
              @click="handleCancel(row)"
            >
              强制取消
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  apiAdminOrderBatchDelete,
  apiAdminOrderCancel,
  apiAdminOrderDelete,
  apiAdminOrderPage,
  apiAdminOrderPayTypeStats,
} from "@/api/order";
import { productImgUrl as resolveProductImgUrl } from "@/utils/img";
import { formatPrice } from "@/utils/price";

type OrderStatus = "UNPAID" | "PAID" | "CANCELLED" | "FINISHED";

type OrderRow = {
  id: number;
  orderNo: string;
  productId: number;
  productTitle: string;
  coverUrl?: string;
  amount: number | string;
  status: OrderStatus;
  payType?: string | null;
  buyerId: number;
  buyerName: string;
  sellerId: number;
  sellerName: string;
  addressId?: number;
  receiver?: string;
  phone?: string;
  receiveAddress?: string;
  tradeLocation?: string;
  createdAt: string;
  paidAt?: string | null;
  cancelledAt?: string | null;
  finishedAt?: string | null;
};

type PayTypeStatRow = {
  payType: string;
  cnt: number | string;
};

const loading = ref(false);
const tableData = ref<OrderRow[]>([]);
const selectedIds = ref<number[]>([]);

const query = reactive({
  keyword: "",
  status: "" as "" | OrderStatus,
  payType: "" as "" | "WECHAT" | "ALIPAY",
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const payTypeStats = reactive({
  wechat: 0,
  alipay: 0,
  unset: 0,
});

function statusText(s: OrderStatus) {
  switch (s) {
    case "UNPAID":
      return "待支付";
    case "PAID":
      return "已支付";
    case "CANCELLED":
      return "已取消";
    case "FINISHED":
      return "已完成";
    default:
      return s;
  }
}

function statusClass(s: OrderStatus) {
  if (s === "FINISHED") return "tag-ok";
  if (s === "PAID") return "tag-mid";
  if (s === "CANCELLED") return "tag-bad";
  return "tag-wait";
}

function payTypeText(payType?: string | null) {
  if (payType === "WECHAT") return "微信支付";
  if (payType === "ALIPAY") return "支付宝";
  return "-";
}

function fmt(v: unknown) {
  if (!v) return "-";
  const s = String(v).replace("T", " ");
  return s.length >= 19 ? s.slice(0, 19) : s;
}


function imgUrl(url?: string) {
  return resolveProductImgUrl(url);
}

function resetStats() {
  payTypeStats.wechat = 0;
  payTypeStats.alipay = 0;
  payTypeStats.unset = 0;
}

async function fetchStats() {
  resetStats();
  const kw = query.keyword.trim();
  const st = query.status;

  const res: any = await apiAdminOrderPayTypeStats({
    keyword: kw || undefined,
    status: st || undefined,
    payType: query.payType || undefined,
  });

  const data = res?.data ?? res;
  const rows = Array.isArray(data?.stats) ? (data.stats as PayTypeStatRow[]) : [];

  for (const row of rows) {
    const cnt = Number(row.cnt || 0);
    if (row.payType === "WECHAT") payTypeStats.wechat = cnt;
    else if (row.payType === "ALIPAY") payTypeStats.alipay = cnt;
    else payTypeStats.unset = cnt;
  }
}

async function fetchList() {
  loading.value = true;
  try {
    const kw = query.keyword.trim();
    const st = query.status;

    const res: any = await apiAdminOrderPage({
      page: pagination.page,
      size: pagination.size,
      keyword: kw || undefined,
      status: st || undefined,
      payType: query.payType || undefined,
    });

    const data = res?.data ?? res;
    pagination.total = Number(data?.total || 0);
    tableData.value = Array.isArray(data?.records) ? data.records : [];

    await fetchStats();
  } catch (e: any) {
    ElMessage.error(e?.message || "加载失败");
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
  query.payType = "";
  pagination.page = 1;
  fetchList();
}

function handleSelectionChange(rows: OrderRow[]) {
  selectedIds.value = rows.map((r) => r.id);
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

async function handleDelete(row: OrderRow) {
  try {
    await ElMessageBox.confirm(`确定删除订单【${row.orderNo}】吗？`, "提示", {
      type: "warning",
    });
    await apiAdminOrderDelete(row.id);
    ElMessage.success("删除成功");
    fetchList();
  } catch {}
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return;
  try {
    await ElMessageBox.confirm("确定批量删除选中的订单吗？", "提示", {
      type: "warning",
    });
    await apiAdminOrderBatchDelete(selectedIds.value);
    ElMessage.success("批量删除成功");
    pagination.page = 1;
    fetchList();
  } catch {}
}

async function handleCancel(row: OrderRow) {
  try {
    await ElMessageBox.confirm(`确定强制取消订单【${row.orderNo}】吗？`, "提示", {
      type: "warning",
    });
    await apiAdminOrderCancel(row.id);
    ElMessage.success("已取消");
    fetchList();
  } catch {}
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

.status-select,
.paytype-select {
  width: 140px;
}

.stats-row {
  margin-top: 14px;
  display: flex;
  gap: 12px;
}

.stat-card {
  min-width: 140px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
}

.stat-label {
  color: #666;
  font-size: 12px;
}

.stat-value {
  margin-top: 4px;
  font-size: 22px;
  font-weight: 700;
  color: #222;
}

.img-ph {
  width: 88px;
  height: 44px;
  border: 1px dashed #bbb;
  background: #f5f5f5;
  color: #666;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.tag-ok {
  padding: 2px 8px;
  border: 1px solid #999;
  background: #efefef;
  color: #222;
  border-radius: 4px;
}

.tag-mid {
  padding: 2px 8px;
  border: 1px solid #999;
  background: #f7f7f7;
  color: #333;
  border-radius: 4px;
}

.tag-wait {
  padding: 2px 8px;
  border: 1px dashed #999;
  background: #fafafa;
  color: #444;
  border-radius: 4px;
}

.tag-bad {
  padding: 2px 8px;
  border: 1px solid #aaa;
  background: #fff;
  color: #666;
  border-radius: 4px;
  text-decoration: line-through;
}

.spacer {
  flex: 1;
}
</style>

