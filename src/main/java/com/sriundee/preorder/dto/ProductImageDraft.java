package com.sriundee.preorder.dto;

import java.math.BigDecimal;
import java.util.List;

/** Suggestions from the current upload only, without reading or writing shop data. */
public record ProductImageDraft(String productName, List<VersionDraft> versions,
        List<String> warnings, String rawText,String readSource,String readMessage) {
    public ProductImageDraft(String productName,List<VersionDraft> versions,List<String> warnings,String rawText){this(productName,versions,warnings,rawText,"TEXT","จัดกลุ่มจากข้อความด้วยกฎในระบบ");}
    public ProductImageDraft withReadInfo(String source,String message){return new ProductImageDraft(productName,versions,warnings,rawText,source,message);}
    public record VersionDraft(String name, List<PriceDraft> groups) {}
    public record PriceDraft(String name, List<String> websites, BigDecimal price, BigDecimal deposit) {}
}
