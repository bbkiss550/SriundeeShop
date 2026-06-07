package com.sriundee.preorder.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
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
import com.sriundee.preorder.bean.ProductOrderSummaryBean;
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
			strProduct.append("<td class='product-image-cell'><img src='" + p.getp_pic() + "' class='table-img product-table-img' tabindex='0'></td>");
			strProduct.append("<td>" + p.getp_name() + "</td>");
			strProduct.append("<td>" + p.gett_name() + "</td>");
			strProduct.append("<td>" + p.geta_name() + "</td>");
			strProduct.append("<td>" + displayScheduleDate(p.getp_end_date()) + "</td>");
			if (p.getID_pro_status() == 1) {
				strProduct.append("<td><span class='badge bg-success' style='padding: 15px;'>" + p.getps_name() + "</span></td>");
			} else {
				strProduct.append("<td><span class='badge bg-danger' style='padding: 15px;'>" + p.getps_name() + "</span></td>");
			}
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-info' onclick='modal_order_summary(" + p.getID_product() + ", this)' data-product-name='" + escapeHtml(p.getp_name()) + "'><i data-feather='bar-chart-2'></i></a></div></td>");
			strProduct.append("</tr>");
		}
	    model.addAttribute("mainProduct", strProduct);

	    String ListType = typeController.getDataList();
	    model.addAttribute("ListType", ListType);
	    
	    String ListArtist = artistController.getDataList();
	    model.addAttribute("ListArtist", ListArtist);

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
            product.setEnd_date(parseNullableDate(formatter, productDto.getEnd_date()));
            product.setSend_date(parseNullableDate(formatter, productDto.getSend_date()));
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
            product.setEnd_date(parseNullableDate(formatter, productDto.getEnd_date()));
            product.setSend_date(parseNullableDate(formatter, productDto.getSend_date()));
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

    @GetMapping("/product/order-summary/{id}")
    @ResponseBody
    public ResponseEntity<String> getOrderSummary(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(buildOrderSummaryRows(productRepository.getOrderSummaryByProduct(id)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<tr><td colspan='4' class='text-center text-danger'>Error</td></tr>");
        }
    }

    private Date parseNullableDate(SimpleDateFormat formatter, String value) throws Exception {
        if (value == null || value.isBlank()) {
            return null;
        }
        return formatter.parse(value);
    }

    private String displayScheduleDate(String value) {
        return value == null || value.isBlank() ? "ไม่มีกำหนด" : value;
    }

    private String buildOrderSummaryRows(List<ProductOrderSummaryBean> rows) {
        if (rows == null || rows.isEmpty()) {
            return "<tr><td colspan='4' class='text-center text-muted'>\u0e44\u0e21\u0e48\u0e21\u0e35\u0e02\u0e49\u0e2d\u0e21\u0e39\u0e25</td></tr>";
        }
        StringBuilder summary = new StringBuilder();
        int rowId = 0;
        String previousWebsite = null;
        String previousVersion = null;
        for (ProductOrderSummaryBean row : rows) {
            rowId += 1;
            String website = toDisplay(row.getw_name());
            String version = toDisplay(row.getv_name());
            boolean newWebsite = !website.equals(previousWebsite);
            boolean newVersion = newWebsite || !version.equals(previousVersion);

            summary.append("<tr");
            if (newWebsite && rowId > 1) {
                summary.append(" class='product-order-summary-group'");
            }
            summary.append(">");
            if (newWebsite) {
                summary.append("<td rowspan='")
                        .append(countWebsiteRows(rows, website))
                        .append("' class='product-order-summary-merged'>")
                        .append(escapeHtml(website))
                        .append("</td>");
            }
            if (newVersion) {
                summary.append("<td rowspan='")
                        .append(countVersionRows(rows, website, version))
                        .append("' class='product-order-summary-merged'>")
                        .append(escapeHtml(version))
                        .append("</td>");
            }
            summary.append("<td>").append(escapeHtml(row.getc_name())).append("</td>");
            summary.append("<td class='text-end product-order-summary-qty'>")
                    .append(row.getproduct_qty() == null ? 0 : row.getproduct_qty())
                    .append("</td>");
            summary.append("</tr>");

            previousWebsite = website;
            previousVersion = version;
        }
        return summary.toString();
    }

    private int countWebsiteRows(List<ProductOrderSummaryBean> rows, String website) {
        int count = 0;
        for (ProductOrderSummaryBean row : rows) {
            if (website.equals(toDisplay(row.getw_name()))) {
                count += 1;
            }
        }
        return count;
    }

    private int countVersionRows(List<ProductOrderSummaryBean> rows, String website, String version) {
        int count = 0;
        for (ProductOrderSummaryBean row : rows) {
            if (website.equals(toDisplay(row.getw_name())) && version.equals(toDisplay(row.getv_name()))) {
                count += 1;
            }
        }
        return count;
    }

    private String escapeHtml(Object value) {
        return toDisplay(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String toDisplay(Object value) {
        return value == null ? "" : value.toString();
    }
}
