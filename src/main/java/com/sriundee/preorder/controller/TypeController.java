package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.dto.TypeDto;
import com.sriundee.preorder.entity.Type;
import com.sriundee.preorder.repository.TypeRepository;

@Controller
public class TypeController {

	@Autowired
    private MenuController menuService;

	@Autowired
	private TypeRepository typeRepository;
	
    @GetMapping("/type")
    public String index(Model model) {
		String menuList = menuService.getMenuList(3,2);
	    model.addAttribute("mainMenus", menuList);
	    
		List<Type> typeList = typeRepository.getDataAll();
		StringBuilder strType = new StringBuilder();
		Integer row_id = 0;
		for (Type t : typeList) {
			row_id +=1;
			strType.append("<tr>");
			strType.append("<td><div class='buttons'><a class='btn icon btn-warning' onclick='edit_data(" + t.getId() + ")'><i data-feather='edit'></i></a></div></td>");
			strType.append("<td><div class='buttons'><a class='btn icon btn-danger' onclick='delete_data(" + t.getId() + ")'><i data-feather='trash-2'></i></a></div></td>");
			strType.append("<td>" + row_id + "</td>");
			strType.append("<td>" + t.getName() + "</td>");
			strType.append("</tr>");
		}
	    model.addAttribute("mainType", strType);
	    
        return "manage/type";
    }
    
    @PostMapping("/manage/type/save")
    @ResponseBody
    public ResponseEntity<String> saveData(@RequestBody TypeDto typeDto) {
        try {
            Type type = new Type();
            type.setName(typeDto.getTypeName());
            type.setDelete("A");

            typeRepository.save(type);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error");
        }
    }
    
    @GetMapping("/manage/type/get/{id}")
    @ResponseBody
    public ResponseEntity<Type> getDataById(@PathVariable Integer id) {
        return typeRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/manage/type/update/{id}")
    @ResponseBody
    public ResponseEntity<String> updateData(@PathVariable Integer id, @RequestBody TypeDto typeDto) {
        try {
            Type type = typeRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลประเภท"));
            type.setName(typeDto.getTypeName());
            
            typeRepository.save(type);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/manage/type/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteData(@PathVariable Integer id) {
        try {
        	Type type = typeRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลประเภท"));
        	type.setDelete("D");
            
            typeRepository.save(type);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    public String getDataList() {
	    List<Type> mainType = typeRepository.getDataAll();
	    StringBuilder strType = new StringBuilder();
	    for (Type t : mainType) {
	    	strType.append("<option value='" + t.getId() + "'>" + t.getName() + "</option>");
	    }
	    
	    return strType.toString();
    }
}