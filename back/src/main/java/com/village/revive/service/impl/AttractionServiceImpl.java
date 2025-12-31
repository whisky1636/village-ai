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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttractionServiceImpl implements AttractionService {
    private final AttractionMapper attractionMapper;
    private final AttractionCategoryMapper categoryMapper;
    @Override
    public IPage<AttractionDTO> getAttractionPage(PageRequest pageRequest, Long categoryId, String keyword, Integer status) {
        Page<Attraction> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        IPage<Attraction> pageResult = attractionMapper.selectAttractionPage(page, categoryId, keyword, status);
        return pageResult.convert(attraction -> {
            AttractionDTO dto = BeanCopyUtils.copyProperties(attraction, AttractionDTO.class);
            if(attraction.getCategoryId() != null){
                AttractionCategory attractionCategory = categoryMapper.selectById(attraction.getCategoryId());
                if(attractionCategory != null) dto.setCategoryName(attractionCategory.getName());
            }
            return dto;
        });

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
    public AttractionDTO updateAttraction(Long id, AttractionDTO attractionDTO) {
        return null;
    }

    @Override
    public void deleteAttraction(Long id) {

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
        List<AttractionCategory> categories = categoryMapper.selectEnabledCategories();
        return categories.stream()
                .map(category -> BeanCopyUtils.copyBean(category, CategoryDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDTO createAttractionCategory(CategoryDTO categoryDTO) {
        return null;
    }

    @Override
    public CategoryDTO updateAttractionCategory(Long id, CategoryDTO categoryDTO) {
        return null;
    }

    @Override
    public void deleteAttractionCategory(Long id) {

    }
}
