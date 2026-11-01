package com.sriundee.preorder.service;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.sriundee.preorder.service.AiApiSettingsService.*;

class AiApiSettingsServiceTest {
    @TempDir Path directory;
    AiApiSettingsService service(){return new AiApiSettingsService(new ObjectMapper(),directory.toString());}
    Draft draft(String provider,String url,String key){return new Draft(provider,url,"test-model",key,false,false,3);}
    HttpServer server(String path,int code,String body,AtomicReference<String> authorization) throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext(path,exchange->{
            assertThat(exchange.getRequestMethod()).isEqualTo("GET");assertThat(exchange.getRequestBody().readAllBytes()).isEmpty();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));byte[] bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(code,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();return server;
    }
    String base(HttpServer server){return "http://127.0.0.1:"+server.getAddress().getPort();}
    @Test void defaultsToLocalAndDoesNotCreateAnyFilesUntilSave(){
        var view=service().get(1);assertThat(view.provider()).isEqualTo("OLLAMA");assertThat(view.allowRemote()).isFalse();assertThat(view.keyConfigured()).isFalse();
        assertThat(directory.toFile().list()).isEmpty();
    }
    @Test void keyIsEncryptedAndSurvivesRestartWithoutBeingReturned() throws Exception {
        var authorization=new AtomicReference<String>();var server=server("/api/tags",200,"{\"models\":[{\"name\":\"test-model\"}]}",authorization);
        try {
            var service=service();var view=service.save(1,draft("OLLAMA",base(server),"private-test-secret"));
            assertThat(view.keyConfigured()).isTrue();assertThat(new ObjectMapper().writeValueAsString(view)).doesNotContain("private-test-secret");
            assertThat(Files.readString(directory.resolve("user-1.json"))).doesNotContain("private-test-secret");
            assertThat(service().test(1,draft("OLLAMA",base(server),"")).modelFound()).isTrue();assertThat(authorization.get()).isEqualTo("Bearer private-test-secret");
            assertThat(service().get(2).keyConfigured()).isFalse();
        }finally{server.stop(0);}
    }
    @Test void changedDestinationNeverReceivesSavedKey() throws Exception {
        var authorization=new AtomicReference<String>();var server=server("/models",200,"{\"data\":[{\"id\":\"test-model\"}]}",authorization);
        try {
            var service=service();service.save(1,draft("COMPATIBLE",base(server)+"/old","saved-secret"));
            service.test(1,draft("COMPATIBLE",base(server),""));assertThat(authorization.get()).isNull();
            assertThat(service.save(1,draft("COMPATIBLE",base(server),"")).keyConfigured()).isFalse();
        }finally{server.stop(0);}
    }
    @Test void removeKeyIsExplicitAndEmptyFieldRetainsIt() {
        var service=service();service.save(1,draft("OLLAMA","http://localhost:11434","secret"));
        assertThat(service.save(1,draft("OLLAMA","http://localhost:11434","")).keyConfigured()).isTrue();
        assertThat(service.save(1,new Draft("OLLAMA","http://localhost:11434","model","",true,false,15)).keyConfigured()).isFalse();
    }
    @Test void readsCompatibleModelsWithoutSendingCustomerDataOrSaving() throws Exception {
        var authorization=new AtomicReference<String>();var server=server("/v1/models",200,"{\"data\":[{\"id\":\"test-model\"},{\"id\":\"another\"}]}",authorization);
        try {var result=service().test(1,draft("LM_STUDIO",base(server)+"/v1",""));assertThat(result.models()).containsExactly("another","test-model");assertThat(directory.toFile().list()).isEmpty();}
        finally{server.stop(0);}
    }
    @Test void remoteRequiresExplicitPermissionAndHttps() {
        assertThatThrownBy(()->service().save(1,draft("COMPATIBLE","https://example.com/v1",""))).hasMessageContaining("นอกเครื่อง");
        assertThatThrownBy(()->service().save(1,new Draft("COMPATIBLE","http://example.com/v1","m","",false,true,15))).hasMessageContaining("HTTPS");
        assertThat(service().save(1,new Draft("COMPATIBLE","https://example.com/v1","m","",false,true,15)).allowRemote()).isTrue();
    }
    @Test void rejectsCredentialsInUrlInvalidTimeoutsAndUnknownProviders() {
        assertThatThrownBy(()->service().save(1,draft("OLLAMA","http://user:secret@localhost:11434",""))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service().save(1,new Draft("OLLAMA","http://localhost:11434","m","",false,false,100))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service().save(1,draft("OTHER","http://localhost:1",""))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service().save(1,draft("OLLAMA","http://localhost:1?key=secret",""))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service().save(1,draft("OLLAMA","http://localhost:1","keyทดสอบ"))).hasMessage("API key ไม่ถูกต้อง");
    }
    @Test void providerErrorsNeverExposeBodyOrCredentials() throws Exception {
        var auth=new AtomicReference<String>();var server=server("/api/tags",401,"private-secret-in-error-body",auth);
        try{assertThatThrownBy(()->service().test(1,draft("OLLAMA",base(server),"test-secret"))).hasMessageContaining("สิทธิ์").hasMessageNotContaining("private-secret");}
        finally{server.stop(0);}
    }
    @Test void arbitrarySuccessfulHtmlIsNotAConnectionSuccess() throws Exception {
        var auth=new AtomicReference<String>();var server=server("/api/tags",200,"{\"hello\":\"world\"}",auth);
        try{assertThatThrownBy(()->service().test(1,draft("OLLAMA",base(server),""))).hasMessageContaining("รูปแบบ");}finally{server.stop(0);}
    }
    @Test void geminiUsesGoogleHeaderPaginationAndGenerationModelsOnly() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        server.createContext("/v1beta/models",exchange->{
            assertThat(exchange.getRequestHeaders().getFirst("x-goog-api-key")).isEqualTo("private-gemini-key");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isNull();
            assertThat(exchange.getRequestURI().toString()).doesNotContain("private-gemini-key");
            boolean second=exchange.getRequestURI().getRawQuery().contains("pageToken=");calls.incrementAndGet();
            if(second)assertThat(exchange.getRequestURI().getRawQuery()).contains("next%2Bpage%2F%3D");
            String json=second?"{\"models\":[{\"name\":\"models/test-model\",\"supportedGenerationMethods\":[\"generateContent\"]}]}"
                :"{\"models\":[{\"name\":\"models/embed-model\",\"supportedGenerationMethods\":[\"embedContent\"]},{\"name\":\"models/other-chat\",\"supportedGenerationMethods\":[\"generateContent\"]}],\"nextPageToken\":\"next+page/=\"}";
            byte[] body=json.getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();
        try {
            var service=service();String url=base(server)+"/v1beta";
            var saved=service.save(1,new Draft("GEMINI",url,"models/test-model","private-gemini-key",false,false,3));
            assertThat(saved.model()).isEqualTo("test-model");assertThat(saved.keyConfigured()).isTrue();
            var result=service().test(1,new Draft("GEMINI",url,"models/test-model","",false,false,3));
            assertThat(result.models()).containsExactly("other-chat","test-model");assertThat(result.modelFound()).isTrue();assertThat(calls.get()).isEqualTo(2);
            assertThat(Files.readString(directory.resolve("user-1.json"))).doesNotContain("private-gemini-key");
        }finally{server.stop(0);}
    }
    @Test void geminiRequiresKeyAndStillRequiresRemoteConsent() {
        assertThatThrownBy(()->service().test(1,draft("GEMINI","http://localhost:1/v1beta",""))).hasMessageContaining("Gemini API key");
        assertThatThrownBy(()->service().save(1,draft("GEMINI","http://localhost:1/v1beta",""))).hasMessageContaining("Gemini API key");
        assertThatThrownBy(()->service().save(1,draft("GEMINI","https://generativelanguage.googleapis.com/v1beta","dummy-key"))).hasMessageContaining("นอกเครื่อง");
    }
}
