package com.village.revive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.village.revive.annotation.SystemOperation;
import com.village.revive.entity.ActivityRegistration;
import com.village.revive.entity.User;
import com.village.revive.service.ActivityRegistrationService;
import com.village.revive.service.UserService;
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
    private UserService userService;
    
    /**
     * 从Authentication获取用户ID
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user.getId();
    }
    
    @Operation(summary = "分页查询报名列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> getRegistrationPage(
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
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "获取用户的报名列表")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/my-registrations")
    public ResponseEntity<Map<String, Object>> getMyRegistrations(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        Page<ActivityRegistration> page = new Page<>(current, size);
        Page<ActivityRegistration> result = registrationService.getRegistrationPage(page, null, userId, null, null);
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "检查是否已报名")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/check/{activityId}")
    public ResponseEntity<Map<String, Object>> checkRegistration(
            @PathVariable Long activityId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            boolean hasRegistered = registrationService.hasRegistered(activityId, userId);
            ActivityRegistration registration = null;
            if (hasRegistered) {
                registration = registrationService.getUserRegistration(activityId, userId);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("hasRegistered", hasRegistered);
            data.put("registration", registration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查报名状态失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "系统内部错误，请联系管理员");
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @Operation(summary = "创建报名")
    @SystemOperation(module = "活动报名", operation = "创建报名")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRegistration(
            @RequestBody ActivityRegistration registration,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        registration.setUserId(userId);
        ActivityRegistration created = registrationService.createRegistration(registration);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "报名成功");
        response.put("data", created);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "取消报名")
    @SystemOperation(module = "活动报名", operation = "取消报名")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelRegistration(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        registrationService.cancelRegistration(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "取消报名成功");
        response.put("data", null);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "审核报名")
    @SystemOperation(module = "活动报名", operation = "审核报名")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/audit")
    public ResponseEntity<Map<String, Object>> auditRegistration(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        
        Integer status = (Integer) params.get("status");
        String adminRemarks = (String) params.get("adminRemarks");
        registrationService.auditRegistration(id, status, adminRemarks);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "审核成功");
        response.put("data", null);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "支付报名费")
    @SystemOperation(module = "活动报名", operation = "支付报名费")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/pay")
    public ResponseEntity<Map<String, Object>> payRegistrationFee(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        
        String paymentMethod = params.get("paymentMethod");
        registrationService.payRegistrationFee(id, paymentMethod);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "支付成功");
        response.put("data", null);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "签到")
    @SystemOperation(module = "活动报名", operation = "签到")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        
        String registrationNo = params.get("registrationNo");
        registrationService.checkIn(id, registrationNo);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "签到成功");
        response.put("data", null);
        
        return ResponseEntity.ok(response);
    }
}


