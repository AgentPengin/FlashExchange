package com.flashexchange.engine;

import com.flashexchange.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook("FLASH-USDT");
    }

    @Test
    @DisplayName("Đặt lệnh khi không có đối ứng: Lệnh nằm chờ trên sổ")
    void testPlaceOrder_NoMatch_RestsOnBook() {
        Order buyOrder = new Order("ord-1", "user-1", "FLASH-USDT", OrderSide.BUY, OrderType.LIMIT, 1000, 50);
        MatchResult result = orderBook.placeOrder(buyOrder);

        assertThat(result.trades()).isEmpty();
        assertThat(result.order().getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(result.order().getRemainingQuantity()).isEqualTo(50);
        assertThat(orderBook.getBestBid()).isEqualTo(1000);
        assertThat(orderBook.getActiveOrderCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Khớp toàn phần (Full Match): 1 Mua và 1 Bán cùng khối lượng và mức giá")
    void testFullMatch_SingleOrder() {
        // Maker đặt bán giá 100, khối lượng 10
        Order sellOrder = new Order("sell-1", "user-1", "FLASH-USDT", OrderSide.SELL, OrderType.LIMIT, 100, 10);
        orderBook.placeOrder(sellOrder);

        // Taker đặt mua giá 100, khối lượng 10
        Order buyOrder = new Order("buy-1", "user-2", "FLASH-USDT", OrderSide.BUY, OrderType.LIMIT, 100, 10);
        MatchResult result = orderBook.placeOrder(buyOrder);

        // Kiểm tra kết quả
        assertThat(result.trades()).hasSize(1);
        Trade trade = result.trades().get(0);
        assertThat(trade.makerOrderId()).isEqualTo("sell-1");
        assertThat(trade.takerOrderId()).isEqualTo("buy-1");
        assertThat(trade.price()).isEqualTo(100);
        assertThat(trade.quantity()).isEqualTo(10);

        // Cả 2 lệnh đều phải FILLED và sổ lệnh sạch bóng
        assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(sellOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(orderBook.getActiveOrderCount()).isEqualTo(0);
        assertThat(orderBook.getBestAsk()).isNull();
        assertThat(orderBook.getBestBid()).isNull();
    }

    @Test
    @DisplayName("Khớp một phần (Partial Match): Taker còn dư khối lượng, phần dư vào sổ lệnh")
    void testPartialMatch_TakerRemaining() {
        // Maker bán 10 ở giá 100
        Order sellOrder = new Order("sell-1", "user-1", "FLASH-USDT", OrderSide.SELL, OrderType.LIMIT, 100, 10);
        orderBook.placeOrder(sellOrder);

        // Taker muốn mua tới 25 ở giá 100
        Order buyOrder = new Order("buy-1", "user-2", "FLASH-USDT", OrderSide.BUY, OrderType.LIMIT, 100, 25);
        MatchResult result = orderBook.placeOrder(buyOrder);

        assertThat(result.trades()).hasSize(1);
        assertThat(result.trades().get(0).quantity()).isEqualTo(10);

        // Taker khớp 10, còn dư 15 và trạng thái PARTIALLY_FILLED
        assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(buyOrder.getRemainingQuantity()).isEqualTo(15);

        // Phần dư 15 của Taker bây giờ thành Maker nằm bên sổ Bids
        assertThat(orderBook.getBestBid()).isEqualTo(100);
        assertThat(orderBook.getActiveOrderCount()).isEqualTo(1);
        assertThat(orderBook.getOrder("buy-1")).isNotNull();
    }

    @Test
    @DisplayName("Khớp một phần (Partial Match): Maker còn dư khối lượng và tiếp tục chờ trên sổ")
    void testPartialMatch_MakerRemaining() {
        // Maker bán 50 ở giá 100
        Order sellOrder = new Order("sell-1", "user-1", "FLASH-USDT", OrderSide.SELL, OrderType.LIMIT, 100, 50);
        orderBook.placeOrder(sellOrder);

        // Taker mua 20 ở giá 100
        Order buyOrder = new Order("buy-1", "user-2", "FLASH-USDT", OrderSide.BUY, OrderType.LIMIT, 100, 20);
        MatchResult result = orderBook.placeOrder(buyOrder);

        assertThat(result.trades()).hasSize(1);
        assertThat(result.trades().get(0).quantity()).isEqualTo(20);

        assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(sellOrder.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(sellOrder.getRemainingQuantity()).isEqualTo(30);

        // Maker vẫn còn 30 đơn vị trên sổ Asks
        assertThat(orderBook.getBestAsk()).isEqualTo(100);
        assertThat(orderBook.getActiveOrderCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Hai lệnh cùng mức giá, lệnh đến trước khớp trước (FIFO)")
    void testPriceTimePriority_FIFO_SamePrice() {
        // User 1 đặt mua trước ở giá 100, số lượng 10
        Order buy1 = new Order("buy-1", "user-1", "FLASH-USDT", OrderSide.BUY, OrderType.LIMIT, 100, 10);
        orderBook.placeOrder(buy1);

        // User 2 đặt mua sau ở CÙNG GIÁ 100, số lượng 10
        Order buy2 = new Order("buy-2", "user-2", "FLASH-USDT", OrderSide.BUY, OrderType.LIMIT, 100, 10);
        orderBook.placeOrder(buy2);

        // User 3 đến bán 10 ở giá 100
        Order sell = new Order("sell-1", "user-3", "FLASH-USDT", OrderSide.SELL, OrderType.LIMIT, 100, 10);
        MatchResult result = orderBook.placeOrder(sell);

        // BẮT BUỘC lệnh buy-1 (đến trước) phải được khớp!
        assertThat(result.trades()).hasSize(1);
        Trade trade = result.trades().get(0);
        assertThat(trade.makerOrderId()).isEqualTo("buy-1");
        assertThat(buy1.getStatus()).isEqualTo(OrderStatus.FILLED);

        // buy-2 (đến sau) vẫn còn nguyên trên sổ lệnh!
        assertThat(buy2.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(buy2.getRemainingQuantity()).isEqualTo(10);
        assertThat(orderBook.getActiveOrderCount()).isEqualTo(1);
        assertThat(orderBook.getOrder("buy-2")).isNotNull();
    }

    @Test
    @DisplayName("Ưu tiên giá (Price Priority): Giá mua cao hơn được ưu tiên khớp trước")
    void testPricePriority_HigherBidMatchedFirst() {
        // User 1 đặt mua giá 100
        Order buy100 = new Order("buy-100", "user-1", "FLASH-USDT", OrderSide.BUY, OrderType.LIMIT, 100, 10);
        orderBook.placeOrder(buy100);

        // User 2 đặt mua giá 105 (đến sau nhưng trả giá cao hơn!)
        Order buy105 = new Order("buy-105", "user-2", "FLASH-USDT", OrderSide.BUY, OrderType.LIMIT, 105, 10);
        orderBook.placeOrder(buy105);

        // User 3 bán giá 95 (chấp nhận khớp với bất kỳ ai >= 95)
        Order sell = new Order("sell-1", "user-3", "FLASH-USDT", OrderSide.SELL, OrderType.LIMIT, 95, 10);
        MatchResult result = orderBook.placeOrder(sell);

        assertThat(result.trades()).hasSize(1);
        Trade trade = result.trades().get(0);

        // Phải khớp với buy-105 trước vì giá tốt hơn cho người bán!
        assertThat(trade.makerOrderId()).isEqualTo("buy-105");
        assertThat(trade.price()).isEqualTo(105); // Giá khớp lấy theo Maker buy-105
        assertThat(buy105.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(buy100.getStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    @DisplayName("Hủy lệnh đang chờ (Cancel Order) thành công và dọn sạch khỏi sổ")
    void testCancelOrder() {
        Order buyOrder = new Order("buy-cancel", "user-1", "FLASH-USDT", OrderSide.BUY, OrderType.LIMIT, 100, 10);
        orderBook.placeOrder(buyOrder);

        assertThat(orderBook.getActiveOrderCount()).isEqualTo(1);

        // Thực hiện hủy lệnh
        boolean canceled = orderBook.cancelOrder("buy-cancel");
        assertThat(canceled).isTrue();
        assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(orderBook.getActiveOrderCount()).isEqualTo(0);
        assertThat(orderBook.getBestBid()).isNull();

        // Hủy lại lần 2 phải trả về false
        boolean cancelAgain = orderBook.cancelOrder("buy-cancel");
        assertThat(cancelAgain).isFalse();
    }
}
