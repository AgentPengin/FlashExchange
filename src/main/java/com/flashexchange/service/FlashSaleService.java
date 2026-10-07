package com.flashexchange.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class FlashSaleService {
    
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> deductScript;

    public FlashSaleService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        
        this.deductScript = new DefaultRedisScript<>();
        this.deductScript.setLocation(new ClassPathResource("scripts/stock_decrement.lua"));
        this.deductScript.setResultType(Long.class);
    }

    public void initStock(String itemKey, long stock) {
        redisTemplate.opsForValue().set(itemKey, String.valueOf(stock));
    }

    public boolean tryDeductStock(String itemKey, long quantity) {
        Long result = redisTemplate.execute(
            deductScript,
            Collections.singletonList(itemKey),
            String.valueOf(quantity)
        );
        return result != null && result == 1L;
    }

    public long getStock(String itemKey) {
        String val = redisTemplate.opsForValue().get(itemKey);
        return val != null ? Long.parseLong(val) : 0;
    }

}
