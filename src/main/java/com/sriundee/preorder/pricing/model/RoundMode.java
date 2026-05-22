package com.sriundee.preorder.pricing.model;

import java.math.BigDecimal;

public enum RoundMode {
    ROUND_UP_10(new BigDecimal("10")),
    ROUND_UP_50(new BigDecimal("50"));

    private final BigDecimal step;

    RoundMode(BigDecimal step) {
        this.step = step;
    }

    public BigDecimal getStep() {
        return step;
    }
}
