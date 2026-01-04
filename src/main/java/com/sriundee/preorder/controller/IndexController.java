package com.sriundee.preorder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.ui.Model;

@Controller
public class IndexController {
	
	@Autowired
    private MenuController menuService;
	
	@GetMapping("/")
	public String index(Model model) {
		String menuList = menuService.getMenuList(1,null);
	    model.addAttribute("mainMenus", menuList);
	    return "index";
	}
}
