<template>
  <section class="hot-page user-page-shell">
    <div class="hero-filter-card">
      <div class="hero-filter-top">
        <div class="hero-copy">
          <span class="hero-badge">校园精选</span>
          <h2>热卖好物</h2>
          <p>发现校内高性价比闲置商品，教材、数码、宿舍好物都能在这里更快找到。</p>
        </div>

        <div class="hero-stats">
          <div class="hero-stat">
            <span>在售商品</span>
            <strong>{{ total }}</strong>
          </div>
          <div class="hero-stat">
            <span>热门分类</span>
            <strong>{{ categories.length }}</strong>
          </div>
          <div class="hero-stat sort-stat">
            <span>当前排序</span>
            <strong>{{ sort }}</strong>
          </div>
        </div>
      </div>

      <div class="hero-filter-toolbar">
        <div class="filter-grid">
        <el-input
          v-model="keyword"
          placeholder="搜索商品名称、教材、数码用品"
          class="search-input"
          clearable
          @keyup.enter="onSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select v-model="categoryId" class="filter-select" placeholder="全部分类" clearable @change="onFilterChange">
          <el-option label="全部分类" :value="null" />
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>

        <el-select v-model="sort" class="filter-select sort-select" placeholder="最新发布" @change="onFilterChange">
          <el-option v-for="item in sortOptions" :key="item" :label="item" :value="item" />
        </el-select>

        <div class="filter-actions">
          <el-button type="primary" class="action-btn primary-btn" @click="onSearch">
            <el-icon><Search /></el-icon>
            <span>搜索</span>
          </el-button>
          <el-button class="action-btn reset-btn" @click="resetFilters">
            <el-icon><RefreshRight /></el-icon>
            <span>重置</span>
          </el-button>
        </div>
      </div>
    </div>
    </div>

    <div class="goods-section" v-loading="loading">
      <div class="section-head">
        <div>
          <h3>商品列表</h3>
          <p>已为你找到 {{ total }} 件校内可交易好物</p>
        </div>
      </div>

      <ListDataState
        :loading="loading"
        :empty="list.length === 0"
        :error-message="errorMessage"
        empty-text="当前筛选条件下暂无商品"
        @retry="loadProducts"
      >
        <div class="grid">
          <el-card v-for="item in list" :key="item.id" class="goods-card" shadow="never" @click="goDetail(item.id)">
            <div class="card-cover">
              <img v-if="item.coverUrl" class="img" :src="toImg(item.coverUrl)" />
              <div v-else class="img-ph">暂无图片</div>

              <div class="cover-tags">
                <span class="cover-tag primary">{{ getPrimaryTag(item) }}</span>
                <span class="cover-tag secondary">{{ getSecondaryTag(item) }}</span>
              </div>
            </div>

              <div class="card-body">
                <div class="card-labels">
                  <span class="mini-label">{{ item.schoolName || "本校商品" }}</span>
                  <span class="mini-label trade-label">{{ getPublicTradeBadge(item.addressText) }}</span>
                </div>

              <h4 class="title text-ellipsis-1" :title="item.title || '未命名商品'">{{ item.title || "未命名商品" }}</h4>

              <div class="stats">
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  <span>{{ formatCount(item.viewCount) }}</span>
                </span>
                <span class="stat-item">
                  <el-icon><Star /></el-icon>
                  <span>{{ formatCount(item.likeCount) }}</span>
                </span>
                <span class="stat-item">
                  <el-icon><Collection /></el-icon>
                  <span>{{ formatCount(item.favoriteCount || 0) }}</span>
                </span>
              </div>

              <div class="card-footer">
                <div class="price-wrap">
                  <span class="price-label">参考价格</span>
                  <div class="price">{{ formatPrice(item.price) }}</div>
                </div>

                <div class="detail-link">
                  <span>查看详情</span>
                  <el-icon><Right /></el-icon>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </ListDataState>
    </div>

    <div v-if="!loading && !errorMessage && total > 0" class="pager">
      <el-pagination
        :current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next"
        @current-change="onPageChange"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Collection, RefreshRight, Right, Search, Star, View } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { apiGetCategoryList } from "@/api/category";
import { apiGetProductPage } from "@/api/product";
import { usePagedQuery } from "@/composables/usePagedQuery";
import ListDataState from "@/components/common/ListDataState.vue";
import { getApiErrorMessage } from "@/utils/apiError";
import { productImgUrl as resolveProductImgUrl } from "@/utils/img";
import { formatPrice, normalizePriceNumber } from "@/utils/price";
import { getPublicTradeBadge } from "@/utils/tradeLocation";

const router = useRouter();

const keyword = ref("");
const categories = ref<{ id: number; name: string }[]>([]);
const categoryId = ref<number | null>(null);

const sortOptions = ["最新发布", "最热关注", "价格从低到高", "价格从高到低"];
const sort = ref("最新发布");

const { query, changePage, search } = usePagedQuery({
  page: 1,
  size: 12,
});
const total = ref(0);
const loading = ref(false);
const list = ref<any[]>([]);
const errorMessage = ref("");

const currentCategoryName = computed(() => {
  return categories.value.find((item) => item.id === categoryId.value)?.name || "全部分类";
});

function toImg(url: string) {
  return resolveProductImgUrl(url);
}

function formatCount(value: unknown) {
  const count = Number(value ?? 0);
  if (!Number.isFinite(count) || count <= 0) return "0";
  if (count < 1000) return String(count);
  if (count < 10000) return `${(count / 1000).toFixed(1)}k`;
  return `${Math.round(count / 1000)}k`;
}

function getPrimaryTag(item: any) {
  const viewCount = Number(item?.viewCount || 0);
  const favoriteCount = Number(item?.favoriteCount || 0);
  if (favoriteCount >= 15 || viewCount >= 120) return "热卖精选";
  if (favoriteCount >= 8 || viewCount >= 60) return "人气好物";
  return currentCategoryName.value === "全部分类" ? "校内推荐" : currentCategoryName.value;
}

function getSecondaryTag(item: any) {
  const price = normalizePriceNumber(item?.price);
  const likeCount = Number(item?.likeCount || 0);
  if (price > 0 && price <= 50) return "低价优选";
  if (likeCount >= 6) return "同学推荐";
  return "高性价比";
}

function getSortParams() {
  switch (sort.value) {
    case "最新发布":
      return { sortBy: "createdAt" as const, sortOrder: "desc" as const };
    case "最热关注":
      return { sortBy: "viewCount" as const, sortOrder: "desc" as const };
    case "价格从低到高":
      return { sortBy: "price" as const, sortOrder: "asc" as const };
    case "价格从高到低":
      return { sortBy: "price" as const, sortOrder: "desc" as const };
    default:
      return {};
  }
}

async function loadCategories() {
  try {
    const data = await apiGetCategoryList();
    categories.value = data ?? [];
  } catch (error) {
    ElMessage.warning(getApiErrorMessage(error, "加载分类失败"));
    categories.value = [];
  }
}

async function loadProducts() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const sortParams = getSortParams();
    const data = await apiGetProductPage({
      page: query.page,
      size: query.size,
      keyword: keyword.value.trim() || "",
      categoryId: categoryId.value ?? undefined,
      status: "ON",
      auditStatus: "APPROVED",
      sortBy: sortParams.sortBy,
      sortOrder: sortParams.sortOrder,
    });

    const records = Array.isArray(data?.records) ? data.records : [];
    list.value = records.map((item: any) => ({
      ...item,
      price: normalizePriceNumber(item?.price),
    }));
    total.value = data?.total ?? 0;
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "加载商品失败");
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function goDetail(id: number) {
  router.push(`/user/hot/product/${id}`);
}

function onSearch() {
  search(loadProducts);
}

function resetFilters() {
  keyword.value = "";
  categoryId.value = null;
  sort.value = "最新发布";
  search(loadProducts);
}

function onPageChange(page: number) {
  changePage(page, loadProducts);
}

function onFilterChange() {
  search(loadProducts);
}

onMounted(async () => {
  await loadCategories();
  await loadProducts();
});
</script>

<style scoped>
.hot-page {
  display: grid;
  gap: 18px;
}

.hero-filter-card,
.goods-section,
.pager {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.88);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.06);
}

.hero-filter-card {
  padding: 18px 20px 20px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.12), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(243, 248, 255, 0.98) 58%, rgba(240, 252, 245, 0.98) 100%);
}

.hero-filter-top {
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(320px, 0.98fr);
  align-items: center;
  gap: 16px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  min-width: 0;
  max-width: 580px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  height: 30px;
  padding: 0 11px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.hero-copy h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.14;
  color: #0f172a;
}

.hero-copy p {
  max-width: 560px;
  margin: 0;
  font-size: 13px;
  line-height: 1.68;
  color: #64748b;
}

.hero-stats {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 0;
  padding-bottom: 2px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.hero-stats::-webkit-scrollbar {
  display: none;
}

.hero-stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: max-content;
  height: 42px;
  padding: 0 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(219, 234, 254, 0.9);
}

.hero-stat strong {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  line-height: 1;
  font-weight: 700;
  color: #1d4ed8;
}

.hero-stat span {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  color: #64748b;
  line-height: 1;
}

.sort-stat strong {
  color: #15803d;
}

.hero-filter-toolbar {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(219, 234, 254, 0.92);
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(250px, 1.3fr) repeat(2, minmax(160px, 0.7fr)) auto;
  gap: 12px;
  align-items: center;
}

.search-input,
.filter-select {
  width: 100%;
}

.hero-filter-card :deep(.el-input__wrapper),
.hero-filter-card :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 14px;
  background: #f8fbff;
  box-shadow: 0 0 0 1px rgba(209, 219, 234, 0.9);
}

.hero-filter-card :deep(.el-input__wrapper.is-focus),
.hero-filter-card :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.16);
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  height: 44px;
  padding: 0 16px;
  border-radius: 14px;
  font-weight: 700;
}

.primary-btn,
.reset-btn {
  min-width: 88px;
}

.primary-btn {
  border: none;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 55%, #22c55e 100%);
  box-shadow: 0 14px 26px rgba(59, 130, 246, 0.2);
}

.reset-btn {
  margin-left: 0;
  border-color: #dbe6f2;
  background: #ffffff;
  color: #475569;
}

.goods-section {
  padding: 20px 22px 22px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-head h3 {
  margin: 0;
  font-size: 21px;
  color: #0f172a;
}

.section-head p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.goods-card {
  overflow: hidden;
  border: none;
  border-radius: 22px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.goods-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 18px 36px rgba(59, 130, 246, 0.12);
}

.goods-card :deep(.el-card__body) {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-cover {
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #f5f8fc 0%, #edf3fa 100%);
}

.img,
.img-ph {
  width: 100%;
  height: 188px;
}

.img {
  display: block;
  object-fit: cover;
  transition: transform 0.28s ease;
}

.goods-card:hover .img {
  transform: scale(1.04);
}

.img-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 14px;
  background: linear-gradient(180deg, #f8fbff 0%, #eef4fb 100%);
}

.cover-tags {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.cover-tag {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.cover-tag.primary {
  background: rgba(255, 255, 255, 0.96);
  color: #1d4ed8;
}

.cover-tag.secondary {
  background: rgba(15, 23, 42, 0.74);
  color: #f8fafc;
}

.card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  padding: 16px 16px 15px;
}

.card-labels {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.mini-label {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.08);
  color: #16a34a;
  font-size: 10px;
  font-weight: 700;
}

.title {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.45;
}

.stats {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(226, 232, 240, 0.72);
  color: #94a3b8;
  font-size: 11px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.stat-item span:last-child {
  color: #94a3b8;
}

.stat-item .el-icon {
  color: #a8b4c5;
  font-size: 13px;
}

.card-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 14px;
}

.price-wrap {
  display: flex;
  flex-direction: column;
}

.price-label {
  font-size: 10px;
  color: #a5b2c3;
  letter-spacing: 0.03em;
}

.price {
  margin-top: 3px;
  color: #1d4ed8;
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.02em;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
}

@media (max-width: 1200px) {
  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .hero-filter-top {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    justify-content: flex-start;
  }

  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }

  .filter-actions {
    grid-column: 1 / -1;
  }

  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .hero-filter-card,
  .goods-section {
    padding: 18px;
    border-radius: 22px;
  }

  .hero-copy h2 {
    font-size: 24px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .card-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .pager {
    justify-content: center;
  }
}
</style>


