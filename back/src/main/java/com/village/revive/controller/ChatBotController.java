package com.village.revive.controller;

import com.village.revive.dto.ChatMessageDTO;
import com.village.revive.dto.ChatRequest;
import com.village.revive.dto.ChatResponse;
import com.village.revive.service.ChatHistoryService;
import com.village.revive.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * 智能客服控制器
 */
@Slf4j
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Tag(name = "智能客服", description = "智能客服相关接口")
public class ChatbotController {
    
    private final ChatbotService chatbotService;
    private final ChatHistoryService chatHistoryService;
    
    @Operation(summary = "发送消息")
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatbotService.handleMessage(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("聊天处理异常", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "抱歉，我遇到了一些问题");
            result.put("data", null);
            return ResponseEntity.ok(result);
        }
    }
    
    @Operation(summary = "流式发送消息")
    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60000L);
        
        // 异步处理
        new Thread(() -> {
            try {
                chatbotService.handleMessageStream(request, emitter);
            } catch (Exception e) {
                log.error("流式聊天处理异常", e);
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }
    
    @Operation(summary = "保存聊天消息")
    @PostMapping("/history/save")
    public ResponseEntity<Map<String, Object>> saveChatMessage(@RequestBody ChatMessageDTO message) {
        try {
            String userId = getCurrentUserId();
            chatHistoryService.saveMessage(userId, message);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("保存聊天消息失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "保存失败");
            result.put("data", null);
            return ResponseEntity.ok(result);
        }
    }
    
    @Operation(summary = "获取聊天历史")
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getChatHistory() {
        try {
            String userId = getCurrentUserId();
            List<ChatMessageDTO> history = chatHistoryService.getHistory(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", history);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "获取失败");
            result.put("data", null);
            return ResponseEntity.ok(result);
        }
    }
    
    @Operation(summary = "清空聊天历史")
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearChatHistory() {
        try {
            String userId = getCurrentUserId();
            chatHistoryService.clearHistory(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("清空聊天历史失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "清空失败");
            result.put("data", null);
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 获取当前用户ID（未登录用户使用sessionId）
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        // 未登录用户使用 "guest_" 前缀 + 随机ID（这里简化处理）
        return "guest";
    }
}





