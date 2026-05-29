package com.company.scraper.common.cache;

import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DedupCache {

    private final RedisTemplate<String, Object> redisTemplate;

    public DedupCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean markIfNew(String key, Duration ttl) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(success);
    }
}
