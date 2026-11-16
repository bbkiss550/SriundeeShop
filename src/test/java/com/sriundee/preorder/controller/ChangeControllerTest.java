package com.sriundee.preorder.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.*;
import com.sriundee.preorder.entity.*;
import com.sriundee.preorder.repository.*;
import com.sriundee.preorder.service.CostCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ChangeControllerTest {
    @Mock OrderDetailRepository details;
    @Mock CostRepository costs;
    @Mock CostDetailRepository costDetails;
    @Mock CostCodeService codes;
    @Mock LotRepository lots;
    @Mock LotDetailRepository lotDetails;
    @Mock JdbcTemplate jdbc;
    @InjectMocks ChangeController controller;

    private OrderDetail current(int status) {
        var detail = new OrderDetail(); detail.setId(10); detail.setOrder_status(status);
        when(details.findAllById(List.of(10))).thenReturn(List.of(detail));
        return detail;
    }
    private Map<String,Object> payload(int status) {
        var p = new HashMap<String,Object>();
        p.put("ids", List.of(10)); p.put("orderStatus",status); p.put("recordDate","2026-11-02");
        return p;
    }

    @Test void threeToFourPreservesExistingLotEvenIfStaleHiddenFieldsWereSubmitted() {
        var detail = current(3); var p=payload(4);
        p.put("l_lot_number","stale");p.put("costPrice","999");p.put("shippingPrice","888");
        assertThat(controller.updateStatus(p).getStatusCode().value()).isEqualTo(200);
        assertThat(detail.getOrder_status()).isEqualTo(4);
        verify(details).saveAll(List.of(detail));
        verifyNoInteractions(costs,costDetails,codes,lots,lotDetails);
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void twoToThreeRequiresLotAndDatesBeforeWritingAnything() {
        var detail = current(2); var p=payload(3);
        assertThat(controller.updateStatus(p).getBody()).isEqualTo("Lot number is required");
        p.put("l_lot_number","LOT-A");
        assertThat(controller.updateStatus(p).getBody()).isEqualTo("Lot expected arrival date range is required");
        p.put("l_start_date","2026-11-10");p.put("l_end_date","2026-11-09");
        assertThat(controller.updateStatus(p).getBody()).isEqualTo("Invalid lot expected arrival date range");
        assertThat(detail.getOrder_status()).isEqualTo(2);
        verify(details,never()).saveAll(any());verifyNoInteractions(lots,lotDetails,costs);
    }
    @Test void twoToThreeCreatesLotAndLinksSelectedOrder() {
        var detail=current(2);var p=payload(3);
        p.put("l_lot_number","LOT-A");p.put("l_start_date","2026-11-10");p.put("l_end_date","2026-11-15");
        doAnswer(call->{((Lot)call.getArgument(0)).setId(20);return call.getArgument(0);}).when(lots).save(any());
        assertThat(controller.updateStatus(p).getStatusCode().value()).isEqualTo(200);
        assertThat(detail.getOrder_status()).isEqualTo(3);
        verify(lotDetails).saveAll(argThat(rows -> {
            var row=rows.iterator().next();return row.getLot()==20 && row.getOrder_detail()==10;
        }));
        verifyNoInteractions(costs,costDetails,codes);
    }
    @ParameterizedTest @CsvSource({"1,2,costPrice,1", "4,5,shippingPrice,2", "5,6,postalPrice,5"})
    void existingCostsMoveToCorrectTransitions(int from,int to,String field,int costType) {
        var detail=current(from);var p=payload(to);p.put(field,"125");p.put("l_arrive_date","2026-11-02");
        assertThat(controller.updateStatus(p).getStatusCode().value()).isEqualTo(200);
        assertThat(detail.getOrder_status()).isEqualTo(to);
        verify(costs).save(argThat(cost -> cost.getType_cost()==costType && "125".equals(cost.getPrice())));
        verifyNoInteractions(lots,lotDetails);
        if(from==4) verify(jdbc).update(contains("UPDATE t_lot"),any(Object[].class));
    }
    @ParameterizedTest @CsvSource({"1,3", "2,4", "3,5", "4,6", "5,4", "6,5", "0,1"})
    void rejectSkippingBackwardsCancelledOrAlreadySent(int from,int to) {
        var detail=current(from);
        assertThat(controller.updateStatus(payload(to)).getStatusCode().value()).isEqualTo(400);
        assertThat(detail.getOrder_status()).isEqualTo(from);
        verify(details,never()).saveAll(any());verifyNoInteractions(costs,lots,lotDetails);
    }
    @Test void rejectMixedStatusesBeforeWriting() {
        var first=new OrderDetail();first.setId(10);first.setOrder_status(2);
        var second=new OrderDetail();second.setId(11);second.setOrder_status(3);
        var p=payload(3);p.put("ids",List.of(10,11));
        when(details.findAllById(List.of(10,11))).thenReturn(List.of(first,second));
        assertThat(controller.updateStatus(p).getStatusCode().value()).isEqualTo(400);
        verify(details,never()).saveAll(any());
    }
    @ParameterizedTest @CsvSource({"1,2", "4,5", "5,6"})
    void shiftedCostStagesStillRequireAmounts(int from,int to) {
        var detail=current(from);
        assertThat(controller.updateStatus(payload(to)).getStatusCode().value()).isEqualTo(400);
        assertThat(detail.getOrder_status()).isEqualTo(from);
        verify(details,never()).saveAll(any());verifyNoInteractions(costs,lots,lotDetails);
    }
}
