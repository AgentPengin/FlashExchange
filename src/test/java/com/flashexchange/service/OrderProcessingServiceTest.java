package com.flashexchange.service;

import com.flashexchange.dto.FlashSaleOrderMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OrderProcessingServiceTest {

    @Autowired 
    private OrderProcessingService orderProcessingService;

    @Test 
    @DisplayName("TEST IDEMPOTENCY: Processing the same order twice should not deduct balance twice")
    void testIdempotentOrderProcessing() {
        String userId = "test-user";
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        long itemPrice = 30;

        orderProcessingService.createOrUpdateWallet(userId, 100);
        assertThat(orderProcessingService.getWalletBalance(userId)).isEqualTo(100);

        FlashSaleOrderMessage message = new FlashSaleOrderMessage(orderId, userId, "ticket_vip", 1, System.currentTimeMillis());
        
        boolean firstAttempt = orderProcessingService.processOrder(message, itemPrice);
        assertThat(firstAttempt).isTrue();

        long balanceAfterFirstAttempt = orderProcessingService.getWalletBalance(userId);
        assertThat(balanceAfterFirstAttempt).isEqualTo(70);
        System.out.println("First time: Subtraction completed! Balance after first attempt: " + balanceAfterFirstAttempt + "$");

        System.out.println("Processing the same order again to test idempotency...");
        boolean secondAttempt = orderProcessingService.processOrder(message, itemPrice);
        assertThat(secondAttempt).isFalse();

        long balanceAfterSecondAttempt = orderProcessingService.getWalletBalance(userId);
        assertThat(balanceAfterSecondAttempt).isEqualTo(70);

        System.out.println("Second time: No subtraction should occur! Balance after second attempt: " + balanceAfterSecondAttempt + "$");

    }
    
}
