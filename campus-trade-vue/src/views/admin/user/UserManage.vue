<template>
  <div class="page">
    <div class="page-title">用户信息管理</div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="请输入用户名/姓名/电话/邮箱关键字"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>

        <div class="spacer" />

        <el-button
          type="warning"
          :disabled="selectedIds.length === 0"
          @click="handleBatchChangeStatus(0)"
        >
          批量禁用
        </el-button>
        <el-button
          type="primary"
          :disabled="selectedIds.length === 0"
          @click="handleBatchChangeStatus(1)"
        >
          批量启用
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" :selectable="isSelectable" />

        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">
            {{ (pagination.page - 1) * pagination.size + $index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="name" label="姓名/昵称" width="120" align="center" />

        <el-table-column label="头像" width="120" align="center">
          <template #default="{ row }">
            <el-avatar :size="44" :src="toImg(row.avatarUrl || row.avatar)">
              {{ (row.name || row.username || "U").slice(0, 1).toUpperCase() }}
            </el-avatar>
          </template>
        </el-table-column>

        <el-table-column label="角色标识" width="120" align="center">
          <template #default="{ row }">
            <span class="tag-role">{{ row.role }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="phone" label="电话" width="140" align="center" />
        <el-table-column prop="email" label="邮箱" min-width="220" />

        <el-table-column label="状态" width="150" align="center">
          <template #default="{ row }">
            <div class="status-switch-wrap">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                :disabled="!canOperate(row) || switchingIds.has(row.id)"
                :before-change="() => confirmStatusChange(row)"
              />
              <span class="status-tip">
                {{ row.status === 1 ? "正常" : "禁用" }}
              </span>
            </div>
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
import {
  apiAdminUserBatchChangeStatus,
  apiAdminUserPage,
  apiAdminUserUpdate,
} from "@/api/user";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { getApiErrorMessage } from "@/utils/apiError";
import { imgUrl as resolveImgUrl } from "@/utils/img";

type UserRow = {
  id: number;
  username: string;
  name: string;
  avatarUrl?: string;
  avatar?: string;
  role: string;
  phone: string;
  email: string;
  status: number;
};

const { runConfirmAction } = useConfirmAction();
const loading = ref(false);
const tableData = ref<UserRow[]>([]);
const selectedIds = ref<number[]>([]);
const switchingIds = ref(new Set<number>());

const query = reactive({
  keyword: "",
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

function toImg(url?: string) {
  return resolveImgUrl(url);
}

function isUserRole(row: Pick<UserRow, "role">) {
  return String(row.role || "").toUpperCase() === "USER";
}

function canOperate(row: Pick<UserRow, "role">) {
  return isUserRole(row);
}

function isSelectable(row: UserRow) {
  return canOperate(row);
}

async function fetchList() {
  loading.value = true;
  try {
    const data = await apiAdminUserPage({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword.trim() || undefined,
    });

    pagination.total = Number(data?.total || 0);
    tableData.value = (data?.records || []).map((item: any) => ({
      id: Number(item.id),
      username: String(item.username || ""),
      name: String(item.name || ""),
      phone: String(item.phone || ""),
      email: String(item.email || ""),
      avatarUrl: item.avatar ?? "",
      avatar: item.avatar ?? "",
      role: String(item.role || "USER").toUpperCase(),
      status: Number(item.status ?? 1),
    }));
  } catch (error: any) {
    ElMessage.error(getApiErrorMessage(error, "加载失败"));
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
  pagination.page = 1;
  fetchList();
}

function handleSelectionChange(rows: UserRow[]) {
  selectedIds.value = rows.filter(canOperate).map((row) => row.id);
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

function setSwitching(id: number, switching: boolean) {
  const next = new Set(switchingIds.value);
  if (switching) {
    next.add(id);
  } else {
    next.delete(id);
  }
  switchingIds.value = next;
}

async function confirmStatusChange(row: UserRow) {
  const currentStatus: 0 | 1 = row.status === 1 ? 1 : 0;
  const nextStatus: 0 | 1 = currentStatus === 1 ? 0 : 1;
  const actionText = nextStatus === 1 ? "启用" : "禁用";
  const message =
    nextStatus === 0
      ? "禁用后该用户将无法正常使用平台功能，确认继续吗？"
      : "确认启用该用户吗？";

  try {
    await ElMessageBox.confirm(message, "二次确认", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
  } catch {
    return false;
  }

  setSwitching(row.id, true);
  try {
    await apiAdminUserUpdate(row.id, { status: nextStatus });
    ElMessage.success(`${actionText}成功`);
    return true;
  } catch (error: any) {
    ElMessage.error(getApiErrorMessage(error, `${actionText}失败`));
    return false;
  } finally {
    setSwitching(row.id, false);
  }
}

async function handleBatchChangeStatus(status: 0 | 1) {
  if (selectedIds.value.length === 0) {
    ElMessage.warning("请先选择用户");
    return;
  }

  const actionText = status === 1 ? "批量启用" : "批量禁用";

  await runConfirmAction({
    title: "二次确认",
    message:
      status === 0
        ? "禁用后这些用户将无法正常使用平台功能，确认继续吗？"
        : `确定${actionText}选中的 ${selectedIds.value.length} 个用户吗？`,
    type: "warning",
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    successMessage: `${actionText}成功`,
    errorMessage: `${actionText}失败`,
    action: async () => {
      await apiAdminUserBatchChangeStatus(selectedIds.value, status);
      selectedIds.value = [];
    },
    onSuccess: fetchList,
  });
}

onMounted(fetchList);
</script>

<style scoped>
.page {
  padding: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #222;
}

.toolbar,
.table-card {
  border: 1px solid #ddd;
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.keyword-input {
  width: 320px;
}

.spacer {
  flex: 1;
}

.status-tip {
  color: #666;
  font-size: 12px;
}

.status-switch-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tag-role {
  padding: 2px 10px;
  border: 1px solid #999;
  background: #efefef;
  color: #222;
  border-radius: 4px;
}

.tag-ok {
  padding: 2px 8px;
  border: 1px solid #999;
  background: #efefef;
  border-radius: 4px;
}

.tag-off {
  padding: 2px 8px;
  border: 1px dashed #999;
  background: #fafafa;
  border-radius: 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
