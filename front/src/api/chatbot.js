import request from './request'

/**
 * 发送消息给智能客服
 */
export function sendMessage(data) {
  return request({
    url: '/chatbot/chat',
    method: 'post',
    data
  })
}

/**
 * 获取聊天历史
 */
export function getChatHistory() {
  return request({
    url: '/chatbot/history',
    method: 'get'
  })
}

/**
 * 保存聊天消息
 */
export function saveChatMessage(data) {
  return request({
    url: '/chatbot/history/save',
    method: 'post',
    data
  })
}

/**
 * 清空聊天历史
 */
export function clearChatHistory() {
  return request({
    url: '/chatbot/history',
    method: 'delete'
  })
}

/**
 * 流式发送消息给智能客服（使用fetch API）
 */
export function sendMessageStream(data, onMessage, onComplete, onError) {
  const token = localStorage.getItem('token')
  
  fetch('/api/chatbot/chat-stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(data)
  })
  .then(response => {
    if (!response.ok) {
      throw new Error('Network response was not ok')
    }
    
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentEvent = null
    
    function readStream() {
      reader.read().then(({ done, value }) => {
        if (done) {
          onComplete()
          return
        }
        
        // 解码数据
        buffer += decoder.decode(value, { stream: true })
        
        // 按行分割SSE消息
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 保留最后不完整的行
        
        for (const line of lines) {
          if (line.startsWith('event:')) {
            // 保存事件类型
            currentEvent = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            const data = line.substring(5).trim()
            if (data) {
              if (currentEvent === 'cards') {
                // 解析卡片JSON
                try {
                  const cards = JSON.parse(data)
                  onMessage({ type: 'cards', data: cards })
                } catch (e) {
                  console.error('解析卡片数据失败:', e)
                }
                currentEvent = null
              } else {
                // 普通文本
                onMessage({ type: 'text', data: data })
              }
            }
          } else if (line.trim() === '') {
            // 空行表示一个完整SSE消息结束
            currentEvent = null
          }
        }
        
        readStream()
      }).catch(error => {
        console.error('读取流失败:', error)
        onError(error)
      })
    }
    
    readStream()
  })
  .catch(error => {
    console.error('请求失败:', error)
    onError(error)
  })
  
  // 返回一个可以取消的对象
  return {
    close: () => {
      // fetch API的流无法直接取消，但可以在这里做清理
    }
  }
}


