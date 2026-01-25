package com.sriundee.preorder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChangeController {

	@Autowired
    private MenuController menuService;

	@Autowired
	private OrderStatusController orderStatusController;
	
    @GetMapping("/change")
    public String index(Model model) {
		String menuList = menuService.getMenuList(8,null);
	    model.addAttribute("mainMenus", menuList);

		String osCheckList = orderStatusController.getDataCheckList();
	    model.addAttribute("osCheckList", osCheckList);
	    
        return "change";
    }
}