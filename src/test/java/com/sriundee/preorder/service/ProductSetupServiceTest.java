package com.sriundee.preorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.sriundee.preorder.service.ProductSetupService.SetupValidationException;

@ExtendWith(MockitoExtension.class)
class ProductSetupServiceTest {
    @Mock ProductRepository products;
    @Mock VersionRepository versions;
    @Mock CoverRepository covers;
    @Mock WebsiteRepository websites;
    @Mock TypeRepository types;
    @Mock ArtistRepository artists;
    @Mock ProductStatusRepository statuses;
    @InjectMocks ProductSetupService service;

    @BeforeEach
    void defaults() {
        lenient().when(types.existsById(1)).thenReturn(true);
        lenient().when(artists.existsById(1)).thenReturn(true);
        lenient().when(statuses.existsById(1)).thenReturn(true);
        lenient().when(websites.getDataAll()).thenReturn(List.of(site(1, "Ktown4u"), site(2, "YES24"),
                site(3, "Aladin"), site(4, "Makestar"), site(5, "Plus Chat"), site(6, "Mnet Plus Merch"), site(100, "ไม่มีเว็บ")));
        lenient().when(products.save(any())).thenAnswer(call -> {
            Product product = call.getArgument(0);
            if (product.getId() == null) product.setId(10);
            return product;
        });
        AtomicInteger nextVersionId = new AtomicInteger(20);
        lenient().when(versions.save(any())).thenAnswer(call -> {
            Version version = call.getArgument(0);
            if (version.getId() == null) version.setId(nextVersionId.getAndIncrement());
            return version;
        });
    }

    @Test
    void createsSixWebsiteOffersFromTwoPriceGroupsInTheAnnouncement() {
        var setup = new ProductSetupDto(productInput(null), List.of(new VersionInput(null, "T-SHIRT VER.", List.of(
                priceGroup(List.of(1, 2), "1350", "700", Map.of()),
                priceGroup(List.of(3, 4, 5, 6), "1450", "700", Map.of())))));

        var result = service.save(setup);

        assertThat(result).isEqualTo(new ProductSetupService.SaveResult(10, 1, 6));
        List<Cover> saved = savedCovers();
        assertThat(saved).hasSize(6);
        assertThat(saved).extracting(Cover::getWebsite).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(saved).extracting(Cover::getPrice_balance).containsExactly(650d, 650d, 750d, 750d, 750d, 750d);
        assertThat(saved).allSatisfy(cover -> {
            assertThat(cover.getProduct()).isEqualTo(10);
            assertThat(cover.getVersion()).isEqualTo(20);
            assertThat(cover.getName()).isEqualTo("ทั่วไป");
        });
    }

    @Test
    void calculatesDecimalBalancesOnServer() {
        service.save(new ProductSetupDto(productInput(null), List.of(new VersionInput(null, "DESK", List.of(
                priceGroup(List.of(1), "950.75", "500.10", Map.of()))))));
        assertThat(savedCovers().getFirst().getPrice_balance()).isEqualTo(450.65d);
    }

    @Test
    void savesDeskOfferWithDummyWebsiteAndCorrectBalance() {
        var result = service.save(new ProductSetupDto(productInput(null), List.of(new VersionInput(null, "DESK VER.", List.of(
                priceGroup(List.of(100), "950", "500", Map.of()))))));
        assertThat(result.offerCount()).isEqualTo(1);
        assertThat(savedCovers().getFirst().getWebsite()).isEqualTo(100);
        assertThat(savedCovers().getFirst().getPrice_balance()).isEqualTo(450d);
    }

    @Test
    void dummyWebsiteCannotBeCombinedWithRealWebsiteInSamePriceGroup() {
        assertThatThrownBy(() -> service.save(new ProductSetupDto(productInput(null), List.of(new VersionInput(null, "DESK", List.of(
                priceGroup(List.of(100, 1), "950", "500", Map.of())))))))
                .isInstanceOf(SetupValidationException.class).hasMessageContaining("ไม่มีเว็บต้องใช้แยก");
        verify(products, never()).save(any());
    }

    @Test
    void keepsWebsiteAndPriceGroupsUnderTheirOwnVersion() {
        service.save(new ProductSetupDto(productInput(null), List.of(
                new VersionInput(null, "T-SHIRT", List.of(priceGroup(List.of(1, 2), "1350", "700", Map.of()))),
                new VersionInput(null, "DESK", List.of(priceGroup(List.of(1), "950", "500", Map.of()))))));
        assertThat(savedCovers()).extracting(Cover::getVersion).containsExactly(20, 20, 21);
        assertThat(savedCovers()).extracting(Cover::getPrice_total).containsExactly(1350d, 1350d, 950d);
    }

    @Test
    void rejectsDepositLargerThanPriceBeforeWritingAnything() {
        assertThatThrownBy(() -> service.save(new ProductSetupDto(productInput(null), List.of(new VersionInput(null, "ทั่วไป", List.of(
                priceGroup(List.of(1), "950", "951", Map.of())))))))
                .isInstanceOf(SetupValidationException.class).hasMessageContaining("มัดจำต้องไม่เกิน");
        verify(products, never()).save(any());
        verify(versions, never()).save(any());
        verify(covers, never()).saveAll(any());
    }

    @Test
    void rejectsDuplicateWebsiteAndCoverAcrossPriceGroups() {
        assertThatThrownBy(() -> service.save(new ProductSetupDto(productInput(null), List.of(new VersionInput(null, "T-SHIRT", List.of(
                priceGroup(List.of(1), "1350", "700", Map.of()),
                priceGroup(List.of(1), "1450", "700", Map.of())))))))
                .isInstanceOf(SetupValidationException.class).hasMessageContaining("ซ้ำ");
        verify(products, never()).save(any());
    }

    @Test
    void allowsDifferentCoversAtTheSameWebsite() {
        var groupA = new PriceGroup("A", List.of(1), new BigDecimal("1350"), new BigDecimal("700"), Map.of());
        var groupB = new PriceGroup("B", List.of(1), new BigDecimal("1350"), new BigDecimal("700"), Map.of());
        var result = service.save(new ProductSetupDto(productInput(null), List.of(new VersionInput(null, "Standard", List.of(groupA, groupB)))));
        assertThat(result.offerCount()).isEqualTo(2);
        assertThat(savedCovers()).extracting(Cover::getName).containsExactly("A", "B");
    }

    @Test
    void preservesExistingCoverIdAndSoftDeletesRemovedOfferForHistoricalOrders() {
        Product existing = existingProduct();
        Version version = existingVersion(20, "T-SHIRT");
        Cover retained = existingCover(30, 20, 1);
        Cover removed = existingCover(31, 20, 2);
        existingData(existing, List.of(version), List.of(retained, removed));

        service.save(new ProductSetupDto(productInput(10), List.of(new VersionInput(20, "T-SHIRT", List.of(
                priceGroup(List.of(1), "1400", "700", Map.of(1, 30)))))));

        List<Cover> saved = savedCovers();
        assertThat(saved).containsExactly(retained, removed);
        assertThat(retained.getId()).isEqualTo(30);
        assertThat(retained.getDelete()).isEqualTo("A");
        assertThat(retained.getPrice_total()).isEqualTo(1400d);
        assertThat(removed.getDelete()).isEqualTo("D");
        assertThat(removed.getPrice_total()).isEqualTo(1350d);
        verify(covers, never()).deleteById(anyInt());
    }

    @Test
    void rejectsAnOfferIdFromAnotherProductBeforeChangingExistingData() {
        Product existing = existingProduct();
        existingData(existing, List.of(existingVersion(20, "T-SHIRT")), List.of(existingCover(30, 20, 1)));
        assertThatThrownBy(() -> service.save(new ProductSetupDto(productInput(10), List.of(new VersionInput(20, "T-SHIRT", List.of(
                priceGroup(List.of(1), "1350", "700", Map.of(1, 999))))))))
                .isInstanceOf(SetupValidationException.class).hasMessageContaining("รายการขายไม่ใช่");
        assertThat(existing.getName()).isEqualTo("Original");
        verify(products, never()).save(any());
    }

    @Test
    void rejectsMovingAnExistingOfferToAnotherWebsite() {
        existingData(existingProduct(), List.of(existingVersion(20, "T-SHIRT")), List.of(existingCover(30, 20, 1)));
        assertThatThrownBy(() -> service.save(new ProductSetupDto(productInput(10), List.of(new VersionInput(20, "T-SHIRT", List.of(
                priceGroup(List.of(2), "1350", "700", Map.of(2, 30))))))))
                .isInstanceOf(SetupValidationException.class);
        verify(products, never()).save(any());
    }

    @Test
    void rejectsMissingWebsiteAndInvalidMoneyPrecision() {
        for (PriceGroup group : List.of(priceGroup(List.of(), "950", "500", Map.of()),
                priceGroup(List.of(1), "950.001", "500", Map.of()), priceGroup(List.of(999), "950", "500", Map.of()))) {
            assertThatThrownBy(() -> service.save(new ProductSetupDto(productInput(null), List.of(new VersionInput(null, "ทั่วไป", List.of(group))))))
                    .isInstanceOf(SetupValidationException.class);
        }
        verify(products, never()).save(any());
    }

    @Test
    void rejectsInvalidCalendarDateBeforeSaving() {
        ProductDto product = productInput(null);
        product.setEnd_date("2026-02-30");
        assertThatThrownBy(() -> service.save(new ProductSetupDto(product, List.of(new VersionInput(null, "ทั่วไป", List.of(
                priceGroup(List.of(1), "950", "500", Map.of())))))))
                .isInstanceOf(SetupValidationException.class).hasMessageContaining("วันที่");
        verify(products, never()).save(any());
    }

    @Test
    void loadsExistingOffersAsPriceGroupsWithTheirOriginalIdsAndCalendarDates() {
        Product product = existingProduct();
        product.setEnd_date(Date.valueOf("2026-11-15"));
        existingData(product, List.of(existingVersion(20, "T-SHIRT")), List.of(existingCover(30, 20, 1), existingCover(31, 20, 2)));

        var loaded = service.load(10);

        assertThat(loaded.product().getEnd_date()).isEqualTo("2026-11-15");
        assertThat(loaded.versions()).hasSize(1);
        assertThat(loaded.versions().getFirst().groups()).hasSize(1);
        assertThat(loaded.versions().getFirst().groups().getFirst().websiteIds()).containsExactly(1, 2);
        assertThat(loaded.versions().getFirst().groups().getFirst().coverIds()).containsExactlyInAnyOrderEntriesOf(Map.of(1, 30, 2, 31));
    }

    private void existingData(Product product, List<Version> oldVersions, List<Cover> oldCovers) {
        when(products.findById(10)).thenReturn(Optional.of(product));
        when(versions.getDataByID_pro(10)).thenReturn(oldVersions);
        when(covers.findByProductAndDeleteOrderByIdAsc(10, "A")).thenReturn(oldCovers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Cover> savedCovers() {
        ArgumentCaptor<Iterable<Cover>> captor = ArgumentCaptor.forClass((Class) Iterable.class);
        verify(covers).saveAll(captor.capture());
        return (List<Cover>) captor.getValue();
    }

    private ProductDto productInput(Integer id) {
        ProductDto product = new ProductDto();
        product.setId(id);
        product.setName("ALD1 Garage");
        product.setType(1);
        product.setArtist(1);
        product.setProduct_status(1);
        return product;
    }

    private PriceGroup priceGroup(List<Integer> sites, String price, String deposit, Map<Integer, Integer> ids) {
        return new PriceGroup("ทั่วไป", sites, new BigDecimal(price), new BigDecimal(deposit), ids);
    }

    private Website site(int id, String name) {
        Website site = new Website();
        site.setId(id);
        site.setName(name);
        site.setDelete("A");
        return site;
    }

    private Product existingProduct() {
        Product product = new Product();
        product.setId(10);
        product.setName("Original");
        product.setDelete("A");
        return product;
    }

    private Version existingVersion(int id, String name) {
        Version version = new Version();
        version.setId(id);
        version.setProduct(10);
        version.setName(name);
        version.setDelete("A");
        return version;
    }

    private Cover existingCover(int id, int versionId, int siteId) {
        Cover cover = new Cover();
        cover.setId(id);
        cover.setProduct(10);
        cover.setVersion(versionId);
        cover.setWebsite(siteId);
        cover.setName("ทั่วไป");
        cover.setPrice_total(1350);
        cover.setPrice_pledge(700);
        cover.setPrice_balance(650);
        cover.setDelete("A");
        return cover;
    }
}
