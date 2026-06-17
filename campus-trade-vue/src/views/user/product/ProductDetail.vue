<template>
  <div class="page" v-loading="loading">
    <div class="shell">
      <div class="topbar">
        <div class="top-left">
          <el-button text class="back-btn" @click="goBack">
            <el-icon>
              <ArrowLeft />
            </el-icon>
            <span>返回热卖</span>
          </el-button>
          <div>
            <span class="eyebrow">校园二手</span>
            <h1>商品详情</h1>
          </div>
        </div>
        <span class="chip">{{ publicTradeBadge }}</span>
      </div>

      <div v-if="errorMessage" class="state-card">
        <el-empty description="商品加载失败">
          <el-button type="primary" @click="loadAll">重新加载</el-button>
        </el-empty>
        <p class="state-text">{{ errorMessage }}</p>
      </div>

      <template v-else>
        <el-card class="hero-card" shadow="never">
          <div class="hero-grid">
            <section class="gallery" :class="{ 'single-image': !hasMultipleImages }">
              <div class="main-stage">
                <el-image v-if="activeImage" class="main-image" :src="imgUrl(activeImage.url)" fit="cover"
                  :preview-src-list="previewImageUrls" :initial-index="activeImageIndex" preview-teleported />
                <div v-else class="empty-stage">暂无图片</div>
              </div>

              <div v-if="hasMultipleImages" class="thumbs">
                <button v-for="(image, index) in displayImages" :key="`${image.url}-${index}`" type="button"
                  class="thumb" :class="{ active: activeImageIndex === index }" @click="setActiveImage(index)">
                  <img :src="imgUrl(image.url)" alt="" />
                </button>
              </div>
            </section>

            <section class="summary">
              <div class="tags">
                <span class="tag primary">校园好物</span>
                <span class="tag">{{ displayImages.length }} 张图片</span>
                <span class="tag soft">支持当面验货</span>
              </div>

              <h2 class="title">{{ detail.title || "未命名商品" }}</h2>
              <div class="price-label">参考价格</div>
              <div class="price">{{ formatPrice(detail.price) }}</div>

              <div class="stats">
                <span class="stat"><el-icon>
                    <View />
                  </el-icon>{{ detail.viewCount || 0 }} 浏览</span>
                <span class="stat"><el-icon>
                    <Star />
                  </el-icon>{{ detail.likeCount || 0 }} 点赞</span>
                <span class="stat"><el-icon>
                    <CollectionTag />
                  </el-icon>{{ detail.favoriteCount || 0 }} 收藏</span>
              </div>

              <div class="meta">
                <div class="meta-item">
                  <el-icon>
                    <Location />
                  </el-icon>
                  <span>交易方式：{{ tradeDetail.methodText }}</span>
                </div>
                <div class="meta-item">
                  <el-icon>
                    <Location />
                  </el-icon>
                  <span v-if="tradeDetail.description">{{ tradeDetail.description }}</span>
                  <span v-else>交易地点：{{ tradeDetail.locationText }}</span>
                </div>
                <div class="meta-item">
                  <span>{{ detail.schoolName || "本校商品" }}</span>
                </div>
                <div class="meta-item">
                  <el-icon>
                    <Timer />
                  </el-icon>
                  <span>发布时间 {{ formatTime(detail.createdAt) }}</span>
                </div>
              </div>

              <div class="actions">
                <el-button class="action-btn stateful-action-btn" :class="{ 'is-active': detail.liked }"
                  :loading="likeLoading" @click="toggleLikeAction">
                  <el-icon>
                    <Star />
                  </el-icon>
                  <span>{{ detail.liked ? "已点赞" : "点赞" }}</span>
                </el-button>
                <el-button class="action-btn stateful-action-btn" :class="{ 'is-active': detail.favorited }"
                  :loading="favoriteLoading" @click="toggleFavoriteAction">
                  <el-icon>
                    <CollectionTag />
                  </el-icon>
                  <span>{{ detail.favorited ? "已收藏" : "收藏" }}</span>
                </el-button>
                <el-button class="action-btn" :disabled="!canChatSeller" @click="goChat">
                  <el-icon>
                    <ChatDotRound />
                  </el-icon>
                  <span>联系卖家</span>
                </el-button>
                <el-button type="primary" class="buy-btn" :loading="buying" :disabled="!canBuyNow" @click="buyNow">
                  <el-icon>
                    <ShoppingCartFull />
                  </el-icon>
                  <span>立即购买</span>
                </el-button>
              </div>

              <div v-if="canReportProduct" class="secondary-tools">
                <el-button text class="report-trigger" @click="openReportDialog">举报商品</el-button>
              </div>

              <div v-if="isOwner" class="owner-tip">这是你自己发布的商品，不能购买或发起聊天。</div>

              <div class="seller" :class="{ clickable: !!detail.sellerId }" @click="goSellerHome">
                <el-avatar :size="52" :src="sellerAvatarUrl">{{ sellerInitial }}</el-avatar>
                <div class="seller-main">
                  <span class="seller-label">卖家信息</span>
                  <div class="seller-name-row">
                    <strong>{{ sellerDisplayName }}</strong>
                    <span class="seller-badge">{{ sellerBadgeText }}</span>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </el-card>

        <el-card class="detail-tabs-card" shadow="never">
          <el-tabs v-model="activeTab" class="detail-tabs">
            <el-tab-pane name="description">
              <template #label>
                <span class="tab-label">商品描述</span>
              </template>

              <section class="tab-panel">
                <div class="section-head">
                  <div>
                    <span class="eyebrow">商品详情</span>
                    <h3>商品描述</h3>
                    <p>查看卖家填写的商品成色、配件和交易说明。</p>
                  </div>
                </div>
                <div class="desc">{{ detail.description || "卖家暂未填写商品描述" }}</div>
              </section>
            </el-tab-pane>

            <el-tab-pane name="comments">
              <template #label>
                <span class="tab-label">评论区</span>
                <span class="tab-count">{{ totalCount }}</span>
              </template>

              <section class="tab-panel comment-tab-panel">
                <section class="product-comment-section">
                  <div class="product-comment-header">
                    <div class="product-comment-copy">
                      <h3 class="product-comment-title">评论区</h3>
                      <p class="product-comment-subtitle">交流商品细节，也可以直接给卖家留言</p>
                    </div>
                  </div>

                  <div class="product-comment-editor">
                    <el-input
                      v-model="newComment"
                      type="textarea"
                      :rows="4"
                      resize="none"
                      :maxlength="commentMaxlength"
                      placeholder="写下你对这件商品的疑问或看法..."
                    />

                    <div class="product-comment-editor-footer">
                      <span class="product-comment-count">{{ newCommentLength }}/{{ commentMaxlength }}</span>
                      <el-button
                        type="primary"
                        class="product-comment-submit"
                        :disabled="!canSubmitNew"
                        @click="submitNew"
                      >
                        发布评论
                      </el-button>
                    </div>
                  </div>

                  <div v-if="commentTree.length" class="product-comment-list">
                    <article
                      v-for="comment in commentTree"
                      :key="getCommentId(comment)"
                      class="product-comment-card"
                    >
                      <div class="product-comment-main">
                        <el-avatar
                          :size="42"
                          class="product-comment-avatar"
                          :class="{ clickable: getCommentUserId(comment) > 0 }"
                          :src="getCommentAvatar(comment)"
                          @click.stop="goCommentUserHome(comment)"
                        >
                          {{ getCommentDisplayName(comment).slice(0, 1) }}
                        </el-avatar>

                        <div class="product-comment-body">
                          <div class="product-comment-meta">
                            <button
                              type="button"
                              class="product-comment-name"
                              :class="{ clickable: getCommentUserId(comment) > 0 }"
                              @click.stop="goCommentUserHome(comment)"
                            >
                              {{ getCommentDisplayName(comment) }}
                            </button>
                            <span class="product-comment-time">{{ formatCommentDisplayTime(comment.createdAt) }}</span>
                          </div>

                          <div class="product-comment-text">{{ comment.content }}</div>

                          <div class="product-comment-actions">
                            <button
                              type="button"
                              class="product-comment-action"
                              @click="openReply(getCommentId(comment), getCommentDisplayName(comment))"
                            >
                              回复
                            </button>
                            <button
                              v-if="canDeleteComment(comment)"
                              type="button"
                              class="product-comment-action danger"
                              @click="doDelete(getCommentId(comment))"
                            >
                              删除
                            </button>
                          </div>

                          <div v-if="replyParentId === getCommentId(comment)" class="product-comment-reply-editor">
                            <el-input
                              v-model="replyContent"
                              type="textarea"
                              :rows="3"
                              resize="none"
                              :maxlength="replyMaxlength"
                              placeholder="请输入回复内容..."
                            />

                            <div class="product-comment-reply-footer">
                              <span class="product-comment-count">{{ replyContentLength }}/{{ replyMaxlength }}</span>
                              <div class="product-comment-reply-buttons">
                                <el-button size="small" @click="closeReply">取消</el-button>
                                <el-button
                                  size="small"
                                  type="primary"
                                  :disabled="!canSubmitReply"
                                  @click="submitReply(getCommentId(comment))"
                                >
                                  发布回复
                                </el-button>
                              </div>
                            </div>
                          </div>

                          <div v-if="getCommentChildren(comment).length" class="product-comment-children">
                            <article
                              v-for="child in getCommentChildren(comment)"
                              :key="getCommentId(child)"
                              class="product-comment-reply-card"
                            >
                              <el-avatar
                                :size="38"
                                class="product-comment-avatar reply-avatar"
                                :class="{ clickable: getCommentUserId(child) > 0 }"
                                :src="getCommentAvatar(child)"
                                @click.stop="goCommentUserHome(child)"
                              >
                                {{ getCommentDisplayName(child).slice(0, 1) }}
                              </el-avatar>

                              <div class="product-comment-reply-body">
                                <div class="product-comment-meta reply-meta">
                                  <button
                                    type="button"
                                    class="product-comment-name reply-name"
                                    :class="{ clickable: getCommentUserId(child) > 0 }"
                                    @click.stop="goCommentUserHome(child)"
                                  >
                                    {{ getCommentDisplayName(child) }}
                                  </button>
                                  <span class="product-comment-time">{{ formatCommentDisplayTime(child.createdAt) }}</span>
                                </div>

                                <div class="product-comment-text reply-text">{{ child.content }}</div>

                                <div v-if="canDeleteComment(child)" class="product-comment-actions reply-actions">
                                  <button
                                    type="button"
                                    class="product-comment-action danger"
                                    @click="doDelete(getCommentId(child))"
                                  >
                                    删除
                                  </button>
                                </div>
                              </div>
                            </article>
                          </div>
                        </div>
                      </div>
                    </article>
                  </div>

                  <div v-else class="product-comment-empty">
                    <el-empty :image-size="88" description="暂无评论，来留下第一条留言吧" />
                  </div>
                </section>
              </section>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </template>
    </div>

    <el-dialog v-model="reportDialog.visible" title="举报商品" width="520px" destroy-on-close>
      <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-width="88px">
        <el-form-item label="举报原因" prop="reason">
          <el-select v-model="reportForm.reason" placeholder="请选择举报原因" class="report-select">
            <el-option v-for="item in reportReasonOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明" prop="detail">
          <el-input
            v-model="reportForm.detail"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            placeholder="可选填，补充举报说明，便于管理员判断"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeReportDialog">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ArrowLeft,
  ChatDotRound,
  CollectionTag,
  Location,
  ShoppingCartFull,
  Star,
  Timer,
  View,
} from "@element-plus/icons-vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { apiCreateOrGetProductSession } from "@/api/chat";
import { apiAddProductComment, apiDeleteComment, apiGetProductComments } from "@/api/comment";
import { apiGetProductDetail, apiToggleFavorite, apiToggleLike, type ProductDetailResp, type ProductImageItem } from "@/api/product";
import { apiSubmitReport } from "@/api/report";
import { useAuthUser } from "@/composables/useAuthUser";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  formatCommentTime as formatCommentDisplayTime,
  getCommentUserAvatar,
  getCommentUserName,
  normalizePositiveNumber,
} from "@/utils/commentDisplay";
import { productImgUrl as resolveProductImgUrl, sanitizeProductImageUrl } from "@/utils/img";
import { formatPrice } from "@/utils/price";
import { getPublicTradeBadge, getTradeDetail } from "@/utils/tradeLocation";
import { goToUserHome } from "@/utils/userHome";

type CommentNode = {
  id: number;
  userId: number;
  nickName?: string;
  nickname?: string;
  username?: string;
  name?: string;
  userName?: string;
  avatar?: string;
  avatarUrl?: string;
  avatar_url?: string;
  content: string;
  createdAt?: string;
  canDelete?: boolean;
  children?: CommentNode[];
};

type DetailState = ProductDetailResp & { images: ProductImageItem[] };

const route = useRoute();
const router = useRouter();
const { getUserId, requireLogin } = useAuthUser();
const { runConfirmAction } = useConfirmAction();

const loading = ref(false);
const errorMessage = ref("");
const likeLoading = ref(false);
const favoriteLoading = ref(false);
const buying = ref(false);
const reportSubmitting = ref(false);
const detail = ref<DetailState>(createEmptyDetail());
const commentTree = ref<CommentNode[]>([]);
const totalCount = ref(0);
const newComment = ref("");
const replyParentId = ref<number | null>(null);
const replyContent = ref("");
const activeImageIndex = ref(0);
const activeTab = ref("description");
const reportFormRef = ref<FormInstance>();
const commentMaxlength = 500;
const replyMaxlength = 500;

const productId = computed(() => {
  const id = Number(route.params.id);
  return Number.isFinite(id) && id > 0 ? id : 0;
});

const currentUserId = computed(() => getUserId());
const isOwner = computed(() => !!currentUserId.value && currentUserId.value === Number(detail.value.sellerId || 0));
const canChatSeller = computed(() => !!detail.value.sellerId && !isOwner.value);
const canBuyNow = computed(() => !!detail.value.id && !isOwner.value);
const canReportProduct = computed(() => !!detail.value.id && !isOwner.value);
const canSubmitNew = computed(() => !!currentUserId.value && newComment.value.trim().length > 0);
const canSubmitReply = computed(() => !!currentUserId.value && replyContent.value.trim().length > 0);
const tradeDetail = computed(() => getTradeDetail(detail.value.addressText));
const publicTradeBadge = computed(() => getPublicTradeBadge(detail.value.addressText));
const sellerAvatarUrl = computed(() => imgUrl(detail.value.sellerAvatar || ""));
const sellerDisplayName = computed(() => {
  const name = String(detail.value.sellerName || "").trim();
  return name || "校园卖家";
});
const sellerBadgeText = computed(() => (isOwner.value ? "我的发布" : "校园卖家"));
const sellerInitial = computed(() => {
  const name = sellerDisplayName.value;
  return name ? name.slice(0, 1) : "卖";
});

const displayImages = computed<ProductImageItem[]>(() => {
  const images = normalizeImages(detail.value.images, detail.value.coverUrl);
  return images.length ? images : [];
});
const hasMultipleImages = computed(() => displayImages.value.length > 1);
const activeImage = computed(() => displayImages.value[activeImageIndex.value] || null);
const previewImageUrls = computed(() => displayImages.value.map((item) => imgUrl(item.url)));
const reportReasonOptions = ["虚假信息", "疑似违规", "价格异常", "商品描述不符", "其他"];
const newCommentLength = computed(() => newComment.value.length);
const replyContentLength = computed(() => replyContent.value.length);

const reportDialog = reactive({
  visible: false,
});

const reportForm = reactive({
  reason: "",
  detail: "",
});

const reportRules: FormRules = {
  reason: [{ required: true, message: "请选择举报原因", trigger: "change" }],
  detail: [{ max: 300, message: "补充说明不能超过 300 个字符", trigger: "blur" }],
};

function createEmptyDetail(): DetailState {
  return {
    id: 0,
    title: "",
    price: 0,
    description: "",
    addressText: "",
    createdAt: "",
    viewCount: 0,
    coverUrl: "",
    images: [],
    likeCount: 0,
    liked: false,
    favoriteCount: 0,
    favorited: false,
    sellerId: 0,
    sellerName: "",
    sellerAvatar: "",
    schoolName: "",
  };
}

function imgUrl(url?: string) {
  return resolveProductImgUrl(url || "");
}

function sanitizeImageUrl(url: unknown) {
  if (typeof url !== "string") return "";
  return sanitizeProductImageUrl(url);
}

function normalizeImages(source: unknown, fallback?: unknown) {
  const list = Array.isArray(source) ? source : [];
  const seen = new Set<string>();
  const images: ProductImageItem[] = [];

  function push(rawUrl: unknown, rawSort?: unknown) {
    const url = sanitizeImageUrl(rawUrl);
    if (!url || seen.has(url)) return;
    seen.add(url);
    const sort = Number(rawSort);
    images.push({ url, sort: Number.isFinite(sort) && sort > 0 ? sort : images.length + 1 });
  }

  list.forEach((item, index) => {
    if (typeof item === "string") {
      push(item, index + 1);
      return;
    }
    if (item && typeof item === "object") {
      const record = item as Record<string, unknown>;
      push(record.url, record.sort ?? index + 1);
    }
  });

  if (!images.length && typeof fallback === "string" && fallback.trim()) {
    push(fallback, 1);
  }

  return images
    .sort((a, b) => Number(a.sort || 0) - Number(b.sort || 0))
    .map((item, index) => ({ ...item, sort: index + 1 }));
}

function formatTime(value?: string) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}

function getCommentId(comment?: CommentNode | null) {
  return normalizePositiveNumber(comment?.id);
}

function getCommentUserId(comment?: CommentNode | null) {
  return normalizePositiveNumber(comment?.userId);
}

function getCommentChildren(comment?: CommentNode | null) {
  return Array.isArray(comment?.children) ? comment.children : [];
}

function getCommentDisplayName(comment?: CommentNode | null) {
  return getCommentUserName(comment ?? undefined);
}

function getCommentAvatar(comment?: CommentNode | null) {
  return getCommentUserAvatar(comment ?? undefined);
}

function canDeleteComment(comment?: CommentNode | null) {
  return !!comment?.canDelete;
}

function goCommentUserHome(comment?: CommentNode | null) {
  const userId = getCommentUserId(comment);
  if (!userId) return;
  void goToUserHome(router, userId);
}

function countAll(tree: CommentNode[]) {
  let count = 0;
  tree.forEach((item) => {
    count += 1;
    if (item.children?.length) count += countAll(item.children);
  });
  return count;
}

function normalizeCommentTreeResponse(payload: unknown) {
  if (Array.isArray(payload)) {
    return {
      records: payload as CommentNode[],
      total: countAll(payload as CommentNode[]),
    };
  }

  if (payload && typeof payload === "object") {
    const record = payload as Record<string, unknown>;
    const rows = Array.isArray(record.records)
      ? (record.records as CommentNode[])
      : Array.isArray(record.list)
        ? (record.list as CommentNode[])
        : [];
    const total = Number(record.total);
    return {
      records: rows,
      total: Number.isFinite(total) ? total : countAll(rows),
    };
  }

  return {
    records: [] as CommentNode[],
    total: 0,
  };
}

function setActiveImage(index: number) {
  if (index < 0 || index >= displayImages.value.length) return;
  activeImageIndex.value = index;
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push("/user/hot");
}

function goSellerHome() {
  if (!detail.value.sellerId) return;
  void goToUserHome(router, detail.value.sellerId);
}

function resetReportForm() {
  reportForm.reason = "";
  reportForm.detail = "";
  reportFormRef.value?.clearValidate();
}

function openReportDialog() {
  const userId = requireLogin("请先登录后再操作");
  if (!userId) return;
  if (isOwner.value) {
    ElMessage.warning("不能举报自己发布的商品");
    return;
  }
  resetReportForm();
  reportDialog.visible = true;
}

function closeReportDialog() {
  reportDialog.visible = false;
  resetReportForm();
}

async function loadDetail() {
  const data = await apiGetProductDetail(productId.value);
  const images = normalizeImages(data?.images, data?.coverUrl);
  detail.value = {
    ...createEmptyDetail(),
    ...data,
    coverUrl: images[0]?.url || sanitizeImageUrl(data?.coverUrl) || "",
    images,
  };
  activeImageIndex.value = 0;
}

async function loadComments() {
  const data = await apiGetProductComments({
    productId: productId.value,
    currentUserId: currentUserId.value ?? undefined,
  });
  const normalized = normalizeCommentTreeResponse(data);
  commentTree.value = normalized.records;
  totalCount.value = normalized.total;
}

async function loadAll() {
  if (!productId.value) {
    errorMessage.value = "商品编号无效";
    return;
  }
  loading.value = true;
  errorMessage.value = "";
  try {
    await loadDetail();
    try {
      await loadComments();
    } catch (error) {
      commentTree.value = [];
      totalCount.value = 0;
      ElMessage.warning(getApiErrorMessage(error, "评论加载失败"));
    }
  } catch (error) {
    detail.value = createEmptyDetail();
    errorMessage.value = getApiErrorMessage(error, "加载商品详情失败");
  } finally {
    loading.value = false;
  }
}

async function toggleLikeAction() {
  const userId = requireLogin("请先登录后再操作");
  if (!userId) return;
  likeLoading.value = true;
  try {
    const data = await apiToggleLike(productId.value);
    detail.value.liked = !!data?.liked;
    detail.value.likeCount = Number(data?.likeCount || 0);
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "点赞失败"));
  } finally {
    likeLoading.value = false;
  }
}

async function toggleFavoriteAction() {
  const userId = requireLogin("请先登录后再操作");
  if (!userId) return;
  favoriteLoading.value = true;
  try {
    const data = await apiToggleFavorite(productId.value);
    detail.value.favorited = !!data?.favorited;
    detail.value.favoriteCount = Number(data?.favoriteCount || 0);
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "收藏失败"));
  } finally {
    favoriteLoading.value = false;
  }
}

function goChat() {
  const userId = requireLogin("请先登录后再操作");
  if (!userId) return;
  if (isOwner.value) {
    ElMessage.warning("不能和自己发起聊天");
    return;
  }
  if (!detail.value.sellerId) {
    ElMessage.warning("未找到卖家信息");
    return;
  }
  void openProductChat();
}

async function openProductChat() {
  try {
    const session: any = await apiCreateOrGetProductSession({ productId: productId.value });
    const sessionId = Number(session?.sessionId || session?.id);
    if (!Number.isFinite(sessionId) || sessionId <= 0) {
      ElMessage.error("创建聊天会话失败");
      return;
    }
    router.push({
      path: "/user/chat",
      query: {
        sessionId: String(sessionId),
        productId: String(productId.value),
      },
    });
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "创建聊天会话失败"));
  }
}

async function buyNow() {
  const userId = requireLogin("请先登录后再操作");
  if (!userId) return;
  if (isOwner.value) {
    ElMessage.warning("不能购买自己发布的商品");
    return;
  }
  buying.value = true;
  try {
    // 跳转到确认订单页面
    router.push(`/user/order/confirm/${productId.value}`);
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "操作失败"));
  } finally {
    buying.value = false;
  }
}

async function submitReport() {
  if (!reportFormRef.value || !productId.value) return;
  const valid = await reportFormRef.value.validate().catch(() => false);
  if (!valid) return;

  reportSubmitting.value = true;
  try {
    await apiSubmitReport({
      productId: productId.value,
      reason: reportForm.reason,
      detail: reportForm.detail.trim() || undefined,
    });
    ElMessage.success("举报已提交，管理员会尽快处理");
    closeReportDialog();
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "举报提交失败"));
  } finally {
    reportSubmitting.value = false;
  }
}

async function submitNew() {
  const userId = requireLogin("请先登录");
  if (!userId) return;
  const content = newComment.value.trim();
  if (!content) return;
  try {
    await apiAddProductComment({ productId: productId.value, userId, content });
    newComment.value = "";
    activeTab.value = "comments";
    ElMessage.success("评论成功");
    await loadComments();
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "评论失败"));
  }
}

function openReply(parentId: number, nickname?: string) {
  const userId = requireLogin("请先登录");
  if (!userId) return;
  replyParentId.value = parentId;
  replyContent.value = nickname ? `@${nickname} ` : "";
}

function closeReply() {
  replyParentId.value = null;
  replyContent.value = "";
}

async function submitReply(parentId: number) {
  const userId = requireLogin("请先登录");
  if (!userId) return;
  const content = replyContent.value.trim();
  if (!content) return;
  try {
    await apiAddProductComment({ productId: productId.value, userId, parentId, content });
    ElMessage.success("回复成功");
    closeReply();
    activeTab.value = "comments";
    await loadComments();
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "回复失败"));
  }
}

async function doDelete(commentId: number) {
  const userId = requireLogin("请先登录");
  if (!userId) return;
  await runConfirmAction({
    title: "删除评论",
    message: "确认删除这条评论吗？",
    type: "warning",
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    successMessage: "删除成功",
    errorMessage: "删除失败，请稍后重试",
    action: () => apiDeleteComment(commentId, userId),
    onSuccess: loadComments,
  });
}

watch(
  () => route.params.id,
  () => {
    newComment.value = "";
    activeTab.value = "description";
    closeReply();
    void loadAll();
  },
  { immediate: true },
);

watch(
  () => displayImages.value.length,
  (length) => {
    if (!length || activeImageIndex.value >= length) {
      activeImageIndex.value = 0;
    }
  },
  { immediate: true },
);
</script>

<style scoped>
.page {
  padding: 16px 14px 34px;
}

.shell {
  width: min(1120px, 100%);
  margin: 0 auto;
}

.topbar,
.top-left,
.section-head {
  display: flex;
  gap: 14px;
}

.topbar,
.section-head {
  align-items: flex-start;
  justify-content: space-between;
}

.topbar {
  margin-bottom: 18px;
  padding: 0 4px;
}

.back-btn {
  gap: 6px;
  padding: 0;
  color: #2563eb;
  font-weight: 700;
}

.eyebrow {
  display: inline-flex;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.top-left h1,
.section-head h3 {
  margin: 4px 0 0;
  color: #0f172a;
  font-weight: 800;
}

.top-left h1 {
  font-size: 28px;
}

.chip,
.tag,
.stat {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-weight: 700;
}

.chip {
  height: 34px;
  padding: 0 14px;
  background: rgba(59, 130, 246, .1);
  color: #1d4ed8;
  font-size: 12px;
}

.state-card,
.hero-card,
.detail-tabs-card {
  border: 1px solid rgba(226, 232, 240, .88);
  border-radius: 28px;
  background: rgba(255, 255, 255, .98);
  box-shadow: 0 18px 44px rgba(15, 23, 42, .06);
}

.state-card {
  padding: 24px;
}

.state-text {
  margin: 12px 0 0;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

.hero-card {
  margin-bottom: 18px;
}

.hero-card :deep(.el-card__body),
.detail-tabs-card :deep(.el-card__body) {
  padding: 24px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, .98fr) minmax(320px, 1.02fr);
  gap: 20px;
  align-items: start;
}

.gallery,
.summary {
  min-width: 0;
  align-self: start;
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.gallery.single-image {
  gap: 0;
}

.main-stage {
  height: clamp(320px, 34vw, 408px);
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%);
  border: 1px solid rgba(226, 232, 240, .84);
}

.main-image {
  width: 100%;
  height: 100%;
  display: block;
}

:deep(.main-image .el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.empty-stage {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.thumb {
  width: 80px;
  flex: 0 0 80px;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, .86);
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  transition: border-color .18s ease, transform .18s ease, box-shadow .18s ease;
}

.thumb:hover,
.thumb.active {
  border-color: rgba(59, 130, 246, .46);
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, .1);
}

.thumb img {
  width: 100%;
  height: 80px;
  display: block;
  object-fit: cover;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  height: 30px;
  padding: 0 12px;
  background: rgba(241, 245, 249, .94);
  color: #475569;
  font-size: 12px;
}

.tag.primary {
  background: rgba(59, 130, 246, .1);
  color: #2563eb;
}

.tag.soft {
  background: rgba(34, 197, 94, .1);
  color: #15803d;
}

.title {
  margin: 16px 0 0;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.32;
  font-weight: 800;
}

.price-label {
  margin-top: 18px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
}

.price {
  margin-top: 6px;
  color: #d2663c;
  font-size: 38px;
  line-height: 1;
  font-weight: 800;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.stat {
  gap: 6px;
  min-height: 34px;
  padding: 0 12px;
  background: rgba(248, 251, 255, .96);
  border: 1px solid rgba(226, 232, 240, .86);
  color: #64748b;
  font-size: 12px;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
  color: #475569;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  line-height: 1.5;
}

.address-select-panel {
  min-height: 120px;
}

.address-radio-group {
  width: 100%;
  display: grid;
  gap: 10px;
}

.address-radio {
  width: 100%;
  height: auto;
  margin: 0;
  padding: 12px;
}

.address-radio-content {
  display: grid;
  gap: 6px;
  white-space: normal;
  line-height: 1.5;
}

.address-radio-content span {
  color: #64748b;
}

.seller {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, .88);
  background: rgba(248, 251, 255, .9);
}

.seller.clickable {
  cursor: pointer;
  transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}

.seller.clickable:hover {
  border-color: rgba(147, 197, 253, .92);
  box-shadow: 0 10px 20px rgba(59, 130, 246, .08);
  transform: translateY(-1px);
}

.seller-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.seller-label {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .06em;
  text-transform: uppercase;
}

.seller-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.seller-name-row strong {
  color: #0f172a;
  font-size: 16px;
  line-height: 1.3;
}

.seller-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, .1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
  align-items: stretch;
}

.secondary-tools {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.report-trigger {
  padding: 0;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 600;
}

.report-trigger:hover {
  color: #64748b;
}

.report-select {
  width: 100%;
}

.action-btn,
.buy-btn,
.submit-btn {
  height: 44px;
  border-radius: 14px;
  font-weight: 700;
}

.action-btn,
.buy-btn {
  width: 100%;
  min-width: 0;
  margin: 0;
  padding: 0 16px;
  justify-content: center;
}

.action-btn {
  border-color: #dbe6f2;
  color: #475569;
  background: #fff;
  box-shadow: none;
}

.stateful-action-btn {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  transition: all .2s ease;
}

.stateful-action-btn:hover {
  border-color: #7aa8ff;
  background: #f5f9ff;
  color: #409eff;
}

.stateful-action-btn.is-active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

.buy-btn,
.submit-btn {
  border: none;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 60%, #22c55e 100%);
  box-shadow: 0 12px 20px rgba(59, 130, 246, .16);
}

.actions :deep(.el-button > span) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
}

.actions :deep(.el-button .el-icon) {
  margin-right: 0;
}

.actions :deep(.stateful-action-btn .el-icon),
.actions :deep(.stateful-action-btn span) {
  color: inherit;
  transition: color .2s ease;
}

.actions :deep(.el-button.is-disabled) {
  opacity: .72;
}

.owner-tip {
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.7;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.detail-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: rgba(226, 232, 240, .92);
}

.detail-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 999px;
  background: #2563eb;
}

.detail-tabs :deep(.el-tabs__item) {
  height: 42px;
  padding: 0 8px;
  color: #64748b;
  font-size: 15px;
  font-weight: 700;
}

.detail-tabs :deep(.el-tabs__item.is-active) {
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
  background: rgba(59, 130, 246, .1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.tab-panel {
  padding-top: 2px;
}

.comment-tab-panel {
  padding-top: 4px;
}

.section-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
}

.section-head p {
  margin: 6px 0 0;
  color: #7b8aa3;
  font-size: 13px;
  line-height: 1.7;
}

.desc {
  margin-top: 14px;
  padding: 20px 22px;
  border-radius: 22px;
  border: 1px solid rgba(226, 232, 240, .88);
  background: linear-gradient(180deg, rgba(255, 255, 255, .98) 0%, rgba(248, 251, 255, .98) 100%);
  color: #334155;
  white-space: pre-wrap;
  line-height: 1.92;
  min-height: 180px;
}

.product-comment-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-comment-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.product-comment-copy {
  min-width: 0;
}

.product-comment-title {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.3;
}

.product-comment-subtitle {
  margin: 8px 0 0;
  color: #7b8aa3;
  font-size: 14px;
  line-height: 1.7;
}

.product-comment-editor {
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, .9);
  background: linear-gradient(180deg, rgba(255, 255, 255, .99) 0%, rgba(249, 251, 255, .98) 100%);
}

.product-comment-editor :deep(.el-textarea__inner),
.product-comment-reply-editor :deep(.el-textarea__inner) {
  min-height: 108px !important;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(214, 223, 235, .9);
  background: #fcfdff;
  box-shadow: none;
  line-height: 1.75;
}

.product-comment-reply-editor :deep(.el-textarea__inner) {
  min-height: 96px !important;
}

.product-comment-editor-footer,
.product-comment-reply-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.product-comment-count {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1;
}

.product-comment-submit {
  min-width: 104px;
  height: 38px;
  padding: 0 18px;
  border-radius: 12px;
  box-shadow: none;
}

.product-comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-comment-card {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, .9);
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, .04);
}

.product-comment-main,
.product-comment-reply-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.product-comment-avatar {
  flex: 0 0 auto;
  border: 1px solid rgba(203, 213, 225, .9);
  background: linear-gradient(135deg, rgba(59, 130, 246, .08), rgba(148, 163, 184, .12));
  color: #334155;
  font-weight: 800;
}

.product-comment-avatar.clickable,
.product-comment-name.clickable {
  cursor: pointer;
}

.product-comment-body,
.product-comment-reply-body,
.product-comment-copy {
  min-width: 0;
}

.product-comment-body,
.product-comment-reply-body {
  flex: 1;
}

.product-comment-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.product-comment-name {
  padding: 0;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.4;
  font-weight: 700;
  transition: color .2s ease;
}

.product-comment-name.clickable:hover {
  color: #2563eb;
}

.product-comment-time {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.4;
}

.product-comment-text {
  margin-top: 8px;
  color: #334155;
  white-space: pre-wrap;
  line-height: 1.75;
}

.product-comment-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 10px;
}

.product-comment-action {
  padding: 0;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  transition: color .2s ease;
}

.product-comment-action:hover {
  color: #2563eb;
}

.product-comment-action.danger:hover,
.product-comment-action.danger {
  color: #dc2626;
}

.product-comment-reply-editor {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, .9);
  background: rgba(248, 251, 255, .92);
}

.product-comment-reply-buttons {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.product-comment-children {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.product-comment-reply-card {
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, .82);
  background: rgba(250, 252, 255, .95);
}

.reply-avatar {
  border-color: rgba(214, 223, 235, .96);
}

.reply-meta .product-comment-name {
  font-size: 14px;
}

.reply-text {
  margin-top: 7px;
}

.reply-actions {
  margin-top: 8px;
}

.product-comment-empty {
  min-height: 198px;
  border-radius: 20px;
  border: 1px dashed rgba(203, 213, 225, .9);
  background: rgba(248, 251, 255, .72);
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-comment-empty :deep(.el-empty) {
  padding: 0;
}

.product-comment-empty :deep(.el-empty__description p) {
  color: #94a3b8;
}

@media (max-width: 980px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .page {
    padding: 14px 12px 28px;
  }

  .topbar,
  .top-left,
  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .top-left h1 {
    font-size: 24px;
  }

  .hero-card :deep(.el-card__body),
  .detail-tabs-card :deep(.el-card__body) {
    padding: 18px;
  }

  .title {
    font-size: 26px;
  }

  .main-stage {
    height: clamp(280px, 78vw, 340px);
  }

  .actions {
    grid-template-columns: 1fr;
  }

  .detail-tabs :deep(.el-tabs__item) {
    font-size: 14px;
  }

  .product-comment-editor,
  .product-comment-card {
    padding: 14px;
  }

  .product-comment-editor-footer,
  .product-comment-reply-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .product-comment-submit {
    width: 100%;
  }

  .product-comment-reply-buttons {
    width: 100%;
    justify-content: flex-end;
  }
}

@media (max-width: 520px) {
  .desc {
    padding: 16px;
    min-height: 140px;
  }

  .thumb {
    width: 68px;
    flex-basis: 68px;
  }

  .thumb img {
    height: 68px;
  }

  .product-comment-title {
    font-size: 22px;
  }

  .product-comment-main,
  .product-comment-reply-card {
    gap: 10px;
  }

  .product-comment-actions {
    gap: 14px;
  }

  .product-comment-reply-buttons {
    flex-wrap: wrap;
  }

  .product-comment-reply-buttons :deep(.el-button) {
    flex: 1 1 0;
    min-width: 0;
  }
}
</style>

