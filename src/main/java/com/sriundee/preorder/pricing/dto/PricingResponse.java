package com.sriundee.preorder.pricing.dto;

import java.math.BigDecimal;

public class PricingResponse {

    private final BigDecimal koreanCostTHB;
    private final String shippingTier;
    private final String shippingTierDescription;
    private final String shippingMethod;
    private final String shippingMethodDescription;
    private final BigDecimal estimatedItemWeightKg;
    private final BigDecimal returnShippingRatePerKg;
    private final BigDecimal estimatedReturnShippingTHB;
    private final BigDecimal priceBeforeRounding;
    private final BigDecimal recommendedSellingPrice;
    private final BigDecimal estimatedProfit;
    private final BigDecimal estimatedMarginPercent;
    private final String warning;
    private final boolean highRiskProduct;

    public PricingResponse(
            BigDecimal koreanCostTHB,
            String shippingTier,
            String shippingTierDescription,
            String shippingMethod,
            String shippingMethodDescription,
            BigDecimal estimatedItemWeightKg,
            BigDecimal returnShippingRatePerKg,
            BigDecimal estimatedReturnShippingTHB,
            BigDecimal priceBeforeRounding,
            BigDecimal recommendedSellingPrice,
            BigDecimal estimatedProfit,
            BigDecimal estimatedMarginPercent,
            String warning,
            boolean highRiskProduct) {
        this.koreanCostTHB = koreanCostTHB;
        this.shippingTier = shippingTier;
        this.shippingTierDescription = shippingTierDescription;
        this.shippingMethod = shippingMethod;
        this.shippingMethodDescription = shippingMethodDescription;
        this.estimatedItemWeightKg = estimatedItemWeightKg;
        this.returnShippingRatePerKg = returnShippingRatePerKg;
        this.estimatedReturnShippingTHB = estimatedReturnShippingTHB;
        this.priceBeforeRounding = priceBeforeRounding;
        this.recommendedSellingPrice = recommendedSellingPrice;
        this.estimatedProfit = estimatedProfit;
        this.estimatedMarginPercent = estimatedMarginPercent;
        this.warning = warning;
        this.highRiskProduct = highRiskProduct;
    }

    public BigDecimal getKoreanCostTHB() {
        return koreanCostTHB;
    }

    public String getShippingTier() {
        return shippingTier;
    }

    public String getShippingTierDescription() {
        return shippingTierDescription;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public String getShippingMethodDescription() {
        return shippingMethodDescription;
    }

    public BigDecimal getEstimatedItemWeightKg() {
        return estimatedItemWeightKg;
    }

    public BigDecimal getReturnShippingRatePerKg() {
        return returnShippingRatePerKg;
    }

    public BigDecimal getEstimatedReturnShippingTHB() {
        return estimatedReturnShippingTHB;
    }

    public BigDecimal getPriceBeforeRounding() {
        return priceBeforeRounding;
    }

    public BigDecimal getRecommendedSellingPrice() {
        return recommendedSellingPrice;
    }

    public BigDecimal getEstimatedProfit() {
        return estimatedProfit;
    }

    public BigDecimal getEstimatedMarginPercent() {
        return estimatedMarginPercent;
    }

    public String getWarning() {
        return warning;
    }

    public boolean isHighRiskProduct() {
        return highRiskProduct;
    }
}
