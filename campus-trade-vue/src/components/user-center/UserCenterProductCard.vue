<template>
  <article class="product-card" :class="[`is-${variant}`, { 'has-actions': !!$slots.actions }]">
    <button type="button" class="cover-wrap" @click="emit('click')">
      <UserCenterStatusTag class="status-tag" :text="statusText" :tone="statusTone" size="sm" />
      <el-image v-if="coverUrl" class="cover-image" :src="coverUrl" fit="cover">
        <template #error>
          <div class="cover-placeholder">{{ placeholderText }}</div>
        </template>
      </el-image>
      <div v-else class="cover-placeholder">{{ placeholderText }}</div>
    </button>

    <div class="body">
      <button type="button" class="title" :title="displayTitle" @click="emit('click')">
        {{ displayTitle }}
      </button>
      <div class="price">{{ formatPrice(price) }}</div>
      <div v-if="metaItems.length && variant === 'manage'" class="meta-list">
        <span v-for="item in metaItems" :key="item" class="meta-item">{{ item }}</span>
      </div>
      <div v-if="$slots.default" class="body-extra">
        <slot />
      </div>
    </div>

    <div v-if="$slots.actions" class="actions">
      <slot name="actions" />
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { StatusTone } from "@/utils/tradeStatus";
import UserCenterStatusTag from "@/components/user-center/UserCenterStatusTag.vue";
import { formatPrice } from "@/utils/price";

const props = withDefaults(
  defineProps<{
    variant?: "overview" | "manage";
    title?: string;
    price: string | number;
    coverUrl?: string;
    statusText: string;
    statusTone?: StatusTone;
    metaItems?: string[];
    placeholderText?: string;
  }>(),
  {
    variant: "manage",
    coverUrl: "",
    statusTone: "info",
    metaItems: () => [],
    placeholderText: "暂无图片",
  }
);

const emit = defineEmits<{
  (e: "click"): void;
}>();

const displayTitle = computed(() => props.title?.trim() || "未命名商品");
</script>

<style scoped>
.product-card {
  display: flex;
  flex-direction: column;
  border: 1px solid #e8eef6;
  border-radius: 20px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.product-card:hover {
  transform: translateY(-3px);
  border-color: #dbeafe;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.cover-wrap {
  position: relative;
  width: 100%;
  border: none;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  padding: 0;
  cursor: pointer;
  text-align: left;
}

.is-overview .cover-wrap {
  aspect-ratio: 1 / 1;
}

.is-overview {
  border-radius: 18px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04);
}

.is-overview:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.06);
}

.is-manage .cover-wrap {
  aspect-ratio: 4 / 3;
}

.status-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 2;
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
  line-height: 1.4;
}

.body {
  padding: 16px;
  min-width: 0;
}

.is-overview .body {
  padding: 12px;
}

.title {
  margin: 0;
  padding: 0;
  width: 100%;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
}

.is-overview .title {
  font-size: 14px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.price {
  margin-top: 10px;
  color: #ea580c;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.05;
}

.is-overview .price {
  margin-top: 8px;
  font-size: 18px;
}

.meta-list {
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
  background: rgba(248, 251, 255, 0.95);
  border: 1px solid rgba(226, 232, 240, 0.88);
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.body-extra {
  margin-top: 10px;
}

.actions {
  margin-top: auto;
  padding: 16px;
  border-top: 1px solid #eef3f8;
}

@media (max-width: 760px) {
  .product-card {
    border-radius: 18px;
  }

  .is-overview {
    border-radius: 16px;
  }

  .body,
  .actions {
    padding: 14px;
  }

  .is-overview .body {
    padding: 12px;
  }
}
</style>
