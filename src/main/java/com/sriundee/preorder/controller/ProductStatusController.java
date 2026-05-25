package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sriundee.preorder.entity.ProductStatus;
import com.sriundee.preorder.repository.ProductStatusRepository;

@Controller
public class ProductStatusController {

	@Autowired
	private ProductStatusRepository productstatusRepository;
	
    public String getDataList() {
	    List<ProductStatus> mainProductstatus = productstatusRepository.getDataAll();
	    StringBuilder strProductstatus = new StringBuilder();
	    for (ProductStatus pt : mainProductstatus) {
	    	strProductstatus.append("<option value='" + pt.getId() + "'>" + pt.getName() + "</option>");
	    }
	    
	    return strProductstatus.toString();
    }
}