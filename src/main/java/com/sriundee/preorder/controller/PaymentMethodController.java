package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sriundee.preorder.entity.PaymentMethod;
import com.sriundee.preorder.repository.PaymentMethodRepository;

@Controller
public class PaymentMethodController {

	@Autowired
	private PaymentMethodRepository paymentMethodRepository;
	
    public String getDataList() {
	    List<PaymentMethod> mainPaymentMethod = paymentMethodRepository.getDataAll();
	    StringBuilder strPaymentMethod = new StringBuilder();
	    for (PaymentMethod pm : mainPaymentMethod) {
	    	strPaymentMethod.append("<option value='" + pm.getId() + "'>" + pm.getName() + "</option>");
	    }
	    
	    return strPaymentMethod.toString();
    }
}