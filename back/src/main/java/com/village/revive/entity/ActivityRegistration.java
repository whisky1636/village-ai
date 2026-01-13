package com.village.revive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动报名实体类
 */
@Data
@TableName("activity_registrations")
public class ActivityRegistration {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long activityId;
    
    private Long userId;
    
    private String registrationNo;
    
    private String participantName;
    
    private String participantPhone;
    
    private String participantIdcard;
    
    private Integer participantCount;
    
    private String companions;
    
    private BigDecimal registrationFee;
    
    /**
     * 支付状态：0待支付，1已支付，2已退款
     */
    private Integer paymentStatus;
    
    private String paymentMethod;
    
    private LocalDateTime paymentTime;
    
    /**
     * 签到状态：0未签到，1已签到
     */
    private Integer checkInStatus;
    
    private LocalDateTime checkInTime;
    
    /**
     * 报名状态：1待审核，2已通过，3已拒绝，4已取消
     */
    private Integer status;
    
    private String remarks;
    
    private String adminRemarks;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}






















