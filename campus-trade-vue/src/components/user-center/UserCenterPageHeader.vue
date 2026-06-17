<template>
  <header class="page-header">
    <div class="copy">
      <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
      <h2 class="title">{{ title }}</h2>
      <p v-if="description" class="description">{{ description }}</p>
    </div>

    <div v-if="stats.length || $slots.actions" class="aside">
      <div v-if="stats.length" class="stats">
        <div v-for="item in stats" :key="`${item.label}-${item.value}`" class="stat-chip" :class="chipToneClass(item.tone)">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <div v-if="$slots.actions" class="actions">
        <slot name="actions" />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import type { StatusTone } from "@/utils/tradeStatus";

type HeaderStat = {
  label: string;
  value: string | number;
  tone?: StatusTone;
};

withDefaults(
  defineProps<{
    eyebrow?: string;
    title: string;
    description?: string;
    stats?: HeaderStat[];
  }>(),
  {
    eyebrow: "",
    description: "",
    stats: () => [],
  }
);

function chipToneClass(tone?: StatusTone) {
  return tone ? `tone-${tone}` : "tone-primary";
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.title {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.14;
  font-weight: 800;
}

.description {
  margin: 0;
  max-width: 680px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.72;
}

.aside {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.stats {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 14px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  background: rgba(239, 246, 255, 0.96);
}

.stat-chip span {
  color: #7b8aa3;
  font-size: 11px;
}

.stat-chip strong {
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.tone-success {
  border-color: rgba(220, 252, 231, 0.92);
  background: rgba(240, 253, 244, 0.96);
}

.tone-success strong {
  color: #15803d;
}

.tone-warning {
  border-color: rgba(253, 186, 116, 0.84);
  background: rgba(255, 247, 237, 0.96);
}

.tone-warning strong {
  color: #c2410c;
}

.tone-danger {
  border-color: rgba(252, 165, 165, 0.86);
  background: rgba(254, 242, 242, 0.96);
}

.tone-danger strong {
  color: #dc2626;
}

.tone-info {
  border-color: rgba(226, 232, 240, 0.92);
  background: rgba(248, 250, 252, 0.98);
}

.tone-info strong {
  color: #475569;
}

.actions {
  display: flex;
  align-items: center;
}

@media (max-width: 860px) {
  .page-header {
    flex-direction: column;
  }

  .aside,
  .stats {
    justify-content: flex-start;
  }

  .title {
    font-size: 24px;
  }

  .actions {
    width: 100%;
  }
}
</style>
