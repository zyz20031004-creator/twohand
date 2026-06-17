<template>
  <div v-if="!loading && hasError" class="state-wrap state-error">
    <el-result icon="error" title="加载失败" :sub-title="errorMessage || defaultErrorText">
      <template #extra>
        <el-button type="primary" @click="emit('retry')">重试</el-button>
      </template>
    </el-result>
  </div>

  <div v-else-if="!loading && empty" class="state-wrap state-empty">
    <el-empty :description="emptyText" />
  </div>

  <slot v-else />
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    loading?: boolean;
    empty?: boolean;
    emptyText?: string;
    errorMessage?: string;
    defaultErrorText?: string;
  }>(),
  {
    loading: false,
    empty: false,
    emptyText: "暂无数据",
    errorMessage: "",
    defaultErrorText: "网络异常，请稍后重试",
  }
);

const emit = defineEmits<{
  (e: "retry"): void;
}>();

const hasError = computed(() => !!props.errorMessage?.trim());
</script>

<style scoped>
.state-wrap {
  min-height: 220px;
  border: 1px dashed #d7dce5;
  border-radius: 12px;
  background: #fff;
}

.state-wrap :deep(.el-result) {
  padding: 36px 12px;
}

.state-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
