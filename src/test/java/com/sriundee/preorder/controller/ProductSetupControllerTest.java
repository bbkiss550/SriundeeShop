package com.sriundee.preorder.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sriundee.preorder.dto.ProductSetupDto;
import com.sriundee.preorder.repository.WebsiteRepository;
import com.sriundee.preorder.service.ProductSetupService;
import com.sriundee.preorder.service.ProductSetupService.SetupValidationException;

@ExtendWith(MockitoExtension.class)
class ProductSetupControllerTest {
    @Mock ProductSetupService service;
    @Mock WebsiteRepository websites;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ProductSetupController(service, websites)).build();
    }

    @Test
    void bindsBrowserDecimalStringsAndExistingIdMaps() throws Exception {
        when(service.save(any())).thenReturn(new ProductSetupService.SaveResult(10, 1, 2));
        mvc.perform(post("/product/setup/save").contentType(MediaType.APPLICATION_JSON).content("""
                {"product":{"id":10,"name":"ALD1 Garage","type":1,"artist":1,"product_status":1},
                 "versions":[{"id":20,"name":"T-SHIRT VER.","groups":[
                   {"name":"ทั่วไป","websiteIds":[1,2],"price":"1350.50","deposit":"700",
                    "coverIds":{"1":30,"2":31}}]}]}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.offerCount").value(2));

        ArgumentCaptor<ProductSetupDto> request = ArgumentCaptor.forClass(ProductSetupDto.class);
        verify(service).save(request.capture());
        var group = request.getValue().versions().getFirst().groups().getFirst();
        assertThat(group.price()).isEqualByComparingTo(new BigDecimal("1350.50"));
        assertThat(group.coverIds().get(1)).isEqualTo(30);
    }

    @Test
    void returnsValidationFieldForInlineFeedback() throws Exception {
        when(service.save(any())).thenThrow(new SetupValidationException("versions.0.groups.0.deposit", "มัดจำต้องไม่เกินราคาเต็ม"));
        mvc.perform(post("/product/setup/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("versions.0.groups.0.deposit"));
    }

    @Test
    void returnsBadRequestForMalformedMoneyInsteadOfInternalServerError() throws Exception {
        mvc.perform(post("/product/setup/save").contentType(MediaType.APPLICATION_JSON).content("""
                {"product":{},"versions":[{"groups":[{"price":"not-a-number"}]}]}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }
}
