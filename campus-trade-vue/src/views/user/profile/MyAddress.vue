<template>
  <div class="page-wrap">
    <section class="page-card" v-loading="loading">
      <header class="header-row">
        <div class="header-copy">
          <span class="header-badge">地址管理</span>
          <h2 class="header-title">我的地址</h2>
          <p class="header-desc">管理常用收货地址，方便下单时快速选择，也能让交易时的联系和配送信息更顺畅。</p>
        </div>

        <div class="header-action">
          <el-button type="primary" class="primary-btn" @click="openCreate">新增地址</el-button>
        </div>
      </header>

      <ListDataState
        :loading="loading"
        :empty="tableData.length === 0"
        :error-message="errorMessage"
        empty-text="还没有收货地址，新增一个常用地址后下单会更方便"
        @retry="load"
      >
        <div class="address-list">
          <article v-for="row in tableData" :key="row.id" class="address-card">
            <div class="address-main">
              <div class="address-top">
                <div class="identity-group">
                  <h3>{{ row.contactName || "未命名联系人" }}</h3>
                  <el-tag
                    v-if="row.isDefault === 1"
                    type="success"
                    effect="light"
                    class="default-tag"
                  >
                    默认地址
                  </el-tag>
                </div>

                <span class="phone-chip">联系电话 {{ row.contactPhone || "-" }}</span>
              </div>

              <div class="address-panel">
                <p class="address-text">{{ row.addressText || "暂无地址信息" }}</p>
              </div>
            </div>

            <div class="address-actions">
              <el-button
                class="action-btn accent-btn"
                @click="setDefault(row.id)"
                :disabled="row.isDefault === 1"
              >
                {{ row.isDefault === 1 ? "当前默认" : "设为默认" }}
              </el-button>
              <el-button class="action-btn" @click="openEdit(row)">编辑</el-button>
              <el-button class="action-btn danger-btn" @click="remove(row.id)">删除</el-button>
            </div>
          </article>
        </div>
      </ListDataState>

      <el-dialog
        v-model="dlg.visible"
        :title="dlg.mode === 'create' ? '新增地址' : '编辑地址'"
        width="620px"
        align-center
      >
        <div class="dialog-form">
          <div class="dialog-row">
            <span class="dialog-label">联系人</span>
            <el-input v-model="dlg.form.contactName" placeholder="请输入联系人姓名" />
          </div>
          <div class="dialog-row">
            <span class="dialog-label">联系电话</span>
            <el-input v-model="dlg.form.contactPhone" placeholder="请输入联系电话" />
          </div>
          <div class="dialog-row textarea-row">
            <span class="dialog-label">详细地址</span>
            <el-input
              v-model="dlg.form.addressText"
              type="textarea"
              :rows="4"
              placeholder="请输入宿舍、楼栋、门牌或其它收货说明"
            />
          </div>
          <div class="dialog-row">
            <span class="dialog-label">设为默认</span>
            <el-switch v-model="dlg.form.isDefault" :active-value="1" :inactive-value="0" />
          </div>
        </div>

        <template #footer>
          <el-button @click="dlg.visible = false">取消</el-button>
          <el-button type="primary" @click="submit">确定</el-button>
        </template>
      </el-dialog>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  apiCreateAddress,
  apiDeleteAddress,
  apiGetMyAddressList,
  apiSetDefaultAddress,
  apiUpdateAddress,
} from "@/api/user";
import ListDataState from "@/components/common/ListDataState.vue";
import { useConfirmAction } from "@/composables/useConfirmAction";
import { getApiErrorMessage } from "@/utils/apiError";

const loading = ref(false);
const errorMessage = ref("");
const tableData = ref<any[]>([]);
const { runConfirmAction } = useConfirmAction();

const dlg = reactive({
  visible: false,
  mode: "create" as "create" | "edit",
  editId: 0,
  form: {
    contactName: "",
    contactPhone: "",
    addressText: "",
    isDefault: 0,
  },
});

async function load() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const res = await apiGetMyAddressList();
    tableData.value = res || [];
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "加载地址失败");
    tableData.value = [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  dlg.mode = "create";
  dlg.editId = 0;
  dlg.form = { contactName: "", contactPhone: "", addressText: "", isDefault: 0 };
  dlg.visible = true;
}

function openEdit(row: any) {
  dlg.mode = "edit";
  dlg.editId = row.id;
  dlg.form = {
    contactName: row.contactName || "",
    contactPhone: row.contactPhone || "",
    addressText: row.addressText || "",
    isDefault: row.isDefault ?? 0,
  };
  dlg.visible = true;
}

async function submit() {
  if (!dlg.form.contactName) return ElMessage.warning("请输入联系人");
  if (!dlg.form.contactPhone) return ElMessage.warning("请输入联系电话");
  if (!dlg.form.addressText) return ElMessage.warning("请输入地址");

  try {
    if (dlg.mode === "create") {
      await apiCreateAddress(dlg.form);
      ElMessage.success("新增成功");
    } else {
      await apiUpdateAddress(dlg.editId, dlg.form);
      ElMessage.success("保存成功");
    }

    dlg.visible = false;
    await load();

    if (dlg.form.isDefault === 1) {
      const newest = tableData.value.find((x) => x.contactPhone === dlg.form.contactPhone && x.addressText === dlg.form.addressText);
      if (newest?.id) await setDefault(newest.id);
    }
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "操作失败"));
  }
}

async function setDefault(id: number) {
  try {
    await apiSetDefaultAddress(id);
    ElMessage.success("已设置默认地址");
    await load();
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "操作失败"));
  }
}

async function remove(id: number) {
  await runConfirmAction({
    title: "提示",
    message: "确认删除该地址吗？",
    successMessage: "已删除",
    action: () => apiDeleteAddress(id),
    onSuccess: load,
  });
}

onMounted(load);
</script>

<style scoped>
.page-wrap {
  max-width: 1240px;
  margin: 0 auto;
  padding: 16px;
}

.page-card {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #eef2f7;
  border-radius: 24px;
  padding: 22px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.header-title {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.14;
  font-weight: 800;
}

.header-desc {
  margin: 0;
  max-width: 660px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.72;
}

.header-action {
  flex-shrink: 0;
}

.primary-btn {
  height: 42px;
  padding: 0 18px;
  border-radius: 12px;
  font-weight: 700;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.address-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid #e8eef6;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.address-card:hover {
  transform: translateY(-2px);
  border-color: #dbeafe;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.address-main {
  min-width: 0;
}

.address-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.identity-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.identity-group h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.default-tag {
  border-radius: 999px;
  font-weight: 700;
}

.phone-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f8fbff;
  border: 1px solid #e7eef8;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.address-panel {
  margin-top: 14px;
  padding: 14px 15px;
  border-radius: 16px;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fbff 100%);
  border: 1px solid #edf2f7;
}

.address-text {
  margin: 2px 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.76;
  word-break: break-word;
}

.address-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  margin: 0;
  min-width: 94px;
  height: 36px;
  border-radius: 10px;
  border-color: #dbe6f2;
  color: #475569;
  background: #ffffff;
}

.accent-btn {
  border-color: rgba(187, 247, 208, 0.92);
  color: #15803d;
  background: rgba(240, 253, 244, 0.9);
}

.danger-btn {
  border-color: rgba(248, 113, 113, 0.28);
  color: #dc2626;
}

.dialog-form {
  display: grid;
  gap: 14px;
  padding: 4px 0;
}

.dialog-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.dialog-row.textarea-row {
  align-items: flex-start;
}

.dialog-label {
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 14px;
  background: #f8fbff;
  box-shadow: 0 0 0 1px rgba(209, 219, 234, 0.9);
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

@media (max-width: 760px) {
  .page-wrap {
    padding: 10px;
  }

  .page-card,
  .address-card {
    padding: 16px;
    border-radius: 18px;
  }

  .header-row {
    flex-direction: column;
  }

  .header-title {
    font-size: 24px;
  }

  .header-action,
  .primary-btn {
    width: 100%;
  }

  .address-card,
  .dialog-row {
    grid-template-columns: 1fr;
  }

  .address-actions {
    justify-content: flex-start;
  }
}
</style>
