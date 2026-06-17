<template>
  <div class="page">
    <div class="page-title">求购信息管理</div>

    <!-- 查询/操作区 -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索：标题/内容/用户名"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />

        <el-select v-model="query.status" clearable placeholder="状态" class="sel">
          <el-option label="待解决" value="OPEN" />
          <el-option label="已解决" value="SOLVED" />
        </el-select>

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>

        <div class="spacer" />

        <el-button
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="求购ID" width="90" align="center" />

        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />

        <el-table-column label="图片" width="120" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="imgUrl(row.imageUrl)"
              style="width: 88px; height: 48px; border: 1px solid #eee"
              fit="cover"
              preview-teleported
              :preview-src-list="[imgUrl(row.imageUrl)]"
            />
            <div v-else class="img-ph">暂无</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'SOLVED'" type="success">已解决</el-tag>
            <el-tag v-else type="warning">待解决</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="userId" label="用户ID" width="90" align="center" />
        <el-table-column prop="username" label="用户名称" width="120" align="center" />
        <el-table-column prop="viewCount" label="浏览量" width="100" align="center" />
        <el-table-column prop="createdAt" label="发布时间" width="170" align="center" />

        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { apiAdminWantedBatchDelete, apiAdminWantedDelete, apiAdminWantedPage } from "@/api/wanted";
import { imgUrl as resolveImgUrl } from "@/utils/img";

type WantedRow = {
  id: number;
  title: string;
  content: string;
  imageUrl?: string;
  status: "OPEN" | "SOLVED";
  userId: number;
  username?: string;
  viewCount: number;
  createdAt: string;
};

const loading = ref(false);
const tableData = ref<WantedRow[]>([]);
const selectedIds = ref<number[]>([]);

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const query = reactive({
  keyword: "",
  status: undefined as ("OPEN" | "SOLVED" | undefined),
});

function imgUrl(url: string) {
  return resolveImgUrl(url);
}

function handleSelectionChange(rows: WantedRow[]) {
  selectedIds.value = rows.map((r) => r.id);
}

async function fetchList() {
  loading.value = true;
  try {
    const res: any = await apiAdminWantedPage({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword || undefined,
      status: query.status,
    });

    // 兼容两种：拦截器返回 data，或返回 {code,data,msg}
    const data = res?.data ?? res;
    const pageData = data?.data ?? data;

    pagination.total = pageData?.total || 0;
    tableData.value = (pageData?.records || []).map((x: any) => ({
      id: x.id,
      title: x.title,
      content: x.content,
      imageUrl: x.imageUrl,
      status: x.status,
      userId: x.userId,
      username: x.username,
      viewCount: x.viewCount ?? 0,
      createdAt: x.createdAt,
    }));
  } catch (e: any) {
    ElMessage.error(e?.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.page = 1;
  fetchList();
}

function handleReset() {
  query.keyword = "";
  query.status = undefined;
  pagination.page = 1;
  fetchList();
}

function handleSizeChange(size: number) {
  pagination.size = size;
  pagination.page = 1;
  fetchList();
}

function handlePageChange(page: number) {
  pagination.page = page;
  fetchList();
}

async function handleDelete(row: WantedRow) {
  try {
    await ElMessageBox.confirm(`确定删除求购「${row.title}」吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    await apiAdminWantedDelete(row.id);
    ElMessage.success("删除成功");
    fetchList();
  } catch {}
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 条求购吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    await apiAdminWantedBatchDelete(selectedIds.value);
    ElMessage.success("批量删除成功");
    selectedIds.value = [];
    fetchList();
  } catch {}
}

onMounted(fetchList);
</script>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #222;
}

.toolbar {
  border: 1px solid #ddd;
  margin-bottom: 12px;
}
.toolbar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.keyword-input {
  width: 320px;
}
.sel {
  width: 160px;
}
.spacer {
  flex: 1;
}

.table-card {
  border: 1px solid #ddd;
}

.img-ph {
  width: 88px;
  height: 48px;
  border: 1px dashed #bbb;
  background: #f5f5f5;
  color: #666;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
