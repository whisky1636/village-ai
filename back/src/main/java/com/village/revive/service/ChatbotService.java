package com.village.revive.service;

import com.village.revive.dto.ChatRequest;
import com.village.revive.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 智能客服服务接口
 */
public interface ChatbotService {
    
    /**
     * 处理用户消息
     * 
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse handleMessage(ChatRequest request);
    
    /**
     * 流式处理用户消息
     * 
     * @param request 聊天请求
     * @param emitter SSE发射器
     */
    void handleMessageStream(ChatRequest request, SseEmitter emitter);
}

