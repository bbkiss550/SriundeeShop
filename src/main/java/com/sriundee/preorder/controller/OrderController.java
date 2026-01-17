package com.sriundee.preorder.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.CoverBean;
import com.sriundee.preorder.bean.CustomerNameBean;
import com.sriundee.preorder.bean.GroupWebsiteBean;
import com.sriundee.preorder.bean.OrderDetailBean;
import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.bean.OrderSummaryBean;
import com.sriundee.preorder.dto.OrderDetailDto;
import com.sriundee.preorder.entity.Cover;
import com.sriundee.preorder.entity.OrderDetail;
import com.sriundee.preorder.entity.Version;
import com.sriundee.preorder.repository.CoverRepository;
import com.sriundee.preorder.repository.OrderDetailRepository;
import com.sriundee.preorder.repository.OrderRepository;
import com.sriundee.preorder.repository.ProductRepository;
import com.sriundee.preorder.repository.VersionRepository;

import org.springframework.ui.Model;

@Controller
public class OrderController {

	@Autowired
    private MenuController menuService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CoverRepository coverRepository;

	@Autowired
	private VersionRepository versionRepository;
	
	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderDetailRepository orderDetailRepository;
	
	@Autowired
	private PaymentMethodController paymentMethodController;
	
    @GetMapping("/order")
    public String index(Model model) {
		String menuList = menuService.getMenuList(7,null);
	    model.addAttribute("mainMenus", menuList);

		List<ProductBean> productList = productRepository.getDataOrder();
		StringBuilder strProduct = new StringBuilder();
		Integer row_id = 0;
		for (ProductBean p : productList) {
			row_id +=1;
			strProduct.append("<tr>");
			strProduct.append("<td><div class='buttons'><a class='btn icon btn-danger' onclick='add_data(" + p.getID_product() + ")'><i data-feather='shopping-cart'></i></a></div></td>");
			strProduct.append("<td>" + row_id + "</td>");
			strProduct.append("<td><img src='" + p.geta_logo() + "' class='table-img'></td>");
			strProduct.append("<td>" + p.getp_name() + "</td>");
			strProduct.append("<td>" + p.gett_name() + "</td>");
			strProduct.append("<td>" + p.geta_name() + "</td>");
			strProduct.append("<td>" + p.getp_end_date() + "</td>");
			strProduct.append("<td>" + p.getp_send_date() + "</td>");
			strProduct.append("<td>" + p.getp_last_pay_date() + "</td>");
			strProduct.append("</tr>");
		}
	    model.addAttribute("mainProduct", strProduct);

		StringBuilder strProduct_card = new StringBuilder();
	    int card_max = productList.size();
	    row_id = 0;
	    int item_per_row = 5; 
	    int bootstrap_col = 12 / item_per_row; 

	    for (ProductBean p : productList) {
	        if (row_id % item_per_row == 0) {
	            strProduct_card.append("<div class='row mb-4'>"); 
	        }

	        strProduct_card.append("<div class='col-md-" + bootstrap_col + " d-flex mb-4'>");
	        strProduct_card.append("<div class='card card-move w-100 d-flex flex-column' style='border: 1px solid #eee; box-shadow: 0 2px 4px rgba(0,0,0,0.05); height: 400px;'>");
	        strProduct_card.append("<img class='card-img-top img-fluid' src='" + p.getp_pic() + "' alt='Card image'>");
	        strProduct_card.append("<div class='card-body d-flex flex-column'>");
	        strProduct_card.append("<h4 class='card-title' style='font-weight: bold; font-size: 1.1rem;'>" + p.getp_name() + "</h4>");
	        strProduct_card.append("<div class='mt-auto d-flex justify-content-between align-items-end'>"); 
	        strProduct_card.append("<div class='date-info' style='font-size: 0.85rem; color: #555;'>"); 
	        strProduct_card.append("<p class='mb-0'>ปิดรับ : " + p.getp_end_date() + "</p>");
	        strProduct_card.append("<p class='mb-0'>วันที่ส่ง : " + p.getp_send_date() + "</p>");
	        strProduct_card.append("</div>");
	        strProduct_card.append("<div class='buttons'>");
	        strProduct_card.append("<a class='btn icon btn-danger' onclick='modal_order(" + p.getID_product() + ")' title='Add to Cart'>");
	        strProduct_card.append("<i class='bi bi-cart-plus'></i>");
	        strProduct_card.append("</a>");
	        strProduct_card.append("</div></div></div></div></div>");
	        row_id++;
	        
	        if (row_id % item_per_row == 0 || row_id == card_max) {
	            strProduct_card.append("</div>");
	        }
	    }
	    
	    model.addAttribute("mainProduct_card", strProduct_card);

	    List<OrderDetailBean> orderList = orderDetailRepository.getCartIsNull();
	    model.addAttribute("CartCount", orderList.size());

        return "order/order";
    }
    
    @GetMapping("/order/load/{id}")
	@ResponseBody
	public Object getData(@PathVariable Integer id) {
    	List<ProductBean> productList = productRepository.getDataAllByID(id);

    	StringBuilder Listqty = new StringBuilder();
    	String strChecked = "";
    	for (int i=1; i<21; i++) {
    		if (i == 1) { strChecked = "checked"; } else { strChecked = ""; }
    		Listqty.append("<input type='radio' class='btn-check' name='group-qty' id='qty" + i + "' value='" + i + "' onchange='cal()' " + strChecked + ">");
    		Listqty.append("<label class='btn btn-outline-danger' for='qty" + i + "'>" + i + "</label>");
		}
    	
    	StringBuilder ListCheckWebsite = new StringBuilder();
		List<GroupWebsiteBean> websietList = orderRepository.getDataByID_pro(id);
		for (GroupWebsiteBean w : websietList) {
			ListCheckWebsite.append("<input type='radio' class='btn-check' name='group-website' id='website" + w.getID_web() + "' value='" + w.getID_web() + "' onchange='select_cover()'>");
			ListCheckWebsite.append("<label class='btn btn-outline-primary' for='website" + w.getID_web() + "'>" + w.getw_name() + "</label>");
		}
		
    	StringBuilder ListCheckVersion = new StringBuilder();
		List<Version> versionList = versionRepository.getDataByID_pro(id);
		for (Version v : versionList) {
			ListCheckVersion.append("<input type='radio' class='btn-check' name='group-version' id='version" + v.getId() + "' value='" + v.getId() + "' onchange='select_cover()'>");
			ListCheckVersion.append("<label class='btn btn-outline-primary' for='version" + v.getId() + "'>" + v.getName() + "</label>");
		}

        Map<String, Object> response = new HashMap<>();
        response.put("product", productList.get(0));
        response.put("listQty", Listqty.toString());
        response.put("listWebsite", ListCheckWebsite.toString());
        response.put("listVersion", ListCheckVersion.toString());
        return response;
	}
    
    @GetMapping("/order/loadcover/{idProduct}/{idWebsite}/{idVersion}")
	@ResponseBody
	public Object getDataCover(@PathVariable("idProduct") Integer idProduct,@PathVariable("idWebsite") Integer idWebsite,@PathVariable("idVersion") Integer idVersion) {
    	StringBuilder ListCheckCover = new StringBuilder();
		List<CoverBean> coverList = coverRepository.SearchData(idProduct,idWebsite,idVersion);
		for (CoverBean c : coverList) {
			ListCheckCover.append("<input type='radio' class='btn-check' name='group-cover' id='cover" + c.getID_cover() + "' value='" + c.getID_cover() + "' onchange='select_price()'>");
			ListCheckCover.append("<label class='btn btn-outline-primary' for='cover" + c.getID_cover() + "'>" + c.getc_name() + "</label>");
		}

        Map<String, Object> response = new HashMap<>();
        response.put("listCover", ListCheckCover.toString());
        return response;
	}
    
    @GetMapping("/order/getprice/{id}")
    @ResponseBody
    public ResponseEntity<Cover> getDataById(@PathVariable Integer id) {
        return coverRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/order/detail/save")
    @ResponseBody
    public ResponseEntity<String> saveData(@RequestBody OrderDetailDto orderDetailDto) {
        try {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(null);
            orderDetail.setCover(orderDetailDto.getCover());
            orderDetail.setQty(orderDetailDto.getQty());
            orderDetail.setPrice_total(orderDetailDto.getPrice_total());
            orderDetail.setPrice_pledge(orderDetailDto.getPrice_pledge());
            orderDetail.setPrice_balance(orderDetailDto.getPrice_balance());
            orderDetail.setOrder_status(1);

            orderDetailRepository.save(orderDetail);

            return ResponseEntity.ok("Success");
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error");
        }
    }
    
	@GetMapping("/order/getcartcount")
	@ResponseBody
	public ResponseEntity<Integer> getCartCount() {
	    List<OrderDetailBean> orderList = orderDetailRepository.getCartIsNull();

	    return ResponseEntity.ok(orderList.size());
	}
	
    @GetMapping("/order/loadcart")
	@ResponseBody
	public Object getDataCover() {
    	StringBuilder ListDetail = new StringBuilder();
		List<OrderDetailBean> orderdetailList = orderDetailRepository.getCartIsNull();
		for (OrderDetailBean o : orderdetailList) {
			ListDetail.append("<div class='row'>");
			ListDetail.append("<div class='col-md-1'>");
			ListDetail.append("<div class='buttons'><a class='btn icon btn-danger' onclick='delete_detail(" + o.getID_order_detail() + ")'><i data-feather='trash-2'></i></a></div>");
			ListDetail.append("</div>");
			ListDetail.append("<div class='col-md-6'>");
			ListDetail.append("<div class='fw-bold text-dark'>" + o.getp_name() + "</div>");
			ListDetail.append("<div class='text-muted small mt-1'>เว็บ : " + o.getw_name() + "</div>");
			ListDetail.append("<div class='text-muted small mt-1'>เวอร์ชั่น : " + o.getv_name() + "</div>");
			ListDetail.append("<div class='text-muted small mt-1'>ปก : " + o.getc_name() + "</div>");
			ListDetail.append("</div>");
			ListDetail.append("<div class='col-md-1'>");
			ListDetail.append("<div class='fw-bold' style='text-align: right !important;'>" + o.getc_price_total() + "</div>");
			ListDetail.append("</div>");
			ListDetail.append("<div class='col-md-1'>");
			ListDetail.append("<div class='fw-bold' style='text-align: right !important;'>" + o.getc_price_pledge() + "</div>");
			ListDetail.append("</div>");
			ListDetail.append("<div class='col-md-1'>");
			ListDetail.append("<div class='input-group input-group-sm mb-1' style='width: 90px;'>");
			ListDetail.append("<button class='btn btn-outline-secondary' type='button' onclick='qty_down(" + o.getID_order_detail() + ")'>-</button>");
			ListDetail.append("<input type='text' id='qty_" + o.getID_order_detail() + "' class='form-control text-center bg-white' value='" + o.getod_qty() + "' readonly>");
			ListDetail.append("<button class='btn btn-outline-secondary' type='button' onclick='qty_up(" + o.getID_order_detail() + ")'>+</button>");
			ListDetail.append("</div>");
			ListDetail.append("</div>");
			ListDetail.append("<div class='col-md-1'>");
			ListDetail.append("<div class='text-danger fw-bold' style='font-size: 1.1rem; text-align: right !important;'>" + o.getod_price_total() + "</div>");
			ListDetail.append("</div>");
			ListDetail.append("<div class='col-md-1'>");
			ListDetail.append("<div class='text-danger fw-bold' style='font-size: 1.1rem; text-align: right !important;'>" + o.getod_price_pledge() + "</div>");
			ListDetail.append("</div>");
			ListDetail.append("</div>");
			ListDetail.append("<hr>");
		}

    	Double sum_price_total = 0.0;
    	Double sum_price_pledge = 0.0;
    	Double sum_price_balance = 0.0;
		List<OrderSummaryBean> summayList = orderDetailRepository.getCartSummary();
		for (OrderSummaryBean s : summayList) {
			sum_price_total = s.getsum_price_total();
	    	sum_price_pledge = s.getsum_price_pledge();
	    	sum_price_balance = s.getsum_price_balance();
		}
		
		String ListPaymentMethod = paymentMethodController.getDataList();
		

	    List<CustomerNameBean> mainCustomerName = orderRepository.getCsutomerList();
	    StringBuilder strCustomerName = new StringBuilder();
	    for (CustomerNameBean c : mainCustomerName) {
	    	strCustomerName.append("<option value='" + c.geto_customer_name() + "'>" + c.geto_customer_name() + "</option>");
	    }

        Map<String, Object> response = new HashMap<>();
        response.put("listDetail", ListDetail.toString());
        response.put("total_price", sum_price_total);
        response.put("pledge_price", sum_price_pledge);
        response.put("balance_price", sum_price_balance);
        response.put("listPaymentMethod", ListPaymentMethod);
        response.put("listCustomerName", strCustomerName.toString());
        return response;
	}

    @PostMapping("/order/detail/update/{id}")
    @ResponseBody
    public ResponseEntity<String> updateDataDetail(@PathVariable Integer id, @RequestBody OrderDetailDto orderDetailDto) {
        try {
        	Double price_total = 0.0;
        	Double price_pledge = 0.0;
        	Double price_balance = 0.0;
        	List<OrderDetailBean> orderdetailList = orderDetailRepository.getCartByID(id);
        	for (OrderDetailBean o : orderdetailList) {
        		price_total = (o.getc_price_total() != null) ? Double.parseDouble(o.getc_price_total().replace(",", "")) : 0.0;
        		price_pledge = (o.getc_price_pledge() != null) ? Double.parseDouble(o.getc_price_pledge().replace(",", "")) : 0.0;
        		price_balance = (o.getc_price_balance() != null) ? Double.parseDouble(o.getc_price_balance().replace(",", "")) : 0.0;
        	}
        	
        	OrderDetail orderDetail = orderDetailRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลเวอร์ชั่น"));
        	Integer od_qty = orderDetailDto.getQty();
            orderDetail.setQty(od_qty);
            orderDetail.setPrice_total(price_total * od_qty);
            orderDetail.setPrice_pledge(price_pledge * od_qty);
            orderDetail.setPrice_balance(price_balance * od_qty);

            orderDetailRepository.save(orderDetail);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error");
        }
    }
    
    @PostMapping("/order/detail/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteData(@PathVariable("id") Integer id) {
        try {
            if (orderDetailRepository.existsById(id)) {
                orderDetailRepository.deleteById(id);
                
                return ResponseEntity.ok("Success");
            } else {
                return ResponseEntity.status(404).body("Data not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error");
        }
    }
}