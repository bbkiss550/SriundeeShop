package com.sriundee.preorder.controller;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sriundee.preorder.entity.Type;
import com.sriundee.preorder.service.TypeQuickAddService;

@RestController
@RequestMapping("/product/setup/types")
public class TypeQuickAddController {
    private static final Logger LOG = LoggerFactory.getLogger(TypeQuickAddController.class);
    private final TypeQuickAddService service;
    public TypeQuickAddController(TypeQuickAddService service) { this.service = service; }
    public record TypeInput(String name) {}
    @PostMapping
    public Type add(@RequestBody TypeInput input) { return service.add(input.name()); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> conflict(IllegalStateException error) {
        return ResponseEntity.status(409).body(Map.of("message", error.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> failure(Exception error) {
        LOG.error("Unable to add product type", error);
        return ResponseEntity.internalServerError().body(Map.of("message", "เพิ่มประเภทไม่สำเร็จ กรุณาลองอีกครั้ง ข้อมูลสินค้าเดิมยังอยู่"));
    }
}
