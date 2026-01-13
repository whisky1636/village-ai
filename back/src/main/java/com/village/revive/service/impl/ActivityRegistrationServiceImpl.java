package com.village.revive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.village.revive.entity.Activity;
import com.village.revive.entity.ActivityRegistration;
import com.village.revive.exception.ServiceException;
import com.village.revive.mapper.ActivityMapper;
import com.village.revive.mapper.ActivityRegistrationMapper;
import com.village.revive.service.ActivityRegistrationService;
import com.village.revive.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 活动报名Service实现类
 */
@Slf4j
@Service
public class ActivityRegistrationServiceImpl extends ServiceImpl<ActivityRegistrationMapper, ActivityRegistration> 
        implements ActivityRegistrationService {
    
    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private ActivityMapper activityMapper;
    
    @Override
    public Page<ActivityRegistration> getRegistrationPage(Page<ActivityRegistration> page, Long activityId, 
                                                          Long userId, Integer status, Integer paymentStatus) {
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(activityId != null, ActivityRegistration::getActivityId, activityId)
               .eq(userId != null, ActivityRegistration::getUserId, userId)
               .eq(status != null, ActivityRegistration::getStatus, status)
               .eq(paymentStatus != null, ActivityRegistration::getPaymentStatus, paymentStatus)
               .orderByDesc(ActivityRegistration::getCreatedAt);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional
    public ActivityRegistration createRegistration(ActivityRegistration registration) {
        // 获取活动信息
        Activity activity = activityService.getById(registration.getActivityId());
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        
        // 检查活动状态
        if (activity.getStatus() != 2) {
            throw new ServiceException("该活动当前不在报名期间");
        }
        
        // 检查报名截止时间
        if (LocalDateTime.now().isAfter(activity.getRegistrationEndTime())) {
            throw new ServiceException("报名已截止");
        }
        
        // 检查是否已报名
        if (hasRegistered(registration.getActivityId(), registration.getUserId())) {
            throw new ServiceException("您已报名该活动");
        }
        
        // 检查人数限制
        if (activity.getMaxParticipants() > 0) {
            if (activity.getCurrentParticipants() + registration.getParticipantCount() > activity.getMaxParticipants()) {
                throw new ServiceException("活动报名人数已满");
            }
        }
        
        // 生成报名编号
        String registrationNo = generateRegistrationNo(registration.getActivityId());
        registration.setRegistrationNo(registrationNo);
        
        // 设置报名费用
        registration.setRegistrationFee(activity.getRegistrationFee().multiply(
                java.math.BigDecimal.valueOf(registration.getParticipantCount())));
        
        // 设置初始状态
        registration.setStatus(1); // 待审核
        registration.setPaymentStatus(0); // 待支付
        registration.setCheckInStatus(0); // 未签到
        
        // 如果免费，直接通过审核和支付
        if (activity.getRegistrationFee().compareTo(java.math.BigDecimal.ZERO) == 0) {
            registration.setStatus(2); // 已通过
            registration.setPaymentStatus(1); // 已支付
            registration.setPaymentTime(LocalDateTime.now());
        }
        
        this.save(registration);
        
        // 更新活动报名人数
        activityMapper.updateCurrentParticipants(registration.getActivityId(), 
                                                registration.getParticipantCount());
        
        return registration;
    }
    
    @Override
    @Transactional
    public boolean cancelRegistration(Long id, Long userId) {
        ActivityRegistration registration = this.getById(id);
        if (registration == null) {
            throw new ServiceException("报名记录不存在");
        }
        
        // 验证是否是本人的报名
        if (!registration.getUserId().equals(userId)) {
            throw new ServiceException("无权取消此报名");
        }
        
        // 检查是否可以取消
        if (registration.getStatus() == 4) {
            throw new ServiceException("该报名已取消");
        }
        
        if (registration.getCheckInStatus() == 1) {
            throw new ServiceException("已签到的报名无法取消");
        }
        
        // 更新状态
        registration.setStatus(4);
        boolean result = this.updateById(registration);
        
        if (result) {
            // 减少活动报名人数
            activityMapper.updateCurrentParticipants(registration.getActivityId(), 
                                                    -registration.getParticipantCount());
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public boolean auditRegistration(Long id, Integer status, String adminRemarks) {
        ActivityRegistration registration = this.getById(id);
        if (registration == null) {
            throw new ServiceException("报名记录不存在");
        }
        
        registration.setStatus(status);
        registration.setAdminRemarks(adminRemarks);
        return this.updateById(registration);
    }
    
    @Override
    @Transactional
    public boolean payRegistrationFee(Long id, String paymentMethod) {
        ActivityRegistration registration = this.getById(id);
        if (registration == null) {
            throw new ServiceException("报名记录不存在");
        }
        
        if (registration.getPaymentStatus() == 1) {
            throw new ServiceException("已支付，请勿重复支付");
        }
        
        registration.setPaymentStatus(1);
        registration.setPaymentMethod(paymentMethod);
        registration.setPaymentTime(LocalDateTime.now());
        
        return this.updateById(registration);
    }
    
    @Override
    @Transactional
    public boolean checkIn(Long id, String registrationNo) {
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getRegistrationNo, registrationNo);
        ActivityRegistration registration = this.getOne(wrapper);
        
        if (registration == null) {
            throw new ServiceException("报名记录不存在");
        }
        
        if (registration.getStatus() != 2) {
            throw new ServiceException("该报名未通过审核");
        }
        
        if (registration.getPaymentStatus() != 1) {
            throw new ServiceException("该报名未支付");
        }
        
        if (registration.getCheckInStatus() == 1) {
            throw new ServiceException("已签到，请勿重复签到");
        }
        
        registration.setCheckInStatus(1);
        registration.setCheckInTime(LocalDateTime.now());
        
        return this.updateById(registration);
    }
    
    @Override
    public boolean hasRegistered(Long activityId, Long userId) {
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getActivityId, activityId)
               .eq(ActivityRegistration::getUserId, userId)
               .ne(ActivityRegistration::getStatus, 4); // 排除已取消的
        return this.count(wrapper) > 0;
    }
    
    @Override
    public ActivityRegistration getUserRegistration(Long activityId, Long userId) {
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getActivityId, activityId)
               .eq(ActivityRegistration::getUserId, userId)
               .ne(ActivityRegistration::getStatus, 4)
               .orderByDesc(ActivityRegistration::getCreatedAt)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }
    
    /**
     * 生成报名编号
     */
    private String generateRegistrationNo(Long activityId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String date = LocalDateTime.now().format(formatter);
        long count = this.count(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)) + 1;
        return date + String.format("%04d", activityId) + String.format("%04d", count);
    }
}

