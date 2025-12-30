package com.village.revive.controller;

import com.village.revive.dto.LoginRequest;
import com.village.revive.dto.LoginResponse;
import com.village.revive.dto.UserRegisterDTO;
import com.village.revive.service.UserService;
import com.village.revive.utils.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
