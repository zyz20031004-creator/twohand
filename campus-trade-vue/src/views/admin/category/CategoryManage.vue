<template>
  <div class="page">
    <!-- 页面标题 -->
    <div class="page-title">分类信息管理</div>

    <!-- 顶部操作区 -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input v-model="query.keyword" placeholder="请输入关键字查询" clearable class="keyword-input"
          @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>

        <div class="toolbar-spacer" />

        <el-button type="success" @click="openCreateDialog">新增</el-button>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe row-key="id"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">
            {{ (pagination.page - 1) * pagination.size + $index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="name" label="分类名称" min-width="220" />

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pager">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </el-card>

    <!-- 新增 / 编辑 弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新增分类' : '编辑分类'" width="480px"
      destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="30" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="handleSave">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  apiAdminCategoryBatchDelete,
  apiAdminCategoryCreate,
  apiAdminCategoryDelete,
  apiAdminCategoryPage,
  apiAdminCategoryUpdate,
} from "@/api/category";

type Category = {
  id: number;
  name: string;
};

const loading = ref(false);
const tableData = ref<Category[]>([]);
const selectedIds = ref<number[]>([]);

const query = reactive({
  keyword: "",
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const dialog = reactive({
  visible: false,
  mode: "create" as "create" | "edit",
  saving: false,
});

const formRef = ref<FormInstance>();
const form = reactive({
  id: 0,
  name: "",
});

const rules: FormRules = {
  name: [
    { required: true, message: "请输入分类名称", trigger: "blur" },
    { min: 2, max: 30, message: "分类名称长度为 2-30 个字符", trigger: "blur" },
  ],
};

/** ✅ 建议你后端按这个风格返回：
 * { code: 0, msg: "ok", data: { list: Category[], total: number } }
 * 如果你用 ApiResp，按你实际字段改一下下面解析即可。
 */
async function fetchList() {
  loading.value = true;
  try {
    const data = await apiAdminCategoryPage({
      keyword: query.keyword || undefined,
      page: pagination.page,
      size: pagination.size,
    });

    const list = data?.list ?? [];
    const total = data?.total ?? 0;

    // ✅ 关键：先清空，让 el-table 彻底重建行
    tableData.value = [];
    await nextTick();

    // ✅ 再填充（顺便断引用）
    tableData.value = list.map((x: any) => ({ ...x }));
    pagination.total = total;
  } catch (e: any) {
    ElMessage.error(e?.message || "加载分类列表失败");
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

function handleSelectionChange(rows: Category[]) {
  selectedIds.value = rows.map((r) => r.id);
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

function openCreateDialog() {
  dialog.mode = "create";
  form.id = 0;
  form.name = "";
  dialog.visible = true;
}

function openEditDialog(row: Category) {
  dialog.mode = "edit";
  form.id = row.id;
  form.name = row.name;
  dialog.visible = true;
}

async function handleSave() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    dialog.saving = true;
    try {
      if (dialog.mode === "create") {
        await apiAdminCategoryCreate({ name: form.name });
        ElMessage.success("新增成功");
      } else {
        await apiAdminCategoryUpdate(form.id, { name: form.name });
        ElMessage.success("保存成功");
      }

      dialog.visible = false;
      await fetchList();
    } catch (e: any) {
      ElMessage.error(e?.message || "保存失败");
    } finally {
      dialog.saving = false;
    }
  });
}
// 删除
async function handleDelete(row: Category) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });

    await apiAdminCategoryDelete(row.id);
    ElMessage.success("删除成功");

    // 如果删到当前页空了，自动回退一页（可选）
    if (tableData.value.length === 1 && pagination.page > 1) {
      pagination.page -= 1;
    }
    await fetchList();
  } catch {
    // 用户取消不提示
  }
}
// 批量删除
async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 条分类吗？`,
      "提示",
      {
        type: "warning",
        confirmButtonText: "确定",
        cancelButtonText: "取消",
      }
    );

    // 推荐后端支持批量：POST /api/admin/categories/batch-delete  { ids: [] }
    await apiAdminCategoryBatchDelete(selectedIds.value);
    ElMessage.success("批量删除成功");

    selectedIds.value = [];
    // 可选：如果删除后当前页可能为空，回退
    pagination.page = 1;
    await fetchList();
  } catch {
    // cancel
  }
}

onMounted(() => {
  fetchList();
});
</script>

<style scoped>
.page {
  padding: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.toolbar {
  margin-bottom: 12px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.keyword-input {
  width: 260px;
}

.toolbar-spacer {
  flex: 1;
}

.table-card {
  margin-top: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
