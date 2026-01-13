<template>
  <div class="activity-detail" v-loading="loading">
    <div class="detail-container" v-if="activity">
      <!-- 活动标题区域 -->
      <div class="activity-header">
        <div class="header-left">
          <h1 class="activity-title">{{ activity.title }}</h1>
          <p class="activity-subtitle" v-if="activity.subtitle">{{ activity.subtitle }}</p>
          <div class="activity-tags">
            <el-tag>{{ getCategoryLabel(activity.category) }}</el-tag>
            <el-tag :type="getStatusType(activity.status)">{{ getStatusLabel(activity.status) }}</el-tag>
            <el-tag type="danger" v-if="activity.isFeatured">推荐活动</el-tag>
          </div>
        </div>
        <div class="header-right">
          <div class="price-box">
            <div class="price-label">报名费</div>
            <div class="price-value" v-if="activity.registrationFee > 0">
              ¥<span class="price-num">{{ activity.registrationFee }}</span>
            </div>
            <div class="price-free" v-else>免费</div>
          </div>
        </div>
      </div>

      <!-- 活动信息 -->
      <el-row :gutter="20">
        <el-col :span="16">
          <!-- 活动详情 -->
          <el-card class="detail-card">
            <template #header>
              <div class="card-header">
                <span>活动详情</span>
              </div>
            </template>
            
            <!-- 活动封面图片 -->
            <div class="activity-cover" v-if="activity.coverImage">
              <img :src="activity.coverImage" :alt="activity.title" class="cover-image" />
            </div>
            
            <!-- 活动图片集 -->
            <div class="activity-images" v-if="activityImages && activityImages.length > 0">
              <div class="images-grid">
                <div 
                  v-for="(image, index) in activityImages" 
                  :key="index" 
                  class="image-item"
                  @click="previewImage(image, index)"
                >
                  <img :src="image" :alt="`活动图片${index + 1}`" />
                </div>
              </div>
            </div>
            
            <div class="activity-content" v-html="activity.description"></div>
          </el-card>

          <!-- 报名须知 -->
          <el-card class="detail-card" style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <span>报名须知</span>
              </div>
            </template>
            <ul class="notice-list">
              <li>请确保填写的信息真实有效</li>
              <li>报名成功后，请按时参加活动</li>
              <li>如需取消报名，请提前联系主办方</li>
              <li>活动当天请携带有效身份证件</li>
            </ul>
          </el-card>
        </el-col>

        <el-col :span="8">
          <!-- 活动信息卡片 -->
          <el-card class="info-card">
            <div class="info-item">
              <div class="info-label">活动时间</div>
              <div class="info-value">
                {{ formatDateTime(activity.startTime) }} 至<br />
                {{ formatDateTime(activity.endTime) }}
              </div>
            </div>
            <div class="info-item">
              <div class="info-label">报名时间</div>
              <div class="info-value">
                {{ formatDateTime(activity.registrationStartTime) }} 至<br />
                {{ formatDateTime(activity.registrationEndTime) }}
              </div>
            </div>
            <div class="info-item">
              <div class="info-label">活动地点</div>
              <div class="info-value">{{ activity.location }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">已报名人数</div>
              <div class="info-value">
                {{ activity.currentParticipants }} / {{ activity.maxParticipants || '不限' }}
              </div>
            </div>
            <div class="info-item">
              <div class="info-label">主办方</div>
              <div class="info-value">{{ activity.organizer }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">联系方式</div>
              <div class="info-value">
                {{ activity.contactPerson }}<br />
                {{ activity.contactPhone }}
              </div>
            </div>

            <!-- 报名按钮 -->
            <div class="action-buttons">
              <el-button
                type="primary"
                size="large"
                :disabled="!canRegister || hasRegistered"
                @click="showRegistrationDialog"
                style="width: 100%"
              >
                {{ getButtonText() }}
              </el-button>
              <el-button
                v-if="hasRegistered && registration"
                size="large"
                @click="showRegistrationInfo"
                style="width: 100%; margin-top: 10px"
              >
                查看我的报名
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 报名对话框 -->
    <el-dialog v-model="registrationDialogVisible" title="活动报名" width="500px">
      <el-form :model="registrationForm" :rules="registrationRules" ref="registrationFormRef" label-width="100px">
        <el-form-item label="参与者姓名" prop="participantName">
          <el-input v-model="registrationForm.participantName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="participantPhone">
          <el-input v-model="registrationForm.participantPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="参与人数" prop="participantCount">
          <el-input-number v-model="registrationForm.participantCount" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="应付金额" v-if="activity.registrationFee > 0">
          <div class="total-fee">
            ¥{{ (activity.registrationFee * registrationForm.participantCount).toFixed(2) }}
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="registrationForm.remarks"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registrationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRegistration">
          {{ activity.registrationFee > 0 ? '确认并支付' : '确认报名' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 支付对话框 -->
    <el-dialog v-model="paymentDialogVisible" title="选择支付方式" width="400px">
      <div class="payment-info">
        <div class="amount-box">
          <span>支付金额：</span>
          <span class="amount">¥{{ paymentAmount }}</span>
        </div>
        <el-radio-group v-model="paymentMethod" class="payment-methods">
          <el-radio value="alipay" size="large">
            <span class="payment-label">支付宝支付</span>
          </el-radio>
          <el-radio value="wechat" size="large">
            <span class="payment-label">微信支付</span>
          </el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="paymentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmPayment">确认支付</el-button>
      </template>
    </el-dialog>

    <!-- 报名信息对话框 -->
    <el-dialog v-model="registrationInfoVisible" title="我的报名信息" width="500px">
      <div class="registration-info" v-if="registration">
        <div class="info-row">
          <span class="label">报名编号：</span>
          <span class="value">{{ registration.registrationNo }}</span>
        </div>
        <div class="info-row">
          <span class="label">参与者姓名：</span>
          <span class="value">{{ registration.participantName }}</span>
        </div>
        <div class="info-row">
          <span class="label">联系电话：</span>
          <span class="value">{{ registration.participantPhone }}</span>
        </div>
        <div class="info-row">
          <span class="label">参与人数：</span>
          <span class="value">{{ registration.participantCount }}</span>
        </div>
        <div class="info-row">
          <span class="label">报名状态：</span>
          <el-tag :type="getRegistrationStatusType(registration.status)">
            {{ getRegistrationStatusLabel(registration.status) }}
          </el-tag>
        </div>
        <div class="info-row" v-if="registration.registrationFee > 0">
          <span class="label">报名费用：</span>
          <span class="value">¥{{ registration.registrationFee }}</span>
        </div>
        <div class="info-row" v-if="registration.registrationFee > 0">
          <span class="label">支付状态：</span>
          <el-tag :type="registration.paymentStatus === 1 ? 'success' : 'warning'">
            {{ registration.paymentStatus === 1 ? '已支付' : '待支付' }}
          </el-tag>
        </div>
        <div class="info-row">
          <span class="label">报名时间：</span>
          <span class="value">{{ formatDateTime(registration.createdAt) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="registrationInfoVisible = false">关闭</el-button>
        <el-button
          type="primary"
          v-if="registration && registration.paymentStatus === 0 && registration.registrationFee > 0"
          @click="handleGoPay"
        >
          去支付
        </el-button>
        <el-button
          type="danger"
          v-if="registration && [1, 2].includes(registration.status)"
          @click="handleCancelRegistration"
        >
          取消报名
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElImageViewer } from 'element-plus'
import {
  getActivityDetail,
  checkRegistration,
  createRegistration,
  cancelRegistration,
  payRegistrationFee
} from '@/api/activity'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activity = ref(null)
const hasRegistered = ref(false)
const registration = ref(null)

// 活动图片
const activityImages = ref([])

// 图片预览
const previewImage = (image, index) => {
  // 使用Element Plus的图片预览功能
  const imageList = activityImages.value.map(img => ({ url: img }))
  ElImageViewer({
    urlList: imageList,
    initialIndex: index
  })
}

// 报名对话框
const registrationDialogVisible = ref(false)
const registrationFormRef = ref(null)
const registrationForm = reactive({
  activityId: null,
  participantName: '',
  participantPhone: '',
  participantCount: 1,
  remarks: ''
})

const registrationRules = {
  participantName: [{ required: true, message: '请输入参与者姓名', trigger: 'blur' }],
  participantPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  participantCount: [{ required: true, message: '请选择参与人数', trigger: 'change' }]
}

// 报名信息对话框
const registrationInfoVisible = ref(false)

// 支付对话框
const paymentDialogVisible = ref(false)
const paymentMethod = ref('alipay')
const paymentAmount = ref(0)
const pendingRegistrationId = ref(null)

// 是否可以报名
const canRegister = computed(() => {
  if (!activity.value) return false
  return activity.value.status === 2
})

// 获取活动详情
const fetchActivityDetail = async () => {
  loading.value = true
  try {
    const res = await getActivityDetail(route.params.id)
    activity.value = res.data
    registrationForm.activityId = activity.value.id
    
    // 处理活动图片
    if (activity.value.images) {
      try {
        activityImages.value = JSON.parse(activity.value.images)
      } catch (e) {
        // 如果不是JSON格式，可能是单个图片URL
        activityImages.value = [activity.value.images]
      }
    } else {
      activityImages.value = []
    }
  } catch (error) {
    ElMessage.error('获取活动详情失败')
  } finally {
    loading.value = false
  }
}

// 检查报名状态
const checkRegistrationStatus = async () => {
  if (!userStore.isLoggedIn) return
  
  try {
    const res = await checkRegistration(route.params.id)
    hasRegistered.value = res.data.hasRegistered
    registration.value = res.data.registration
  } catch (error) {
    console.error('检查报名状态失败', error)
  }
}

// 显示报名对话框
const showRegistrationDialog = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  registrationDialogVisible.value = true
}

// 提交报名
const handleSubmitRegistration = async () => {
  const valid = await registrationFormRef.value.validate()
  if (!valid) return

  try {
    const res = await createRegistration(registrationForm)
    registrationDialogVisible.value = false
    
    // 如果有报名费，弹出支付对话框
    if (activity.value.registrationFee > 0) {
      ElMessage.success('报名信息提交成功，请完成支付')
      pendingRegistrationId.value = res.data.id
      paymentAmount.value = res.data.registrationFee
      paymentDialogVisible.value = true
    } else {
      ElMessage.success('报名成功')
      checkRegistrationStatus()
      fetchActivityDetail()
    }
  } catch (error) {
    ElMessage.error(error.message || '报名失败')
  }
}

// 显示报名信息
const showRegistrationInfo = () => {
  registrationInfoVisible.value = true
}

// 去支付
const handleGoPay = () => {
  registrationInfoVisible.value = false
  pendingRegistrationId.value = registration.value.id
  paymentAmount.value = registration.value.registrationFee
  paymentDialogVisible.value = true
}

// 确认支付
const handleConfirmPayment = async () => {
  try {
    await payRegistrationFee(pendingRegistrationId.value, { paymentMethod: paymentMethod.value })
    ElMessage.success('支付成功')
    paymentDialogVisible.value = false
    checkRegistrationStatus()
    fetchActivityDetail()
  } catch (error) {
    ElMessage.error(error.message || '支付失败')
  }
}

// 取消报名
const handleCancelRegistration = async () => {
  try {
    await ElMessageBox.confirm('确定要取消报名吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelRegistration(registration.value.id)
    ElMessage.success('取消成功')
    registrationInfoVisible.value = false
    checkRegistrationStatus()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

// 获取按钮文字
const getButtonText = () => {
  if (hasRegistered.value) return '已报名'
  if (!canRegister.value) return '暂不可报名'
  return '立即报名'
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 获取类型标签
const getCategoryLabel = (category) => {
  const map = {
    festival: '节庆活动',
    picking: '采摘体验',
    culture: '文化活动',
    sports: '体育活动',
    other: '其他'
  }
  return map[category] || category
}

// 获取状态标签
const getStatusLabel = (status) => {
  const map = {
    0: '草稿',
    1: '已发布',
    2: '报名中',
    3: '报名结束',
    4: '活动进行中',
    5: '已结束',
    6: '已取消'
  }
  return map[status] || '未知'
}

// 获取状态类型
const getStatusType = (status) => {
  const map = {
    0: 'info',
    1: 'success',
    2: 'primary',
    3: 'warning',
    4: 'primary',
    5: 'info',
    6: 'danger'
  }
  return map[status] || 'info'
}

// 获取报名状态标签
const getRegistrationStatusLabel = (status) => {
  const map = {
    1: '待审核',
    2: '已通过',
    3: '已拒绝',
    4: '已取消'
  }
  return map[status] || '未知'
}

// 获取报名状态类型
const getRegistrationStatusType = (status) => {
  const map = {
    1: 'warning',
    2: 'success',
    3: 'danger',
    4: 'info'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchActivityDetail()
  checkRegistrationStatus()
})
</script>

<style scoped lang="scss">
.activity-detail {
  padding: 20px;
  min-height: calc(100vh - 200px);

  .detail-container {
    max-width: 1200px;
    margin: 0 auto;

    .activity-header {
      background: white;
      padding: 30px;
      border-radius: 8px;
      margin-bottom: 20px;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;

      .header-left {
        flex: 1;

        .activity-title {
          font-size: 28px;
          font-weight: bold;
          margin: 0 0 10px 0;
          color: #333;
        }

        .activity-subtitle {
          font-size: 16px;
          color: #666;
          margin: 0 0 15px 0;
        }

        .activity-tags {
          .el-tag {
            margin-right: 10px;
          }
        }
      }

      .header-right {
        .price-box {
          text-align: center;
          padding: 20px;
          background: #fff;
          border: 1px solid #e5e5e5;
          border-radius: 4px;

          .price-label {
            font-size: 14px;
            color: #999;
            margin-bottom: 8px;
          }

          .price-value {
            font-size: 16px;
            color: #e6423f;

            .price-num {
              font-size: 28px;
              font-weight: 600;
            }
          }

          .price-free {
            font-size: 24px;
            font-weight: 600;
            color: #67c23a;
          }
        }
      }
    }

    .detail-card {
      .card-header {
        font-weight: bold;
        font-size: 16px;
      }

      .activity-cover {
        margin-bottom: 20px;
        
        .cover-image {
          width: 100%;
          height: 300px;
          object-fit: cover;
          border-radius: 8px;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }
      }
      
      .activity-images {
        margin-bottom: 20px;
        
        .images-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
          gap: 16px;
          
          .image-item {
            cursor: pointer;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            transition: transform 0.3s ease, box-shadow 0.3s ease;
            
            &:hover {
              transform: translateY(-4px);
              box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
            }
            
            img {
              width: 100%;
              height: 150px;
              object-fit: cover;
              display: block;
            }
          }
        }
      }

      .activity-content {
        line-height: 1.8;
        color: #666;
      }

      .notice-list {
        margin: 0;
        padding-left: 20px;

        li {
          margin-bottom: 10px;
          line-height: 1.8;
          color: #666;
        }
      }
    }

    .info-card {
      position: sticky;
      top: 20px;

      .info-item {
        margin-bottom: 20px;
        padding-bottom: 20px;
        border-bottom: 1px solid #f0f0f0;

        &:last-of-type {
          border-bottom: none;
        }

        .info-label {
          font-size: 14px;
          color: #999;
          margin-bottom: 8px;
        }

        .info-value {
          font-size: 14px;
          color: #333;
          line-height: 1.6;
        }
      }

      .action-buttons {
        margin-top: 20px;
      }
    }
  }

  .registration-info {
    .info-row {
      display: flex;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;

      .label {
        width: 100px;
        color: #999;
        font-size: 14px;
      }

      .value {
        flex: 1;
        color: #333;
        font-size: 14px;
      }
    }
  }
}

.total-fee {
  font-size: 24px;
  color: #ff6b6b;
  font-weight: bold;
}

.payment-info {
  .amount-box {
    text-align: center;
    padding: 30px 0;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 30px;

    span:first-child {
      color: #666;
      font-size: 14px;
    }

    .amount {
      font-size: 36px;
      color: #ff6b6b;
      font-weight: bold;
      margin-left: 10px;
    }
  }

  .payment-methods {
    width: 100%;
    
    :deep(.el-radio) {
      width: 100%;
      margin: 0;
      padding: 20px;
      border: 1px solid #dcdfe6;
      border-radius: 8px;
      margin-bottom: 15px;
      display: flex;
      align-items: center;

      &:hover {
        border-color: #409eff;
      }

      &.is-checked {
        border-color: #409eff;
        background-color: #ecf5ff;
      }
    }

    .payment-label {
      font-size: 16px;
      margin-left: 10px;
    }
  }
}
</style>
    


