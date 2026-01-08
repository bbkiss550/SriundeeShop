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

import com.sriundee.preorder.dto.VersionDto;
import com.sriundee.preorder.entity.Version;
import com.sriundee.preorder.repository.VersionRepository;

@Controller
public class VersionController {

	@Autowired
	private VersionRepository versionRepository;

	@GetMapping("/product/version/{id}")
	@ResponseBody
	public ResponseEntity<String> getListVersion(@PathVariable Integer id) {
	    List<Version> versionList = versionRepository.getDataAll(id);
	    StringBuilder strVersion = new StringBuilder();
	    Integer row_id = 0;
	    for (Version a : versionList) {
	        row_id += 1;
	        strVersion.append("<tr>");
	        strVersion.append("<td><div class='buttons'><a class='btn icon btn-warning' onclick='ver_edit_data(" + a.getId() + ")'><i data-feather='edit'></i></a></div></td>");
	        strVersion.append("<td><div class='buttons'><a class='btn icon btn-danger' onclick='ver_delete_data(" + a.getId() + ")'><i data-feather='trash-2'></i></a></div></td>");
	        strVersion.append("<td>" + row_id + "</td>");
	        strVersion.append("<td>" + a.getName() + "</td>");
	        strVersion.append("</tr>");
	    }

	    return ResponseEntity.ok(strVersion.toString());
	}
	
    @PostMapping("/product/version/save")
    @ResponseBody
    public ResponseEntity<String> saveData(@RequestBody VersionDto versionDto) {
        try {
        	Version version = new Version();
        	version.setName(versionDto.getName());
        	version.setProduct(versionDto.getProduct());
        	version.setDelete("A");

            versionRepository.save(version);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error");
        }
    }
    
    @GetMapping("/product/version/get/{id}")
    @ResponseBody
    public ResponseEntity<Version> getDataById(@PathVariable Integer id) {
        return versionRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/product/version/update/{id}")
    @ResponseBody
    public ResponseEntity<String> updateData(@PathVariable Integer id, @RequestBody  VersionDto versionDto) {
        try {
        	Version version = versionRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลเวอร์ชั่น"));
        	version.setName(versionDto.getName());
            
        	versionRepository.save(version);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/product/version/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteData(@PathVariable Integer id) {
        try {
        	Version version = versionRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลเวอร์ชั่น"));
        	version.setDelete("D");
            
        	versionRepository.save(version);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}