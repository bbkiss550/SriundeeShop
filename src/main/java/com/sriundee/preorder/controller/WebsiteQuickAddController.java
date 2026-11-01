package com.sriundee.preorder.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sriundee.preorder.entity.Website;
import com.sriundee.preorder.service.WebsiteQuickAddService;
import com.sriundee.preorder.service.WebsiteQuickAddService.WebsiteAddException;

@RestController
@RequestMapping("/product/setup/websites")
public class WebsiteQuickAddController {
    private static final Logger LOG = LoggerFactory.getLogger(WebsiteQuickAddController.class);
    private final WebsiteQuickAddService service;
    public WebsiteQuickAddController(WebsiteQuickAddService service) { this.service = service; }
    public record WebsiteInput(String name) {}

    @PostMapping
    public Website add(@RequestBody WebsiteInput input) {
        return service.add(input.name());
    }

    @ExceptionHandler(WebsiteAddException.class)
    public ResponseEntity<?> validation(WebsiteAddException error) {
        return ResponseEntity.status(error.status()).body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> failure(Exception error) {
        LOG.error("Unable to add website from product form", error);
        return ResponseEntity.internalServerError().body(Map.of("message", "เพิ่มเว็บไม่สำเร็จ กรุณาลองอีกครั้ง ข้อมูลสินค้าเดิมยังอยู่"));
    }
}
