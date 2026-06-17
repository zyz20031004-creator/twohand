<template>
  <div class="page">
    <div class="page-title">二手商品管理</div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="请输入关键字查询"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.school"
          placeholder="学校筛选"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.auditStatus" placeholder="审核状态" clearable class="sel">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-select v-model="query.saleStatus" placeholder="上架状态" clearable class="sel">
          <el-option label="上架中" value="ON" />
          <el-option label="已下架" value="OFF" />
        </el-select>
        <el-button type="primary" class="action-btn" @click="handleSearch">查询</el-button>
        <el-button class="action-btn" @click="handleReset">重置</el-button>

        <div class="spacer" />
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        row-key="id"
      >
        <el-table-column prop="id" label="商品ID" width="90" align="center" />
        <el-table-column prop="name" label="商品名称" min-width="160" />

        <el-table-column label="价格" width="110" align="center">
          <template #default="{ row }">
            {{ formatPrice(row.price) }}
          </template>
        </el-table-column>

        <el-table-column prop="shipAddress" label="发货地址" min-width="180" />
        <el-table-column prop="schoolName" label="所属学校" min-width="150" show-overflow-tooltip />

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

        <el-table-column prop="createdAt" label="上架日期" width="140" align="center" />

        <el-table-column label="审核状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row)" effect="plain">
              {{ auditText(getAuditStatus(row)) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="驳回/下架原因" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ reasonText(row) }}
          </template>
        </el-table-column>

        <el-table-column prop="categoryName" label="商品分类" width="120" align="center" />

        <el-table-column label="所属用户" min-width="180" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatOwnerName(row) }}
          </template>
        </el-table-column>

        <el-table-column label="上架状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getSaleStatus(row) === 'ON' ? 'success' : 'info'" effect="plain">
              {{ getSaleStatus(row) === "ON" ? "上架中" : "已下架" }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" align="center" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
              <el-button v-if="showApprove(row)" type="success" link @click="handleApprove(row)">通过</el-button>
              <el-button v-if="showReject(row)" type="warning" link @click="handleReject(row)">驳回</el-button>
              <el-button v-if="showOff(row)" type="warning" link @click="handleOff(row)">下架</el-button>
              <el-button v-if="showOn(row)" type="primary" link @click="handleOn(row)">上架</el-button>
            </div>
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

    <el-dialog v-model="detail.visible" title="商品详情" width="560px" destroy-on-close>
      <div class="detail">
        <div class="detail-row"><span class="k">商品ID：</span>{{ detail.data?.id }}</div>
        <div class="detail-row"><span class="k">商品名称：</span>{{ detail.data?.name }}</div>
        <div class="detail-row"><span class="k">价格：</span>{{ formatPrice(detail.data?.price) }}</div>
        <div class="detail-row"><span class="k">发货地址：</span>{{ detail.data?.shipAddress }}</div>
        <div class="detail-row"><span class="k">所属学校：</span>{{ detail.data?.schoolName || "-" }}</div>
        <div class="detail-row"><span class="k">上架日期：</span>{{ detail.data?.createdAt }}</div>
        <div class="detail-row"><span class="k">审核状态：</span>{{ auditText(getAuditStatus(detail.data)) }}</div>
        <div v-if="detail.data?.auditReason" class="detail-row detail-row--stack">
          <span class="k">驳回/下架原因：</span>
          <div class="reason-box">{{ reasonText(detail.data) }}</div>
        </div>
        <div class="detail-row"><span class="k">分类：</span>{{ detail.data?.categoryName }}</div>
        <div class="detail-row"><span class="k">所属用户：</span>{{ formatOwnerName(detail.data) }}</div>
        <div class="detail-row"><span class="k">上架状态：</span>{{ getSaleStatus(detail.data) === "ON" ? "上架中" : "已下架" }}</div>
        <div class="detail-row"><span class="k">描述：</span></div>
        <div class="desc">{{ detail.data?.description || "-" }}</div>

        <div class="img-box">
          <div v-if="detailImages.length" class="gallery">
            <el-image
              :src="activeDetailImageUrl"
              class="gallery-main"
              fit="cover"
              preview-teleported
              :preview-src-list="detailPreviewList"
              :initial-index="detail.activeImageIndex"
            />
            <div class="gallery-thumbs">
              <button
                v-for="(item, index) in detailImages"
                :key="`${item.url}-${index}`"
                type="button"
                class="thumb-btn"
                :class="{ active: index === detail.activeImageIndex }"
                @click="setActiveDetailImage(index)"
              >
                <img class="thumb-img" :src="imgUrl(item.url)" :alt="`商品图片${index + 1}`" />
              </button>
            </div>
          </div>
          <div v-else class="img-ph big">暂无图片</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detail.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  apiAdminProductApprove,
  apiAdminProductDetail,
  apiAdminProductOff,
  apiAdminProductOn,
  apiAdminProductPage,
  apiAdminProductReject,
} from "@/api/adminProduct";
import { productImgUrl as resolveProductImgUrl, sanitizeProductImageUrl } from "@/utils/img";
import { formatPrice } from "@/utils/price";

function imgUrl(url?: string) {
  return resolveProductImgUrl(url);
}

type AuditStatus = "PENDING" | "APPROVED" | "REJECTED";
type SaleStatus = "ON" | "OFF";

type ProductImageItem = {
  id?: number;
  productId?: number;
  url: string;
  sort?: number;
};

type Product = {
  id: number;
  name: string;
  price: number | string;
  description?: string;
  shipAddress?: string;
  createdAt?: string;
  auditStatus?: AuditStatus;
  auditReason?: string;
  categoryName?: string;
  username?: string;
  sellerNickName?: string;
  sellerUsername?: string;
  schoolName?: string;
  saleStatus?: SaleStatus;
  soldFlag?: number | string | boolean;
  coverUrl?: string;
  images?: ProductImageItem[];
};

const loading = ref(false);
const tableData = ref<Product[]>([]);

const query = reactive({
  keyword: "",
  school: "",
  auditStatus: "" as "" | AuditStatus,
  saleStatus: "" as "" | SaleStatus,
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const detail = reactive({
  visible: false,
  data: null as Product | null,
  activeImageIndex: 0,
});

function normalizeText(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}

function getAuditStatus(row?: Partial<Product> | Record<string, any> | null) {
  const value = row?.auditStatus ?? (row as any)?.audit_status;
  const text = normalizeText(value);
  if (text === "APPROVED" || text === "REJECTED" || text === "PENDING") {
    return text as AuditStatus;
  }
  return "PENDING";
}

function getSaleStatus(row?: Partial<Product> | Record<string, any> | null) {
  const value = row?.saleStatus ?? (row as any)?.sale_status ?? (row as any)?.status;
  const text = normalizeText(value);
  if (text === "ON" || text === "OFF") {
    return text as SaleStatus;
  }
  return "OFF";
}

function isSold(row?: Partial<Product> | Record<string, any> | null) {
  const value = row?.soldFlag ?? (row as any)?.sold_flag ?? 0;
  return value === 1 || value === "1" || value === true;
}

function auditText(status?: AuditStatus) {
  if (status === "APPROVED") return "通过";
  if (status === "REJECTED") return "驳回";
  return "待审核";
}

function auditTagType(row?: Product | null) {
  const status = getAuditStatus(row);
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "warning";
}

function reasonText(row?: Product | null) {
  return String(row?.auditReason || "").trim() || "-";
}

function formatOwnerName(row?: Product | null) {
  const username = normalizeText(row?.sellerUsername || row?.username);
  const displayName = normalizeText(row?.sellerNickName) || username;
  if (!displayName || !username) {
    return "未知用户";
  }
  return `${displayName}（${username}）`;
}

function showApprove(row: Product) {
  return getAuditStatus(row) === "PENDING" && !isSold(row);
}

function showReject(row: Product) {
  return getAuditStatus(row) === "PENDING" && !isSold(row);
}

function isAdminDown(row?: Product | null) {
  return getSaleStatus(row) === "OFF" && String(row?.auditReason || "").includes("管理员下架");
}

function canPutOn(row?: Product | null) {
  return getAuditStatus(row) === "APPROVED" && getSaleStatus(row) === "OFF" && !isSold(row) && !isAdminDown(row);
}

function showOff(row: Product) {
  return getAuditStatus(row) === "APPROVED" && getSaleStatus(row) === "ON" && !isSold(row);
}

function showOn(row: Product) {
  return canPutOn(row);
}

function sanitizeImageUrl(url: unknown) {
  if (typeof url !== "string") return "";
  return sanitizeProductImageUrl(url);
}

function normalizeImages(source: unknown, fallback?: unknown) {
  const list = Array.isArray(source) ? source : [];
  const seen = new Set<string>();
  const images: ProductImageItem[] = [];

  function push(rawUrl: unknown, rawSort?: unknown, rawId?: unknown) {
    const url = sanitizeImageUrl(rawUrl);
    if (!url || seen.has(url)) return;
    seen.add(url);

    const sort = Number(rawSort);
    const id = Number(rawId);
    images.push({
      url,
      sort: Number.isFinite(sort) && sort > 0 ? sort : images.length + 1,
      id: Number.isFinite(id) && id > 0 ? id : undefined,
    });
  }

  list.forEach((item, index) => {
    if (typeof item === "string") {
      push(item, index + 1);
      return;
    }
    if (item && typeof item === "object") {
      const record = item as Record<string, unknown>;
      push(record.url, record.sort ?? index + 1, record.id);
    }
  });

  if (!images.length) {
    push(fallback, 1);
  }

  return images
    .sort((a, b) => {
      const sortDiff = Number(a.sort || 0) - Number(b.sort || 0);
      if (sortDiff !== 0) return sortDiff;
      return Number(a.id || 0) - Number(b.id || 0);
    })
    .map((item, index) => ({
      ...item,
      sort: index + 1,
    }));
}

function mapProduct(source: Record<string, any>): Product {
  const images = normalizeImages(source.images, source.coverUrl);
  const sold =
    source.soldFlag === 1 ||
    source.soldFlag === "1" ||
    source.soldFlag === true ||
    source.sold_flag === 1 ||
    source.sold_flag === "1" ||
    source.sold_flag === true;

  return {
    id: Number(source.id),
    name: source.name ?? source.title ?? "",
    price: source.price ?? 0,
    description: source.description ?? "",
    shipAddress: source.shipAddress ?? source.addressText ?? "",
    createdAt: source.createdAt ?? "",
    auditStatus: getAuditStatus(source),
    auditReason: source.auditReason ?? source.audit_reason ?? "",
    categoryName: source.categoryName ?? "",
    username: source.username ?? source.sellerUsername ?? source.seller_username ?? "",
    sellerNickName: source.sellerNickName ?? source.seller_nick_name ?? "",
    sellerUsername: source.sellerUsername ?? source.seller_username ?? source.username ?? "",
    schoolName: source.schoolName ?? source.school_name ?? "",
    saleStatus: getSaleStatus(source),
    soldFlag: sold ? 1 : 0,
    coverUrl: images[0]?.url || sanitizeImageUrl(source.coverUrl),
    images,
  };
}

const detailImages = computed<ProductImageItem[]>(() => normalizeImages(detail.data?.images, detail.data?.coverUrl));
const detailPreviewList = computed(() => detailImages.value.map((item) => imgUrl(item.url)).filter(Boolean));
const activeDetailImageUrl = computed(() => {
  const current = detailImages.value[detail.activeImageIndex] || detailImages.value[0];
  return current ? imgUrl(current.url) : "";
});

function setActiveDetailImage(index: number) {
  if (index < 0 || index >= detailImages.value.length) {
    detail.activeImageIndex = 0;
    return;
  }
  detail.activeImageIndex = index;
}

async function fetchList() {
  loading.value = true;
  try {
    const data: any = await apiAdminProductPage({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword || undefined,
      school: query.school || undefined,
      auditStatus: query.auditStatus || undefined,
      saleStatus: query.saleStatus || undefined,
    });

    const list = Array.isArray(data?.list) ? data.list : [];
    pagination.total = Number(data?.total || 0);

    tableData.value = [];
    await nextTick();
    tableData.value = list.map((item: any) => mapProduct(item));

    if (tableData.value.length === 0 && pagination.page > 1) {
      pagination.page -= 1;
      await fetchList();
    }
  } catch (e: any) {
    console.error(e);
    ElMessage.error(e?.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

async function handleApprove(row: Product) {
  try {
    await ElMessageBox.confirm(`确定通过「${row.name}」吗？`, "提示", { type: "warning" });
    await apiAdminProductApprove(row.id);
    ElMessage.success("已通过");
    await fetchList();
  } catch (e) {
    console.error(e);
  }
}

async function handleReject(row: Product) {
  try {
    const { value } = (await ElMessageBox.prompt("请输入驳回原因（必填）", "驳回审核", {
      inputValidator: (val) => (!val || !String(val).trim() ? "不能为空" : true),
    })) as any;
    await apiAdminProductReject(row.id, { reason: String(value).trim() });
    ElMessage.success("已驳回");
    await fetchList();
  } catch (e) {
    console.error(e);
  }
}

async function handleOff(row: Product) {
  try {
    const { value } = (await ElMessageBox.prompt("请输入下架原因（必填）", "下架商品", {
      inputValidator: (val) => (!val || !String(val).trim() ? "不能为空" : true),
    })) as any;
    await apiAdminProductOff(row.id, { reason: String(value).trim() });
    ElMessage.success("已下架");
    await fetchList();
  } catch (e) {
    console.error(e);
  }
}

async function handleOn(row: Product) {
  try {
    await ElMessageBox.confirm(`确定重新上架「${row.name}」吗？`, "提示", { type: "warning" });
    await apiAdminProductOn(row.id);
    ElMessage.success("已上架");
    await fetchList();
  } catch (e) {
    console.error(e);
  }
}

async function openDetail(row: Product) {
  try {
    const data: any = await apiAdminProductDetail(row.id);
    detail.data = data ? mapProduct(data) : null;
  } catch {
    detail.data = mapProduct({ ...row });
  } finally {
    detail.activeImageIndex = 0;
    detail.visible = true;
  }
}

function handleSearch() {
  pagination.page = 1;
  fetchList();
}

function handleReset() {
  query.keyword = "";
  query.school = "";
  query.auditStatus = "";
  query.saleStatus = "";
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
  flex-wrap: nowrap;
  overflow-x: auto;
}

.keyword-input {
  width: 240px;
}

.sel {
  width: 156px;
  flex: 0 0 156px;
}

.action-btn {
  width: 80px;
}

.spacer {
  flex: 1;
  min-width: 12px;
}

.img-ph {
  width: 88px;
  height: 40px;
  border: 1px dashed #bbb;
  background: #f5f5f5;
  color: #666;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.img-ph.big {
  width: 100%;
  height: 180px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #333;
}

.detail-row .k {
  color: #666;
}

.detail-row--stack {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.reason-box {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid #fecdd3;
  border-radius: 6px;
  background: #fff1f2;
  color: #be123c;
  line-height: 1.6;
}

.desc {
  padding: 10px;
  border: 1px dashed #ccc;
  background: #fafafa;
  color: #444;
  line-height: 1.6;
}

.img-box {
  margin-top: 10px;
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gallery-main {
  width: 100%;
  height: 220px;
  border: 1px solid #eee;
  border-radius: 6px;
  overflow: hidden;
  background: #fafafa;
}

.gallery-thumbs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.thumb-btn {
  padding: 0;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  line-height: 0;
}

.thumb-btn.active {
  border-color: #409eff;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.2);
}

.thumb-img {
  width: 72px;
  height: 72px;
  display: block;
  object-fit: cover;
  background: #f5f5f5;
}
</style>
