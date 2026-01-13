package com.village.revive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.village.revive.entity.Activity;
import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * 活动Service接口
 */
public interface ActivityService extends IService<Activity> {
    
    /**
     * 分页查询活动列表
     */
    Page<Activity> getActivityPage(Page<Activity> page, String title, String category, Integer status, Integer isFeatured);
    
    /**
     * 获取活动详情（增加浏览量）
     */
    Activity getActivityDetail(Long id);
    
    /**
     * 创建活动
     */
    Activity createActivity(Activity activity);
    
    /**
     * 更新活动
     */
    boolean updateActivity(Activity activity);
    
    /**
     * 删除活动
     */
    boolean deleteActivity(Long id);
    
    /**
     * 发布活动
     */
    boolean publishActivity(Long id);
    
    /**
     * 取消活动
     */
    boolean cancelActivity(Long id);
    
    /**
     * 更新活动状态（系统自动调用）
     */
    void updateActivityStatus();
    
    /**
     * 获取我的活动报名记录
     */
    Page<Map<String, Object>> getMyActivityRegistrations(Page<Map<String, Object>> page, String title, String status, Authentication authentication);
}












