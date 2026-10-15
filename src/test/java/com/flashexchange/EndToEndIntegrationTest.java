package com.flashexchange;

import com.flashexchange.dto.DeductResult;
import com.flashexchange.engine.OrderBook;
import com.flashexchange.model.MatchResult;
import com.flashexchange.model.Order;
import com.flashexchange.model.OrderSide;
import com.flashexchange.model.OrderStatus;
import com.flashexchange.model.OrderType;
import com.flashexchange.service.FlashSaleService;
import com.flashexchange.service.OrderProcessingService;
import com.flashexchange.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest 
public class EndToEndIntegrationTest {

    @Autowired
    private RateLimiterService rateLimiterService;
    
    @Autowired
    private FlashSaleService flashSaleService;
    
    @Autowired
    private OrderProcessingService orderProcessingService;
    
    @Autowired
    private OrderBook orderBook;

    @Test
    @DisplayName("E2E Flow: Rate Limit -> Flash sale buy ticket -> RabbitMQ to Postgres -> Trading")
    void testCompleteLifecycleFlow() throws InterruptedException {
        String aliceId = "alice-vip";
        String bobId = "bob-trader";
        String flashItemKey = "flashsale:token:FLASH";

        // Step 1: Initialize stock for the flash sale item
        orderProcessingService.createOrUpdateWallet(aliceId, 100L);
        assertThat(orderProcessingService.getWalletBalance(aliceId)).isEqualTo(100L);

        // Step 2: Flash sale item stock initialization
        flashSaleService.initStock(flashItemKey, 5L);
        assertThat(flashSaleService.getStock(flashItemKey)).isEqualTo(5L);

        // Step 3: Alice tries to buy the flash sale item
        boolean isAliceAllowed = rateLimiterService.isAllowed("user:" + aliceId, 20, 1.0);
        assertThat(isAliceAllowed).isTrue();

        // Step 4: Alice attempts to buy the flash sale item
        DeductResult deductResult = flashSaleService.tryDeductStock(aliceId, flashItemKey, 1);
        assertThat(deductResult.success()).isTrue();
        assertThat(deductResult.orderId()).isNotNull();
        assertThat(flashSaleService.getStock(flashItemKey)).isEqualTo(4L);

        // Step 5: RabbitMQ consumer processes the order and updates the wallet
        Thread.sleep(1000); // Wait for RabbitMQ consumer to process the order
        String orderStatus = flashSaleService.getOrderStatus(deductResult.orderId());
        assertThat(orderStatus).isEqualTo("PAID");

        long aliceBalance = orderProcessingService.getWalletBalance(aliceId);
        assertThat(aliceBalance).isEqualTo(90L);

        Order aliceSellOrder = new Order (
            "ord-alice-sell-1",
            aliceId,
            "FLASH-USDT",
            OrderSide.SELL,
            OrderType.LIMIT,
            15L,
            1L
        );
        MatchResult sellResult = orderBook.placeOrder(aliceSellOrder);

        assertThat(sellResult.trades()).isEmpty();
        assertThat(sellResult.order().getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(orderBook.getBestAsk()).isEqualTo(15L);

        Order bobBuyOrder = new Order (
            "ord-bob-buy-1",
            bobId,
            "FLASH-USDT",
            OrderSide.BUY,
            OrderType.LIMIT,
            15L,
            1L
        );

        MatchResult buyResult = orderBook.placeOrder(bobBuyOrder);

        assertThat(buyResult.trades()).hasSize(1);
        assertThat(buyResult.trades().get(0).price()).isEqualTo(15L);
        assertThat(buyResult.trades().get(0).quantity()).isEqualTo(1L);
        assertThat(buyResult.trades().get(0).makerOrderId()).isEqualTo("ord-alice-sell-1");
        assertThat(buyResult.trades().get(0).takerOrderId()).isEqualTo("ord-bob-buy-1");

        assertThat(aliceSellOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(bobBuyOrder.getStatus()).isEqualTo(OrderStatus.FILLED);

    }

}
