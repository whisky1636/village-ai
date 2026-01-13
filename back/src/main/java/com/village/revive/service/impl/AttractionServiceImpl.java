package com.village.revive.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.village.revive.dto.AttractionDTO;
import com.village.revive.dto.CategoryDTO;
import com.village.revive.dto.PageRequest;
import com.village.revive.entity.Attraction;
import com.village.revive.entity.AttractionCategory;
import com.village.revive.exception.ServiceException;
import com.village.revive.mapper.AttractionCategoryMapper;
import com.village.revive.mapper.AttractionMapper;
import com.village.revive.service.AttractionService;
import com.village.revive.utils.BeanCopyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttractionServiceImpl implements AttractionService {
    private final AttractionMapper attractionMapper;
    private final AttractionCategoryMapper categoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 缓存键常量
    private static final String ATTRACTION_CATEGORIES_CACHE_KEY = "attraction:categories:enabled";
    private static final String ATTRACTION_CATEGORY_CACHE_PREFIX = "attraction:category:id:";
    
    @Override
    public IPage<AttractionDTO> getAttractionPage(PageRequest pageRequest, Long categoryId, String keyword, Integer status) {
        Page<Attraction> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        IPage<AttractionDTO> pageResult = attractionMapper.selectAttractionPage(page, categoryId, keyword, status);
        return pageResult;
    }

    @Override
    public AttractionDTO getAttractionById(Long id) {
        Attraction attraction = attractionMapper.selectById(id);
        if (attraction == null || attraction.getDeleted()) {
            throw new ServiceException("景点不存在");
        }

        AttractionDTO dto = BeanCopyUtils.copyBean(attraction, AttractionDTO.class);
        // 设置分类名称
        if (attraction.getCategoryId() != null) {
            AttractionCategory category = categoryMapper.selectById(attraction.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getName());
            }
        }
        log.info("获取景点信息成功：{}", dto);
        return dto;
    }

    @Override
    public AttractionDTO createAttraction(AttractionDTO attractionDTO) {
        // 验证分类是否存在
        if (attractionDTO.getCategoryId() != null) {
            AttractionCategory category = categoryMapper.selectById(attractionDTO.getCategoryId());
            if (category == null || category.getStatus() != 1) {
                throw new ServiceException("景点分类不存在或已禁用");
            }
        }
        Attraction attraction = BeanCopyUtils.copyBean(attractionDTO, Attraction.class);
        attraction.setCreatedAt(LocalDateTime.now());
        attraction.setUpdatedAt(LocalDateTime.now());
        attraction.setDeleted(false);
        attraction.setViewCount(0);
        attraction.setRating(attractionDTO.getRating() != null ? attractionDTO.getRating() : java.math.BigDecimal.ZERO);

        attractionMapper.insert(attraction);
        return BeanCopyUtils.copyBean(attraction, AttractionDTO.class);
    }

    @Override
    @Transactional
    public AttractionDTO updateAttraction(Long id, AttractionDTO attractionDTO) {
        Attraction existingAttraction = attractionMapper.selectById(id);
        if (existingAttraction == null || existingAttraction.getDeleted()) {
            throw new ServiceException("景点不存在");
        }
        // 验证分类是否存在
        if (attractionDTO.getCategoryId() != null) {
            AttractionCategory category = categoryMapper.selectById(attractionDTO.getCategoryId());
            if (category == null || category.getStatus() != 1) {
                throw new ServiceException("景点分类不存在或已禁用");
            }
        }

        Attraction attraction = BeanCopyUtils.copyBean(attractionDTO, Attraction.class);
        attraction.setId(id);
        attraction.setUpdatedAt(LocalDateTime.now());

        attractionMapper.updateById(attraction);
        return getAttractionById(id);
    }

    @Override
    @Transactional
    public void deleteAttraction(Long id) {
        Attraction attraction = attractionMapper.selectById(id);
        if (attraction == null || attraction.getDeleted()) {
            throw new ServiceException("景点不存在");
        }

        attraction.setDeleted(true);
        attraction.setUpdatedAt(LocalDateTime.now());
        log.info("删除景点成功：{}", attraction);
        attractionMapper.deleteById(attraction);
    }

    @Override
    public List<AttractionDTO> getRecommendAttractions(Integer limit) {
        List<Attraction> attractions = attractionMapper.selectRecommendAttractions(limit);
        return attractions.stream()
                .map(attraction -> BeanCopyUtils.copyBean(attraction, AttractionDTO.class))
                .collect(Collectors.toList());

    }

    @Override
    public List<AttractionDTO> getHotAttractions(Integer limit) {
        List<Attraction> attractions = attractionMapper.selectHotAttractions(limit);
        return attractions.stream()
                .map(attraction -> BeanCopyUtils.copyBean(attraction, AttractionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AttractionDTO> getAttractionsByCategory(Long categoryId) {
        List<Attraction> attractions = attractionMapper.selectByCategoryId(categoryId);
        return attractions.stream()
                .map(attraction -> BeanCopyUtils.copyBean(attraction, AttractionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void incrementViewCount(Long id) {
        attractionMapper.incrementViewCount(id);
    }


    @Override
    public List<CategoryDTO> getAttractionCategories() {
        // 尝试从缓存获取数据
        String cacheKey = ATTRACTION_CATEGORIES_CACHE_KEY;
        List<CategoryDTO> cachedCategories = (List<CategoryDTO>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedCategories != null && !cachedCategories.isEmpty()) {
            log.debug("从缓存获取景点分类列表");
            return cachedCategories;
        }
        
        // 如果缓存中没有，则从数据库获取
        List<AttractionCategory> categories = categoryMapper.selectEnabledCategories();
        List<CategoryDTO> result = categories.stream()
                .map(category -> BeanCopyUtils.copyBean(category, CategoryDTO.class))
                .collect(Collectors.toList());
        
        // 将结果存入缓存，设置过期时间为30分钟
        redisTemplate.opsForValue().set(cacheKey, result, 30, TimeUnit.MINUTES);
        log.debug("从数据库获取景点分类列表并存入缓存");
        
        return result;
    }

    @Override
    @Transactional
    public CategoryDTO createAttractionCategory(CategoryDTO categoryDTO) {
        AttractionCategory category = BeanCopyUtils.copyBean(categoryDTO, AttractionCategory.class);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        category.setStatus(1); // 默认启用

        categoryMapper.insert(category);
        CategoryDTO result = BeanCopyUtils.copyBean(category, CategoryDTO.class);
        
        // 清除分类缓存
        evictAttractionCategoryCache();
        
        return result;
    }

    @Override
    @Transactional
    public CategoryDTO updateAttractionCategory(Long id, CategoryDTO categoryDTO) {
        AttractionCategory existingCategory = categoryMapper.selectById(id);
        if (existingCategory == null) {
            throw new ServiceException("分类不存在");
        }

        AttractionCategory category = BeanCopyUtils.copyBean(categoryDTO, AttractionCategory.class);
        category.setId(id);
        category.setUpdatedAt(LocalDateTime.now());

        categoryMapper.updateById(category);
        CategoryDTO result = BeanCopyUtils.copyBean(category, CategoryDTO.class);
        
        // 清除分类缓存
        evictAttractionCategoryCache();
        
        return result;
    }

    @Override
    @Transactional
    public void deleteAttractionCategory(Long id) {
        AttractionCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new ServiceException("分类不存在");
        }

        // 检查是否有关联的景点
        List<Attraction> attractions = attractionMapper.selectByCategoryId(id);
        if (!attractions.isEmpty()) {
            throw new ServiceException("该分类下还有景点，无法删除");
        }

        categoryMapper.deleteById(id);
        
        // 清除分类缓存
        evictAttractionCategoryCache();
    }
    
    /**
     * 清除景点分类缓存
     */
    private void evictAttractionCategoryCache() {
        redisTemplate.delete(ATTRACTION_CATEGORIES_CACHE_KEY);
        log.debug("清除景点分类缓存");
    }
}