package com.village.revive.service.impl;

import com.village.revive.service.QwenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 通义千问AI服务实现
 */
@Slf4j
@Service
public class QwenServiceImpl implements QwenService {
    
    @Value("${qwen.api-key:}")
    private String apiKey;
    
    @Value("${qwen.api-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String apiUrl;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public QwenServiceImpl() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String chat(String systemPrompt, String userMessage) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen-plus"); // 使用qwen-plus模型
            
            Map<String, Object> input = new HashMap<>();
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            );
            input.put("messages", messages);
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message");
            requestBody.put("parameters", parameters);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            // 构建请求
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();
            
            // 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("通义千问API调用失败: {}", response.code());
                    return "抱歉，我现在有点忙，请稍后再试~";
                }
                
                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                // 解析响应
                if (jsonNode.has("output") && jsonNode.get("output").has("choices")) {
                    JsonNode choices = jsonNode.get("output").get("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode firstChoice = choices.get(0);
                        if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                            return firstChoice.get("message").get("content").asText();
                        }
                    }
                }
                
                log.error("通义千问API响应格式异常: {}", responseBody);
                return "抱歉，我理解出现了问题，请换个方式问我吧~";
            }
        } catch (IOException e) {
            log.error("调用通义千问API异常", e);
            return "抱歉，网络出现了问题，请稍后再试~";
        } catch (Exception e) {
            log.error("通义千问服务异常", e);
            return "抱歉，我遇到了一些问题，请稍后再试~";
        }
    }
    
    @Override
    public void chatStream(String systemPrompt, String userMessage, SseEmitter emitter) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen-plus");
            
            Map<String, Object> input = new HashMap<>();
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            );
            input.put("messages", messages);
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message");
            parameters.put("incremental_output", true);
            requestBody.put("parameters", parameters);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            // 构建SSE请求
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-DashScope-SSE", "enable")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();
            
            // 发送流式请求
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                emitter.send(SseEmitter.event().data("抱歉，服务暂时不可用"));
                emitter.complete();
                return;
            }
            
            // 逐行读取SSE流
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
            String line;
            String lastContent = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if (data.equals("[DONE]")) {
                        break;
                    }
                    
                    try {
                        JsonNode jsonNode = objectMapper.readTree(data);
                        if (jsonNode.has("output") && jsonNode.get("output").has("choices")) {
                            JsonNode choices = jsonNode.get("output").get("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode firstChoice = choices.get(0);
                                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                                    String content = firstChoice.get("message").get("content").asText();
                                    // 通义千问的incremental_output返回完整内容，我们只发送新增部分
                                    if (content.startsWith(lastContent)) {
                                        String newContent = content.substring(lastContent.length());
                                        if (!newContent.isEmpty()) {
                                            emitter.send(SseEmitter.event().data(newContent));
                                            lastContent = content;
                                        }
                                    } else {
                                        // 如果不是增量，直接发送
                                        emitter.send(SseEmitter.event().data(content));
                                        lastContent = content;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析SSE消息失败: {}", data);
                    }
                }
            }
            
            // 不立即关闭连接，让调用者决定何时关闭
            // emitter.complete();
        } catch (Exception e) {
            log.error("流式调用通义千问异常", e);
            try {
                emitter.send(SseEmitter.event().data("抱歉，出现了一些问题"));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
    }
}

