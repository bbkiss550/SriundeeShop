package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.dto.ArtistDto;
import com.sriundee.preorder.entity.Artist;
import com.sriundee.preorder.entity.Group;
import com.sriundee.preorder.entity.OrderStatus;
import com.sriundee.preorder.repository.ArtistRepository;
import com.sriundee.preorder.repository.GroupRepository;
import com.sriundee.preorder.repository.OrderStatusRepository;

import org.springframework.ui.Model;

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