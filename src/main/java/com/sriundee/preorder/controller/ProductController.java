package com.sriundee.preorder.controller;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.dto.ProductDto;
import com.sriundee.preorder.entity.Product;
import com.sriundee.preorder.repository.ProductRepository;

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
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-warning' onclick='edit_data(" + p.getID_product() + ")'><i data-feather='edit'></i></a></div></td>");
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-danger' onclick='delete_data(" + p.getID_product() + ")'><i data-feather='trash-2'></i></a></div></td>");
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-dark' onclick='modal_cover(" + p.getID_product() + ")'><i data-feather='image'></i></a></div></td>");
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-primary' onclick='modal_version(" + p.getID_product() + ")'><i data-feather='list'></i></a></div></td>");
			strProduct.append("<td>" + row_id + "</td>");
			strProduct.append("<td><img src='" + p.getp_pic() + "' class='table-img'></td>");
			strProduct.append("<td>" + p.getp_name() + "</td>");
			strProduct.append("<td>" + p.gett_name() + "</td>");
			strProduct.append("<td>" + p.geta_name() + "</td>");
			strProduct.append("<td>" + p.getp_end_date() + "</td>");
			if (p.getID_pro_status() == 1) {
				strProduct.append("<td><span class='badge bg-success' style='padding: 15px;'>" + p.getps_name() + "</span></td>");
			} else {
				strProduct.append("<td><span class='badge bg-danger' style='padding: 15px;'>" + p.getps_name() + "</span></td>");
			}
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

	    return "product/product";
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
            product.setPayment_type(productDto.getPayment_type());
            if (productDto.getLast_pay_date() != null) {
                product.setLast_pay_date(formatter.parse(productDto.getLast_pay_date()));
            }
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
    
    @PostMapping("/product/update/{id}")
    @ResponseBody
    public ResponseEntity<String> updateData(@PathVariable Integer id, @RequestBody ProductDto productDto) {
        try {
        	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        	Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลสินค้า"));
        	product.setName(productDto.getName());
            product.setType(productDto.getType());
            product.setArtist(productDto.getArtist());
            product.setEnd_date(formatter.parse(productDto.getEnd_date()));
            product.setSend_date(formatter.parse(productDto.getSend_date()));
            product.setPayment_type(productDto.getPayment_type());
            if (productDto.getLast_pay_date() != null) {
                product.setLast_pay_date(formatter.parse(productDto.getLast_pay_date()));
            }
            product.setProduct_status(productDto.getProduct_status());
            product.setDelete("A");
            product.setPic(productDto.getPic());
            
            productRepository.save(product);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/product/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteData(@PathVariable Integer id) {
        try {
        	Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลสินค้า"));
        	product.setDelete("D");
            
        	productRepository.save(product);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
