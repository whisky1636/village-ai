package com.village.revive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.village.revive.dto.CategoryDTO;
import com.village.revive.dto.PageRequest;
import com.village.revive.dto.ProductDTO;
import com.village.revive.entity.Product;
import com.village.revive.entity.ProductCategory;
import com.village.revive.exception.ServiceException;
import com.village.revive.mapper.ProductCategoryMapper;
import com.village.revive.mapper.ProductMapper;
import com.village.revive.service.ProductService;
import com.village.revive.utils.BeanCopyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 缓存键常量
    private static final String PRODUCT_CATEGORIES_CACHE_KEY = "product:categories:enabled";
    private static final String PRODUCT_CATEGORY_CACHE_PREFIX = "product:category:id:";
    
    @Override
    public IPage<ProductDTO> getProductPage(PageRequest pageRequest, Long categoryId, String keyword, Integer status, Boolean isFeatured) {

        Page<ProductDTO> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        IPage<ProductDTO> pageResult = productMapper.selectProductPage(page, categoryId, keyword, status, isFeatured);

        // 1. 计算销量前五（基于本次查询结果）
        List<ProductDTO> records = pageResult.getRecords();
        List<Long> hotIds = records.stream()
                .sorted(Comparator.comparing(ProductDTO::getSalesCount, Comparator.nullsLast(Integer::compareTo)).reversed())
                .limit(5)
                .map(ProductDTO::getId)
                .toList();

        // 2. 当前时间
        LocalDateTime now = LocalDateTime.now();

        // 3. 设置 isHot / isNew
        records.forEach(p -> {
            p.setIsHot(hotIds.contains(p.getId()));
            p.setIsNew(p.getUpdatedAt() != null && p.getUpdatedAt().isAfter(now.minusDays(5)));
        });

        return pageResult;
    }


    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getDeleted()) {
            throw new ServiceException("商品不存在");
        }
        ProductDTO dto = BeanCopyUtils.copyBean(product, ProductDTO.class);
        // 设置分类名称
        if (product.getCategoryId() != null) {
            ProductCategory category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getName());
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        // 验证分类是否存在
        if (productDTO.getCategoryId() != null) {
            ProductCategory category = categoryMapper.selectById(productDTO.getCategoryId());
            if (category == null || category.getStatus() != 1) {
                throw new ServiceException("商品分类不存在或已禁用");
            }
        }

        Product product = BeanCopyUtils.copyBean(productDTO, Product.class);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setDeleted(false);
        product.setSalesCount(0);
        product.setRating(productDTO.getRating() != null ? productDTO.getRating() : java.math.BigDecimal.ZERO);
        product.setIsFeatured(productDTO.getIsFeatured() != null ? productDTO.getIsFeatured() : false);

        productMapper.insert(product);
        return BeanCopyUtils.copyBean(product, ProductDTO.class);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existingProduct = productMapper.selectById(id);
        if (existingProduct == null || existingProduct.getDeleted()) {
            throw new ServiceException("商品不存在");
        }

        // 验证分类是否存在
        if (productDTO.getCategoryId() != null) {
            ProductCategory category = categoryMapper.selectById(productDTO.getCategoryId());
            if (category == null || category.getStatus() != 1) {
                throw new ServiceException("商品分类不存在或已禁用");
            }
        }

        Product product = BeanCopyUtils.copyBean(productDTO, Product.class);
        product.setId(id);
        product.setUpdatedAt(LocalDateTime.now());

        productMapper.updateById(product);
        return getProductById(id);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getDeleted()) {
            throw new ServiceException("商品不存在");
        }

        product.setDeleted(true);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.deleteById(product);
    }

    @Override
    public List<ProductDTO> getFeaturedProducts(Integer limit) {
        List<Product> products = productMapper.selectFeaturedProducts(limit);
        return products.stream()
                .map(product -> BeanCopyUtils.copyBean(product, ProductDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getHotProducts(Integer limit) {
        List<Product> products = productMapper.selectHotProducts(limit);
        return products.stream()
                .map(product -> BeanCopyUtils.copyBean(product, ProductDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        List<Product> products = productMapper.selectByCategoryId(categoryId);
        return products.stream()
                .map(product -> BeanCopyUtils.copyBean(product, ProductDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateStock(Long productId, Integer quantity) {
        //TOD 性能优化
        int result = productMapper.updateStock(productId, quantity);
        return result > 0;
    }

    @Override
    public void incrementSalesCount(Long productId, Integer quantity) {
        productMapper.incrementSalesCount(productId, quantity);
    }

    @Override
    public List<CategoryDTO> getProductCategories() {
        // 尝试从缓存获取数据
        String cacheKey = PRODUCT_CATEGORIES_CACHE_KEY;
        List<CategoryDTO> cachedCategories = (List<CategoryDTO>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedCategories != null && !cachedCategories.isEmpty()) {
            log.info("从缓存获取商品分类列表");
            return cachedCategories;
        }
        
        // 如果缓存中没有，则从数据库获取
        List<ProductCategory> categories = categoryMapper.selectEnabledCategories();
        List<CategoryDTO> result = categories.stream()
                .map(category -> BeanCopyUtils.copyBean(category, CategoryDTO.class))
                .collect(Collectors.toList());
        
        // 将结果存入缓存，设置过期时间为30分钟
        redisTemplate.opsForValue().set(cacheKey, result, 30, TimeUnit.MINUTES);
        log.debug("从数据库获取商品分类列表并存入缓存");
        
        return result;
    }

    @Override
    @Transactional
    public CategoryDTO createProductCategory(CategoryDTO categoryDTO) {
        ProductCategory category = BeanCopyUtils.copyBean(categoryDTO, ProductCategory.class);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        category.setStatus(1); // 默认启用

        categoryMapper.insert(category);
        CategoryDTO result = BeanCopyUtils.copyBean(category, CategoryDTO.class);
        
        // 清除分类缓存
        evictProductCategoryCache();
        
        return result;
    }

    @Override
    @Transactional
    public CategoryDTO updateProductCategory(Long id, CategoryDTO categoryDTO) {
        ProductCategory existingCategory = categoryMapper.selectById(id);
        if (existingCategory == null) {
            throw new ServiceException("分类不存在");
        }

        ProductCategory category = BeanCopyUtils.copyBean(categoryDTO, ProductCategory.class);
        category.setId(id);
        category.setUpdatedAt(LocalDateTime.now());

        categoryMapper.updateById(category);
        CategoryDTO result = BeanCopyUtils.copyBean(category, CategoryDTO.class);
        
        // 清除分类缓存
        evictProductCategoryCache();
        
        return result;
    }

    @Override
    @Transactional
    public void deleteProductCategory(Long id) {
        ProductCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new ServiceException("分类不存在");
        }

        // 检查是否有关联的商品
        List<Product> products = productMapper.selectByCategoryId(id);
        if (!products.isEmpty()) {
            throw new ServiceException("该分类下还有商品，无法删除");
        }

        categoryMapper.deleteById(id);
        
        // 清除分类缓存
        evictProductCategoryCache();
    }

    @Override
    public Object getProductCategoriesPage(Integer current, Integer size, String keyword, Integer status) {
        Page<ProductCategory> page = new Page<>(current, size);
        LambdaQueryWrapper<ProductCategory> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(ProductCategory::getName, keyword);
        }
        if (status != null) {
            queryWrapper.eq(ProductCategory::getStatus, status);
        }
        queryWrapper.orderByDesc(ProductCategory::getCreatedAt);

        IPage<ProductCategory> categoryPage = categoryMapper.selectPage(page, queryWrapper);

        // 转换结果
        Page<CategoryDTO> resultPage = new Page<>();
        BeanUtils.copyProperties(categoryPage, resultPage, "records");
        resultPage.setRecords(BeanCopyUtils.copyListProperties(categoryPage.getRecords(), CategoryDTO.class));

        Map<String, Object> result = new HashMap<>();
        result.put("records", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());

        return result;
    }

    @Override
    @Transactional
    public void updateProductCategoryStatus(Long id, Integer status) {
        ProductCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new ServiceException("分类不存在");
        }

        category.setStatus(status);
        categoryMapper.updateById(category);
        
        // 清除分类缓存
        evictProductCategoryCache();
    }
    
    /**
     * 清除商品分类缓存
     */
    private void evictProductCategoryCache() {
        redisTemplate.delete(PRODUCT_CATEGORIES_CACHE_KEY);
        log.debug("清除商品分类缓存");
    }
}