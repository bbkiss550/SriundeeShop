package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.repository.ProductRepository;

import org.springframework.ui.Model;

@Controller
public class OrderController {

	@Autowired
    private MenuController menuService;

	@Autowired
	private ProductRepository productRepository;

    @GetMapping("/order")
    public String index(Model model) {
		String menuList = menuService.getMenuList(7,null);
	    model.addAttribute("mainMenus", menuList);

		List<ProductBean> productList = productRepository.getDataAll();
		StringBuilder strProduct = new StringBuilder();
		Integer row_id = 0;
		for (ProductBean p : productList) {
			row_id +=1;
			strProduct.append("<tr>");
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-warning' onclick='add_data(" + p.getID_product() + ")'><i data-feather='edit'></i></a></div></td>");
			strProduct.append("<td>" + row_id + "</td>");
			strProduct.append("<td><img src='" + p.geta_logo() + "' class='table-img'></td>");
			strProduct.append("<td>" + p.getp_name() + "</td>");
			strProduct.append("<td>" + p.gett_name() + "</td>");
			strProduct.append("<td>" + p.geta_name() + "</td>");
			strProduct.append("<td>" + p.getp_end_date() + "</td>");
			strProduct.append("<td>" + p.getp_send_date() + "</td>");
			strProduct.append("<td>" + p.getp_second_pay_date() + "</td>");
			strProduct.append("<td>" + p.getp_last_pay_date() + "</td>");
			strProduct.append("</tr>");
		}
	    model.addAttribute("mainProduct", strProduct);
	    
        return "order";
    }
}