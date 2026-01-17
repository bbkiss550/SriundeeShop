package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sriundee.preorder.entity.OrderStatus;
import com.sriundee.preorder.repository.OrderStatusRepository;

@Controller
public class OrderStatusController {

	@Autowired
	private OrderStatusRepository orderStatusRepository;
	
    public String getDataList() {
	    List<OrderStatus> mainOrderStatus = orderStatusRepository.getDataAll();
	    StringBuilder strOrderStatus = new StringBuilder();
	    for (OrderStatus os : mainOrderStatus) {
	    	strOrderStatus.append("<option value='" + os.getId() + "'>" + os.getName() + "</option>");
	    }
	    
	    return strOrderStatus.toString();
    }
}