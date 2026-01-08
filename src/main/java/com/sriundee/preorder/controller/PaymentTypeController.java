package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sriundee.preorder.entity.PaymentType;
import com.sriundee.preorder.repository.PaymentTypeRepository;

@Controller
public class PaymentTypeController {

	@Autowired
	private PaymentTypeRepository paymenttypeRepository;
	
    public String getDataList() {
	    List<PaymentType> mainPaymentType = paymenttypeRepository.getDataAll();
	    StringBuilder strGroup = new StringBuilder();
	    for (PaymentType pt : mainPaymentType) {
	    	strGroup.append("<option value='" + pt.getId() + "'>" + pt.getName() + "</option>");
	    }
	    
	    return strGroup.toString();
    }
}