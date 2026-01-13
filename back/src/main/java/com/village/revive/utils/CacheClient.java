package com.village.revive.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    //缓存数据
    public void set(String key, Object value, Long time, TimeUnit unit){
        long seconds = unit.toSeconds(time);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), seconds+RandomUtil.randomInt(10, 1000), TimeUnit.SECONDS);
    }
    //缓存逻辑过期数据
    public void setWithLogicExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }
    public <T,ID> T queryWithPassThrough(String keyPrefix, ID id, Class<T> type, Function<ID,T> dbFallback,Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //尝试从redis查询
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }
        if(json != null){
            //缓存命中但是空值
            return null;
        }
        T t = dbFallback.apply(id);
        if(t == null){
            //将空值缓存到redis中，随机缓存时间
            stringRedisTemplate.opsForValue().set(key,"", 2L+ RandomUtil.randomInt(10, 1000), TimeUnit.SECONDS);
            return null;
        }
        this.set(key, t, time, unit);
        return t;
    }
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public <T,ID> T queryWithLogicExpire(String keyPrefix, String lockKeyPrefix, ID id, Class<T> type, Function<ID, T> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //尝试从redis查询
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(shopJson)){
            return null;
        }
        //命中，判断是否逻辑过期
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        T t = JSONUtil.toBean((JSONObject)redisData.getData(), type);

        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            //未过期
            return t;
        }
        //已过期，重建缓存
        String lock = lockKeyPrefix + id;
        boolean isLock = tryLock(lock);
        if(isLock){
            //成功，开启新线程重建缓存
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    T t1 = dbFallback.apply(id);
                    //模拟耗时
//                    Thread.sleep(200);
                    this.setWithLogicExpire(key, t1, 20L, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lock);
                }
            });
        }
        //未获取锁，返回旧数据
        return t;
    }
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return flag != null && flag;
    }
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

}
