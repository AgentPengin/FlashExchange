package com.flashexchange.dto;

/**
 * Snapshot tình trạng sổ lệnh:
 * - Giá Mua cao nhất (Best Bid)
 * - Giá Bán thấp nhất (Best Ask)
 * - Tổng số lệnh đang active trên sổ
 */
public record OrderBookDepthResponse(
    String symbol,
    Long bestBid,
    Long bestAsk,
    int activeOrderCount,
    int bidPriceLevels,
    int askPriceLevels
) {
}
