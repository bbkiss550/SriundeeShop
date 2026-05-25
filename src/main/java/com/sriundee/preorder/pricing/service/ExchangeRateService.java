package com.sriundee.preorder.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.sriundee.preorder.pricing.dto.ExchangeRateResponse;

@Service
public class ExchangeRateService {

    private static final String SOURCE = "Frankfurter";

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.frankfurter.dev/v1")
            .build();

    public ExchangeRateResponse getKrwToThbRate() {
        FrankfurterResponse response = restClient.get()
                .uri("/latest?base=KRW&symbols=THB")
                .retrieve()
                .body(FrankfurterResponse.class);

        if (response == null || response.getRates() == null || !response.getRates().containsKey("THB")) {
            throw new IllegalStateException("Exchange rate is unavailable.");
        }

        BigDecimal rate = response.getRates().get("THB").setScale(4, RoundingMode.HALF_UP);
        return new ExchangeRateResponse("KRW", "THB", rate, response.getDate(), SOURCE);
    }

    public static class FrankfurterResponse {

        private String date;
        private Map<String, BigDecimal> rates;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Map<String, BigDecimal> getRates() {
            return rates;
        }

        public void setRates(Map<String, BigDecimal> rates) {
            this.rates = rates;
        }
    }
}
