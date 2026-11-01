package com.sriundee.preorder.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import jakarta.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;
import com.sriundee.preorder.service.LocalProductImageReader.ReadProgress;
import com.sriundee.preorder.service.LocalProductImageReader.ProgressView;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.sriundee.preorder.dto.ProductImageDraft;
import com.sriundee.preorder.service.LocalProductImageReader;
import com.sriundee.preorder.service.LocalProductImageReader.ImageReadException;
import com.sriundee.preorder.service.ProductImageTextParser;

@RestController
@RequestMapping("/product/setup/image")
public class ProductImageController {
    private final LocalProductImageReader reader;
    private final ProductImageTextParser parser;
    private final com.sriundee.preorder.service.ProductImageAiReader aiReader;

    public ProductImageController(LocalProductImageReader reader, ProductImageTextParser parser) {
        this(reader,parser,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public ProductImageController(LocalProductImageReader reader,ProductImageTextParser parser,com.sriundee.preorder.service.ProductImageAiReader aiReader){this.reader=reader;this.parser=parser;this.aiReader=aiReader;}

    @PostMapping(value = "/read", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductImageDraft read(@RequestParam("image") MultipartFile image,
            @RequestHeader(value = "X-OCR-Request-Id", required = false) String requestId, HttpSession session) {
        if(requestId==null && (aiReader==null || !(session.getAttribute(LoginController.SESSION_USER_ID) instanceof Number)))return reader.read(image);
        ReadProgress progress = requestId==null?new ReadProgress():job(session, requestId, true);
        try {Object user=session.getAttribute(LoginController.SESSION_USER_ID);
            return aiReader!=null && user instanceof Number id?aiReader.read(id.intValue(),image,progress):reader.read(image, progress); }
        finally { progress.finish(); }
    }

    @GetMapping("/progress/{id}")
    public ProgressView progress(@PathVariable String id, HttpSession session) {
        ReadProgress progress = job(session, id, false);
        return progress == null ? new ProgressView(0, "กำลังส่งภาพส่วนที่เลือก", false, false) : progress.view();
    }

    @PostMapping("/cancel/{id}")
    public ProgressView cancel(@PathVariable String id, HttpSession session) {
        ReadProgress progress = job(session, id, true);
        progress.cancel();
        return progress.view();
    }

    // Jobs belong to the logged-in session. Retain only a small bounded history.
    @SuppressWarnings("unchecked")
    private ReadProgress job(HttpSession session, String id, boolean create) {
        if (!id.matches("[a-fA-F0-9-]{36}")) throw new IllegalArgumentException("รหัสงานอ่านภาพไม่ถูกต้อง");
        synchronized (session) {
            var jobs = (Map<String, ReadProgress>) session.getAttribute("productImageJobs");
            if (jobs == null) {
                jobs = new ConcurrentHashMap<>(); session.setAttribute("productImageJobs", jobs);
            }
            if (!create) return jobs.get(id);
            if (!jobs.containsKey(id)) {
                // Keep cancelled IDs briefly so a late upload cannot restart an already stopped job.
                jobs.entrySet().removeIf(entry -> entry.getValue().expired());
                if (jobs.size() >= 64) throw new ImageReadException(429, "มีงานอ่านภาพค้างอยู่ กรุณารอสักครู่");
                jobs.put(id, new ReadProgress());
            }
            return jobs.get(id);
        }
    }

    public record TextInput(String text) {}

    @PostMapping("/parse")
    public ProductImageDraft parse(@RequestBody TextInput input) {
        return parser.parse(input.text());
    }

    @ExceptionHandler(ImageReadException.class)
    public ResponseEntity<?> readingError(ImageReadException error) {
        return ResponseEntity.status(error.status()).body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> tooLarge(MaxUploadSizeExceededException error) {
        return ResponseEntity.status(413).body(Map.of("message", "ภาพต้องมีขนาดไม่เกิน 8 MB"));
    }
}
