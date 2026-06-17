<template>
  <div class="page">
    <div class="page-title">问题反馈管理</div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索：主题/内容/联系方式/邮箱"
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

    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="反馈ID" width="90" align="center" />
        <el-table-column prop="subject" label="主题" min-width="160" />
        <el-table-column prop="content" label="反馈内容" min-width="220" show-overflow-tooltip />

        <el-table-column prop="phone" label="联系方式" width="140" align="center" />
        <el-table-column prop="email" label="邮箱" width="200" align="center" />

        <el-table-column label="管理员回复内容" min-width="220">
          <template #default="{ row }">
            <div class="ellipsis-2" :title="row.reply || ''">
              {{ row.reply || "-" }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openReply(row)">回复</el-button>
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

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyDialog.visible" title="回复反馈" width="620px" destroy-on-close>
      <div class="reply-info">
        <div class="row"><span class="k">反馈ID：</span>{{ replyDialog.data?.id }}</div>
        <div class="row"><span class="k">主题：</span>{{ replyDialog.data?.subject }}</div>
        <div class="row">
          <span class="k">反馈内容：</span>
          <div class="box">{{ replyDialog.data?.content }}</div>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="回复内容" prop="reply">
          <el-input
            v-model="form.reply"
            type="textarea"
            :rows="6"
            placeholder="请输入管理员回复内容"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="replyDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleReplySave">保存回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  apiAdminFeedbackBatchDelete,
  apiAdminFeedbackDelete,
  apiAdminFeedbackPage,
  apiAdminFeedbackReply,
} from "@/api/feedback";

type FeedbackRow = {
  id: number;
  subject: string;
  content: string;
  phone: string;
  email: string;
  reply: string;
  createdAt: string;
};

const loading = ref(false);
const saving = ref(false);
const tableData = ref<FeedbackRow[]>([]);
const selectedIds = ref<number[]>([]);

const query = reactive({ keyword: "" });

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const replyDialog = reactive({
  visible: false,
  data: null as FeedbackRow | null,
});

const formRef = ref<FormInstance>();
const form = reactive({
  id: 0,
  reply: "",
});

const rules: FormRules = {
  reply: [
    { required: true, message: "请输入回复内容", trigger: "blur" },
    { min: 2, max: 1000, message: "回复内容长度 2-1000 个字符", trigger: "blur" },
  ],
};

function handleSelectionChange(rows: FeedbackRow[]) {
  selectedIds.value = rows.map((r) => r.id);
}

/** 后端分页 */
function unwrap(resp: any) {
  const r = resp?.data ?? resp;
  if (typeof r?.code !== "undefined") {
    if (r.code !== 0) throw new Error(r.msg || "请求失败");
    return r.data;
  }
  return r;
}

async function fetchList() {
  loading.value = true;
  try {
    const data = unwrap(
      await apiAdminFeedbackPage({
        page: pagination.page,
        size: pagination.size,
        keyword: query.keyword?.trim() || undefined,
      })
    );

    pagination.total = data?.total || 0;

    tableData.value = (data?.records || []).map((x: any) => ({
      id: Number(x.id),
      subject: x.subject ?? "",
      content: x.content ?? "",
      phone: x.phone ?? x.contact ?? "",
      email: x.email ?? "",
      reply: x.reply ?? "",
      createdAt: x.createdAt ?? "",
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

function openReply(row: FeedbackRow) {
  replyDialog.data = { ...row };
  replyDialog.visible = true;
  form.id = row.id;
  form.reply = row.reply || "";
}

async function handleReplySave() {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    saving.value = true;
    try {
      const replyText = (form.reply || "").trim();
      if (!replyText) throw new Error("回复内容不能为空");

      unwrap(await apiAdminFeedbackReply(form.id, { reply: replyText }));

      ElMessage.success("回复已保存");
      replyDialog.visible = false;

      // ✅ 强制重新拉取后端数据
      await fetchList();
    } catch (e: any) {
      ElMessage.error(e?.message || "保存失败");
    } finally {
      saving.value = false;
    }
  });
}

async function handleDelete(row: FeedbackRow) {
  try {
    await ElMessageBox.confirm(`确定删除反馈「${row.subject}」吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    await apiAdminFeedbackDelete(row.id);
    ElMessage.success("删除成功");
    fetchList();
  } catch {}
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定批量删除选中的 ${selectedIds.value.length} 条反馈吗？`,
      "提示",
      { type: "warning", confirmButtonText: "确定", cancelButtonText: "取消" }
    );
    await apiAdminFeedbackBatchDelete(selectedIds.value);
    selectedIds.value = [];
    ElMessage.success("批量删除成功");
    fetchList();
  } catch {}
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

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.ellipsis-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
  line-height: 1.5;
  color: #333;
}

.reply-info {
  margin-bottom: 12px;
  color: #333;
}
.reply-info .row {
  margin-bottom: 8px;
}
.reply-info .k {
  color: #666;
}
.reply-info .box {
  margin-top: 6px;
  padding: 10px;
  border: 1px dashed #ccc;
  background: #fafafa;
  line-height: 1.6;
  color: #444;
}
</style>