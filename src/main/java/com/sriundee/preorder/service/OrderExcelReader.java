package com.sriundee.preorder.service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Reads supplied workbook bytes only; formulas are never executed. */
@Component
public class OrderExcelReader {
    public record Issue(String sheet, int row, String source, String message) {}
    public record SourceRow(String sheet, int sheetIndex, int row, Map<String,String> cells) {
        public String get(String key) { return cells.getOrDefault(key, ""); }
    }
    public record WorkbookRows(List<SourceRow> rows, List<Issue> issues) {}
    public record Column(int index,String header,String field,String sampleType) {}
    public record Layout(int sheetIndex,String sheet,int headerRow,List<Column> columns) {}
    public record Mapping(int sheetIndex,int headerRow,Map<Integer,String> columns) {}
    public static final Map<String,String> FIELDS;
    private static final Map<String,String> HEADERS = new HashMap<>();
    static {
        aliases("customer", "ลูกค้า", "ชื่อลูกค้า", "username", "Twitter/X Username หรือ Line ID", "Twitter/X Username หรือ Line ID ที่มาของลูกค้า");
        aliases("channel", "ช่องทาง", "ที่มาของลูกค้า");
        aliases("product", "สินค้า", "ชื่อสินค้า", "รายการสินค้า");
        aliases("website", "เว็บ", "เว็บไซต์", "website");
        aliases("version", "เวอ", "เวอร์ชัน", "เวอร์ชั่น", "version");
        aliases("cover", "ปก", "ชื่อปก"); aliases("qty", "จำนวน", "quantity");
        aliases("total", "ราคาเต็มรวม", "ยอดรวมสินค้า");
        aliases("paid", "จ่ายแล้ว", "ยอดชำระแล้ว"); aliases("balance", "ยอดคงเหลือ", "ยอดค้าง", "คงเหลือ");
        aliases("payment", "รูปแบบการชำระ", "รูปแบบชำระ", "การชำระเงิน");
        aliases("date", "วันที่สั่งซื้อ", "วันที่กดของ"); aliases("paidDate", "วันที่ชำระ", "วันที่ชำระเงิน");
        aliases("status", "สถานะ", "สถานะสั่งซื้อ"); aliases("remark", "หมายเหตุ");
        aliases("shippingCost", "ค่าส่ง", "ค่าจัดส่ง"); aliases("discount", "ส่วนลด");
        Map<String,String> fields=new LinkedHashMap<>();
        for(String[] pair:new String[][]{{"customer","ชื่อลูกค้า"},{"channel","ช่องทาง"},{"product","สินค้า"},{"website","เว็บ"},{"version","เวอร์ชัน"},{"cover","ปก"},{"qty","จำนวน"},{"total","ราคาเต็มรวม"},{"paid","ยอดชำระแล้ว"},{"balance","ยอดคงเหลือ"},{"payment","รูปแบบชำระ"},{"date","วันที่สั่งซื้อ"},{"paidDate","วันที่ชำระ"},{"status","สถานะ"},{"remark","หมายเหตุ"},{"shippingCost","ค่าส่ง"},{"discount","ส่วนลด"}})fields.put(pair[0],pair[1]);
        FIELDS=Collections.unmodifiableMap(fields);
    }
    private static void aliases(String key, String... names) { for (String name:names) HEADERS.put(normalize(name),key); }
    public static String normalize(String value) { return value == null ? "" : value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
    public WorkbookRows read(byte[] bytes) {
        return read(bytes,List.of());
    }
    public WorkbookRows read(byte[] bytes,List<Mapping> approved) {
        if (bytes == null || bytes.length == 0 || bytes.length > 8*1024*1024) throw new IllegalArgumentException("ไฟล์ต้องมีข้อมูลและขนาดไม่เกิน 8 MB");
        List<SourceRow> rows = new ArrayList<>(); List<Issue> issues = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.US); formatter.setUseCachedValuesForFormulaCells(true);
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (book.getNumberOfSheets() > 20) throw new IllegalArgumentException("รองรับไม่เกิน 20 ชีตต่อไฟล์");
            Map<Integer,Mapping> overrides=new HashMap<>();
            for(Mapping mapping:approved) {
                if(mapping==null || mapping.sheetIndex()<0 || mapping.sheetIndex()>=book.getNumberOfSheets() || mapping.headerRow()<1 || mapping.headerRow()>50_001 || mapping.columns()==null || mapping.columns().size()>101 || overrides.put(mapping.sheetIndex(),mapping)!=null)throw new IllegalArgumentException("การจับคู่ชีตหรือหัวตารางไม่ถูกต้อง");
                Set<String> used=new HashSet<>();for(var entry:mapping.columns().entrySet()) {
                    if(entry.getKey()<0 || entry.getKey()>100 || !FIELDS.containsKey(entry.getValue()) || !used.add(entry.getValue()))throw new IllegalArgumentException("ฟิลด์ซ้ำหรือการจับคู่คอลัมน์ไม่ถูกต้อง");
                }
                if(!used.containsAll(List.of("customer","product")))throw new IllegalArgumentException("ทุกชีตต้องจับคู่ชื่อลูกค้าและสินค้า");
            }
            int visited = 0;
            for (int si=0;si<book.getNumberOfSheets();si++) {
                Sheet sheet=book.getSheetAt(si); Map<Integer,String> columns=new LinkedHashMap<>();
                Mapping override=overrides.get(si);
                if (sheet.getLastRowNum()>50_000) throw new IllegalArgumentException("ชีตมีเลขแถวเกิน 50,000 กรุณาแยกไฟล์");
                for (Row row:sheet) {
                    if (++visited>50_000) throw new IllegalArgumentException("รองรับไม่เกิน 50,000 แถวต่อไฟล์");
                    Map<Integer,String> candidate=new LinkedHashMap<>();
                    for (Cell cell:row) {
                        if (cell.getColumnIndex()>100) throw new IllegalArgumentException("รองรับไม่เกิน 101 คอลัมน์ต่อชีต");
                        String key=HEADERS.get(normalize(formatter.formatCellValue(cell)));
                        if(key!=null) candidate.put(cell.getColumnIndex(),key);
                    }
                    if(override!=null && row.getRowNum()+1==override.headerRow()){columns=new LinkedHashMap<>(override.columns());continue;}
                    if(override!=null && row.getRowNum()+1<override.headerRow())continue;
                    if(override!=null && !columns.isEmpty()) {
                        boolean repeated=true;Row header=sheet.getRow(override.headerRow()-1);
                        for(int col:columns.keySet())if(header==null || !normalize(formatter.formatCellValue(row.getCell(col))).equals(normalize(formatter.formatCellValue(header.getCell(col))))){repeated=false;break;}
                        if(repeated)continue;
                    }
                    if(override==null && candidate.containsValue("customer") && candidate.containsValue("product")) {
                        if(new HashSet<>(candidate.values()).size()!=candidate.size()) issues.add(new Issue(sheet.getSheetName(),row.getRowNum()+1,"","พบหัวคอลัมน์ซ้ำ กรุณาให้แต่ละช่องข้อมูลมีคอลัมน์เดียว"));
                        columns=candidate; continue;
                    }
                    if(columns.isEmpty()) continue;
                    Map<String,String> values=new LinkedHashMap<>();
                    for(var entry:columns.entrySet()) values.put(entry.getValue(),value(sheet,row.getRowNum(),entry.getKey(),entry.getValue(),formatter));
                    // Ignore empty separator rows, including status dropdown placeholders.
                    if(values.getOrDefault("customer","").isBlank() && values.getOrDefault("product","").isBlank()
                            && values.getOrDefault("website","").isBlank()) continue;
                    // A merged section title has a single real value; it is not a customer order.
                    long filled=0; for(Cell cell:row) if(!formatter.formatCellValue(cell).isBlank()) filled++;
                    final int rowIndex=row.getRowNum();
                    boolean section=sheet.getMergedRegions().stream().anyMatch(m->m.getFirstRow()==rowIndex && m.getLastColumn()-m.getFirstColumn()>=2);
                    Cell first=row.getCell(row.getFirstCellNum());
                    boolean colored=first!=null && first.getCellStyle().getFillPattern()==FillPatternType.SOLID_FOREGROUND;
                    if(filled==1 && (section || colored)) continue;
                    for(var entry:columns.entrySet()) if(Set.of("total","paid","balance").contains(entry.getValue())) {
                        for(var merged:sheet.getMergedRegions()) if(merged.isInRange(rowIndex,entry.getKey()) && merged.getLastRow()>merged.getFirstRow()
                                && !values.getOrDefault(entry.getValue(),"").isBlank()) {
                            issues.add(new Issue(sheet.getSheetName(),rowIndex+1,values.getOrDefault("product",""),"ยอดเงินใช้เซลล์รวมหลายแถว กรุณาแยกยอดของแต่ละคำสั่งซื้อก่อน"));
                        }
                    }
                    for(Cell cell:row) {
                        String text=formatter.formatCellValue(cell);
                        if(!text.isBlank() && !columns.containsKey(cell.getColumnIndex())) values.put("คอลัมน์ "+(cell.getColumnIndex()+1),text);
                    }
                    rows.add(new SourceRow(sheet.getSheetName(),si,row.getRowNum()+1,Map.copyOf(values)));
                    if(rows.size()>10_000) throw new IllegalArgumentException("รองรับคำสั่งซื้อไม่เกิน 10,000 แถวต่อไฟล์");
                }
                if(columns.isEmpty() && sheet.getPhysicalNumberOfRows()>0) issues.add(new Issue(sheet.getSheetName(),sheet.getFirstRowNum()+1,"", "ไม่พบหัวคอลัมน์ชื่อลูกค้าและรายการสินค้า"));
            }
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("อ่านไฟล์ .xlsx ไม่สำเร็จ ไฟล์อาจเสียหาย มีรหัสผ่าน หรือไม่ใช่ Excel ที่รองรับ"); }
        if(rows.isEmpty()) issues.add(new Issue("",0,"","ไม่พบแถวคำสั่งซื้อในไฟล์"));
        return new WorkbookRows(List.copyOf(rows),List.copyOf(issues));
    }
    /** Only synthetic value shapes leave this reader; actual row values remain local. */
    public List<Layout> inspect(byte[] bytes) {
        if(bytes==null || bytes.length==0 || bytes.length>8*1024*1024)throw new IllegalArgumentException("ไฟล์ต้องมีข้อมูลและขนาดไม่เกิน 8 MB");
        List<Layout> result=new ArrayList<>();DataFormatter formatter=new DataFormatter(Locale.US);formatter.setUseCachedValuesForFormulaCells(true);
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if(book.getNumberOfSheets()>20)throw new IllegalArgumentException("รองรับไม่เกิน 20 ชีตต่อไฟล์");
            for(int si=0;si<book.getNumberOfSheets();si++) {
                Sheet sheet=book.getSheetAt(si);if(sheet.getPhysicalNumberOfRows()==0)continue;
                if(sheet.getLastRowNum()>50_000)throw new IllegalArgumentException("ชีตมีเลขแถวเกิน 50,000 กรุณาแยกไฟล์");
                Row header=null;int best=-1;
                for(Row row:sheet) {
                    if(row.getRowNum()>50)break;
                    int filled=0,known=0;boolean customer=false,product=false;
                    for(Cell cell:row){String label=formatter.formatCellValue(cell).strip();if(!label.isEmpty())filled++;String field=HEADERS.get(normalize(label));if(field!=null){known++;customer|=field.equals("customer");product|=field.equals("product");}}
                    if(filled<2)continue;int score=(customer&&product?1000:0)+known*10;
                    if(score>best){header=row;best=score;}
                    if(customer&&product)break;
                }
                if(header==null)header=sheet.getRow(sheet.getFirstRowNum());
                if(header==null)continue;List<Column> columns=new ArrayList<>();
                if(header.getLastCellNum()>101)throw new IllegalArgumentException("รองรับไม่เกิน 101 คอลัมน์ต่อชีต");
                for(int col=0;col<header.getLastCellNum();col++) {
                    String label=formatter.formatCellValue(header.getCell(col)).strip();if(label.length()>255)label=label.substring(0,255);
                    Set<String> types=new LinkedHashSet<>();int count=0;
                    for(int row=header.getRowNum()+1;row<=Math.min(sheet.getLastRowNum(),header.getRowNum()+8);row++) {
                        Row data=sheet.getRow(row);Cell cell=data==null?null:data.getCell(col);if(cell==null||cell.getCellType()==CellType.BLANK)continue;
                        String shape;
                        if(cell.getCellType()==CellType.NUMERIC)shape=DateUtil.isCellDateFormatted(cell)?"DATE":"NUMBER";
                        else {String text=formatter.formatCellValue(cell).strip();shape=text.matches("(?:\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{2,4})")?"DATE":text.matches("-?\\d+(?:,\\d{3})*(?:\\.\\d+)?")?"NUMBER":"TEXT";}
                        types.add(shape);if(++count==3)break;
                    }
                    String type=types.isEmpty()?"EMPTY":types.size()==1?types.iterator().next():"MIXED";
                    columns.add(new Column(col,label,HEADERS.getOrDefault(normalize(label),""),type));
                }
                result.add(new Layout(si,sheet.getSheetName(),header.getRowNum()+1,List.copyOf(columns)));
            }
        }catch(IllegalArgumentException failure){throw failure;}catch(Exception failure){throw new IllegalArgumentException("อ่านโครงสร้าง Excel ไม่สำเร็จ กรุณาตรวจไฟล์ .xlsx");}
        if(result.isEmpty())throw new IllegalArgumentException("ไม่พบชีตที่มีข้อมูล");return List.copyOf(result);
    }
    private String value(Sheet sheet,int row,int col,String key,DataFormatter formatter) {
        for(var merged:sheet.getMergedRegions()) if(merged.isInRange(row,col)) { row=merged.getFirstRow(); col=merged.getFirstColumn(); break; }
        Row r=sheet.getRow(row); Cell cell=r==null?null:r.getCell(col);
        if(cell==null) return "";
        boolean numeric=cell.getCellType()==CellType.NUMERIC || cell.getCellType()==CellType.FORMULA && cell.getCachedFormulaResultType()==CellType.NUMERIC;
        if(numeric && DateUtil.isCellDateFormatted(cell)) {
            try { return cell.getLocalDateTimeCellValue().toLocalDate().toString(); }
            catch(RuntimeException invalidDate) { return "วันที่ในเซลล์ไม่ถูกต้อง"; }
        }
        if(numeric && Set.of("total","paid","balance","qty","shippingCost","discount").contains(key)) return java.math.BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        return formatter.formatCellValue(cell).strip();
    }
}
