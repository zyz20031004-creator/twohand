<template>
  <div class="page-wrap" v-loading="pageLoading">
    <section v-if="pageError" class="page-card state-card">
      <el-result icon="error" title="主页加载失败" :sub-title="pageError">
        <template #extra>
          <el-button type="primary" @click="loadAll">重试</el-button>
        </template>
      </el-result>
    </section>

    <template v-else>
      <section class="page-card profile-card">
        <div class="profile-hero">
          <el-avatar :size="112" class="hero-avatar" :src="profileAvatar">
            <img :src="DEFAULT_COMMENT_AVATAR" alt="" class="avatar-fallback-img" />
          </el-avatar>

          <div class="hero-copy">
            <div class="hero-top">
              <div class="hero-heading">
                <span class="hero-badge">公开主页</span>
                <h1 class="hero-title">{{ displayName }}</h1>
                <p class="hero-subtitle">{{ publicUserSubtitle }}</p>

                <div class="hero-tags">
                  <el-tag :type="verifyTagType" effect="light" class="hero-tag">{{ verifyTagText }}</el-tag>
                  <span v-if="profile.school" class="hero-chip">{{ profile.school }}</span>
                  <span v-if="isVerified" class="hero-chip soft">校园认证用户</span>
                </div>
              </div>

              <div class="hero-metrics">
                <div class="metric-card">
                  <span>公开发布</span>
                  <strong>{{ publishedCount }}</strong>
                </div>
                <div class="metric-card soft">
                  <span>信用分</span>
                  <strong>{{ creditScore }}</strong>
                </div>
              </div>
            </div>

            <p class="hero-desc">
              这里只展示该用户允许公开查看的资料、商品与信誉记录，不包含手机号、地址等隐私信息。
            </p>
          </div>
        </div>
      </section>

      <section class="page-card content-card">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane name="products">
            <template #label>
              <span class="tab-label">TA 的发布</span>
              <span class="tab-count">{{ productTotal }}</span>
            </template>

            <div class="section-shell">
              <div class="section-head toolbar-head">
                <div>
                  <h3 class="section-title">公开商品</h3>
                  <p class="section-desc">仅展示审核通过且允许公开查看的商品信息。</p>
                </div>

                <div class="status-switch">
                  <button
                    v-for="option in statusOptions"
                    :key="option.value"
                    type="button"
                    class="status-chip"
                    :class="{ active: productQuery.status === option.value }"
                    @click="changeStatusFilter(option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>
              </div>

              <ListDataState
                :loading="productsLoading"
                :empty="!productsLoading && productCards.length === 0"
                :error-message="productsError"
                empty-text="TA 暂无公开商品"
                @retry="loadProducts"
              >
                <div class="card-grid">
                  <article v-for="item in productCards" :key="item.id" class="goods-card">
                    <div class="cover-wrap" @click="goProductDetail(item.id)">
                      <el-image v-if="item.coverUrl" class="cover-image" :src="imgUrl(item.coverUrl)" fit="cover" />
                      <div v-else class="cover-empty">暂无图片</div>
                      <el-tag class="status-tag" size="small" effect="light" :type="getProductStatusMeta(item).type">
                        {{ getProductStatusMeta(item).text }}
                      </el-tag>
                    </div>

                    <div class="card-content">
                      <h3 class="goods-title text-ellipsis-1" :title="item.title" @click="goProductDetail(item.id)">
                        {{ item.title || "未命名商品" }}
                      </h3>
                      <div class="price">{{ item.priceText }}</div>
                      <div class="meta-line">
                        <span class="meta-item">发布时间 {{ formatTime(item.createdAt) }}</span>
                        <span class="meta-item">浏览 {{ formatCount(item.viewCount) }}</span>
                      </div>
                    </div>
                  </article>
                </div>
              </ListDataState>

              <div v-if="!productsLoading && !productsError && productTotal > 0" class="pager">
                <div class="pager-copy">共 {{ productTotal }} 件公开商品</div>
                <el-pagination
                  background
                  layout="prev, pager, next"
                  :total="productTotal"
                  :page-size="productQuery.size"
                  :current-page="productQuery.page"
                  @current-change="handleProductPageChange"
                />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane name="credit">
            <template #label>
              <span class="tab-label">信誉与评价</span>
              <span class="tab-count">{{ creditTotal }}</span>
            </template>

            <div class="section-shell">
              <section class="record-section review-section">
                <div class="toolbar">
                  <div class="chips review-chips">
                    <button
                      v-for="item in reviewFilterOptions"
                      :key="item.key"
                      type="button"
                      class="chip"
                      :class="{ active: activeReviewFilter === item.key }"
                      @click="activeReviewFilter = item.key"
                    >
                      {{ item.label }}
                      <em v-if="item.count > 0">{{ item.count }}</em>
                    </button>
                  </div>
                </div>

                <ListDataState
                  :loading="creditLoading"
                  :empty="!creditLoading && filteredReviews.length === 0"
                  :error-message="creditError"
                  empty-text="暂无评价记录"
                  @retry="loadCredit"
                >
                  <div class="review-list">
                    <article v-for="item in filteredReviews" :key="item.id" class="review-item">
                      <el-avatar :size="44" class="review-avatar" :src="item.avatar">
                        {{ item.avatarText }}
                      </el-avatar>

                      <div class="review-main">
                        <div class="review-head">
                          <div class="review-user">
                            <span class="review-name">{{ item.username }}</span>
                            <span
                              v-if="item.reviewLevel !== 'NONE'"
                              class="identity"
                              :class="reviewLevelClass(item.reviewLevel)"
                            >
                              {{ reviewLevelText(item.reviewLevel) }}
                            </span>
                          </div>
                          <span class="review-time">{{ formatTime(item.createdAt) }}</span>
                        </div>
                        <p class="review-text">{{ item.content }}</p>
                      </div>
                    </article>
                  </div>
                </ListDataState>
              </section>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  apiGetUserPublicCredit,
  apiGetUserPublicProducts,
  apiGetUserPublicProfile,
  type UserPublicCreditResp,
  type UserPublicProductItem,
  type UserPublicProfileResp,
} from "@/api/user";
import ListDataState from "@/components/common/ListDataState.vue";
import { getApiErrorMessage } from "@/utils/apiError";
import { getCurrentUserId } from "@/utils/auth";
import { DEFAULT_COMMENT_AVATAR, getCommentUserAvatar } from "@/utils/commentDisplay";
import { imgUrl as resolveImgUrl, productImgUrl as resolveProductImgUrl } from "@/utils/img";
import { formatPrice } from "@/utils/price";

type StatusFilter = "ALL" | "ON" | "OFF" | "SOLD";
type CreditRecord = UserPublicCreditResp["records"][number];
type PublicProductCard = UserPublicProductItem & { priceText: string };
type ReviewFilter = "ALL" | "GOOD" | "NEUTRAL" | "BAD";
type ReviewLevel = "GOOD" | "NEUTRAL" | "BAD" | "NONE";
type ReviewItem = {
  id: number;
  username: string;
  avatar: string;
  avatarText: string;
  content: string;
  createdAt?: string;
  reviewLevel: ReviewLevel;
};

const route = useRoute();
const router = useRouter();

const activeTab = ref("products");
const pageLoading = ref(false);
const pageError = ref("");

const productsLoading = ref(false);
const productsError = ref("");
const productCards = ref<PublicProductCard[]>([]);
const productTotal = ref(0);
const productQuery = reactive({
  page: 1,
  size: 8,
  status: "ALL" as StatusFilter,
});

const creditLoading = ref(false);
const creditError = ref("");
const creditTotal = ref(0);
const creditScore = ref(100);
const activeReviewFilter = ref<ReviewFilter>("ALL");
const reviews = ref<ReviewItem[]>([]);

const profile = ref<UserPublicProfileResp>({
  id: 0,
  nickname: "",
  name: "",
  avatar: "",
  school: "",
  verifyStatus: "UNVERIFIED",
  creditScore: 100,
  publishedCount: 0,
});

const statusOptions: Array<{ label: string; value: StatusFilter }> = [
  { label: "综合", value: "ALL" },
  { label: "在售", value: "ON" },
  { label: "已售出", value: "SOLD" },
  { label: "已下架", value: "OFF" },
];

const profileUserId = computed(() => {
  const id = Number(route.params.id);
  return Number.isFinite(id) && id > 0 ? id : 0;
});

const currentUserId = computed(() => getCurrentUserId());
const isSelfProfile = computed(() => !!currentUserId.value && currentUserId.value === profileUserId.value);
const displayName = computed(() => {
  const name = String(profile.value.nickname || profile.value.name || "").trim();
  return name || "校园用户";
});
const publicUserSubtitle = computed(() => (isVerified.value ? "已认证用户" : "普通校园用户"));
const profileAvatar = computed(() => getCommentUserAvatar(profile.value));
const publishedCount = computed(() => Number(profile.value.publishedCount ?? productTotal.value ?? 0));
const isVerified = computed(() => profile.value.verifyStatus === "VERIFIED");

const verifyTagText = computed(() => {
  if (profile.value.verifyStatus === "VERIFIED") return "已认证";
  if (profile.value.verifyStatus === "PENDING") return "审核中";
  if (profile.value.verifyStatus === "REJECTED") return "认证未通过";
  return "未认证";
});

const verifyTagType = computed(() => {
  if (profile.value.verifyStatus === "VERIFIED") return "success";
  if (profile.value.verifyStatus === "PENDING") return "warning";
  if (profile.value.verifyStatus === "REJECTED") return "danger";
  return "info";
});
const reviewFilterOptions = computed(() => {
  const all = reviews.value;
  const good = all.filter((item) => item.reviewLevel === "GOOD").length;
  const neutral = all.filter((item) => item.reviewLevel === "NEUTRAL").length;
  const bad = all.filter((item) => item.reviewLevel === "BAD").length;

  return [
    { key: "ALL" as const, label: "全部", count: all.length },
    { key: "GOOD" as const, label: "好评", count: good },
    { key: "NEUTRAL" as const, label: "中评", count: neutral },
    { key: "BAD" as const, label: "差评", count: bad },
  ];
});
const filteredReviews = computed(() => {
  if (activeReviewFilter.value === "ALL") return reviews.value;
  if (activeReviewFilter.value === "GOOD") return reviews.value.filter((item) => item.reviewLevel === "GOOD");
  if (activeReviewFilter.value === "NEUTRAL") return reviews.value.filter((item) => item.reviewLevel === "NEUTRAL");
  return reviews.value.filter((item) => item.reviewLevel === "BAD");
});

function imgUrl(url?: string) {
  return resolveProductImgUrl(url);
}

function formatTime(value?: string) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}

function formatCount(value?: number) {
  const count = Number(value ?? 0);
  return Number.isFinite(count) && count > 0 ? count : 0;
}

function toAvatar(url?: string) {
  return resolveImgUrl(url);
}

function resolveReviewUserName(record: CreditRecord) {
  const sourceName = String(record?.sourceNickName || record?.sourceUserName || "").trim();
  if (sourceName) return sourceName;
  return "匿名用户";
}

function resolveReviewAvatar(record: CreditRecord) {
  const sourceAvatar = String(record?.sourceAvatar || "").trim();
  if (sourceAvatar) return toAvatar(sourceAvatar);
  return "";
}

function resolveReviewContent(record: CreditRecord) {
  const reviewContent = String(record?.reviewContent || "").trim();
  if (isDisplayableReviewText(reviewContent)) return reviewContent;

  const content = String(record?.content || "").trim();
  if (isDisplayableReviewText(content)) return content;

  const remark = String(record?.remark || "").trim();
  if (isDisplayableReviewText(remark)) return remark;
  return "买家已完成交易";
}

function isDisplayableReviewText(value: string) {
  if (!value) return false;
  return !/^(交易完成加分|浜ゆ槗瀹屾垚鍔犲垎|买家确认收货，交易完成|确认收货，交易完成)$/.test(value);
}

function extractReviewLevel(text: string): ReviewLevel {
  if (!text) return "NONE";
  const normalized = text.toLowerCase();
  if (/差评|待改进|不满意|很差|糟糕|bad/.test(normalized)) return "BAD";
  if (/中评|一般|中等|neutral/.test(normalized)) return "NEUTRAL";
  if (/好评|满意|很棒|不错|推荐|good|赞/.test(normalized)) return "GOOD";
  return "NONE";
}

function resolveReviewLevel(record: CreditRecord, content: string, isOrderFinish: boolean): ReviewLevel {
  const rating = String(record?.ratingLevel || "").toUpperCase();
  if (rating === "GOOD" || rating === "NEUTRAL" || rating === "BAD") return rating;

  const ratingText = String(record?.ratingText || "");
  const textLevel = extractReviewLevel(ratingText);
  if (textLevel !== "NONE") return textLevel;

  const combined = `${record?.remark || ""} ${content}`.trim();
  const explicitLevel = extractReviewLevel(combined);
  if (explicitLevel !== "NONE") return explicitLevel;

  const delta = Number(record?.changeVal ?? 0);
  if (isOrderFinish || delta > 0) return "GOOD";
  if (delta < 0) return "BAD";
  return "NONE";
}

function reviewLevelText(level: ReviewLevel) {
  if (level === "GOOD") return "好评";
  if (level === "NEUTRAL") return "中评";
  if (level === "BAD") return "差评";
  return "";
}

function reviewLevelClass(level: ReviewLevel) {
  if (level === "GOOD") return "good";
  if (level === "NEUTRAL") return "neutral";
  if (level === "BAD") return "bad";
  return "";
}

function toReviewItems(records: CreditRecord[]): ReviewItem[] {
  return records.map((record, index) => {
    const reason = String(record?.reason || "");
    const isOrderFinish = reason === "ORDER_FINISH" || reason === "ORDER_FINISHED";
    const username = resolveReviewUserName(record);
    const avatar = resolveReviewAvatar(record);
    const content = resolveReviewContent(record);
    const reviewLevel = resolveReviewLevel(record, content, isOrderFinish);

    return {
      id: Number(record?.id || index + 1),
      username,
      avatar: avatar || DEFAULT_COMMENT_AVATAR,
      avatarText: username.slice(0, 1).toUpperCase(),
      content,
      createdAt: record?.createdAt,
      reviewLevel,
    };
  });
}

function getProductStatusMeta(item: UserPublicProductItem) {
  const key = String(item.displayStatus || item.status || "OFF").toUpperCase();
  if (key === "ON") {
    return { text: "在售", type: "success" as const };
  }
  if (key === "SOLD") {
    return { text: "已售出", type: "danger" as const };
  }
  return { text: "已下架", type: "info" as const };
}

function goProductDetail(id: number) {
  router.push(`/user/hot/product/${id}`);
}

async function loadProfile() {
  const data = await apiGetUserPublicProfile(profileUserId.value);
  profile.value = {
    ...profile.value,
    ...data,
  };
  creditScore.value = Number(data?.creditScore ?? creditScore.value ?? 100);
}

async function loadProducts() {
  if (!profileUserId.value) return;
  productsLoading.value = true;
  productsError.value = "";
  try {
    const data = await apiGetUserPublicProducts(profileUserId.value, {
      page: productQuery.page,
      size: productQuery.size,
      status: productQuery.status === "ALL" ? undefined : productQuery.status,
    });
    productCards.value = Array.isArray(data?.records)
      ? data.records.map((item) => ({
          ...item,
          priceText: formatPrice(item.price),
        }))
      : [];
    productTotal.value = Number(data?.total ?? 0);
  } catch (error) {
    productsError.value = getApiErrorMessage(error, "加载公开商品失败");
    productCards.value = [];
    productTotal.value = 0;
  } finally {
    productsLoading.value = false;
  }
}

async function loadCredit() {
  if (!profileUserId.value) return;
  creditLoading.value = true;
  creditError.value = "";
  try {
    const data = await apiGetUserPublicCredit(profileUserId.value, {
      page: 1,
      size: 80,
    });
    creditScore.value = Number(data?.score ?? creditScore.value ?? 100);
    creditTotal.value = Number(data?.total ?? 0);
    const records = Array.isArray(data?.records) ? data.records : [];
    reviews.value = toReviewItems(records);
  } catch (error) {
    creditError.value = getApiErrorMessage(error, "加载信誉记录失败");
    reviews.value = [];
    creditTotal.value = 0;
  } finally {
    creditLoading.value = false;
  }
}

async function loadAll() {
  if (!profileUserId.value) {
    pageError.value = "用户编号无效";
    return;
  }

  pageLoading.value = true;
  pageError.value = "";
  try {
    await Promise.all([loadProfile(), loadProducts(), loadCredit()]);
  } catch (error) {
    pageError.value = getApiErrorMessage(error, "加载公开主页失败");
  } finally {
    pageLoading.value = false;
  }
}

function changeStatusFilter(status: StatusFilter) {
  if (productQuery.status === status) return;
  productQuery.status = status;
  productQuery.page = 1;
  void loadProducts();
}

function handleProductPageChange(page: number) {
  productQuery.page = page;
  void loadProducts();
}

watch(
  () => profileUserId.value,
  async () => {
    if (!profileUserId.value) {
      pageError.value = "用户编号无效";
      return;
    }
    if (isSelfProfile.value) {
      await router.replace("/user/mine/home");
      return;
    }

    productQuery.page = 1;
    productQuery.status = "ALL";
    activeReviewFilter.value = "ALL";
    creditTotal.value = 0;
    reviews.value = [];
    activeTab.value = "products";
    await loadAll();
  },
  { immediate: true }
);

watch(
  () => isSelfProfile.value,
  async (value) => {
    if (!value) return;
    await router.replace("/user/mine/home");
  }
);
</script>

<style scoped>
.page-wrap {
  max-width: 1240px;
  margin: 0 auto;
  padding: 12px 16px 24px;
}

.page-card {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #eef2f7;
  border-radius: 24px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.state-card {
  padding: 18px;
}

.profile-card {
  padding: 28px 30px;
}

.profile-hero {
  display: flex;
  align-items: flex-start;
  gap: 28px;
}

.hero-avatar {
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid rgba(191, 219, 254, 0.92);
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
}

.avatar-fallback-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.hero-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.hero-heading {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.hero-badge {
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

.hero-title {
  margin: 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.08;
  font-weight: 800;
}

.hero-subtitle {
  margin: 0;
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-tag {
  height: 34px;
  border-radius: 999px;
  padding: 0 14px;
  font-weight: 700;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(239, 246, 255, 0.96);
  border: 1px solid rgba(191, 219, 254, 0.9);
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.hero-chip.soft {
  border-color: rgba(220, 252, 231, 0.92);
  background: rgba(240, 253, 244, 0.96);
  color: #15803d;
}

.hero-metrics {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.metric-card {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  min-width: 126px;
  min-height: 84px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  background: rgba(239, 246, 255, 0.96);
}

.metric-card.soft {
  border-color: rgba(220, 252, 231, 0.92);
  background: rgba(240, 253, 244, 0.96);
}

.metric-card span {
  color: #7b8aa3;
  font-size: 12px;
  font-weight: 700;
}

.metric-card strong {
  margin-top: 8px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
}

.metric-card.soft strong {
  color: #15803d;
}

.hero-desc {
  margin: 0;
  max-width: 760px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.72;
}

.content-card {
  margin-top: 18px;
  padding: 22px 24px 24px;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: rgba(226, 232, 240, 0.92);
}

.profile-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 999px;
  background: #2563eb;
}

.profile-tabs :deep(.el-tabs__item) {
  height: 42px;
  padding: 0 8px;
  color: #64748b;
  font-size: 15px;
  font-weight: 700;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: #0f172a;
}

.tab-label {
  display: inline-flex;
  align-items: center;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  margin-left: 8px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.section-shell {
  padding-top: 2px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-head {
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
}

.section-desc {
  margin: 6px 0 0;
  color: #7b8aa3;
  font-size: 13px;
  line-height: 1.72;
}

.status-switch {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.status-chip {
  height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid #dbe6f2;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.status-chip:hover,
.status-chip.active {
  border-color: #93c5fd;
  background: rgba(239, 246, 255, 0.96);
  color: #2563eb;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chips {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.chip {
  height: 34px;
  border: 1px solid #e5edf7;
  border-radius: 12px;
  background: #ffffff;
  color: #475569;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chip:hover {
  transform: translateY(-1px);
}

.chip.active {
  background: rgba(239, 246, 255, 0.96);
  color: #1d4ed8;
  border-color: rgba(191, 219, 254, 0.9);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.12);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
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
  height: 220px;
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
  padding: 16px;
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

.record-section {
  border: 1px solid #edf2f7;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.record-section {
  padding: 20px;
}

.review-chips .chip {
  height: 34px;
  padding: 0 14px;
}

.chip em {
  margin-left: 6px;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  color: inherit;
  font-style: normal;
  font-size: 11px;
  line-height: 18px;
}

.chip.active em {
  background: rgba(59, 130, 246, 0.12);
}

.review-list {
  border: 1px solid #e8eef6;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.review-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  transition: background 0.2s ease;
}

.review-item + .review-item {
  border-top: 1px solid #edf1f5;
}

.review-item:hover {
  background: #fcfdff;
}

.review-avatar {
  border: 1px solid #d9e1ea;
  flex: 0 0 auto;
}

.review-main {
  flex: 1;
  min-width: 0;
}

.review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.review-name {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.identity {
  display: inline-flex;
  align-items: center;
  height: 22px;
  border-radius: 999px;
  padding: 0 9px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.identity.buyer {
  background: #dbeafe;
  color: #1d4ed8;
}

.identity.system {
  background: #f8fafc;
  color: #475569;
}

.identity.seller {
  background: #f8fafc;
  color: #475569;
}

.identity.good {
  background: #dcfce7;
  color: #15803d;
}

.identity.neutral {
  background: #fff7ed;
  color: #c2410c;
}

.identity.bad {
  background: #fee2e2;
  color: #b91c1c;
}

.review-time {
  color: #6b7280;
  font-size: 12px;
  white-space: nowrap;
}

.review-text {
  margin: 8px 0 0;
  color: #374151;
  font-size: 14px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 1180px) {
  .card-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 920px) {
  .profile-hero,
  .hero-top,
  .section-head,
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-metrics,
  .status-switch {
    justify-content: flex-start;
  }

  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-wrap {
    padding: 10px 12px 20px;
  }

  .profile-card,
  .content-card,
  .record-section {
    padding: 18px;
    border-radius: 18px;
  }

  .hero-title {
    font-size: 28px;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }

  .review-item {
    padding: 12px;
  }

  .review-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>


