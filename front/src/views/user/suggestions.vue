<template>
  <div class="suggestions-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的建议</span>
          <el-button type="primary" @click="goToForum">
            <el-icon><Plus /></el-icon>
            发布新建议
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="建议标题">
          <el-input v-model="searchForm.title" placeholder="请输入建议标题" clearable style="width: 200px;" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="请选择分类" clearable style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="环境问题" value="environment" />
            <el-option label="基础设施" value="infrastructure" />
            <el-option label="农业发展" value="agriculture" />
            <el-option label="旅游发展" value="tourism" />
            <el-option label="教育文化" value="education" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 140px;">
            <el-option label="全部" :value="null" />
            <el-option label="待审核" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已拒绝" :value="2" />
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
        <el-table-column prop="title" label="建议标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110" align="center">
          <template #default="{ row }">
            <el-tag>{{ getCategoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="80" align="center" />
        <el-table-column prop="likeCount" label="点赞数" width="80" align="center" />
        <el-table-column prop="commentCount" label="评论数" width="80" align="center" />
        <el-table-column prop="createTime" label="发布时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; gap: 8px; justify-content: center;">
              <el-button link type="primary" @click="handleView(row)" size="small">查看</el-button>
              <el-button link type="success" @click="handleEdit(row)" size="small" v-if="row.status === 0">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)" size="small">删除</el-button>
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
import { forumPostAPI } from '@/api/forum'

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  title: '',
  category: '',
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
    const res = await forumPostAPI.getPostsByUser(params)
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
  searchForm.category = ''
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

// 查看
const handleView = (row) => {
  router.push(`/forum/detail/${row.id}`)
}

// 编辑
const handleEdit = (row) => {
  ElMessage.info('编辑功能暂未开放')
  // router.push(`/forum/edit/${row.id}`)
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该建议吗？删除后无法恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await forumPostAPI.deletePost(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 跳转到论坛发布页面
const goToForum = () => {
  router.push('/forum')
}

// 获取分类标签
const getCategoryLabel = (category) => {
  const map = {
    environment: '环境问题',
    infrastructure: '基础设施',
    agriculture: '农业发展',
    tourism: '旅游发展',
    education: '教育文化',
    other: '其他'
  }
  return map[category] || category
}

// 获取状态标签
const getStatusLabel = (status) => {
  const map = {
    0: '待审核',
    1: '已发布',
    2: '已拒绝'
  }
  return map[status] || '未知'
}

// 获取状态类型
const getStatusType = (status) => {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.suggestions-container {
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
