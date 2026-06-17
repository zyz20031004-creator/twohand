<template>
  <div class="page">
    <div class="page-title">地址信息管理</div>

    <!-- 顶部操作区 -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索：联系人/电话/地址/用户名/昵称"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>

        <div class="spacer" />

        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
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

        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">
            {{ (pagination.page - 1) * pagination.size + $index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="contactName" label="联系人" width="120" align="center" />

        <el-table-column prop="addressText" label="联系地址" min-width="260" show-overflow-tooltip />

        <el-table-column prop="contactPhone" label="联系电话" width="150" align="center" />

        <el-table-column label="默认" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success">是</el-tag>
            <el-tag v-else type="info">否</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="所属用户" width="200" align="center">
          <template #default="{ row }">
            {{ formatOwnerName(row) }}
          </template>
        </el-table-column>

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
import { apiAdminAddressBatchDelete, apiAdminAddressDelete, apiAdminAddressPage } from "@/api/address";

type AddressRow = {
  id: number;
  userId: number;
  ownerNickName: string;
  ownerUsername: string;
  contactName: string;
  contactPhone: string;
  addressText: string;
  isDefault: 0 | 1;
};

const loading = ref(false);
const tableData = ref<AddressRow[]>([]);
const selectedIds = ref<number[]>([]);

const query = reactive({ keyword: "" });

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

function handleSelectionChange(rows: AddressRow[]) {
  selectedIds.value = rows.map((r) => r.id);
}

function formatOwnerName(row: AddressRow) {
  const username = String(row.ownerUsername || "").trim();
  const nickName = String(row.ownerNickName || "").trim();
  const displayName = nickName || username;
  if (!displayName && !username) {
    return "-";
  }
  return `${displayName}（${username || displayName}）`;
}

async function fetchList() {
  loading.value = true;
  try {
    const res: any = await apiAdminAddressPage({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword?.trim() || undefined,
    });

    const data = res?.data ?? res;
    const pageData = data?.data ?? data;

    pagination.total = pageData?.total || 0;
    tableData.value = (pageData?.records || []).map((x: any) => ({
      id: x.id,
      userId: x.userId,
      ownerNickName: x.ownerNickName ?? "",
      ownerUsername: x.ownerUsername ?? "",
      contactName: x.contactName,
      contactPhone: x.contactPhone,
      addressText: x.addressText,
      isDefault: x.isDefault ?? 0,
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

async function handleDelete(row: AddressRow) {
  try {
    await ElMessageBox.confirm(`确定删除联系人「${row.contactName}」的地址吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    await apiAdminAddressDelete(row.id);
    ElMessage.success("删除成功");
    fetchList();
  } catch {}
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 条地址吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    await apiAdminAddressBatchDelete(selectedIds.value);
    selectedIds.value = [];
    ElMessage.success("批量删除成功");
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

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
