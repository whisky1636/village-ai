<template>
  <div class="product-reviews-container">
    <div class="page-header">
      <h2>订单评价管理</h2>
      <p>管理用户对商品的评价</p>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.productName" placeholder="请输入商品名称" clearable />
        </el-form-item>
        <el-form-item label="评价人">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="评分">
          <el-select v-model="searchForm.rating" placeholder="请选择评分" clearable>
            <el-option label="5星" :value="5" />
            <el-option label="4星" :value="4" />
            <el-option label="3星" :value="3" />
            <el-option label="2星" :value="2" />
            <el-option label="1星" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="商品信息" width="250">
          <template #default="scope">
            <div class="product-info">
              <el-image 
                v-if="scope.row.productImage"
                :src="scope.row.productImage" 
                style="width: 60px; height: 60px; margin-right: 10px"
                fit="cover"
              />
              <div>
                <div>{{ scope.row.productName }}</div>
                <div class="text-gray">订单号: {{ scope.row.orderNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="评价人" width="100" />
        <el-table-column label="评分" width="120">
          <template #default="scope">
            <el-rate v-model="scope.row.rating" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="评价图片" width="120">
          <template #default="scope">
            <div v-if="scope.row.images && JSON.parse(scope.row.images).length > 0" class="images-preview">
              <el-image 
                v-for="(img, index) in JSON.parse(scope.row.images).slice(0, 3)" 
                :key="index"
                :src="img" 
                :preview-src-list="JSON.parse(scope.row.images)"
                style="width: 30px; height: 30px; margin-right: 5px"
                fit="cover"
              />
            </div>
            <span v-else class="text-gray">无</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success">已通过</el-tag>
            <el-tag v-else type="danger">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="评价时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button 
              v-if="scope.row.status === 0" 
              type="success" 
              size="small" 
              @click="handleAudit(scope.row, 1)"
            >
              通过
            </el-button>
            <el-button 
              v-if="scope.row.status === 0" 
              type="danger" 
              size="small" 
              @click="handleAudit(scope.row, 2)"
            >
              拒绝
            </el-button>
            <el-button 
              type="primary" 
              size="small" 
              @click="handleReply(scope.row)"
            >
              回复
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 回复对话框 -->
    <el-dialog v-model="replyDialogVisible" title="商家回复" width="500px">
      <el-form :model="replyForm" label-width="80px">
        <el-form-item label="评价内容">
          <div class="review-content">
            <el-rate v-model="replyForm.rating" disabled size="small" />
            <p>{{ replyForm.content }}</p>
          </div>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="replyForm.replyContent"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import reviewAPI from '@/api/review'

// 搜索表单
const searchForm = reactive({
  productName: '',
  username: '',
  rating: null,
  status: null
})

// 分页数据
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 回复对话框
const replyDialogVisible = ref(false)
const replyForm = reactive({
  id: null,
  rating: 0,
  content: '',
  replyContent: ''
})

// 获取评价列表
const fetchReviews = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    const res = await reviewAPI.getReviewPage(params)
    if (res.code === 200) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error(error.message || '获取评价列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchReviews()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    productName: '',
    username: '',
    rating: null,
    status: null
  })
  handleSearch()
}

// 审核
const handleAudit = async (row, status) => {
  const text = status === 1 ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${text}该评价吗？`, '提示', {
      type: 'warning'
    })
    await reviewAPI.auditReview(row.id, status)
    ElMessage.success(`${text}成功`)
    fetchReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `${text}失败`)
    }
  }
}

// 打开回复对话框
const handleReply = (row) => {
  replyForm.id = row.id
  replyForm.rating = row.rating
  replyForm.content = row.content
  replyForm.replyContent = row.replyContent || ''
  replyDialogVisible.value = true
}

// 提交回复
const handleSubmitReply = async () => {
  if (!replyForm.replyContent.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  try {
    await reviewAPI.replyReview({
      reviewId: replyForm.id,
      replyContent: replyForm.replyContent
    })
    ElMessage.success('回复成功')
    replyDialogVisible.value = false
    fetchReviews()
  } catch (error) {
    ElMessage.error(error.message || '回复失败')
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该评价吗？', '提示', {
      type: 'warning'
    })
    await reviewAPI.deleteReview(row.id)
    ElMessage.success('删除成功')
    fetchReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 分页
const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchReviews()
}

const handleCurrentChange = (page) => {
  pagination.pageNum = page
  fetchReviews()
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString()
}

onMounted(() => {
  fetchReviews()
})
</script>

<style scoped>
.product-reviews-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 24px;
  margin: 0 0 8px 0;
  color: #333;
}

.page-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.product-info {
  display: flex;
  align-items: center;
}

.images-preview {
  display: flex;
  flex-wrap: wrap;
}

.text-gray {
  color: #999;
  font-size: 12px;
  margin-top: 5px;
}

.review-content {
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
}

.review-content p {
  margin: 10px 0 0 0;
  color: #333;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>









