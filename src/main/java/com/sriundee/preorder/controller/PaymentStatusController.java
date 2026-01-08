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
import com.sriundee.preorder.entity.PaymentType;
import com.sriundee.preorder.repository.ArtistRepository;
import com.sriundee.preorder.repository.GroupRepository;
import com.sriundee.preorder.repository.PaymentTypeRepository;

import org.springframework.ui.Model;

@Controller
public class PaymentStatusController {

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