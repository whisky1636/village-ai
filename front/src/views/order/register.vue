<template>
  <div class="register-container">
    <div class="register-card">
      <div class="logo-container">
        <h1>智慧乡村振兴</h1>
      </div>
      <h3>用户注册</h3>
      
      <el-form :model="registerForm" :rules="rules" ref="registerFormRef" label-position="top">
        <el-form-item prop="username">
          <el-input 
            v-model="registerForm.username" 
            placeholder="请输入用户名" 
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="registerForm.password" 
            type="password" 
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input 
            v-model="registerForm.confirmPassword" 
            type="password" 
            placeholder="请确认密码"
            size="large"
            :prefix-icon="Lock"
          />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item prop="email">
              <el-input 
                v-model="registerForm.email" 
                placeholder="请输入邮箱"
                size="large"
                :prefix-icon="Message"
              />
            </el-form-item>
          </el-col>          
          <el-col :span="24">
            <el-form-item prop="phoneNumber">
              <el-input 
                v-model="registerForm.phoneNumber" 
                placeholder="请输入手机号码（选填）"
                size="large"
                :prefix-icon="Phone"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item prop="verifyCode">
  <!-- 布局：输入框 + 发送按钮 -->
  <el-row :gutter="1">
    <el-col :span="22">
      <el-input 
        v-model="registerForm.verifyCode" 
        placeholder="请输入验证码"
        size="large"
        :prefix-icon="Postcard"
        :disabled="false"
      />
    </el-col>
    <el-col :span="1">
      <el-button 
        type="primary" 
        size="large" 
        class="verify-code-btn"
        :disabled="!canGetCode" 
        @click="sendVerifyCode"
      >
        {{ codeText }} <!-- 动态显示“发送验证码”/“60s后重新发送” -->
      </el-button>
    </el-col>
  </el-row>
</el-form-item>
        <el-button 
          type="primary" 
          class="register-btn" 
          @click="handleRegister" 
          :loading="loading"
          size="large"
          round
        >
          注册
        </el-button>
      </el-form>
      
      <div class="login-link">
        <router-link to="/login">已有账号？立即登录</router-link>
      </div>
    </div>
    
    <div class="footer">
      &copy; {{ new Date().getFullYear() }} 智慧乡村振兴
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Phone } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { sendVerifyCodeAPI } from '@/api/user'

// 声明表单引用和状态
const registerFormRef = ref(null)
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
// 验证码相关状态
const canGetCode = ref(true) // 是否可点击发送按钮
const codeText = ref('发送验证码') // 按钮文字
let countdownTimer = null // 倒计时定时器
// 表单数据
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phoneNumber: '',
  verifyCode: '',
  role: 'USER' // 默认为普通用户角色
})
const sendVerifyCode = async () => {
  // 1. 校验邮箱/手机号是否填写（根据实际业务调整）
  if (!registerForm.email) {
    ElMessage.warning('请先填写邮箱！')
    return
  }

  try {
    // 2. 调用发送验证码接口
    const res = await sendVerifyCodeAPI({ email: registerForm.email })
    if (res.code === 200) {
      ElMessage.success('验证码已发送，请注意查收！')
      
      // 3. 启动60秒倒计时
      let countdown = 60
      canGetCode.value = false
      codeText.value = `${countdown}s后重新发送`
      
      countdownTimer = setInterval(() => {
        countdown--
        codeText.value = `${countdown}s后重新发送`
        // 倒计时结束恢复状态
        if (countdown <= 0) {
          clearInterval(countdownTimer)
          canGetCode.value = true
          codeText.value = '发送验证码'
        }
      }, 1000)
    }
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败，请重试！')
  }
}
// 验证确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

// 邮箱格式验证
const validateEmail = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入邮箱'))
    return
  }
  
  const emailRegex = /^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)*\.[a-zA-Z0-9]{2,6}$/
  if (!emailRegex.test(value)) {
    callback(new Error('请输入正确的邮箱格式'))
  } else {
    callback()
  }
}

// 手机号码验证
const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback()
    return
  }
  
  const phoneRegex = /^1[3456789]\d{9}$/
  if (!phoneRegex.test(value)) {
    callback(new Error('请输入正确的手机号码'))
  } else {
    callback()
  }
}

// 表单规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  phoneNumber: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码长度为6位', trigger: 'blur' }
  ]
}

// 注册方法
const handleRegister = () => {
  if (registerFormRef.value) {
    registerFormRef.value.validate(async (valid) => {
      if (valid) {
        try {
          loading.value = true
          
          // 确保两次密码一致
          if (registerForm.password !== registerForm.confirmPassword) {
            ElMessage.error('两次密码输入不一致')
            loading.value = false
            return
          }
          
          // 克隆表单数据，确保所有字段都包含在请求中
          const registerData = JSON.parse(JSON.stringify(registerForm))
          
          await userStore.registerAction(registerData)
          ElMessage.success('注册成功，请登录')
          router.push('/login')
        } catch (error) {
          console.error('注册失败:', error)
          ElMessage.error(error.message || '注册失败，请稍后再试')
        } finally {
          loading.value = false
        }
      } else {
        ElMessage.error('请填写完整的注册信息')
        return false
      }
    })
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f1 100%);
  padding: 40px 0;
  position: relative;
}

.register-card {
  width: 450px;
  padding: 40px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.register-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
}

.logo-container {
  text-align: center;
  margin-bottom: 20px;
}

h1 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 10px;
  background: linear-gradient(90deg, #409EFF, #67C23A);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

h3 {
  text-align: center;
  margin-bottom: 30px;
  color: #606266;
  font-weight: normal;
  font-size: 18px;
}

.register-btn {
  width: 100%;
  margin-top: 20px;
  height: 50px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 1px;
}

.login-link, .back-link {
  text-align: center;
  margin-top: 25px;
}

.login-link a, .back-link a {
  color: #409EFF;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}

.login-link a:hover, .back-link a:hover {
  color: #66b1ff;
}

.footer {
  position: absolute;
  bottom: 20px;
  color: #909399;
  font-size: 13px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  border-radius: 25px;
  height: 50px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409EFF inset !important;
}

:deep(.el-form-item__label) {
  padding-bottom: 5px;
  font-weight: 500;
}
.verify-code-btn {
    /* 1. 移除背景色，设置为透明 */
    background-color: transparent !important; 
    
    /* 2. 移除边框，防止边框颜色干扰透明效果 */
    border: none !important; 
    
    /* 3. 设置文字颜色，使其在透明背景下清晰可见 */
    /* 保持与主题颜色一致或使用黑色/深灰色 */
    color: var(--el-color-primary) !important; 
    
    /* 4. 可选：移除鼠标悬停时的背景色 */
    /* 针对悬停/聚焦状态 */
    height: 100%;
}
</style> 