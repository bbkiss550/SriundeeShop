package com.sriundee.preorder.pricing.dto;

import java.math.BigDecimal;

public class ExchangeRateResponse {

    private final String baseCurrency;
    private final String targetCurrency;
    private final BigDecimal rate;
    private final String date;
    private final String source;

    public ExchangeRateResponse(String baseCurrency, String targetCurrency, BigDecimal rate, String date, String source) {
        this.baseCurrency = baseCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.date = date;
        this.source = source;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public String getDate() {
        return date;
    }

    public String getSource() {
        return source;
    }
}
