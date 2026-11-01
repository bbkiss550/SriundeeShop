package com.sriundee.preorder.service;

import java.util.*;
import java.util.regex.Pattern;
import com.sriundee.preorder.dto.ProductImageDraft.PriceDraft;

/** Applies only to inferred cover names in the same version and website price group. */
final class ProductImageCoverNaming {
    private static final Pattern SET=Pattern.compile("(?iu)(?:^|\\s)SET\\b");
    static List<PriceDraft> apply(List<PriceDraft> groups,Set<PriceDraft> unnamed) {
        return groups.stream().map(group->{
            boolean paired=unnamed.contains(group) && groups.stream().anyMatch(other->SET.matcher(other.name()).find()
                && new HashSet<>(other.websites()).equals(new HashSet<>(group.websites())));
            return paired?new PriceDraft("สุ่ม",group.websites(),group.price(),group.deposit()):group;
        }).toList();
    }
    private ProductImageCoverNaming() {}
}
