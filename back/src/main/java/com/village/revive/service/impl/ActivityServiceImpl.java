package com.village.revive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.village.revive.entity.Activity;
import com.village.revive.entity.ActivityRegistration;
import com.village.revive.exception.ServiceException;
import com.village.revive.mapper.ActivityMapper;
import com.village.revive.mapper.ActivityRegistrationMapper;
import com.village.revive.security.SecurityService;
import com.village.revive.service.ActivityService;
import com.village.revive.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 活动Service实现类
 */
@Slf4j
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {
    
    @Autowired
    private ActivityRegistrationMapper activityRegistrationMapper;
    
    @Autowired
    private SecurityService  securityService;
    
    @Override
    public Page<Activity> getActivityPage(Page<Activity> page, String title, String category, Integer status, Integer isFeatured) {
        // 先自动更新所有活动的状态
        updateActivityStatus();
        
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), Activity::getTitle, title)
               .eq(StringUtils.hasText(category), Activity::getCategory, category)
               .eq(status != null, Activity::getStatus, status)
               .eq(isFeatured != null, Activity::getIsFeatured, isFeatured)
               .orderByDesc(Activity::getSortOrder)
               .orderByDesc(Activity::getCreatedAt);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional
    public Activity getActivityDetail(Long id) {
        // 先更新活动状态
        updateActivityStatus();
        
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        
        // 增加浏览量
        baseMapper.incrementViewCount(id);
        return activity;
    }
    
    @Override
    @Transactional
    public Activity createActivity(Activity activity) {
        // 设置初始状态为草稿
        if (activity.getStatus() == null) {
            activity.setStatus(0);
        }
        // 设置初始值
        if (activity.getCurrentParticipants() == null) {
            activity.setCurrentParticipants(0);
        }
        if (activity.getViewCount() == null) {
            activity.setViewCount(0);
        }
        this.save(activity);
        return activity;
    }
    
    @Override
    @Transactional
    public boolean updateActivity(Activity activity) {
        Activity existActivity = this.getById(activity.getId());
        if (existActivity == null) {
            throw new ServiceException("活动不存在");
        }
        return this.updateById(activity);
    }
    
    @Override
    @Transactional
    public boolean deleteActivity(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        // 检查是否有报名记录
        if (activity.getCurrentParticipants() > 0) {
            throw new ServiceException("该活动已有报名记录，无法删除");
        }
        return this.removeById(id);
    }
    
    @Override
    @Transactional
    public boolean publishActivity(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 根据时间自动设置合适的状态
        if (now.isBefore(activity.getRegistrationStartTime())) {
            // 报名还未开始
            activity.setStatus(1); // 已发布
        } else if (now.isAfter(activity.getRegistrationEndTime())) {
            // 报名已结束
            activity.setStatus(3); // 报名结束
        } else {
            // 当前在报名时间范围内
            activity.setStatus(2); // 报名中
        }
        
        return this.updateById(activity);
    }
    
    @Override
    @Transactional
    public boolean cancelActivity(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        activity.setStatus(6); // 已取消
        return this.updateById(activity);
    }
    
    @Override
    @Transactional
    public void updateActivityStatus() {
        LocalDateTime now = LocalDateTime.now();
        
        // 更新为报名中（已发布 && 当前时间在报名时间范围内）
        LambdaQueryWrapper<Activity> registrationWrapper = new LambdaQueryWrapper<>();
        registrationWrapper.eq(Activity::getStatus, 1)
                          .le(Activity::getRegistrationStartTime, now)
                          .ge(Activity::getRegistrationEndTime, now);
        Activity registrationUpdate = new Activity();
        registrationUpdate.setStatus(2);
        this.update(registrationUpdate, registrationWrapper);
        
        // 更新为报名结束（报名中 && 报名截止时间已过）
        LambdaQueryWrapper<Activity> endRegistrationWrapper = new LambdaQueryWrapper<>();
        endRegistrationWrapper.eq(Activity::getStatus, 2)
                             .lt(Activity::getRegistrationEndTime, now);
        Activity endRegistrationUpdate = new Activity();
        endRegistrationUpdate.setStatus(3);
        this.update(endRegistrationUpdate, endRegistrationWrapper);
        
        // 更新为活动进行中（报名结束 && 活动开始时间已到）
        LambdaQueryWrapper<Activity> ongoingWrapper = new LambdaQueryWrapper<>();
        ongoingWrapper.eq(Activity::getStatus, 3)
                     .le(Activity::getStartTime, now)
                     .ge(Activity::getEndTime, now);
        Activity ongoingUpdate = new Activity();
        ongoingUpdate.setStatus(4);
        this.update(ongoingUpdate, ongoingWrapper);
        
        // 更新为已结束（活动进行中 && 活动结束时间已过）
        LambdaQueryWrapper<Activity> finishedWrapper = new LambdaQueryWrapper<>();
        finishedWrapper.eq(Activity::getStatus, 4)
                      .lt(Activity::getEndTime, now);
        Activity finishedUpdate = new Activity();
        finishedUpdate.setStatus(5);
        this.update(finishedUpdate, finishedWrapper);
    }
    
    @Override
    public Page<Map<String, Object>> getMyActivityRegistrations(Page<Map<String, Object>> page, String title, String status, Authentication authentication) {
        // 先自动更新所有活动的状态
        updateActivityStatus();
        
        Long currentUserId = securityService.getCurrentUserId();
        if (currentUserId == null) {
            throw new ServiceException("用户未登录");
        }
        
        // 构建查询条件
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getUserId, currentUserId);
        
        // 按活动标题搜索
        if (StringUtils.hasText(title)) {
            wrapper.exists("SELECT 1 FROM activities a WHERE a.id = activity_registrations.activity_id AND a.title LIKE {0}", "%" + title + "%");
        }
        
        // 按状态筛选
        if (StringUtils.hasText(status)) {
            if ("pending".equals(status)) {
                wrapper.eq(ActivityRegistration::getStatus, 0);
            } else if ("ongoing".equals(status)) {
                wrapper.exists("SELECT 1 FROM activities a WHERE a.id = activity_registrations.activity_id AND a.status = 4");
            } else if ("completed".equals(status)) {
                wrapper.exists("SELECT 1 FROM activities a WHERE a.id = activity_registrations.activity_id AND a.status = 5");
            } else if ("cancelled".equals(status)) {
                wrapper.exists("SELECT 1 FROM activities a WHERE a.id = activity_registrations.activity_id AND a.status = 6");
            }
        }
        
        // 按报名时间排序
        wrapper.orderByDesc(ActivityRegistration::getCreatedAt);
        
        // 分页查询
        Page<ActivityRegistration> registrationPage = new Page<>(page.getCurrent(), page.getSize());
        Page<ActivityRegistration> result = activityRegistrationMapper.selectPage(registrationPage, wrapper);
        
        // 转换为包含活动信息的Map
        Page<Map<String, Object>> resultPage = new Page<>(page.getCurrent(), page.getSize());
        resultPage.setTotal(result.getTotal());
        
        // 创建可修改的记录列表
        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>();
        
        for (ActivityRegistration registration : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", registration.getId());
            item.put("activityId", registration.getActivityId());
            item.put("registrationStatus", registration.getStatus());
            item.put("registrationTime", registration.getCreatedAt());
            
            // 获取活动信息
            Activity activity = this.getById(registration.getActivityId());
            if (activity != null) {
                item.put("activityTitle", activity.getTitle());
                item.put("activityLocation", activity.getLocation());
                item.put("activityStartTime", activity.getStartTime());
                item.put("activityStatus", activity.getStatus());
            }
            
            records.add(item);
        }
        
        // 设置记录列表
        resultPage.setRecords(records);
        
        return resultPage;
    }
}



