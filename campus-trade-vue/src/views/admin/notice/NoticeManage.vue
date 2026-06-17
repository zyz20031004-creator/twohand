<template>
  <div class="page">
    <div class="page-title">公告信息管理</div>

    <!-- 顶部操作区 -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="请输入标题关键字查询"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />

        <el-select v-model="query.status" clearable placeholder="状态" class="sel">
          <el-option label="展示" :value="1" />
          <el-option label="下线" :value="0" />
        </el-select>

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>

        <div class="spacer" />

        <el-button type="success" @click="openCreate">新增公告</el-button>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>
    </el-card>

    <!-- 表格区域 -->
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

        <el-table-column prop="title" label="公告标题" min-width="180" show-overflow-tooltip />

        <el-table-column label="公告内容" min-width="320">
          <template #default="{ row }">
            <div class="ellipsis-3" :title="row.content">
              {{ row.content }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">展示</el-tag>
            <el-tag v-else type="info">下线</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />

        <el-table-column prop="creatorName" label="创建人" width="120" align="center" />

        <el-table-column label="上下线" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val:any)=>handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" clearable maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">展示</el-radio>
            <el-radio :value="0">下线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  apiAdminNoticeBatchDelete,
  apiAdminNoticeCreate,
  apiAdminNoticeDelete,
  apiAdminNoticePage,
  apiAdminNoticeSetStatus,
  apiAdminNoticeUpdate,
} from "@/api/notice";

type NoticeRow = {
  id: number;
  title: string;
  content: string;
  status: 0 | 1;
  createdAt: string;
  creatorId: number;
  creatorName: string;
};

const loading = ref(false);
const tableData = ref<NoticeRow[]>([]);
const selectedIds = ref<number[]>([]);

const query = reactive({
  keyword: "",
  status: undefined as (0 | 1 | undefined),
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const dialog = reactive({
  visible: false,
  title: "新增公告",
  mode: "create" as "create" | "edit",
});

const formRef = ref<FormInstance>();
const form = reactive({
  id: 0,
  title: "",
  content: "",
  status: 1 as 0 | 1,
});

const rules: FormRules = {
  title: [
    { required: true, message: "请输入公告标题", trigger: "blur" },
    { min: 2, max: 50, message: "标题长度 2-50 个字符", trigger: "blur" },
  ],
  content: [
    { required: true, message: "请输入公告内容", trigger: "blur" },
    { min: 5, max: 1000, message: "内容长度 5-1000 个字符", trigger: "blur" },
  ],
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
};

async function fetchList() {
  loading.value = true;
  try {
    const res: any = await apiAdminNoticePage({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword?.trim() || undefined,
      status: query.status,
    });

    const data = res?.data ?? res;
    const pageData = data?.data ?? data;

    pagination.total = pageData?.total || 0;
    tableData.value = (pageData?.records || []).map((x: any) => ({
      id: x.id,
      title: x.title,
      content: x.content,
      status: x.status,
      createdAt: x.createdAt,
      creatorId: x.creatorId,
      creatorName: x.creatorName,
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

function handleSelectionChange(rows: NoticeRow[]) {
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

function openCreate() {
  dialog.visible = true;
  dialog.mode = "create";
  dialog.title = "新增公告";
  form.id = 0;
  form.title = "";
  form.content = "";
  form.status = 1;
}

function openEdit(row: NoticeRow) {
  dialog.visible = true;
  dialog.mode = "edit";
  dialog.title = "编辑公告";
  form.id = row.id;
  form.title = row.title;
  form.content = row.content;
  form.status = row.status;
}

async function handleSave() {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    if (dialog.mode === "create") {
      await apiAdminNoticeCreate({ title: form.title, content: form.content, status: form.status });
      ElMessage.success("新增成功");
    } else {
      await apiAdminNoticeUpdate(form.id, { title: form.title, content: form.content, status: form.status });
      ElMessage.success("保存成功");
    }

    dialog.visible = false;
    fetchList();
  });
}

async function handleDelete(row: NoticeRow) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    await apiAdminNoticeDelete(row.id);
    ElMessage.success("删除成功");
    fetchList();
  } catch {}
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 条公告吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    await apiAdminNoticeBatchDelete(selectedIds.value);
    selectedIds.value = [];
    ElMessage.success("批量删除成功");
    fetchList();
  } catch {}
}

async function handleToggleStatus(row: NoticeRow, val: boolean) {
  const newStatus: 0 | 1 = val ? 1 : 0;
  try {
    await apiAdminNoticeSetStatus(row.id, newStatus);
    ElMessage.success(newStatus === 1 ? "已展示" : "已下线");
    fetchList();
  } catch (e: any) {
    ElMessage.error(e?.message || "操作失败");
    fetchList();
  }
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
  width: 280px;
}
.sel {
  width: 160px;
}

.spacer {
  flex: 1;
}

.ellipsis-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
  line-height: 1.5;
  color: #333;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>