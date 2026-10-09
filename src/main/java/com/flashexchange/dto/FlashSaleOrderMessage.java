package com.flashexchange.dto;

import java.io.Serializable;

public record FlashSaleOrderMessage (
    String orderId,
    String userId,
    String itemKey,
    long quantity,
    long timestamp
) implements Serializable {
}
