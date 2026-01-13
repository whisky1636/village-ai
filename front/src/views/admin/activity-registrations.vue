<template>
  <div class="registrations-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>活动报名管理</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="报名状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="待审核" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已拒绝" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="searchForm.paymentStatus" placeholder="请选择" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已退款" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column prop="registrationNo" label="报名编号" width="150" />
        <el-table-column prop="participantName" label="参与者" width="100" />
        <el-table-column prop="participantPhone" label="手机号" width="120" />
        <el-table-column prop="participantCount" label="人数" width="80" />
        <el-table-column prop="registrationFee" label="费用" width="100">
          <template #default="{ row }">
            ¥{{ row.registrationFee }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="报名状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paymentStatus" label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getPaymentType(row.paymentStatus)" size="small">{{ getPaymentLabel(row.paymentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkInStatus" label="签到" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.checkInStatus" type="success" size="small">已签到</el-tag>
            <el-tag v-else type="info" size="small">未签到</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="报名时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" size="small" v-if="row.status === 1" @click="handleAudit(row, 2)">通过</el-button>
            <el-button link type="danger" size="small" v-if="row.status === 1" @click="handleAudit(row, 3)">拒绝</el-button>
            <el-button link type="primary" size="small" v-if="row.status === 2 && !row.checkInStatus" @click="handleCheckIn(row)">签到</el-button>
            <el-button link type="info" size="small" @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="报名详情" width="60%">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="报名编号">{{ currentRow.registrationNo }}</el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ currentRow.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="参与者姓名">{{ currentRow.participantName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentRow.participantPhone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentRow.participantIdcard || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="参与人数">{{ currentRow.participantCount }}</el-descriptions-item>
        <el-descriptions-item label="报名费用">¥{{ currentRow.registrationFee }}</el-descriptions-item>
        <el-descriptions-item label="报名状态">
          <el-tag :type="getStatusType(currentRow.status)">{{ getStatusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="getPaymentType(currentRow.paymentStatus)">{{ getPaymentLabel(currentRow.paymentStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ currentRow.paymentMethod || '未支付' }}</el-descriptions-item>
        <el-descriptions-item label="签到状态">
          <el-tag v-if="currentRow.checkInStatus" type="success">已签到</el-tag>
          <el-tag v-else type="info">未签到</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remarks || '无' }}</el-descriptions-item>
        <el-descriptions-item label="管理员备注" :span="2">{{ currentRow.adminRemarks || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRegistrationPage, auditRegistration, checkIn } from '@/api/activity'

const route = useRoute()
const activityId = ref(route.query.activityId)

// 搜索表单
const searchForm = reactive({
  status: null,
  paymentStatus: null
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 详情
const detailVisible = ref(false)
const currentRow = ref(null)

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      activityId: activityId.value,
      ...searchForm
    }
    const res = await getRegistrationPage(params)
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

// 重置
const handleReset = () => {
  searchForm.status = null
  searchForm.paymentStatus = null
  handleSearch()
}

// 审核
const handleAudit = async (row, status) => {
  try {
    const { value: adminRemarks } = await ElMessageBox.prompt(
      status === 2 ? '审核通过，可填写备注' : '拒绝原因',
      '审核',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea'
      }
    )
    
    await auditRegistration(row.id, { status, adminRemarks })
    ElMessage.success('审核成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('审核失败')
    }
  }
}

// 签到
const handleCheckIn = async (row) => {
  try {
    await ElMessageBox.confirm('确认签到？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    await checkIn(row.id, { registrationNo: row.registrationNo })
    ElMessage.success('签到成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('签到失败')
    }
  }
}

// 查看详情
const handleView = (row) => {
  currentRow.value = row
  detailVisible.value = true
}

// 获取状态标签
const getStatusLabel = (status) => {
  const map = { 1: '待审核', 2: '已通过', 3: '已拒绝', 4: '已取消' }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = { 1: 'warning', 2: 'success', 3: 'danger', 4: 'info' }
  return map[status] || 'info'
}

const getPaymentLabel = (status) => {
  const map = { 0: '待支付', 1: '已支付', 2: '已退款' }
  return map[status] || '未知'
}

const getPaymentType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || 'info'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.registrations-container {
  padding: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>

