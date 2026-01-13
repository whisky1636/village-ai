package com.village.revive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.village.revive.annotation.SystemOperation;
import com.village.revive.entity.ActivityRegistration;
import com.village.revive.entity.User;
import com.village.revive.security.SecurityService;
import com.village.revive.service.ActivityRegistrationService;
import com.village.revive.service.UserService;
import com.village.revive.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;





/**
 * 活动报名Controller
 */
@Slf4j
@Tag(name = "活动报名管理", description = "活动报名相关接口")
@RestController
@RequestMapping("/activity-registrations")
@CrossOrigin
public class ActivityRegistrationController {
    
    @Autowired
    private ActivityRegistrationService registrationService;
    
    @Autowired
    private SecurityService securityService;
    
    /**
     * 从Authentication获取用户ID
     */

    
    @Operation(summary = "分页查询报名列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/page")
    public Result<Map<String, Object>> getRegistrationPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer paymentStatus) {
        
        Page<ActivityRegistration> page = new Page<>(current, size);
        Page<ActivityRegistration> result = registrationService.getRegistrationPage(page, activityId, userId, status, paymentStatus);
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        

        
        return Result.ok(data);
    }
    
    @Operation(summary = "获取用户的报名列表")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/my-registrations")
    public Result<Map<String, Object>> getMyRegistrations(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Long userId = securityService.getCurrentUserId();
        Page<ActivityRegistration> page = new Page<>(current, size);
        Page<ActivityRegistration> result = registrationService.getRegistrationPage(page, null, userId, null, null);
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        

        
        return Result.ok(data);
    }
    
    @Operation(summary = "检查是否已报名")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/check/{activityId}")
    public Result<Map<String, Object>> checkRegistration(
            @PathVariable Long activityId
            ) {
        
        try {
            Long userId = securityService.getCurrentUserId();
            boolean hasRegistered = registrationService.hasRegistered(activityId, userId);
            ActivityRegistration registration = null;
            if (hasRegistered) {
                registration = registrationService.getUserRegistration(activityId, userId);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("hasRegistered", hasRegistered);
            data.put("registration", registration);

            
            return Result.ok(data);
        } catch (Exception e) {
            log.error("检查报名状态失败", e);

            return Result.fail("系统错误，请联系管理员");
        }
    }
    
    @Operation(summary = "创建报名")
    @SystemOperation(module = "活动报名", operation = "创建报名")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public Result<ActivityRegistration> createRegistration(
            @RequestBody ActivityRegistration registration
           ) {
        
        Long userId = securityService.getCurrentUserId();
        registration.setUserId(userId);
        ActivityRegistration created = registrationService.createRegistration(registration);
        

        
        return Result.ok(created);
    }
    
    @Operation(summary = "取消报名")
    @SystemOperation(module = "活动报名", operation = "取消报名")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/cancel")
    public Result cancelRegistration(
            @PathVariable Long id) {
        
        Long userId = securityService.getCurrentUserId();
        registrationService.cancelRegistration(id, userId);
        

        
        return Result.ok();
    }
    
    @Operation(summary = "审核报名")
    @SystemOperation(module = "活动报名", operation = "审核报名")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/audit")
    public Result<Map<String, Object>> auditRegistration(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        
        Integer status = (Integer) params.get("status");
        String adminRemarks = (String) params.get("adminRemarks");
        registrationService.auditRegistration(id, status, adminRemarks);
        

        
        return Result.ok();
    }
    
    @Operation(summary = "支付报名费")
    @SystemOperation(module = "活动报名", operation = "支付报名费")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/pay")
    public Result payRegistrationFee(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        
        String paymentMethod = params.get("paymentMethod");
        registrationService.payRegistrationFee(id, paymentMethod);
        

        
        return Result.ok("支付成功");
    }
    
    @Operation(summary = "签到")
    @SystemOperation(module = "活动报名", operation = "签到")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/check-in")
    public Result checkIn(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        
        String registrationNo = params.get("registrationNo");
        registrationService.checkIn(id, registrationNo);
        

        
        return Result.ok("签到成功");
    }
}


