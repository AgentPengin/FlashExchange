package com.flashexchange.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.time.Instant;

@Service
public class RateLimiterService {
    
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimiterScript;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.rateLimiterScript = new DefaultRedisScript<>();
        this.rateLimiterScript.setLocation(new ClassPathResource("scripts/rate_limiter.lua"));
        this.rateLimiterScript.setResultType(Long.class);
    }

    public boolean isAllowed(String key, long capacity, double refillRatePerSecond) {
        String redisKey = "rate_limit:" + key;
        double now = Instant.now().toEpochMilli() / 1000.0; 

        Long result = redisTemplate.execute(
            rateLimiterScript,
            Collections.singletonList(redisKey),
            String.valueOf(capacity),
            String.valueOf(refillRatePerSecond),
            String.valueOf(now),
            "1"
        );

        return result != null && result == 1;
    }
}
