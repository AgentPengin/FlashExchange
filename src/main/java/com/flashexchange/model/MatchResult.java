package com.flashexchange.model;

import java.util.Collections;
import java.util.List;

public record MatchResult(
    Order order,
    List<Trade> trades
) {
    public MatchResult {
        trades = Collections.unmodifiableList(trades); // Ensure the list of trades is immutable
    }
}
