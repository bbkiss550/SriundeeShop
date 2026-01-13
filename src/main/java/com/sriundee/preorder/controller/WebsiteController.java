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

import com.sriundee.preorder.dto.WebsiteDto;
import com.sriundee.preorder.entity.Version;
import com.sriundee.preorder.entity.Website;
import com.sriundee.preorder.repository.WebsiteRepository;

import org.springframework.ui.Model;

@Controller
public class WebsiteController {
	
	@Autowired
    private MenuController menuController;

	@Autowired
	private WebsiteRepository websiteRepository;
	
	@GetMapping("/website")
	public String index(Model model) {
		String menuList = menuController.getMenuList(5,2);
	    model.addAttribute("mainMenus", menuList);

		List<Website> websiteList = websiteRepository.getDataAll();
		StringBuilder strWebsite = new StringBuilder();
		Integer row_id = 0;
		for (Website w : websiteList) {
			row_id +=1;
			strWebsite.append("<tr>");
			strWebsite.append("<td><div class='buttons'><a class='btn icon btn-warning' onclick='edit_data(" + w.getId() + ")'><i data-feather='edit'></i></a></div></td>");
			strWebsite.append("<td><div class='buttons'><a class='btn icon btn-danger' onclick='delete_data(" + w.getId() + ")'><i data-feather='trash-2'></i></a></div></td>");
			strWebsite.append("<td>" + row_id + "</td>");
			strWebsite.append("<td>" + w.getName() + "</td>");
			strWebsite.append("</tr>");
		}
	    model.addAttribute("mainWebsite", strWebsite);
	    
	    return "manage/website";
	}

    @PostMapping("/manage/website/save")
    @ResponseBody
    public ResponseEntity<String> saveData(@RequestBody WebsiteDto websiteDto) {
        try {
        	Website website = new Website();
        	website.setName(websiteDto.getWebsiteName());
        	website.setDelete("A");

            websiteRepository.save(website);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error");
        }
    }
    
    @GetMapping("/manage/website/get/{id}")
    @ResponseBody
    public ResponseEntity<Website> getDataById(@PathVariable Integer id) {
        return websiteRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/manage/website/update/{id}")
    @ResponseBody
    public ResponseEntity<String> updateData(@PathVariable Integer id, @RequestBody WebsiteDto websiteDto) {
        try {
        	Website website = websiteRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลเว็บไซต์"));
        	website.setName(websiteDto.getWebsiteName());
            
            websiteRepository.save(website);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/manage/website/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteData(@PathVariable Integer id) {
        try {
        	Website website = websiteRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลเว็บไซต์"));
        	website.setDelete("D");
            
        	websiteRepository.save(website);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @ResponseBody
    public String getDataList() {
    	List<Website> websiteList = websiteRepository.getDataAll();
	    StringBuilder strWebsite = new StringBuilder();
	    for (Website w : websiteList) {
	    	strWebsite.append("<option value='" + w.getId() + "'>" + w.getName() + "</option>");
	    }

	    return strWebsite.toString();
    }
}
