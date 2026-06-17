<template>
  <div class="confirm-order-page" v-loading="loading">
    <div class="page-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <el-button text class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </el-button>
        <div class="header-info">
          <span class="eyebrow">订单确认</span>
          <h1>确认订单信息</h1>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-if="errorMessage" class="state-card">
        <el-empty description="加载失败">
          <el-button type="primary" @click="loadProductDetail">重新加载</el-button>
        </el-empty>
        <p class="state-text">{{ errorMessage }}</p>
      </div>

      <template v-else>
        <!-- 收货地址区域 -->
        <section class="section-card address-section">
          <div class="section-header">
            <div class="section-title-group">
              <span class="eyebrow">STEP 1</span>
              <h2 class="section-title">收货信息</h2>
            </div>
            <div class="section-actions">
              <el-button class="action-btn" @click="showAddressDialog = true">
                <el-icon><Location /></el-icon>
                选择地址
              </el-button>
              <el-button class="action-btn add-btn" @click="showAddAddressDialog = true">
                <el-icon><Plus /></el-icon>
                新增地址
              </el-button>
            </div>
          </div>

          <!-- 默认地址展示 -->
          <div v-if="selectedAddress" class="address-card selected" @click="showAddressDialog = true">
            <div class="address-icon">
              <el-icon><Location /></el-icon>
            </div>
            <div class="address-info">
              <div class="address-contact">
                <span class="contact-name">{{ selectedAddress.contactName }}</span>
                <span class="contact-phone">{{ selectedAddress.contactPhone }}</span>
              </div>
              <div class="address-text">{{ selectedAddress.addressText }}</div>
            </div>
            <div class="address-arrow">
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>

          <!-- 无地址提示 -->
          <div v-else class="no-address-tip">
            <el-icon><Warning /></el-icon>
            <span>请先添加收货地址</span>
            <el-button type="primary" size="small" @click="showAddAddressDialog = true">立即添加</el-button>
          </div>
        </section>

        <!-- 商品信息区域 -->
        <section class="section-card product-section">
          <div class="section-header">
            <div class="section-title-group">
              <span class="eyebrow">STEP 2</span>
              <h2 class="section-title">商品信息</h2>
            </div>
          </div>

          <div class="product-card">
            <div class="product-image" @click="goProductDetail">
              <el-image
                v-if="productDetail.coverUrl"
                :src="imgUrl(productDetail.coverUrl)"
                fit="cover"
                class="product-img"
              >
                <template #error>
                  <div class="img-placeholder">暂无图片</div>
                </template>
              </el-image>
              <div v-else class="img-placeholder">暂无图片</div>
            </div>
            <div class="product-info">
              <h3 class="product-title" @click="goProductDetail">{{ productDetail.title || '未命名商品' }}</h3>
              <div class="product-meta">
                <span class="meta-tag">
                  <el-icon><Goods /></el-icon>
                  {{ productDetail.schoolName || '本校商品' }}
                </span>
              </div>
              <div class="seller-info">
                <span class="seller-label">卖家：{{ productDetail.sellerName || '校园卖家' }}</span>
                <span class="view-count">
                  <el-icon><View /></el-icon>
                  {{ productDetail.viewCount || 0 }} 浏览
                </span>
              </div>
            </div>
            <div class="product-price-section">
              <div class="price-label">商品金额</div>
              <div class="price-value">{{ formatPrice(productDetail.price) }}</div>
            </div>
          </div>
        </section>

        <!-- 交易方式区域 -->
        <section class="section-card trade-section">
          <div class="section-header">
            <div class="section-title-group">
              <span class="eyebrow">STEP 3</span>
              <h2 class="section-title">交易方式</h2>
            </div>
          </div>

          <!-- 送货上门 -->
          <div v-if="tradeMethod === 'DELIVERY'" class="trade-card delivery">
            <div class="trade-icon delivery-icon">
              <el-icon><Van /></el-icon>
            </div>
            <div class="trade-content">
              <div class="trade-title">送货上门</div>
              <div class="trade-desc">商品将通过快递/送货服务送达至您的收货地址</div>
              <div class="trade-tip" v-if="!selectedAddress">
                <el-icon><Warning /></el-icon>
                请先选择收货地址
              </div>
            </div>
          </div>

          <!-- 线下自提（校内当面交易） -->
          <div v-else-if="tradeMethod === 'MEETUP'" class="trade-card pickup">
            <div class="trade-icon pickup-icon">
              <el-icon><Location /></el-icon>
            </div>
            <div class="trade-content">
              <div class="trade-title">线下自提</div>
              <div class="trade-desc">请前往指定地点与卖家当面交易验货</div>
              <div class="trade-location" v-if="tradeLocationText">
                <el-icon><MapLocation /></el-icon>
                {{ tradeLocationText }}
              </div>
              <div class="trade-tip">
                <el-icon><ChatDotRound /></el-icon>
                请通过站内聊天与卖家协商具体交易地点和时间
              </div>
            </div>
          </div>

          <!-- 双方协商 -->
          <div v-else class="trade-card negotiate">
            <div class="trade-icon negotiate-icon">
              <el-icon><ChatLineSquare /></el-icon>
            </div>
            <div class="trade-content">
              <div class="trade-title">双方协商</div>
              <div class="trade-desc">交易方式由买卖双方协商确定</div>
              <div class="trade-tip">
                <el-icon><ChatDotRound /></el-icon>
                请通过站内聊天与卖家协商交易方式和地点
              </div>
            </div>
          </div>
        </section>

        <!-- 金额明细区域 -->
        <section class="section-card amount-section">
          <div class="amount-row">
            <span class="amount-label">商品金额</span>
            <span class="amount-value">{{ formatPrice(productDetail.price) }}</span>
          </div>
          <div class="amount-divider"></div>
          <div class="amount-row total-row">
            <span class="total-label">实付款</span>
            <span class="total-value">{{ formatPrice(productDetail.price) }}</span>
          </div>
        </section>
      </template>

      <!-- 底部操作栏 -->
      <div class="bottom-bar">
        <div class="bottom-total">
          <span class="total-text">合计：</span>
          <span class="total-amount">{{ formatPrice(productDetail.price) }}</span>
        </div>
        <el-button
          type="primary"
          class="submit-btn"
          :disabled="!canSubmit || submitting"
          :loading="submitting"
          @click="submitOrder"
        >
          提交订单
        </el-button>
      </div>
    </div>

    <!-- 地址选择对话框 -->
    <el-dialog v-model="showAddressDialog" title="选择收货地址" width="580px" destroy-on-close>
      <div v-loading="addressLoading" class="address-list-panel">
        <div
          v-for="addr in addressList"
          :key="addr.id"
          class="address-item"
          :class="{ selected: selectedAddressId === addr.id, default: addr.isDefault === 1 }"
          @click="selectAddress(addr)"
        >
          <div class="address-item-content">
            <div class="address-item-contact">
              <span class="contact-name">{{ addr.contactName }}</span>
              <span class="contact-phone">{{ addr.contactPhone }}</span>
              <span v-if="addr.isDefault === 1" class="default-tag">默认</span>
            </div>
            <div class="address-item-text">{{ addr.addressText }}</div>
          </div>
          <div class="address-item-check" v-if="selectedAddressId === addr.id">
            <el-icon><Check /></el-icon>
          </div>
        </div>
        <el-empty v-if="!addressLoading && addressList.length === 0" description="暂无收货地址" />
      </div>
      <template #footer>
        <el-button @click="showAddressDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedAddressId" @click="confirmAddressSelect">确认选择</el-button>
      </template>
    </el-dialog>

    <!-- 新增地址对话框 -->
    <el-dialog v-model="showAddAddressDialog" title="新增收货地址" width="520px" destroy-on-close>
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="80px">
        <el-form-item label="收货人" prop="contactName">
          <el-input v-model="addressForm.contactName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="addressForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="收货地址" prop="addressText">
          <el-input v-model="addressForm.addressText" type="textarea" :rows="3" placeholder="请输入详细收货地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddAddressDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingAddress" @click="saveNewAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  ChatDotRound,
  ChatLineSquare,
  Goods,
  Location,
  MapLocation,
  Plus,
  Van,
  View,
  Warning,
} from "@element-plus/icons-vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { apiCreateOrder } from "@/api/order";
import { apiGetProductDetail } from "@/api/product";
import { apiGetMyAddressList, apiCreateAddress, type UserAddressItem } from "@/api/user";
import { useAuthUser } from "@/composables/useAuthUser";
import { getApiErrorMessage } from "@/utils/apiError";
import { productImgUrl as resolveProductImgUrl } from "@/utils/img";
import { formatPrice } from "@/utils/price";
import { getTradeDetail, inferTradeMethod } from "@/utils/tradeLocation";

const route = useRoute();
const router = useRouter();
const { requireLogin } = useAuthUser();

const productId = computed(() => {
  const id = Number(route.params.productId);
  return Number.isFinite(id) && id > 0 ? id : 0;
});

const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
interface ProductDetail {
  id: number;
  title: string;
  price: number;
  coverUrl: string;
  sellerId: number;
  sellerName: string;
  schoolName: string;
  conditionLevel?: string;
  viewCount: number;
  addressText: string;
  images: any[];
}

const productDetail = ref<ProductDetail>({
  id: 0,
  title: "",
  price: 0,
  coverUrl: "",
  sellerId: 0,
  sellerName: "",
  schoolName: "",
  viewCount: 0,
  addressText: "",
  images: [],
});

const addressList = ref<UserAddressItem[]>([]);
const selectedAddressId = ref<number | null>(null);
const addressLoading = ref(false);
const showAddressDialog = ref(false);
const showAddAddressDialog = ref(false);
const savingAddress = ref(false);
const addressFormRef = ref<FormInstance>();

const addressForm = reactive({
  contactName: "",
  contactPhone: "",
  addressText: "",
});

const addressRules: FormRules = {
  contactName: [{ required: true, message: "请输入收货人姓名", trigger: "blur" }],
  contactPhone: [{ required: true, message: "请输入联系电话", trigger: "blur" }],
  addressText: [{ required: true, message: "请输入详细收货地址", trigger: "blur" }],
};

const selectedAddress = computed(() => {
  if (!selectedAddressId.value) return null;
  return addressList.value.find((a) => a.id === selectedAddressId.value) || null;
});

const tradeDetail = computed(() => getTradeDetail(productDetail.value.addressText));
const tradeMethod = computed(() => inferTradeMethod(productDetail.value.addressText));
const tradeLocationText = computed(() => tradeDetail.value.description || tradeDetail.value.locationText || "");

const canSubmit = computed(() => {
  if (!productId.value) return false;
  if (tradeMethod.value === "DELIVERY" && !selectedAddressId.value) return false;
  return true;
});

function imgUrl(url?: string) {
  return resolveProductImgUrl(url || "");
}

function goBack() {
  router.back();
}

function goProductDetail() {
  if (productId.value) {
    router.push(`/user/hot/product/${productId.value}`);
  }
}

async function loadProductDetail() {
  if (!productId.value) {
    errorMessage.value = "商品编号无效";
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await apiGetProductDetail(productId.value);
    productDetail.value = {
      ...productDetail.value,
      ...data,
    };
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, "加载商品详情失败");
  } finally {
    loading.value = false;
  }
}

async function loadAddressList() {
  addressLoading.value = true;
  try {
    const list = await apiGetMyAddressList();
    addressList.value = (Array.isArray(list) ? list : []) as UserAddressItem[];

    // 默认选中地址
    if (!selectedAddressId.value) {
      const defaultAddr = addressList.value.find((a) => a.isDefault === 1);
      const firstAddr = addressList.value[0];
      selectedAddressId.value = defaultAddr?.id || firstAddr?.id || null;
    }
  } finally {
    addressLoading.value = false;
  }
}

function selectAddress(addr: UserAddressItem) {
  selectedAddressId.value = addr.id;
}

function confirmAddressSelect() {
  showAddressDialog.value = false;
}

async function saveNewAddress() {
  if (!addressFormRef.value) return;

  const valid = await addressFormRef.value.validate().catch(() => false);
  if (!valid) return;

  savingAddress.value = true;
  try {
    await apiCreateAddress({
      contactName: addressForm.contactName,
      contactPhone: addressForm.contactPhone,
      addressText: addressForm.addressText,
    });
    ElMessage.success("地址添加成功");
    showAddAddressDialog.value = false;

    // 重置表单
    addressForm.contactName = "";
    addressForm.contactPhone = "";
    addressForm.addressText = "";

    // 重新加载地址列表
    await loadAddressList();
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "添加地址失败"));
  } finally {
    savingAddress.value = false;
  }
}

async function submitOrder() {
  const userId = requireLogin("请先登录后再操作");
  if (!userId) return;

  if (!canSubmit.value) {
    if (tradeMethod.value === "DELIVERY" && !selectedAddressId.value) {
      ElMessage.warning("请先选择收货地址");
    } else {
      ElMessage.warning("请确认订单信息");
    }
    return;
  }

  submitting.value = true;
  try {
    await apiCreateOrder(productId.value, selectedAddressId.value || undefined);
    ElMessage.success("订单已创建，请前往我的买入完成支付");
    router.push("/user/mine/bought");
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, "下单失败"));
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  await loadProductDetail();
  await loadAddressList();
});
</script>

<style scoped>
.confirm-order-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  padding-bottom: 100px;
}

.page-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 16px 14px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.back-btn {
  gap: 6px;
  padding: 0;
  color: #2563eb;
  font-weight: 700;
}

.eyebrow {
  display: inline-flex;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.header-info h1 {
  margin: 4px 0 0;
  color: #0f172a;
  font-size: 26px;
  font-weight: 800;
}

.state-card {
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 22px;
  padding: 32px;
  text-align: center;
}

.state-text {
  margin: 12px 0 0;
  color: #94a3b8;
  font-size: 13px;
}

.section-card {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #e8eef6;
  border-radius: 22px;
  padding: 20px;
  margin-bottom: 14px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 10px;
  border-color: #dbe6f2;
  color: #475569;
  background: #fff;
  font-weight: 600;
  font-size: 13px;
}

.action-btn.add-btn {
  border-color: #3b82f6;
  color: #3b82f6;
}

/* 地址卡片 */
.address-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #e8eef6;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
  cursor: pointer;
  transition: all 0.2s ease;
}

.address-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.08);
}

.address-card.selected {
  border-color: #3b82f6;
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.04) 0%, #fff 100%);
}

.address-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  font-size: 20px;
}

.address-info {
  flex: 1;
  min-width: 0;
}

.address-contact {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.contact-name {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.contact-phone {
  color: #64748b;
  font-size: 13px;
}

.address-text {
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
}

.address-arrow {
  color: #94a3b8;
  font-size: 18px;
}

.no-address-tip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  border-radius: 16px;
  border: 1px dashed #d1d5db;
  background: #fafafa;
  color: #64748b;
  font-size: 14px;
}

/* 商品卡片 */
.product-card {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #e8eef6;
  background: #fff;
}

.product-image {
  width: 120px;
  height: 90px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #eef2f7;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
}

.product-img {
  width: 100%;
  height: 100%;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 12px;
}

.product-info {
  min-width: 0;
}

.product-title {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease;
}

.product-title:hover {
  color: #2563eb;
}

.product-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
}

.seller-label {
  color: #475569;
}

.view-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.product-price-section {
  text-align: right;
}

.price-label {
  color: #94a3b8;
  font-size: 11px;
  margin-bottom: 4px;
}

.price-value {
  color: #ea580c;
  font-size: 22px;
  font-weight: 800;
}

/* 交易方式 */
.trade-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #e8eef6;
  background: #fff;
}

.trade-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  font-size: 22px;
  flex-shrink: 0;
}

.delivery-icon {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.pickup-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.negotiate-icon {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.trade-content {
  flex: 1;
  min-width: 0;
}

.trade-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
}

.trade-desc {
  color: #64748b;
  font-size: 13px;
  margin-bottom: 8px;
}

.trade-location {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 13px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f8fafc;
  margin-bottom: 8px;
}

.trade-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f59e0b;
  font-size: 12px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(245, 158, 11, 0.08);
}

/* 金额区域 */
.amount-section {
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.amount-label {
  color: #64748b;
  font-size: 14px;
}

.amount-value {
  color: #475569;
  font-size: 14px;
}

.amount-value.free {
  color: #22c55e;
  font-weight: 600;
}

.amount-divider {
  height: 1px;
  background: #eef2f7;
  margin: 8px 0;
}

.total-row {
  padding-top: 12px;
}

.total-label {
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.total-value {
  color: #ea580c;
  font-size: 26px;
  font-weight: 800;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #e8eef6;
  box-shadow: 0 -4px 16px rgba(15, 23, 42, 0.06);
  z-index: 100;
}

.bottom-total {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.total-text {
  color: #64748b;
  font-size: 14px;
}

.total-amount {
  color: #ea580c;
  font-size: 24px;
  font-weight: 800;
}

.submit-btn {
  height: 48px;
  padding: 0 32px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 60%, #22c55e 100%);
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.2);
  font-size: 16px;
  font-weight: 700;
}

/* 地址列表对话框 */
.address-list-panel {
  max-height: 400px;
  overflow-y: auto;
}

.address-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #e8eef6;
  background: #fff;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.address-item:hover {
  border-color: #93c5fd;
  background: #f8faff;
}

.address-item.selected {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.04);
}

.address-item.default {
  border-color: #22c55e;
}

.address-item-content {
  flex: 1;
  min-width: 0;
}

.address-item-contact {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.address-item-contact .contact-name {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.address-item-contact .contact-phone {
  color: #64748b;
  font-size: 13px;
}

.default-tag {
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
  font-size: 11px;
  font-weight: 600;
}

.address-item-text {
  color: #475569;
  font-size: 13px;
}

.address-item-check {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #3b82f6;
  color: #fff;
}

/* 响应式 */
@media (max-width: 720px) {
  .page-container {
    padding: 12px;
  }

  .section-card {
    padding: 16px;
    border-radius: 18px;
  }

  .product-card {
    grid-template-columns: 100px 1fr;
    gap: 12px;
  }

  .product-price-section {
    grid-column: 1 / -1;
    text-align: left;
    padding-top: 12px;
    border-top: 1px solid #f1f5f9;
  }

  .price-value {
    font-size: 20px;
  }

  .total-value {
    font-size: 22px;
  }

  .submit-btn {
    height: 44px;
    padding: 0 24px;
    font-size: 15px;
  }

  .bottom-bar {
    padding: 10px 14px;
  }
}
</style>
