package com.sriundee.preorder.pricing.dto;

import java.math.BigDecimal;

import com.sriundee.preorder.pricing.model.RoundMode;
import com.sriundee.preorder.pricing.model.ShippingMethod;
import com.sriundee.preorder.pricing.model.ShippingTier;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class PricingRequest {

    @NotNull(message = "Korean price is required.")
    @DecimalMin(value = "0.01", message = "Korean price must be greater than 0.")
    private BigDecimal koreanPriceKRW;

    @NotNull(message = "Korean shipping is required.")
    @DecimalMin(value = "0.00", message = "Korean shipping cannot be negative.")
    private BigDecimal koreanShippingKRW;

    @NotNull(message = "Exchange rate is required.")
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than 0.")
    private BigDecimal exchangeRate;

    @NotNull(message = "Shipping tier is required.")
    private ShippingTier shippingTier;

    @NotNull(message = "Shipping method is required.")
    private ShippingMethod shippingMethod;

    @NotNull(message = "Round mode is required.")
    private RoundMode roundMode;

    public BigDecimal getKoreanPriceKRW() {
        return koreanPriceKRW;
    }

    public void setKoreanPriceKRW(BigDecimal koreanPriceKRW) {
        this.koreanPriceKRW = koreanPriceKRW;
    }

    public BigDecimal getKoreanShippingKRW() {
        return koreanShippingKRW;
    }

    public void setKoreanShippingKRW(BigDecimal koreanShippingKRW) {
        this.koreanShippingKRW = koreanShippingKRW;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public ShippingTier getShippingTier() {
        return shippingTier;
    }

    public void setShippingTier(ShippingTier shippingTier) {
        this.shippingTier = shippingTier;
    }

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(ShippingMethod shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public RoundMode getRoundMode() {
        return roundMode;
    }

    public void setRoundMode(RoundMode roundMode) {
        this.roundMode = roundMode;
    }
}
