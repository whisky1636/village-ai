package com.village.revive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.village.revive.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 活动Mapper接口
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    
    /**
     * 增加浏览次数
     */
    void incrementViewCount(@Param("id") Long id);
    
    /**
     * 更新报名人数
     */
    void updateCurrentParticipants(@Param("id") Long id, @Param("count") Integer count);
}






















