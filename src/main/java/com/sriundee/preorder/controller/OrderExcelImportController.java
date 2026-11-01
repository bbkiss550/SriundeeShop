package com.sriundee.preorder.controller;

import java.security.MessageDigest;
import java.util.*;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import com.sriundee.preorder.service.OrderExcelImportService;
import com.sriundee.preorder.service.OrderExcelMappingService;
import com.sriundee.preorder.service.OrderExcelReader.Mapping;

@RestController
@RequestMapping("/orders/import")
public class OrderExcelImportController {
    private final OrderExcelImportService service;
    private final OrderExcelMappingService mappingService;
    private record Pending(String token,byte[] bytes,String fingerprint,long expires,List<Mapping> mappings) {}
    private record Uploaded(String token,byte[] bytes,OrderExcelMappingService.Plan plan,long expires) {}
    public record MappingApproval(String token,List<Mapping> mappings) {}
    public record Confirmation(String token) {}
    public OrderExcelImportController(OrderExcelImportService service){this(service,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public OrderExcelImportController(OrderExcelImportService service,OrderExcelMappingService mappingService){this.service=service;this.mappingService=mappingService;}
    @PostMapping(value="/inspect",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> inspect(@RequestParam("file") MultipartFile file,HttpSession session) throws Exception {
        Object id=session.getAttribute(LoginController.SESSION_USER_ID);if(!(id instanceof Number user))throw new IllegalArgumentException("กรุณาเข้าสู่ระบบใหม่");
        String ticket=UUID.randomUUID().toString();
        synchronized(session){session.removeAttribute("orderExcelPending");session.removeAttribute("orderExcelLayout");session.setAttribute("orderExcelInspectionId",ticket);}
        checkFile(file);byte[] bytes=file.getBytes();
        var plan=mappingService.inspect(user.intValue(),bytes);
        synchronized(session){if(!ticket.equals(session.getAttribute("orderExcelInspectionId")))throw new IllegalArgumentException("มีการเลือกไฟล์ใหม่ กรุณาใช้ผลตรวจของไฟล์ล่าสุด");session.setAttribute("orderExcelLayout",new Uploaded(ticket,bytes,plan,System.currentTimeMillis()+10*60_000));}
        return Map.of("mappingToken",ticket,"plan",plan);
    }
    @PostMapping("/validate-mapping")
    public Map<String,Object> validateMapping(@RequestBody MappingApproval approval,HttpSession session) throws Exception {
        synchronized(session){
            session.removeAttribute("orderExcelPending");Uploaded upload=(Uploaded)session.getAttribute("orderExcelLayout");
            if(upload==null || upload.expires()<System.currentTimeMillis() || !upload.token().equals(approval.token()))throw new IllegalArgumentException("ไฟล์หรือผลจับคู่หมดอายุ กรุณาเลือกไฟล์ใหม่");
            mappingService.validateApproval(upload.plan(),approval.mappings());
            var report=service.validate(upload.bytes(),approval.mappings());String token="";
            if(report.valid()){token=UUID.randomUUID().toString();String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(upload.bytes()));session.setAttribute("orderExcelPending",new Pending(token,upload.bytes(),hash,System.currentTimeMillis()+10*60_000,List.copyOf(approval.mappings())));}
            return Map.of("report",report,"token",token);
        }
    }
    @PostMapping(value="/validate",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> validate(@RequestParam("file") MultipartFile file,HttpSession session) throws Exception {
        synchronized(session) {
            session.removeAttribute("orderExcelPending");
            session.removeAttribute("orderExcelLayout");session.removeAttribute("orderExcelInspectionId");
            checkFile(file);
            byte[] bytes=file.getBytes();var report=service.validate(bytes);
            String token="";
            if(report.valid()) {
                token=UUID.randomUUID().toString();String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                session.setAttribute("orderExcelPending",new Pending(token,bytes,hash,System.currentTimeMillis()+10*60_000,List.of()));
            }
            return Map.of("report",report,"token",token);
        }
    }
    @PostMapping("/confirm")
    public Map<String,Object> confirm(@RequestBody Confirmation confirmation,HttpSession session) {
        synchronized(session) {
            Pending pending=(Pending)session.getAttribute("orderExcelPending");
            if(pending==null || pending.expires()<System.currentTimeMillis() || !pending.token().equals(confirmation.token())) throw new IllegalArgumentException("ผลตรวจหมดอายุหรือไม่ตรงกับไฟล์ กรุณาอ่านและตรวจสอบใหม่");
            Object user=session.getAttribute(LoginController.SESSION_USER_ID);
            if(!(user instanceof Number)) throw new IllegalArgumentException("กรุณาเข้าสู่ระบบใหม่");
            var result=pending.mappings().isEmpty()?service.commit(pending.bytes(),pending.fingerprint(),((Number)user).intValue()):service.commit(pending.bytes(),pending.fingerprint(),((Number)user).intValue(),pending.mappings());
            session.removeAttribute("orderExcelPending");session.removeAttribute("orderExcelLayout");session.removeAttribute("orderExcelInspectionId");return result;
        }
    }
    private void checkFile(MultipartFile file){if(file.isEmpty() || file.getSize()>8*1024*1024 || file.getOriginalFilename()==null || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))throw new IllegalArgumentException("กรุณาเลือกไฟล์ .xlsx ขนาดไม่เกิน 8 MB");}
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        try(var book=new XSSFWorkbook();var output=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("Orders");var row=sheet.createRow(0);
            String[] headers={"ชื่อลูกค้า","ช่องทาง","สินค้า","เว็บ","เวอร์ชัน","ปก","จำนวน","ราคาเต็มรวม","ยอดชำระแล้ว","ยอดคงเหลือ","วันที่สั่งซื้อ","วันที่ชำระ","สถานะ","หมายเหตุ"};
            for(int i=0;i<headers.length;i++){row.createCell(i).setCellValue(headers[i]);sheet.setColumnWidth(i,24*256);}
            sheet.createFreezePane(0,1);book.write(output);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=orders-import-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(output.toByteArray());
        }
    }
    @ExceptionHandler(OrderExcelImportService.Rejected.class)
    public ResponseEntity<?> rejected(OrderExcelImportService.Rejected error){return ResponseEntity.status(409).body(Map.of("message",error.getMessage(),"report",error.report));}
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException error){return ResponseEntity.badRequest().body(Map.of("message",error.getMessage()));}
}
