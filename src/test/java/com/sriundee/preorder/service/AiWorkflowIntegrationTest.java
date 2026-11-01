package com.sriundee.preorder.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import com.sriundee.preorder.dto.ProductImageDraft;
import com.sriundee.preorder.dto.ProductImageDraft.*;
import com.sriundee.preorder.service.AiApiSettingsService.*;
import com.sriundee.preorder.service.LocalProductImageReader.*;
import com.sriundee.preorder.service.OrderExcelReader.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

class AiWorkflowIntegrationTest {
    @TempDir Path directory;
    final ObjectMapper mapper=new ObjectMapper();
    HttpServer server;AiApiSettingsService ai;String base;
    final AtomicInteger posts=new AtomicInteger(),status=new AtomicInteger(200),delay=new AtomicInteger();
    final AtomicReference<String> output=new AtomicReference<>();final AtomicReference<JsonNode> sent=new AtomicReference<>();
    final CountDownLatch received=new CountDownLatch(1);
    final LocalProductImageReader local=mock(LocalProductImageReader.class);
    final ProductImageDraft fallback=new ProductImageDraft(null,List.of(new VersionDraft("Standard",List.of(new PriceDraft("เดี่ยว",List.of("ไม่มีเว็บ"),new java.math.BigDecimal("590"),new java.math.BigDecimal("300"))))),List.of(),"OCR 590 300");
    static final String IMAGE="""
        {"productName":null,"rawText":"Weverse Shop PHOTOBOOK VER. เดี่ยว 590 มัดจำ300 SET 1750 มัดจำ900 ENGENE VER. เดี่ยว400 มัดจำ200 DAY SET2390 มัดจำ1200 NIGHT SET2390 มัดจำ1200 WEVERSE ALBUMS VER. เดี่ยว300 มัดจำ150","versions":[{"name":"PHOTOBOOK VER.","groups":[{"name":"เดี่ยว","websites":["Weverse Shop"],"price":590,"deposit":300},{"name":"SET","websites":["Weverse Shop"],"price":1750,"deposit":900}]},{"name":"ENGENE VER.","groups":[{"name":"เดี่ยว","websites":["Weverse Shop"],"price":400,"deposit":200},{"name":"DAY SET","websites":["Weverse Shop"],"price":2390,"deposit":1200},{"name":"NIGHT SET","websites":["Weverse Shop"],"price":2390,"deposit":1200}]},{"name":"WEVERSE ALBUMS VER.","groups":[{"name":"เดี่ยว","websites":["Weverse Shop"],"price":300,"deposit":150}]}]}
        """;
    @BeforeEach void start() throws Exception {
        ai=new AiApiSettingsService(mapper,directory.toString());server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1beta/models",exchange->{
            String text;
            if(exchange.getRequestMethod().equals("GET"))text="{\"models\":[{\"name\":\"models/selected-model\",\"supportedGenerationMethods\":[\"generateContent\"]}]}";
            else {
                posts.incrementAndGet();sent.set(mapper.readTree(exchange.getRequestBody().readAllBytes()));received.countDown();
                assertThat(exchange.getRequestURI().getPath()).isEqualTo("/v1beta/models/selected-model:generateContent");
                assertThat(exchange.getRequestHeaders().getFirst("x-goog-api-key")).isEqualTo("dummy-key");
                if(delay.get()>0)try{Thread.sleep(delay.get());}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
                text=mapper.createObjectNode().set("candidates",mapper.createArrayNode().add(mapper.createObjectNode().put("finishReason","STOP").set("content",mapper.createObjectNode().set("parts",mapper.createArrayNode().add(mapper.createObjectNode().put("text",output.get()==null?IMAGE:output.get())))))).toString();
            }
            byte[] bytes=text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try{exchange.sendResponseHeaders(exchange.getRequestMethod().equals("GET")?200:status.get(),bytes.length);exchange.getResponseBody().write(bytes);}finally{exchange.close();}
        });server.start();base="http://127.0.0.1:"+server.getAddress().getPort()+"/v1beta";
        when(local.normalizedImage(any())).thenReturn(new byte[]{1,2,3});when(local.read(any(),any(ReadProgress.class))).thenReturn(fallback);
    }
    @AfterEach void stop(){server.stop(0);}
    Draft draft(boolean enabled){return new Draft("GEMINI",base,"selected-model","dummy-key",false,false,3,enabled);}
    void enable(){ai.test(1,draft(false));ai.save(1,draft(true));}
    MockMultipartFile image(){return new MockMultipartFile("image","selected-crop.png","image/png",new byte[]{1,2,3});}
    ProductImageAiReader pipeline(){return new ProductImageAiReader(ai,local,mapper);}
    @Test void enabledAiUsesConfiguredModelStructuredPricesAndOnlyTheSuppliedCrop() throws Exception {
        enable();var result=pipeline().read(1,image(),new ReadProgress());
        assertThat(result.versions().getFirst().groups()).extracting(group->group.price().intValue()).containsExactly(590,1750);
        assertThat(result.versions().getFirst().groups().getFirst().deposit()).isEqualByComparingTo("300");
        assertThat(sent.get().at("/contents/0/parts/1/inlineData/data").asText()).isEqualTo("AQID");
        assertThat(sent.get().at("/generationConfig/responseJsonSchema/type").asText()).isEqualTo("object");
        assertThat(result.readSource()).isEqualTo("AI");
        verify(local,never()).read(any(),any(ReadProgress.class));assertThat(posts.get()).isEqualTo(1);
        assertThat(result.versions().stream().flatMap(version->version.groups().stream()).count()).isEqualTo(6);
        Path output=Path.of(".runtime/product-setup-qa/ai-image-draft.json");Files.createDirectories(output.getParent());mapper.writeValue(output.toFile(),result);
    }
    @Test void disabledAiNeverCallsGenerationAndKeepsOcr(){
        ai.save(1,draft(false));var result=pipeline().read(1,image(),new ReadProgress());assertThat(result.rawText()).isEqualTo(fallback.rawText());assertThat(result.readSource()).isEqualTo("OCR");assertThat(result.readMessage()).contains("ยังไม่ได้เปิด");assertThat(posts.get()).isZero();
    }
    @Test void enablingRequiresSuccessfulTestAndVerificationSurvivesRestart(){
        assertThatThrownBy(()->ai.save(1,draft(true))).hasMessageContaining("ทดสอบ");enable();
        var restarted=new AiApiSettingsService(mapper,directory.toString());assertThat(restarted.get(1).enabled()).isTrue();assertThat(restarted.get(1).tested()).isTrue();
        assertThat(new ProductImageAiReader(restarted,local,mapper).read(1,image(),new ReadProgress()).versions()).isNotEmpty();
    }
    @Test void quotaFallsBackAndDoesNotRetryOrSwitchToAnotherModel(){
        enable();status.set(429);var result=pipeline().read(1,image(),new ReadProgress());
        assertThat(result.warnings()).anySatisfy(text->assertThat(text).contains("OCR").contains("โควตา"));
        assertThat(result.readSource()).isEqualTo("OCR");assertThat(result.readMessage()).contains("โควตา");
        pipeline().read(1,image(),new ReadProgress());assertThat(posts.get()).isEqualTo(1);verify(local,times(2)).read(any(),any(ReadProgress.class));
    }
    @Test void invalidStructuredOutputFallsBackWithoutFillingWrongPrices(){
        enable();output.set("{\"versions\":[]}");var result=pipeline().read(1,image(),new ReadProgress());assertThat(result.rawText()).isEqualTo("OCR 590 300");assertThat(posts.get()).isEqualTo(1);
    }
    @Test void aiCoverDirectivesRepairWrongVersionHierarchyWithoutDuplicatingSets() throws Exception {
        enable();
        String raw="Ktown4u\nระบุปก Fallen Angel / Alt ver.\nบั้มละ 380 บาท (มัดจำ150)\nSET(2)750 บาท (มัดจำ300)\nWithmuu / Jump Up\nระบุปก Fallen Angel / Alt ver.\nบั้มละ450 บาท (มัดจำ200)\nEverline / Makestar\nสุ่มปก Fallen Angel / Alt ver.\nบั้มละ450 บาท (มัดจำ200)";
        var json=mapper.createObjectNode().putNull("productName").put("rawText",raw);
        var versions=json.putArray("versions");
        for(String name:List.of("Fallen Angel","Alt")){
            var groups=versions.addObject().put("name",name).putArray("groups");
            groups.addObject().put("name","ระบุปก").put("price",380).put("deposit",150).putArray("websites").add("Ktown4u");
            groups.addObject().put("name","SET(2)").put("price",750).put("deposit",300).putArray("websites").add("Ktown4u");
            groups.addObject().put("name","ระบุปก").put("price",450).put("deposit",200).putArray("websites").add("Withmuu").add("Jump Up");
        }
        versions.addObject().put("name","Fallen Angel / Alt").putArray("groups").addObject().put("name","สุ่ม").put("price",450).put("deposit",200).putArray("websites").add("Everline").add("Makestar");
        output.set(json.toString());var result=pipeline().read(1,image(),new ReadProgress());
        assertThat(result.readSource()).isEqualTo("AI");
        assertThat(result.versions()).hasSize(1);assertThat(result.versions().getFirst().name()).isEqualTo("Standard");
        assertThat(result.versions().getFirst().groups()).extracting(offer->offer.name()).containsExactly("Fallen Angel","Alt","SET(2)","Fallen Angel","Alt","สุ่ม (Fallen Angel / Alt)");
        verify(local,never()).read(any(),any(ReadProgress.class));
        // If AI/source money differs, preserve the validated AI values rather than silently replace them.
        ((com.fasterxml.jackson.databind.node.ObjectNode)versions.get(0).path("groups").get(0)).put("price",390);
        output.set(json.toString());var differing=pipeline().read(1,image(),new ReadProgress());
        assertThat(differing.versions().getFirst().groups().getFirst().price()).isEqualByComparingTo("390");
    }
    @Test void aiAbsentDepositsDefaultToZeroAndStatedUnreadableDepositsRemainUnknown() throws Exception {
        enable();var json=mapper.readTree(IMAGE);
        var groups=json.path("versions").get(0).path("groups");
        ((com.fasterxml.jackson.databind.node.ObjectNode)groups.get(0)).putNull("deposit").put("depositSpecified",false);
        ((com.fasterxml.jackson.databind.node.ObjectNode)groups.get(1)).putNull("deposit").put("depositSpecified",true);
        output.set(json.toString());var result=pipeline().read(1,image(),new ReadProgress());
        assertThat(result.readSource()).isEqualTo("AI");
        assertThat(result.versions().getFirst().groups().get(0).deposit()).isEqualByComparingTo("0");
        assertThat(result.versions().getFirst().groups().get(1).deposit()).isNull();
        ((com.fasterxml.jackson.databind.node.ObjectNode)groups.get(1)).put("deposit",0);
        output.set(json.toString());assertThat(pipeline().read(1,image(),new ReadProgress()).versions().getFirst().groups().get(1).deposit()).isEqualByComparingTo("0");
    }
    @Test void aiCannotMergeIndependentShopPricesIntoOneWebsiteGroup() throws Exception {
        enable();
        var expected=new ProductImageTextParser().parse("Applemusic\nStandard ver.\nบั้มละ490 (มัดจำ250)\nSET(2)950 (มัดจำ500)\nFANS SHOP / YES24 / Musicplant\nStandard ver.\nบั้มละ550 (มัดจำ250)\nSET(2)1050 (มัดจำ500)");
        var json=mapper.valueToTree(expected);
        output.set(json.toString());assertThat(pipeline().read(1,image(),new ReadProgress()).readSource()).isEqualTo("AI");
        for(JsonNode group:json.path("versions").get(0).path("groups")){
            ((com.fasterxml.jackson.databind.node.ObjectNode)group).putArray("websites").add("Applemusic").add("FANS SHOP").add("YES24").add("Musicplant");
        }
        when(local.read(any(),any(ReadProgress.class))).thenReturn(expected);
        output.set(json.toString());var result=pipeline().read(1,image(),new ReadProgress());
        assertThat(result.readSource()).isEqualTo("OCR");
        assertThat(result.versions().getFirst().groups().getFirst().websites()).containsExactly("Applemusic");
        assertThat(result.versions().getFirst().groups().get(2).websites()).containsExactlyInAnyOrder("FANS SHOP","YES24","Musicplant");
        assertThat(result.readMessage()).contains("ราคาต่างกัน");assertThat(posts.get()).isEqualTo(2);
    }
    @Test void aiNoBenefitSectionCannotInheritTheEarlierShopHeader() throws Exception {
        enable();
        var expected=new ProductImageTextParser().parse("Everline / KPOP TOGETHER / Whosfan Store\nPulse ver.\nบั้มละ650 (มัดจำ350)\nTenuto ver.\n2 บั้ม ราคา780 (มัดจำ400)\nไม่มีการ์ดเบเน\nPulse ver.\nบั้มละ550 (มัดจำ300)\nTenuto ver.\nบั้มละ350 (มัดจำ200)");
        var json=mapper.valueToTree(expected);
        output.set(json.toString());var correct=pipeline().read(1,image(),new ReadProgress());
        assertThat(correct.readSource()).isEqualTo("AI");
        assertThat(correct.versions().getFirst().groups().getLast().websites()).containsExactly("ไม่มีเว็บ");
        for(JsonNode version:json.path("versions"))for(JsonNode group:version.path("groups")){
            ((com.fasterxml.jackson.databind.node.ObjectNode)group).putArray("websites").add("Everline").add("KPOP TOGETHER").add("Whosfan Store");
        }
        when(local.read(any(),any(ReadProgress.class))).thenReturn(expected);
        output.set(json.toString());var fallback=pipeline().read(1,image(),new ReadProgress());
        assertThat(fallback.readSource()).isEqualTo("OCR");assertThat(fallback.readMessage()).contains("ไม่มีการ์ดเบเน");
        assertThat(fallback.versions().getFirst().groups().getLast().websites()).containsExactly("ไม่มีเว็บ");
    }
    @Test void aiInferredSinglesBecomeRandomOnlyWithPairedSetAndExplicitNamesRemain() throws Exception {
        enable();var response=mapper.readTree(IMAGE);
        ((com.fasterxml.jackson.databind.node.ObjectNode)response).put("rawText",response.path("rawText").asText().replace("เดี่ยว","บั้มละ"));
        for(JsonNode version:response.path("versions"))for(JsonNode group:version.path("groups"))((com.fasterxml.jackson.databind.node.ObjectNode)group).put("nameExplicit",false);
        output.set(response.toString());var inferred=pipeline().read(1,image(),new ReadProgress());
        assertThat(inferred.versions().get(0).groups().getFirst().name()).isEqualTo("สุ่ม");assertThat(inferred.versions().get(1).groups().getFirst().name()).isEqualTo("สุ่ม");assertThat(inferred.versions().get(2).groups().getFirst().name()).isEqualTo("เดี่ยว");
        ((com.fasterxml.jackson.databind.node.ObjectNode)response.path("versions").get(0).path("groups").get(0)).put("nameExplicit",true);
        output.set(response.toString());assertThat(pipeline().read(1,image(),new ReadProgress()).versions().getFirst().groups().getFirst().name()).isEqualTo("เดี่ยว");
    }
    @Test void aiNoShopGroupsUseOnlyAnUnambiguousSharedMultiShopHeading() throws Exception {
        enable();var json=mapper.readTree(IMAGE);((com.fasterxml.jackson.databind.node.ObjectNode)json).put("rawText","Applemusic / Ktown4u / Soundwave\nPHOTOBOOK VER.\nเดี่ยว590 (มัดจำ300)\nSET1750 (มัดจำ900)\nENGENE VER.\nเดี่ยว400 (มัดจำ200)\nWEVERSE ALBUMS VER.\nเดี่ยว300 (มัดจำ150)");
        for(JsonNode version:json.path("versions"))for(JsonNode group:version.path("groups"))((com.fasterxml.jackson.databind.node.ObjectNode)group).putArray("websites").add("ไม่มีเว็บ");
        output.set(json.toString());var result=pipeline().read(1,image(),new ReadProgress());
        assertThat(result.versions().stream().flatMap(version->version.groups().stream()).toList()).allSatisfy(offer->assertThat(offer.websites()).containsExactlyInAnyOrder("Applemusic","Ktown4u","Soundwave"));assertThat(result.readSource()).isEqualTo("AI");
    }
    @Test void timeoutFallsBackWithoutAnotherGenerationCall(){
        enable();delay.set(4000);long start=System.nanoTime();var result=pipeline().read(1,image(),new ReadProgress());
        assertThat(result.rawText()).isEqualTo(fallback.rawText());assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-start)).isLessThan(6);assertThat(posts.get()).isEqualTo(1);
    }
    @Test void cancellationStopsAiAndMustNotStartOcr() throws Exception {
        enable();delay.set(4000);var progress=new ReadProgress();var executor=Executors.newSingleThreadExecutor();
        try{var task=executor.submit(()->pipeline().read(1,image(),progress));assertThat(received.await(2,TimeUnit.SECONDS)).isTrue();progress.cancel();
            assertThatThrownBy(()->task.get(2,TimeUnit.SECONDS)).hasCauseInstanceOf(ImageReadException.class);verify(local,never()).read(any(),any(ReadProgress.class));
        }finally{executor.shutdownNow();}
    }
    byte[] spreadsheet() throws Exception {
        try(var book=new XSSFWorkbook();var bytes=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("Private customer sheet");String[] headers={"Buyer handle","Contact channel","Item title","Outlet","Variant label","Cover label","Pieces","Line total","Received amount","Outstanding amount","Order day","Payment day","Shipping state","Notes"};
            var header=sheet.createRow(0);for(int i=0;i<headers.length;i++)header.createCell(i).setCellValue(headers[i]);
            String[] values={"@private_person","X","Album","Weverse Shop","PHOTOBOOK VER.","เดี่ยว","1","590","300","290","29/10/69","30/10/69","รอเว็บส่งของ","private@example.com 0812345678"};
            var row=sheet.createRow(1);for(int i=0;i<values.length;i++)row.createCell(i).setCellValue(values[i]);book.write(bytes);return bytes.toByteArray();
        }
    }
    String mappingJson(){
        var root=mapper.createObjectNode();var columns=root.putArray("sheets").addObject().put("sheetIndex",0).putArray("columns");
        String[] keys={"customer","channel","product","website","version","cover","qty","total","paid","balance","date","paidDate","status","remark"};
        for(int i=0;i<keys.length;i++)columns.addObject().put("column",i).put("field",keys[i]);root.putArray("suggestions").add("ใช้วันที่ วว/ดด/ปปปป");return root.toString();
    }
    @Test void excelSendsOnlyHeadersAndSyntheticShapesAndApprovedMappingReadsOriginalRows() throws Exception {
        enable();output.set(mappingJson());var reader=new OrderExcelReader();var mapping=new OrderExcelMappingService(reader,ai,mapper);byte[] bytes=spreadsheet();var plan=mapping.inspect(1,bytes);
        assertThat(plan.source()).isEqualTo("AI");String payload=sent.get().toString();
        assertThat(payload).contains("Buyer handle").doesNotContain("@private_person","private@example.com","0812345678","Private customer sheet","PHOTOBOOK VER.");
        mapping.validateApproval(plan,plan.mappings());var rows=reader.read(bytes,plan.mappings());assertThat(rows.rows()).hasSize(1);assertThat(rows.rows().getFirst().get("customer")).isEqualTo("@private_person");assertThat(rows.rows().getFirst().get("total")).isEqualTo("590");
        Files.createDirectories(Path.of(".runtime/product-setup-qa"));new ObjectMapper().writeValue(Path.of(".runtime/product-setup-qa/orders-ai-mapping-plan.json").toFile(),plan);
    }
    @Test void excelAiFailureUsesRulesAndHumanCanCorrectMappingBeforeValidation() throws Exception {
        enable();status.set(429);var reader=new OrderExcelReader();var mapping=new OrderExcelMappingService(reader,ai,mapper);byte[] bytes=spreadsheet();var plan=mapping.inspect(1,bytes);
        assertThat(plan.source()).isEqualTo("FALLBACK");assertThat(plan.message()).contains("โควตา");
        Map<Integer,String> fields=new LinkedHashMap<>();int index=0;for(String field:List.of("customer","channel","product","website","version","cover","qty","total","paid","balance","date","paidDate","status","remark"))fields.put(index++,field);
        var approved=List.of(new Mapping(0,1,fields));mapping.validateApproval(plan,approved);assertThat(reader.read(bytes,approved).rows().getFirst().get("product")).isEqualTo("Album");
        var duplicate=new LinkedHashMap<>(fields);duplicate.put(6,"customer");assertThatThrownBy(()->mapping.validateApproval(plan,List.of(new Mapping(0,1,duplicate)))).hasMessageContaining("ซ้ำ");
    }
    @Test void disabledExcelUsesRulesWithoutAiRequest() throws Exception {
        var plan=new OrderExcelMappingService(new OrderExcelReader(),ai,mapper).inspect(1,spreadsheet());assertThat(plan.source()).isEqualTo("RULES");assertThat(posts.get()).isZero();
    }
    @Test void malformedAiMappingFallsBackInsteadOfTruncatingColumnOrSheetNumbers() throws Exception {
        enable();output.set(mappingJson().replace("\"sheetIndex\":0","\"sheetIndex\":0.5"));
        var plan=new OrderExcelMappingService(new OrderExcelReader(),ai,mapper).inspect(1,spreadsheet());assertThat(plan.source()).isEqualTo("FALLBACK");assertThat(posts.get()).isEqualTo(1);
    }
    @Test void otherConfiguredProvidersUseTheirOwnProtocolAndTheSelectedModel() throws Exception {
        String root="http://127.0.0.1:"+server.getAddress().getPort();var calls=new AtomicInteger();
        server.createContext("/api/tags",exchange->{byte[] body="{\"models\":[{\"name\":\"selected-model\"}]}".getBytes();exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});
        server.createContext("/models",exchange->{byte[] body="{\"data\":[{\"id\":\"selected-model\"}]}".getBytes();exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});
        server.createContext("/api/chat",exchange->{
            JsonNode body=mapper.readTree(exchange.getRequestBody().readAllBytes());assertThat(body.path("model").asText()).isEqualTo("selected-model");assertThat(body.at("/messages/0/images/0").asText()).isEqualTo("AQID");assertThat(body.path("stream").asBoolean()).isFalse();calls.incrementAndGet();
            byte[] response=mapper.createObjectNode().set("message",mapper.createObjectNode().put("content",IMAGE)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
        });
        server.createContext("/chat/completions",exchange->{
            JsonNode body=mapper.readTree(exchange.getRequestBody().readAllBytes());assertThat(body.path("model").asText()).isEqualTo("selected-model");assertThat(body.at("/messages/0/content/1/image_url/url").asText()).isEqualTo("data:image/png;base64,AQID");calls.incrementAndGet();
            var response=mapper.createObjectNode();response.putArray("choices").addObject().put("finish_reason","stop").putObject("message").put("content",IMAGE);byte[] bytes=response.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        for(String provider:List.of("OLLAMA","LM_STUDIO","COMPATIBLE")) {
            ai.test(1,new Draft(provider,root,"selected-model","",false,false,3));ai.save(1,new Draft(provider,root,"selected-model","",false,false,3,true));
            assertThat(pipeline().read(1,image(),new ReadProgress()).versions().getFirst().groups().getFirst().price()).isEqualByComparingTo("590");
        }
        assertThat(calls.get()).isEqualTo(3);assertThat(posts.get()).isZero();verify(local,never()).read(any(),any(ReadProgress.class));
    }
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="ocr.ai-fallback.sample",matches=".+")
    void quotaFallbackReadsTheRealSelectedPosterCropWithInstalledLocalOcr() throws Exception {
        enable();status.set(429);
        var pixels=javax.imageio.ImageIO.read(Path.of(System.getProperty("ocr.ai-fallback.sample")).toFile());var bytes=new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(pixels.getSubimage(595,415,605,785),"png",bytes);
        var upload=new MockMultipartFile("image","selected-crop.png","image/png",bytes.toByteArray());
        var realOcr=new LocalProductImageReader(mapper,new ProductImageTextParser(),"python","",120);
        var result=new ProductImageAiReader(ai,realOcr,mapper).read(1,upload,new ReadProgress());
        assertThat(result.versions().stream().flatMap(version->version.groups().stream()).count()).isEqualTo(6);
        assertThat(result.versions().getFirst().groups().getFirst().price()).isEqualByComparingTo("590");
        assertThat(result.versions().getFirst().groups().getFirst().deposit()).isEqualByComparingTo("300");
        assertThat(result.warnings()).anySatisfy(text->assertThat(text).contains("OCR").contains("โควตา"));assertThat(posts.get()).isEqualTo(1);
    }
}
