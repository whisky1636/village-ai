package com.village.revive.dto;

import lombok.Data;

/**
 * 聊天请求DTO
 */
@Data
public class ChatRequest {
    /**
     * 用户消息
     */
    private String message;
    
    /**
     * 会话ID（可选，用于保持上下文）
     */
    private String sessionId;
}















