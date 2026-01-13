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

import com.sriundee.preorder.bean.CoverBean;
import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.dto.CoverDto;
import com.sriundee.preorder.dto.ProductDto;
import com.sriundee.preorder.dto.VersionDto;
import com.sriundee.preorder.entity.Cover;
import com.sriundee.preorder.entity.Product;
import com.sriundee.preorder.entity.Version;
import com.sriundee.preorder.repository.CoverRepository;
import com.sriundee.preorder.repository.ProductRepository;
import com.sriundee.preorder.response.CoverResponse;

import org.springframework.ui.Model;

@Controller
public class CoverController {
	
	@Autowired
	VersionController versionController;

	@Autowired
	WebsiteController websiteController;
	
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CoverRepository coverRepository;
	
	@GetMapping("/product/cover/search/{id}")
	@ResponseBody
	public ResponseEntity<String> getListData(@PathVariable Integer id) {
		List<CoverBean> coverList = coverRepository.getDataAll(id);
		StringBuilder strCover = new StringBuilder();
		Integer row_id = 0;
		for (CoverBean c : coverList) {
			row_id +=1;
			strCover.append("<tr>");
			strCover.append("<td><div class='buttons'><a class='btn icon btn-warning' onclick='cov_edit_data(" + c.getID_cover() + ")'><i data-feather='edit'></i></a></div></td>");
			strCover.append("<td><div class='buttons'><a class='btn icon btn-danger' onclick='cov_delete_data(" + c.getID_cover() + ")'><i data-feather='trash-2'></i></a></div></td>");
			strCover.append("<td><div class='buttons'><a class='btn icon btn-primary' onclick='cov_copy_data(" + c.getID_cover() + ")'><i data-feather='copy'></i></a></div></td>");
			strCover.append("<td>" + row_id + "</td>");
			strCover.append("<td>" + c.getc_name() + "</td>");
			strCover.append("<td>" + c.getw_name() + "</td>");
			strCover.append("<td>" + c.getv_name() + "</td>");
			strCover.append("<td>" + c.getc_price_total() + "</td>");
			strCover.append("<td>" + c.getc_price_pledge() + "</td>");
			strCover.append("<td>" + c.getc_price_balance() + "</td>");
			strCover.append("<td>" + c.getc_price_1st() + "</td>");
			strCover.append("<td>" + c.getc_price_2nd() + "</td>");
			strCover.append("<td>" + c.getc_price_last() + "</td>");
			strCover.append("</tr>");
		}

	    return ResponseEntity.ok(strCover.toString());
	}
	
	@GetMapping("/product/cover/{id}")
	@ResponseBody
	public Object getProductData(@PathVariable Integer id) {
		List<ProductBean> productList = productRepository.getDataAllByID(id);
		String ListVersion = versionController.getDataList(id);
		String ListWebsite = websiteController.getDataList();
		
		if (!productList.isEmpty()) {
	        return new CoverResponse(
	        		productList.get(0),
	        		ListVersion.toString(),
	        		ListWebsite.toString()
	        );
	    }
	    return null;
	}

    @PostMapping("/product/cover/save")
    @ResponseBody
    public ResponseEntity<String> saveData(@RequestBody CoverDto coverDto) {
        try {
            Cover cover = new Cover();
            cover.setProduct(coverDto.getProduct());
            cover.setWebsite(coverDto.getWebsite());
            cover.setVersion(coverDto.getVersion());
            cover.setName(coverDto.getName());
            cover.setPrice_total(coverDto.getPrice_total());
            cover.setPrice_pledge(coverDto.getPrice_pledge());
            cover.setPrice_balance(coverDto.getPrice_balance());
            cover.setPrice_1st(coverDto.getPrice_1st());
            cover.setPrice_2nd(coverDto.getPrice_2nd());
            cover.setPrice_last(coverDto.getPrice_last());
            cover.setDelete("A");

            coverRepository.save(cover);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error");
        }
    }
    
    @GetMapping("/product/cover/get/{id}")
    @ResponseBody
    public ResponseEntity<Cover> getDataById(@PathVariable Integer id) {
        return coverRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/product/cover/update/{id}")
    @ResponseBody
    public ResponseEntity<String> updateData(@PathVariable Integer id, @RequestBody CoverDto coverDto) {
        try {
        	Cover cover = coverRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลเวอร์ชั่น"));
        	cover.setProduct(coverDto.getProduct());
            cover.setWebsite(coverDto.getWebsite());
            cover.setVersion(coverDto.getVersion());
            cover.setName(coverDto.getName());
            cover.setPrice_total(coverDto.getPrice_total());
            cover.setPrice_pledge(coverDto.getPrice_pledge());
            cover.setPrice_balance(coverDto.getPrice_balance());
            cover.setPrice_1st(coverDto.getPrice_1st());
            cover.setPrice_2nd(coverDto.getPrice_2nd());
            cover.setPrice_last(coverDto.getPrice_last());
            
        	coverRepository.save(cover);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/product/cover/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteData(@PathVariable Integer id) {
        try {
        	Cover cover = coverRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลเวอร์ชั่น"));
        	cover.setDelete("D");
            
        	coverRepository.save(cover);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
