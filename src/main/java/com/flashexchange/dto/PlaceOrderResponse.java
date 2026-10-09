package com.flashexchange.dto;

import com.flashexchange.model.OrderStatus;
import com.flashexchange.model.Trade;

import java.util.List;

/**
 * Phản hồi trả về sau khi đặt lệnh thành công:
 * Báo cho khách biết ID lệnh, trạng thái (FILLED / PARTIALLY_FILLED / NEW)
 * và danh sách các Trade đã khớp ngay lập tức.
 */
public record PlaceOrderResponse(
    String orderId,
    String symbol,
    OrderStatus status,
    long remainingQuantity,
    List<Trade> executedTrades
) {}