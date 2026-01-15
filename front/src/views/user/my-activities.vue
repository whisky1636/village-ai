<template>
  <div class="my-activities-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的活动</span>
          <el-button type="primary" @click="goToActivities">
            <el-icon><Plus /></el-icon>
            浏览更多活动
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="活动名称">
          <el-input v-model="searchForm.title" placeholder="请输入活动名称" clearable style="width: 200px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 140px;">
            <el-option label="全部" :value="null" />
            <el-option label="待开始" value="pending" />
            <el-option label="进行中" value="ongoing" />
            <el-option label="已结束" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="activityTitle" label="活动名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="activityLocation" label="活动地点" width="150" show-overflow-tooltip />
        <el-table-column prop="registrationStatus" label="报名状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getRegistrationStatusType(row.registrationStatus)" size="small">
              {{ getRegistrationStatusLabel(row.registrationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="activityStatus" label="活动状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getActivityStatusType(row.activityStatus)" size="small">
              {{ getActivityStatusLabel(row.activityStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="registrationTime" label="报名时间" width="160" />
        <el-table-column prop="activityStartTime" label="活动开始时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; gap: 8px; justify-content: center;">
              <el-button link type="primary" @click="handleView(row)" size="small">查看详情</el-button>
              <el-button 
                link 
                type="danger" 
                @click="handleCancel(row)" 
                size="small" 
                v-if="canCancel(row)"
              >
                取消报名
              </el-button>
            </div>
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
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyActivityRegistrations, cancelActivityRegistration } from '@/api/activity'

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  title: '',
  status: null
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

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    }
    const res = await getMyActivityRegistrations(params)
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
  searchForm.title = ''
  searchForm.status = null
  handleSearch()
}

// 分页变化
const handleSizeChange = () => {
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

// 查看详情
const handleView = (row) => {
  router.push(`/activities/${row.activityId}`)
}

// 取消报名
const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确定要取消报名吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelActivityRegistration(row.id)
    ElMessage.success('取消报名成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消报名失败')
    }
  }
}

// 跳转到活动页面
const goToActivities = () => {
  router.push('/activities')
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

// 获取活动状态标签
const getActivityStatusLabel = (status) => {
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

// 获取活动状态类型
const getActivityStatusType = (status) => {
  const map = {
    0: 'info',
    1: 'info',
    2: 'success',
    3: 'warning',
    4: 'success',
    5: 'warning',
    6: 'danger'
  }
  return map[status] || 'info'
}

// 判断是否可以取消报名
const canCancel = (row) => {
  // 只有报名状态为"已通过"且活动状态为"报名中"或"报名结束"时才能取消
  return row.registrationStatus === 2 && (row.activityStatus === 2 || row.activityStatus === 3)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.my-activities-container {
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
