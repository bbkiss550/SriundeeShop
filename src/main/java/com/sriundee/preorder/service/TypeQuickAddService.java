package com.sriundee.preorder.service;

import java.text.Normalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sriundee.preorder.entity.Type;
import com.sriundee.preorder.repository.TypeRepository;

@Service
public class TypeQuickAddService {
    private final TypeRepository types;
    public TypeQuickAddService(TypeRepository types) { this.types = types; }

    @Transactional
    public Type add(String requestedName) {
        String name = requestedName == null ? "" : Normalizer.normalize(requestedName, Normalizer.Form.NFKC).strip().replaceAll("\\s+", " ");
        if (name.isBlank() || name.length() > 255 || WebsiteQuickAddService.normalizedName(name).isEmpty()
                || name.matches("(?s).*[<>\\p{Cntrl}].*")) {
            throw new IllegalArgumentException("กรอกชื่อประเภท 1–255 ตัวอักษร โดยไม่ใช้แท็ก HTML หรืออักขระควบคุม");
        }
        if (!Boolean.TRUE.equals(types.lockTypeCreation())) throw new IllegalStateException("กำลังเพิ่มประเภทอีกคำขอหนึ่ง กรุณาลองอีกครั้ง");
        String key = WebsiteQuickAddService.normalizedName(name);
        for (Type type : types.getDropdownData()) {
            if (key.equals(WebsiteQuickAddService.normalizedName(type.getName()))) return type;
        }
        Type type = new Type();
        type.setName(name); type.setDelete("A");
        return types.save(type);
    }
}
