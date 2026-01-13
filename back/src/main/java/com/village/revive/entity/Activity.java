package com.village.revive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动实体类
 */
@Data
@TableName("activities")
public class Activity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String subtitle;
    
    private String category;
    
    private String coverImage;
    
    private String images;
    
    private String description;
    
    private String location;
    
    private BigDecimal longitude;
    
    private BigDecimal latitude;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private LocalDateTime registrationStartTime;
    
    private LocalDateTime registrationEndTime;
    
    private Integer maxParticipants;
    
    private Integer currentParticipants;
    
    private BigDecimal registrationFee;
    
    private String organizer;
    
    private String contactPerson;
    
    private String contactPhone;
    
    /**
     * 状态：0草稿，1已发布，2报名中，3报名结束，4活动进行中，5已结束，6已取消
     */
    private Integer status;
    
    private Integer isFeatured;
    
    private Integer viewCount;
    
    private Integer sortOrder;
    
    private Long createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}






















