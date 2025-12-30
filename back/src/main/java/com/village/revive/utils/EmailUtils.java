package com.village.revive.utils;

import com.village.revive.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailUtils {
    private final StringRedisTemplate stringRedisTemplate;
    private final long EXPIRE_MINUTES = 5;
    private final int CODE_LENGTH = 6;
    private final long SEND_INTERVAL_SECONDS = 60;
    private final String REDIS_PREFIX = "email:code:";
    private final String REDIS_INTERVAL_PREFIX = "email:interval:";
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail; // 发件人邮箱

    @Value("${verify-code.email.template}")
    private String emailTemplate; // 验证码邮件模板
    public String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    public void sendVerifyCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // 发件人
            log.info("验证码邮件发送成功，发件人：{}", fromEmail);
            message.setTo(toEmail);     // 收件人
            message.setSubject("乡村振兴-注册验证码"); // 邮件标题
            String format = String.format(emailTemplate, code);
            log.info("format{}", format);
            message.setText(String.format(emailTemplate, code)); // 邮件内容（替换验证码）
            // 发送邮件
            javaMailSender.send(message);
            log.info("验证码邮件发送成功，收件人：{}", toEmail);
        } catch (Exception e) {
            log.error("验证码邮件发送失败，收件人：{}，原因：{}", toEmail, e.getMessage(), e);
            throw new ServiceException("邮件发送失败，请稍后重试");
        }
    }
    /**
     * 缓存验证码到Redis（设置有效期）
     */
    public void cacheCode(String email, String code) {
        // 缓存验证码（5分钟过期）
        stringRedisTemplate.opsForValue().set(
                REDIS_PREFIX + email,
                code,
                EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );
        // 缓存发送间隔（60秒内禁止重复发送）
        stringRedisTemplate.opsForValue().set(
                REDIS_INTERVAL_PREFIX + email,
                "1",
                SEND_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * 验证验证码是否有效（匹配+未过期）
     */
    public boolean validateCode(String email, String inputCode) {
        String cacheKey = REDIS_PREFIX + email;
        // 1. 检查验证码是否存在
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(cacheKey))) {
            return false;
        }
        // 2. 比对验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(cacheKey);
        boolean match = inputCode.equals(cacheCode);
        // 3. 验证通过后删除验证码（防止重复使用）
        if (match) {
            stringRedisTemplate.delete(cacheKey);
        }
        return match;
    }

    /**
     * 检查邮箱是否在发送间隔内（防刷）
     */
    public boolean isInSendInterval(String email) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(REDIS_INTERVAL_PREFIX + email));
    }

}
