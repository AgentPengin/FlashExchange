package com.flashexchange.model;

/**
 * Một giao dịch khớp lệnh thành công giữa Maker (lệnh nằm chờ sẵn trên sổ) 
 * và Taker (lệnh mới đến chủ động khớp).
 * 
 * Sử dụng Java Record vì bản ghi Trade mang tính bất biến (Immutable Event).
 */
public record Trade(
    long tradeId,
    String symbol,
    String makerOrderId,
    String takerOrderId,
    long price,
    long quantity,
    long executedAt
) {
}
