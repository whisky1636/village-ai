package com.village.revive.exception;

import com.village.revive.utils.Result;
import com.village.revive.utils.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ServiceException.class)
    public Result handleServiceException(ServiceException e) {
        log.error("ServiceException: {}", e.getMessage());
        return Result.error(e.getMessage());
    }
    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Object> handleAuthenticationException(AuthenticationException e) {
        log.error("认证异常: {}", e.getMessage());
        if (e instanceof BadCredentialsException) {
            return Result.fail(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        return Result.unauthorized();
    }
}
