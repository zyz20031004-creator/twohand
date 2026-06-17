<template>
  <section class="panel" v-loading="loading">
    <div class="page-shell user-page-shell">
      <div class="hero">
        <div class="hero-copy">
          <span class="hero-badge">平台公告</span>
          <h2>公告通知</h2>
          <p>及时了解平台更新、活动通知与安全提醒，第一时间获取校园交易平台的重要信息。</p>
        </div>

        <div class="hero-side">
          <div class="hero-stat">
            <span class="hero-stat__label">当前页公告</span>
            <strong>{{ records.length }}</strong>
          </div>
          <div class="hero-stat soft">
            <span class="hero-stat__label">累计公告</span>
            <strong>{{ total }}</strong>
          </div>
        </div>
      </div>

      <div class="list-card">
        <ListDataState
          :loading="loading"
          :empty="records.length === 0"
          :error-message="errorMessage"
          empty-text="暂无公告"
          @retry="loadData"
        >
          <div class="list">
            <div v-for="n in records" :key="n.id" class="item">
              <div class="left">
                <div class="item-top">
                  <span class="item-tag">平台通知</span>
                  <span class="time">{{ fmtTime(n.createdAt) }}</span>
                </div>
                <div class="title">{{ displayTitle(n.title) }}</div>
              </div>

              <div class="right">
                <el-button class="detail-btn" @click="openDetail(n.id)">
                  <span>查看详情</span>
                  <span class="detail-btn__arrow">›</span>
                </el-button>
              </div>
            </div>
          </div>
        </ListDataState>

        <div v-if="!loading && !errorMessage && total > 0" class="pager">
          <el-pagination
            :current-page="query.page"
            :page-size="query.size"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <el-drawer v-model="drawerOpen" class="notice-drawer" title="公告详情" size="min(560px, 100vw)">
      <div class="detail-shell">
        <div class="detail-card">
          <div class="detail-head">
            <span class="detail-tag">平台公告</span>
            <span class="detail-time">发布时间 {{ fmtTime(detail.createdAt) }}</span>
          </div>

          <div class="detail-title">{{ displayTitle(detail.title) }}</div>

          <p class="detail-desc">以下内容为平台公告正文，请及时关注更新、活动安排与安全提醒。</p>

          <div class="detail-divider"></div>

          <div class="detail-content">{{ detail.content || "暂无公告内容" }}</div>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { apiNoticeDetail, apiNoticePage } from "@/api/notice";
import ListDataState from "@/components/common/ListDataState.vue";
import { usePagedQuery } from "@/composables/usePagedQuery";
import { getApiErrorMessage } from "@/utils/apiError";

const TITLE_MAP: Record<string, string> = {
  "system update notice": "系统升级通知",
  "new semester activity": "新学期活动通知",
  "safe transaction reminder": "安全交易提醒",
  "new features launched": "新功能上线通知",
  "user guide": "平台使用指南",
};

const loading = ref(false);
const errorMessage = ref("");

const { query, changePage } = usePagedQuery({
  page: 1,
  size: 8,
});
const total = ref(0);
const records = ref<any[]>([]);

const drawerOpen = ref(false);
const detail = ref<any>({ title: "", content: "", createdAt: "" });

function fmtTime(t?: string) {
  if (!t) return "";
  return String(t).replace("T", " ").slice(0, 16);
}

function displayTitle(title?: string) {
  const raw = String(title || "").trim();
  if (!raw) return "未命名公告";
  return TITLE_MAP[raw.toLowerCase()] || raw;
}

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const resp: any = await apiNoticePage({ page: query.page, size: query.size });
    const payload = resp?.data ?? resp;
    total.value = Number(payload?.total || 0);
    records.value = payload?.records || [];
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "加载公告失败");
    total.value = 0;
    records.value = [];
  } finally {
    loading.value = false;
  }
}

async function openDetail(id: number) {
  try {
    const resp: any = await apiNoticeDetail(id);
    const payload = resp?.data ?? resp;
    detail.value = payload;
    drawerOpen.value = true;
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "加载公告详情失败"));
  }
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

.hero {
  margin-bottom: 16px;
  padding: 18px 20px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.08), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(246, 250, 255, 0.98) 100%);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(300px, 0.98fr);
  align-items: center;
  gap: 18px;
}

.hero-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-badge,
.item-tag,
.detail-tag {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.hero-copy h2 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.14;
  font-weight: 800;
}

.hero-copy p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.68;
  max-width: 580px;
}

.hero-side {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
  min-width: 0;
  padding-bottom: 2px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.hero-side::-webkit-scrollbar {
  display: none;
}

.hero-stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: max-content;
  height: 42px;
  padding: 0 12px;
  border-radius: 14px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  background: rgba(239, 246, 255, 0.96);
  white-space: nowrap;
}

.hero-stat.soft {
  border-color: rgba(220, 252, 231, 0.92);
  background: rgba(240, 253, 244, 0.96);
}

.hero-stat__label {
  color: #7b8aa3;
  font-size: 11px;
  line-height: 1;
}

.hero-stat strong {
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1;
  font-weight: 700;
}

.hero-stat.soft strong {
  color: #15803d;
}

.list-card {
  padding: 10px 0 0;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.84);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.item:hover {
  transform: translateY(-3px);
  border-color: rgba(191, 219, 254, 0.96);
  box-shadow: 0 18px 34px rgba(59, 130, 246, 0.1);
}

.left {
  min-width: 0;
  flex: 1;
}

.item-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.title {
  color: #0f172a;
  font-size: 20px;
  line-height: 1.35;
  font-weight: 800;
}

.time {
  color: #94a3b8;
  font-size: 12px;
}

.detail-btn {
  height: 40px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(191, 219, 254, 0.92);
  background: rgba(239, 246, 255, 0.92);
  color: #2563eb;
  font-weight: 700;
}

.detail-btn__arrow {
  margin-left: 6px;
  font-size: 16px;
  line-height: 1;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.detail-shell {
  padding: 8px;
}

.detail-card {
  max-width: 100%;
  padding: 24px;
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.08), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(248, 251, 255, 0.98) 100%);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.05);
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-time {
  color: #94a3b8;
  font-size: 12px;
}

.detail-title {
  margin-top: 16px;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.35;
  font-weight: 800;
}

.detail-desc {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.75;
}

.detail-divider {
  height: 1px;
  margin: 18px 0;
  background: rgba(226, 232, 240, 0.88);
}

.detail-content {
  max-width: 100%;
  color: #334155;
  font-size: 15px;
  line-height: 2;
  white-space: pre-wrap;
}

.notice-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 22px 24px 14px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  border-bottom: 1px solid rgba(226, 232, 240, 0.82);
}

.notice-drawer :deep(.el-drawer__body) {
  padding: 16px 18px 22px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.06), transparent 24%),
    linear-gradient(180deg, #f8fbff 0%, #f4f8fd 100%);
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .hero-side {
    justify-content: flex-start;
  }

  .item {
    align-items: flex-start;
    flex-direction: column;
  }

  .right {
    width: 100%;
  }

  .detail-btn {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .panel {
    padding-bottom: 20px;
  }

  .hero,
  .item,
  .detail-card {
    padding: 16px;
  }

  .hero-copy h2 {
    font-size: 24px;
  }

  .title {
    font-size: 18px;
  }

  .detail-title {
    font-size: 24px;
  }

  .pager {
    justify-content: center;
  }
}
</style>
