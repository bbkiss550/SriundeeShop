package com.sriundee.preorder.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sriundee.preorder.pricing.dto.ExchangeRateResponse;
import com.sriundee.preorder.pricing.service.ExchangeRateService;

@RestController
@RequestMapping("/api/exchange-rate")
public class ExchangeRateApiController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateApiController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/krw-thb")
    public ExchangeRateResponse getKrwToThbRate() {
        return exchangeRateService.getKrwToThbRate();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleExchangeRateError(RuntimeException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "Cannot load exchange rate. Please enter it manually."));
    }
}
