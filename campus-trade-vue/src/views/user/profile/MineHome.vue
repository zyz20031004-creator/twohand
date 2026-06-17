<template>
  <div class="mine-home">
    <section class="home-card">
      <div class="hero-card" v-loading="loadingProfile || loadingVerify">
        <div class="hero-head">
          <div class="hero-user">
            <el-avatar :size="72" class="hero-avatar" :src="avatarSrc">{{ avatarText }}</el-avatar>

            <div class="hero-meta">
              <div class="name-row">
                <h2 class="name">{{ displayName }}</h2>
                <span class="badge badge-blue">{{ verifyBadgeText }}</span>
              </div>

              <div class="meta-row">
                <span>{{ schoolText }}</span>
                <span class="dot">|</span>
                <span>{{ verify.status === "VERIFIED" ? "已完成校园认证" : "待完善身份信息" }}</span>
              </div>

              <div class="metric-row">
                <div v-for="item in overviewStats" :key="item.label" class="metric-item">
                  <span class="metric-label">{{ item.label }}</span>
                  <strong class="metric-value">{{ item.value }}</strong>
                </div>
              </div>
            </div>
          </div>

          <div class="hero-actions">
            <el-button class="edit-btn" @click="goTab('profile')">编辑资料</el-button>
          </div>
        </div>

        <div class="main-tabs">
          <button
            type="button"
            class="main-tab"
            :class="{ active: activePane === 'products' }"
            @click="activePane = 'products'"
          >
            我的发布
            <em>{{ counters.total }}</em>
          </button>

          <button
            type="button"
            class="main-tab"
            :class="{ active: activePane === 'credit' }"
            @click="activePane = 'credit'"
          >
            信誉与评价
            <em>{{ creditRecordsTotal }}</em>
          </button>
        </div>
      </div>

      <div class="content-card" v-loading="panelLoading">
        <template v-if="activePane === 'products'">
          <div class="section-head">
            <div>
              <h3 class="section-title">最近发布</h3>
              <p class="section-desc">这里只保留最近发布的概览，更多商品操作请前往商品管理页。</p>
            </div>
            <el-button class="ghost-btn" @click="goTab('products')">管理商品</el-button>
          </div>

          <div v-if="recentProducts.length" class="goods-grid">
            <UserCenterProductCard
              v-for="item in recentProducts"
              :key="item.id"
              variant="overview"
              :title="item.title"
              :price="item.price"
              :cover-url="toImg(item.coverUrl)"
              :status-text="resolveProductStatusText(item)"
              :status-tone="resolveDisplayStatus(item).tagType"
              @click="goProductDetail(item.id)"
            />
          </div>

          <el-empty v-else :description="homeEmptyText" />
        </template>

        <template v-else>
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

          <div v-if="filteredReviews.length" class="review-list">
            <article v-for="item in filteredReviews" :key="item.id" class="review-item">
              <el-avatar :size="44" class="review-avatar" :src="item.avatar">{{ item.avatarText }}</el-avatar>

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

          <el-empty v-else description="暂无评价记录" />
        </template>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { apiGetMyProductPage } from "@/api/product";
import { apiCreditMyReviews, type CreditRecordItem } from "@/api/credit";
import { apiGetMe, apiGetMyOrderPage } from "@/api/user";
import { apiVerifyMy } from "@/api/verify";
import UserCenterProductCard from "@/components/user-center/UserCenterProductCard.vue";
import { getStoredUser } from "@/utils/auth";
import { productImgUrl as resolveProductImgUrl, userAvatarUrl } from "@/utils/img";
import {
  getProductDisplayStatus,
  normalizeProductAuditStatus,
  type ProductAuditStatus,
} from "@/utils/productStatus";

type HomePane = "products" | "credit";
type ReviewFilter = "ALL" | "GOOD" | "NEUTRAL" | "BAD";
type ReviewLevel = "GOOD" | "NEUTRAL" | "BAD" | "NONE";

type MyProduct = {
  id: number;
  title: string;
  price: number | string;
  coverUrl?: string;
  status: "ON" | "OFF";
  auditStatus: ProductAuditStatus;
  createdAt?: string;
};

type ReviewItem = {
  id: number;
  username: string;
  avatar: string;
  avatarText: string;
  content: string;
  createdAt?: string;
  reviewLevel: ReviewLevel;
};

const router = useRouter();

const activePane = ref<HomePane>("products");
const activeReviewFilter = ref<ReviewFilter>("ALL");

const loadingProfile = ref(false);
const loadingVerify = ref(false);
const loadingProducts = ref(false);
const loadingCredit = ref(false);

const products = ref<MyProduct[]>([]);
const reviews = ref<ReviewItem[]>([]);
const soldProductIds = ref<Set<number>>(new Set());

const creditScore = ref(100);
const creditRecordsTotal = ref(0);

const user = reactive({
  username: "",
  name: "",
  avatar: "",
  fans: 0,
  follows: 0,
});

const verify = reactive({
  status: "UNVERIFIED" as "UNVERIFIED" | "PENDING" | "VERIFIED" | "REJECTED",
  school: "",
});

const counters = reactive({
  total: 0,
  on: 0,
  sold: 0,
  down: 0,
});

const panelLoading = computed(() => {
  return activePane.value === "products" ? loadingProducts.value : loadingCredit.value;
});

const displayName = computed(() => user.name || user.username || "用户");
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());

const verifyBadgeText = computed(() => {
  if (verify.status === "VERIFIED") return "校园认证用户";
  if (verify.status === "PENDING") return "认证审核中";
  if (verify.status === "REJECTED") return "认证未通过";
  return "未认证";
});

const schoolText = computed(() => {
  if (verify.school) return verify.school;
  if (verify.status === "VERIFIED") return "已完成校园认证";
  if (verify.status === "PENDING") return "校园认证审核中";
  if (verify.status === "REJECTED") return "校园认证待重提";
  return "校园二手交易用户";
});

const avatarSrc = computed(() => {
  return userAvatarUrl(user.avatar);
});

const overviewStats = computed(() => [
  { label: "发布", value: counters.total },
  { label: "成交", value: counters.sold },
  { label: "信用分", value: creditScore.value },
  { label: "在售中", value: counters.on },
]);

const recentProducts = computed(() => {
  return [...products.value]
    .sort((left, right) => {
      const leftTime = Date.parse(String(left.createdAt || ""));
      const rightTime = Date.parse(String(right.createdAt || ""));
      if (Number.isNaN(leftTime) && Number.isNaN(rightTime)) return right.id - left.id;
      if (Number.isNaN(leftTime)) return 1;
      if (Number.isNaN(rightTime)) return -1;
      return rightTime - leftTime;
    })
    .slice(0, 4);
});

const homeEmptyText = computed(() => {
  return counters.total > 0 ? "最近没有可展示的商品" : "还没有发布商品";
});

const reviewFilterOptions = computed(() => {
  const all = reviews.value;
  const good = all.filter((x) => x.reviewLevel === "GOOD").length;
  const neutral = all.filter((x) => x.reviewLevel === "NEUTRAL").length;
  const bad = all.filter((x) => x.reviewLevel === "BAD").length;

  return [
    { key: "ALL" as const, label: "全部", count: all.length },
    { key: "GOOD" as const, label: "好评", count: good },
    { key: "NEUTRAL" as const, label: "中评", count: neutral },
    { key: "BAD" as const, label: "差评", count: bad },
  ];
});

const filteredReviews = computed(() => {
  if (activeReviewFilter.value === "ALL") return reviews.value;
  if (activeReviewFilter.value === "GOOD") return reviews.value.filter((x) => x.reviewLevel === "GOOD");
  if (activeReviewFilter.value === "NEUTRAL") return reviews.value.filter((x) => x.reviewLevel === "NEUTRAL");
  return reviews.value.filter((x) => x.reviewLevel === "BAD");
});

function applyUser(source: any) {
  user.username = typeof source?.username === "string" ? source.username : "";
  user.name = typeof source?.name === "string" ? source.name : "";
  user.avatar = typeof source?.avatar === "string" ? source.avatar : "";
  user.fans = Number(source?.fans ?? source?.fanCount ?? user.fans ?? 0);
  user.follows = Number(source?.follows ?? source?.followCount ?? user.follows ?? 0);
}

function readStoredUser() {
  return getStoredUser();
}

function goTab(tab: string) {
  router.push(`/user/mine/${tab}`);
}

function goProductDetail(id: number) {
  router.push(`/user/hot/product/${id}`);
}

function toImg(url?: string) {
  return resolveProductImgUrl(url);
}

function formatTime(value?: string) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}

function toAvatar(url?: string) {
  return userAvatarUrl(url);
}

function normalizeProducts(records: any[]): MyProduct[] {
  return records.map((item) => ({
    id: Number(item?.id),
    title: String(item?.title || ""),
    price: item?.price ?? 0,
    coverUrl: typeof item?.coverUrl === "string" ? item.coverUrl : "",
    status: item?.status === "OFF" ? "OFF" : "ON",
    auditStatus: normalizeProductAuditStatus(item?.auditStatus),
    createdAt: typeof item?.createdAt === "string" ? item.createdAt : "",
  }));
}

function resolveDisplayStatus(product: MyProduct) {
  return getProductDisplayStatus(product, { soldProductIds: soldProductIds.value });
}

function hasDisplayStatus(product: MyProduct, key: "ON" | "OFF" | "SOLD") {
  return resolveDisplayStatus(product).key === key;
}

function isApprovedOn(product: MyProduct) {
  return hasDisplayStatus(product, "ON");
}

function isApprovedSold(product: MyProduct) {
  return hasDisplayStatus(product, "SOLD");
}

function isApprovedDown(product: MyProduct) {
  return hasDisplayStatus(product, "OFF");
}

function resolveProductStatusText(product: MyProduct) {
  return resolveDisplayStatus(product).text;
}

function resolveReviewUserName(record: CreditRecordItem) {
  return String(record?.sourceNickName || record?.sourceUserName || "").trim() || "匿名用户";
}

function resolveReviewAvatar(record: CreditRecordItem) {
  return toAvatar(record?.sourceAvatar);
}

function resolveReviewContent(record: CreditRecordItem) {
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

function resolveReviewLevel(record: CreditRecordItem, content: string, isOrderFinish: boolean): ReviewLevel {
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

function toReviewItems(records: CreditRecordItem[]): ReviewItem[] {
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
      avatar: avatar || toAvatar(""),
      avatarText: username.slice(0, 1).toUpperCase(),
      content,
      createdAt: record?.createdAt,
      reviewLevel,
    };
  });
}

async function loadSoldProductIds() {
  try {
    const result = await apiGetMyOrderPage<{ records: any[] }>({
      type: "SELL",
      page: 1,
      size: 500,
      status: "FINISHED",
    });

    const next = new Set<number>();
    (result?.records || []).forEach((row) => {
      const productId = Number(row?.productId);
      if (Number.isFinite(productId) && productId > 0) {
        next.add(productId);
      }
    });
    soldProductIds.value = next;
  } catch {
    soldProductIds.value = new Set<number>();
  }
}

async function loadProfile() {
  loadingProfile.value = true;
  try {
    const profile = await apiGetMe();
    applyUser(profile);
  } catch {
    const stored = readStoredUser();
    if (stored) applyUser(stored);
  } finally {
    loadingProfile.value = false;
  }
}

async function loadVerify() {
  loadingVerify.value = true;
  try {
    const data = await apiVerifyMy();
    verify.status = (data?.verifyStatus || "UNVERIFIED") as any;
    verify.school = data?.latestApply?.school || "";
  } catch {
    verify.status = "UNVERIFIED";
    verify.school = "";
  } finally {
    loadingVerify.value = false;
  }
}

async function loadCounters() {
  try {
    await loadSoldProductIds();
    const all = await apiGetMyProductPage({ page: 1, size: 500 });
    const records = normalizeProducts(Array.isArray(all?.records) ? all.records : []);

    counters.total = Number(all?.total ?? records.length);
    counters.on = records.filter(isApprovedOn).length;
    counters.sold = records.filter(isApprovedSold).length;
    counters.down = records.filter(isApprovedDown).length;
  } catch {
    counters.total = 0;
    counters.on = 0;
    counters.sold = 0;
    counters.down = 0;
  }
}

async function loadProducts() {
  loadingProducts.value = true;
  try {
    await loadSoldProductIds();
    const resp = await apiGetMyProductPage({ page: 1, size: 500 });
    products.value = normalizeProducts(Array.isArray(resp?.records) ? resp.records : []);
  } catch {
    products.value = [];
  } finally {
    loadingProducts.value = false;
  }
}

async function loadCredit() {
  loadingCredit.value = true;
  try {
    const resp = await apiCreditMyReviews({ page: 1, size: 80 });
    creditScore.value = Number(resp?.score ?? 100);
    creditRecordsTotal.value = Number(resp?.total ?? 0);

    const records = Array.isArray(resp?.records) ? (resp.records as CreditRecordItem[]) : [];
    reviews.value = toReviewItems(records);
  } catch {
    creditScore.value = 100;
    creditRecordsTotal.value = 0;
    reviews.value = [];
  } finally {
    loadingCredit.value = false;
  }
}

async function init() {
  const stored = readStoredUser();
  if (stored) applyUser(stored);

  await Promise.all([loadProfile(), loadVerify(), loadCounters(), loadProducts(), loadCredit()]);
}

watch(activePane, (pane) => {
  if (pane === "products") {
    loadCounters();
    loadProducts();
  } else {
    loadCredit();
  }
});

onMounted(init);
</script>

<style scoped>
.mine-home {
  display: block;
}

.home-card {
  border: 1px solid #ebeef4;
  border-radius: 24px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.hero-card {
  background:
    radial-gradient(circle at 82% 12%, rgba(187, 247, 208, 0.16), rgba(187, 247, 208, 0) 42%),
    radial-gradient(circle at 88% 22%, rgba(191, 219, 254, 0.34), rgba(191, 219, 254, 0) 36%),
    linear-gradient(96deg, #ffffff 0%, #f9fbff 58%, #f5fbf8 100%);
}

.hero-head {
  padding: 18px 22px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.hero-user {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
  flex: 1;
}

.hero-avatar {
  border: 2px solid #fff;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.14);
  flex: 0 0 auto;
}

.hero-meta {
  min-width: 0;
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.name {
  margin: 0;
  font-size: 24px;
  line-height: 1.1;
  color: #111827;
  letter-spacing: -0.01em;
}

.badge {
  height: 24px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.badge-blue {
  color: #1d4ed8;
  background: rgba(239, 246, 255, 0.96);
  border: 1px solid rgba(191, 219, 254, 0.9);
}

.meta-row {
  margin-top: 7px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: #4b5563;
  font-size: 14px;
}

.metric-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px 18px;
  flex-wrap: wrap;
}

.metric-item {
  min-width: 0;
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
}

.metric-label {
  color: #6b7280;
  font-size: 13px;
}

.metric-value {
  color: #0f172a;
  font-size: 18px;
  line-height: 1;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
}

.dot {
  color: #d1d5db;
}

.edit-btn {
  height: 38px;
  border-radius: 12px;
  padding: 0 16px;
  border: 1px solid #dbe6f2;
  background: #ffffff;
  color: #475569;
  font-weight: 700;
  box-shadow: none;
}

.edit-btn:hover {
  background: #f8fbff;
  color: #1d4ed8;
  border-color: rgba(191, 219, 254, 0.9);
}

.main-tabs {
  min-height: 56px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.88);
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 18px;
}

.main-tab {
  border: none;
  background: transparent;
  color: #6b7280;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  padding: 14px 2px;
  cursor: pointer;
  position: relative;
  transition: color 0.2s ease;
}

.main-tab em {
  margin-left: 6px;
  font-style: normal;
  font-size: 12px;
  color: #9ca3af;
}

.main-tab.active {
  color: #111827;
}

.main-tab.active::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: #3b82f6;
}

.content-card {
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  padding: 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.05);
}

.section-head {
  margin-bottom: 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-title {
  margin: 0;
  color: #111827;
  font-size: 17px;
  font-weight: 700;
}

.section-desc {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.6;
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
  height: 38px;
  border: 1px solid #e5edf7;
  border-radius: 12px;
  background: #ffffff;
  color: #475569;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.chip:hover {
  transform: translateY(-1px);
}

.chip em {
  margin-left: 6px;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-style: normal;
  font-size: 12px;
  line-height: 1;
}

.chip.active {
  background: rgba(239, 246, 255, 0.96);
  color: #1d4ed8;
  border-color: rgba(191, 219, 254, 0.9);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.12);
}

.chip.active em {
  background: rgba(59, 130, 246, 0.12);
}

.ghost-btn {
  height: 38px;
  border-radius: 12px;
  border: 1px solid #dbe6f2;
  color: #475569;
  background: #fff;
  font-weight: 700;
}

.ghost-btn:hover {
  border-color: rgba(191, 219, 254, 0.9);
  color: #1d4ed8;
  background: #f8fbff;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.goods-card {
  border-radius: 20px;
  padding: 12px;
  cursor: pointer;
  background: #fff;
  border: 1px solid #e8eef6;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.goods-card:hover {
  transform: translateY(-3px);
  border-color: #dbeafe;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.cover-wrap {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  aspect-ratio: 1 / 1;
  background: #f3f4f6;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 14px;
}

.status-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  border-radius: 999px;
  padding: 0 10px;
  height: 24px;
  line-height: 24px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: none;
}

.status-tag.on {
  background: rgba(240, 253, 244, 0.96);
  color: #15803d;
  border: 1px solid rgba(187, 247, 208, 0.92);
}

.status-tag.off {
  background: rgba(248, 250, 252, 0.96);
  color: #475569;
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.status-tag.pending {
  background: rgba(255, 247, 237, 0.96);
  color: #c2410c;
  border: 1px solid rgba(253, 186, 116, 0.9);
}

.status-tag.rejected {
  background: rgba(248, 250, 252, 0.96);
  color: #475569;
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.goods-body {
  padding: 2px 2px 0;
}

.goods-title {
  margin: 12px 0 8px;
  font-size: 15px;
  line-height: 1.5;
  font-weight: 600;
  color: #111827;
}

.goods-price {
  color: #ff5a2f;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
}

.goods-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #9ca3af;
  font-size: 12px;
  line-height: 1.5;
}

.review-chips .chip {
  height: 34px;
  padding: 0 14px;
  font-size: 14px;
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
  gap: 6px;
  flex-wrap: wrap;
  min-width: 0;
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
}

.identity.buyer {
  background: #dbeafe;
  color: #1d4ed8;
}

.identity.seller {
  background: #f8fafc;
  color: #475569;
}

.identity.system {
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

@media (min-width: 1560px) {
  .goods-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}

@media (max-width: 1240px) {
  .goods-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .hero-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-row {
    width: 100%;
    gap: 10px 16px;
  }

  .name {
    font-size: 22px;
  }

  .main-tab {
    font-size: 16px;
  }

  .goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .home-card {
    border-radius: 18px;
  }

  .hero-head {
    padding: 14px 12px 10px;
  }

  .hero-user {
    align-items: flex-start;
  }

  .hero-avatar {
    width: 64px;
    height: 64px;
  }

  .main-tabs {
    min-height: 46px;
    padding: 0 12px;
    gap: 10px;
  }

  .main-tab {
    font-size: 15px;
    padding: 12px 2px;
  }

  .main-tab em {
    font-size: 12px;
  }

  .content-card {
    padding: 12px;
  }

  .metric-row {
    gap: 8px 14px;
  }

  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .ghost-btn {
    width: 100%;
  }

  .goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .goods-card {
    padding: 10px;
    border-radius: 18px;
  }

  .goods-title {
    font-size: 14px;
  }

  .goods-price {
    font-size: 18px;
  }

  .review-item {
    padding: 12px;
  }

  .review-head {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 540px) {
  .goods-grid {
    grid-template-columns: 1fr;
  }

  .name-row,
  .meta-row {
    gap: 6px;
  }
}
</style>

