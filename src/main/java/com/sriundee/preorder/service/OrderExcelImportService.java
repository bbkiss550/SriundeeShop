package com.sriundee.preorder.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sriundee.preorder.entity.*;
import com.sriundee.preorder.repository.*;
import com.sriundee.preorder.service.OrderExcelReader.*;
import static com.sriundee.preorder.service.OrderExcelReader.normalize;

@Service
public class OrderExcelImportService {
    public record Catalog(int id,String product,String website,String version,String cover) {}
    public record Item(String sheet,int row,String customer,String product,String website,String version,String cover,
            int coverId,int qty,BigDecimal total,BigDecimal paid,BigDecimal balance,LocalDate date,LocalDate paidDate,int status,String remark) {}
    public record Report(boolean valid,int checkedRows,int problemRows,List<Issue> issues,List<Item> items) {}
    private final OrderExcelReader reader; private final JdbcTemplate jdbc;
    private final OrderRepository orders; private final OrderDetailRepository details; private final IncomeRepository incomes;
    public OrderExcelImportService(OrderExcelReader reader,JdbcTemplate jdbc,OrderRepository orders,OrderDetailRepository details,IncomeRepository incomes) {
        this.reader=reader;this.jdbc=jdbc;this.orders=orders;this.details=details;this.incomes=incomes;
    }
    List<Catalog> catalog() {
        return jdbc.query("""
            SELECT c.ID_cover, p.p_name, w.w_name, v.v_name, c.c_name
            FROM t_cover c JOIN t_product p ON p.ID_product=c.ID_pro
            JOIN t_website w ON w.ID_web=c.ID_web JOIN t_version v ON v.ID_ver=c.ID_ver
            WHERE c.c_delete='A' AND p.p_delete='A' AND w.w_delete='A' AND v.v_delete='A'
            """,(rs,n)->new Catalog(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)));
    }
    Map<String,Integer> statuses() { Map<String,Integer> map=new HashMap<>(); jdbc.query("SELECT ID_order_status,os_name FROM t_order_status",(org.springframework.jdbc.core.RowCallbackHandler)rs->map.put(normalize(rs.getString(2)),rs.getInt(1))); return map; }
    public Report validate(byte[] bytes) {
        var source=reader.read(bytes); return validate(source,catalog(),statuses());
    }
    public Report validate(byte[] bytes,List<Mapping> mappings){return validate(reader.read(bytes,mappings),catalog(),statuses());}
    Report validate(WorkbookRows source,List<Catalog> catalog,Map<String,Integer> statuses) {
        List<Issue> issues=new ArrayList<>(source.issues()); List<Item> items=new ArrayList<>();
        for(SourceRow row:source.rows()) {
            int before=issues.size(); String text=row.get("product");
            String product=text.lines().findFirst().orElse("").strip();
            if(row.get("customer").isBlank()) add(issues,row,"ไม่ระบุชื่อลูกค้า");
            if(row.get("customer").length()>255) add(issues,row,"ชื่อลูกค้ายาวเกิน 255 ตัวอักษร");
            List<Catalog> products=catalog.stream().filter(c->normalize(c.product()).equals(normalize(product))).toList();
            if(products.isEmpty()) add(issues,row,"ไม่พบสินค้า: "+product);
            String website=row.get("website");
            if(website.isBlank()) add(issues,row,"ไม่ระบุเว็บ หากสินค้าไม่มีเว็บให้ระบุ ไม่มีเว็บ");
            List<Catalog> shops=products.stream().filter(c->normalize(c.website()).equals(normalize(website))).toList();
            if(!products.isEmpty() && !website.isBlank() && shops.isEmpty()) add(issues,row,"สินค้าไม่มีเว็บที่ระบุ: "+website);
            String version=row.get("version"), cover=row.get("cover"); String qtyText=row.get("qty");
            if(version.isBlank() && cover.isBlank()) {
                List<String> lines=text.lines().skip(1).filter(s->!s.isBlank()).toList();
                if(lines.size()>1) add(issues,row,"มีหลายรายการในเซลล์เดียว กรุณาแยกเป็นแถวและระบุเวอร์ชัน ปก จำนวน และราคาเต็มรวมของแต่ละรายการ");
                if(lines.size()==1) {
                    String option=lines.getFirst().strip().replaceFirst("^(?:-\\s*|\\d+\\.\\s*)","");
                    Matcher qty=Pattern.compile("^(.*?)\\s+(\\d+)\\s*(?:แท่ง|ชิ้น|ชุด|บั้ม|อัลบั้ม)?$").matcher(option);
                    if(qty.matches()) { option=qty.group(1).strip(); qtyText=qty.group(2); }
                    String label=option;
                    List<Catalog> matched=shops.stream().filter(c->normalize(c.version()).equals(normalize(label)) || normalize(c.cover()).equals(normalize(label))).toList();
                    if(matched.size()==1) { version=matched.getFirst().version(); cover=matched.getFirst().cover(); }
                    else add(issues,row,"จับคู่รายการไม่ได้อย่างชัดเจน: "+label+" กรุณาระบุเวอร์ชันและปกแยกคอลัมน์");
                }
            }
            if(version.isBlank()) add(issues,row,"ไม่ระบุเวอร์ชัน"); if(cover.isBlank()) add(issues,row,"ไม่ระบุปก");
            String ver=version, cov=cover;
            List<Catalog> matches=shops.stream().filter(c->normalize(c.version()).equals(normalize(ver)) && normalize(c.cover()).equals(normalize(cov))).toList();
            if(!shops.isEmpty() && !version.isBlank() && !cover.isBlank() && matches.size()!=1) add(issues,row,matches.isEmpty()?"ไม่พบเวอร์ชัน / ปกในสินค้าและเว็บที่ระบุ: "+version+" / "+cover:"มีรายการสินค้าตรงกันหลายรายการในระบบ กรุณาแก้ข้อมูลซ้ำก่อน");
            int qty=0; try { qty=Integer.parseInt(qtyText); if(qty<=0 || qty>100_000) throw new NumberFormatException(); } catch(Exception e) {add(issues,row,"จำนวนต้องเป็นจำนวนเต็ม 1–100,000");}
            BigDecimal total=amount(row.get("total"),"ราคาเต็มรวม",true,row,issues);
            BigDecimal balance=amount(row.get("balance"),"ยอดคงเหลือ",false,row,issues);
            BigDecimal paid=amount(row.get("paid"),"ยอดชำระแล้ว",false,row,issues);
            for(String extra:List.of("shippingCost","discount")) {
                BigDecimal value=amount(row.get(extra),extra.equals("shippingCost")?"ค่าส่ง":"ส่วนลด",false,row,issues);
                if(value!=null && value.signum()!=0) add(issues,row,"ยังไม่รองรับค่าส่งหรือส่วนลดที่ไม่ใช่ 0 ในไฟล์นำเข้า กรุณาแยกตรวจยอดก่อน");
            }
            String payment=normalize(row.get("payment"));
            if(!payment.isBlank() && !Set.of("เต็ม","จ่ายเต็ม","ชำระเต็ม","มัดจำ","จ่ายมัดจำ","ชำระมัดจำ").contains(payment)) add(issues,row,"รูปแบบชำระเงินไม่รู้จัก: "+row.get("payment"));
            if(balance==null && Set.of("เต็ม","จ่ายเต็ม","ชำระเต็ม").contains(payment)) balance=BigDecimal.ZERO;
            if(total!=null && paid==null && balance!=null) paid=total.subtract(balance);
            if(total!=null && balance==null && paid!=null) balance=total.subtract(paid);
            if(paid==null || balance==null) add(issues,row,"ต้องระบุยอดคงเหลือหรือยอดชำระแล้ว ไม่สามารถเดาจากราคาสินค้าปัจจุบันได้");
            if(total!=null && (total.signum()<=0 || paid!=null && paid.signum()<0 || balance!=null && balance.signum()<0
                    || paid!=null && balance!=null && total.compareTo(paid.add(balance))!=0)) add(issues,row,"ราคาเต็มรวมต้องมากกว่า 0 และเท่ากับยอดชำระแล้ว + ยอดคงเหลือ");
            if(paid!=null && balance!=null && Set.of("เต็ม","จ่ายเต็ม","ชำระเต็ม").contains(payment) && balance.signum()!=0) add(issues,row,"ระบุจ่ายเต็มแต่ยังมียอดคงเหลือ");
            LocalDate date=date(row.get("date"),"วันที่สั่งซื้อ",row,issues,true);
            LocalDate paidDate=date(row.get("paidDate"),"วันที่ชำระ",row,issues,paid!=null && paid.signum()>0);
            Integer status=statuses.get(normalize(row.get("status")));
            if(status==null) add(issues,row,"ไม่พบสถานะในระบบ: "+row.get("status"));
            String remark=remark(row);
            if(remark.length()>255) add(issues,row,"หมายเหตุและข้อมูลประกอบรวมยาวเกิน 255 ตัวอักษร กรุณาย่อก่อนนำเข้า");
            if(issues.size()==before) items.add(new Item(row.sheet(),row.row(),row.get("customer"),product,website,version,cover,matches.getFirst().id(),qty,total,paid,balance,date,paidDate,status,remark));
        }
        int problems=(int)issues.stream().map(i->i.sheet()+"|"+i.row()).distinct().count();
        return new Report(issues.isEmpty(),source.rows().size(),problems,List.copyOf(issues),List.copyOf(items));
    }
    private String remark(SourceRow row) {
        List<String> parts=new ArrayList<>(); parts.add("Excel: "+row.sheet()+"!"+row.row()); if(!row.get("remark").isBlank()) parts.add(row.get("remark"));
        if(!row.get("channel").isBlank()) parts.add("ช่องทาง: "+row.get("channel"));
        row.cells().entrySet().stream().filter(e->e.getKey().startsWith("คอลัมน์ ")).sorted(Map.Entry.comparingByKey()).forEach(e->parts.add(e.getKey()+": "+e.getValue()));
        return String.join(" | ",parts);
    }
    private BigDecimal amount(String text,String label,boolean required,SourceRow row,List<Issue> issues) {
        if(text.isBlank()) {if(required)add(issues,row,"ไม่มี"+label+" กรุณาเพิ่มคอลัมน์ "+label+" (ยอดรวมของรายการ ไม่ใช้ราคาปัจจุบันแทน)");return null;}
        try { if(!text.matches("(?:\\d+|\\d{1,3}(?:,\\d{3})+)(?:\\.\\d{1,2})?")) throw new NumberFormatException();
            var value=new BigDecimal(text.replace(",","")).setScale(2,RoundingMode.UNNECESSARY);
            if(value.compareTo(new BigDecimal("999999999.99"))>0) throw new NumberFormatException(); return value;
        } catch(Exception e) {add(issues,row,label+" ต้องเป็นตัวเลขไม่ติดลบและทศนิยมไม่เกิน 2 ตำแหน่ง"); return null;}
    }
    private LocalDate date(String text,String label,SourceRow row,List<Issue> issues,boolean required) {
        if(text.isBlank()) {if(required)add(issues,row,"ไม่ระบุ"+label);return null;}
        try {
            if(text.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(text);
            String[] parts=text.split("/"); if(parts.length!=3) throw new IllegalArgumentException();
            int year=Integer.parseInt(parts[2]); if(year<100)year+=2500; if(year>=2400)year-=543;
            return LocalDate.of(year,Integer.parseInt(parts[1]),Integer.parseInt(parts[0]));
        } catch(Exception e){add(issues,row,label+" ไม่ถูกต้อง ใช้ วว/ดด/ปปปป หรือ YYYY-MM-DD");return null;}
    }
    private void add(List<Issue> issues,SourceRow row,String message) { issues.add(new Issue(row.sheet(),row.row(),row.get("product"),message)); }

    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> commit(byte[] bytes,String fingerprint,int userId) {
        return commit(bytes,fingerprint,userId,List.of());
    }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> commit(byte[] bytes,String fingerprint,int userId,List<Mapping> mappings) {
        // Serialize imports of the identical workbook, across sessions and application instances.
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtext(?))",Object.class,"excel:"+fingerprint);
        String prefix="EX-"+fingerprint.substring(0,32)+"-";
        Integer existing=jdbc.queryForObject("SELECT COUNT(*) FROM t_order WHERE o_order_code LIKE ?",Integer.class,prefix+"%");
        if(existing!=null && existing>0) throw new IllegalArgumentException("ไฟล์นี้เคยนำเข้าแล้ว ไม่สามารถนำเข้าซ้ำได้");
        Report report=mappings.isEmpty()?validate(bytes):validate(bytes,mappings);
        if(!report.valid()) throw new Rejected(report);
        for(int index=0;index<report.items().size();index++) {
            Item item=report.items().get(index);
            Order order=new Order(); order.setOrder_code(prefix+(index+1)); order.setOrder_date(java.sql.Date.valueOf(item.date()));
            order.setCustomer_name(item.customer()); order.setPay_method(item.balance().signum()==0?1:2); order.setPay_type(1);
            order.setPrice_total(item.total().doubleValue()); order.setPrice_pledge(item.balance().signum()==0?0:item.paid().doubleValue());
            order.setPrice_balance(item.balance().doubleValue()); order.setNet(item.total().doubleValue()); order.setRemark(item.remark());order.setActive_status("A");
            orders.save(order);
            OrderDetail detail=new OrderDetail();detail.setOrder(order.getId());detail.setCover(item.coverId());detail.setQty(item.qty());
            detail.setPrice_total(item.total().doubleValue());detail.setPrice_pledge(item.paid().doubleValue());detail.setPrice_balance(item.balance().doubleValue());detail.setOrder_status(item.status());detail.setUser(userId);details.save(detail);
            if(item.paid().signum()>0) {
                Income income=new Income();income.setOrder(order.getId());income.setCreateDate(item.paidDate().toString());income.setCustomerName(item.customer());
                income.setTypeIncome(item.balance().signum()==0?1:2);income.setPrice(item.paid().toPlainString());income.setNote("นำเข้าคำสั่งซื้อ Excel");income.setDelete("A");income.setActiveStatus("A");incomes.save(income);
            }
        }
        return Map.of("orderCount",report.items().size(),"balance",report.items().stream().map(Item::balance).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
    public static class Rejected extends RuntimeException { public final Report report; public Rejected(Report report){super("ข้อมูลเปลี่ยนแปลง กรุณาตรวจไฟล์ใหม่");this.report=report;} }
}
