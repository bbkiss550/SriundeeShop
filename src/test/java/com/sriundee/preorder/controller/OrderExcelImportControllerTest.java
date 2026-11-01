package com.sriundee.preorder.controller;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sriundee.preorder.service.OrderExcelImportService;
import com.sriundee.preorder.service.OrderExcelImportService.Report;
import com.sriundee.preorder.service.OrderExcelReader.Issue;

class OrderExcelImportControllerTest {
    final OrderExcelImportService service=mock(OrderExcelImportService.class);
    final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new OrderExcelImportController(service)).build();
    final Report good=new Report(true,1,0,List.of(),List.of());
    final Report bad=new Report(false,2,2,List.of(new Issue("Orders",3,"Bad","ไม่พบสินค้า"),new Issue("Orders",9,"Bad","ไม่พบปก")),List.of());
    MockMultipartFile file(){return new MockMultipartFile("file","orders.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",new byte[]{1,2,3});}
    String preview(MockHttpSession session) throws Exception {
        when(service.validate(any())).thenReturn(good);
        String json=mvc.perform(multipart("/orders/import/validate").file(file()).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(json).get("token").asText();
    }
    @Test void rejectedFileIncludesAllRowsAndHasNoConfirmationToken() throws Exception {
        when(service.validate(any())).thenReturn(bad);
        mvc.perform(multipart("/orders/import/validate").file(file())).andExpect(status().isOk()).andExpect(jsonPath("$.report.issues.length()").value(2)).andExpect(jsonPath("$.token").value(""));
        verify(service,never()).commit(any(),anyString(),anyInt());
    }
    @Test void confirmationBelongsToSessionAndCanBeUsedOnlyOnce() throws Exception {
        var session=new MockHttpSession();session.setAttribute(LoginController.SESSION_USER_ID,7);String token=preview(session);
        when(service.commit(any(),anyString(),eq(7))).thenReturn(Map.of("orderCount",1));
        mvc.perform(post("/orders/import/confirm").session(new MockHttpSession()).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/orders/import/confirm").session(session).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isOk());
        mvc.perform(post("/orders/import/confirm").session(session).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isBadRequest());
        verify(service,times(1)).commit(any(),anyString(),eq(7));
    }
    @Test void uploadingBadFileInvalidatesEarlierValidPreview() throws Exception {
        var session=new MockHttpSession();String token=preview(session);when(service.validate(any())).thenReturn(bad);
        mvc.perform(multipart("/orders/import/validate").file(file()).session(session)).andExpect(status().isOk());
        mvc.perform(post("/orders/import/confirm").session(session).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isBadRequest());
    }
    @Test void changedCatalogOnConfirmationReturnsUpdatedReport() throws Exception {
        var session=new MockHttpSession();session.setAttribute(LoginController.SESSION_USER_ID,7);String token=preview(session);
        when(service.commit(any(),anyString(),eq(7))).thenThrow(new OrderExcelImportService.Rejected(bad));
        mvc.perform(post("/orders/import/confirm").session(session).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.report.issues.length()").value(2));
    }
    @Test void templateIsARealExcelWorkbookWithRequiredHeaders() throws Exception {
        byte[] bytes=mvc.perform(get("/orders/import/template")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))){assertThat(book.getSheetAt(0).getRow(0).getCell(7).getStringCellValue()).isEqualTo("ราคาเต็มรวม");}
    }
    @Test void inspectedMappingsMustBeApprovedAndAreRetainedUntilCommit() throws Exception {
        var mappingService=new com.sriundee.preorder.service.OrderExcelMappingService(new com.sriundee.preorder.service.OrderExcelReader(),mock(com.sriundee.preorder.service.AiApiSettingsService.class),new ObjectMapper());
        var mappedMvc=MockMvcBuilders.standaloneSetup(new OrderExcelImportController(service,mappingService)).build();var session=new MockHttpSession();session.setAttribute(LoginController.SESSION_USER_ID,7);
        byte[] bytes;
        try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var output=new java.io.ByteArrayOutputStream()) {
            var sheet=book.createSheet("Orders");sheet.createRow(0).createCell(0).setCellValue("ชื่อลูกค้า");sheet.getRow(0).createCell(1).setCellValue("สินค้า");sheet.createRow(1).createCell(0).setCellValue("@customer");sheet.getRow(1).createCell(1).setCellValue("Album");book.write(output);bytes=output.toByteArray();
        }
        String inspection=mappedMvc.perform(multipart("/orders/import/inspect").file(new MockMultipartFile("file","orders.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes)).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String mappingToken=new ObjectMapper().readTree(inspection).get("mappingToken").asText();verifyNoInteractions(service);
        String invalid="{\"token\":\""+mappingToken+"\",\"mappings\":[{\"sheetIndex\":0,\"headerRow\":1,\"columns\":{\"0\":\"customer\",\"1\":\"customer\"}}]}";
        mappedMvc.perform(post("/orders/import/validate-mapping").session(session).contentType("application/json").content(invalid)).andExpect(status().isBadRequest());verifyNoInteractions(service);
        when(service.validate(any(),anyList())).thenReturn(good);
        String approval="{\"token\":\""+mappingToken+"\",\"mappings\":[{\"sheetIndex\":0,\"headerRow\":1,\"columns\":{\"0\":\"customer\",\"1\":\"product\"}}]}";
        String reviewed=mappedMvc.perform(post("/orders/import/validate-mapping").session(session).contentType("application/json").content(approval)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token=new ObjectMapper().readTree(reviewed).get("token").asText();
        when(service.commit(any(),anyString(),eq(7),anyList())).thenReturn(Map.of("orderCount",1));
        mappedMvc.perform(post("/orders/import/confirm").session(session).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isOk());
        verify(service).commit(any(),anyString(),eq(7),argThat(mappings->mappings.size()==1 && mappings.getFirst().columns().get(1).equals("product")));
    }
}
