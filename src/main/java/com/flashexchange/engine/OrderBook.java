package com.flashexchange.engine;

import com.flashexchange.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Quy tắc thị trường:
 * 1. Bids (Mua): Sắp xếp giá GIẢM DẦN (Ưu tiên mua giá cao nhất trước).
 * 2. Asks (Bán): Sắp xếp giá TĂNG DẦN (Ưu tiên bán giá thấp nhất trước).
 * 3. Cùng mức giá: FIFO (Lệnh đến trước đứng trước hàng đợi và khớp trước).
 * 4. Khi khớp: Giá khớp tính theo giá của MAKER (người đã đặt lệnh nằm chờ sẵn).
 * 5. Phần còn dư của lệnh Taker chưa khớp hết: Treo lên sổ tương ứng.
 */

public class OrderBook {
    public static long currentEpochNanos() {
        Instant now = Instant.now();
        return now.getEpochSecond() * 1_000_000_000L + now.getNano();
    }
    
    private final String symbol;
    private final TreeMap<Long, ArrayDeque<Order>> bids;
    private final TreeMap<Long, ArrayDeque<Order>> asks;

    private final Map<String, Order> activeOrders; 

    private final AtomicLong tradeSequence;

    public OrderBook(String symbol) {
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.bids = new TreeMap<> (Collections.reverseOrder());
        this.asks = new TreeMap<>();
        this.activeOrders = new HashMap<>();
        this.tradeSequence = new AtomicLong(1);
    }

    public synchronized MatchResult placeOrder(Order incomingOrder) {
        if (!incomingOrder.getSymbol().equalsIgnoreCase(this.symbol)) {
            throw new IllegalArgumentException("Order symbol does not match order book symbol");
        }

        List<Trade> trades = new ArrayList<>();

        if (incomingOrder.getSide() == OrderSide.BUY) {
            matchBuyOrder(incomingOrder, trades);
        } else {
            matchSellOrder(incomingOrder, trades);
        }
        
        if (!incomingOrder.isFilled()) {
            addOrderToBook(incomingOrder);
        }

        return new MatchResult(incomingOrder, trades);
    }

    private void matchBuyOrder(Order buyOrder, List<Trade> trades) {
        while(!buyOrder.isFilled() && !asks.isEmpty()) {
            Map.Entry<Long, ArrayDeque<Order>> bestAskEntry = asks.firstEntry();
            long bestAskPrice = bestAskEntry.getKey();
            if (buyOrder.getPrice() < bestAskPrice && buyOrder.getType() == OrderType.LIMIT) {
                break; 
            }            
            ArrayDeque<Order> queue = bestAskEntry.getValue();
            while(!buyOrder.isFilled() && !queue.isEmpty()) {
                Order makerSellOrder = queue.peekFirst();

                long matchPrice = makerSellOrder.getPrice();
                long matchQuantity = Math.min(buyOrder.getRemainingQuantity(), makerSellOrder.getRemainingQuantity());

                buyOrder.fill(matchQuantity);
                makerSellOrder.fill(matchQuantity);

                trades.add(new Trade(
                    tradeSequence.getAndIncrement(),
                    this.symbol,
                    makerSellOrder.getOrderId(),
                    buyOrder.getOrderId(),
                    matchPrice,
                    matchQuantity,
                    currentEpochNanos()
                ));
                if (makerSellOrder.isFilled()) {
                    queue.pollFirst();
                    activeOrders.remove(makerSellOrder.getOrderId());
                }

            }
            if (queue.isEmpty()) {
                asks.pollFirstEntry();
            }
        }
    }

    private void matchSellOrder(Order sellOrder, List<Trade> trades) {
        while(!sellOrder.isFilled() && !bids.isEmpty()) {
            Map.Entry<Long, ArrayDeque<Order>> bestBidEntry = bids.firstEntry();
            long bestBidPrice = bestBidEntry.getKey();
            if (sellOrder.getPrice() > bestBidPrice && sellOrder.getType() == OrderType.LIMIT) {
                break; 
            }
            ArrayDeque<Order> queue = bestBidEntry.getValue();
            while(!sellOrder.isFilled() && !queue.isEmpty()) {
                Order makerBuyOrder = queue.peekFirst();

                long matchPrice = makerBuyOrder.getPrice();
                long matchQuantity = Math.min(sellOrder.getRemainingQuantity(), makerBuyOrder.getRemainingQuantity());

                sellOrder.fill(matchQuantity);
                makerBuyOrder.fill(matchQuantity);

                trades.add(new Trade(
                    tradeSequence.getAndIncrement(),
                    this.symbol,
                    makerBuyOrder.getOrderId(),
                    sellOrder.getOrderId(),
                    matchPrice,
                    matchQuantity,
                    currentEpochNanos()
                ));
                if (makerBuyOrder.isFilled()) {
                    queue.pollFirst();
                    activeOrders.remove(makerBuyOrder.getOrderId());
                }
            }
            if (queue.isEmpty()) {
                bids.pollFirstEntry();
            }
        }
    }

    private void addOrderToBook(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            bids.computeIfAbsent(order.getPrice(), k -> new ArrayDeque<>()).addLast(order);
        } else {
            asks.computeIfAbsent(order.getPrice(), k -> new ArrayDeque<>()).addLast(order);
        }
        activeOrders.put(order.getOrderId(), order);
    }

    public synchronized boolean cancelOrder(String orderId) {
        Order order = activeOrders.get(orderId);
        if (order == null || order.isFilled() || order.isCanceled()) {
            return false;
        }
        if (order.getSide() == OrderSide.BUY) {
            ArrayDeque<Order> queue = bids.get(order.getPrice());
            if (queue != null) {
                queue.remove(order);
                if (queue.isEmpty()) {
                    bids.remove(order.getPrice());
                }
            }
        } else {
            ArrayDeque<Order> queue = asks.get(order.getPrice());
            if (queue != null) {
                queue.remove(order);
                if (queue.isEmpty()) {
                    asks.remove(order.getPrice());
                }
            }
        }
        order.cancel();
        activeOrders.remove(order.getOrderId());
        return true;
    }

    public synchronized Long getBestBid() {
        return bids.isEmpty() ? null : bids.firstKey();
    }

    public synchronized Long getBestAsk() {
        return asks.isEmpty() ? null : asks.firstKey();
    }

    public synchronized Order getOrder(String orderId) {
        return activeOrders.get(orderId);
    }

    public synchronized int getActiveOrderCount() {
        return activeOrders.size();
    }

    public synchronized int getBidPriceLevelsCount() {
        return bids.size();
    }

    public synchronized int getAskPriceLevelsCount() {
        return asks.size();
    }

    public String getSymbol() {
        return symbol;
    }
}
