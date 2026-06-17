<template>
  <div class="layout">
    <aside class="left">
      <div class="left-header">聊天联系人</div>

      <div class="contacts">
        <div v-if="loadingContacts" class="placeholder">加载中...</div>
        <template v-else>
          <div
            v-for="contact in contacts"
            :key="contact.sessionId"
            class="contact-item"
            :class="{ active: contact.sessionId === selectedSessionId }"
            @click="selectSession(contact.sessionId)"
          >
            <button class="contact-delete" type="button" title="删除聊天" @click.stop="removeConversation(contact)">
              ×
            </button>
            <el-avatar :size="38">{{ contact.name.slice(0, 1) }}</el-avatar>
            <div class="contact-main">
              <div class="contact-name">{{ contact.name }}</div>
              <div class="contact-hint">{{ contact.lastMessage || "暂无消息" }}</div>
            </div>
            <div v-if="contact.unreadCount > 0" class="badge">{{ contact.unreadCount }}</div>
          </div>
          <div v-if="!contacts.length" class="placeholder">暂无聊天记录</div>
        </template>
      </div>
    </aside>

    <main class="right">
      <div class="chat-header">
        <div class="chat-title">{{ activeContactName }}</div>
        <div class="chat-sub">
          {{ selectedSessionId ? "可直接沟通商品、改价和交易细节" : "请选择联系人开始聊天" }}
        </div>
      </div>

      <div v-if="chatProduct" class="product-card">
        <div class="product-click-area" role="button" tabindex="0" @click="goChatProductDetail" @keydown.enter.prevent="goChatProductDetail">
          <img v-if="chatProduct.coverUrl" class="product-cover" :src="toImg(chatProduct.coverUrl)" alt="" />
          <div class="product-info">
            <div class="product-label">当前聊天商品</div>
            <div class="product-title text-ellipsis-1" :title="chatProduct.title || undefined">{{ chatProduct.title }}</div>
            <div class="product-meta">
              <span class="product-price">{{ formatPrice(chatProduct.price) }}</span>
              <span :class="['product-status', isChatProductTradable ? 'sale-on' : 'sale-off']">
                {{ chatProductStatusText }}
              </span>
            </div>
          </div>
        </div>
        <div class="product-actions">
          <el-button v-if="isSellerOfProduct" plain :disabled="!canEditChatProductPrice" @click.stop="changePrice">
            修改价格
          </el-button>
          <el-button
            v-else
            type="danger"
            :disabled="!canBuyProduct"
            :loading="buying"
            @click.stop="buyFromChat"
          >
            {{ chatBuyButtonText }}
          </el-button>
        </div>
      </div>

      <div ref="messagesRef" class="messages" v-loading="loadingMessages">
        <div v-if="!loadingMessages && !messages.length" class="placeholder">暂无消息，发一条试试</div>
        <div
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.fromId === currentUserId ? 'mine' : 'theirs'"
        >
          <div class="bubble">{{ message.content }}</div>
          <div class="message-time">{{ formatTime(message.createdAt) }}</div>
        </div>
      </div>

      <div class="composer">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          :disabled="!selectedSessionId"
          placeholder="输入消息，按回车发送，按上档键配合回车换行"
          @keydown.enter.exact.prevent="send"
        />
        <div class="composer-actions">
          <el-button type="primary" :loading="sending" :disabled="!selectedSessionId || !draft.trim()" @click="send">
            发送消息
          </el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  apiCreateOrGetProductSession,
  apiDeleteConversation,
  apiGetChatHistory,
  apiGetContacts,
  apiMarkAsRead,
  apiSendMessage,
  apiUpdateChatProductPrice,
} from "@/api/chat";
import { apiGetMyAddressList } from "@/api/user";
import { apiCreateOrder } from "@/api/order";
import { getStoredUser } from "@/utils/auth";
import { productImgUrl as resolveProductImgUrl } from "@/utils/img";
import { formatPrice, formatPriceInput } from "@/utils/price";

type Contact = {
  id: number;
  sessionId: number;
  otherUserId: number;
  name: string;
  avatar?: string;
  unreadCount: number;
  lastMessage?: string;
  lastTime?: string;
  productId?: number;
  productCard?: ChatProduct | null;
};

type ChatMessage = {
  id: number;
  fromId: number;
  toId: number;
  content: string;
  createdAt: string;
};

type ChatProduct = {
  id: number;
  productId: number;
  title: string;
  price: number | string;
  status: string;
  auditStatus?: string;
  coverUrl?: string;
  sellerId: number;
  sellerName?: string;
  productStatus?: string;
  canBuy?: boolean;
  buyDisabledReason?: string;
  isSeller?: boolean;
  isOwnProduct?: boolean;
};

const route = useRoute();
const router = useRouter();

const contacts = ref<Contact[]>([]);
const messages = ref<ChatMessage[]>([]);
const selectedSessionId = ref<number | null>(null);
const loadingContacts = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const buying = ref(false);
const draft = ref("");
const messagesRef = ref<HTMLElement | null>(null);
const chatProduct = ref<ChatProduct | null>(null);

function getLoginUser() {
  return getStoredUser();
}

const currentUser = computed(() => getLoginUser());
const currentUserId = computed<number | null>(() => {
  const id = Number(currentUser.value?.id);
  return Number.isFinite(id) && id > 0 ? id : null;
});

const routeSessionId = computed<number | null>(() => {
  const raw = route.query.sessionId;
  const value = Array.isArray(raw) ? raw[0] : raw;
  if (!value) return null;

  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});

const routeProductId = computed<number | null>(() => {
  const raw = route.query.productId;
  const value = Array.isArray(raw) ? raw[0] : raw;
  if (!value) return null;

  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});

const activeContactName = computed(() => {
  if (!selectedSessionId.value) return "聊天消息";
  return contacts.value.find((item) => item.sessionId === selectedSessionId.value)?.name || "聊天消息";
});

const isSellerOfProduct = computed(() => {
  if (!chatProduct.value) return false;
  if (chatProduct.value.isSeller === true || chatProduct.value.isOwnProduct === true) return true;
  return !!currentUserId.value && currentUserId.value === chatProduct.value.sellerId;
});

const isChatProductTradable = computed(() => {
  return !!chatProduct.value && chatProduct.value.canBuy === true;
});

const canBuyProduct = computed(() => {
  return !!chatProduct.value && chatProduct.value.canBuy === true;
});

const canEditChatProductPrice = computed(() => {
  return !!chatProduct.value && isSellerOfProduct.value;
});

const chatProductStatusText = computed(() => {
  if (!chatProduct.value) return "";
  if (chatProduct.value.canBuy) return "可交易";
  return chatProduct.value.buyDisabledReason || "暂不可购买";
});

const chatBuyButtonText = computed(() => {
  return canBuyProduct.value ? "立即购买" : chatProductStatusText.value;
});

function toImg(url?: string) {
  return resolveProductImgUrl(url || "");
}

function formatTime(value?: string) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}

function normalizeChatProduct(data: any): ChatProduct | null {
  const id = Number(data?.productId || data?.id);
  if (!Number.isFinite(id) || id <= 0) return null;

  const title = String(data?.productTitle || data?.title || "").trim();
  if (!title) return null;

  const sellerId = Number(data?.sellerId || 0);
  return {
    id,
    productId: id,
    title,
    price: data?.productPrice ?? data?.price ?? "",
    status: String(data?.status || data?.productStatus || "OFF"),
    auditStatus: String(data?.auditStatus || ""),
    coverUrl: String(data?.productCover || data?.coverUrl || ""),
    sellerId: Number.isFinite(sellerId) && sellerId > 0 ? sellerId : 0,
    sellerName: String(data?.sellerName || ""),
    productStatus: String(data?.productStatus || data?.status || ""),
    canBuy: data?.canBuy === true,
    buyDisabledReason: String(data?.buyDisabledReason || ""),
    isSeller: data?.isSeller === true || data?.isOwnProduct === true,
    isOwnProduct: data?.isOwnProduct === true || data?.isSeller === true,
  };
}

function normalizeContact(item: any): Contact | null {
  const sessionId = Number(item?.sessionId || item?.id);
  if (!Number.isFinite(sessionId) || sessionId <= 0) return null;
  const otherUserId = Number(item?.otherUserId || 0);
  return {
    id: sessionId,
    sessionId,
    otherUserId: Number.isFinite(otherUserId) ? otherUserId : 0,
    name: String(item?.otherUserName || item?.name || `会话${sessionId}`),
    avatar: String(item?.otherUserAvatar || item?.avatar || ""),
    unreadCount: Number(item?.unreadCount || 0),
    lastMessage: typeof item?.lastMsg === "string" ? item.lastMsg : String(item?.lastMessage || ""),
    lastTime: String(item?.lastTime || ""),
    productId: Number(item?.productId || item?.productCard?.productId || item?.productCard?.id || 0) || undefined,
    productCard: normalizeChatProduct(item?.productCard || item),
  };
}

function upsertContact(raw: any) {
  const contact = normalizeContact(raw);
  if (!contact) return null;
  contacts.value = [contact, ...contacts.value.filter((item) => item.sessionId !== contact.sessionId)];
  return contact;
}

function updateSelectedProductFromContact() {
  if (!selectedSessionId.value) {
    chatProduct.value = null;
    return;
  }

  const contact = contacts.value.find((item) => item.sessionId === selectedSessionId.value);
  chatProduct.value = contact?.productCard || null;
}

async function loadContacts() {
  if (!currentUserId.value) return;

  loadingContacts.value = true;
  try {
    const data = await apiGetContacts({ userId: currentUserId.value });
    const list = Array.isArray(data) ? data : [];
    contacts.value = list.map((item: any) => normalizeContact(item)).filter((item): item is Contact => !!item);

    if (routeSessionId.value && contacts.value.some((item) => item.sessionId === routeSessionId.value)) {
      selectedSessionId.value = routeSessionId.value;
    } else if (!selectedSessionId.value && contacts.value[0]) {
      selectedSessionId.value = contacts.value[0].sessionId;
    }
  } catch (error: any) {
    ElMessage.error(error?.message || "加载联系人失败");
  } finally {
    loadingContacts.value = false;
  }
}

async function loadMessages() {
  if (!currentUserId.value || !selectedSessionId.value) {
    messages.value = [];
    return;
  }

  const sessionId = selectedSessionId.value;
  loadingMessages.value = true;
  try {
    const data: any = await apiGetChatHistory({
      sessionId,
      page: 1,
      size: 100,
    });
    if (selectedSessionId.value !== sessionId) return;

    const records = Array.isArray(data?.records) ? data.records : [];
    messages.value = records
      .map((item: any) => ({
        id: Number(item.id),
        fromId: Number(item.fromId),
        toId: Number(item.toId),
        content: String(item.content || ""),
        createdAt: String(item.createdAt || ""),
      }))
      .reverse();

    await apiMarkAsRead({ sessionId });
    if (selectedSessionId.value !== sessionId) return;

    const current = contacts.value.find((item) => item.sessionId === sessionId);
    const sessionContact = normalizeContact(data?.session);
    if (sessionContact) {
      contacts.value = contacts.value.map((item) =>
        item.sessionId === sessionContact.sessionId
          ? { ...item, ...sessionContact, unreadCount: 0 }
          : item
      );
      if (!contacts.value.some((item) => item.sessionId === sessionContact.sessionId)) {
        contacts.value.unshift({ ...sessionContact, unreadCount: 0 });
      }
      chatProduct.value = sessionContact.productCard || null;
    } else if (current) {
      current.unreadCount = 0;
      chatProduct.value = current.productCard || null;
    }

    await nextTick();
    scrollToBottom();
  } catch (error: any) {
    ElMessage.error(error?.message || "加载聊天记录失败");
  } finally {
    loadingMessages.value = false;
  }
}

function buildSessionRouteQuery(sessionId: number) {
  const contact = contacts.value.find((item) => item.sessionId === sessionId);
  const query: Record<string, string> = { sessionId: String(sessionId) };
  if (contact?.productId) query.productId = String(contact.productId);
  return query;
}

function selectSession(sessionId: number) {
  if (selectedSessionId.value === sessionId) return;
  selectedSessionId.value = sessionId;

  router.replace({
    path: "/user/chat",
    query: buildSessionRouteQuery(sessionId),
  });
}

async function send() {
  if (!currentUserId.value || !selectedSessionId.value) return;

  const content = draft.value.trim();
  if (!content) return;

  sending.value = true;
  try {
    const resp: any = await apiSendMessage({
      sessionId: selectedSessionId.value,
      content,
    });
    const activeContact = contacts.value.find((item) => item.sessionId === selectedSessionId.value);
    const toId = activeContact?.otherUserId || 0;

    messages.value.push({
      id: Number(resp?.messageId || Date.now()),
      fromId: currentUserId.value,
      toId: Number(resp?.toId || toId),
      content,
      createdAt: String(resp?.createdAt || new Date().toISOString()),
    });

    const current = contacts.value.find((item) => item.sessionId === selectedSessionId.value);
    if (current) {
      current.lastMessage = content;
      const nextContacts = contacts.value.filter((item) => item.sessionId !== selectedSessionId.value);
      contacts.value = [current, ...nextContacts];
    }

    draft.value = "";
    await nextTick();
    scrollToBottom();
  } catch (error: any) {
    ElMessage.error(error?.message || "发送消息失败");
  } finally {
    sending.value = false;
  }
}

async function removeConversation(contact: Contact) {
  try {
    await ElMessageBox.confirm("删除这条聊天记录后将无法恢复，是否继续？", "删除聊天", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      confirmButtonClass: "contact-delete-confirm",
    });

    await apiDeleteConversation(contact.sessionId);

    const remainingContacts = contacts.value.filter((item) => item.sessionId !== contact.sessionId);
    contacts.value = remainingContacts;

    if (selectedSessionId.value === contact.sessionId) {
      const nextActive = remainingContacts[0]?.sessionId ?? null;
      selectedSessionId.value = nextActive;
      messages.value = [];
      chatProduct.value = null;

      if (nextActive) {
        router.replace({
          path: "/user/chat",
          query: buildSessionRouteQuery(nextActive),
        });
      } else {
        router.replace({ path: "/user/chat" });
      }
    }

    ElMessage.success("聊天已删除");
  } catch (error: any) {
    if (error === "cancel" || error === "close") return;
    ElMessage.error(error?.message || "删除聊天失败");
  }
}

function goChatProductDetail() {
  const productId = Number(chatProduct.value?.productId || chatProduct.value?.id);
  if (!Number.isFinite(productId) || productId <= 0) return;
  router.push(`/user/hot/product/${productId}`);
}

async function changePrice() {
  if (!chatProduct.value || !isSellerOfProduct.value) return;

  try {
    const currentProduct = chatProduct.value;
    const result: any = await ElMessageBox.prompt(
      `商品：${currentProduct.title}\n当前价格：${formatPrice(currentProduct.price)}`,
      "修改价格",
      {
      confirmButtonText: "保存",
      cancelButtonText: "取消",
      inputValue: formatPriceInput(currentProduct.price),
      inputPattern: /^(?!0+(?:\.0{1,2})?$)(?:0|[1-9]\d*)(?:\.\d{1,2})?$/,
      inputErrorMessage: "请输入大于 0 的合法价格，最多两位小数",
      }
    );

    const nextPrice = String(result.value || "").trim();
    const numericPrice = Number(nextPrice);
    if (!nextPrice || !Number.isFinite(numericPrice) || numericPrice <= 0 || !/^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(nextPrice)) {
      ElMessage.warning("请输入大于 0 的合法价格，最多两位小数");
      return;
    }

    const updated: any = await apiUpdateChatProductPrice({
      productId: currentProduct.id,
      price: nextPrice,
    });
    const normalized = normalizeChatProduct(updated);
    if (normalized) {
      chatProduct.value = normalized;
      const contact = contacts.value.find((item) => item.sessionId === selectedSessionId.value);
      if (contact) {
        contact.productCard = normalized;
        contact.productId = normalized.id;
      }
    }
    await loadMessages();
    ElMessage.success("价格已更新");
  } catch (error: any) {
    if (error === "cancel" || error === "close") return;
    ElMessage.error(error?.message || "改价失败");
  }
}

async function buyFromChat() {
  if (!chatProduct.value) return;
  if (!canBuyProduct.value) return ElMessage.warning("当前商品暂不可购买");

  buying.value = true;
  try {
    const addressId = await resolveAddressId();
    if (!addressId) return;

    await apiCreateOrder(chatProduct.value.id, addressId);
    await loadMessages();
    ElMessage.success("订单已创建，请前往订单页支付");
    router.push("/user/bought");
  } catch (error: any) {
    ElMessage.error(error?.message || "购买失败");
  } finally {
    buying.value = false;
  }
}

async function resolveAddressId() {
  const list: any[] = await apiGetMyAddressList();
  const address = list.find((item) => Number(item.isDefault) === 1) || list[0];
  if (!address?.id) {
    ElMessage.warning("请先在个人中心添加收货地址");
    router.push("/user/address");
    return null;
  }
  return Number(address.id);
}

function scrollToBottom() {
  if (!messagesRef.value) return;
  messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
}

watch(selectedSessionId, () => {
  updateSelectedProductFromContact();
  loadMessages();
});

watch(
  () => route.fullPath,
  async () => {
    if (routeSessionId.value) {
      selectedSessionId.value = routeSessionId.value;
      return;
    }
    if (routeProductId.value) {
      await ensureProductSessionFromRoute();
      return;
    }
    updateSelectedProductFromContact();
  }
);

onMounted(async () => {
  if (!routeSessionId.value && routeProductId.value) {
    await ensureProductSessionFromRoute();
  }
  await loadContacts();
  updateSelectedProductFromContact();
});

async function ensureProductSessionFromRoute() {
  if (!routeProductId.value) return;
  try {
    const data: any = await apiCreateOrGetProductSession({ productId: routeProductId.value });
    const contact = upsertContact(data);
    if (!contact) return;
    selectedSessionId.value = contact.sessionId;
    chatProduct.value = contact.productCard || null;
    router.replace({ path: "/user/chat", query: buildSessionRouteQuery(contact.sessionId) });
  } catch (error: any) {
    ElMessage.error(error?.message || "创建聊天会话失败");
  }
}
</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  min-height: 620px;
}

.left,
.right {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.left {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.left-header,
.chat-header {
  padding: 16px 18px;
  border-bottom: 1px solid #eef0f3;
}

.left-header {
  font-weight: 700;
}

.contacts {
  flex: 1;
  padding: 12px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.contact-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  padding-right: 40px;
  border: 1px solid #eef0f3;
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.contact-item.active {
  border-color: #93c5fd;
  background: #eff6ff;
}

.contact-item:hover {
  border-color: #d6dbe4;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.contact-delete {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #94a3b8;
  font-size: 16px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transform: scale(0.9);
  transition: opacity 0.2s ease, transform 0.2s ease, background-color 0.2s ease, color 0.2s ease;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.contact-item:hover .contact-delete,
.contact-item.active .contact-delete {
  opacity: 1;
  transform: scale(1);
}

.contact-delete:hover {
  background: #fee2e2;
  color: #dc2626;
}

.contact-main {
  min-width: 0;
  flex: 1;
}

.contact-name {
  font-weight: 600;
}

.contact-hint {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.right {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-title {
  font-size: 16px;
  font-weight: 700;
}

.chat-sub {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.product-card {
  margin: 14px 18px 0;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.product-card:hover {
  border-color: #d1d5db;
  background: #ffffff;
}

.product-click-area {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  border-radius: 10px;
}

.product-cover {
  width: 72px;
  height: 72px;
  flex: 0 0 auto;
  border-radius: 10px;
  object-fit: cover;
  background: #eef2f7;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-label {
  color: #6b7280;
  font-size: 12px;
}

.product-title {
  margin-top: 4px;
  font-weight: 700;
}

.product-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-price {
  color: #dc2626;
  font-size: 20px;
  font-weight: 700;
}

.product-status {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.sale-on {
  color: #065f46;
  background: #d1fae5;
}

.sale-off {
  color: #991b1b;
  background: #fee2e2;
}

.product-actions {
  flex: 0 0 auto;
}

.messages {
  flex: 1;
  padding: 18px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f8fafc;
}

.message-row {
  display: flex;
  flex-direction: column;
  max-width: 72%;
}

.message-row.mine {
  align-self: flex-end;
  align-items: flex-end;
}

.message-row.theirs {
  align-self: flex-start;
  align-items: flex-start;
}

.bubble {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #fff;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.mine .bubble {
  background: #e0f2fe;
  border-color: #bae6fd;
}

.message-time {
  margin-top: 4px;
  color: #9ca3af;
  font-size: 12px;
}

.composer {
  padding: 16px 18px;
  border-top: 1px solid #eef0f3;
  background: #fff;
}

.composer-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  width: 100%;
}

.composer-actions :deep(.el-button) {
  min-width: 120px;
}

.placeholder {
  margin: auto;
  padding: 24px 0;
  text-align: center;
  color: #6b7280;
}

</style>

