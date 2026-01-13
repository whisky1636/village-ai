<template>
  <div class="activities-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.title"
        placeholder="搜索活动名称"
        clearable
        style="max-width: 300px"
        @change="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select
        v-model="searchForm.category"
        placeholder="活动类型"
        clearable
        @change="handleSearch"
        style="width: 150px; margin-left: 10px"
      >
        <el-option label="全部类型" value="" />
        <el-option label="节庆活动" value="festival" />
        <el-option label="采摘体验" value="picking" />
        <el-option label="文化活动" value="culture" />
        <el-option label="体育活动" value="sports" />
        <el-option label="其他" value="other" />
      </el-select>
    </div>

    <!-- 活动列表 -->
    <div class="activities-list" v-loading="loading">
      <div class="activity-card" v-for="activity in activities" :key="activity.id" @click="goToDetail(activity.id)">
        <div class="activity-image">
          <img :src="activity.coverImage || '/default-activity.jpg'" :alt="activity.title" />
          <div class="activity-tag" v-if="activity.isFeatured">
            <el-tag type="danger" size="small">推荐</el-tag>
          </div>
          <div class="activity-status">
            <el-tag :type="getStatusType(activity.status)" size="small">{{ getStatusLabel(activity.status) }}</el-tag>
          </div>
        </div>
        <div class="activity-info">
          <h3 class="activity-title">{{ activity.title }}</h3>
          <p class="activity-subtitle" v-if="activity.subtitle">{{ activity.subtitle }}</p>
          <div class="activity-meta">
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>{{ formatDate(activity.startTime) }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Location /></el-icon>
              <span>{{ activity.location }}</span>
            </div>
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>{{ activity.currentParticipants }} / {{ activity.maxParticipants || '不限' }}</span>
            </div>
          </div>
          <div class="activity-footer">
            <div class="activity-price">
              <span class="price-label">报名费：</span>
              <span class="price-value" v-if="activity.registrationFee > 0">¥{{ activity.registrationFee }}</span>
              <span class="price-free" v-else>免费</span>
            </div>
            <el-button type="primary" size="small" @click.stop="handleRegister(activity)" :disabled="!canRegister(activity)">
              {{ getButtonText(activity) }}
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && activities.length === 0" description="暂无活动" />
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Calendar, Location, User } from '@element-plus/icons-vue'
import { getActivityPage } from '@/api/activity'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const searchForm = reactive({
  title: '',
  category: '',
  status: null // 显示所有活动
})

const activities = ref([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({
  current: 1,
  size: 12
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
    const res = await getActivityPage(params)
    activities.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取活动列表错误:', error)
    ElMessage.error('获取活动列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

// 跳转详情
const goToDetail = (id) => {
  router.push(`/activities/${id}`)
}

// 报名
const handleRegister = (activity) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push(`/activities/${activity.id}`)
}

// 是否可以报名
const canRegister = (activity) => {
  return activity.status === 2
}

// 获取按钮文字
const getButtonText = (activity) => {
  if (activity.status === 0) return '未发布'
  if (activity.status === 1) return '未开始'
  if (activity.status === 2) return '立即报名'
  if (activity.status === 3) return '报名结束'
  if (activity.status === 4) return '进行中'
  if (activity.status === 5) return '已结束'
  if (activity.status === 6) return '已取消'
  return '查看详情'
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

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.activities-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;

  .search-bar {
    margin-bottom: 30px;
    display: flex;
    align-items: center;
  }

  .activities-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
    min-height: 400px;

    .activity-card {
      background: white;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
      }

      .activity-image {
        position: relative;
        width: 100%;
        height: 180px;
        overflow: hidden;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .activity-tag {
          position: absolute;
          top: 10px;
          left: 10px;
        }

        .activity-status {
          position: absolute;
          top: 10px;
          right: 10px;
        }
      }

      .activity-info {
        padding: 15px;

        .activity-title {
          font-size: 16px;
          font-weight: bold;
          color: #333;
          margin: 0 0 8px 0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .activity-subtitle {
          font-size: 13px;
          color: #666;
          margin: 0 0 10px 0;
          line-height: 1.4;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        }

        .activity-meta {
          .meta-item {
            display: flex;
            align-items: center;
            font-size: 13px;
            color: #999;
            margin-bottom: 8px;

            .el-icon {
              margin-right: 5px;
            }

            span {
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
          }
        }

        .activity-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 15px;
          padding-top: 15px;
          border-top: 1px solid #f0f0f0;

          .activity-price {
            .price-label {
              font-size: 13px;
              color: #999;
            }

            .price-value {
              font-size: 18px;
              font-weight: bold;
              color: #ff6b6b;
            }

            .price-free {
              font-size: 16px;
              font-weight: bold;
              color: #4caf50;
            }
          }
        }
      }
    }
  }

  .pagination {
    margin-top: 30px;
    display: flex;
    justify-content: center;
  }
}
</style>
