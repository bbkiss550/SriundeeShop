package com.sriundee.preorder.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
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

import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.dto.ArtistDto;
import com.sriundee.preorder.dto.ProductDto;
import com.sriundee.preorder.model.Artist;
import com.sriundee.preorder.model.Product;
import com.sriundee.preorder.repository.ProductRepository;
import com.sriundee.preorder.repository.TypeRepository;

import org.springframework.ui.Model;

@Controller
public class ProductController {
	
	@Autowired
    private MenuController menuService;
	
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ArtistController artistController;

	@Autowired
	private TypeController typeController;

	@Autowired
	private PaymentTypeController paymenttypeController;

	@Autowired
	private ProductStatusController productstatusController;
	
	@GetMapping("/product")
	public String index(Model model) {
		String menuList = menuService.getMenuList(6,null);
	    model.addAttribute("mainMenus", menuList);
	    
		List<ProductBean> productList = productRepository.getDataAll();
		StringBuilder strProduct = new StringBuilder();
		Integer row_id = 0;
		for (ProductBean p : productList) {
			row_id +=1;
			strProduct.append("<tr>");
			strProduct.append("<td>" + row_id + "</td>");
			strProduct.append("<td><img src='" + p.getP_pic() + "' class='table-img'></td>");
			strProduct.append("<td>" + p.getP_name() + "</td>");
			strProduct.append("<td>" + p.getT_name() + "</td>");
			strProduct.append("<td>" + p.getA_name() + "</td>");
			strProduct.append("<td>" + p.getP_end_date() + "</td>");
			strProduct.append("<td>" + p.getP_send_date() + "</td>");
			strProduct.append("<td>" + p.getP_second_pay_date() + "</td>");
			strProduct.append("<td>" + p.getP_last_pay_date() + "</td>");
			strProduct.append("<td>" + p.getPs_name() + "</td>");
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-primary' onclick='edit_data(" + p.getID_product() + ")'><i data-feather='edit'></i></a></div></td>");
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-warning' onclick='edit_data(" + p.getID_product() + ")'><i data-feather='edit'></i></a></div></td>");
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-danger' onclick='delete_data(" + p.getID_product() + ")'><i data-feather='trash-2'></i></a></div></td>");
			strProduct.append("</tr>");
		}
	    model.addAttribute("mainProduct", strProduct);

	    String ListType = typeController.getDataList();
	    model.addAttribute("ListType", ListType);
	    
	    String ListArtist = artistController.getDataList();
	    model.addAttribute("ListArtist", ListArtist);

	    String ListPaymentType = paymenttypeController.getDataList();
	    model.addAttribute("ListPaymentType", ListPaymentType);

	    String ListProductstatus = productstatusController.getDataList();
	    model.addAttribute("ListProductstatus", ListProductstatus);

	    return "product";
	}
    
    @PostMapping("/product/save")
    @ResponseBody
    public ResponseEntity<String> saveData(@RequestBody ProductDto productDto) {
        try {
        	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            Product product = new Product();
            product.setName(productDto.getName());
            product.setType(productDto.getType());
            product.setArtist(productDto.getArtist());
            product.setEnd_date(formatter.parse(productDto.getEnd_date()));
            product.setSend_date(formatter.parse(productDto.getSend_date()));
            product.setSecond_pay_date(formatter.parse(productDto.getSecond_pay_date()));
            product.setPayment_type(productDto.getPayment_type());
            product.setLast_pay_date(formatter.parse(productDto.getLast_pay_date()));
            product.setProduct_status(productDto.getProduct_status());
            product.setDelete("A");
            product.setPic(productDto.getPic());

            productRepository.save(product);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error");
        }
    }
    
    @GetMapping("/product/get/{id}")
    @ResponseBody
    public ResponseEntity<Product> getDataById(@PathVariable Integer id) {
        return productRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
