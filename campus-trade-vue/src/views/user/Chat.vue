<template>
  <section class="page">
    <div class="topbar">
      <el-button text @click="goBack">返回</el-button>
      <div class="title">聊天消息</div>
      <div />
    </div>
    <ChatPanelLite />
  </section>
</template>

<script setup lang="ts">
import ChatPanelLite from "@/components/ChatPanelLite.vue";
import { useRouter } from "vue-router";

const router = useRouter();

function goBack() {
  router.back();
}
</script>

<style scoped>
.page {
  width: 100%;
}

.topbar {
  display: grid;
  grid-template-columns: 80px 1fr 80px;
  align-items: center;
  margin-bottom: 10px;
}

.title {
  text-align: center;
  font-size: 16px;
  font-weight: 700;
}
</style>
