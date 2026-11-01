package com.sriundee.preorder.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sriundee.preorder.dto.ProductDto;
import com.sriundee.preorder.dto.ProductSetupDto;
import com.sriundee.preorder.dto.ProductSetupDto.PriceGroup;
import com.sriundee.preorder.dto.ProductSetupDto.VersionInput;
import com.sriundee.preorder.entity.Cover;
import com.sriundee.preorder.entity.Product;
import com.sriundee.preorder.entity.Version;
import com.sriundee.preorder.entity.Website;
import com.sriundee.preorder.repository.ArtistRepository;
import com.sriundee.preorder.repository.CoverRepository;
import com.sriundee.preorder.repository.ProductRepository;
import com.sriundee.preorder.repository.ProductStatusRepository;
import com.sriundee.preorder.repository.TypeRepository;
import com.sriundee.preorder.repository.VersionRepository;
import com.sriundee.preorder.repository.WebsiteRepository;

@Service
public class ProductSetupService {
    private final ProductRepository products;
    private final VersionRepository versions;
    private final CoverRepository covers;
    private final WebsiteRepository websites;
    private final TypeRepository types;
    private final ArtistRepository artists;
    private final ProductStatusRepository statuses;

    public ProductSetupService(ProductRepository products, VersionRepository versions,
            CoverRepository covers, WebsiteRepository websites, TypeRepository types,
            ArtistRepository artists, ProductStatusRepository statuses) {
        this.products = products;
        this.versions = versions;
        this.covers = covers;
        this.websites = websites;
        this.types = types;
        this.artists = artists;
        this.statuses = statuses;
    }

    public record SetupView(ProductDto product, List<VersionInput> versions, List<Website> websites) {}
    public record SaveResult(Integer productId, int versionCount, int offerCount) {}

    @Transactional(readOnly = true)
    public SetupView load(Integer id) {
        Product product = requireProduct(id);
        List<Version> productVersions = versions.getDataByID_pro(id);
        List<Cover> productCovers = covers.findByProductAndDeleteOrderByIdAsc(id, "A");
        List<VersionInput> inputs = new ArrayList<>();
        for (Version version : productVersions) {
            List<PriceGroup> groups = new ArrayList<>();
            for (Cover cover : productCovers) {
                if (!Objects.equals(cover.getVersion(), version.getId())) {
                    continue;
                }
                PriceGroup matching = groups.stream().filter(group ->
                        Objects.equals(group.name(), cover.getName())
                        && group.price().compareTo(BigDecimal.valueOf(cover.getPrice_total())) == 0
                        && group.deposit().compareTo(BigDecimal.valueOf(cover.getPrice_pledge())) == 0
                        && !group.websiteIds().contains(cover.getWebsite())).findFirst().orElse(null);
                if (matching == null) {
                    matching = new PriceGroup(cover.getName(), new ArrayList<>(),
                            BigDecimal.valueOf(cover.getPrice_total()),
                            BigDecimal.valueOf(cover.getPrice_pledge()), new LinkedHashMap<>());
                    groups.add(matching);
                }
                matching.websiteIds().add(cover.getWebsite());
                matching.coverIds().put(cover.getWebsite(), cover.getId());
            }
            inputs.add(new VersionInput(version.getId(), version.getName(), groups));
        }
        // Keep an inactive website visible when an existing offer still references it.
        Map<Integer, Website> options = new LinkedHashMap<>();
        websites.getDataAll().forEach(site -> options.put(site.getId(), site));
        Set<Integer> referencedSites = new HashSet<>();
        productCovers.forEach(cover -> referencedSites.add(cover.getWebsite()));
        if (!referencedSites.isEmpty()) {
            websites.findAllById(referencedSites).forEach(site -> options.putIfAbsent(site.getId(), site));
        }
        ProductDto productInput = new ProductDto();
        productInput.setId(product.getId());
        productInput.setName(product.getName());
        productInput.setType(product.getType());
        productInput.setArtist(product.getArtist());
        productInput.setProduct_status(product.getProduct_status());
        productInput.setPic(product.getPic());
        // Send calendar dates, avoiding UTC serialization shifting a Thai midnight to the previous day.
        productInput.setEnd_date(product.getEnd_date() == null ? null : new Date(product.getEnd_date().getTime()).toLocalDate().toString());
        productInput.setSend_date(product.getSend_date() == null ? null : new Date(product.getSend_date().getTime()).toLocalDate().toString());
        return new SetupView(productInput, inputs, new ArrayList<>(options.values()));
    }

    @Transactional
    public SaveResult save(ProductSetupDto setup) {
        require(setup != null && setup.product() != null, "product", "กรุณาระบุข้อมูลสินค้า");
        ProductDto input = setup.product();
        Product product = input.getId() == null ? new Product() : requireProduct(input.getId());
        List<Version> oldVersions = input.getId() == null ? List.of() : versions.getDataByID_pro(input.getId());
        List<Cover> oldCovers = input.getId() == null ? List.of()
                : covers.findByProductAndDeleteOrderByIdAsc(input.getId(), "A");
        Map<Integer, Version> versionById = new HashMap<>();
        oldVersions.forEach(version -> versionById.put(version.getId(), version));
        Map<Integer, Cover> coverById = new HashMap<>();
        oldCovers.forEach(cover -> coverById.put(cover.getId(), cover));

        requireText(input.getName(), "product.name", "กรุณากรอกชื่อสินค้า");
        require(input.getType() != null && types.existsById(input.getType()), "product.type", "กรุณาเลือกประเภทสินค้า");
        require(input.getArtist() != null && artists.existsById(input.getArtist()), "product.artist", "กรุณาเลือกศิลปิน");
        require(input.getProduct_status() != null && statuses.existsById(input.getProduct_status()),
                "product.product_status", "กรุณาเลือกสถานะสินค้า");
        Date endDate = parseDate(input.getEnd_date(), "product.end_date");
        Date sendDate = parseDate(input.getSend_date(), "product.send_date");
        require(setup.versions() != null && !setup.versions().isEmpty(), "versions", "เพิ่มอย่างน้อย 1 เวอร์ชัน");
        require(setup.versions().size() <= 100, "versions", "เพิ่มได้ไม่เกิน 100 เวอร์ชันต่อสินค้า");
        Set<Integer> activeSites = new HashSet<>();
        Set<Integer> noWebsiteIds = new HashSet<>();
        websites.getDataAll().forEach(site -> {
            activeSites.add(site.getId());
            if (WebsiteQuickAddService.normalizedName(site.getName()).equals(WebsiteQuickAddService.normalizedName(WebsiteQuickAddService.NO_WEBSITE))) noWebsiteIds.add(site.getId());
        });
        Set<Integer> keptVersions = new HashSet<>();
        Set<Integer> keptCovers = new HashSet<>();
        Set<String> versionNames = new HashSet<>();
        int offerCount = 0;
        for (int vi = 0; vi < setup.versions().size(); vi++) {
            VersionInput version = setup.versions().get(vi);
            String path = "versions." + vi;
            require(version != null, path, "ข้อมูลเวอร์ชันไม่ถูกต้อง");
            requireText(version.name(), path + ".name", "กรุณากรอกชื่อเวอร์ชัน หรือใช้ชื่อ Standard");
            require(versionNames.add(version.name().trim().toLowerCase(Locale.ROOT)), path + ".name", "ชื่อเวอร์ชันซ้ำกัน");
            if (version.id() != null) {
                require(versionById.containsKey(version.id()) && keptVersions.add(version.id()), path, "เวอร์ชันไม่ใช่ของสินค้านี้หรือถูกใช้ซ้ำ");
            }
            require(version.groups() != null && !version.groups().isEmpty(), path + ".groups", "เพิ่มกลุ่มราคาอย่างน้อย 1 กลุ่ม");
            Set<String> offers = new HashSet<>();
            for (int gi = 0; gi < version.groups().size(); gi++) {
                PriceGroup group = version.groups().get(gi);
                String groupPath = path + ".groups." + gi;
                require(group != null, groupPath, "ข้อมูลกลุ่มราคาไม่ถูกต้อง");
                requireMoney(group.price(), groupPath + ".price", false);
                requireMoney(group.deposit(), groupPath + ".deposit", true);
                require(group.deposit().compareTo(group.price()) <= 0, groupPath + ".deposit", "มัดจำต้องไม่เกินราคาเต็ม");
                require(group.websiteIds() != null && !group.websiteIds().isEmpty(), groupPath + ".websites", "เลือกอย่างน้อย 1 เว็บต่อกลุ่มราคา");
                require(group.websiteIds().size() == 1 || group.websiteIds().stream().noneMatch(noWebsiteIds::contains),
                        groupPath + ".websites", "ตัวเลือกไม่มีเว็บต้องใช้แยกจากเว็บอื่นในกลุ่มราคา");
                String coverName = coverName(group.name());
                require(coverName.length() <= 255, groupPath + ".name", "ชื่อปกยาวเกิน 255 ตัวอักษร");
                for (Integer websiteId : group.websiteIds()) {
                    require(websiteId != null && offers.add(websiteId + ":" + coverName.toLowerCase(Locale.ROOT)),
                            groupPath + ".websites", "เว็บและปกเดียวกันซ้ำในเวอร์ชันนี้");
                    Integer coverId = group.coverIds() == null ? null : group.coverIds().get(websiteId);
                    Cover existing = coverId == null ? null : coverById.get(coverId);
                    if (coverId != null) {
                        require(existing != null && Objects.equals(existing.getVersion(), version.id())
                                && Objects.equals(existing.getWebsite(), websiteId) && keptCovers.add(coverId),
                                groupPath, "รายการขายไม่ใช่ของเวอร์ชันหรือเว็บนี้ หรือถูกใช้ซ้ำ");
                    }
                    require(activeSites.contains(websiteId) || existing != null,
                            groupPath + ".websites", "เว็บที่เลือกไม่มีอยู่หรือหยุดใช้งานแล้ว");
                    offerCount++;
                }
            }
        }
        require(offerCount <= 1000, "versions", "รายการขายรวมต้องไม่เกิน 1,000 รายการ");

        // All validation completes before writes; any persistence failure rolls back the whole setup.
        product.setName(input.getName().trim());
        product.setType(input.getType());
        product.setArtist(input.getArtist());
        product.setProduct_status(input.getProduct_status());
        product.setEnd_date(endDate);
        product.setSend_date(sendDate);
        product.setPic(input.getPic());
        product.setDelete("A");
        products.save(product);
        List<Version> changedVersions = new ArrayList<>();
        List<Cover> changedCovers = new ArrayList<>();
        for (VersionInput versionInput : setup.versions()) {
            Version version = versionInput.id() == null ? new Version() : versionById.get(versionInput.id());
            version.setProduct(product.getId());
            version.setName(versionInput.name().trim());
            version.setDelete("A");
            versions.save(version);
            for (PriceGroup group : versionInput.groups()) {
                for (Integer site : group.websiteIds()) {
                    Integer coverId = group.coverIds() == null ? null : group.coverIds().get(site);
                    Cover cover = coverId == null ? new Cover() : coverById.get(coverId);
                    cover.setProduct(product.getId());
                    cover.setVersion(version.getId());
                    cover.setWebsite(site);
                    cover.setName(coverName(group.name()));
                    cover.setPrice_total(group.price().doubleValue());
                    cover.setPrice_pledge(group.deposit().doubleValue());
                    cover.setPrice_balance(group.price().subtract(group.deposit()).doubleValue());
                    cover.setDelete("A");
                    changedCovers.add(cover);
                }
            }
        }
        // Retire removed options without deleting IDs referenced by historical orders.
        for (Cover old : oldCovers) {
            if (!keptCovers.contains(old.getId())) {
                old.setDelete("D");
                changedCovers.add(old);
            }
        }
        for (Version old : oldVersions) {
            if (!keptVersions.contains(old.getId())) {
                old.setDelete("D");
                changedVersions.add(old);
            }
        }
        covers.saveAll(changedCovers);
        versions.saveAll(changedVersions);
        return new SaveResult(product.getId(), setup.versions().size(), offerCount);
    }

    private Product requireProduct(Integer id) {
        Product product = products.findById(id).orElseThrow(() -> new SetupValidationException("product", "ไม่พบสินค้า"));
        require("A".equals(product.getDelete()), "product", "สินค้านี้ถูกลบแล้ว");
        return product;
    }

    private static String coverName(String name) {
        return name == null || name.isBlank() ? "ทั่วไป" : name.trim();
    }

    private static Date parseDate(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Date.valueOf(LocalDate.parse(value));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new SetupValidationException(field, "วันที่ไม่ถูกต้อง");
        }
    }

    private static void requireMoney(BigDecimal value, String field, boolean allowZero) {
        require(value != null && (allowZero ? value.signum() >= 0 : value.signum() > 0)
                && value.compareTo(new BigDecimal("999999999.99")) <= 0
                && value.stripTrailingZeros().scale() <= 2, field,
                allowZero ? "กรอกมัดจำตั้งแต่ 0 และทศนิยมไม่เกิน 2 ตำแหน่ง" : "กรอกราคามากกว่า 0 และทศนิยมไม่เกิน 2 ตำแหน่ง");
    }

    private static void requireText(String value, String field, String message) {
        require(value != null && !value.isBlank() && value.trim().length() <= 255, field, message);
    }

    private static void require(boolean valid, String field, String message) {
        if (!valid) throw new SetupValidationException(field, message);
    }

    public static class SetupValidationException extends RuntimeException {
        private final String field;
        public SetupValidationException(String field, String message) {
            super(message);
            this.field = field;
        }
        public String field() { return field; }
    }
}
