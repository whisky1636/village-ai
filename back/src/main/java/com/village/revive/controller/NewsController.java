package com.village.revive.controller;

import com.village.revive.annotation.SystemOperation;
import com.village.revive.dto.AddressDTO;
import com.village.revive.dto.NewsDTO;
import com.village.revive.security.SecurityService;
import com.village.revive.service.AddressService;
import com.village.revive.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资讯控制器
 */
@Tag(name = "资讯管理", description = "资讯相关接口")
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Validated
public class NewsController {
    

}
