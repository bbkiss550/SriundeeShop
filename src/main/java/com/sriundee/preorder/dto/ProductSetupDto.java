package com.sriundee.preorder.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** One submission contains the product and every sellable website/version/cover. */
public record ProductSetupDto(ProductDto product, List<VersionInput> versions) {
    public record VersionInput(Integer id, String name, List<PriceGroup> groups) {}

    public record PriceGroup(String name, List<Integer> websiteIds, BigDecimal price,
            BigDecimal deposit, Map<Integer, Integer> coverIds) {}
}
