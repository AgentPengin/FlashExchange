package com.flashexchange.dto;

import com.flashexchange.model.OrderSide;
import com.flashexchange.model.OrderType;

/**
 * Tờ khai đặt lệnh từ người dùng gửi lên qua HTTP POST body (JSON).
 * Sử dụng Java Record: Cực kỳ tinh gọn, bất biến và tự động sinh getter/setter.
 */
public record CreateOrderRequest(
    String userId,
    String symbol,
    OrderSide side,
    OrderType type,
    long price,
    long quantity
) {
}
