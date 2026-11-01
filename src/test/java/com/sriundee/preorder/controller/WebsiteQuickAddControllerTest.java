package com.sriundee.preorder.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sriundee.preorder.entity.Website;
import com.sriundee.preorder.service.WebsiteQuickAddService;
import com.sriundee.preorder.service.WebsiteQuickAddService.WebsiteAddException;

@ExtendWith(MockitoExtension.class)
class WebsiteQuickAddControllerTest {
    @Mock WebsiteQuickAddService service;
    MockMvc mvc;
    @BeforeEach
    void setup() { mvc = MockMvcBuilders.standaloneSetup(new WebsiteQuickAddController(service)).build(); }

    @Test
    void returnsTheCreatedWebsiteForImmediateSelection() throws Exception {
        Website website = new Website();
        website.setId(7); website.setName("FNC STORE"); website.setDelete("A");
        when(service.add("FNC STORE")).thenReturn(website);
        mvc.perform(post("/product/setup/websites").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"FNC STORE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("FNC STORE")).andExpect(jsonPath("$.delete").value("A"));
    }

    @Test
    void returnsAValidationErrorTheDialogCanDisplayWithoutLosingTheProductDraft() throws Exception {
        when(service.add("")).thenThrow(new WebsiteAddException(400, "กรุณากรอกชื่อเว็บ"));
        mvc.perform(post("/product/setup/websites").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("กรุณากรอกชื่อเว็บ"));
    }
}
