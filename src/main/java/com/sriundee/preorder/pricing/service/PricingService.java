package com.sriundee.preorder.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.sriundee.preorder.pricing.dto.PricingRequest;
import com.sriundee.preorder.pricing.dto.PricingResponse;
import com.sriundee.preorder.pricing.model.ShippingTier;

@Service
public class PricingService {

    public PricingResponse calculate(PricingRequest request) {
        BigDecimal koreanCostTHB = request.getKoreanPriceKRW()
                .add(request.getKoreanShippingKRW())
                .multiply(request.getExchangeRate());

        BigDecimal estimatedItemWeightKg = request.getShippingTier().getEstimatedWeightKg();
        BigDecimal returnShippingRatePerKg = request.getShippingMethod().getRatePerKg();
        BigDecimal estimatedReturnShippingTHB = estimatedItemWeightKg.multiply(returnShippingRatePerKg);
        BigDecimal priceBeforeRounding = koreanCostTHB.add(estimatedReturnShippingTHB);
        BigDecimal recommendedSellingPrice = roundUp(priceBeforeRounding, request.getRoundMode().getStep());

        BigDecimal estimatedProfit = recommendedSellingPrice
                .subtract(koreanCostTHB)
                .subtract(estimatedReturnShippingTHB);
        BigDecimal estimatedMarginPercent = estimatedProfit
                .multiply(new BigDecimal("100"))
                .divide(recommendedSellingPrice, 2, RoundingMode.HALF_UP);

        boolean highRiskProduct = request.getShippingTier() == ShippingTier.MAGAZINE;

        return new PricingResponse(
                koreanCostTHB.setScale(2, RoundingMode.HALF_UP),
                request.getShippingTier().name(),
                request.getShippingTier().getDescription(),
                request.getShippingMethod().name(),
                request.getShippingMethod().getDescription(),
                estimatedItemWeightKg,
                returnShippingRatePerKg.setScale(2, RoundingMode.HALF_UP),
                estimatedReturnShippingTHB.setScale(2, RoundingMode.HALF_UP),
                priceBeforeRounding.setScale(2, RoundingMode.HALF_UP),
                recommendedSellingPrice.setScale(0, RoundingMode.UNNECESSARY),
                estimatedProfit.setScale(2, RoundingMode.HALF_UP),
                estimatedMarginPercent,
                null,
                highRiskProduct);
    }

    private BigDecimal roundUp(BigDecimal value, BigDecimal step) {
        return value.divide(step, 0, RoundingMode.CEILING).multiply(step);
    }
}
