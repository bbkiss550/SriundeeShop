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
    
    public String getDataCheckList() {
	    List<OrderStatus> mainOrderStatus = orderStatusRepository.getDataAll();
	    StringBuilder strOrderStatus = new StringBuilder();
    	strOrderStatus.append("<div class='col-2'>");
    	strOrderStatus.append("<input type='radio' class='btn-check' name='group-os' id='osAll' value='' onchange='load_change_data()' checked>");
    	strOrderStatus.append("<label class='btn btn-outline-primary btn-status-grid w-100' for='osAll'>ทั้งหมด</label>");
    	strOrderStatus.append("</div>");
	    for (OrderStatus os : mainOrderStatus) {
	    	strOrderStatus.append("<div class='col-2'>");
	    	strOrderStatus.append("<input type='radio' class='btn-check' name='group-os' id='os" + os.getId() + "' value='" + os.getId() + "' onchange='load_change_data()'>");
	    	strOrderStatus.append("<label class='btn btn-outline-" + os.getColor() + " btn-status-grid w-100' for='os" + os.getId() + "'>" + os.getName() + "</label>");
	    	strOrderStatus.append("</div>");
	    }
	    
	    return strOrderStatus.toString();
    }
}
