package com.village.revive.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 通义千问AI服务接口
 */
public interface QwenService {
    
    /**
     * 调用通义千问API生成回复
     * 
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return AI生成的回复
     */
    String chat(String systemPrompt, String userMessage);
    
    /**
     * 流式调用通义千问API
     * 
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param emitter SSE发射器
     */
    void chatStream(String systemPrompt, String userMessage, SseEmitter emitter);
}



