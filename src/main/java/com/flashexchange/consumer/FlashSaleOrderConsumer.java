package com.flashexchange.consumer;

import com.flashexchange.config.RabbitMQConfig;
import com.flashexchange.dto.FlashSaleOrderMessage;
import com.flashexchange.service.OrderProcessingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


@Component 
public class FlashSaleOrderConsumer {

    @Autowired 
    private OrderProcessingService orderProcessingService;

    @Autowired 
    private StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleOrder(FlashSaleOrderMessage orderMessage) {
        // Xử lý đơn hàng tại đây
        System.out.println("[WORKER-DB] Received from queue: "
            + orderMessage.orderId() 
            + " | User: " + orderMessage.userId()
            + " | Item: " + orderMessage.itemKey()
            + " | Quantity: " + orderMessage.quantity()
            + " | Timestamp: " + orderMessage.timestamp()
        );
        try {

            long itemPrice = 10L;
            boolean processed = orderProcessingService.processOrder(orderMessage, itemPrice);
        
            if (processed) {
                redisTemplate.opsForValue().set("order:status:" + orderMessage.orderId(), "PAID");
                System.out.println("[WORKER-DB] Order " + orderMessage.orderId() + " processed successfully.");
            }
        } catch (Exception e) {
            System.err.println("[WORKER-DB] Error processing order " + orderMessage.orderId() + ": " + e.getMessage());
        }
    }
    
    @RabbitListener(queues = RabbitMQConfig.CANCEL_QUEUE_NAME)
    public void handleOrderCancellation(FlashSaleOrderMessage message) {
        String statusKey = "order:status:" + message.orderId();
        String status = redisTemplate.opsForValue().get(statusKey);
        
        if ("UNPAID".equals(status)) {
            redisTemplate.opsForValue().set(statusKey, "CANCELLED");

            Long newStock = redisTemplate.opsForValue().increment(message.itemKey(), message.quantity());
            
            System.out.println("[TIMEOUT-WORKER] Order " + message.orderId() 
                    + " 10s not paid -> RETURNED " + message.quantity() 
                    + " product returned: " + newStock);
        } else {
            System.out.println("[TIMEOUT-WORKER] Order " + message.orderId() + " is purchased.");
        }
    }
}