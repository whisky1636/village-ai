package com.village.revive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.village.revive.entity.ActivityRegistration;

/**
 * 活动报名Service接口
 */
public interface ActivityRegistrationService extends IService<ActivityRegistration> {
    
    /**
     * 分页查询报名列表
     */
    Page<ActivityRegistration> getRegistrationPage(Page<ActivityRegistration> page, Long activityId, Long userId, Integer status, Integer paymentStatus);
    
    /**
     * 创建报名
     */
    ActivityRegistration createRegistration(ActivityRegistration registration);
    
    /**
     * 取消报名
     */
    boolean cancelRegistration(Long id, Long userId);
    
    /**
     * 审核报名
     */
    boolean auditRegistration(Long id, Integer status, String adminRemarks);
    
    /**
     * 支付报名费
     */
    boolean payRegistrationFee(Long id, String paymentMethod);
    
    /**
     * 签到
     */
    boolean checkIn(Long id, String registrationNo);
    
    /**
     * 检查是否已报名
     */
    boolean hasRegistered(Long activityId, Long userId);
    
    /**
     * 获取用户的报名记录
     */
    ActivityRegistration getUserRegistration(Long activityId, Long userId);
}












