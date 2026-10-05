package com.flashexchange.controller;

import com.flashexchange.dto.CreateOrderRequest;
import com.flashexchange.dto.OrderBookDepthResponse;
import com.flashexchange.dto.PlaceOrderResponse;
import com.flashexchange.engine.OrderBook;
import com.flashexchange.model.MatchResult;
import com.flashexchange.model.Order;
import com.flashexchange.model.Trade;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")

public class OrderController {

    private final OrderBook orderBook;
    private final SimpMessagingTemplate messagingTemplate;

    // Dependency Injection: Spring Boot tự động đưa bean defaultOrderBook vào đây
    public OrderController(OrderBook orderBook, SimpMessagingTemplate messagingTemplate) {
        this.orderBook = orderBook;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * API 1: Đặt lệnh mới
     * Endpoint: POST http://localhost:8080/api/v1/orders
     * Body: JSON theo mẫu CreateOrderRequest
     */
    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(@RequestBody CreateOrderRequest request) {
        String orderId = "ord-" + UUID.randomUUID().toString().substring(0, 8);
        Order order = new Order(orderId, request.userId(), request.symbol(), request.side(), request.type(), request.price(), request.quantity());
        MatchResult matchResult = orderBook.placeOrder(order);
        PlaceOrderResponse response = new PlaceOrderResponse(
            matchResult.order().getOrderId(),
            matchResult.order().getSymbol(),
            matchResult.order().getStatus(),
            matchResult.order().getRemainingQuantity(),
            matchResult.trades()
        );
        if (!matchResult.trades().isEmpty()) {
            matchResult.trades().forEach(trade -> 
                messagingTemplate.convertAndSend("/topic/trades", trade)
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API 2: Hủy lệnh đang chờ
     * Endpoint: DELETE http://localhost:8080/api/v1/orders/{orderId}
     * 
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId) {
        // TODO: Em hãy hoàn thiện logic hủy lệnh ở đây!
        boolean cancelled = orderBook.cancelOrder(orderId);
        if (cancelled) {
            return ResponseEntity.ok("Cancelled successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found or already filled");
        }
    }

    /**
     * API 3: Xem trạng thái sổ lệnh (Độ sâu / Depth)
     * Endpoint: GET http://localhost:8080/api/v1/orders/depth
     */
    @GetMapping("/depth")
    public ResponseEntity<OrderBookDepthResponse> getDepth() {
        String symbol = orderBook.getSymbol();
        Long bestBid = orderBook.getBestBid();
        Long bestAsk = orderBook.getBestAsk();
        int activeOrderCount = orderBook.getActiveOrderCount();
        int bidPriceLevels = orderBook.getBidPriceLevelsCount();
        int askPriceLevels = orderBook.getAskPriceLevelsCount();

        OrderBookDepthResponse response = new OrderBookDepthResponse(
            symbol,
            bestBid,
            bestAsk,
            activeOrderCount,
            bidPriceLevels,
            askPriceLevels
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API 4: Tra cứu thông tin 1 lệnh theo ID
     * Endpoint: GET http://localhost:8080/api/v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        Order order = orderBook.getOrder(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(order);
    }
}
