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

		List<ProductBean> productList = productRepository.getDataOrder();
		StringBuilder strProduct = new StringBuilder();
		Integer row_id = 0;
		for (ProductBean p : productList) {
			row_id +=1;
			strProduct.append("<tr>");
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-danger' onclick='add_data(" + p.getID_product() + ")'><i data-feather='shopping-cart'></i></a></div></td>");
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

		StringBuilder strProduct_card = new StringBuilder();
	    int card_max = productList.size();
	    row_id = 0;
	    int item_per_row = 5; 
	    int bootstrap_col = 12 / item_per_row; 

	    for (ProductBean p : productList) {
	        if (row_id % item_per_row == 0) {
	            strProduct_card.append("<div class='row mb-4'>"); 
	        }

	        strProduct_card.append("<div class='col-md-" + bootstrap_col + " d-flex mb-4'>");
	        strProduct_card.append("<div class='card card-move w-100 d-flex flex-column' style='border: 1px solid #eee; box-shadow: 0 2px 4px rgba(0,0,0,0.05); height: 400px;'>");
	        strProduct_card.append("<img class='card-img-top img-fluid' src='" + p.getp_pic() + "' alt='Card image'>");
	        strProduct_card.append("<div class='card-body d-flex flex-column'>");
	        strProduct_card.append("<h4 class='card-title' style='font-weight: bold; font-size: 1.1rem;'>" + p.getp_name() + "</h4>");
	        strProduct_card.append("<div class='mt-auto d-flex justify-content-between align-items-end'>"); 
	        strProduct_card.append("<div class='date-info' style='font-size: 0.85rem; color: #555;'>"); 
	        strProduct_card.append("<p class='mb-0'>ปิดรับ : " + p.getp_end_date() + "</p>");
	        strProduct_card.append("<p class='mb-0'>วันที่ส่ง : " + p.getp_send_date() + "</p>");
	        strProduct_card.append("</div>");
	        strProduct_card.append("<div class='buttons'>");
	        strProduct_card.append("<a class='btn icon btn-danger' onclick='modal_order(" + p.getID_product() + ")' title='Add to Cart'>");
	        strProduct_card.append("<i class='bi bi-cart-plus'></i>");
	        strProduct_card.append("</a>");
	        strProduct_card.append("</div></div></div></div></div>");
	        row_id++;
	        
	        if (row_id % item_per_row == 0 || row_id == card_max) {
	            strProduct_card.append("</div>");
	        }
	    }
	    
	    model.addAttribute("mainProduct_card", strProduct_card);

        return "order/order";
    }
}