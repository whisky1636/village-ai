package com.village.revive.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.village.revive.dto.PageRequest;
import com.village.revive.dto.ProductDTO;
import com.village.revive.service.ProductService;
import com.village.revive.utils.Result;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Tag(name = "商品管理", description = "商品相关接口")
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    @RequestMapping("/page")
    public Result<IPage<ProductDTO>> getProductPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1")Integer current,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10")Integer size,
            @Parameter(description = "商品分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "商品名称") @RequestParam(required = false) String keyword,
            @Parameter(description = "商品状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "是否推荐") @RequestParam(required = false) Boolean isFeatured
    ){
        PageRequest pageRequest = new PageRequest(current, size);
        IPage<ProductDTO> page = productService.getProductPage(pageRequest, categoryId, keyword, status, isFeatured);
        return Result.success(page);
    }

}
