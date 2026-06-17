<template>
  <div class="page">
    <div class="page-title">管理员信息管理</div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="账号/姓名/电话/邮箱关键字"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>

        <div class="spacer" />

        <el-button v-if="isSuperAdmin" type="success" @click="openCreate">新增管理员</el-button>
        <el-button v-if="isSuperAdmin" type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" :selectable="isSelectable" />

        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">
            {{ (pagination.page - 1) * pagination.size + $index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="account" label="管理员账号" min-width="160" />

        <el-table-column prop="name" label="姓名" width="120" align="center" />

        <el-table-column prop="phone" label="电话" width="140" align="center" />

        <el-table-column prop="email" label="邮箱" min-width="200" />

        <el-table-column label="头像" width="120" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatarUrl ? toImg(row.avatarUrl) : undefined">
              {{ row.name?.slice(0, 1) || "A" }}
            </el-avatar>
          </template>
        </el-table-column>

        <el-table-column label="角色标识" width="120" align="center">
          <template #default="{ row }">
            <span class="tag-role">{{ row.role }}</span>
          </template>
        </el-table-column>

        <el-table-column label="管理员类型" width="130" align="center">
          <template #default="{ row }">
            <span :class="row.isSuperAdmin ? 'tag-super' : 'tag-role'">
              {{ row.isSuperAdmin ? "超级管理员" : "普通管理员" }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-ok' : 'tag-off'">
              {{ row.status === 1 ? "正常" : "禁用" }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit(row)" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canDelete(row)" type="danger" link @click="handleDelete(row)">删除</el-button>
            <span v-if="!canEdit(row) && !canDelete(row)" class="muted">不可操作</span>
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

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" placeholder="请输入管理员账号" clearable :disabled="dialog.mode === 'edit'" />
        </el-form-item>

        <el-form-item v-if="dialog.mode === 'create'" label="初始密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入初始密码"
            clearable
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item v-if="dialog.mode === 'create'" label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入初始密码"
            clearable
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item v-if="dialog.mode === 'edit'" label="重置密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="不填写则不修改密码"
            clearable
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item v-if="dialog.mode === 'edit' && form.password" label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            clearable
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" clearable />
        </el-form-item>

        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入电话" clearable />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" clearable />
        </el-form-item>

        <el-form-item label="头像链接">
          <div class="avatar-upload-wrap">
            <el-upload class="avatar-uploader" action="/api/upload" :show-file-list="false" :on-success="handleUploadSuccess">
              <img v-if="form.avatarUrl" :src="toImg(form.avatarUrl)" class="avatar-preview" />
              <el-avatar v-else :size="72" class="avatar-preview avatar-fallback">
                {{ form.name?.slice(0, 1) || form.account?.slice(0, 1) || "A" }}
              </el-avatar>
            </el-upload>
            <el-input v-model="form.avatarUrl" placeholder="可选：头像链接" clearable />
          </div>
        </el-form-item>

        <el-form-item label="状态">
          <el-switch v-model="form.statusSwitch" />
          <span style="margin-left: 10px; color: #666">{{ form.statusSwitch ? "正常" : "禁用" }}</span>
        </el-form-item>

        <el-form-item label="角色">
          <el-input value="ADMIN" disabled />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { apiAdminBatchDelete, apiAdminCreate, apiAdminDelete, apiAdminPage, apiAdminUpdate } from "@/api/user";
import { imgUrl as resolveImgUrl } from "@/utils/img";
import { getStoredUser, isCurrentSuperAdmin } from "@/utils/auth";

type AdminRow = {
  id: number;
  account: string;
  name: string;
  phone: string;
  email: string;
  avatarUrl?: string;
  role: "ADMIN";
  status: number; // 1正常 0禁用
  isSuperAdmin?: boolean;
};


const loading = ref(false);
const saving = ref(false);
const tableData = ref<AdminRow[]>([]);
const selectedIds = ref<number[]>([]);
const currentUser = computed(() => getStoredUser());
const currentUserId = computed(() => Number(currentUser.value?.id || 0));
const isSuperAdmin = computed(() => isCurrentSuperAdmin());

const query = reactive({ keyword: "" });

const pagination = reactive({ page: 1, size: 10, total: 0 });

const dialog = reactive({ visible: false, title: "新增管理员", mode: "create" as "create" | "edit" });

const formRef = ref<FormInstance>();
const form = reactive({
  id: 0,
  account: "",
  password: "",
  confirmPassword: "",
  name: "",
  phone: "",
  email: "",
  avatarUrl: "",
  statusSwitch: true, // true=1 false=0
});

function validatePassword(_rule: unknown, value: string, callback: (error?: Error) => void) {
  const password = String(value || "");
  if (dialog.mode === "edit" && !password) {
    callback();
    return;
  }
  if (!password) {
    callback(new Error("请输入初始密码"));
    return;
  }
  if (password.length < 6 || password.length > 20) {
    callback(new Error("密码长度 6-20 个字符"));
    return;
  }
  callback();
}

function validateConfirmPassword(_rule: unknown, value: string, callback: (error?: Error) => void) {
  const confirmPassword = String(value || "");
  if (dialog.mode === "edit" && !form.password && !confirmPassword) {
    callback();
    return;
  }
  if (!confirmPassword) {
    callback(new Error("请再次输入密码"));
    return;
  }
  if (confirmPassword !== form.password) {
    callback(new Error("两次密码不一致"));
    return;
  }
  callback();
}

const rules: FormRules = {
  account: [
    { required: true, message: "请输入管理员账号", trigger: "blur" },
    { min: 3, max: 30, message: "账号长度 3-30 个字符", trigger: "blur" },
  ],
  password: [
    { validator: validatePassword, trigger: "blur" },
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: "blur" },
  ],
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  phone: [{ required: true, message: "请输入电话", trigger: "blur" }],
  email: [{ required: true, message: "请输入邮箱", trigger: "blur" }],
};

function unwrap(resp: any) {
  const r = resp?.data ?? resp;
  if (typeof r?.code !== "undefined") {
    if (r.code !== 0) throw new Error(r.msg || "请求失败");
    return r.data;
  }
  return r;
}

function toImg(url?: string) {
  return resolveImgUrl(url);
}

function handleUploadSuccess(res: any) {
  const url = res?.data || res?.url || res;
  if (typeof url === "string" && url.trim()) {
    form.avatarUrl = url;
    ElMessage.success("头像上传成功");
    return;
  }
  ElMessage.error("头像上传失败");
}

function handleSelectionChange(rows: AdminRow[]) {
  selectedIds.value = rows.map((r) => r.id);
}

function isAdminSuper(row: AdminRow) {
  return row.isSuperAdmin === true || row.account?.toLowerCase() === "admin";
}

function isSelf(row: AdminRow) {
  return Number(row.id) === currentUserId.value;
}

function canEdit(row: AdminRow) {
  return isSuperAdmin.value && !isAdminSuper(row);
}

function canDelete(row: AdminRow) {
  return isSuperAdmin.value && !isAdminSuper(row) && !isSelf(row);
}

function isSelectable(row: AdminRow) {
  return canDelete(row);
}

async function fetchList() {
  loading.value = true;
  try {
    const data = unwrap(
      await apiAdminPage({
        page: pagination.page,
        size: pagination.size,
        keyword: query.keyword?.trim() || undefined,
      })
    );

    pagination.total = data?.total || 0;
    tableData.value = (data?.records || []).map((x: any) => ({
      id: Number(x.id),
      account: x.username ?? x.account ?? "",
      name: x.name ?? "",
      phone: x.phone ?? "",
      email: x.email ?? "",
      avatarUrl: x.avatar ?? x.avatarUrl ?? "",
      role: "ADMIN",
      status: Number(x.status ?? 1),
      isSuperAdmin: x.isSuperAdmin === true || String(x.username ?? x.account ?? "").toLowerCase() === "admin",
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

function openCreate() {
  if (!isSuperAdmin.value) {
    ElMessage.error("仅超级管理员可操作");
    return;
  }
  dialog.visible = true;
  dialog.mode = "create";
  dialog.title = "新增管理员";

  form.id = 0;
  form.account = "";
  form.password = "";
  form.confirmPassword = "";
  form.name = "";
  form.phone = "";
  form.email = "";
  form.avatarUrl = "";
  form.statusSwitch = true;
  formRef.value?.clearValidate();
}

function openEdit(row: AdminRow) {
  if (!canEdit(row)) {
    ElMessage.warning("超级管理员账号不可编辑");
    return;
  }
  dialog.visible = true;
  dialog.mode = "edit";
  dialog.title = "编辑管理员";

  form.id = row.id;
  form.account = row.account;
  form.password = "";
  form.confirmPassword = "";
  form.name = row.name;
  form.phone = row.phone;
  form.email = row.email;
  form.avatarUrl = row.avatarUrl || "";
  form.statusSwitch = row.status === 1;
  formRef.value?.clearValidate();
}

async function handleSave() {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    saving.value = true;
    try {
      if (dialog.mode === "create") {
        unwrap(
          await apiAdminCreate({
            username: form.account.trim(),
            password: form.password,
            name: form.name.trim(),
            phone: form.phone.trim(),
            email: form.email.trim(),
            avatar: form.avatarUrl.trim() || undefined,
            status: form.statusSwitch ? 1 : 0,
          })
        );
        ElMessage.success("新增成功");
      } else {
        const updatePayload: {
          name: string;
          phone: string;
          email: string;
          avatar?: string;
          status: number;
          password?: string;
        } = {
          name: form.name.trim(),
          phone: form.phone.trim(),
          email: form.email.trim(),
          avatar: form.avatarUrl.trim() || undefined,
          status: form.statusSwitch ? 1 : 0,
        };
        if (form.password) {
          updatePayload.password = form.password;
        }
        unwrap(
          await apiAdminUpdate(form.id, updatePayload)
        );
        ElMessage.success("保存成功");
      }

      dialog.visible = false;
      form.password = "";
      form.confirmPassword = "";
      fetchList();
    } catch (e: any) {
      ElMessage.error(e?.message || "保存失败");
    } finally {
      saving.value = false;
    }
  });
}

async function handleDelete(row: AdminRow) {
  if (!canDelete(row)) {
    ElMessage.warning("不能删除超级管理员或当前登录账号");
    return;
  }
  try {
    await ElMessageBox.confirm(`确定删除管理员账号「${row.account}」吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    unwrap(await apiAdminDelete(row.id));
    ElMessage.success("删除成功");
    fetchList();
  } catch {}
}

async function handleBatchDelete() {
  const selectedRows = tableData.value.filter((row) => selectedIds.value.includes(row.id));
  if (selectedRows.some((row) => isAdminSuper(row) || isSelf(row))) {
    ElMessage.warning("不能删除超级管理员或当前登录账号");
    return;
  }
  try {
    await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 个管理员吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    unwrap(await apiAdminBatchDelete(selectedIds.value));
    selectedIds.value = [];
    ElMessage.success("批量删除成功");
    fetchList();
  } catch {}
}

onMounted(fetchList);
</script>

<style scoped>
.page { padding: 0; }
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 12px; color: #222; }

.toolbar, .table-card { border: 1px solid #ddd; }
.toolbar-row { display: flex; align-items: center; gap: 10px; }
.keyword-input { width: 320px; }
.spacer { flex: 1; }

.tag-role { padding: 2px 10px; border: 1px solid #999; background: #efefef; color: #222; border-radius: 4px; }
.tag-super { padding: 2px 10px; border: 1px solid #b45309; background: #fff7ed; color: #9a3412; border-radius: 4px; }
.tag-ok { padding: 2px 8px; border: 1px solid #999; background: #efefef; border-radius: 4px; }
.tag-off { padding: 2px 8px; border: 1px dashed #999; background: #fafafa; border-radius: 4px; }
.muted { color: #909399; font-size: 13px; }

.pager { display: flex; justify-content: flex-end; padding-top: 12px; }

.avatar-upload-wrap { display: flex; flex-direction: column; gap: 10px; width: 100%; }
.avatar-uploader { display: inline-flex; width: fit-content; }
.avatar-preview { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; border: 1px solid #dcdfe6; }
.avatar-fallback { background: #f4f4f5; color: #606266; }
</style>


