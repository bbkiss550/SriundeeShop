package com.sriundee.preorder.pricing.model;

import java.math.BigDecimal;

public enum ShippingMethod {
    SEA("เรือ", new BigDecimal("150")),
    AIR("เครื่องบิน", new BigDecimal("400"));

    private final String description;
    private final BigDecimal ratePerKg;

    ShippingMethod(String description, BigDecimal ratePerKg) {
        this.description = description;
        this.ratePerKg = ratePerKg;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getRatePerKg() {
        return ratePerKg;
    }
}
