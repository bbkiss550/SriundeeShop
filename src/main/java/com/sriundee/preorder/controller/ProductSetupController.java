package com.sriundee.preorder.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sriundee.preorder.dto.ProductSetupDto;
import com.sriundee.preorder.entity.Website;
import com.sriundee.preorder.repository.WebsiteRepository;
import com.sriundee.preorder.service.ProductSetupService;
import com.sriundee.preorder.service.ProductSetupService.SetupValidationException;

@RestController
@RequestMapping("/product/setup")
public class ProductSetupController {
    private static final Logger LOG = LoggerFactory.getLogger(ProductSetupController.class);
    private final ProductSetupService service;
    private final WebsiteRepository websites;

    public ProductSetupController(ProductSetupService service, WebsiteRepository websites) {
        this.service = service;
        this.websites = websites;
    }

    @GetMapping("/options")
    public List<Website> options() {
        return websites.getDataAll();
    }

    @GetMapping("/{id}")
    public ProductSetupService.SetupView load(@PathVariable Integer id) {
        return service.load(id);
    }

    @PostMapping("/save")
    public ProductSetupService.SaveResult save(@RequestBody ProductSetupDto setup) {
        return service.save(setup);
    }

    @ExceptionHandler(SetupValidationException.class)
    public ResponseEntity<?> validation(SetupValidationException error) {
        return ResponseEntity.badRequest().body(Map.of("message", error.getMessage(), "field", error.field()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> invalidBody(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body(Map.of("message", "รูปแบบข้อมูลไม่ถูกต้อง กรุณาตรวจราคาและข้อมูลที่กรอก"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> failure(Exception error) {
        LOG.error("Unable to save or load product setup", error);
        return ResponseEntity.internalServerError().body(Map.of("message", "ไม่สามารถบันทึกหรือโหลดสินค้าได้ กรุณาลองใหม่ ข้อมูลที่กรอกยังอยู่"));
    }
}
