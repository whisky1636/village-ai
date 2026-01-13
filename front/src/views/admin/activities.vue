<template>
  <div class="activities-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>活动管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            创建活动
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="活动标题">
          <el-input v-model="searchForm.title" placeholder="请输入活动标题" clearable style="width: 200px;" />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="searchForm.category" placeholder="请选择" clearable style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="节庆活动" value="festival" />
            <el-option label="采摘体验" value="picking" />
            <el-option label="文化活动" value="culture" />
            <el-option label="体育活动" value="sports" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 140px;">
            <el-option label="全部" :value="null" />
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="报名中" :value="2" />
            <el-option label="报名结束" :value="3" />
            <el-option label="活动进行中" :value="4" />
            <el-option label="已结束" :value="5" />
            <el-option label="已取消" :value="6" />
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
        <el-table-column label="封面" width="90" align="center">
          <template #default="{ row }">
            <el-image 
              v-if="row.coverImage" 
              :src="row.coverImage" 
              :preview-src-list="[row.coverImage]"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px;"
            />
              <span v-else style="color: #999;">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="活动标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="活动类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag>{{ getCategoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="活动地点" width="130" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="currentParticipants" label="报名人数" width="90" align="center">
          <template #default="{ row }">
            {{ row.currentParticipants }} / {{ row.maxParticipants || '不限' }}
          </template>
        </el-table-column>
        <el-table-column prop="isFeatured" label="推荐" width="60" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isFeatured" type="danger" size="small">是</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; gap: 8px; justify-content: center; flex-wrap: wrap;">
              <el-button link type="primary" @click="handleEdit(row)" size="small">编辑</el-button>
              <el-button link type="success" v-if="row.status === 0" @click="handlePublish(row)" size="small">发布</el-button>
              <el-dropdown v-if="row.status > 0 && row.status < 6" @command="(cmd) => handleStatusChange(row.id, cmd)" size="small">
                <el-button link type="primary" size="small">
                  状态<el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="2" v-if="row.status !== 2">设为报名中</el-dropdown-item>
                    <el-dropdown-item :command="3" v-if="row.status !== 3">设为报名结束</el-dropdown-item>
                    <el-dropdown-item :command="6">取消活动</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button link type="primary" @click="handleRegistrations(row)" size="small">报名</el-button>
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

    <!-- 创建/编辑活动对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="70%"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="活动标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入活动标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动副标题" prop="subtitle">
              <el-input v-model="form.subtitle" placeholder="请输入活动副标题" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="活动类型" prop="category">
              <el-select v-model="form.category" placeholder="请选择活动类型" style="width: 100%">
                <el-option label="节庆活动" value="festival" />
                <el-option label="采摘体验" value="picking" />
                <el-option label="文化活动" value="culture" />
                <el-option label="体育活动" value="sports" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动地点" prop="location">
              <el-input v-model="form.location" placeholder="请输入活动地点" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="封面图片" prop="coverImage">
          <el-upload
            class="cover-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            :before-upload="beforeUpload"
          >
            <img v-if="form.coverImage" :src="form.coverImage" class="cover-image" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸400x600，支持jpg/png格式，大小不超过2MB</div>
        </el-form-item>

        <el-form-item label="活动图片集" prop="images">
          <el-upload
            class="images-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :file-list="imageFileList"
            list-type="picture-card"
            :on-success="handleImagesSuccess"
            :on-remove="handleImageRemove"
            :before-upload="beforeUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">可上传多张图片，支持jpg/png格式，每张不超过2MB</div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选择结束时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报名开始时间" prop="registrationStartTime">
              <el-date-picker
                v-model="form.registrationStartTime"
                type="datetime"
                placeholder="选择报名开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名截止时间" prop="registrationEndTime">
              <el-date-picker
                v-model="form.registrationEndTime"
                type="datetime"
                placeholder="选择报名截止时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="最大参与人数" prop="maxParticipants">
              <el-input-number v-model="form.maxParticipants" :min="0" placeholder="0表示不限" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="报名费用(元)" prop="registrationFee">
              <el-input-number v-model="form.registrationFee" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" placeholder="数值越大越靠前" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="是否推荐" prop="isFeatured">
              <el-switch v-model="form.isFeatured" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number v-model="form.longitude" :precision="6" :step="0.000001" placeholder="请输入经度" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number v-model="form.latitude" :precision="6" :step="0.000001" placeholder="请输入纬度" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="主办方" prop="organizer">
              <el-input v-model="form.organizer" placeholder="请输入主办方" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="活动详情" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            placeholder="请输入活动详情"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowDown } from '@element-plus/icons-vue'
import {
  getActivityPage,
  createActivity,
  updateActivity,
  deleteActivity,
  publishActivity,
  cancelActivity
} from '@/api/activity'
import { useRouter } from 'vue-router'

const uploadUrl = '/api/file/upload'
const uploadHeaders = {
  'Authorization': `Bearer ${sessionStorage.getItem('token')}`
}

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

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  subtitle: '',
  category: '',
  coverImage: '',
  images: '',
  location: '',
  longitude: null,
  latitude: null,
  startTime: '',
  endTime: '',
  registrationStartTime: '',
  registrationEndTime: '',
  maxParticipants: 0,
  registrationFee: 0,
  organizer: '',
  contactPerson: '',
  contactPhone: '',
  description: '',
  isFeatured: 0,
  sortOrder: 0
})

// 图片文件列表（用于展示）
const imageFileList = ref([])

// 表单验证规则
const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  registrationStartTime: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  registrationEndTime: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }]
}

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

// 新增
const handleAdd = () => {
  dialogTitle.value = '创建活动'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogTitle.value = '编辑活动'
  Object.assign(form, row)
  
  // 处理图片集回显
  if (row.images) {
    try {
      const images = JSON.parse(row.images)
      imageFileList.value = images.map((url, index) => ({
        name: `image-${index}`,
        url: url,
        response: { code: 200, data: url }
      }))
    } catch (e) {
      imageFileList.value = []
    }
  } else {
    imageFileList.value = []
  }
  
  dialogVisible.value = true
}

// 查看
const handleView = (row) => {
  router.push(`/admin/activities/${row.id}`)
}

// 提交表单
const handleSubmit = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return

  try {
    if (form.id) {
      await updateActivity(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createActivity(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 发布
const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm('确定要发布该活动吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await publishActivity(row.id)
    ElMessage.success('发布成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('发布失败')
    }
  }
}

// 修改活动状态
const handleStatusChange = async (id, status) => {
  try {
    if (status === 6) {
      await ElMessageBox.confirm('确定要取消该活动吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await cancelActivity(id)
      ElMessage.success('已取消活动')
    } else {
      // 直接更新状态
      const activity = tableData.value.find(a => a.id === id)
      if (activity) {
        await updateActivity(id, { ...activity, status })
        ElMessage.success('状态更新成功')
      }
    }
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该活动吗？删除后无法恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteActivity(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 报名管理
const handleRegistrations = (row) => {
  router.push(`/admin/activity-registrations?activityId=${row.id}`)
}

// 封面图片上传成功
const handleCoverSuccess = (response, file) => {
  if (response.code === 200) {
    form.coverImage = response.data
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 活动图片集上传成功
const handleImagesSuccess = (response, file, fileList) => {
  if (response.code === 200) {
    // 收集所有已上传的图片URL
    const imageUrls = fileList
      .filter(f => f.response && f.response.code === 200)
      .map(f => f.response.data)
    form.images = JSON.stringify(imageUrls)
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 移除图片
const handleImageRemove = (file, fileList) => {
  const imageUrls = fileList
    .filter(f => f.response && f.response.code === 200)
    .map(f => f.response.data)
  form.images = JSON.stringify(imageUrls)
}

// 上传前检查
const beforeUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.title = ''
  form.subtitle = ''
  form.category = ''
  form.coverImage = ''
  form.images = ''
  form.location = ''
  form.longitude = null
  form.latitude = null
  form.startTime = ''
  form.endTime = ''
  form.registrationStartTime = ''
  form.registrationEndTime = ''
  form.maxParticipants = 0
  form.registrationFee = 0
  form.organizer = ''
  form.contactPerson = ''
  form.contactPhone = ''
  form.description = ''
  form.isFeatured = 0
  form.sortOrder = 0
  imageFileList.value = []
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

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.activities-container {
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

.cover-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: border-color 0.3s;

    &:hover {
      border-color: #409eff;
    }
  }
}

.cover-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
  line-height: 178px;
}

.cover-image {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.images-uploader {
  :deep(.el-upload-list__item) {
    transition: all 0.3s;
  }
}
</style>



