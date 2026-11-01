package com.sriundee.preorder.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sriundee.preorder.entity.*;
import com.sriundee.preorder.repository.*;
import com.sriundee.preorder.service.OrderExcelReader.*;
import com.sriundee.preorder.service.OrderExcelImportService.*;

class OrderExcelImportTest {
    final OrderExcelReader reader=new OrderExcelReader();
    final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    final OrderRepository orders=mock(OrderRepository.class);
    final OrderDetailRepository details=mock(OrderDetailRepository.class);
    final IncomeRepository incomes=mock(IncomeRepository.class);
    final OrderExcelImportService service=spy(new OrderExcelImportService(reader,jdbc,orders,details,incomes));
    final List<Catalog> catalog=List.of(new Catalog(8,"Album","Weverse Shop","PHOTOBOOK VER.","เดี่ยว"));
    static final String[] HEADERS={"ชื่อลูกค้า","ช่องทาง","สินค้า","เว็บ","เวอร์ชัน","ปก","จำนวน","ราคาเต็มรวม","ยอดชำระแล้ว","ยอดคงเหลือ","วันที่สั่งซื้อ","วันที่ชำระ","สถานะ","หมายเหตุ"};
    String[] valid(){return new String[]{"@customer","X","Album","Weverse Shop","PHOTOBOOK VER.","เดี่ยว","1","590","300","290","29/10/69","30/10/2569","รอเว็บส่งของ","EMS"};}
    byte[] workbook(String[]... data) throws Exception {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("Orders");var head=sheet.createRow(0);
            for(int i=0;i<HEADERS.length;i++)head.createCell(i).setCellValue(HEADERS[i]);
            for(int r=0;r<data.length;r++){var row=sheet.createRow(r+2);for(int c=0;c<data[r].length;c++)row.createCell(c).setCellValue(data[r][c]);}
            book.write(out);return out.toByteArray();
        }
    }
    Report validate(byte[] bytes){return service.validate(reader.read(bytes),catalog,Map.of("รอเว็บส่งของ",3));}
    void prepare(){doReturn(catalog).when(service).catalog();doReturn(Map.of("รอเว็บส่งของ",3)).when(service).statuses();}
    @Test void readsActualRowsAndMaintainsOriginalMoneyAndThaiDates() throws Exception {
        var report=validate(workbook(valid()));assertThat(report.valid()).isTrue();
        var item=report.items().getFirst();assertThat(item.row()).isEqualTo(3);
        assertThat(item.date().toString()).isEqualTo("2026-10-29");assertThat(item.paidDate().toString()).isEqualTo("2026-10-30");
        assertThat(item.total()).isEqualByComparingTo("590");assertThat(item.paid()).isEqualByComparingTo("300");assertThat(item.balance()).isEqualByComparingTo("290");
        verifyNoInteractions(orders,details,incomes);
        Path dir=Path.of(".runtime/product-setup-qa");Files.createDirectories(dir);
        Files.write(dir.resolve("orders-valid.xlsx"),workbook(valid()));
        new ObjectMapper().findAndRegisterModules().writeValue(dir.resolve("orders-valid-report.json").toFile(),report);
    }
    @Test void collectsEveryIssueFromEveryRowBeforeRejecting() throws Exception {
        var first=valid();first[2]="Missing album";first[6]="0";first[7]="";
        var second=valid();second[3]="Wrong shop";second[12]="Unknown status";
        var third=valid();third[5]="Missing cover";third[9]="900";
        var report=validate(workbook(first,second,third));assertThat(report.valid()).isFalse();
        assertThat(report.checkedRows()).isEqualTo(3);assertThat(report.problemRows()).isEqualTo(3);
        assertThat(report.issues()).extracting(Issue::row).contains(3,4,5);
        assertThat(report.issues()).hasSizeGreaterThanOrEqualTo(6);verifyNoInteractions(orders,details,incomes);
        Files.createDirectories(Path.of(".runtime/product-setup-qa"));
        new ObjectMapper().findAndRegisterModules().writeValue(Path.of(".runtime/product-setup-qa/orders-invalid-report.json").toFile(),report);
    }
    @Test void doesNotSubstituteCatalogPriceWhenHistoricalTotalMissing() throws Exception {
        var row=valid();row[7]="";row[8]="";
        var report=validate(workbook(row));assertThat(report.valid()).isFalse();assertThat(report.issues()).anySatisfy(i->assertThat(i.message()).contains("ราคาเต็มรวม"));
    }
    @Test void derivesPaidAmountOnlyFromExplicitHistoricalTotalAndBalance() throws Exception {
        var row=valid();row[8]="";var report=validate(workbook(row));assertThat(report.valid()).isTrue();assertThat(report.items().getFirst().paid()).isEqualByComparingTo("300");
    }
    @Test void rejectsAmbiguousLegacyMultiItemsInsteadOfDroppingAnItem() throws Exception {
        var row=valid();row[2]="Album\n1. PHOTOBOOK VER. 1\n2. Other VER. 1";row[4]="";row[5]="";
        assertThat(validate(workbook(row)).issues()).anySatisfy(i->assertThat(i.message()).contains("หลายรายการ"));
    }
    @Test void readsHiddenSheetsTooAndKeepsMergedCustomerValues() throws Exception {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            for(int si=0;si<2;si++) {
                var sheet=book.createSheet("Sheet"+si);var head=sheet.createRow(0);for(int c=0;c<HEADERS.length;c++)head.createCell(c).setCellValue(HEADERS[c]);
                for(int n=1;n<=2;n++){var row=sheet.createRow(n);var data=valid();for(int c=0;c<data.length;c++)if(c!=0 || n==1)row.createCell(c).setCellValue(data[c]);}
                sheet.addMergedRegion(new CellRangeAddress(1,2,0,0));
            }
            book.setSheetHidden(1,true);book.write(out);var report=validate(out.toByteArray());assertThat(report.valid()).isTrue();assertThat(report.checkedRows()).isEqualTo(4);
        }
    }
    @Test void rejectsSharedMergedMoneyForEachAffectedRow() throws Exception {
        try(var book=new XSSFWorkbook(new java.io.ByteArrayInputStream(workbook(valid(),valid())));var out=new ByteArrayOutputStream()) {
            book.getSheetAt(0).addMergedRegion(new CellRangeAddress(2,3,9,9));book.write(out);
            var report=validate(out.toByteArray());assertThat(report.valid()).isFalse();assertThat(report.issues()).filteredOn(i->i.message().contains("เซลล์รวม")).extracting(Issue::row).containsExactly(3,4);
        }
    }
    @Test void corruptAndEmptyFilesCannotPass() {
        assertThatThrownBy(()->reader.read(new byte[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->reader.read("fake xlsx".getBytes())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void numericCurrencyFormattingDoesNotChangeActualAmounts() throws Exception {
        try(var book=new XSSFWorkbook(new java.io.ByteArrayInputStream(workbook(valid())));var out=new ByteArrayOutputStream()) {
            var cell=book.getSheetAt(0).getRow(2).getCell(7);cell.setCellValue(590);
            var style=book.createCellStyle();style.setDataFormat(book.createDataFormat().getFormat("\"฿\"#,##0.00"));cell.setCellStyle(style);
            book.write(out);var report=validate(out.toByteArray());assertThat(report.valid()).isTrue();assertThat(report.items().getFirst().total()).isEqualByComparingTo("590");
        }
    }
    @Test void missingHeadersAreReportedForEachSheet() throws Exception {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()){
            book.createSheet("One").createRow(3).createCell(0).setCellValue("Unknown");book.createSheet("Two").createRow(8).createCell(0).setCellValue("Unknown");book.write(out);
            assertThat(reader.read(out.toByteArray()).issues()).extracting(Issue::sheet).contains("One","Two");
        }
    }
    @Test void commitRevalidatesWholeFileAndWritesNothingOnAnyProblem() throws Exception {
        prepare();var row=valid();row[5]="Missing";
        assertThatThrownBy(()->service.commit(workbook(valid(),row),"a".repeat(64),1)).isInstanceOf(Rejected.class);
        verifyNoInteractions(orders,details,incomes);
    }
    @Test void identicalWorkbookCannotBeCommittedTwice() throws Exception {
        when(jdbc.queryForObject("SELECT COUNT(*) FROM t_order WHERE o_order_code LIKE ?",Integer.class,"EX-"+"a".repeat(32)+"-%")).thenReturn(1);
        assertThatThrownBy(()->service.commit(workbook(valid()),"a".repeat(64),1)).hasMessageContaining("เคยนำเข้า");verifyNoInteractions(orders,details,incomes);
    }
    @Test void createsSeparateOrdersForSameCustomerAndHistoricalIncomeDate() throws Exception {
        prepare();when(orders.save(any())).thenAnswer(call->{Order order=call.getArgument(0);order.setId(100);return order;});
        var result=service.commit(workbook(valid(),valid()),"a".repeat(64),7);assertThat(result.get("orderCount")).isEqualTo(2);
        verify(orders,times(2)).save(any());verify(details,times(2)).save(argThat(d->d.getCover()==8 && d.getUser()==7 && d.getPrice_total()==590 && d.getPrice_balance()==290));
        verify(incomes,times(2)).save(argThat(i->i.getCreateDate().equals("2026-10-30") && i.getPrice().equals("300.00")));
    }
    @Test void transactionRollsBackIfAnyWriteFails() throws Exception {
        prepare();when(details.save(any())).thenThrow(new IllegalStateException("simulated database failure"));
        PlatformTransactionManager manager=mock(PlatformTransactionManager.class);TransactionStatus status=mock(TransactionStatus.class);
        when(manager.getTransaction(any())).thenReturn(status);
        ProxyFactory proxy=new ProxyFactory(service);proxy.setProxyTargetClass(true);proxy.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        var transactional=(OrderExcelImportService)proxy.getProxy();
        assertThatThrownBy(()->transactional.commit(workbook(valid()),"a".repeat(64),1)).hasMessageContaining("database failure");
        verify(manager).rollback(status);verify(manager,never()).commit(any());
    }
    @Test void approvedMappingKeepsEveryExistingRowValidationAndCommitUsesTheSameMapping() throws Exception {
        prepare();byte[] bytes=workbook(valid());var layout=reader.inspect(bytes).getFirst();Map<Integer,String> fields=new LinkedHashMap<>();
        for(var column:layout.columns())if(!column.field().isBlank())fields.put(column.index(),column.field());
        var mapping=List.of(new Mapping(0,1,fields));assertThat(service.validate(bytes,mapping).valid()).isTrue();
        service.commit(bytes,"a".repeat(64),7,mapping);verify(details).save(argThat(detail->detail.getCover()==8 && detail.getPrice_total()==590));
        var wrong=new LinkedHashMap<>(fields);wrong.put(7,"paid");wrong.put(8,"total");
        var invalid=service.validate(bytes,List.of(new Mapping(0,1,wrong)));assertThat(invalid.valid()).isFalse();assertThat(invalid.issues()).anySatisfy(issue->assertThat(issue.message()).contains("ราคาเต็มรวม"));
    }
}
