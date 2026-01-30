package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.sriundee.preorder.bean.OrderDetailBean;
import com.sriundee.preorder.repository.OrderDetailRepository;

@Controller
public class ChangeController {

	@Autowired
    private MenuController menuService;

	@Autowired
	private OrderStatusController orderStatusController;
	
	@Autowired
	private OrderDetailRepository orderDetailRepository;
	
    @GetMapping("/change")
    public String index(Model model) {
		String menuList = menuService.getMenuList(8,null);
	    model.addAttribute("mainMenus", menuList);

		String osCheckList = orderStatusController.getDataCheckList();
	    model.addAttribute("osCheckList", osCheckList);

    	StringBuilder ListDetail = new StringBuilder();
		List<OrderDetailBean> orderdetailList = orderDetailRepository.getDataByIDOrderIsNull();
		for (OrderDetailBean od : orderdetailList) {
			ListDetail.append("<tr>");
			ListDetail.append("<td>" + od.geto_customer_name() + "</td>");
			ListDetail.append("<td>" + od.getp_name() + "</td>");
			ListDetail.append("<td>" + od.getw_name() + "</td>");
			ListDetail.append("<td>" + od.getv_name() + "</td>");
			ListDetail.append("<td>" + od.getc_name() + "</td>");
			ListDetail.append("<td>" + od.getod_qty() + "</td>");
			ListDetail.append("<td>");
		    ListDetail.append("<button type='button' class='btn btn-outline-" + od.getos_color() + " btn-sm'>");
		    ListDetail.append(od.getos_name());
		    ListDetail.append("</button>");
		    ListDetail.append("</td>");
			ListDetail.append("</tr>");
		}
		
	    model.addAttribute("listOrderDeatil", ListDetail);
	    
        return "change";
    }
}