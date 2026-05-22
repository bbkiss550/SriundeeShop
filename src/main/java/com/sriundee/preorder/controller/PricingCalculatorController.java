package com.sriundee.preorder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PricingCalculatorController {

    private static final int MENU_ID = 17;

    @Autowired
    private MenuController menuService;

    @GetMapping("/pricing-calculator")
    public String index(Model model) {
        model.addAttribute("mainMenus", menuService.getMenuList(MENU_ID, null));
        return "pricing/calculator";
    }
}
