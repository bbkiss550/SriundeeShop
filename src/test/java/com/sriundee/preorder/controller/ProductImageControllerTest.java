package com.sriundee.preorder.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sriundee.preorder.service.LocalProductImageReader;
import com.sriundee.preorder.service.LocalProductImageReader.ImageReadException;
import com.sriundee.preorder.service.ProductImageTextParser;

@ExtendWith(MockitoExtension.class)
class ProductImageControllerTest {
    @Test
    void cancellationIsScopedToSessionAndPreventsLateStart() throws Exception {
        String id = "00000000-0000-0000-0000-000000000001";
        MockHttpSession owner = new MockHttpSession();
        mvc.perform(post("/product/setup/image/cancel/" + id).session(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cancelled").value(true)).andExpect(jsonPath("$.done").value(true));
        mvc.perform(get("/product/setup/image/progress/" + id).session(owner))
                .andExpect(jsonPath("$.cancelled").value(true));
        mvc.perform(get("/product/setup/image/progress/" + id).session(new MockHttpSession()))
                .andExpect(jsonPath("$.cancelled").value(false)).andExpect(jsonPath("$.percent").value(0));
    }

    @Test
    void progressTracksTheSameJobPassedToReader() throws Exception {
        var draft = new ProductImageTextParser().parse("YES24\nกล่องละ950 (มัดจำ500)");
        when(reader.read(any(), any())).thenAnswer(call -> {
            var progress = (LocalProductImageReader.ReadProgress) call.getArgument(1);
            progress.begin(); progress.update(100, "เสร็จแล้ว"); return draft;
        });
        String id = "00000000-0000-0000-0000-000000000002";
        MockHttpSession session = new MockHttpSession();
        mvc.perform(multipart("/product/setup/image/read").file(new MockMultipartFile("image", new byte[] {1}))
                .header("X-OCR-Request-Id", id).session(session)).andExpect(status().isOk());
        mvc.perform(get("/product/setup/image/progress/" + id).session(session))
                .andExpect(jsonPath("$.percent").value(100)).andExpect(jsonPath("$.done").value(true));
    }
    @Mock LocalProductImageReader reader;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ProductImageController(reader, new ProductImageTextParser())).build();
    }

    @Test
    void uploadReturnsOnlyADraftWithoutSavingProductData() throws Exception {
        when(reader.read(any())).thenReturn(new ProductImageTextParser().parse("YES24\nDESK VER.\nกล่องละ950 (มัดจำ500)"));
        mvc.perform(multipart("/product/setup/image/read").file(new MockMultipartFile("image", "sample.png", "image/png", new byte[] {1})))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versions[0].groups[0].price").value(950));
    }

    @Test
    void correctedOcrTextCanBeParsedWithoutReadingAnotherImage() throws Exception {
        mvc.perform(post("/product/setup/image/parse").contentType(MediaType.APPLICATION_JSON).content("""
                {"text":"YES24\\nDESK VER.\\nกล่องละ 950 (มัดจำ500)"}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versions[0].name").value("DESK VER."));
    }

    @Test
    void missingLocalModelsAreReportedWithoutExternalFallback() throws Exception {
        when(reader.read(any())).thenThrow(new ImageReadException(503, "ยังไม่พร้อมอ่านภาพในเครื่อง"));
        mvc.perform(multipart("/product/setup/image/read").file(new MockMultipartFile("image", new byte[] {1})))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("ยังไม่พร้อมอ่านภาพในเครื่อง"));
    }

    @Test
    void correctedTextWithoutVersionHeadingReturnsUsablePriceGroups() throws Exception {
        mvc.perform(post("/product/setup/image/parse").contentType(MediaType.APPLICATION_JSON).content("""
                {"text":"YES24 / FNC STORE\\nPlus Chat / Mnet Plus Merch\\nกล่องละ 1,390 (มัดจำ700)\\nเว็บ Ktown4u ไม่มี POB\\nกล่องละ 1,290 (มัดจำ600)"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions[0].name").value("Standard"))
                .andExpect(jsonPath("$.versions[0].groups.length()").value(2))
                .andExpect(jsonPath("$.versions[0].groups[0].websites[1]").value("FNC STORE"))
                .andExpect(jsonPath("$.versions[0].groups[1].price").value(1290));
    }
}
