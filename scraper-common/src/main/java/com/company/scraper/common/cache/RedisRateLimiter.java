package com.company.scraper.common.cache;

import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allow(String key, int limit, Duration window) {
        String redisKey = "rate:" + key;
        Long current = redisTemplate.opsForValue().increment(redisKey);
        if (current != null && current == 1L) {
            redisTemplate.expire(redisKey, window);
        }
        return current != null && current <= limit;
    }
}
