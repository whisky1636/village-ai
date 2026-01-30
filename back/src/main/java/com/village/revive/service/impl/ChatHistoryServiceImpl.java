package com.village.revive.service.impl;

import com.village.revive.dto.ChatMessageDTO;
import com.village.revive.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 聊天历史服务实现
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String CHAT_HISTORY_PREFIX = "chat:history:";
    private static final int MAX_HISTORY_SIZE = 50; // 最多保存50条
    private static final long EXPIRE_DAYS = 7; // 7天过期
    
    @Override
    public void saveMessage(String userId, ChatMessageDTO message) {
        try {
            String key = CHAT_HISTORY_PREFIX + userId;
            message.setTimestamp(LocalDateTime.now());
            
            // 添加到列表右侧
            redisTemplate.opsForList().rightPush(key, message);
            
            // 限制列表长度
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_HISTORY_SIZE) {
                redisTemplate.opsForList().trim(key, size - MAX_HISTORY_SIZE, -1);
            }
            
            // 设置过期时间
            redisTemplate.expire(key, EXPIRE_DAYS, TimeUnit.DAYS);
            
        } catch (Exception e) {
            log.error("保存聊天记录失败", e);
        }
    }
    
    @Override
    public List<ChatMessageDTO> getHistory(String userId) {
        try {
            String key = CHAT_HISTORY_PREFIX + userId;
            List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
            
            if (list == null || list.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<ChatMessageDTO> history = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof ChatMessageDTO) {
                    history.add((ChatMessageDTO) obj);
                }
            }
            return history;
            
        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void clearHistory(String userId) {
        try {
            String key = CHAT_HISTORY_PREFIX + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("清空聊天历史失败", e);
        }
    }
}






