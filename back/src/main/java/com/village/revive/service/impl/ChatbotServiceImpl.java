package com.village.revive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.village.revive.dto.*;
import com.village.revive.entity.Activity;
import com.village.revive.service.*;
import com.village.revive.dto.*;
import com.village.revive.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能客服服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {
    
    private final QwenService qwenService;
    private final AttractionService attractionService;
    private final ProductService productService;
    private final ActivityService activityService;
    private final NewsService newsService;
    
    @Override
    public ChatResponse handleMessage(ChatRequest request) {
        String message = request.getMessage().toLowerCase();
        
        // 意图识别
        if (containsKeywords(message, "景点", "游玩", "旅游", "开放时间", "门票")) {
            return handleAttractionQuery(request.getMessage());
        } else if (containsKeywords(message, "特产", "商品", "购买", "买", "产品")) {
            return handleProductQuery(request.getMessage());
        } else if (containsKeywords(message, "推荐") && containsKeywords(message, "特产", "商品", "产品")) {
            return handleProductRecommendation(request.getMessage());
        } else if (containsKeywords(message, "推荐") && containsKeywords(message, "景点", "游玩", "旅游")) {
            return handleAttractionRecommendation(request.getMessage());
        } else if (containsKeywords(message, "活动", "报名", "参加")) {
            return handleActivityQuery(request.getMessage());
        } else if (containsKeywords(message, "资讯", "新闻", "动态")) {
            return handleNewsQuery(request.getMessage());
        } else {
            return handleGeneralChat(request.getMessage());
        }
    }
    
    /**
     * 处理景点查询
     */
    private ChatResponse handleAttractionQuery(String message) {
        List<AttractionDTO> attractions = attractionService.getRecommendAttractions(5);
        
        if (attractions == null || attractions.isEmpty()) {
            return ChatResponse.builder()
                    .type("text")
                    .message("抱歉，暂时没有景点信息哦~")
                    .build();
        }
        
        // 构建提示词
        String attractionInfo = attractions.stream()
                .map(a -> String.format("- %s：%s，开放时间：%s，门票：¥%s",
                        a.getName(), a.getDescription(), a.getOpeningHours(), a.getTicketPrice()))
                .collect(Collectors.joining("\n"));
        
        String systemPrompt = "你是桃源智界的智能客服，热情友好。当前可用景点信息：\n" + attractionInfo +
                "\n请基于以上信息回答用户问题。如果用户问题简单，简短回复即可；如果用户需要详细信息，提供完整的介绍。";
        
        String aiReply = qwenService.chat(systemPrompt, message);
        
        // 构建推荐卡片
        List<ChatResponse.RecommendItem> items = attractions.stream()
                .map(a -> ChatResponse.RecommendItem.builder()
                        .id(a.getId())
                        .type("attraction")
                        .title(a.getName())
                        .description(a.getDescription())
                        .image(a.getCoverImage())
                        .price("门票：¥" + a.getTicketPrice())
                        .path("/attractions/" + a.getId())
                        .build())
                .collect(Collectors.toList());
        
        return ChatResponse.builder()
                .type("card")
                .message(aiReply)
                .items(items)
                .build();
    }
    
    /**
     * 处理特产查询
     */
    private ChatResponse handleProductQuery(String message) {
        List<ProductDTO> products = productService.getFeaturedProducts(5);
        
        if (products == null || products.isEmpty()) {
            return ChatResponse.builder()
                    .type("text")
                    .message("抱歉，暂时没有特产信息哦~")
                    .build();
        }
        
        String productInfo = products.stream()
                .map(p -> String.format("- %s：%s，价格：¥%s",
                        p.getName(), p.getDescription(), p.getPrice()))
                .collect(Collectors.joining("\n"));
        
        String systemPrompt = "你是桃源智界的智能客服，热情友好。当前可用特产信息：\n" + productInfo +
                "\n请基于以上信息回答用户问题。根据问题详细程度灵活回复，简单问题简短回答，复杂问题提供详细信息。";
        
        String aiReply = qwenService.chat(systemPrompt, message);
        
        List<ChatResponse.RecommendItem> items = products.stream()
                .map(p -> ChatResponse.RecommendItem.builder()
                        .id(p.getId())
                        .type("product")
                        .title(p.getName())
                        .description(p.getDescription())
                        .image(p.getCoverImage())
                        .price("¥" + p.getPrice())
                        .path("/products/" + p.getId())
                        .build())
                .collect(Collectors.toList());
        
        return ChatResponse.builder()
                .type("card")
                .message(aiReply)
                .items(items)
                .build();
    }
    
    /**
     * 处理特产推荐
     */
    private ChatResponse handleProductRecommendation(String message) {
        List<ProductDTO> products = productService.getFeaturedProducts(4);
        
        if (products == null || products.isEmpty()) {
            products = productService.getHotProducts(4);
        }
        
        if (products == null || products.isEmpty()) {
            return ChatResponse.builder()
                    .type("text")
                    .message("抱歉，暂时没有推荐的特产哦~")
                    .build();
        }
        
        String productInfo = products.stream()
                .map(p -> String.format("- %s：%s，¥%s",
                        p.getName(), p.getDescription(), p.getPrice()))
                .collect(Collectors.joining("\n"));
        
        String systemPrompt = "你是桃源智界的智能客服。为用户推荐以下特产：\n" + productInfo +
                "\n请用热情的语气介绍这些特产的特点，突出乡村特色和产品优势。";
        
        String aiReply = qwenService.chat(systemPrompt, "请推荐一些特产");
        
        List<ChatResponse.RecommendItem> items = products.stream()
                .map(p -> ChatResponse.RecommendItem.builder()
                        .id(p.getId())
                        .type("product")
                        .title(p.getName())
                        .description(p.getDescription())
                        .image(p.getCoverImage())
                        .price("¥" + p.getPrice())
                        .path("/products/" + p.getId())
                        .build())
                .collect(Collectors.toList());
        
        return ChatResponse.builder()
                .type("card")
                .message(aiReply)
                .items(items)
                .build();
    }
    
    /**
     * 处理景点推荐
     */
    private ChatResponse handleAttractionRecommendation(String message) {
        List<AttractionDTO> attractions = attractionService.getRecommendAttractions(4);
        
        if (attractions == null || attractions.isEmpty()) {
            attractions = attractionService.getHotAttractions(4);
        }
        
        if (attractions == null || attractions.isEmpty()) {
            return ChatResponse.builder()
                    .type("text")
                    .message("抱歉，暂时没有推荐的景点哦~")
                    .build();
        }
        
        String attractionInfo = attractions.stream()
                .map(a -> String.format("- %s：%s",
                        a.getName(), a.getDescription()))
                .collect(Collectors.joining("\n"));
        
        String systemPrompt = "你是桃源智界的智能客服。为用户推荐以下景点：\n" + attractionInfo +
                "\n请用热情的语气介绍这些景点的特色，帮助用户做出选择。";
        
        String aiReply = qwenService.chat(systemPrompt, "请推荐一些景点");
        
        List<ChatResponse.RecommendItem> items = attractions.stream()
                .map(a -> ChatResponse.RecommendItem.builder()
                        .id(a.getId())
                        .type("attraction")
                        .title(a.getName())
                        .description(a.getDescription())
                        .image(a.getCoverImage())
                        .price("门票：¥" + a.getTicketPrice())
                        .path("/attractions/" + a.getId())
                        .build())
                .collect(Collectors.toList());
        
        return ChatResponse.builder()
                .type("card")
                .message(aiReply)
                .items(items)
                .build();
    }
    
    /**
     * 处理活动查询
     */
    private ChatResponse handleActivityQuery(String message) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getStatus, 2).last("LIMIT 4");
        List<Activity> activities = activityService.list(wrapper);
        
        if (activities.isEmpty()) {
            return ChatResponse.builder()
                    .type("text")
                    .message("抱歉，当前没有可报名的活动哦~")
                    .build();
        }
        
        String activityInfo = activities.stream()
                .map(a -> String.format("- %s：%s，报名费：¥%s",
                        a.getTitle(), a.getDescription(), a.getRegistrationFee()))
                .collect(Collectors.joining("\n"));
        
        String systemPrompt = "你是桃源智界的智能客服。当前可报名活动：\n" + activityInfo +
                "\n请基于以上信息回答用户问题。根据用户需求提供合适长度的回复，确保信息完整。";
        
        String aiReply = qwenService.chat(systemPrompt, message);
        
        List<ChatResponse.RecommendItem> items = activities.stream()
                .map(a -> ChatResponse.RecommendItem.builder()
                        .id(a.getId())
                        .type("activity")
                        .title(a.getTitle())
                        .description(a.getDescription())
                        .image(a.getCoverImage())
                        .price(a.getRegistrationFee() != null && a.getRegistrationFee().compareTo(BigDecimal.ZERO) > 0 
                                ? "¥" + a.getRegistrationFee() : "免费")
                        .path("/activities/" + a.getId())
                        .build())
                .collect(Collectors.toList());
        
        return ChatResponse.builder()
                .type("card")
                .message(aiReply)
                .items(items)
                .build();
    }
    
    /**
     * 处理资讯查询
     */
    private ChatResponse handleNewsQuery(String message) {
        List<NewsDTO> newsList = newsService.getFeaturedNews(4);
        
        if (newsList == null || newsList.isEmpty()) {
            newsList = newsService.getHotNews(4);
        }
        
        if (newsList == null || newsList.isEmpty()) {
            return ChatResponse.builder()
                    .type("text")
                    .message("抱歉，暂时没有最新资讯哦~")
                    .build();
        }
        
        String newsInfo = newsList.stream()
                .map(n -> String.format("- %s：%s", n.getTitle(), n.getSummary()))
                .collect(Collectors.joining("\n"));
        
        String systemPrompt = "你是桃源智界的智能客服。最新资讯：\n" + newsInfo +
                "\n请基于以上资讯回答用户问题，提供有价值的信息。";
        
        String aiReply = qwenService.chat(systemPrompt, message);
        
        List<ChatResponse.RecommendItem> items = newsList.stream()
                .map(n -> ChatResponse.RecommendItem.builder()
                        .id(n.getId())
                        .type("news")
                        .title(n.getTitle())
                        .description(n.getSummary())
                        .image(n.getCoverImage())
                        .path("/news/" + n.getId())
                        .build())
                .collect(Collectors.toList());
        
        return ChatResponse.builder()
                .type("card")
                .message(aiReply)
                .items(items)
                .build();
    }
    
    /**
     * 处理通用对话
     */
    private ChatResponse handleGeneralChat(String message) {
        String systemPrompt = "你是桃源智界的智能客服助手。" +
                "你可以帮助用户查询景点、特产、活动和资讯信息。" +
                "你的回复要友好亲切，富有乡村特色。" +
                "根据用户问题的复杂程度灵活调整回复长度：" +
                "简单问候或确认，简短回复即可；" +
                "咨询具体信息时，提供详细准确的答案；" +
                "需要建议或推荐时，给出充分的理由和说明。";
        
        String aiReply = qwenService.chat(systemPrompt, message);
        
        return ChatResponse.builder()
                .type("text")
                .message(aiReply)
                .build();
    }
    
    /**
     * 关键词匹配
     */
    private boolean containsKeywords(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void handleMessageStream(ChatRequest request, SseEmitter emitter) {
        String message = request.getMessage().toLowerCase();
        
        try {
            // 准备推荐卡片数据（但不立即发送）
            List<ChatResponse.RecommendItem> recommendItems = new ArrayList<>();
            String systemPrompt;
            
            if (containsKeywords(message, "景点", "游玩", "旅游", "开放时间", "门票")) {
                List<AttractionDTO> attractions = attractionService.getRecommendAttractions(5);
                if (attractions != null && !attractions.isEmpty()) {
                    recommendItems = attractions.stream()
                            .map(a -> ChatResponse.RecommendItem.builder()
                                    .id(a.getId()).type("attraction").title(a.getName())
                                    .description(a.getDescription()).image(a.getCoverImage())
                                    .price("门票：¥" + a.getTicketPrice())
                                    .path("/attractions/" + a.getId()).build())
                            .collect(Collectors.toList());
                    
                    String attractionInfo = attractions.stream()
                            .map(a -> String.format("- %s：%s，开放时间：%s，门票：¥%s",
                                    a.getName(), a.getDescription(), a.getOpeningHours(), a.getTicketPrice()))
                            .collect(Collectors.joining("\n"));
                    systemPrompt = "你是桃源智界的智能客服。当前可用景点信息：\n" + attractionInfo +
                            "\n请基于以上信息回答用户问题。根据问题详细程度灵活回复。";
                } else {
                    systemPrompt = "你是桃源智界的智能客服助手。回复友好亲切，富有乡村特色。";
                }
            } else if (containsKeywords(message, "特产", "商品", "购买", "买", "产品") || 
                      (containsKeywords(message, "推荐") && containsKeywords(message, "特产", "商品", "产品"))) {
                // 检查用户是否明确询问特产/商品信息
                boolean shouldShowCards = containsKeywords(message, "推荐", "有什么", "哪些", "介绍", "看看", "看看", "展示", "显示") ||
                                        containsKeywords(message, "特产", "商品",  "酒","产品") && 
                                        (containsKeywords(message, "好吃", "美味", "好喝", "特色", "地方", "当地") ||
                                         containsKeywords(message, "价格", "多少钱", "怎么卖"));
                
                if (shouldShowCards) {
                    // 根据用户需求筛选商品类型
                    List<ProductDTO> allProducts = productService.getFeaturedProducts(10);
                    List<ProductDTO> filteredProducts = new ArrayList<>();
                    
                    if (allProducts != null && !allProducts.isEmpty()) {
                        // 根据用户关键词筛选商品
                        if (containsKeywords(message, "好吃", "美味", "吃", "食品", "食物", "零食", "蜂蜜", "米酒", "茶叶", "茶", "酒")) {
                            // 推荐食品类商品
                            filteredProducts = allProducts.stream()
                                    .filter(p -> containsKeywords(p.getName(), "蜂蜜", "米酒", "茶叶", "茶", "酒", "果", "蜜", "糖", "饼", "糕", "干", "脆") ||
                                               containsKeywords(p.getDescription(), "食品", "食物", "可食用", "美味", "香甜", "醇香", "清香"))
                                    .limit(5)
                                    .collect(Collectors.toList());
                        } else if (containsKeywords(message, "手工艺品", "工艺品", "编织", "竹编", "篮子", "框", "装饰", "摆件")) {
                            // 推荐手工艺品类商品
                            filteredProducts = allProducts.stream()
                                    .filter(p -> containsKeywords(p.getName(), "竹编", "篮子", "框", "工艺品", "手工艺", "编织") ||
                                               containsKeywords(p.getDescription(), "手工", "编织", "装饰", "摆件", "工艺品"))
                                    .limit(5)
                                    .collect(Collectors.toList());
                        } else if (containsKeywords(message, "农产品", "蔬菜", "水果", "新鲜", "有机", "绿色")) {
                            // 推荐农产品类商品
                            filteredProducts = allProducts.stream()
                                    .filter(p -> containsKeywords(p.getName(), "蔬菜", "水果", "农产品", "有机", "绿色") ||
                                               containsKeywords(p.getDescription(), "新鲜", "有机", "绿色", "农产品", "蔬菜", "水果"))
                                    .limit(5)
                                    .collect(Collectors.toList());
                        } else {
                            // 默认推荐所有商品
                            filteredProducts = allProducts.stream().limit(5).collect(Collectors.toList());
                        }
                    }
                    
                    if (!filteredProducts.isEmpty()) {
                        recommendItems = filteredProducts.stream()
                                .map(p -> ChatResponse.RecommendItem.builder()
                                        .id(p.getId()).type("product").title(p.getName())
                                        .description(p.getDescription()).image(p.getCoverImage())
                                        .price("¥" + p.getPrice())
                                        .path("/products/" + p.getId()).build())
                                .collect(Collectors.toList());
                        
                        String productInfo = filteredProducts.stream()
                                .map(p -> String.format("- %s：%s，价格：¥%s",
                                        p.getName(), p.getDescription(), p.getPrice()))
                                .collect(Collectors.joining("\n"));
                        systemPrompt = "你是桃源智界的智能客服。当前可用特产信息：\n" + productInfo +
                                "\n请基于以上信息回答用户问题。根据问题详细程度灵活回复。";
                    } else {
                        systemPrompt = "你是桃源智界的智能客服助手。回复友好亲切，富有乡村特色。";
                    }
                } else {
                    // 用户只是提到特产但没有明确要求推荐，只提供文字回复
                    List<ProductDTO> products = productService.getFeaturedProducts(3);
                    if (products != null && !products.isEmpty()) {
                        String productInfo = products.stream()
                                .map(p -> String.format("- %s：%s，价格：¥%s",
                                        p.getName(), p.getDescription(), p.getPrice()))
                                .collect(Collectors.joining("\n"));
                        systemPrompt = "你是桃源智界的智能客服。当前可用特产信息：\n" + productInfo +
                                "\n请基于以上信息回答用户问题，但不要主动推荐所有商品，只回答用户具体询问的内容。";
                    } else {
                        systemPrompt = "你是桃源智界的智能客服助手。回复友好亲切，富有乡村特色。";
                    }
                }
            } else if (containsKeywords(message, "活动", "报名", "参加")) {
                LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Activity::getStatus, 2).last("LIMIT 5");
                List<Activity> activities = activityService.list(wrapper);
                if (activities != null && !activities.isEmpty()) {
                    recommendItems = activities.stream()
                            .map(a -> ChatResponse.RecommendItem.builder()
                                    .id(a.getId()).type("activity").title(a.getTitle())
                                    .description(a.getDescription()).image(a.getCoverImage())
                                    .price(a.getRegistrationFee() != null && a.getRegistrationFee().compareTo(BigDecimal.ZERO) > 0 
                                            ? "¥" + a.getRegistrationFee() : "免费")
                                    .path("/activities/" + a.getId()).build())
                            .collect(Collectors.toList());
                    
                    String activityInfo = activities.stream()
                            .map(a -> String.format("- %s：%s，报名费：%s",
                                    a.getTitle(), a.getDescription(), 
                                    a.getRegistrationFee() != null && a.getRegistrationFee().compareTo(BigDecimal.ZERO) > 0 
                                            ? "¥" + a.getRegistrationFee() : "免费"))
                            .collect(Collectors.joining("\n"));
                    systemPrompt = "你是桃源智界的智能客服。当前可报名活动：\n" + activityInfo +
                            "\n请基于以上信息回答用户问题。根据问题详细程度灵活回复。";
                } else {
                    systemPrompt = "你是桃源智界的智能客服助手。回复友好亲切，富有乡村特色。";
                }
            } else if (containsKeywords(message, "资讯", "新闻", "动态", "消息")) {
                List<NewsDTO> newsList = newsService.getFeaturedNews(5);
                if (newsList == null || newsList.isEmpty()) {
                    newsList = newsService.getHotNews(5);
                }
                if (newsList != null && !newsList.isEmpty()) {
                    recommendItems = newsList.stream()
                            .map(n -> ChatResponse.RecommendItem.builder()
                                    .id(n.getId()).type("news").title(n.getTitle())
                                    .description(n.getSummary()).image(n.getCoverImage())
                                    .path("/news/" + n.getId()).build())
                            .collect(Collectors.toList());
                    
                    String newsInfo = newsList.stream()
                            .map(n -> String.format("- %s：%s", n.getTitle(), n.getSummary()))
                            .collect(Collectors.joining("\n"));
                    systemPrompt = "你是桃源智界的智能客服。最新资讯：\n" + newsInfo +
                            "\n请基于以上资讯回答用户问题，提供有价值的信息。";
                } else {
                    systemPrompt = "你是桃源智界的智能客服助手。回复友好亲切，富有乡村特色。";
                }
            } else {
                systemPrompt = "你是桃源智界的智能客服助手。" +
                        "你可以帮助用户查询景点、特产、活动和资讯信息。" +
                        "你的回复要友好亲切，富有乡村特色。" +
                        "根据用户问题的复杂程度灵活调整回复长度。";
            }
            
            // 流式输出AI回复
            qwenService.chatStream(systemPrompt, request.getMessage(), emitter);
            
            // AI回复完成后，发送推荐卡片（如果有）
            if (!recommendItems.isEmpty()) {
                // 等待一小段时间确保文字输出完成
                Thread.sleep(500);
                emitter.send(SseEmitter.event().name("cards").data(recommendItems));
            }
            
        } catch (IOException e) {
            log.error("发送流式消息失败", e);
            emitter.completeWithError(e);
        } catch (InterruptedException e) {
            log.error("等待发送卡片失败", e);
            Thread.currentThread().interrupt();
        }
    }
}

