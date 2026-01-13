package com.village.revive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.village.revive.annotation.SystemOperation;
import com.village.revive.entity.Activity;
import com.village.revive.service.ActivityService;
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
 * 活动管理Controller
 */
@Slf4j
@Tag(name = "活动管理", description = "活动管理相关接口")
@RestController
@RequestMapping("/activities")
@CrossOrigin
public class ActivityController {
    
    @Autowired
    private ActivityService activityService;
    
    @Operation(summary = "分页查询活动列表")
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> getActivityPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer isFeatured) {
        
        Page<Activity> page = new Page<>(current, size);
        Page<Activity> result = activityService.getActivityPage(page, title, category, status, isFeatured);
        
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
    
    @Operation(summary = "获取活动详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getActivityDetail(@PathVariable Long id) {
        Activity activity = activityService.getActivityDetail(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", activity);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "创建活动")
    @SystemOperation(module = "活动管理", operation = "创建活动")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createActivity(@RequestBody Activity activity) {
        Activity created = activityService.createActivity(activity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "创建成功");
        response.put("data", created);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "更新活动")
    @SystemOperation(module = "活动管理", operation = "更新活动")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateActivity(@PathVariable Long id, @RequestBody Activity activity) {
        activity.setId(id);
        activityService.updateActivity(activity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "更新成功");
        response.put("data", null);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "删除活动")
    @SystemOperation(module = "活动管理", operation = "删除活动")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "删除成功");
        response.put("data", null);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "发布活动")
    @SystemOperation(module = "活动管理", operation = "发布活动")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishActivity(@PathVariable Long id) {
        activityService.publishActivity(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "发布成功");
        response.put("data", null);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "取消活动")
    @SystemOperation(module = "活动管理", operation = "取消活动")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelActivity(@PathVariable Long id) {
        activityService.cancelActivity(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "取消成功");
        response.put("data", null);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "获取我的活动报名记录")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/my-registrations")
    public ResponseEntity<Map<String, Object>> getMyActivityRegistrations(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        
        Page<Map<String, Object>> page = new Page<>(current, size);
        Page<Map<String, Object>> result = activityService.getMyActivityRegistrations(page, title, status, authentication);
        
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
}


