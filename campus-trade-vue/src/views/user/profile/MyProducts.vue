<template>
  <div class="page-wrap">
    <section class="page-card">
      <UserCenterPageHeader
        eyebrow="我发布的"
        title="管理已发布的闲置商品"
        description="保留现有管理流程，把列表和商品卡压得更轻、更紧凑。"
        :stats="headerStats"
      >
        <template #actions>
          <el-button
            type="primary"
            class="toolbar-btn primary-btn"
            :loading="publishCheckLoading"
            @click="openCreate"
          >
            发布商品
          </el-button>
        </template>
      </UserCenterPageHeader>

      <UserCenterFilterBar class="section-gap">
        <el-input
          v-model="query.keyword"
          class="search-input"
          clearable
          placeholder="搜索商品标题或关键词"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="query.status"
          class="status-select"
          clearable
          placeholder="按状态筛选"
          @change="handleFilterChange"
        >
          <el-option label="全部状态" value="" />
          <el-option label="待审" value="PENDING" />
          <el-option label="在售" value="ON" />
          <el-option label="已售出" value="SOLD" />
          <el-option label="已下架" value="OFF" />
          <el-option label="未通过" value="REJECTED" />
        </el-select>
        <el-button type="primary" class="toolbar-btn" @click="handleSearch">查询</el-button>
        <el-button class="toolbar-btn light-btn" @click="reset">重置</el-button>
      </UserCenterFilterBar>

      <section class="content-board">
          <div class="section-head">
            <div>
              <h3 class="section-title">商品列表</h3>
              <p class="section-desc">保留核心信息与常用操作，方便快速查看和处理商品。</p>
            </div>
            <div class="section-chip">当前展示 {{ cards.length }} 件</div>
          </div>

        <ListDataState
          :loading="loading"
          :empty="cards.length === 0"
          :error-message="errorMessage"
          :empty-text="emptyText"
          @retry="load"
        >
          <div class="card-grid">
            <UserCenterProductCard
              v-for="item in cards"
              :key="item.id"
              variant="manage"
              :title="item.title"
              :price="item.price"
              :cover-url="toImg(item.coverUrl)"
              :status-text="item.displayStatus.text"
              :status-tone="item.displayStatus.tagType"
              :meta-items="buildProductMeta(item)"
              @click="goDetail(item.id)"
            >
              <div
                v-if="item.displayStatus.key === 'REJECTED'"
                class="reject-reason"
                :title="rejectReasonText(item)"
              >
                驳回原因：{{ rejectReasonText(item) }}
              </div>

              <template #actions>
                <div class="card-actions-grid">
                  <el-button
                    v-if="hasProductAction(item, 'view')"
                    class="action-btn action-btn--primary"
                    @click="goDetail(item.id)"
                  >
                    查看
                  </el-button>
                  <span v-else class="action-placeholder" aria-hidden="true"></span>
                  <el-button
                    v-if="hasProductAction(item, 'edit')"
                    class="action-btn action-btn--primary"
                    @click="openEdit(item)"
                  >
                    编辑
                  </el-button>
                  <span v-else class="action-placeholder" aria-hidden="true"></span>
                  <el-button
                    v-if="hasProductAction(item, 'toggle')"
                    class="action-btn action-btn--secondary"
                    @click="toggle(item)"
                  >
                    {{ toggleLabel(item) }}
                  </el-button>
                  <span v-else class="action-placeholder" aria-hidden="true"></span>
                  <el-button
                    v-if="hasProductAction(item, 'delete')"
                    class="action-btn action-btn--danger"
                    @click="removeOne(item.id)"
                  >
                    删除
                  </el-button>
                  <span v-else class="action-placeholder" aria-hidden="true"></span>
                </div>
              </template>
            </UserCenterProductCard>
          </div>
        </ListDataState>

        <div v-if="!loading && !errorMessage && total > query.size" class="pager">
          <div class="pager-copy">共 {{ total }} 件商品</div>
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

    <el-dialog v-model="dlg.visible" :title="dlg.mode === 'create' ? '发布商品' : '编辑商品'" width="680px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="商品标题">
          <el-input v-model="dlg.form.title" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="商品价格">
          <el-input v-model="dlg.form.price" placeholder="请输入价格，例如 99.00" />
        </el-form-item>

        <el-form-item label="商品分类">
          <el-select v-model="dlg.form.categoryId" placeholder="请选择分类">
            <el-option v-for="c in categoryList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="商品描述">
          <el-input v-model="dlg.form.description" type="textarea" :rows="3" />
        </el-form-item>

        <el-form-item label="交易方式">
          <el-select
            v-model="dlg.form.tradeMethod"
            placeholder="请选择交易方式"
            @change="handleTradeMethodChange"
          >
            <el-option v-for="item in TRADE_METHOD_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="dlg.form.tradeMethod === 'MEETUP'" label="交易地点">
          <el-select v-model="dlg.form.tradeLocation" placeholder="请选择大致交易地点" @change="syncAddressTextFromTrade">
            <el-option v-for="item in MEETUP_LOCATION_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item v-else label="交易说明">
          <el-input :model-value="tradeLocationHint" disabled />
        </el-form-item>

        <el-form-item label="商品图片">
          <div class="upload-panel">
            <el-upload
              v-model:file-list="uploadFileList"
              class="uploader"
              action="/api/upload"
              list-type="picture-card"
              accept="image/*"
              multiple
              :limit="6"
              :before-upload="beforeUpload"
              :on-success="handleUploadSuccess"
              :on-remove="handleUploadRemove"
              :on-preview="handleUploadPreview"
              :on-exceed="handleUploadExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">最多上传6张商品图片。空图片或无效图片会自动使用商品占位图。</div>
          </div>
        </el-form-item>

        <el-form-item label="商品状态">
          <el-select v-model="dlg.form.status">
            <el-option label="上架中" value="ON" />
            <el-option label="已下架" value="OFF" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadPreview.visible" title="预览图片" width="min(720px, 92vw)" append-to-body>
      <img class="preview-image" :src="toImg(uploadPreview.url)" alt="商品图片预览" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage, type UploadProps, type UploadUserFile } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { apiGetCategoryList } from "@/api/category";
import { apiGetMyOrderPage } from "@/api/user";
import {
  apiCreateProduct,
  apiDeleteProduct,
  apiGetMyProductPage,
  apiToggleProduct,
  apiUpdateProduct,
  type ProductImageItem,
  type ProductSavePayload,
} from "@/api/product";
import { apiVerifyMy, type VerifyStatus } from "@/api/verify";
import ListDataState from "@/components/common/ListDataState.vue";
import UserCenterFilterBar from "@/components/user-center/UserCenterFilterBar.vue";
import UserCenterPageHeader from "@/components/user-center/UserCenterPageHeader.vue";
import UserCenterProductCard from "@/components/user-center/UserCenterProductCard.vue";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { usePagedQuery } from "@/composables/usePagedQuery";
import { getStoredUser, mergeStoredUser } from "@/utils/auth";
import { getApiErrorMessage } from "@/utils/apiError";
import { productImgUrl as resolveProductImgUrl, sanitizeProductImageUrl } from "@/utils/img";
import {
  getProductActionKeys,
  getProductDisplayStatus,
  normalizeProductAuditStatus,
  type ProductActionKey,
  type ProductAuditStatus,
  type ProductDisplayStatus,
  type ProductDisplayStatusKey,
} from "@/utils/productStatus";
import {
  MEETUP_LOCATION_OPTIONS,
  TRADE_METHOD_OPTIONS,
  buildAddressText,
  getPublicTradeBadge,
  inferMeetupLocation,
  inferTradeMethod,
  validateSafeProductAddressText,
  type TradeMethod,
} from "@/utils/tradeLocation";

type ProductStatus = "ON" | "OFF";
type StatusFilter = "" | ProductDisplayStatusKey;

type ProductRow = {
  id: number;
  title: string;
  price: number | string;
  coverUrl?: string;
  images: ProductImageItem[];
  status: ProductStatus;
  auditStatus: ProductAuditStatus;
  createdAt?: string;
  categoryId?: number;
  description?: string;
  addressText?: string;
  viewCount?: number;
  favoriteCount?: number;
  auditReason?: string;
};

type ProductCard = ProductRow & {
  displayStatus: ProductDisplayStatus;
};

type ProductForm = {
  title: string;
  price: string;
  coverUrl: string;
  images: ProductImageItem[];
  status: ProductStatus;
  categoryId?: number;
  description: string;
  addressText: string;
  tradeMethod: TradeMethod;
  tradeLocation: string;
};

const route = useRoute();
const router = useRouter();
const { runConfirmAction } = useConfirmAction();

const loading = ref(false);
const errorMessage = ref("");
const total = ref(0);
const totalAll = ref(0);
const cards = ref<ProductCard[]>([]);
const categoryList = ref<any[]>([]);
const soldProductIds = ref<Set<number>>(new Set());
const uploadFileList = ref<UploadUserFile[]>([]);
const publishCheckLoading = ref(false);

const uploadPreview = reactive({
  visible: false,
  url: "",
});

const FETCH_BATCH_SIZE = 100;

const { query, changePage, search } = usePagedQuery({
  keyword: "",
  status: "" as StatusFilter,
  page: 1,
  size: 6,
});

const currentStatusLabel = computed(() => {
  if (query.status === "PENDING") return "待审";
  if (query.status === "ON") return "在售";
  if (query.status === "OFF") return "已下架";
  if (query.status === "SOLD") return "已售出";
  if (query.status === "REJECTED") return "未通过";
  return "全部";
});

const totalLabel = computed(() => {
  return String(query.keyword || "").trim() ? "匹配商品" : "商品总数";
});

const headerStats = computed(() => [
  { label: totalLabel.value, value: totalAll.value },
  { label: "当前筛选", value: currentStatusLabel.value, tone: "success" as const },
]);

const emptyText = computed(() => {
  if (String(query.keyword || "").trim() || query.status) {
    return "没有匹配的商品";
  }
  return "暂无商品";
});

function createEmptyForm(): ProductForm {
  return {
    title: "",
    price: "",
    coverUrl: "",
    images: [],
    status: "OFF",
    categoryId: undefined,
    description: "",
    addressText: "",
    tradeMethod: "MEETUP",
    tradeLocation: "",
  };
}

function normalizeVerifyStatus(value: unknown): VerifyStatus {
  const normalized = typeof value === "string" ? value.trim().toUpperCase() : "";
  if (normalized === "PENDING") return "PENDING";
  if (normalized === "VERIFIED") return "VERIFIED";
  if (normalized === "REJECTED") return "REJECTED";
  return "UNVERIFIED";
}

function getVerifyBlockedMessage(status: VerifyStatus) {
  if (status === "PENDING") {
    return "当前学号认证正在审核中，审核通过后再发布商品";
  }
  if (status === "REJECTED") {
    return "学号认证未通过，请先完成学号认证后再发布商品";
  }
  return "请先完成学号认证后再发布商品";
}

async function ensureVerifiedBeforeCreate() {
  const cachedStatus = normalizeVerifyStatus(getStoredUser()?.verifyStatus);
  if (cachedStatus === "VERIFIED") {
    return true;
  }

  try {
    const verifyResp = await apiVerifyMy();
    const latestStatus = normalizeVerifyStatus(verifyResp?.verifyStatus);
    mergeStoredUser({ verifyStatus: latestStatus });
    if (latestStatus === "VERIFIED") {
      return true;
    }

    ElMessage.warning(getVerifyBlockedMessage(latestStatus));
    await router.push("/user/verify");
    return false;
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "获取认证状态失败，请稍后重试"));
    return false;
  }
}

const dlg = reactive({
  visible: false,
  mode: "create" as "create" | "edit",
  editId: 0,
  form: createEmptyForm(),
});

const tradeLocationHint = computed(() => {
  if (dlg.form.tradeMethod === "DELIVERY") return "买家下单后选择收货地址";
  if (dlg.form.tradeMethod === "NEGOTIATE") return "聊天中确认";
  return "";
});

function syncAddressTextFromTrade() {
  dlg.form.addressText = buildAddressText(dlg.form.tradeMethod, dlg.form.tradeLocation);
}

function handleTradeMethodChange() {
  if (dlg.form.tradeMethod !== "MEETUP") {
    dlg.form.tradeLocation = "";
  }
  syncAddressTextFromTrade();
}

function applyTradeFormFromAddressText(addressText?: string) {
  dlg.form.tradeMethod = inferTradeMethod(addressText);
  dlg.form.tradeLocation = dlg.form.tradeMethod === "MEETUP" ? inferMeetupLocation(addressText) : "";
  syncAddressTextFromTrade();
}

function hasCreateAction(action: unknown) {
  if (Array.isArray(action)) return action.includes("create");
  return action === "create";
}

function clearCreateActionQuery() {
  const nextQuery = { ...route.query };
  delete nextQuery.action;
  void router.replace({ path: route.path, query: nextQuery });
}

function parseCount(value: unknown) {
  const num = Number(value);
  if (!Number.isFinite(num) || num < 0) return 0;
  return Math.floor(num);
}

function toImg(url: unknown) {
  return resolveProductImgUrl(typeof url === "string" ? url : "");
}

function formatTime(value?: string) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function resolveDisplayStatus(item: ProductRow) {
  return getProductDisplayStatus(item, { soldProductIds: soldProductIds.value });
}

function normalizeStoredUrl(url: unknown) {
  if (typeof url !== "string") return "";
  const value = url.trim();
  if (!value) return "";
  if (/^(blob:|data:|file:)/i.test(value)) return "";
  if (/^(https?:)/i.test(value)) {
    try {
      const parsed = new URL(value);
      if (parsed.pathname.startsWith("/upload/")) {
        return sanitizeProductImageUrl(`${parsed.pathname}${parsed.search}`);
      }
      return sanitizeProductImageUrl(value);
    } catch {
      return sanitizeProductImageUrl(value);
    }
  }
  if (value.startsWith("upload/")) {
    return sanitizeProductImageUrl(`/${value}`);
  }
  return sanitizeProductImageUrl(value);
}

function normalizeImages(source: unknown, fallbackCoverUrl?: unknown): ProductImageItem[] {
  const list = Array.isArray(source) ? source : [];
  const seen = new Set<string>();
  const images: ProductImageItem[] = [];

  function pushUrl(rawUrl: unknown, rawSort?: unknown) {
    const url = normalizeStoredUrl(rawUrl);
    if (!url || seen.has(url)) return;
    seen.add(url);
    const sortValue = Number(rawSort);
    images.push({
      url,
      sort: Number.isFinite(sortValue) && sortValue > 0 ? sortValue : images.length + 1,
    });
  }

  list.forEach((item, index) => {
    if (typeof item === "string") {
      pushUrl(item, index + 1);
      return;
    }
    if (item && typeof item === "object") {
      const record = item as Record<string, unknown>;
      pushUrl(record.url, record.sort ?? index + 1);
    }
  });

  if (!images.length) {
    pushUrl(fallbackCoverUrl, 1);
  }

  return images
    .sort((a, b) => Number(a.sort || 0) - Number(b.sort || 0))
    .map((item, index) => ({
      ...item,
      sort: index + 1,
    }));
}

function coverFromImages(images: ProductImageItem[], fallback?: unknown) {
  const url = images[0]?.url;
  if (url) return url;
  return normalizeStoredUrl(fallback);
}

function createUploadFile(url: string, index: number): UploadUserFile {
  return {
    uid: Date.now() + index,
    name: `image-${index + 1}`,
    status: "success",
    url: toImg(url),
    response: { data: url },
  };
}

function syncUploadFileList(images = dlg.form.images) {
  uploadFileList.value = dedupeImageItems(images).map((item, index) => createUploadFile(item.url, index));
}

function applyImages(images: ProductImageItem[], syncUploadList = true) {
  dlg.form.images = dedupeImageItems(images);
  dlg.form.coverUrl = coverFromImages(dlg.form.images, dlg.form.coverUrl);
  if (syncUploadList) {
    syncUploadFileList();
  }
}

function dedupeImageItems(source: Array<Partial<ProductImageItem> | null | undefined>) {
  const seen = new Set<string>();
  const images: ProductImageItem[] = [];

  source.forEach((item) => {
    const url = normalizeStoredUrl(item?.url);
    if (!url || seen.has(url)) return;
    seen.add(url);
    images.push({
      url,
      sort: images.length + 1,
    });
  });

  return images;
}

function extractUploadResponseUrl(response: unknown) {
  if (!response || typeof response !== "object") return "";
  const record = response as Record<string, unknown>;
  return normalizeStoredUrl(record.data ?? record.url);
}

function extractFileRawUrl(file: Partial<UploadUserFile>) {
  const responseUrl = extractUploadResponseUrl(file.response);
  if (responseUrl) return responseUrl;
  return normalizeStoredUrl(file.url);
}

function hasPendingUploads() {
  return uploadFileList.value.some((file) => file.status === "ready" || file.status === "uploading");
}

function hasFailedUploads() {
  return uploadFileList.value.some((file) => file.status === "fail");
}

function syncFormImagesFromUploadFiles(files: UploadUserFile[] = []) {
  const seen = new Set<string>();
  const images: ProductImageItem[] = [];
  files.forEach((file) => {
    const url = extractFileRawUrl(file);
    if (!url || seen.has(url)) return;
    seen.add(url);
    images.push({
      url,
      sort: images.length + 1,
    });
  });
  // Keep Element Plus internal uploading queue intact; only sync form data.
  applyImages(images, false);
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
  query.keyword = "";
  query.status = "";
  query.page = 1;
  load();
}

function goDetail(id: number) {
  router.push(`/user/hot/product/${id}`);
}

function buildProductMeta(item: ProductCard) {
  return [getPublicTradeBadge(item.addressText), `发布时间 ${formatTime(item.createdAt)}`];
}

function rejectReasonText(item: ProductCard) {
  return String(item.auditReason || "").trim() || "管理员未填写具体原因";
}

function hasProductAction(item: ProductCard, action: ProductActionKey) {
  return getProductActionKeys(item.displayStatus).includes(action);
}

function toggleLabel(item: ProductCard) {
  return item.displayStatus.key === "ON" ? "下架" : "申请上架";
}

async function openCreate() {
  if (publishCheckLoading.value) return;

  publishCheckLoading.value = true;
  try {
    const verified = await ensureVerifiedBeforeCreate();
    if (!verified) {
      return;
    }

    dlg.mode = "create";
    dlg.editId = 0;
    dlg.form = createEmptyForm();
    syncUploadFileList([]);
    applyTradeFormFromAddressText("");
    dlg.visible = true;
  } finally {
    publishCheckLoading.value = false;
  }
}

watch(
  () => route.query.action,
  (action) => {
    if (!hasCreateAction(action)) return;
    void openCreate();
    clearCreateActionQuery();
  },
  { immediate: true },
);

async function openEdit(row: ProductCard) {
  const images = normalizeImages(row.images, row.coverUrl);
  dlg.mode = "edit";
  dlg.editId = row.id;
  dlg.form = {
    title: row.title,
    price: row.price ? String(row.price) : "",
    coverUrl: coverFromImages(images, row.coverUrl),
    images,
    status: row.status,
    categoryId: row.categoryId,
    description: row.description || "",
    addressText: row.addressText || "",
    tradeMethod: "MEETUP",
    tradeLocation: "",
  };
  applyTradeFormFromAddressText(row.addressText || "");
  syncUploadFileList(images);
  dlg.visible = true;
}

async function loadSoldProductIds() {
  const next = new Set<number>();
  let page = 1;
  let total = 0;

  do {
    const result = await apiGetMyOrderPage<{ records: any[]; total?: number }>({
      type: "SELL",
      page,
      size: FETCH_BATCH_SIZE,
      status: "FINISHED",
    });

    const records = Array.isArray(result?.records) ? result.records : [];
    records.forEach((row) => {
      const productId = Number(row?.productId);
      if (Number.isFinite(productId) && productId > 0) {
        next.add(productId);
      }
    });

    total = Number(result?.total ?? records.length);
    if (!records.length) break;
    page += 1;
  } while ((page - 1) * FETCH_BATCH_SIZE < total);

  soldProductIds.value = next;
}

async function loadAllProductRecords(keyword: string) {
  const records: any[] = [];
  let page = 1;
  let total = 0;

  do {
    const result = await apiGetMyProductPage({
      page,
      size: FETCH_BATCH_SIZE,
      keyword: keyword || undefined,
    });

    const currentRecords = Array.isArray(result?.records) ? result.records : [];
    records.push(...currentRecords);

    total = Number(result?.total ?? currentRecords.length);
    if (!currentRecords.length) break;
    page += 1;
  } while ((page - 1) * FETCH_BATCH_SIZE < total);

  return records;
}

function toCardList(records: any[]): ProductCard[] {
  return (records || []).map((item) => {
    const images = normalizeImages(item?.images, item?.coverUrl);
    const base: ProductRow = {
      id: Number(item?.id || 0),
      title: String(item?.title || ""),
      price: item?.price ?? 0,
      coverUrl: coverFromImages(images, item?.coverUrl),
      images,
      status: item?.status === "ON" ? "ON" : "OFF",
      auditStatus: normalizeProductAuditStatus(item?.auditStatus),
      createdAt: typeof item?.createdAt === "string" ? item.createdAt : "",
      categoryId: Number.isFinite(Number(item?.categoryId)) ? Number(item.categoryId) : undefined,
      description: typeof item?.description === "string" ? item.description : "",
      addressText: typeof item?.addressText === "string" ? item.addressText : "",
      viewCount: parseCount(item?.viewCount ?? item?.views ?? item?.browseCount ?? item?.viewNum),
      favoriteCount: parseCount(item?.favoriteCount ?? item?.favorites ?? item?.collectCount ?? item?.likeCount),
      auditReason: typeof item?.auditReason === "string"
        ? item.auditReason
        : typeof item?.audit_reason === "string"
          ? item.audit_reason
          : "",
    };
    return {
      ...base,
      displayStatus: resolveDisplayStatus(base),
    };
  });
}

function filterCardsByStatus(list: ProductCard[]) {
  if (!query.status) return list;
  return list.filter((item) => item.displayStatus.key === query.status);
}

async function loadSoldCardsByOrder(keyword: string) {
  const [soldResp, productResp] = await Promise.all([
    apiGetMyOrderPage<{ records: any[]; total: number }>({
      type: "SELL",
      page: query.page,
      size: query.size,
      status: "FINISHED",
      keyword: keyword || undefined,
    }),
    apiGetMyProductPage({
      page: 1,
      size: 500,
      keyword: keyword || undefined,
    }),
  ]);

  const productMap = new Map<number, any>();
  (productResp?.records || []).forEach((item: any) => {
    const id = Number(item?.id);
    if (Number.isFinite(id) && id > 0) {
      productMap.set(id, item);
    }
  });

  const seen = new Set<number>();
  cards.value = (soldResp?.records || [])
    .map((row: any) => {
      const productId = Number(row?.productId || 0);
      const meta = productMap.get(productId) || {};
      const images = normalizeImages(meta?.images, meta?.coverUrl || row?.coverUrl);
      return {
        id: productId,
        title: String(meta?.title || row?.productTitle || "已售商品"),
        price: meta?.price ?? row?.amount ?? 0,
        coverUrl: coverFromImages(images, meta?.coverUrl || row?.coverUrl),
        images,
        status: meta?.status === "ON" ? "ON" : "OFF",
        auditStatus: normalizeProductAuditStatus(meta?.auditStatus),
        createdAt: String(meta?.createdAt || ""),
        categoryId: Number.isFinite(Number(meta?.categoryId)) ? Number(meta.categoryId) : undefined,
        description: typeof meta?.description === "string" ? meta.description : "",
        addressText: typeof meta?.addressText === "string" ? meta.addressText : "",
        viewCount: parseCount(meta?.viewCount ?? meta?.views ?? row?.viewCount),
        favoriteCount: parseCount(meta?.favoriteCount ?? meta?.favorites ?? row?.favoriteCount),
        auditReason: typeof meta?.auditReason === "string"
          ? meta.auditReason
          : typeof meta?.audit_reason === "string"
            ? meta.audit_reason
            : "",
        displayStatus: getProductDisplayStatus(
          {
            id: productId,
            status: meta?.status,
            auditStatus: meta?.auditStatus,
            orderStatus: "FINISHED",
          },
          { isSold: true }
        ),
      } satisfies ProductCard;
    })
    .filter((item) => {
      if (!item.id || seen.has(item.id)) return false;
      seen.add(item.id);
      return true;
    });

    total.value = Number(soldResp?.total || cards.value.length);
}

void loadSoldCardsByOrder;

function sortCards(list: ProductCard[]) {
  return [...list].sort((left, right) => {
    const leftTime = Date.parse(String(left.createdAt || ""));
    const rightTime = Date.parse(String(right.createdAt || ""));
    if (Number.isNaN(leftTime) && Number.isNaN(rightTime)) {
      return right.id - left.id;
    }
    if (Number.isNaN(leftTime)) return 1;
    if (Number.isNaN(rightTime)) return -1;
    return rightTime - leftTime;
  });
}

function clearCards() {
  totalAll.value = 0;
  total.value = 0;
  cards.value = [];
}

function updateCards(records: any[]) {
  const allCards = sortCards(toCardList(records));
  const filteredCards = filterCardsByStatus(allCards);
  const maxPage = Math.max(1, Math.ceil(filteredCards.length / query.size));
  if (query.page > maxPage) {
    query.page = maxPage;
  }
  const start = (query.page - 1) * query.size;

  totalAll.value = allCards.length;
  total.value = filteredCards.length;
  cards.value = filteredCards.slice(start, start + query.size);
}

async function load() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const keyword = String(query.keyword || "").trim();
    await loadSoldProductIds();
    updateCards(await loadAllProductRecords(keyword));
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "加载商品失败");
    clearCards();
  } finally {
    loading.value = false;
  }
}

async function toggle(item: ProductCard) {
  if (!hasProductAction(item, "toggle")) {
    ElMessage.warning("当前状态不可切换上下架");
    return;
  }
  await apiToggleProduct(item.id);
  ElMessage.success(item.displayStatus.key === "ON" ? "已下架" : "已提交上架审核");
  await load();
}

async function removeOne(id: number) {
  await runConfirmAction({
    title: "删除商品",
    message: "确定删除该商品吗？此操作不可撤销。",
    successMessage: "删除成功",
    action: () => apiDeleteProduct(id),
    onSuccess: load,
  });
}

function buildPayload(): ProductSavePayload {
  syncAddressTextFromTrade();
  const dedupedImages = dedupeImageItems(dlg.form.images);
  const images = dedupedImages.map((item) => item.url);
  return {
    title: dlg.form.title.trim(),
    price: dlg.form.price.trim(),
    coverUrl: coverFromImages(dedupedImages, dlg.form.coverUrl),
    images,
    status: dlg.form.status,
    categoryId: dlg.form.categoryId,
    description: dlg.form.description.trim(),
    addressText: dlg.form.addressText.trim(),
  };
}

function validateForm() {
  syncAddressTextFromTrade();
  if (!dlg.form.title.trim()) {
    ElMessage.warning("请输入商品标题");
    return false;
  }
  if (!dlg.form.price.trim()) {
    ElMessage.warning("请输入商品价格");
    return false;
  }
  if (!/^(0|[1-9]\d*)(\.\d{1,2})?$/.test(dlg.form.price.trim())) {
    ElMessage.warning("请输入合法价格");
    return false;
  }
  if (!dlg.form.addressText.trim()) {
    ElMessage.warning(dlg.form.tradeMethod === "MEETUP" ? "请选择交易地点" : "请选择交易方式");
    return false;
  }
  if (dlg.form.tradeMethod === "MEETUP" && !dlg.form.tradeLocation.trim()) {
    ElMessage.warning("校内当面交易时请选择交易地点");
    return false;
  }
  const addressError = validateSafeProductAddressText(dlg.form.addressText);
  if (addressError) {
    ElMessage.warning(addressError);
    return false;
  }
  if (hasPendingUploads()) {
    ElMessage.warning("图片仍在上传中");
    return false;
  }
  if (hasFailedUploads()) {
    ElMessage.warning("部分图片上传失败，请先移除后重试");
    return false;
  }
  if (!dlg.form.images.length && !dlg.form.coverUrl) {
    ElMessage.warning("请至少上传一张图片");
    return false;
  }
  return true;
}

async function submitEdit() {
  if (!validateForm()) return;

  try {
    const payload = buildPayload();
    if (dlg.mode === "create") {
      await apiCreateProduct(payload);
      ElMessage.success("创建成功");
    } else {
      await apiUpdateProduct(dlg.editId, payload);
      ElMessage.success("保存成功，商品已重新提交审核");
    }
    dlg.visible = false;
    await load();
  } catch (error) {
    const err = error as any;
    if (err?.code === 4001) {
      ElMessage.warning("请先完成学生认证");
      router.push("/user/verify");
      return;
    }
    ElMessage.error(getApiErrorMessage(error, dlg.mode === "create" ? "创建失败" : "更新失败"));
  }
}

const beforeUpload: UploadProps["beforeUpload"] = (rawFile) => {
  const isImage = rawFile.type.startsWith("image/");
  if (!isImage) {
    ElMessage.warning("请上传图片文件");
    return false;
  }

  const isLt5M = rawFile.size / 1024 / 1024 < 5;
  if (!isLt5M) {
    ElMessage.warning("图片大小不能超过 5MB");
    return false;
  }
  return true;
};

const handleUploadSuccess: UploadProps["onSuccess"] = (response, uploadFile, uploadFiles) => {
  const rawUrl = extractUploadResponseUrl(response);
  if (rawUrl) {
    uploadFile.response = { data: rawUrl };
    uploadFile.url = toImg(rawUrl);
  }
  syncFormImagesFromUploadFiles(uploadFiles as UploadUserFile[]);
};

const handleUploadRemove: UploadProps["onRemove"] = (_uploadFile, uploadFiles) => {
  syncFormImagesFromUploadFiles(uploadFiles as UploadUserFile[]);
};

const handleUploadPreview: UploadProps["onPreview"] = (uploadFile) => {
  uploadPreview.url = extractFileRawUrl(uploadFile as UploadUserFile);
  uploadPreview.visible = !!uploadPreview.url;
};

const handleUploadExceed: UploadProps["onExceed"] = () => {
  ElMessage.warning("最多上传6张图片");
};

onMounted(async () => {
  await load();
  const categoryRes = await apiGetCategoryList();
  categoryList.value = categoryRes || [];
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
  border: 1px solid #eef2f7;
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
  width: 136px;
}

.toolbar-btn {
  height: 36px;
  border-radius: 10px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 700;
}

.primary-btn {
  padding: 0 16px;
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
  padding: 0 12px;
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

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.card-grid :deep(.product-card.is-manage) {
  border-radius: 16px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04);
}

.card-grid :deep(.product-card.is-manage:hover) {
  transform: translateY(-2px);
  box-shadow: 0 10px 18px rgba(15, 23, 42, 0.05);
}

.card-grid :deep(.product-card.is-manage .cover-wrap) {
  aspect-ratio: 16 / 10;
}

.card-grid :deep(.product-card.is-manage .status-tag) {
  top: 10px;
  left: 10px;
}

.card-grid :deep(.product-card.is-manage .body) {
  padding: 12px 12px 8px;
  min-width: 0;
}

.card-grid :deep(.product-card.is-manage .title) {
  font-size: 14px;
  line-height: 1.45;
  display: block;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.card-grid :deep(.product-card.is-manage .price) {
  margin-top: 8px;
  font-size: 20px;
}

.card-grid :deep(.product-card.is-manage .meta-list) {
  margin-top: 6px;
  gap: 0;
  min-width: 0;
}

.card-grid :deep(.product-card.is-manage .meta-item) {
  min-height: 24px;
  padding: 0 8px;
  font-size: 11px;
}

.card-grid :deep(.product-card.is-manage .actions) {
  padding: 9px 12px 12px;
}

.reject-reason {
  padding: 8px 10px;
  border: 1px solid #fecdd3;
  border-radius: 10px;
  background: #fff1f2;
  color: #be123c;
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-card {
  display: flex;
  flex-direction: column;
  border: 1px solid #e8eef6;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.goods-card:hover {
  transform: translateY(-4px);
  border-color: #dbeafe;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.cover-wrap {
  position: relative;
  height: 210px;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  overflow: hidden;
  cursor: pointer;
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

.status-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  border-radius: 999px;
  font-weight: 700;
}

.card-content {
  padding: 16px 16px 0;
  min-width: 0;
}

.goods-title {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.45;
  cursor: pointer;
}

.price {
  margin-top: 10px;
  color: #d26a3d;
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}

.meta-line {
  margin-top: 12px;
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

.action-area {
  margin-top: auto;
  padding: 16px;
  border-top: 1px solid #eef3f8;
}

.card-actions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.action-btn {
  width: 100%;
  margin: 0;
  height: 34px;
  border-radius: 9px;
  border-color: #dbe6f2;
  color: #475569;
  background: #ffffff;
  font-size: 13px;
  font-weight: 600;
  padding: 0 10px;
}

.card-actions-grid .action-btn,
.card-actions-grid .action-placeholder {
  width: 100%;
  min-width: 0;
}

.action-placeholder {
  height: 34px;
  border-radius: 9px;
  visibility: hidden;
  pointer-events: none;
}

.action-btn--primary {
  border-color: #dbe6f2;
  color: #334155;
  background: #ffffff;
}

.action-btn--secondary {
  border-color: #e5edf7;
  color: #64748b;
  background: #f8fbff;
}

.danger-btn,
.action-btn--danger {
  border-color: rgba(248, 113, 113, 0.28);
  color: #dc2626;
  background: rgba(255, 250, 250, 0.92);
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

.upload-panel {
  width: 100%;
}

.upload-tip {
  margin-top: 10px;
  color: #7b8aa3;
  font-size: 12px;
  line-height: 1.6;
}

.preview-image {
  width: 100%;
  max-height: 70vh;
}

:deep(.uploader .el-upload--picture-card),
:deep(.uploader .el-upload-list__item) {
  border-radius: 18px;
}

:deep(.uploader .el-upload--picture-card) {
  width: 108px;
  height: 108px;
  border-color: rgba(147, 197, 253, 0.92);
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
}

:deep(.uploader .el-upload-list__item) {
  width: 108px;
  height: 108px;
}

:deep(.uploader .el-upload-list__item-thumbnail) {
  object-fit: cover;
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

@media (max-width: 1180px) {
  .card-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .page-card,
  .toolbar-panel,
  .content-board,
  .goods-card {
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

  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>

















