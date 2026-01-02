package com.village.revive.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.village.revive.entity.Product;
import com.village.revive.mapper.ProductMapper;
import com.village.revive.mapper.ProductReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 商品评价统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewStatsService {

    private final ProductMapper productMapper;
    private final ProductReviewMapper productReviewMapper;

    /**
     * 更新商品评价统计信息
     */
    public void updateProductReviewStats(Long productId) {
        try {
            // 获取评价统计信息
            Map<String, Object> stats = productReviewMapper.selectReviewStats(productId);
            
            // 修复类型转换：数据库返回的可能是Long类型
            Integer totalCount = stats.get("total_count") != null 
                ? ((Number) stats.get("total_count")).intValue() 
                : 0;
            BigDecimal avgRating = (BigDecimal) stats.get("avg_rating");
            
            // 更新商品表的统计信息
            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Product::getId, productId)
                    .set(Product::getReviewCount, totalCount)
                    .set(Product::getAvgRating, avgRating != null ? avgRating : BigDecimal.ZERO);
            
            productMapper.update(null, updateWrapper);
            
            log.info("Updated product review stats for product {}: count={}, avgRating={}", 
                    productId, totalCount, avgRating);
        } catch (Exception e) {
            log.error("Failed to update product review stats for product {}", productId, e);
        }
    }
}

