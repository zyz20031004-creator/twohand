<template>
  <section class="panel user-page-shell" v-loading="loading">
    <div class="filter-bar">
      <div class="filter-copy">
        <span class="filter-badge">求购专区</span>
        <h2>求购专区</h2>
        <p>浏览同学们正在寻找的校园好物，快速筛选当前状态，更高效地找到你能帮上的信息。</p>
      </div>

      <div class="filter-tools-shell">
        <div class="filter-tools">
          <div class="filter-control is-filter">
            <span class="control-label">状态筛选</span>
            <el-select
              v-model="statusFilter"
              class="status-select"
              placeholder="请选择状态"
              @change="handleFilterChange"
            >
              <el-option label="全部" value="ALL" />
              <el-option label="进行中" value="OPEN" />
              <el-option label="已解决" value="SOLVED" />
            </el-select>
          </div>

          <div class="filter-stat is-status">
            <span class="stat-label">当前状态</span>
            <span class="stat-value">{{ currentFilterLabel }}</span>
          </div>

          <div class="filter-stat soft is-total">
            <span class="stat-label">累计信息</span>
            <span class="stat-value">{{ total }} 条</span>
          </div>
        </div>
      </div>
    </div>

    <ListDataState
      :loading="loading"
      :empty="pagedList.length === 0"
      :error-message="errorMessage"
      empty-text="当前筛选下暂无求购信息"
      @retry="loadData"
    >
      <div class="list">
        <el-card v-for="item in pagedList" :key="item.id" shadow="never" class="card">
          <article class="wanted-card" :class="{ 'is-open': item.open }">
            <div class="wanted-main">
              <div class="wanted-content">
                <div class="wanted-head">
                  <div class="wanted-author">
                    <el-avatar
                      :size="42"
                      class="ph-avatar user-link"
                      :src="getUserAvatar(item)"
                      @click="goUserHome(item.userId)"
                      @error="handleAvatarError"
                    >
                      <img :src="DEFAULT_AVATAR" alt="" class="avatar-fallback-img" />
                    </el-avatar>

                    <div class="wanted-author-meta">
                      <button type="button" class="wanted-user user-link" @click="goUserHome(item.userId)">
                        {{ getUserNickname(item) }}
                      </button>
                      <span class="wanted-time">发布时间 {{ item.time }}</span>
                    </div>
                  </div>

                  <span class="status" :class="item.status === 'SOLVED' ? 'done' : 'todo'">
                    {{ item.status === "SOLVED" ? "已解决" : "进行中" }}
                  </span>
                </div>

                <h3 class="wanted-title" :title="item.title || '未命名求购'">
                  {{ item.title || "未命名求购" }}
                </h3>

                <p class="wanted-desc">{{ item.desc || "暂未填写求购描述" }}</p>
              </div>

              <div class="wanted-media">
                <div class="img-box">
                  <img
                    v-if="item.imageUrl && !isImageBroken(item.id)"
                    :src="imgUrl(item.imageUrl)"
                    class="img-real"
                    @error="handleImageError(item.id)"
                  />
                  <div v-else class="img-ph">
                    <span class="img-badge">求购配图</span>
                    <strong>暂无图片</strong>
                    <p>当前求购暂未上传参考图片</p>
                  </div>
                </div>
              </div>
            </div>

            <div class="wanted-footer">
              <div class="wanted-meta">
                <span class="meta-pill">评论 {{ item.commentTotal }} 条</span>
                <span class="meta-pill">浏览 {{ item.viewCount }} 次</span>
              </div>

              <div class="wanted-actions">
                <el-button class="toggle-btn" :class="{ 'is-open': item.open }" @click="toggle(item)">
                  {{ item.open ? "收起评论" : "查看评论" }}
                </el-button>
              </div>
            </div>

            <transition name="comment-expand">
              <div v-if="item.open" class="comments" v-loading="item.commentLoading">
                <section class="comment-section">
                  <div class="comment-header">
                    <div class="comment-copy">
                      <h4 class="comment-title">评论区</h4>
                      <p class="comment-subtitle">交流需求细节，也可以帮对方补充线索</p>
                    </div>
                  </div>

                  <div class="comment-editor">
                    <el-input
                      v-model="item.newComment"
                      type="textarea"
                      :rows="4"
                      resize="none"
                      :maxlength="COMMENT_MAXLENGTH"
                      placeholder="请输入评论内容（最多 200 字）"
                    />

                    <div class="comment-editor-footer">
                      <span class="comment-count">{{ getDraftLength(item.newComment) }}/{{ COMMENT_MAXLENGTH }}</span>
                      <el-button
                        type="primary"
                        class="comment-submit-btn"
                        :disabled="!canSubmitNew(item)"
                        @click="submitNew(item)"
                      >
                        发表评论
                      </el-button>
                    </div>
                  </div>

                  <div v-if="item.rootPageList.length" class="comment-list">
                    <article
                      v-for="comment in item.rootPageList"
                      :key="getCommentId(comment)"
                      class="comment-card"
                    >
                      <div class="comment-main">
                        <el-avatar
                          :size="40"
                          class="comment-avatar"
                          :class="{ clickable: getCommentUserId(comment) > 0 }"
                          :src="getCommentAvatar(comment)"
                          @click.stop="goCommentUserHome(comment)"
                        >
                          {{ getCommentDisplayName(comment).slice(0, 1) }}
                        </el-avatar>

                        <div class="comment-body">
                          <div class="comment-meta">
                            <button
                              type="button"
                              class="comment-name"
                              :class="{ clickable: getCommentUserId(comment) > 0 }"
                              @click.stop="goCommentUserHome(comment)"
                            >
                              {{ getCommentDisplayName(comment) }}
                            </button>
                            <span class="comment-time">{{ formatCommentDisplayTime(comment.createdAt) }}</span>
                          </div>

                          <div class="comment-text">{{ comment.content }}</div>

                          <div class="comment-actions">
                            <button
                              type="button"
                              class="comment-action"
                              @click="openReply(item, getCommentId(comment), getCommentDisplayName(comment))"
                            >
                              回复
                            </button>
                            <button
                              v-if="canDeleteComment(comment)"
                              type="button"
                              class="comment-action danger"
                              @click="deleteComment(item, getCommentId(comment))"
                            >
                              删除
                            </button>
                          </div>

                          <div v-if="item.replyParentId === getCommentId(comment)" class="reply-editor">
                            <el-input
                              v-model="item.replyContent"
                              type="textarea"
                              :rows="3"
                              resize="none"
                              :maxlength="REPLY_MAXLENGTH"
                              placeholder="请输入回复内容（最多 200 字）"
                            />

                            <div class="reply-editor-footer">
                              <span class="comment-count">{{ getDraftLength(item.replyContent) }}/{{ REPLY_MAXLENGTH }}</span>
                              <div class="reply-editor-actions">
                                <el-button size="small" @click="closeReply(item)">取消</el-button>
                                <el-button
                                  size="small"
                                  type="primary"
                                  :disabled="!canSubmitReply(item)"
                                  @click="submitReply(item, getCommentId(comment))"
                                >
                                  发布回复
                                </el-button>
                              </div>
                            </div>
                          </div>

                          <div v-if="getCommentChildren(comment).length" class="reply-list">
                            <article
                              v-for="child in getCommentChildren(comment)"
                              :key="getCommentId(child)"
                              class="reply-card"
                            >
                              <el-avatar
                                :size="36"
                                class="comment-avatar reply-avatar"
                                :class="{ clickable: getCommentUserId(child) > 0 }"
                                :src="getCommentAvatar(child)"
                                @click.stop="goCommentUserHome(child)"
                              >
                                {{ getCommentDisplayName(child).slice(0, 1) }}
                              </el-avatar>

                              <div class="reply-body">
                                <div class="comment-meta">
                                  <button
                                    type="button"
                                    class="comment-name reply-name"
                                    :class="{ clickable: getCommentUserId(child) > 0 }"
                                    @click.stop="goCommentUserHome(child)"
                                  >
                                    {{ getCommentDisplayName(child) }}
                                  </button>
                                  <span class="comment-time">{{ formatCommentDisplayTime(child.createdAt) }}</span>
                                </div>

                                <div class="comment-text reply-text">{{ child.content }}</div>

                                <div v-if="canDeleteComment(child)" class="comment-actions reply-actions">
                                  <button
                                    type="button"
                                    class="comment-action danger"
                                    @click="deleteComment(item, getCommentId(child))"
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

                  <div v-else class="comment-empty">
                    <el-empty :image-size="76" description="暂无评论，来留下第一条留言吧" />
                  </div>

                  <div v-if="item.rootTotal > item.commentQuery.size" class="comment-pager">
                    <el-pagination
                      background
                      layout="prev, pager, next, jumper"
                      :current-page="item.commentQuery.page"
                      :page-size="item.commentQuery.size"
                      :total="item.rootTotal"
                      @current-change="(page: number) => onRootPageChange(item, page)"
                    />
                  </div>
                </section>
              </div>
            </transition>
          </article>
        </el-card>
      </div>
    </ListDataState>

    <div v-if="!loading && !errorMessage && total > 0" class="page-pager">
      <el-pagination
        :current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { apiIncreaseWantedView, apiWantedPage } from "@/api/wanted";
import { apiAddWantedComment, apiDeleteWantedComment, apiGetWantedComments } from "@/api/comment";
import { useAuthUser } from "@/composables/useAuthUser";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { usePagedQuery } from "@/composables/usePagedQuery";
import ListDataState from "@/components/common/ListDataState.vue";
import {
  DEFAULT_COMMENT_AVATAR,
  formatCommentTime as formatCommentDisplayTime,
  getCommentUserAvatar,
  getCommentUserName,
  normalizeDisplayText,
  normalizePositiveNumber,
} from "@/utils/commentDisplay";
import { getApiErrorMessage } from "@/utils/apiError";
import { imgUrl as resolveImgUrl } from "@/utils/img";
import { goToUserHome } from "@/utils/userHome";

type UserDisplayLike = {
  userId?: number | null;
  nickname?: string | null;
  nickName?: string | null;
  name?: string | null;
  username?: string | null;
  userName?: string | null;
  avatar?: string | null;
  avatarUrl?: string | null;
};

type CommentNode = UserDisplayLike & {
  id: number;
  userId: number;
  parentId: number | null;
  content: string;
  createdAt?: string;
  canDelete?: boolean;
  mine?: boolean;
  children?: CommentNode[];
};

type WantedItem = UserDisplayLike & {
  id: number;
  userId: number;
  title: string;
  status: "OPEN" | "SOLVED";
  time: string;
  desc: string;
  open: boolean;
  commentLoading: boolean;
  commentTree: CommentNode[];
  commentTotal: number;
  viewCount: number;
  rootTotal: number;
  commentQuery: { page: number; size: number };
  newComment: string;
  replyParentId: number | null;
  replyToName: string;
  replyContent: string;
  rootPageList: CommentNode[];
  imageUrl?: string | null;
};

type StatusFilter = "ALL" | "OPEN" | "SOLVED";

const DEFAULT_AVATAR = DEFAULT_COMMENT_AVATAR;
const COMMENT_MAXLENGTH = 200;
const REPLY_MAXLENGTH = 200;
const COMMENT_PAGE_SIZE = 5;

const router = useRouter();

const loading = ref(false);
const statusFilter = ref<StatusFilter>("ALL");
const list = ref<WantedItem[]>([]);
const total = ref(0);
const errorMessage = ref("");
const brokenImageIds = ref<number[]>([]);
const { query, changePage, search } = usePagedQuery({
  page: 1,
  size: 5,
});
const { getUserId, requireLogin } = useAuthUser();
const { runConfirmAction } = useConfirmAction();

const currentFilterLabel = computed(() => {
  if (statusFilter.value === "OPEN") return "进行中";
  if (statusFilter.value === "SOLVED") return "已解决";
  return "全部";
});

const pagedList = computed(() => list.value);

function mapStatus(status?: string): "OPEN" | "SOLVED" {
  return status === "SOLVED" ? "SOLVED" : "OPEN";
}

function fmtTime(value?: string) {
  if (!value) return "";
  return value.replace("T", " ").slice(0, 16);
}

function imgUrl(url?: string | null) {
  return resolveImgUrl(url ?? undefined);
}

const normalizeText = normalizeDisplayText;
const normalizeNumber = normalizePositiveNumber;

function getUserNickname(user?: UserDisplayLike | null) {
  return getCommentUserName(user);
}

function getUserAvatar(user?: Pick<UserDisplayLike, "avatar" | "avatarUrl"> | null) {
  return getCommentUserAvatar(user);
}

function getCommentId(comment?: CommentNode | null) {
  return normalizeNumber(comment?.id);
}

function getCommentUserId(comment?: CommentNode | null) {
  return normalizeNumber(comment?.userId);
}

function getCommentDisplayName(comment?: CommentNode | null) {
  return getCommentUserName(comment);
}

function getCommentAvatar(comment?: CommentNode | null) {
  return getCommentUserAvatar(comment);
}

function getCommentChildren(comment?: CommentNode | null) {
  return Array.isArray(comment?.children) ? comment.children : [];
}

function canDeleteComment(comment?: CommentNode | null) {
  return !!(comment?.canDelete || comment?.mine);
}

function getDraftLength(value?: string | null) {
  return String(value || "").length;
}

function handleAvatarError() {
  return false;
}

function goUserHome(userId?: number | null) {
  if (!userId) return;
  void goToUserHome(router, userId);
}

function goCommentUserHome(comment?: CommentNode | null) {
  goUserHome(getCommentUserId(comment));
}

function normalizeCommentNode(source: any): CommentNode {
  return {
    id: Number(source?.id || 0),
    userId: normalizeNumber(source?.userId),
    parentId: source?.parentId == null ? null : normalizeNumber(source?.parentId),
    content: String(source?.content || ""),
    createdAt: normalizeText(source?.createdAt),
    nickName: normalizeText(source?.nickName),
    name: normalizeText(source?.name),
    username: normalizeText(source?.username),
    nickname: getUserNickname({
      userId: normalizeNumber(source?.userId),
      nickname: source?.nickname,
      nickName: source?.nickName,
      name: source?.name,
    }),
    userName: "",
    avatar: normalizeText(source?.avatar),
    avatarUrl: normalizeText(source?.avatarUrl),
    canDelete: !!source?.canDelete,
    mine: !!source?.mine,
    children: Array.isArray(source?.children) ? source.children.map((item: any) => normalizeCommentNode(item)) : [],
  };
}

function isImageBroken(id: number) {
  return brokenImageIds.value.includes(id);
}

function handleImageError(id: number) {
  if (brokenImageIds.value.includes(id)) return;
  brokenImageIds.value = [...brokenImageIds.value, id];
}

function getLoginUserId(): number | null {
  return getUserId();
}

function resolveCommentTotal(item: any) {
  const raw = item?.commentCount ?? item?.comment_count ?? item?.commentTotal ?? 0;
  const count = Number(raw);
  return Number.isFinite(count) && count >= 0 ? count : 0;
}

function resolveViewCount(item: any) {
  const raw = item?.viewCount ?? item?.view_count ?? 0;
  const count = Number(raw);
  return Number.isFinite(count) && count >= 0 ? count : 0;
}

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const response: any = await apiWantedPage({
      page: query.page,
      size: query.size,
      status: statusFilter.value === "ALL" ? undefined : statusFilter.value,
    });

    total.value = Number(response?.total || 0);
    list.value = (response?.records || []).map((item: any) => ({
      id: Number(item.id),
      userId: normalizeNumber(item.userId),
      title: String(item.title || ""),
      status: mapStatus(item.status),
      nickName: normalizeText(item.nickName),
      name: normalizeText(item.name),
      username: normalizeText(item.username),
      avatar: normalizeText(item.avatar),
      nickname: getUserNickname({
        userId: normalizeNumber(item.userId),
        nickname: item.nickname,
        nickName: item.nickName,
        name: item.name,
      }),
      avatarUrl: item.avatarUrl ?? null,
      time: fmtTime(item.createdAt),
      desc: String(item.content || ""),
      open: false,
      commentLoading: false,
      commentTree: [],
      commentTotal: resolveCommentTotal(item),
      viewCount: resolveViewCount(item),
      rootTotal: 0,
      commentQuery: { page: 1, size: COMMENT_PAGE_SIZE },
      newComment: "",
      replyParentId: null,
      replyToName: "",
      replyContent: "",
      rootPageList: [],
      imageUrl: item.imageUrl ?? null,
    }));
    brokenImageIds.value = [];
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "加载求购信息失败");
    list.value = [];
    total.value = 0;
    brokenImageIds.value = [];
  } finally {
    loading.value = false;
  }
}

async function loadCommentTree(item: WantedItem) {
  item.commentLoading = true;
  try {
    const userId = getLoginUserId();
    const response: any = await apiGetWantedComments({
      targetId: item.id,
      currentUserId: userId ?? undefined,
    });

    const roots = Array.isArray(response?.records)
      ? response.records.map((comment: any) => normalizeCommentNode(comment))
      : [];
    item.commentTree = roots;
    item.commentTotal = Number(response?.total ?? 0);
    item.rootTotal = roots.length;

    const maxPage = Math.max(1, Math.ceil((roots.length || 0) / item.commentQuery.size));
    if (item.commentQuery.page > maxPage) item.commentQuery.page = 1;
    item.rootPageList = getRootPageList(item);
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "评论加载失败"));
    item.commentTree = [];
    item.commentTotal = 0;
    item.rootTotal = 0;
    item.rootPageList = [];
  } finally {
    item.commentLoading = false;
  }
}

function getRootPageList(item: WantedItem) {
  const roots = item.commentTree || [];
  const start = (item.commentQuery.page - 1) * item.commentQuery.size;
  return roots.slice(start, start + item.commentQuery.size);
}

async function toggle(item: WantedItem) {
  item.open = !item.open;
  if (item.open) {
    await recordWantedView(item);
    await loadCommentTree(item);
  }
}

async function recordWantedView(item: WantedItem) {
  try {
    await apiIncreaseWantedView(item.id);
    item.viewCount += 1;
  } catch {
    // 浏览量记录失败不阻断评论查看。
  }
}

function canSubmitNew(item: WantedItem) {
  return !!getLoginUserId() && item.newComment.trim().length > 0;
}

function canSubmitReply(item: WantedItem) {
  return !!getLoginUserId() && item.replyContent.trim().length > 0;
}

async function submitNew(item: WantedItem) {
  const userId = requireLogin("请先登录");
  if (!userId) return;

  const content = item.newComment.trim();
  if (!content) return;

  await apiAddWantedComment({
    targetId: item.id,
    userId,
    content,
    parentId: null,
  });

  ElMessage.success("评论成功");
  item.newComment = "";
  item.commentQuery.page = 1;
  await loadCommentTree(item);
}

function openReply(item: WantedItem, parentId: number, name?: string | null) {
  if (!requireLogin("请先登录")) return;

  item.replyParentId = parentId;
  item.replyToName = name || "";
  item.replyContent = item.replyToName ? `@${item.replyToName} ` : "";
}

function closeReply(item: WantedItem) {
  item.replyParentId = null;
  item.replyToName = "";
  item.replyContent = "";
}

async function submitReply(item: WantedItem, parentId: number) {
  const userId = requireLogin("请先登录");
  if (!userId) return;

  const content = item.replyContent.trim();
  if (!content) return;

  await apiAddWantedComment({
    targetId: item.id,
    userId,
    parentId,
    content,
  });

  ElMessage.success("回复成功");
  closeReply(item);
  await loadCommentTree(item);
}

async function deleteComment(item: WantedItem, commentId: number) {
  const userId = requireLogin("请先登录");
  if (!userId) return;

  await runConfirmAction({
    title: "删除评论",
    message: "确认删除这条评论吗？",
    type: "warning",
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    successMessage: "删除成功",
    errorMessage: "删除失败",
    action: () => apiDeleteWantedComment(commentId, userId),
    onSuccess: () => loadCommentTree(item),
  });
}

function onRootPageChange(item: WantedItem, page: number) {
  item.commentQuery.page = page;
  item.rootPageList = getRootPageList(item);
}

function handleFilterChange() {
  search(loadData);
}

function handlePageChange(page: number) {
  changePage(page, loadData);
}

onMounted(loadData);
</script>

<style scoped>
.panel {
  background: transparent;
}

.filter-bar {
  margin-bottom: 14px;
  padding: 16px 18px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 22px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.07), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 250, 255, 0.98) 100%);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 1fr);
  gap: 14px;
  align-items: center;
}

.filter-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.filter-copy h2 {
  margin: 0;
  color: #0f172a;
  font-size: 26px;
  line-height: 1.16;
  font-weight: 800;
}

.filter-copy p {
  max-width: 560px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.62;
}

.filter-tools-shell {
  display: flex;
  justify-content: flex-end;
  min-width: 0;
}

.filter-tools {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  min-width: max-content;
  flex-wrap: nowrap;
  max-width: 100%;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.filter-tools::-webkit-scrollbar {
  display: none;
}

.filter-control {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 46px;
  padding: 0 14px 0 16px;
  border-radius: 17px;
  border: 1px solid rgba(219, 234, 254, 0.96);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(246, 250, 255, 0.98) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    0 8px 16px rgba(148, 163, 184, 0.08);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  flex: 0 0 auto;
}

.filter-control:focus-within {
  border-color: rgba(96, 165, 250, 0.82);
  box-shadow:
    0 0 0 3px rgba(59, 130, 246, 0.08),
    0 10px 20px rgba(59, 130, 246, 0.12);
}

.control-label,
.stat-label {
  color: #7b8aa3;
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
  line-height: 1;
}

.status-select {
  width: 150px;
}

.filter-control :deep(.el-select__wrapper) {
  min-height: 44px;
  height: 44px;
  padding: 0 22px 0 2px;
  border-radius: 14px;
  background: transparent;
  box-shadow: none !important;
}

.filter-control :deep(.el-select__selection) {
  min-height: 44px;
}

.filter-control :deep(.el-select__selected-item),
.filter-control :deep(.el-select__input),
.filter-control :deep(.el-select__placeholder) {
  font-size: 14px;
}

.filter-control :deep(.el-select__selected-item),
.filter-control :deep(.el-select__input) {
  color: #0f172a;
  font-weight: 700;
}

.filter-control :deep(.el-select__placeholder),
.filter-control :deep(.el-select__caret) {
  color: #94a3b8;
}

.filter-stat {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 46px;
  padding: 0 14px;
  border-radius: 17px;
  border: 1px solid rgba(219, 234, 254, 0.92);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 250, 255, 0.98) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 8px 16px rgba(148, 163, 184, 0.07);
  color: #475569;
  white-space: nowrap;
  flex: 0 0 auto;
}

.filter-stat.soft {
  border-color: rgba(220, 252, 231, 0.94);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(243, 252, 246, 0.98) 100%);
}

.stat-value {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
}

.is-status .stat-value {
  color: #2563eb;
}

.filter-stat.soft .stat-value {
  color: #16a34a;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card {
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(59, 130, 246, 0.08);
}

.card :deep(.el-card__body) {
  padding: 16px;
}

.wanted-card {
  display: flex;
  flex-direction: column;
}

.wanted-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 184px;
  gap: 16px;
  align-items: start;
}

.wanted-content {
  min-width: 0;
}

.wanted-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.wanted-author {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.wanted-author-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ph-avatar,
.comment-avatar {
  overflow: hidden;
  border: 1px solid rgba(191, 219, 254, 0.88);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(34, 197, 94, 0.12));
  color: #1f2937;
  font-weight: 800;
}

.user-link {
  cursor: pointer;
}

.avatar-fallback-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.wanted-user,
.comment-name {
  padding: 0;
  border: none;
  background: transparent;
  font: inherit;
  text-align: left;
}

.wanted-user {
  color: #0f172a;
  font-size: 15px;
  line-height: 1.35;
  font-weight: 700;
}

.wanted-user.user-link:hover,
.comment-name.clickable:hover {
  color: #2563eb;
}

.wanted-time,
.comment-time {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.wanted-title {
  margin: 12px 0 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.34;
  font-weight: 800;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wanted-desc {
  margin: 8px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.72;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.status {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status.todo {
  color: #b45309;
  background: rgba(251, 191, 36, 0.14);
}

.status.done {
  color: #15803d;
  background: rgba(34, 197, 94, 0.14);
}

.wanted-media {
  width: 184px;
}

.img-box {
  width: 100%;
  height: 132px;
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(180deg, #f6f9fc 0%, #eef4fb 100%);
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.82);
}

.img-real {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.img-ph {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  text-align: center;
  color: #64748b;
}

.img-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.82);
  color: #475569;
  font-size: 10px;
  font-weight: 700;
}

.img-ph strong {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.img-ph p {
  margin: 0;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.45;
}

.wanted-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(226, 232, 240, 0.72);
}

.wanted-meta,
.wanted-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(248, 251, 255, 0.98);
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.toggle-btn {
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(191, 219, 254, 0.92);
  background: rgba(239, 246, 255, 0.9);
  color: #2563eb;
  font-weight: 700;
  box-shadow: none;
}

.toggle-btn.is-open {
  border-color: rgba(147, 197, 253, 0.96);
  background: rgba(234, 244, 255, 0.96);
}

.comments {
  margin-top: 12px;
  padding: 15px 16px 16px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.98) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.comment-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.comment-copy {
  min-width: 0;
}

.comment-title {
  margin: 0;
  color: #0f172a;
  font-size: 21px;
  font-weight: 800;
  line-height: 1.32;
}

.comment-subtitle {
  margin: 6px 0 0;
  color: #7b8aa3;
  font-size: 13px;
  line-height: 1.64;
}

.comment-editor {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(255, 255, 255, 0.95);
}

.comment-editor :deep(.el-textarea__inner),
.reply-editor :deep(.el-textarea__inner) {
  min-height: 96px !important;
  padding: 13px 14px;
  border-radius: 15px;
  border: 1px solid rgba(214, 223, 235, 0.9);
  background: #fcfdff;
  box-shadow: none;
  line-height: 1.72;
}

.reply-editor :deep(.el-textarea__inner) {
  min-height: 88px !important;
}

.comment-editor-footer,
.reply-editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.comment-count {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1;
}

.comment-submit-btn {
  min-width: 100px;
  height: 36px;
  padding: 0 16px;
  border-radius: 12px;
  box-shadow: none;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-card {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(255, 255, 255, 0.98);
}

.comment-main,
.reply-card {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.comment-avatar {
  flex: 0 0 auto;
}

.comment-avatar.clickable,
.comment-name.clickable {
  cursor: pointer;
}

.comment-body,
.reply-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.comment-name {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.4;
  font-weight: 700;
  transition: color 0.2s ease;
}

.comment-text {
  margin-top: 7px;
  color: #334155;
  white-space: pre-wrap;
  line-height: 1.72;
  font-size: 14px;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 9px;
}

.comment-action {
  padding: 0;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  transition: color 0.2s ease;
}

.comment-action:hover {
  color: #2563eb;
}

.comment-action.danger,
.comment-action.danger:hover {
  color: #dc2626;
}

.reply-editor {
  margin-top: 11px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(248, 251, 255, 0.94);
}

.reply-editor-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 11px;
}

.reply-card {
  padding: 11px 12px;
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.84);
  background: rgba(250, 252, 255, 0.96);
}

.reply-avatar {
  border-color: rgba(214, 223, 235, 0.94);
}

.reply-name {
  font-size: 13px;
}

.reply-text {
  margin-top: 6px;
}

.reply-actions {
  margin-top: 8px;
}

.comment-empty {
  min-height: 156px;
  border-radius: 16px;
  border: 1px dashed rgba(203, 213, 225, 0.92);
  background: rgba(248, 251, 255, 0.74);
  display: flex;
  align-items: center;
  justify-content: center;
}

.comment-empty :deep(.el-empty) {
  padding: 0;
}

.comment-empty :deep(.el-empty__image) {
  margin-bottom: 8px;
}

.comment-empty :deep(.el-empty__description p) {
  color: #94a3b8;
}

.comment-pager,
.page-pager {
  display: flex;
  justify-content: flex-end;
}

.comment-pager {
  margin-top: 2px;
}

.page-pager {
  margin-top: 14px;
  padding: 12px 16px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.comment-expand-enter-active,
.comment-expand-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.comment-expand-enter-from,
.comment-expand-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 920px) {
  .filter-bar {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .filter-tools-shell,
  .filter-tools {
    justify-content: flex-start;
  }

  .wanted-main {
    grid-template-columns: minmax(0, 1fr) 168px;
  }

  .wanted-media {
    width: 168px;
  }

  .img-box {
    height: 122px;
  }
}

@media (max-width: 720px) {
  .panel {
    padding-bottom: 20px;
  }

  .filter-bar,
  .card :deep(.el-card__body),
  .comments {
    padding: 14px;
  }

  .filter-copy h2 {
    font-size: 24px;
  }

  .wanted-main {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .wanted-media {
    width: 100%;
  }

  .img-box {
    height: 156px;
  }

  .wanted-title {
    -webkit-line-clamp: 2;
  }

  .wanted-footer,
  .comment-editor-footer,
  .reply-editor-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .wanted-actions,
  .reply-editor-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .toggle-btn,
  .comment-submit-btn {
    width: 100%;
  }

  .reply-editor-actions {
    flex-wrap: wrap;
  }

  .reply-editor-actions :deep(.el-button) {
    flex: 1 1 0;
    min-width: 0;
  }
}

@media (max-width: 520px) {
  .filter-tools-shell,
  .filter-tools {
    width: 100%;
    justify-content: flex-start;
  }

  .filter-control,
  .filter-stat {
    flex: 0 0 auto;
  }

  .status-select {
    width: 128px;
  }

  .wanted-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .comment-main,
  .reply-card {
    gap: 9px;
  }

  .comment-actions {
    gap: 14px;
  }

  .page-pager,
  .comment-pager {
    justify-content: center;
  }
}
</style>
