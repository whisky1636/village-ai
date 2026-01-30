package com.village.revive.service;

import java.util.List;


import com.village.revive.dto.ChatMessageDTO;

/**
 * 聊天历史服务接口
 */
public interface ChatHistoryService {
    
    /**
     * 保存聊天消息
     */
    void saveMessage(String userId, ChatMessageDTO message);
    
    /**
     * 获取聊天历史
     */
    List<ChatMessageDTO> getHistory(String userId);
    
    /**
     * 清空聊天历史
     */
    void clearHistory(String userId);
}













