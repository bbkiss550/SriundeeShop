package com.sriundee.preorder.controller;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.dto.ProductDto;
import com.sriundee.preorder.entity.Product;
import com.sriundee.preorder.repository.ProductRepository;

import org.springframework.ui.Model;

@Controller
public class CoverController {
	
	@Autowired
	private ProductRepository productRepository;

	@GetMapping("/product/cover/{id}")
	@ResponseBody
	public ProductBean getProductData(@PathVariable Integer id) {
		List<ProductBean> productList = productRepository.getDataAllByID(id);
		if (!productList.isEmpty()) {
	        return productList.get(0);
	    }
	    return null;
	}
}
