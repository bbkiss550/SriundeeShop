package com.sriundee.preorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiApiSettingsService {
    public record Draft(String provider,String baseUrl,String model,String apiKey,boolean removeKey,boolean allowRemote,Integer timeoutSeconds,Boolean enabled) {
        public Draft(String provider,String baseUrl,String model,String apiKey,boolean removeKey,boolean allowRemote,Integer timeoutSeconds){this(provider,baseUrl,model,apiKey,removeKey,allowRemote,timeoutSeconds,null);}
    }
    public record View(String provider,String baseUrl,String model,boolean keyConfigured,boolean allowRemote,int timeoutSeconds,boolean enabled,boolean tested) {
        public View(String provider,String baseUrl,String model,boolean keyConfigured,boolean allowRemote,int timeoutSeconds){this(provider,baseUrl,model,keyConfigured,allowRemote,timeoutSeconds,false,false);}
    }
    public record Connection(boolean connected,boolean modelFound,List<String> models,String message) {}
    private record Stored(String provider,String baseUrl,String model,String encryptedKey,boolean allowRemote,int timeoutSeconds,boolean enabled,String verified) {}
    private final Map<Integer,Set<String>> tested=new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer,java.util.concurrent.Semaphore> calls=new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String,Long> cooldowns=new java.util.concurrent.ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final Path directory;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final SecureRandom random=new SecureRandom();
    public AiApiSettingsService(ObjectMapper mapper,@Value("${shop.ai.config-directory:${user.home}/.sriundee-shop/ai}") String directory) {
        this.mapper=mapper;this.directory=Path.of(directory);
    }
    public synchronized View get(int user) { return view(read(user)); }
    public synchronized View save(int user,Draft draft) {
        Stored previous=read(user); Stored next=validated(draft,true);
        String key=resolveKey(previous,next,draft);
        if(next.provider().equals("GEMINI") && key.isBlank() && !draft.removeKey())throw new IllegalArgumentException("กรุณาระบุ Gemini API key");
        String signature=signature(next,key,next.model());
        boolean verified=signature.equals(previous.verified()) || tested.getOrDefault(user,Set.of()).contains(signature);
        boolean enabled=draft.enabled()==null?previous.enabled():draft.enabled();
        if(enabled && !verified)throw new IllegalArgumentException("กรุณาทดสอบการเชื่อมต่อให้สำเร็จและเลือกโมเดลที่พบก่อนเปิดใช้ AI");
        Stored stored=new Stored(next.provider(),next.baseUrl(),next.model(),key.isEmpty()?"":encrypt(key),next.allowRemote(),next.timeoutSeconds(),enabled,verified?signature:"");
        try {
            prepareDirectory();Path temporary=Files.createTempFile(directory,"settings-",".tmp");
            try {
                restrict(temporary,false);Files.write(temporary,mapper.writeValueAsBytes(stored));
                try {Files.move(temporary,path(user),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,path(user),StandardCopyOption.REPLACE_EXISTING);}
            } finally {Files.deleteIfExists(temporary);}
            return view(stored);
        } catch(Exception failure){throw new IllegalStateException("บันทึกการตั้งค่า AI ไม่สำเร็จ กรุณาตรวจสิทธิ์โฟลเดอร์ของเซิร์ฟเวอร์");}
    }
    public Connection test(int user,Draft draft) {
        Stored target=validated(draft,false);String key;
        synchronized(this){key=resolveKey(read(user),target,draft);}
        boolean gemini=target.provider().equals("GEMINI");
        if(gemini && key.isBlank())throw new IllegalArgumentException("กรุณาระบุ Gemini API key ก่อนทดสอบ");
        Set<String> models=new TreeSet<>();Set<String> visited=new HashSet<>();String pageToken="";
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(target.timeoutSeconds());
        do {
            String suffix=target.provider().equals("OLLAMA")?"/api/tags":"/models";
            if(gemini)suffix+="?pageSize=1000"+(pageToken.isEmpty()?"":"&pageToken="+java.net.URLEncoder.encode(pageToken,java.nio.charset.StandardCharsets.UTF_8));
            JsonNode json=fetchModels(URI.create(target.baseUrl()+suffix),target,key,deadline);
            JsonNode list=json.path(gemini || target.provider().equals("OLLAMA")?"models":"data");
            if(!list.isArray())throw new IllegalArgumentException("รูปแบบคำตอบไม่ใช่ API รายชื่อโมเดลที่รองรับ");
            for(JsonNode item:list) {
                if(gemini) {
                    boolean generation=false;
                    for(JsonNode method:item.path("supportedGenerationMethods"))if(method.asText().equals("generateContent"))generation=true;
                    if(!generation)continue;
                }
                String name=item.path(gemini || target.provider().equals("OLLAMA")?"name":"id").asText("");
                if(gemini)name=name.replaceFirst("^models/","");
                if(!name.isBlank() && name.length()<=256 && models.size()<500)models.add(name);
            }
            pageToken=gemini?json.path("nextPageToken").asText(""):"";
            if(!pageToken.isEmpty() && (pageToken.length()>2048 || !visited.add(pageToken) || visited.size()>10))throw new IllegalArgumentException("ข้อมูลแบ่งหน้าของ API ไม่ถูกต้อง กรุณาทดลองใหม่");
        } while(!pageToken.isEmpty() && models.size()<500);
        boolean found=models.contains(target.model());
        Set<String> signatures=new HashSet<>();for(String model:models)signatures.add(signature(target,key,model));tested.put(user,Set.copyOf(signatures));
        return new Connection(true,found,List.copyOf(models),models.isEmpty()?"เชื่อมต่อได้ แต่ยังไม่มีโมเดลสร้างข้อความให้ใช้งาน":found?"เชื่อมต่อได้และพบโมเดลที่เลือก":"เชื่อมต่อได้ กรุณาเลือกโมเดลจากรายการที่พบ");
    }
    private JsonNode fetchModels(URI uri,Stored target,String key,long deadline) {
        return exchange(uri,target,key,deadline,null,()->false);
    }
    private JsonNode exchange(URI uri,Stored target,String key,long deadline,JsonNode payload,java.util.function.BooleanSupplier cancelled) {
        HttpRequest.Builder request=HttpRequest.newBuilder(uri).timeout(Duration.ofNanos(Math.max(1,deadline-System.nanoTime()))).header("Accept","application/json");
        if(payload==null)request.GET();else request.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(payload.toString()));
        if(!key.isBlank()) {
            if(target.provider().equals("GEMINI"))request.header("x-goog-api-key",key);
            else request.header("Authorization","Bearer "+key);
        }
        try {
            if(System.nanoTime()>=deadline)throw new IllegalArgumentException("อ่านรายชื่อโมเดลหมดเวลา กรุณาเพิ่มเวลารอแล้วลองใหม่");
            var pending=client.sendAsync(request.build(),HttpResponse.BodyHandlers.ofInputStream());
            HttpResponse<java.io.InputStream> response;
            try {response=await(pending,deadline,cancelled);} finally {if(!pending.isDone())pending.cancel(true);}
            try(var body=response.body()) {
                if(response.statusCode()!=200) {
                    String message=switch(response.statusCode()) {
                        case 401,403 -> "API ปฏิเสธสิทธิ์ กรุณาตรวจ API key";
                        case 404 -> "ไม่พบ API รายชื่อโมเดล กรุณาตรวจชนิดบริการและ Base URL";
                        case 429 -> "API จำกัดความถี่หรือโควตา กรุณาตรวจโควตาของบัญชีแล้วลองใหม่ภายหลัง";
                        default -> "API ตอบกลับ HTTP "+response.statusCode()+" กรุณาตรวจบริการปลายทาง";
                    };
                    throw new AiHttpError(response.statusCode(),message);
                }
                var reading=java.util.concurrent.CompletableFuture.supplyAsync(()->{
                    try{return body.readNBytes(1_048_577);}catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
                });
                byte[] bytes;
                try{bytes=await(reading,deadline,cancelled);}finally{if(!reading.isDone())reading.cancel(true);}
                if(bytes.length>1_048_576)throw new IllegalArgumentException("API ส่งข้อมูลขนาดใหญ่เกินที่รองรับ");
                return mapper.readTree(bytes);
            }
        } catch(IllegalArgumentException failure){throw failure;}
        catch(AiCancelled cancelledRequest){throw cancelledRequest;}
        catch(HttpTimeoutException timeout){throw new IllegalArgumentException("เชื่อมต่อหมดเวลา กรุณาตรวจบริการปลายทางหรือเพิ่มเวลารอ");}
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalArgumentException("ยกเลิกการทดสอบการเชื่อมต่อแล้ว");}
        catch(Exception failure){throw new IllegalArgumentException("เชื่อมต่อไม่ได้ กรุณาตรวจว่า AI Server เปิดอยู่ และ Base URL เข้าถึงได้จากเครื่องที่รันระบบร้าน");}
    }
    private Stored validated(Draft draft,boolean requireModel) {
        if(draft==null || !Set.of("OLLAMA","LM_STUDIO","COMPATIBLE","GEMINI").contains(Objects.toString(draft.provider(),"")))throw new IllegalArgumentException("กรุณาเลือกบริการ AI ที่รองรับ");
        String base=Objects.toString(draft.baseUrl(),"").strip().replaceAll("/+$","");
        URI uri;try{uri=URI.create(base);}catch(Exception failure){throw new IllegalArgumentException("Base URL ไม่ถูกต้อง");}
        String host=Objects.toString(uri.getHost(),"").toLowerCase(Locale.ROOT).replace("[","").replace("]","");
        boolean loopback=host.equals("localhost") || host.equals("::1") || host.matches("127(?:\\.\\d{1,3}){3}");
        boolean privateLan=host.matches("192\\.168(?:\\.\\d{1,3}){2}") || host.matches("10(?:\\.\\d{1,3}){3}") || host.matches("172\\.(?:1[6-9]|2\\d|3[01])(?:\\.\\d{1,3}){2}");
        if(base.length()>512 || host.isBlank() || uri.getPort()>65535 || uri.getPort()==0 || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null || !Set.of("http","https").contains(uri.getScheme()))throw new IllegalArgumentException("Base URL ต้องเป็น http หรือ https และไม่มีรหัสผ่าน query หรือ fragment");
        if(host.startsWith("169.254.") || host.startsWith("fe80:") || host.equals("0.0.0.0") || host.equals("metadata.google.internal"))throw new IllegalArgumentException("ไม่รองรับปลายทางนี้");
        if(!draft.allowRemote() && !loopback)throw new IllegalArgumentException("ตั้งค่าให้ใช้ภายในเครื่องเท่านั้น หากต้องการปลายทางนอกเครื่องให้เปิดอนุญาตก่อน");
        if(!loopback && !privateLan && !"https".equals(uri.getScheme()))throw new IllegalArgumentException("API ภายนอกต้องใช้ HTTPS");
        String model=Objects.toString(draft.model(),"").strip();
        if(draft.provider().equals("GEMINI"))model=model.replaceFirst("^models/","");
        if(model.length()>128 || requireModel && model.isEmpty())throw new IllegalArgumentException("กรุณาระบุชื่อโมเดล ไม่เกิน 128 ตัวอักษร");
        String key=Objects.toString(draft.apiKey(),"");
        if(key.length()>2048 || key.chars().anyMatch(c->c<32 || c>126))throw new IllegalArgumentException("API key ไม่ถูกต้อง");
        int timeout=draft.timeoutSeconds()==null?15:draft.timeoutSeconds();
        if(timeout<3 || timeout>60)throw new IllegalArgumentException("เวลารอต้องอยู่ระหว่าง 3–60 วินาที");
        return new Stored(draft.provider(),base,model,"",draft.allowRemote(),timeout,Boolean.TRUE.equals(draft.enabled()),"");
    }
    private String resolveKey(Stored old,Stored next,Draft draft) {
        if(draft.removeKey())return "";
        if(draft.apiKey()!=null && !draft.apiKey().isBlank())return draft.apiKey().strip();
        // Never forward a saved credential to a changed destination.
        if(old.provider().equals(next.provider()) && old.baseUrl().equals(next.baseUrl()) && !old.encryptedKey().isBlank())return decrypt(old.encryptedKey());
        return "";
    }
    private View view(Stored value){return new View(value.provider(),value.baseUrl(),value.model(),!value.encryptedKey().isBlank(),value.allowRemote(),value.timeoutSeconds(),value.enabled(),value.verified()!=null&&!value.verified().isBlank());}
    private Path path(int user){if(user<=0)throw new IllegalArgumentException("บัญชีผู้ใช้ไม่ถูกต้อง");return directory.resolve("user-"+user+".json");}
    private Stored read(int user) {
        Path path=path(user);if(!Files.exists(path))return new Stored("OLLAMA","http://localhost:11434","","",false,15,false,"");
        try{if(Files.size(path)>16_384)throw new IllegalStateException();return mapper.readValue(Files.readAllBytes(path),Stored.class);}
        catch(Exception failure){throw new IllegalStateException("อ่านการตั้งค่า AI ไม่สำเร็จ กรุณาตรวจไฟล์ตั้งค่าบนเซิร์ฟเวอร์");}
    }
    private void prepareDirectory() throws Exception {Files.createDirectories(directory);restrict(directory,true);}
    private void restrict(Path path,boolean folder) throws Exception {
        if(Files.getFileAttributeView(path,PosixFileAttributeView.class)!=null)Files.setPosixFilePermissions(path,PosixFilePermissions.fromString(folder?"rwx------":"rw-------"));
        else if(Files.getFileAttributeView(path,AclFileAttributeView.class)!=null) {
            var view=Files.getFileAttributeView(path,AclFileAttributeView.class);
            view.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(Files.getOwner(path)).setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
        } else throw new IllegalStateException("ไม่สามารถจำกัดสิทธิ์ไฟล์ตั้งค่าได้");
    }
    private synchronized byte[] masterKey() throws Exception {
        prepareDirectory();Path key=directory.resolve("master.key");
        if(!Files.exists(key)){byte[] bytes=new byte[32];random.nextBytes(bytes);Files.createFile(key);restrict(key,false);Files.write(key,bytes);}
        byte[] bytes=Files.readAllBytes(key);if(bytes.length!=32)throw new IllegalStateException();return bytes;
    }
    private String encrypt(String secret) {
        try {
            byte[] iv=new byte[12];random.nextBytes(iv);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(masterKey(),"AES"),new GCMParameterSpec(128,iv));
            return Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(cipher.doFinal(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }catch(Exception failure){throw new IllegalStateException("จัดเก็บ API key ไม่สำเร็จ");}
    }
    private String decrypt(String secret) {
        try {
            String[] parts=secret.split(":");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(masterKey(),"AES"),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])),java.nio.charset.StandardCharsets.UTF_8);
        }catch(Exception failure){throw new IllegalStateException("อ่าน API key ไม่สำเร็จ กรุณากรอกและบันทึกใหม่");}
    }
    private String signature(Stored value,String key,String model) {
        try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((value.provider()+"\n"+value.baseUrl()+"\n"+model+"\n"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(Exception failure){throw new IllegalStateException("ตรวจการตั้งค่า AI ไม่สำเร็จ");}
    }
    public boolean enabled(int user){try{return read(user).enabled();}catch(RuntimeException unavailable){return false;}}
    private <T> T await(java.util.concurrent.CompletableFuture<T> future,long deadline,java.util.function.BooleanSupplier cancelled) throws Exception {
        while(true){
            if(cancelled.getAsBoolean())throw new AiCancelled();
            long remaining=deadline-System.nanoTime();if(remaining<=0)throw new AiUnavailable("AI หมดเวลา");
            try{return future.get(Math.min(remaining,100_000_000),java.util.concurrent.TimeUnit.NANOSECONDS);}
            catch(java.util.concurrent.TimeoutException wait){ /* Poll cancellation without issuing another request. */ }
        }
    }
    /** The shared runtime uses exactly the saved provider/model; never changes models or retries a paid call. */
    public JsonNode generateJson(int user,String prompt,byte[] image,String mimeType,JsonNode schema,java.util.function.BooleanSupplier cancelled) {
        Stored config;String key;
        try{synchronized(this){config=read(user);key=config.encryptedKey().isBlank()?"":decrypt(config.encryptedKey());}}
        catch(RuntimeException failure){throw new AiUnavailable("อ่านการตั้งค่า AI ไม่สำเร็จ");}
        String signature=signature(config,key,config.model());
        if(!config.enabled() || !(signature.equals(config.verified()) || tested.getOrDefault(user,Set.of()).contains(signature)))throw new AiUnavailable("AI ยังไม่ได้เปิดใช้หรือยังไม่ผ่านการทดสอบการเชื่อมต่อ");
        validated(new Draft(config.provider(),config.baseUrl(),config.model(),"",false,config.allowRemote(),config.timeoutSeconds()),true);
        if(cooldowns.getOrDefault(signature,0L)>System.currentTimeMillis())throw new AiUnavailable("AI จำกัดโควตา/ความถี่ (429) กำลังพักการเรียก API");
        var slot=calls.computeIfAbsent(user,ignored->new java.util.concurrent.Semaphore(1));
        if(!slot.tryAcquire())throw new AiUnavailable("AI กำลังประมวลผลคำขออื่น");
        try {
            var body=mapper.createObjectNode();String suffix;
            if(config.provider().equals("GEMINI")) {
                suffix="/models/"+java.net.URLEncoder.encode(config.model(),java.nio.charset.StandardCharsets.UTF_8)+":generateContent";
                var parts=body.putArray("contents").addObject().put("role","user").putArray("parts");parts.addObject().put("text",prompt);
                if(image!=null)parts.addObject().putObject("inlineData").put("mimeType",mimeType).put("data",Base64.getEncoder().encodeToString(image));
                var generation=body.putObject("generationConfig");generation.put("responseMimeType","application/json").put("temperature",0).put("maxOutputTokens",8192);
                if(schema!=null)generation.set("responseJsonSchema",schema);
            } else {
                body.put("model",config.model());var message=body.putArray("messages").addObject().put("role","user");
                if(config.provider().equals("OLLAMA")) {
                    suffix="/api/chat";message.put("content",prompt);if(image!=null)message.putArray("images").add(Base64.getEncoder().encodeToString(image));
                    body.put("stream",false);if(schema!=null)body.set("format",schema);else body.put("format","json");body.putObject("options").put("temperature",0).put("num_predict",8192);
                } else {
                    suffix="/chat/completions";body.put("temperature",0).put("max_tokens",8192);body.putObject("response_format").put("type","json_object");
                    if(image==null)message.put("content",prompt);else {var content=message.putArray("content");content.addObject().put("type","text").put("text",prompt);content.addObject().put("type","image_url").putObject("image_url").put("url","data:"+mimeType+";base64,"+Base64.getEncoder().encodeToString(image));}
                }
            }
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(config.timeoutSeconds());
            JsonNode response=exchange(URI.create(config.baseUrl()+suffix),config,key,deadline,body,cancelled);String text;
            if(config.provider().equals("GEMINI")) {
                JsonNode candidate=response.path("candidates").path(0);
                if(!candidate.path("finishReason").asText("STOP").equals("STOP"))throw new AiUnavailable("AI ตอบไม่ครบหรือไม่สามารถวิเคราะห์ข้อมูลนี้ได้");
                StringBuilder result=new StringBuilder();for(JsonNode part:candidate.path("content").path("parts"))if(!part.path("thought").asBoolean(false))result.append(part.path("text").asText(""));text=result.toString();
            } else if(config.provider().equals("OLLAMA"))text=response.path("message").path("content").asText("");
            else {JsonNode choice=response.path("choices").path(0);if(!choice.path("finish_reason").asText("stop").equals("stop"))throw new AiUnavailable("AI ตอบไม่ครบ");text=choice.path("message").path("content").asText("");}
            text=text.strip().replaceFirst("^```(?:json)?\\s*","").replaceFirst("\\s*```$","");
            if(text.isEmpty()||text.length()>100_000)throw new AiUnavailable("AI ไม่ได้ส่ง Structured JSON ที่ใช้งานได้");
            JsonNode result=mapper.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(text);
            if(!result.isObject())throw new AiUnavailable("AI ต้องตอบกลับเป็น JSON object");return result;
        } catch(AiCancelled cancellation){throw cancellation;}
        catch(AiHttpError failure){if(failure.status==429)cooldowns.put(signature,System.currentTimeMillis()+60_000);throw new AiUnavailable(failure.getMessage());}
        catch(AiUnavailable failure){throw failure;}
        catch(Exception failure){throw new AiUnavailable("AI ไม่พร้อมใช้งานหรือผลลัพธ์ JSON ไม่ถูกต้อง");}
        finally{slot.release();}
    }
    public static class AiUnavailable extends IllegalArgumentException {public AiUnavailable(String message){super(message);}}
    public static class AiCancelled extends RuntimeException {}
    private static class AiHttpError extends IllegalArgumentException {final int status;AiHttpError(int status,String message){super(message);this.status=status;}}
}
