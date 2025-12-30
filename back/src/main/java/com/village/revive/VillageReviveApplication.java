package com.village.revive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 乡村振兴·新桃源智界系统启动类
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.village.revive.mapper")
@EnableAspectJAutoProxy
public class VillageReviveApplication {
    public static void main(String[] args) {
        SpringApplication.run(VillageReviveApplication.class, args);
    }
} 