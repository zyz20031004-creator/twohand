<template>
  <div class="page">
    <div class="page-title">举报管理</div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索：举报人 / 商品标题 / 商品ID"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="处理状态" clearable class="status-select">
          <el-option label="待处理" value="PENDING" />
          <el-option label="举报成立" value="VALID" />
          <el-option label="举报不成立" value="INVALID" />
          <el-option label="已处理" value="HANDLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="举报ID" width="90" align="center" />
        <el-table-column prop="productId" label="商品ID" width="100" align="center" />
        <el-table-column label="商品标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.productTitle || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="举报人" min-width="150">
          <template #default="{ row }">
            <span>{{ reporterText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="举报原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="当前状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报时间" width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.handledAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" link @click="openHandle(row, 'VALID')">举报成立</el-button>
              <el-button type="danger" link @click="openHandle(row, 'INVALID')">举报不成立</el-button>
            </template>
            <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
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

    <el-dialog
      v-model="detailDialog.visible"
      title="举报详情"
      width="1080px"
      destroy-on-close
      @closed="resetDetailDialog"
    >
      <div v-if="detailDialog.data" class="detail-scroll">
        <div class="detail-wrap">
          <section class="detail-section">
            <div class="section-title">举报信息</div>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">举报ID</span>
                <strong>{{ detailDialog.data.id }}</strong>
              </div>
              <div class="detail-item">
                <span class="detail-label">举报人</span>
                <strong>{{ reporterText(detailDialog.data) }}</strong>
              </div>
              <div class="detail-item">
                <span class="detail-label">当前状态</span>
                <el-tag :type="statusTagType(detailDialog.data.status)" effect="plain">
                  {{ statusText(detailDialog.data.status) }}
                </el-tag>
              </div>
              <div class="detail-item">
                <span class="detail-label">举报原因</span>
                <strong>{{ formatText(detailDialog.data.reason) }}</strong>
              </div>
              <div class="detail-item">
                <span class="detail-label">举报时间</span>
                <strong>{{ formatTime(detailDialog.data.createdAt) }}</strong>
              </div>
              <div class="detail-item">
                <span class="detail-label">处理时间</span>
                <strong>{{ formatTime(detailDialog.data.handledAt) }}</strong>
              </div>
              <div class="detail-item">
                <span class="detail-label">处理人</span>
                <strong>{{ handlerText(detailDialog.data) }}</strong>
              </div>
              <div class="detail-item detail-item-full">
                <span class="detail-label">补充说明</span>
                <div class="plain-box compact-box" :class="{ empty: !detailDialog.data.detail }">
                  {{ formatText(detailDialog.data.detail, "暂无补充说明") }}
                </div>
              </div>
              <div class="detail-item detail-item-full">
                <span class="detail-label">处理备注</span>
                <div class="plain-box compact-box" :class="{ empty: !detailDialog.data.handleRemark }">
                  {{ formatText(detailDialog.data.handleRemark, "暂无处理备注") }}
                </div>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="section-head">
              <div class="section-title">被举报商品概览</div>
              <el-button v-if="detailDialog.product" type="primary" link @click="openProductPage">
                查看商品详情
              </el-button>
            </div>

            <div class="product-panel" v-loading="detailDialog.loading" element-loading-text="正在加载商品信息...">
              <template v-if="detailDialog.product">
                <div class="product-layout">
                  <div class="gallery-panel">
                    <div v-if="detailImages.length" class="gallery compact-gallery">
                      <el-image
                        :src="activeDetailImageUrl"
                        class="gallery-main"
                        fit="cover"
                        preview-teleported
                        :preview-src-list="detailPreviewList"
                        :initial-index="detailDialog.activeImageIndex"
                      />
                      <div class="gallery-thumbs">
                        <button
                          v-for="(item, index) in detailImages"
                          :key="`${item.url}-${index}`"
                          type="button"
                          class="thumb-btn"
                          :class="{ active: index === detailDialog.activeImageIndex }"
                          @click="setActiveDetailImage(index)"
                        >
                          <img class="thumb-img" :src="imgUrl(item.url)" :alt="`商品图片${index + 1}`" />
                        </button>
                      </div>
                    </div>
                    <div v-else class="gallery-empty">暂无商品图片</div>
                  </div>

                  <div class="product-summary">
                    <div class="product-summary-top">
                      <div class="product-heading">
                        <div class="product-title">{{ formatText(detailDialog.product.name) }}</div>
                        <div class="product-subtitle">商品ID：{{ detailDialog.product.id }}</div>
                      </div>
                      <div class="product-price">{{ formatPrice(detailDialog.product.price) }}</div>
                    </div>

                    <div class="product-tags">
                      <el-tag :type="saleTagType(detailDialog.product.saleStatus)" effect="plain" size="small">
                        {{ saleText(detailDialog.product.saleStatus) }}
                      </el-tag>
                      <el-tag :type="auditTagType(detailDialog.product.auditStatus)" effect="plain" size="small">
                        {{ auditText(detailDialog.product.auditStatus) }}
                      </el-tag>
                      <el-tag v-if="detailDialog.product.categoryName" type="info" effect="plain" size="small">
                        {{ detailDialog.product.categoryName }}
                      </el-tag>
                    </div>

                    <div class="summary-grid">
                      <div class="summary-item">
                        <span class="summary-label">卖家信息</span>
                        <span class="summary-value">{{ sellerText(detailDialog.product) }}</span>
                      </div>
                      <div class="summary-item">
                        <span class="summary-label">交易地点</span>
                        <span class="summary-value">{{ formatText(detailDialog.product.shipAddress, "暂无地点") }}</span>
                      </div>
                      <div class="summary-item">
                        <span class="summary-label">发布时间</span>
                        <span class="summary-value">{{ formatTime(detailDialog.product.createdAt) }}</span>
                      </div>
                      <div class="summary-item">
                        <span class="summary-label">商品分类</span>
                        <span class="summary-value">{{ formatText(detailDialog.product.categoryName, "未分类") }}</span>
                      </div>
                    </div>

                    <div class="summary-block">
                      <div class="summary-block-head">
                        <span class="summary-block-title">商品描述</span>
                        <el-button
                          v-if="canToggleDescription"
                          type="primary"
                          link
                          class="toggle-link"
                          @click="toggleDescription"
                        >
                          {{ detailDialog.descriptionExpanded ? "收起" : "展开" }}
                        </el-button>
                      </div>
                      <div
                        class="summary-desc"
                        :class="{
                          empty: !detailDialog.product.description,
                          expanded: detailDialog.descriptionExpanded,
                        }"
                      >
                        {{ formatText(detailDialog.product.description, "暂无商品描述") }}
                      </div>
                    </div>

                    <div class="summary-block" v-if="detailDialog.product.auditReason">
                      <div class="summary-block-head">
                        <span class="summary-block-title">审核备注</span>
                      </div>
                      <div class="summary-desc expanded">
                        {{ formatText(detailDialog.product.auditReason) }}
                      </div>
                    </div>
                  </div>
                </div>
              </template>

              <div v-else class="product-empty">
                <el-empty :description="detailDialog.productError || '商品信息不存在或已被删除'" />
              </div>
            </div>
          </section>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleDialog.visible" :title="handleDialogTitle" width="560px" destroy-on-close>
      <div class="handle-summary">
        <div>举报ID：{{ handleDialog.data?.id || "-" }}</div>
        <div>商品：{{ handleDialog.data?.productTitle || `商品ID ${handleDialog.data?.productId || "-"}` }}</div>
        <div>举报原因：{{ handleDialog.data?.reason || "-" }}</div>
      </div>

      <el-form ref="handleFormRef" :model="handleForm" :rules="handleRules" label-width="88px">
        <el-form-item label="处理结果">
          <el-tag :type="statusTagType(handleDialog.status || 'PENDING')" effect="plain">
            {{ statusText(handleDialog.status || "PENDING") }}
          </el-tag>
        </el-form-item>
        <el-form-item label="处理备注" prop="handleRemark">
          <el-input
            v-model="handleForm.handleRemark"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            placeholder="可选填写处理说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="handling" @click="handleSave">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import {
  apiAdminProductDetail,
  type AdminProductDetailResp,
  type ProductImageItem,
} from "@/api/product";
import {
  apiAdminReportHandle,
  apiAdminReportPage,
  type AdminReportItem,
  type ReportHandleStatus,
  type ReportStatus,
} from "@/api/report";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { getApiErrorMessage } from "@/utils/apiError";
import { productImgUrl as resolveProductImgUrl, sanitizeProductImageUrl } from "@/utils/img";
import { formatPrice } from "@/utils/price";

type AuditStatus = "PENDING" | "APPROVED" | "REJECTED";
type SaleStatus = "ON" | "OFF";

type ReportRow = AdminReportItem & {
  id: number;
  reporterId: number;
  reporterName: string;
  reporterNickName: string;
  reporterUsername: string;
  productId: number;
  productTitle: string;
  reason: string;
  detail: string;
  status: ReportStatus;
  handleRemark: string;
  handledBy?: number;
  handlerName: string;
  handlerNickName: string;
  handlerUsername: string;
  handledAt: string;
  createdAt: string;
};

type ProductRow = {
  id: number;
  name: string;
  price: number | string;
  description: string;
  shipAddress: string;
  createdAt: string;
  auditStatus: AuditStatus;
  auditReason: string;
  categoryName: string;
  userId?: number;
  username: string;
  sellerNickName: string;
  sellerUsername: string;
  saleStatus: SaleStatus;
  coverUrl: string;
  images: ProductImageItem[];
};

const { runConfirmAction } = useConfirmAction();

const loading = ref(false);
const handling = ref(false);
const tableData = ref<ReportRow[]>([]);
const handleFormRef = ref<FormInstance>();
let detailRequestToken = 0;

const query = reactive({
  keyword: "",
  status: "" as "" | ReportStatus,
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const detailDialog = reactive({
  visible: false,
  loading: false,
  data: null as ReportRow | null,
  product: null as ProductRow | null,
  productError: "",
  activeImageIndex: 0,
  descriptionExpanded: false,
});

const handleDialog = reactive({
  visible: false,
  data: null as ReportRow | null,
  status: "" as "" | ReportHandleStatus,
});

const handleForm = reactive({
  handleRemark: "",
});

function validateHandleRemark(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (!String(value || "").trim()) {
    callback(new Error("请输入处理说明"));
    return;
  }
  callback();
}

const handleRules: FormRules = {
  handleRemark: [
    { validator: validateHandleRemark, trigger: "blur" },
    { max: 300, message: "处理备注不能超过 300 个字符", trigger: "blur" },
  ],
};

const handleDialogTitle = computed(() => {
  if (handleDialog.status === "VALID") return "举报成立";
  if (handleDialog.status === "INVALID") return "举报不成立";
  return "处理举报";
});

const detailImages = computed<ProductImageItem[]>(() => {
  if (!detailDialog.product) return [];
  return normalizeImages(detailDialog.product.images, detailDialog.product.coverUrl);
});

const detailPreviewList = computed(() => {
  return detailImages.value.map((item) => imgUrl(item.url)).filter(Boolean);
});

const activeDetailImageUrl = computed(() => {
  const current = detailImages.value[detailDialog.activeImageIndex] || detailImages.value[0];
  return current ? imgUrl(current.url) : "";
});

const canToggleDescription = computed(() => {
  const text = String(detailDialog.product?.description || "").trim();
  return text.length > 72;
});

function imgUrl(url?: string) {
  return resolveProductImgUrl(url);
}

function stringValue(value: unknown) {
  return value == null ? "" : String(value);
}

function numberValue(value: unknown) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function formatText(value?: string, fallback = "-") {
  const text = String(value || "").trim();
  return text || fallback;
}

function formatTime(value?: string) {
  if (!value) return "-";
  const text = String(value).replace("T", " ");
  return text.length >= 19 ? text.slice(0, 19) : text;
}

function normalizeStatus(value: unknown): ReportStatus {
  const text = String(value || "").toUpperCase();
  if (text === "VALID") return "VALID";
  if (text === "INVALID") return "INVALID";
  if (text === "HANDLED") return "HANDLED";
  return "PENDING";
}

function normalizeAuditStatus(value: unknown): AuditStatus {
  const text = String(value || "").toUpperCase();
  if (text === "APPROVED") return "APPROVED";
  if (text === "REJECTED") return "REJECTED";
  return "PENDING";
}

function normalizeSaleStatus(value: unknown): SaleStatus {
  return String(value || "").toUpperCase() === "ON" ? "ON" : "OFF";
}

function statusText(status: ReportStatus) {
  if (status === "VALID") return "举报成立";
  if (status === "INVALID") return "举报不成立";
  if (status === "HANDLED") return "已处理";
  return "待处理";
}

function statusTagType(status: ReportStatus) {
  if (status === "VALID") return "success";
  if (status === "INVALID") return "danger";
  if (status === "HANDLED") return "info";
  return "warning";
}

function auditText(status?: AuditStatus) {
  if (status === "APPROVED") return "审核通过";
  if (status === "REJECTED") return "审核驳回";
  return "待审核";
}

function auditTagType(status?: AuditStatus) {
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "warning";
}

function saleText(status?: SaleStatus) {
  return status === "ON" ? "上架中" : "已下架";
}

function saleTagType(status?: SaleStatus) {
  return status === "ON" ? "success" : "info";
}

function formatUserDisplay(name?: string, username?: string) {
  const normalizedName = String(name || "").trim();
  const normalizedUsername = String(username || "").trim();
  const displayName = normalizedName || normalizedUsername;
  if (!displayName && !normalizedUsername) {
    return "未知用户";
  }
  if (!normalizedUsername) {
    return displayName || "未知用户";
  }
  return `${displayName}（${normalizedUsername}）`;
}

function reporterText(row: ReportRow) {
  return formatUserDisplay(row.reporterNickName || row.reporterName, row.reporterUsername);
}

function handlerText(row: ReportRow) {
  return formatUserDisplay(row.handlerNickName || row.handlerName, row.handlerUsername);
}

function sellerText(product: ProductRow) {
  return formatUserDisplay(product.sellerNickName || product.username, product.sellerUsername);
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

function mapRow(item: any): ReportRow {
  return {
    id: numberValue(item?.id),
    reporterId: numberValue(item?.reporterId),
    reporterName: stringValue(item?.reporterName),
    reporterNickName: stringValue(item?.reporterNickName),
    reporterUsername: stringValue(item?.reporterUsername),
    productId: numberValue(item?.productId),
    productTitle: stringValue(item?.productTitle),
    reason: stringValue(item?.reason),
    detail: stringValue(item?.detail),
    status: normalizeStatus(item?.status),
    handleRemark: stringValue(item?.handleRemark),
    handledBy: numberValue(item?.handledBy) || undefined,
    handlerName: stringValue(item?.handlerName),
    handlerNickName: stringValue(item?.handlerNickName),
    handlerUsername: stringValue(item?.handlerUsername),
    handledAt: stringValue(item?.handledAt),
    createdAt: stringValue(item?.createdAt),
  };
}

function mapProduct(item: AdminProductDetailResp | Record<string, unknown>): ProductRow {
  const images = normalizeImages((item as any)?.images, (item as any)?.coverUrl);
  return {
    id: numberValue((item as any)?.id),
    name: stringValue((item as any)?.name || (item as any)?.title),
    price: (item as any)?.price ?? 0,
    description: stringValue((item as any)?.description),
    shipAddress: stringValue((item as any)?.shipAddress || (item as any)?.addressText),
    createdAt: stringValue((item as any)?.createdAt),
    auditStatus: normalizeAuditStatus((item as any)?.auditStatus),
    auditReason: stringValue((item as any)?.auditReason),
    categoryName: stringValue((item as any)?.categoryName),
    userId: numberValue((item as any)?.userId) || undefined,
    username: stringValue((item as any)?.username),
    sellerNickName: stringValue((item as any)?.sellerNickName),
    sellerUsername: stringValue((item as any)?.sellerUsername),
    saleStatus: normalizeSaleStatus((item as any)?.saleStatus || (item as any)?.status),
    coverUrl: images[0]?.url || sanitizeImageUrl((item as any)?.coverUrl),
    images,
  };
}

function setActiveDetailImage(index: number) {
  if (index < 0 || index >= detailImages.value.length) {
    detailDialog.activeImageIndex = 0;
    return;
  }
  detailDialog.activeImageIndex = index;
}

function toggleDescription() {
  detailDialog.descriptionExpanded = !detailDialog.descriptionExpanded;
}

function resetDetailDialog() {
  detailRequestToken += 1;
  detailDialog.loading = false;
  detailDialog.data = null;
  detailDialog.product = null;
  detailDialog.productError = "";
  detailDialog.activeImageIndex = 0;
  detailDialog.descriptionExpanded = false;
}

async function fetchList() {
  loading.value = true;
  try {
    const data = await apiAdminReportPage({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword.trim() || undefined,
      status: query.status || undefined,
    });
    pagination.total = numberValue(data?.total);
    tableData.value = Array.isArray(data?.records) ? data.records.map(mapRow) : [];
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "加载举报列表失败"));
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

async function openDetail(row: ReportRow) {
  const requestToken = ++detailRequestToken;
  detailDialog.data = { ...row };
  detailDialog.product = null;
  detailDialog.productError = "";
  detailDialog.activeImageIndex = 0;
  detailDialog.descriptionExpanded = false;
  detailDialog.visible = true;

  if (!row.productId) {
    detailDialog.loading = false;
    detailDialog.productError = "举报记录缺少商品ID，无法获取商品信息";
    return;
  }

  detailDialog.loading = true;
  try {
    const data = await apiAdminProductDetail(row.productId);
    if (requestToken !== detailRequestToken) return;
    detailDialog.product = mapProduct(data);
  } catch (error) {
    if (requestToken !== detailRequestToken) return;
    detailDialog.product = null;
    detailDialog.productError = getApiErrorMessage(error, "商品信息获取失败");
  } finally {
    if (requestToken === detailRequestToken) {
      detailDialog.loading = false;
    }
  }
}

function openProductPage() {
  if (!detailDialog.product?.id || typeof window === "undefined") return;
  window.open(`/user/hot/product/${detailDialog.product.id}`, "_blank", "noopener");
}

function openHandle(row: ReportRow, status: ReportHandleStatus) {
  handleDialog.data = { ...row };
  handleDialog.status = status;
  handleForm.handleRemark = "";
  handleDialog.visible = true;
  handleFormRef.value?.clearValidate();
}

async function handleSave() {
  if (!handleFormRef.value || !handleDialog.data || !handleDialog.status) return;
  const valid = await handleFormRef.value.validate().catch(() => false);
  if (!valid) return;
  const targetStatus = handleDialog.status as ReportHandleStatus;

  handling.value = true;
  try {
    const confirmMessage =
      targetStatus === "VALID"
        ? `确定将举报 ${handleDialog.data.id} 标记为“${statusText(targetStatus)}”吗？举报成立后，商品将自动下架并标记为审核未通过。`
        : `确定将举报 ${handleDialog.data.id} 标记为“${statusText(targetStatus)}”吗？`;

    await runConfirmAction({
      title: "提示",
      message: confirmMessage,
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      successMessage: "举报处理成功",
      errorMessage: "举报处理失败",
      action: () =>
        apiAdminReportHandle({
          id: handleDialog.data!.id,
          status: targetStatus,
          handleRemark: handleForm.handleRemark.trim() || undefined,
        }),
      onSuccess: async () => {
        handleDialog.visible = false;
        handleForm.handleRemark = "";
        await fetchList();
        if (detailDialog.visible && detailDialog.data?.id === handleDialog.data?.id) {
          const refreshed = tableData.value.find((item) => item.id === handleDialog.data?.id);
          if (refreshed) {
            await openDetail(refreshed);
          }
        }
      },
    });
  } finally {
    handling.value = false;
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
  width: 320px;
}

.status-select {
  width: 140px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.detail-scroll {
  max-height: 82vh;
  overflow-y: auto;
  padding-right: 4px;
}

.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-section {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
  padding: 14px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #222;
  margin-bottom: 12px;
}

.section-head .section-title {
  margin-bottom: 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 14px;
}

.detail-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #ececec;
  border-radius: 8px;
  background: #fafafa;
}

.detail-item-full {
  grid-column: 1 / -1;
}

.detail-label {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 12px;
}

.detail-item strong {
  color: #222;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.plain-box {
  padding: 10px 12px;
  border: 1px dashed #ccc;
  border-radius: 8px;
  background: #fff;
  color: #444;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.plain-box.empty {
  color: #999;
}

.compact-box {
  max-height: 88px;
  overflow-y: auto;
}

.product-panel {
  min-height: 180px;
}

.product-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.gallery-panel {
  min-width: 0;
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gallery-main {
  width: 100%;
  height: 196px;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  background: #fafafa;
}

.gallery-thumbs {
  display: flex;
  gap: 8px;
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
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.18);
}

.thumb-img {
  width: 56px;
  height: 56px;
  display: block;
  object-fit: cover;
  background: #f5f5f5;
}

.gallery-empty,
.product-empty {
  min-height: 196px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  background: #fafafa;
}

.product-summary {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-summary-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.product-heading {
  min-width: 0;
}

.product-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.5;
  color: #222;
  word-break: break-word;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.product-subtitle {
  margin-top: 4px;
  color: #8a8f99;
  font-size: 12px;
}

.product-price {
  flex-shrink: 0;
  font-size: 22px;
  font-weight: 700;
  color: #d14545;
  line-height: 1.2;
}

.product-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.summary-label {
  color: #909399;
  font-size: 12px;
}

.summary-value {
  color: #303133;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.summary-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.summary-block-title {
  color: #666;
  font-size: 12px;
}

.toggle-link {
  padding: 0;
  min-height: auto;
}

.summary-desc {
  padding: 10px 12px;
  border: 1px dashed #d5d7de;
  border-radius: 8px;
  background: #fff;
  color: #444;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

.summary-desc.expanded {
  display: block;
}

.summary-desc.empty {
  color: #999;
}

.handle-summary {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px dashed #d1d5db;
  background: #fafafa;
  color: #444;
  line-height: 1.8;
}

@media (max-width: 900px) {
  .toolbar-row {
    flex-wrap: wrap;
  }

  .keyword-input,
  .status-select {
    width: 100%;
  }

  .detail-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .product-layout {
    grid-template-columns: 1fr;
  }

  .gallery-main {
    height: 200px;
  }
}
</style>
