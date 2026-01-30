package com.village.revive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;



/**
 * 聊天响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /**
     * 回复消息
     */
    private String message;
    
    /**
     * 消息类型：text-纯文本, card-卡片
     */
    private String type;
    
    /**
     * 推荐项列表（景点/特产/活动）
     */
    private List<RecommendItem> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendItem {
        /**
         * ID
         */
        private Long id;
        
        /**
         * 类型：attraction-景点, product-特产, activity-活动, news-资讯
         */
        private String type;
        
        /**
         * 标题
         */
        private String title;
        
        /**
         * 描述
         */
        private String description;
        
        /**
         * 图片
         */
        private String image;
        
        /**
         * 价格（特产/活动）
         */
        private String price;
        
        /**
         * 跳转路径
         */
        private String path;
    }
}













