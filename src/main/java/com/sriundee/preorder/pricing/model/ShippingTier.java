package com.sriundee.preorder.pricing.model;

import java.math.BigDecimal;

public enum ShippingTier {
    S("Digipack / Jewel / KiT", new BigDecimal("0.30")),
    M("Album ปกติ", new BigDecimal("0.55")),
    L("Box Album / Album หนา", new BigDecimal("0.85")),
    XL("Season Greetings / Box Set ใหญ่", new BigDecimal("1.50")),
    MAGAZINE("Magazine / Photobook / DICON / นิตยสารเกาหลี", new BigDecimal("0.90"));

    private final String description;
    private final BigDecimal estimatedWeightKg;

    ShippingTier(String description, BigDecimal estimatedWeightKg) {
        this.description = description;
        this.estimatedWeightKg = estimatedWeightKg;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getEstimatedWeightKg() {
        return estimatedWeightKg;
    }
}
