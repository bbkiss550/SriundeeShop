package com.sriundee.preorder.controller;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sriundee.preorder.entity.Artist;
import com.sriundee.preorder.entity.Group;
import com.sriundee.preorder.service.ArtistQuickAddService;
import com.sriundee.preorder.service.ArtistQuickAddService.ArtistAddException;

@RestController
@RequestMapping("/product/setup/artists")
public class ArtistQuickAddController {
    private static final Logger LOG = LoggerFactory.getLogger(ArtistQuickAddController.class);
    private final ArtistQuickAddService service;
    public ArtistQuickAddController(ArtistQuickAddService service) { this.service = service; }
    public record ArtistInput(String name, Integer groupId) {}
    @GetMapping("/groups")
    public List<Group> groups() { return service.groups(); }
    @PostMapping
    public Artist add(@RequestBody ArtistInput input) { return service.add(input.name(), input.groupId()); }
    @ExceptionHandler(ArtistAddException.class)
    public ResponseEntity<?> validation(ArtistAddException error) {
        return ResponseEntity.status(error.status()).body(Map.of("message", error.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> failure(Exception error) {
        LOG.error("Unable to add artist from product form", error);
        return ResponseEntity.internalServerError().body(Map.of("message", "เพิ่มศิลปินไม่สำเร็จ กรุณาลองอีกครั้ง ข้อมูลสินค้าเดิมยังอยู่"));
    }
}
