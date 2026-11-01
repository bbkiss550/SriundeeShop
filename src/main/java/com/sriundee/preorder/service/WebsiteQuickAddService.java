package com.sriundee.preorder.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sriundee.preorder.entity.Website;
import com.sriundee.preorder.repository.WebsiteRepository;

@Service
public class WebsiteQuickAddService {
    public static final String NO_WEBSITE = "ไม่มีเว็บ";
    private final WebsiteRepository websites;

    public WebsiteQuickAddService(WebsiteRepository websites) { this.websites = websites; }

    @Transactional
    public Website add(String requestedName) {
        String name = requestedName == null ? "" : Normalizer.normalize(requestedName, Normalizer.Form.NFKC).strip().replaceAll("\\s+", " ");
        String key = normalizedName(name);
        if (name.isBlank() || name.length() > 255 || key.isEmpty() || name.matches("(?s).*[<>\\p{Cntrl}].*")) {
            throw new WebsiteAddException(400, "กรอกชื่อเว็บ 1–255 ตัวอักษร โดยไม่ใช้แท็ก HTML หรืออักขระควบคุม");
        }
        if (!Boolean.TRUE.equals(websites.lockWebsiteCreation())) {
            throw new WebsiteAddException(409, "กำลังเพิ่มเว็บอีกคำขอหนึ่ง กรุณาลองบันทึกอีกครั้ง");
        }
        // Reuse an active website even when OCR adds spaces, punctuation or different letter case.
        for (Website existing : websites.getDataAll()) {
            if (key.equals(normalizedName(existing.getName()))) return existing;
        }
        Website website = new Website();
        website.setName(name);
        website.setDelete("A");
        return websites.save(website);
    }

    static String normalizedName(String name) {
        return Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{M}\\p{N}]", "");
    }

    public static class WebsiteAddException extends RuntimeException {
        private final int status;
        public WebsiteAddException(int status, String message) { super(message); this.status = status; }
        public int status() { return status; }
    }
}
