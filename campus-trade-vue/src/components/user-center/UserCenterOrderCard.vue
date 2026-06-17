<template>
  <article class="order-card" :class="{ 'has-countdown': !!countdownText }">
    <button type="button" class="media" @click="emit('click')">
      <el-image v-if="coverUrl" class="cover-image" :src="coverUrl" fit="cover">
        <template #error>
          <div class="cover-placeholder">{{ placeholderText }}</div>
        </template>
      </el-image>
      <div v-else class="cover-placeholder">{{ placeholderText }}</div>
    </button>

    <div class="content">
      <button type="button" class="title" :title="displayTitle" @click="emit('click')">
        {{ displayTitle }}
      </button>

      <div v-if="infoItems.length" class="info-list">
        <div
          v-for="item in infoItems"
          :key="`${item.label}-${item.value}`"
          class="info-row"
          :class="{ 'is-muted': item.muted }"
        >
          <span class="info-label">{{ item.label }}</span>
          <span class="info-value" :class="{ 'is-strong': item.strong, 'is-ellipsis': item.ellipsis }">
            {{ displayValue(item.value) }}
          </span>
        </div>
      </div>

      <div v-if="timelineItems.length" class="timeline-list">
        <div
          v-for="item in timelineItems"
          :key="`${item.label}-${item.value}`"
          class="timeline-item"
          :class="{ 'is-muted': item.muted }"
        >
          <span class="timeline-label">{{ item.label }}</span>
          <span class="timeline-value">{{ displayValue(item.value) }}</span>
        </div>
      </div>
    </div>

    <aside class="aside">
      <UserCenterStatusTag :text="statusText" :tone="statusTone" />
      <div class="amount-block">
        <span class="amount-label">{{ amountLabel }}</span>
        <strong class="amount-value">{{ formatPrice(amount) }}</strong>
      </div>
      <div v-if="countdownText" class="countdown-note" :class="`tone-${countdownTone}`">
        {{ countdownText }}
      </div>
      <div class="actions">
        <slot name="actions" />
      </div>
    </aside>
  </article>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { StatusTone } from "@/utils/tradeStatus";
import UserCenterStatusTag from "@/components/user-center/UserCenterStatusTag.vue";
import { formatPrice } from "@/utils/price";

type DetailItem = {
  label: string;
  value: string | number;
  strong?: boolean;
  muted?: boolean;
  ellipsis?: boolean;
};

const props = withDefaults(
  defineProps<{
    title?: string;
    coverUrl?: string;
    statusText: string;
    statusTone?: StatusTone;
    amount: string | number;
    amountLabel?: string;
    infoItems?: DetailItem[];
    timelineItems?: DetailItem[];
    countdownText?: string;
    countdownTone?: StatusTone;
    placeholderText?: string;
  }>(),
  {
    title: "",
    coverUrl: "",
    statusTone: "info",
    amountLabel: "金额",
    infoItems: () => [],
    timelineItems: () => [],
    countdownText: "",
    countdownTone: "warning",
    placeholderText: "暂无图片",
  }
);

const emit = defineEmits<{
  (e: "click"): void;
}>();

const displayTitle = computed(() => props.title?.trim() || "未命名商品");

function displayValue(value: unknown) {
  const text = String(value ?? "").trim();
  return text || "-";
}
</script>

<style scoped>
.order-card {
  display: grid;
  grid-template-columns: 156px minmax(0, 1fr) 220px;
  gap: 18px;
  padding: 18px;
  border: 1px solid #e8eef6;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.order-card:hover {
  transform: translateY(-2px);
  border-color: #dbeafe;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.media {
  position: relative;
  width: 156px;
  height: 118px;
  border: 1px solid #edf2f7;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  padding: 0;
  cursor: pointer;
}

.cover-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

:deep(.cover-image .el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}

.content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.title {
  margin: 0;
  padding: 0;
  width: 100%;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
}

.info-list,
.timeline-list {
  display: grid;
  gap: 8px;
}

.info-row,
.timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.info-label,
.timeline-label {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.55;
}

.info-value,
.timeline-value {
  min-width: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
}

.info-row.is-muted .info-value,
.timeline-item.is-muted .timeline-value {
  color: #94a3b8;
}

.info-value.is-strong {
  color: #334155;
  font-weight: 600;
}

.info-value.is-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-list {
  margin-top: auto;
}

.aside {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 12px;
}

.amount-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.amount-label {
  color: #94a3b8;
  font-size: 12px;
}

.amount-value {
  color: #ea580c;
  font-size: 30px;
  line-height: 1.05;
  font-weight: 800;
}

.countdown-note {
  width: 100%;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
}

.tone-warning {
  color: #b45309;
  background: rgba(255, 247, 237, 0.92);
  border-color: rgba(251, 191, 36, 0.26);
}

.tone-danger {
  color: #dc2626;
  background: rgba(254, 242, 242, 0.92);
  border-color: rgba(248, 113, 113, 0.22);
}

.actions {
  width: 100%;
  display: grid;
  gap: 8px;
}

@media (max-width: 1080px) {
  .order-card {
    grid-template-columns: 148px minmax(0, 1fr);
  }

  .aside {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    align-items: start;
  }

  .actions {
    align-self: stretch;
  }
}

@media (max-width: 760px) {
  .order-card {
    grid-template-columns: 1fr;
    padding: 16px;
    border-radius: 18px;
  }

  .media {
    width: 100%;
    height: 188px;
  }

  .aside {
    grid-template-columns: 1fr;
  }

  .amount-value {
    font-size: 28px;
  }
}
</style>
