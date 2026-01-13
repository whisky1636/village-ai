package com.village.revive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.village.revive.annotation.SystemOperation;
import com.village.revive.entity.Activity;
import com.village.revive.service.ActivityService;
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
    public Result<Map<String, Object>> getActivityPage(
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
        

        
        return Result.ok(data);
    }
    
    @Operation(summary = "获取活动详情")
    @GetMapping("/{id}")
    public Result<Activity> getActivityDetail(@PathVariable Long id) {
        Activity activity = activityService.getActivityDetail(id);
        
        return Result.ok(activity);
    }
    
    @Operation(summary = "创建活动")
    @SystemOperation(module = "活动管理", operation = "创建活动")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Activity> createActivity(@RequestBody Activity activity) {
        Activity created = activityService.createActivity(activity);


        
        return Result.ok(created);
    }
    
    @Operation(summary = "更新活动")
    @SystemOperation(module = "活动管理", operation = "更新活动")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Result updateActivity(@PathVariable Long id, @RequestBody Activity activity) {
        activity.setId(id);
        activityService.updateActivity(activity);
        return Result.ok();
    }
    
    @Operation(summary = "删除活动")
    @SystemOperation(module = "活动管理", operation = "删除活动")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        

        
        return Result.ok();
    }
    
    @Operation(summary = "发布活动")
    @SystemOperation(module = "活动管理", operation = "发布活动")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/publish")
    public Result publishActivity(@PathVariable Long id) {
        activityService.publishActivity(id);


        
        return Result.ok();
    }
    
    @Operation(summary = "取消活动")
    @SystemOperation(module = "活动管理", operation = "取消活动")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/cancel")
    public Result cancelActivity(@PathVariable Long id) {
        activityService.cancelActivity(id);

        
        return Result.ok();
    }
    
    @Operation(summary = "获取我的活动报名记录")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/my-registrations")
    public Result<Map<String, Object>> getMyActivityRegistrations(
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
        

        
        return Result.ok(data);
    }
}


