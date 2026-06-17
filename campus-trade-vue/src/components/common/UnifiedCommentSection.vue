<template>
  <section class="comment-section" v-loading="loading">
    <div class="comment-head">
      <div class="comment-copy">
        <span class="comment-eyebrow">{{ eyebrow }}</span>
        <div class="comment-title">{{ title }}</div>
        <p>{{ description }}</p>
      </div>
      <div class="comment-count">{{ totalCount }} 条评论</div>
    </div>

    <div class="editor-card">
      <div class="editor-head">
        <div>
          <div class="editor-title">{{ editorTitle }}</div>
          <p>{{ editorDescription }}</p>
        </div>
      </div>

      <el-input
        :model-value="newComment"
        type="textarea"
        :rows="editorRows"
        :maxlength="editorMaxlength"
        show-word-limit
        :placeholder="editorPlaceholder"
        @update:model-value="updateNewComment"
      />

      <div class="editor-actions">
        <span class="comment-hint">{{ editorHint }}</span>
        <el-button class="submit-btn" type="primary" :disabled="!canSubmitNew" @click="$emit('submit-new')">
          {{ submitText }}
        </el-button>
      </div>
    </div>

    <div class="comment-divider"></div>

    <div v-if="comments.length" class="comment-list">
      <article v-for="comment in comments" :key="getCommentId(comment)" class="comment-item">
        <div class="comment-main">
          <el-avatar
            :size="40"
            class="comment-avatar"
            :class="{ clickable: getCommentUserId(comment) > 0 }"
            :src="getCommentUserAvatar(comment)"
            @click.stop="goUserProfile(comment)"
          >
            <img :src="DEFAULT_COMMENT_AVATAR" alt="" class="avatar-fallback-img" />
          </el-avatar>

          <div class="comment-body">
            <div class="comment-meta">
              <span
                class="comment-name"
                :class="{ clickable: getCommentUserId(comment) > 0 }"
                @click.stop="goUserProfile(comment)"
              >
                {{ getCommentUserName(comment) }}
              </span>
              <span class="comment-time">{{ formatCommentTime(comment.createdAt) }}</span>
            </div>

            <div class="comment-text">{{ comment.content }}</div>

            <div class="comment-actions">
              <span class="action-btn" @click="emitOpenReply(comment)">回复</span>
              <span
                v-if="canDelete(comment)"
                class="action-btn danger"
                @click="$emit('delete-comment', getCommentId(comment))"
              >
                删除
              </span>
            </div>

            <div v-if="replyParentId === getCommentId(comment)" class="reply-editor">
              <el-input
                :model-value="replyContent"
                :maxlength="replyMaxlength"
                show-word-limit
                :placeholder="replyPlaceholder"
                @update:model-value="updateReplyContent"
              />
              <div class="reply-actions">
                <el-button size="small" type="primary" :disabled="!canSubmitReply" @click="$emit('submit-reply', getCommentId(comment))">
                  {{ replySubmitText }}
                </el-button>
                <el-button size="small" @click="$emit('close-reply')">{{ cancelText }}</el-button>
              </div>
            </div>

            <div v-if="getCommentChildren(comment).length" class="children">
              <div v-for="child in getCommentChildren(comment)" :key="getCommentId(child)" class="child-item">
                <el-avatar
                  :size="36"
                  class="child-avatar"
                  :class="{ clickable: getCommentUserId(child) > 0 }"
                  :src="getCommentUserAvatar(child)"
                  @click.stop="goUserProfile(child)"
                >
                  <img :src="DEFAULT_COMMENT_AVATAR" alt="" class="avatar-fallback-img" />
                </el-avatar>
                <div class="child-body">
                  <div class="child-meta">
                    <span
                      class="child-name"
                      :class="{ clickable: getCommentUserId(child) > 0 }"
                      @click.stop="goUserProfile(child)"
                    >
                      {{ getCommentUserName(child) }}
                    </span>
                    <span class="child-time">{{ formatCommentTime(child.createdAt) }}</span>
                    <span
                      v-if="canDelete(child)"
                      class="child-del"
                      @click="$emit('delete-comment', getCommentId(child))"
                    >
                      删除
                    </span>
                  </div>
                  <div class="child-text">{{ child.content }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </article>

      <div v-if="$slots.footer" class="comment-footer">
        <slot name="footer" />
      </div>
    </div>

    <div v-else class="comment-empty">
      <div class="comment-empty-box">
        <el-empty :description="emptyText" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import {
  DEFAULT_COMMENT_AVATAR,
  formatCommentTime,
  getCommentUserAvatar,
  getCommentUserName,
  normalizePositiveNumber,
} from "@/utils/commentDisplay";
import { goToUserHome } from "@/utils/userHome";

type CommentRecord = {
  id?: number | string | null;
  userId?: number | null;
  nickname?: string | null;
  nickName?: string | null;
  name?: string | null;
  username?: string | null;
  userName?: string | null;
  avatar?: string | null;
  avatarUrl?: string | null;
  avatar_url?: string | null;
  content?: string | null;
  createdAt?: string | null;
  canDelete?: boolean;
  mine?: boolean;
  children?: CommentRecord[];
  replyList?: CommentRecord[];
};

const props = withDefaults(defineProps<{
  comments: CommentRecord[];
  totalCount: number;
  loading?: boolean;
  newComment: string;
  replyContent: string;
  replyParentId: number | null;
  canSubmitNew: boolean;
  canSubmitReply: boolean;
  eyebrow?: string;
  title?: string;
  description?: string;
  editorTitle?: string;
  editorDescription?: string;
  editorHint?: string;
  submitText?: string;
  replySubmitText?: string;
  cancelText?: string;
  editorPlaceholder?: string;
  replyPlaceholder?: string;
  emptyText?: string;
  editorRows?: number;
  editorMaxlength?: number;
  replyMaxlength?: number;
}>(), {
  loading: false,
  eyebrow: "评论互动",
  title: "评论区",
  description: "欢迎留言交流，补充细节与观点，帮助彼此更快获得有效反馈。",
  editorTitle: "评论互动",
  editorDescription: "欢迎文明交流，理性分享你的想法。",
  editorHint: "文明交流，理性沟通",
  submitText: "发表评论",
  replySubmitText: "发布回复",
  cancelText: "取消",
  editorPlaceholder: "请输入评论内容...",
  replyPlaceholder: "请输入回复内容...",
  emptyText: "暂无评论",
  editorRows: 3,
  editorMaxlength: 500,
  replyMaxlength: 500,
});

const emit = defineEmits<{
  "update:newComment": [value: string];
  "update:replyContent": [value: string];
  "submit-new": [];
  "open-reply": [payload: { id: number; nickname: string }];
  "close-reply": [];
  "submit-reply": [parentId: number];
  "delete-comment": [commentId: number];
}>();

const router = useRouter();

function updateNewComment(value: string | number | undefined) {
  emit("update:newComment", typeof value === "string" ? value : String(value ?? ""));
}

function updateReplyContent(value: string | number | undefined) {
  emit("update:replyContent", typeof value === "string" ? value : String(value ?? ""));
}

function getCommentId(comment: CommentRecord) {
  return normalizePositiveNumber(comment?.id);
}

function getCommentUserId(comment: CommentRecord) {
  return normalizePositiveNumber(comment?.userId);
}

function getCommentChildren(comment: CommentRecord) {
  return Array.isArray(comment?.children)
    ? comment.children
    : Array.isArray(comment?.replyList)
      ? comment.replyList
      : [];
}

function canDelete(comment: CommentRecord) {
  return !!(comment?.canDelete || comment?.mine);
}

function goUserProfile(comment: CommentRecord) {
  const userId = getCommentUserId(comment);
  if (!userId) return;
  void goToUserHome(router, userId);
}

function emitOpenReply(comment: CommentRecord) {
  emit("open-reply", {
    id: getCommentId(comment),
    nickname: getCommentUserName(comment),
  });
}
</script>

<style scoped>
.comment-section {
  min-width: 0;
}

.comment-head,
.editor-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.comment-copy {
  min-width: 0;
}

.comment-eyebrow {
  display: inline-flex;
  align-items: center;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.comment-title {
  margin-top: 6px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
}

.comment-copy p,
.editor-head p {
  margin: 6px 0 0;
  color: #7b8aa3;
  font-size: 13px;
  line-height: 1.7;
}

.comment-count {
  display: inline-flex;
  align-items: center;
  height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(59, 130, 246, .08);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.editor-card {
  margin-top: 18px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(226, 232, 240, .86);
  background: linear-gradient(180deg, rgba(255, 255, 255, .99) 0%, rgba(248, 251, 255, .98) 100%);
}

.editor-title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

.editor-card :deep(.el-textarea__inner) {
  min-height: 112px !important;
  border-radius: 18px;
  background: #fbfdff;
  box-shadow: inset 0 0 0 1px rgba(209, 219, 234, .9);
}

.editor-card :deep(.el-input__wrapper) {
  border-radius: 16px;
}

.editor-actions,
.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.comment-hint {
  margin-right: auto;
  color: #94a3b8;
  font-size: 12px;
}

.submit-btn {
  min-width: 108px;
  height: 40px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 60%, #22c55e 100%);
  box-shadow: 0 14px 24px rgba(59, 130, 246, .18);
}

.comment-divider {
  height: 1px;
  margin: 18px 0;
  background: rgba(226, 232, 240, .9);
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-item {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, .84);
  background: #fff;
  box-shadow: 0 10px 22px rgba(15, 23, 42, .04);
}

.comment-main {
  display: flex;
  gap: 12px;
}

.comment-avatar,
.child-avatar {
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid rgba(191, 219, 254, .86);
  background: linear-gradient(135deg, rgba(59, 130, 246, .12), rgba(34, 197, 94, .14));
  color: #1f2937;
  font-weight: 800;
}

.comment-avatar.clickable,
.child-avatar.clickable,
.comment-name.clickable,
.child-name.clickable {
  cursor: pointer;
}

.avatar-fallback-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.comment-body,
.child-body {
  flex: 1;
  min-width: 0;
}

.comment-meta,
.child-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.comment-name,
.child-name {
  color: #0f172a;
  font-weight: 800;
  transition: color 0.2s ease;
}

.comment-name.clickable:hover,
.child-name.clickable:hover {
  color: #2563eb;
}

.comment-time,
.child-time {
  color: #94a3b8;
  font-size: 12px;
}

.comment-text,
.child-text {
  margin-top: 8px;
  color: #334155;
  white-space: pre-wrap;
  line-height: 1.8;
}

.comment-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  font-size: 12px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(248, 251, 255, .96);
  color: #64748b;
  cursor: pointer;
  user-select: none;
}

.action-btn.danger,
.child-del {
  color: #d93026;
}

.reply-editor {
  margin-top: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(248, 251, 255, .96);
  border: 1px solid rgba(226, 232, 240, .84);
}

.children {
  margin-top: 12px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(248, 251, 255, .92);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.child-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, .82);
}

.child-avatar {
  width: 36px;
  height: 36px;
}

.child-del {
  margin-left: auto;
  cursor: pointer;
}

.comment-footer {
  margin-top: 4px;
}

.comment-empty-box {
  border-radius: 18px;
  border: 1px dashed rgba(203, 213, 225, .9);
  background: rgba(248, 251, 255, .84);
  padding: 12px 0;
}

@media (max-width: 640px) {
  .comment-head,
  .editor-head,
  .editor-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .comment-hint {
    margin-right: 0;
  }

  .submit-btn {
    width: 100%;
  }

  .comment-main,
  .child-item {
    gap: 10px;
  }
}
</style>

