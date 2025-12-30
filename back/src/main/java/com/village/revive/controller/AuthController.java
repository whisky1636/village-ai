package com.village.revive.controller;

import com.village.revive.dto.LoginRequest;
import com.village.revive.dto.LoginResponse;
import com.village.revive.dto.UserInfoDto;
import com.village.revive.dto.UserRegisterDTO;
import com.village.revive.service.UserService;
import com.village.revive.utils.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "认证相关接口")
@RequiredArgsConstructor
public class AuthController {
    private final UserService  userService;
    /**
     * 登录
     */
     @RequestMapping("/login")
      public Result<LoginResponse> login(LoginRequest loginRequest) {

         LoginResponse loginResponse = userService.login(loginRequest);
         return Result.ok(loginResponse);
     }
    @PostMapping("/register")
    public Result<Long> register(@RequestBody UserRegisterDTO registerDTO) {
        Long userId = userService.register(registerDTO);
        return Result.success(userId);
    }
    /**
     * 获取当前用户信息
     * GET /auth/user/info
     */
    @GetMapping("/user/info")
    public Result<UserInfoDto> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserInfoDto userInfoDto = userService.getUserInfo(username);
        return Result.success(userInfoDto);
    }
    /**
     * 用户登出
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        return Result.success(true);
    }
}
