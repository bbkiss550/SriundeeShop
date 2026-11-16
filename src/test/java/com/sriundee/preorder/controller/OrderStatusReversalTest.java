package com.sriundee.preorder.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.*;
import com.sriundee.preorder.bean.CostDetailBean;
import com.sriundee.preorder.entity.*;
import com.sriundee.preorder.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrderStatusReversalTest {
    @Test void cancellingLotReturnsToWaitingWebsite() {
        var controller=new LotController();var lots=mock(LotRepository.class);
        var links=mock(LotDetailRepository.class);var details=mock(OrderDetailRepository.class);
        ReflectionTestUtils.setField(controller,"lotRepository",lots);
        ReflectionTestUtils.setField(controller,"lotDetailRepository",links);
        ReflectionTestUtils.setField(controller,"orderDetailRepository",details);
        var lot=new Lot();lot.setId(20);lot.setDelete("A");
        var link=new LotDetail();link.setLot(20);link.setOrder_detail(10);
        var detail=new OrderDetail();detail.setId(10);detail.setOrder_status(3);
        when(lots.findById(20)).thenReturn(Optional.of(lot));when(links.findByLot(20)).thenReturn(List.of(link));
        when(details.findAllById(List.of(10))).thenReturn(List.of(detail));
        assertThat(controller.cancelLot(20).getStatusCode().value()).isEqualTo(200);
        assertThat(detail.getOrder_status()).isEqualTo(2);
    }
    @Test void removingLotDetailReturnsToWaitingWebsite() {
        var controller=new LotController();var lots=mock(LotRepository.class);
        var links=mock(LotDetailRepository.class);var details=mock(OrderDetailRepository.class);
        ReflectionTestUtils.setField(controller,"lotRepository",lots);
        ReflectionTestUtils.setField(controller,"lotDetailRepository",links);
        ReflectionTestUtils.setField(controller,"orderDetailRepository",details);
        var link=new LotDetail();link.setLot(20);link.setOrder_detail(10);
        var detail=new OrderDetail();detail.setId(10);detail.setOrder_status(3);
        when(links.findById(30)).thenReturn(Optional.of(link));when(links.countByLot(20)).thenReturn(1);
        when(details.findById(10)).thenReturn(Optional.of(detail));
        assertThat(controller.deleteLotDetail(30).getStatusCode().value()).isEqualTo(200);
        assertThat(detail.getOrder_status()).isEqualTo(2);
    }
    @Test void cancellingShippingCostChecksStageFiveAndReturnsToFour() {
        var controller=new CostShippingController();var costs=mock(CostRepository.class);
        var links=mock(CostDetailRepository.class);var details=mock(OrderDetailRepository.class);
        ReflectionTestUtils.setField(controller,"costRepository",costs);
        ReflectionTestUtils.setField(controller,"costDetailRepository",links);
        ReflectionTestUtils.setField(controller,"orderDetailRepository",details);
        var cost=new Cost();cost.setId(20);cost.setDelete("A");
        var link=mock(CostDetailBean.class);when(link.getID_order_detail()).thenReturn(10);
        var detail=new OrderDetail();detail.setId(10);detail.setOrder_status(5);
        when(costs.findById(20)).thenReturn(Optional.of(cost));when(links.countDataByCost(20)).thenReturn(1);
        when(links.countNotStatusByCost(20,5)).thenReturn(0);when(links.getDataByCost(20)).thenReturn(List.of(link));
        when(details.findAllById(List.of(10))).thenReturn(List.of(detail));
        assertThat(controller.cancel(20).getStatusCode().value()).isEqualTo(200);
        verify(links).countNotStatusByCost(20,5);
        assertThat(detail.getOrder_status()).isEqualTo(4);
    }
}
