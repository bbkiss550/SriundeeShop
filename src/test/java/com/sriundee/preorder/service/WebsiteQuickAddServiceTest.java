package com.sriundee.preorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sriundee.preorder.entity.Website;
import com.sriundee.preorder.repository.WebsiteRepository;
import com.sriundee.preorder.service.WebsiteQuickAddService.WebsiteAddException;

@ExtendWith(MockitoExtension.class)
class WebsiteQuickAddServiceTest {
    @Mock WebsiteRepository websites;
    @InjectMocks WebsiteQuickAddService service;

    @Test
    void addsAnActiveWebsiteAndReturnsItsIdWithoutSavingAProduct() {
        when(websites.lockWebsiteCreation()).thenReturn(true);
        when(websites.getDataAll()).thenReturn(List.of());
        when(websites.save(any())).thenAnswer(call -> {
            Website result = call.getArgument(0); result.setId(12); return result;
        });
        Website result = service.add("  FNC   STORE  ");
        assertThat(result.getId()).isEqualTo(12);
        assertThat(result.getName()).isEqualTo("FNC STORE");
        assertThat(result.getDelete()).isEqualTo("A");
    }

    @Test
    void reusesAnExistingWebsiteWhenCaseSpacesOrPunctuationDiffer() {
        Website existing = new Website();
        existing.setId(7); existing.setName("FNC STORE"); existing.setDelete("A");
        when(websites.lockWebsiteCreation()).thenReturn(true);
        when(websites.getDataAll()).thenReturn(List.of(existing));
        assertThat(service.add("fnc-store")).isSameAs(existing);
        verify(websites, never()).save(any());
    }

    @Test
    void rejectsBlankOversizedAndMarkupNamesBeforeTouchingTheDatabase() {
        for (String input : List.of("", "   ", "- / -", "a".repeat(256), "<script>alert(1)</script>")) {
            assertThatThrownBy(() -> service.add(input)).isInstanceOf(WebsiteAddException.class);
        }
        verify(websites, never()).lockWebsiteCreation();
        verify(websites, never()).save(any());
    }

    @Test
    void concurrentAddReportsRetryRatherThanInsertingDuplicates() {
        when(websites.lockWebsiteCreation()).thenReturn(false);
        assertThatThrownBy(() -> service.add("FNC STORE")).isInstanceOfSatisfying(WebsiteAddException.class,
                error -> assertThat(error.status()).isEqualTo(409));
        verify(websites, never()).save(any());
    }

    @Test
    void normalizesFullWidthNamesAndPreservesNonLatinNames() {
        assertThat(WebsiteQuickAddService.normalizedName("ＦＮＣ Store")).isEqualTo("fncstore");
        assertThat(WebsiteQuickAddService.normalizedName("ร้านค้า ไทย")).isEqualTo("ร้านค้าไทย");
        assertThat(WebsiteQuickAddService.normalizedName("音楽 店")).isEqualTo("音楽店");
    }
}
