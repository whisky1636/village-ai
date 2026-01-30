package com.village.revive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 是否用户消息
     */
    private Boolean isUser;
    
    /**
     * 消息文本
     */
    private String text;
    
    /**
     * 推荐卡片列表
     */
    private List<ChatResponse.RecommendItem> items;
    
    /**
     * 消息时间
     */
    private LocalDateTime timestamp;
}















