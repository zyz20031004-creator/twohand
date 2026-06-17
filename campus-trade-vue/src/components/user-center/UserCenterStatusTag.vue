<template>
  <span class="status-tag" :class="[toneClass, sizeClass]">
    {{ text }}
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { StatusTone } from "@/utils/tradeStatus";

const props = withDefaults(
  defineProps<{
    text: string;
    tone?: StatusTone;
    size?: "sm" | "md";
  }>(),
  {
    tone: "info",
    size: "md",
  }
);

const toneClass = computed(() => `tone-${props.tone}`);
const sizeClass = computed(() => `size-${props.size}`);
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid transparent;
  font-weight: 700;
  white-space: nowrap;
}

.size-sm {
  min-height: 24px;
  padding: 0 10px;
  font-size: 12px;
}

.size-md {
  min-height: 28px;
  padding: 0 12px;
  font-size: 12px;
}

.tone-primary {
  color: #1d4ed8;
  background: rgba(239, 246, 255, 0.96);
  border-color: rgba(147, 197, 253, 0.34);
}

.tone-success {
  color: #15803d;
  background: rgba(240, 253, 244, 0.96);
  border-color: rgba(134, 239, 172, 0.34);
}

.tone-warning {
  color: #b45309;
  background: rgba(255, 247, 237, 0.98);
  border-color: rgba(251, 191, 36, 0.28);
}

.tone-danger {
  color: #dc2626;
  background: rgba(254, 242, 242, 0.98);
  border-color: rgba(248, 113, 113, 0.24);
}

.tone-info {
  color: #64748b;
  background: rgba(248, 250, 252, 0.98);
  border-color: rgba(226, 232, 240, 0.92);
}
</style>
