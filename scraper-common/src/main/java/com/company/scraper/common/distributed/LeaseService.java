package com.company.scraper.common.distributed;

import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LeaseService {

    private final RedisTemplate<String, Object> redisTemplate;

    public LeaseService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquire(String key, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent("lease:" + key, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public void release(String key) {
        redisTemplate.delete("lease:" + key);
    }
}
