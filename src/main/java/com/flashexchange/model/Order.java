package com.flashexchange.model;

import java.util.Objects;

public class Order {
    private final String orderId;
    private final String userId;
    private final String symbol;
    private final OrderSide side;
    private final OrderType type;
    private final long price;
    private final long initialQuantity;
    private long remainingQuantity;
    private OrderStatus status;
    private final long timestamp;

    public Order(String orderId, String userId, String symbol, OrderSide side, OrderType type, long price, long quantity) {
        if (type == OrderType.LIMIT && price <= 0) {
            throw new IllegalArgumentException("Limit order price must be greater than 0");
        }
        if (quantity <= 0) {
             throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        this.orderId = Objects.requireNonNull(orderId, "orderId cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "symbol cannot be null");
        this.side = Objects.requireNonNull(side, "side cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.price = price;
        this.initialQuantity = quantity;
        this.remainingQuantity = quantity;
        this.status = OrderStatus.NEW;
        this.timestamp = System.nanoTime();
    }

    public void fill(long filledQuantity) {
        if (filledQuantity <= 0 || filledQuantity > remainingQuantity) {
            throw new IllegalArgumentException("Invalid filled quantity");
        }
        this.remainingQuantity -= filledQuantity;
        if (remainingQuantity == 0) {
            this.status = OrderStatus.FILLED;
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }    

    public void cancel() {
        if (this.status == OrderStatus.FILLED) {
            throw new IllegalStateException("Cannot cancel a filled order");
        }
        this.status = OrderStatus.CANCELED;
    }

    public boolean isFilled() {
        return this.status == OrderStatus.FILLED;
    }

    public boolean isCanceled() {
        return this.status == OrderStatus.CANCELED;
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public OrderSide getSide() { return side; }
    public OrderType getType() { return type; }
    public long getPrice() { return price; }
    public long getInitialQuantity() { return initialQuantity; }
    public long getRemainingQuantity() { return remainingQuantity; }
    public OrderStatus getStatus() { return status; }
    public long getTimestamp() { return timestamp; }

    @Override 
    public String toString() {
        return "Order{" +
                "id='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", side=" + side +
                ", type=" + type +
                ", price=" + price +
                ", initial=" + initialQuantity +
                ", remaining=" + remainingQuantity +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }

}