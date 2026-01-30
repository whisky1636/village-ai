<template>
  <div class="chatbot-container">
    <!-- 悬浮按钮 -->
    <div v-if="!isOpen" class="chatbot-button" @click="toggleChat">
      <el-icon :size="28"><ChatDotRound /></el-icon>
      <div class="button-badge" v-if="hasUnread">1</div>
    </div>

    <!-- 聊天窗口 -->
    <transition name="slide-up">
      <div v-if="isOpen" class="chatbot-window">
        <!-- 头部 -->
        <div class="chatbot-header">
          <div class="header-left">
            <el-avatar :size="36" style="background: #f5f5f5; color: #67c23a; font-size: 20px;">客</el-avatar>
            <div class="header-info">
              <div class="header-title">智能客服</div>
              <div class="header-status">在线</div>
            </div>
          </div>
          <div class="header-actions">
            <el-button link @click="toggleChat">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chatbot-messages" ref="messagesContainer">
          <!-- 欢迎消息 -->
          <div class="message-item bot-message" v-if="messages.length === 0">
            <el-avatar :size="32" style="background: #f5f5f5; color: #67c23a; font-size: 18px;">客</el-avatar>
            <div class="message-content">
              <div class="message-bubble">
                <div class="message-text">您好，我是智能客服助手，可以帮您查询景点、特产、活动等信息。</div>
              </div>
            </div>
          </div>

          <!-- 历史消息 -->
          <div 
            v-for="(msg, index) in messages" 
            :key="index" 
            class="message-item"
            :class="msg.isUser ? 'user-message' : 'bot-message'"
          >
            <!-- AI头像（左侧） -->
            <el-avatar v-if="!msg.isUser" :size="32" style="background: #f5f5f5; color: #67c23a; font-size: 18px;">客</el-avatar>
            
            <div class="message-content">
              <div class="message-bubble">
                <div class="message-text">{{ msg.text }}</div>
                
                <!-- 推荐卡片 -->
                <div v-if="msg.items && msg.items.length > 0" class="recommend-cards">
                  <div 
                    v-for="item in msg.items" 
                    :key="item.id"
                    class="recommend-card"
                    @click="handleCardClick(item)"
                  >
                    <img :src="item.image" :alt="item.title" />
                    <div class="card-content">
                      <div class="card-title">{{ item.title }}</div>
                      <div class="card-desc">{{ item.description }}</div>
                      <div class="card-price">{{ item.price }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 用户头像（右侧） -->
            <el-avatar v-if="msg.isUser" :size="32" :src="userAvatar" style="background: #f5f5f5;">
              <el-icon v-if="!userAvatar"><User /></el-icon>
            </el-avatar>
          </div>

          <!-- 加载中 -->
          <div v-if="isLoading" class="message-item bot-message">
            <el-avatar :size="32" style="background: #f5f5f5; color: #67c23a; font-size: 18px;">客</el-avatar>
            <div class="message-content">
              <div class="message-bubble loading">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷问题 -->
        <div class="quick-questions" v-if="messages.length === 0">
          <div 
            v-for="q in quickQuestions" 
            :key="q"
            class="quick-item"
            @click="sendQuickQuestion(q)"
          >
            {{ q }}
          </div>
        </div>

        <!-- 输入框 -->
        <div class="chatbot-input">
          <el-input
            v-model="inputMessage"
            placeholder="输入您的问题..."
            @keyup.enter="sendMessage"
            :disabled="isLoading"
          />
          <el-button 
            type="primary" 
            @click="sendMessage"
            :loading="isLoading"
            :disabled="!inputMessage.trim()"
          >
            发送
          </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Close, User, Delete } from '@element-plus/icons-vue'
import { sendMessageStream, getChatHistory, saveChatMessage, clearChatHistory } from '@/api/chatbot'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 响应式数据
const isOpen = ref(false)
const inputMessage = ref('')
const messages = ref([])
const isLoading = ref(false)
const hasUnread = ref(false)
const messagesContainer = ref(null)

// 用户头像
const userAvatar = computed(() => userStore.userInfo?.avatar || '')

// 快捷问题
const quickQuestions = ref([
  '推荐一些景点',
  '有什么特产',
  '最近有什么活动',
  '查看最新资讯'
])

// 切换聊天窗口
const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    hasUnread.value = false
    loadChatHistory()
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return
  
  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''
  
  // 添加用户消息
  messages.value.push({
    text: userMessage,
    isUser: true
  })
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
  
  // 保存用户消息
  try {
    await saveChatMessage({
      text: userMessage,
      isUser: true,
      timestamp: new Date().toISOString()
    })
  } catch (error) {
    console.error('保存消息失败:', error)
  }
  
  // 发送到后端
  isLoading.value = true
  
  sendMessageStream(
    { message: userMessage },
    // onMessage 回调
    (response) => {
      // 处理流式响应
      if (response.type === 'text' && response.data) {
        // 处理SSE格式的文本数据（流式）
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage && !lastMessage.isUser) {
          lastMessage.text += response.data
        } else {
          messages.value.push({
            text: response.data,
            isUser: false,
            items: []
          })
        }
        
        nextTick(() => {
          scrollToBottom()
        })
      } else if (response.type === 'cards' && response.data) {
        // 处理卡片数据
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage && !lastMessage.isUser) {
          lastMessage.items = response.data
        } else {
          // 如果还没有消息，先创建一个空消息用于放置卡片
          messages.value.push({
            text: '',
            isUser: false,
            items: response.data
          })
        }
        
        nextTick(() => {
          scrollToBottom()
        })
      } else if (response.message || response.text) {
        // 处理完整响应（非流式，兼容旧格式）
        const text = response.message || response.text
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage && !lastMessage.isUser) {
          lastMessage.text = text
          if (response.items) {
            lastMessage.items = response.items
          }
        } else {
          messages.value.push({
            text: text,
            isUser: false,
            items: response.items || []
          })
        }
        
        nextTick(() => {
          scrollToBottom()
        })
      }
    },
    // onComplete 回调
    () => {
      isLoading.value = false
      // 保存AI回复
      const lastMessage = messages.value[messages.value.length - 1]
      if (lastMessage && !lastMessage.isUser) {
        saveChatMessage({
          text: lastMessage.text,
          isUser: false,
          items: lastMessage.items || [],
          timestamp: new Date().toISOString()
        }).catch(error => {
          console.error('保存消息失败:', error)
        })
      }
    },
    // onError 回调
    (error) => {
      console.error('发送消息失败:', error)
      ElMessage.error('发送失败，请重试')
      isLoading.value = false
    }
  )
}

// 发送快捷问题
const sendQuickQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

// 处理卡片点击
const handleCardClick = (item) => {
  if (item.type === 'attraction') {
    router.push(`/attractions/${item.id}`)
  } else if (item.type === 'product') {
    router.push(`/products/${item.id}`)
  } else if (item.type === 'activity') {
    router.push(`/activities/${item.id}`)
  } else if (item.type === 'news') {
    router.push(`/news/${item.id}`)
  }
  toggleChat()
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 加载聊天历史
const loadChatHistory = async () => {
  try {
    const res = await getChatHistory()
    // 确保返回的是数组
    if (res && res.data) {
      messages.value = Array.isArray(res.data) ? res.data : []
    } else if (Array.isArray(res)) {
      messages.value = res
    } else {
      messages.value = []
    }
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载聊天历史失败:', error)
    messages.value = []
  }
}

// 清空聊天历史
const clearHistory = async () => {
  try {
    await clearChatHistory()
    messages.value = []
    ElMessage.success('聊天记录已清空')
  } catch (error) {
    console.error('清空聊天历史失败:', error)
    ElMessage.error('清空失败')
  }
}

// 组件挂载时加载历史记录
onMounted(() => {
  loadChatHistory()
})
</script>

<style scoped>
.chatbot-container {
  position: fixed;
  bottom: 0;
  right: 0;
  z-index: 1000;
}

.chatbot-button {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 60px;
  height: 60px;
  background: #67c23a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.4);
  transition: all 0.3s ease;
  color: white;
}

.chatbot-button:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(103, 194, 58, 0.6);
}

.button-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #f56c6c;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.chatbot-window {
  position: fixed;
  right: 20px;
  bottom: 90px;
  width: 40vw;
  max-width: 1200px;
  height: 600px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chatbot-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafafa;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-weight: 600;
  color: #333;
  font-size: 16px;
}

.header-status {
  font-size: 12px;
  color: #67c23a;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.chatbot-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.user-message {
  justify-content: flex-end;
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  background: #f5f5f5;
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
}

.user-message .message-bubble {
  background: #67c23a;
  color: white;
}

.message-text {
  line-height: 1.4;
  word-wrap: break-word;
}

.recommend-cards {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recommend-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e5e5;
  cursor: pointer;
  transition: all 0.2s;
}

.recommend-card:hover {
  border-color: #67c23a;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.2);
}

.recommend-card img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.card-desc {
  color: #666;
  font-size: 12px;
  line-height: 1.3;
}

.card-price {
  color: #67c23a;
  font-weight: 600;
  font-size: 14px;
}

.loading {
  display: flex;
  gap: 4px;
  align-items: center;
}

.loading span {
  width: 6px;
  height: 6px;
  background: #67c23a;
  border-radius: 50%;
  animation: loading 1.4s infinite ease-in-out;
}

.loading span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes loading {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.quick-questions {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-item {
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 20px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-item:hover {
  background: #67c23a;
  color: white;
}

.chatbot-input {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
  align-items: center;
}

.chatbot-input .el-input {
  flex: 1;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chatbot-window {
    right: 10px;
    left: 10px;
    width: auto;
    height: 70vh;
  }

  .chatbot-button {
    right: 16px;
    bottom: 16px;
  }
}
</style>

