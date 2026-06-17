<template>
  <div class="dashboard">
    <section class="welcome-panel">
      <div>
        <h2 class="welcome-title">您好，管理员！欢迎使用校园二手交易平台后台管理系统</h2>
        <p class="welcome-desc">这里展示平台当前运营数据、待处理事项和近期变化趋势。</p>
      </div>
      <div class="welcome-extra">
        <span class="extra-label">今日订单</span>
        <strong class="extra-value">{{ formatCount(stats.todayOrderCount) }}</strong>
      </div>
    </section>

    <el-row :gutter="16" class="summary-row">
      <el-col v-for="card in summaryCards" :key="card.key" :xs="24" :sm="12" :lg="8" :xl="6">
        <el-card class="summary-card" shadow="never" v-loading="loading">
          <div class="summary-card__inner">
            <div class="summary-body">
              <div class="summary-label">{{ card.label }}</div>
              <div class="summary-value">{{ formatCount(card.value) }}</div>
              <div class="summary-desc">{{ card.desc }}</div>
            </div>
            <div class="summary-icon" :class="card.tone">
              <component :is="card.icon" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="middle-row">
      <el-col :xs="24" :lg="10">
        <el-card class="todo-card" shadow="never" v-loading="loading">
          <template #header>
            <div class="section-title">待办事项</div>
          </template>

          <div class="todo-list">
            <div v-for="item in todoItems" :key="item.label" class="todo-item">
              <div class="todo-main">
                <div class="todo-name">{{ item.label }}</div>
                <div class="todo-hint">{{ item.hint }}</div>
              </div>
              <div class="todo-side">
                <span class="todo-count">{{ formatCount(item.value) }} 条</span>
                <el-button text type="primary" @click="goTo(item.path)">去处理</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card class="todo-card" shadow="never" v-loading="loading">
          <template #header>
            <div class="section-title">运营补充</div>
          </template>

          <div class="mini-metrics">
            <div class="mini-metric">
              <span class="mini-label">已完成订单</span>
              <strong class="mini-value">{{ formatCount(stats.finishedOrderCount) }}</strong>
            </div>
            <div class="mini-metric">
              <span class="mini-label">待支付订单</span>
              <strong class="mini-value">{{ formatCount(orderStatusValue("待支付")) }}</strong>
            </div>
            <div class="mini-metric">
              <span class="mini-label">已支付订单</span>
              <strong class="mini-value">{{ formatCount(orderStatusValue("已支付")) }}</strong>
            </div>
            <div class="mini-metric">
              <span class="mini-label">已取消订单</span>
              <strong class="mini-value">{{ formatCount(orderStatusValue("已取消")) }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Bell, ChatDotRound, Checked, Goods, Tickets, User } from "@element-plus/icons-vue";

import { http } from "../../api/http";

type StatusPoint = {
  name: string;
  value: number;
};

type DashboardResp = {
  userCount?: number;
  onSaleProductCount?: number;
  pendingProductCount?: number;
  pendingVerifyCount?: number;
  pendingReportCount?: number;
  pendingFeedbackCount?: number;
  todayOrderCount?: number;
  finishedOrderCount?: number;
  orderStatusStats?: StatusPoint[];
};

const router = useRouter();
const loading = ref(false);

const stats = ref<Required<DashboardResp>>({
  userCount: 0,
  onSaleProductCount: 0,
  pendingProductCount: 0,
  pendingVerifyCount: 0,
  pendingReportCount: 0,
  pendingFeedbackCount: 0,
  todayOrderCount: 0,
  finishedOrderCount: 0,
  orderStatusStats: [],
});

const summaryCards = computed(() => [
  {
    key: "userCount",
    label: "用户总数",
    value: stats.value.userCount,
    desc: "当前平台注册普通用户数量",
    tone: "tone-blue",
    icon: User,
  },
  {
    key: "onSaleProductCount",
    label: "在售商品",
    value: stats.value.onSaleProductCount,
    desc: "已审核通过且仍在售卖的商品",
    tone: "tone-green",
    icon: Goods,
  },
  {
    key: "pendingProductCount",
    label: "待审核商品",
    value: stats.value.pendingProductCount,
    desc: "等待管理员审核的商品",
    tone: "tone-orange",
    icon: Tickets,
  },
  {
    key: "pendingVerifyCount",
    label: "待审核认证",
    value: stats.value.pendingVerifyCount,
    desc: "待处理的学号认证申请",
    tone: "tone-cyan",
    icon: Checked,
  },
  {
    key: "pendingReportCount",
    label: "待处理举报",
    value: stats.value.pendingReportCount,
    desc: "用户提交后尚未处理的举报",
    tone: "tone-red",
    icon: Bell,
  },
  {
    key: "pendingFeedbackCount",
    label: "待处理反馈",
    value: stats.value.pendingFeedbackCount,
    desc: "等待回复的用户反馈",
    tone: "tone-purple",
    icon: ChatDotRound,
  },
]);

const todoItems = computed(() => [
  {
    label: "商品待审核",
    value: stats.value.pendingProductCount,
    hint: "请尽快检查商品信息是否符合发布规范",
    path: "/admin/products",
  },
  {
    label: "学号认证待审核",
    value: stats.value.pendingVerifyCount,
    hint: "核验学生身份信息，避免冒用账号",
    path: "/admin/verify",
  },
  {
    label: "举报待处理",
    value: stats.value.pendingReportCount,
    hint: "优先处理影响交易安全的举报内容",
    path: "/admin/reports",
  },
  {
    label: "反馈待回复",
    value: stats.value.pendingFeedbackCount,
    hint: "及时回复用户反馈，减少重复咨询",
    path: "/admin/feedbacks",
  },
]);

const orderStatusData = computed(() => {
  const defaults = ["待支付", "已支付", "已完成", "已取消"];
  const source = new Map(
    stats.value.orderStatusStats.map((item) => [String(item?.name ?? ""), toInt(item?.value)])
  );
  return defaults.map((label) => ({
    label,
    value: source.get(label) ?? 0,
  }));
});

onMounted(loadStats);

async function loadStats() {
  loading.value = true;
  try {
    const data = await http.get<DashboardResp>("/admin/dashboard/stats");
    stats.value = {
      userCount: toInt(data?.userCount),
      onSaleProductCount: toInt(data?.onSaleProductCount),
      pendingProductCount: toInt(data?.pendingProductCount),
      pendingVerifyCount: toInt(data?.pendingVerifyCount),
      pendingReportCount: toInt(data?.pendingReportCount),
      pendingFeedbackCount: toInt(data?.pendingFeedbackCount),
      todayOrderCount: toInt(data?.todayOrderCount),
      finishedOrderCount: toInt(data?.finishedOrderCount),
      orderStatusStats: normalizeStatusStats(data?.orderStatusStats),
    };
  } finally {
    loading.value = false;
  }
}

function normalizeStatusStats(source: StatusPoint[] | undefined) {
  if (!Array.isArray(source)) {
    return [];
  }
  return source.map((item) => ({
    name: String(item?.name ?? ""),
    value: toInt(item?.value),
  }));
}

function orderStatusValue(name: string) {
  return orderStatusData.value.find((item) => item.label === name)?.value ?? 0;
}

function formatCount(value: number) {
  return `${toInt(value)}`;
}

function goTo(path: string) {
  router.push(path);
}

function toInt(value: unknown) {
  const num = Number(value);
  if (!Number.isFinite(num) || num < 0) {
    return 0;
  }
  return Math.round(num);
}
</script>

<style scoped>
.dashboard {
  width: 100%;
}

.welcome-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.welcome-title {
  margin: 0;
  color: #1f2937;
  font-size: 20px;
  font-weight: 600;
}

.welcome-desc {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.welcome-extra {
  min-width: 120px;
  padding: 12px 14px;
  text-align: right;
  background: #f8fafc;
  border-radius: 8px;
}

.extra-label {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.extra-value {
  color: #111827;
  font-size: 24px;
  line-height: 1.3;
}

.summary-row,
.middle-row {
  margin-bottom: 12px;
}

.summary-card,
.todo-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.summary-card {
  height: 104px;
}

.summary-card :deep(.el-card__body) {
  height: 100%;
  padding: 16px 18px;
  box-sizing: border-box;
}

.summary-card__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 100%;
}

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border-radius: 12px;
  font-size: 22px;
  opacity: 0.92;
}

.summary-icon :deep(svg),
.summary-icon :deep(.el-icon) {
  width: 22px;
  height: 22px;
  font-size: 22px;
}

.tone-blue {
  color: #2563eb;
  background: #dbeafe;
}

.tone-green {
  color: #059669;
  background: #d1fae5;
}

.tone-orange {
  color: #ea580c;
  background: #ffedd5;
}

.tone-cyan {
  color: #0891b2;
  background: #cffafe;
}

.tone-red {
  color: #dc2626;
  background: #fee2e2;
}

.tone-purple {
  color: #7c3aed;
  background: #ede9fe;
}

.summary-body {
  min-width: 0;
  flex: 1;
}

.summary-label {
  color: #374151;
  font-size: 14px;
  line-height: 1.4;
}

.summary-value {
  margin-top: 6px;
  color: #111827;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.15;
}

.summary-desc {
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.section-title {
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

.todo-list {
  display: grid;
  gap: 12px;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #f1f5f9;
}

.todo-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.todo-name {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
}

.todo-hint {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.todo-side {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.todo-count {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
}

.mini-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mini-metric {
  padding: 18px 16px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.mini-label {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.mini-value {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 26px;
  line-height: 1.2;
}

@media (max-width: 1200px) {
  .welcome-panel {
    align-items: flex-start;
    flex-direction: column;
  }

  .welcome-extra {
    width: 100%;
    text-align: left;
  }
}

@media (max-width: 768px) {
  .summary-card {
    height: auto;
    min-height: 100px;
  }

  .summary-card :deep(.el-card__body) {
    padding: 14px 16px;
  }

  .summary-icon {
    width: 40px;
    height: 40px;
    flex-basis: 40px;
  }

  .summary-icon :deep(svg),
  .summary-icon :deep(.el-icon) {
    width: 20px;
    height: 20px;
    font-size: 20px;
  }

  .mini-metrics {
    grid-template-columns: 1fr;
  }

  .todo-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .todo-side {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
