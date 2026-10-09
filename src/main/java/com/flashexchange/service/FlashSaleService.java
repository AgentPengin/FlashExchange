package com.flashexchange.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.flashexchange.dto.FlashSaleOrderMessage;
import com.flashexchange.config.RabbitMQConfig;
import java.util.UUID;
import com.flashexchange.dto.DeductResult;

import java.util.Collections;

@Service
public class FlashSaleService {
    
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> deductScript;
    private final RabbitTemplate rabbitTemplate;

    public FlashSaleService(StringRedisTemplate redisTemplate, RabbitTemplate rabbitTemplate) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.deductScript = new DefaultRedisScript<>();
        this.deductScript.setLocation(new ClassPathResource("scripts/stock_decrement.lua"));
        this.deductScript.setResultType(Long.class);
    }

    public void initStock(String itemKey, long stock) {
        redisTemplate.opsForValue().set(itemKey, String.valueOf(stock));
    }

    public DeductResult tryDeductStock(String itemKey, long quantity) {
        Long result = redisTemplate.execute(
            deductScript,
            Collections.singletonList(itemKey),
            String.valueOf(quantity)
        );
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        if (result != null && result == 1L) {
            // Gửi thông điệp đến RabbitMQ để xử lý đơn hàng
            String userId = "user-" + UUID.randomUUID().toString().substring(0, 4);
            redisTemplate.opsForValue().set("order:status:" + orderId, "UNPAID");
            FlashSaleOrderMessage message = new FlashSaleOrderMessage(
                orderId, userId, itemKey, quantity, System.currentTimeMillis()
            );
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                message
            );
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME, 
                RabbitMQConfig.DELAY_ROUTING_KEY, 
                message
            );
        }

        return new DeductResult(result != null && result == 1L, orderId);
    } 
    public void payOrder(String orderId) {
        System.out.println("[TICK]: Order " + orderId + " has been paid.");
        redisTemplate.opsForValue().set("order:status:" + orderId, "PAID");
    }
    public String getOrderStatus(String orderId) {
        return redisTemplate.opsForValue().get("order:status:" + orderId);
    }

    public long getStock(String itemKey) {
        String val = redisTemplate.opsForValue().get(itemKey);
        return val != null ? Long.parseLong(val) : 0;
    }

}
