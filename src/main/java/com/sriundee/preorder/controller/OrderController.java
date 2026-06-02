package com.sriundee.preorder.controller;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.CoverBean;
import com.sriundee.preorder.bean.CustomerNameBean;
import com.sriundee.preorder.bean.GroupWebsiteBean;
import com.sriundee.preorder.bean.OrderDetailBean;
import com.sriundee.preorder.bean.OrderSummaryBean;
import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.dto.OrderDetailDto;
import com.sriundee.preorder.dto.OrderDto;
import com.sriundee.preorder.entity.Cover;
import com.sriundee.preorder.entity.Income;
import com.sriundee.preorder.entity.Order;
import com.sriundee.preorder.entity.OrderDetail;
import com.sriundee.preorder.entity.Product;
import com.sriundee.preorder.entity.Version;
import com.sriundee.preorder.repository.CoverRepository;
import com.sriundee.preorder.repository.IncomeRepository;
import com.sriundee.preorder.repository.OrderDetailRepository;
import com.sriundee.preorder.repository.OrderRepository;
import com.sriundee.preorder.repository.ProductRepository;
import com.sriundee.preorder.repository.VersionRepository;

import jakarta.transaction.Transactional;

@Controller
public class OrderController {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
    private IncomeRepository incomeRepository;

	@Autowired
	private ArtistController artistController;

	@Autowired
	private PaymentTypeController paymenttypeController;

	@Autowired
	private PaymentMethodController paymentMethodController;

	@Autowired
	private SettingController settingController;
	
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
			strProduct.append("<td>" + displayScheduleDate(p.getp_end_date()) + "</td>");
			strProduct.append("<td>" + displayScheduleDate(p.getp_send_date()) + "</td>");
			strProduct.append("<td>" + p.getp_last_pay_date() + "</td>");
			strProduct.append("</tr>");
		}
	    model.addAttribute("mainProduct", strProduct);

		StringBuilder strProduct_card = new StringBuilder();
	    int bootstrap_col = 12 / 5;
	    strProduct_card.append("<div class='row g-4 product-grid-list'>");

	    for (ProductBean p : productList) {
	        boolean isOpenPreorder = Integer.valueOf(1).equals(p.getID_pro_status());
	        strProduct_card.append("<div class='col-md-" + bootstrap_col + " d-flex product-grid-item" + (isOpenPreorder ? "" : " is-hidden-closed") + "' data-product-status='" + p.getID_pro_status() + "' data-artist-id='" + p.getID_art() + "' data-product-name='" + escapeHtml(p.getp_name()) + "'>");
	        strProduct_card.append("<div class='card card-move product-grid-card w-100 d-flex flex-column " + (isOpenPreorder ? "" : "product-grid-card-closed") + "'");
	        strProduct_card.append(" onclick='modal_order(" + p.getID_product() + ")'");
	        strProduct_card.append(">");
	        strProduct_card.append("<div class='product-grid-image-wrap'>");
	        strProduct_card.append("<span class='product-status-tag " + getProductStatusTagClass(p.getID_pro_status()) + "'><span class='product-status-text'>" + p.getps_name() + "</span></span>");
	        strProduct_card.append("<img class='product-grid-image' src='" + p.getp_pic() + "' alt='Card image'>");
	        strProduct_card.append("</div>");
	        strProduct_card.append("<div class='card-body d-flex flex-column'>");
	        strProduct_card.append("<h4 class='card-title' style='font-weight: bold; font-size: 1.1rem;'>" + p.getp_name() + "</h4>");
	        strProduct_card.append("<div class='mt-auto d-flex justify-content-between align-items-end'>"); 
	        strProduct_card.append("<div class='date-info' style='font-size: 0.85rem; color: #555;'>"); 
	        strProduct_card.append("<p class='mb-0'>ปิดรับ : " + displayScheduleDate(p.getp_end_date()) + "</p>");
	        strProduct_card.append("<p class='mb-0'>วันที่ส่ง : " + displayScheduleDate(p.getp_send_date()) + "</p>");
	        strProduct_card.append("</div>");
	        strProduct_card.append("<span class='product-days-left-badge'>" + getDaysLeftLabel(p.getp_end_date()) + "</span>");
	        strProduct_card.append("</div></div></div></div>");
	    }
	    strProduct_card.append("</div>");
	    
	    model.addAttribute("mainProduct_card", strProduct_card);
	    model.addAttribute("showClosedProducts", settingController.getOrderShowClosedProductsValue());
	    model.addAttribute("artistList", "<option value=''>ทั้งหมด</option>" + artistController.getDataList());

	    List<OrderDetailBean> orderList = orderDetailRepository.getCartIsNull(settingController.currentUserId());
	    model.addAttribute("CartCount", orderList.size());

        return "order/order";
    }
    
    @GetMapping("/order/load/{id}")
	@ResponseBody
	public Object getData(@PathVariable Integer id) {
    	List<ProductBean> productList = productRepository.getDataAllByID(id);
    	if (productList.isEmpty()) {
    		return ResponseEntity.badRequest().body("Product not found");
    	}

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
        	Cover cover = coverRepository.findById(orderDetailDto.getCover()).orElse(null);
        	if (cover == null) {
        		return ResponseEntity.status(404).body("Cover not found");
        	}

        	Product product = productRepository.findById(cover.getProduct()).orElse(null);
        	if (product == null) {
        		return ResponseEntity.badRequest().body("Product not found");
        	}

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(null);
            orderDetail.setCover(orderDetailDto.getCover());
            orderDetail.setQty(orderDetailDto.getQty());
            orderDetail.setPrice_total(orderDetailDto.getPrice_total());
            orderDetail.setPrice_pledge(orderDetailDto.getPrice_pledge());
            orderDetail.setPrice_balance(orderDetailDto.getPrice_balance());
            orderDetail.setOrder_status(1);
            orderDetail.setUser(settingController.currentUserId());

            orderDetailRepository.save(orderDetail);

            return ResponseEntity.ok("Success");
            
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(500).body("Error");
        }
    }
    
	@GetMapping("/order/getcartcount")
	@ResponseBody
	public ResponseEntity<Integer> getCartCount() {
	    List<OrderDetailBean> orderList = orderDetailRepository.getCartIsNull(settingController.currentUserId());

	    return ResponseEntity.ok(orderList.size());
	}
	
    @GetMapping("/order/loadcart")
	@ResponseBody
	public Object getLoadcart() {
    	StringBuilder ListDetail = new StringBuilder();
		List<OrderDetailBean> orderdetailList = orderDetailRepository.getCartIsNull(settingController.currentUserId());
		for (OrderDetailBean o : orderdetailList) {
			ListDetail.append("<div class='row'>");
			ListDetail.append("<div class='col-md-7'>");
			ListDetail.append("<div class='row'>");
			ListDetail.append("<div class='col-md-1'>");
			ListDetail.append("<div class='buttons'><a class='btn icon btn-danger' onclick='delete_detail(" + o.getID_order_detail() + ")'><i data-feather='trash-2'></i></a></div>");
			ListDetail.append("</div>");
			ListDetail.append("<div class='col-md-11'>");
			ListDetail.append("<div class='fw-bold text-dark'>" + o.getp_name() + "</div>");
			ListDetail.append("<div class='text-muted small mt-1'>เว็บ : " + o.getw_name() +  " เวอร์ชั่น : " + o.getv_name() + " ปก : " + o.getc_name() + "</div>");
			ListDetail.append("</div>");
			ListDetail.append("</div>");
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
			ListDetail.append("<hr style='margin:10px;'>");
		}

    	Double sum_price_total = 0.0;
    	Double sum_price_pledge = 0.0;
    	Double sum_price_balance = 0.0;
		List<OrderSummaryBean> summayList = orderDetailRepository.getCartSummary(settingController.currentUserId());
		if (orderdetailList.size() > 0 ) {
			for (OrderSummaryBean s : summayList) {
				sum_price_total = s.getsum_price_total();
		    	sum_price_pledge = s.getsum_price_pledge();
		    	sum_price_balance = s.getsum_price_balance();
			}
		}
		
		String ListPaymentMethod = paymentMethodController.getDataList();
		

	    List<CustomerNameBean> mainCustomerName = orderRepository.getCsutomerList();
	    StringBuilder strCustomerName = new StringBuilder();
	    for (CustomerNameBean c : mainCustomerName) {
	    	strCustomerName.append("<option value='" + c.geto_customer_name() + "'>" + c.geto_customer_name() + "</option>");
	    }

	    String ListPaymentType = paymenttypeController.getDataList();

        Map<String, Object> response = new HashMap<>();
        response.put("listDetail", ListDetail.toString());
        response.put("total_price", sum_price_total);
        response.put("pledge_price", sum_price_pledge);
        response.put("balance_price", sum_price_balance);
        response.put("listPaymentMethod", ListPaymentMethod);
        response.put("listCustomerName", strCustomerName.toString());
	    response.put("listPaymentType", ListPaymentType);
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
    
    @PostMapping("/order/save")
    @ResponseBody
    @Transactional
    public synchronized ResponseEntity<?> saveOrderData(@RequestBody OrderDto orderDto) {
        try {
        	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        	
            LocalDate orderDate = parseRequiredOrderDate(orderDto.getOrder_date());
            Integer currentUserId = settingController.currentUserId();
            List<OrderDetail> orderDetail = orderDetailRepository.getDataIsNull(currentUserId);
            if (orderDetail == null || orderDetail.isEmpty()) {
                return ResponseEntity.badRequest().body("Cart is empty");
            }

            Order order = new Order();
            order.setOrder_date(java.sql.Date.valueOf(orderDate));
            order.setOrder_code(generateOrderCode(orderDate));
            order.setCustomer_name(orderDto.getCustomer_name());
            order.setPay_method(orderDto.getPay_method());
            order.setPay_type(orderDto.getPay_type());
        	if (orderDto.getPay_method() == 2) {
        		if (orderDto.getPay_type() == 2) {
                    order.setLast_pay_date(formatter.parse(orderDto.getLast_pay_date()));
        		}
        	}
            order.setSend_cost(orderDto.getSend_cost());
            order.setDiscount(orderDto.getDiscount());
            double cartProductTotal = getCartProductTotal();
            order.setPrice_total(cartProductTotal);
            if (orderDto.getPay_method() == 1) {
                order.setPrice_pledge(0);
                order.setPrice_balance(0);
            } else {
                order.setPrice_pledge(orderDto.getPrice_pledge());
                order.setPrice_balance(orderDto.getPrice_balance());
            }
            order.setNet(orderDto.getNet());
            order.setRemark(orderDto.getRemark());

            orderRepository.save(order);
            
            Integer newOrderId = order.getId();

            for (OrderDetail o : orderDetail) {
            	o.setOrder(newOrderId);
            }
            orderDetailRepository.saveAll(orderDetail);
            saveOrderIncome(order, orderDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "Success");
            response.put("orderId", newOrderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private synchronized String generateOrderCode(LocalDate orderDate) {
        String prefix = String.format("PR-%02d-", orderDate.getYear() % 100);
        Boolean locked = orderRepository.lockOrderCodeGeneration(prefix);
        if (!Boolean.TRUE.equals(locked)) {
            throw new IllegalStateException("Order code generation is locked. Please try again.");
        }
        Integer maxRunning = orderRepository.getMaxOrderCodeRunning(prefix);
        int nextRunning = (maxRunning == null ? 0 : maxRunning) + 1;
        return prefix + String.format("%06d", nextRunning);
    }

    private double getCartProductTotal() {
        List<OrderSummaryBean> summaryList = orderDetailRepository.getCartSummary(settingController.currentUserId());
        if (summaryList == null || summaryList.isEmpty()) {
            return 0;
        }
        return summaryList.get(0).getsum_price_total();
    }

    @PostMapping(value = "/order/receipt-preview", produces = "text/html; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<String> receiptPreview(@RequestBody OrderDto orderDto) {
        List<OrderDetailBean> detailList = orderDetailRepository.getCartIsNull(settingController.currentUserId());
        StringBuilder rows = new StringBuilder();
        int rowNumber = 0;
        for (OrderDetailBean detail : detailList) {
            rowNumber++;
            rows.append("<tr>");
            rows.append("<td>" + rowNumber + "</td>");
            rows.append("<td class='product-cell'>" + escapeHtml(detail.getp_name()) + "<div class='muted'>"
                    + escapeHtml(detail.geta_name()) + " / "
                    + escapeHtml(detail.getw_name()) + " / "
                    + escapeHtml(detail.getv_name()) + " / "
                    + escapeHtml(detail.getc_name()) + "</div></td>");
            rows.append("<td class='right'>" + toDisplay(detail.getod_qty()) + "</td>");
            rows.append("<td class='right'>" + formatMoney(detail.getod_price_total()) + "</td>");
            rows.append("<td class='right'>" + formatMoney(detail.getod_price_pledge()) + "</td>");
            rows.append("<td class='right'>" + formatMoney(detail.getod_price_balance()) + "</td>");
            rows.append("</tr>");
        }

        double productTotal = getCartProductTotal();
        double pledge = Integer.valueOf(1).equals(orderDto.getPay_method()) ? 0 : orderDto.getPrice_pledge();
        double balance = Integer.valueOf(1).equals(orderDto.getPay_method()) ? 0 : orderDto.getPrice_balance();
        String discountRow = "";
        if (BigDecimal.valueOf(orderDto.getDiscount()).compareTo(BigDecimal.ZERO) > 0) {
            discountRow = """
                        <div class="summary-row"><span>ส่วนลด</span><strong class="right">%s</strong></div>
                    """.formatted(formatMoney(orderDto.getDiscount()));
        }

        return ResponseEntity.ok("""
                <div class="receipt">
                    <div class="header">
                        <div class="receipt-brand">
                            <img class="receipt-logo" src="/mazer/dist/assets/images/logo/logo-web.png" alt="Sriundee Shop">
                            <div class="receipt-title">
                                <h1>ใบเสร็จรับเงิน</h1>
                                <div class="shop">Sriundee Shop</div>
                            </div>
                        </div>
                        <div class="meta">
                            <div><strong>เลขที่คำสั่งซื้อ:</strong> PREVIEW</div>
                            <div><strong>วันที่:</strong> %s</div>
                        </div>
                    </div>
                    <div class="info">
                        <div><strong>ลูกค้า:</strong> %s</div>
                        <div><strong>สถานะชำระเงิน:</strong> %s</div>
                        <div><strong>หมายเหตุ:</strong> %s</div>
                    </div>
                    <table>
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>สินค้า</th>
                                <th class="right">จำนวน</th>
                                <th class="right">ราคาเต็ม</th>
                                <th class="right">มัดจำ</th>
                                <th class="right">คงเหลือ</th>
                            </tr>
                        </thead>
                        <tbody>%s</tbody>
                    </table>
                    <div class="summary">
                        <div class="summary-row"><span>ราคาสินค้า</span><strong class="right">%s</strong></div>
                        <div class="summary-row"><span>ค่าส่ง</span><strong class="right">%s</strong></div>
                        %s
                        <div class="summary-row"><span>มัดจำ</span><strong class="right">%s</strong></div>
                        <div class="summary-row"><span>ยอดคงเหลือ</span><strong class="right">%s</strong></div>
                        <div class="summary-row total"><span>สุทธิ</span><strong class="right">%s</strong></div>
                    </div>
                </div>
                """.formatted(
                getReceiptDate(orderDto).format(DISPLAY_DATE_FORMAT),
                escapeHtml(orderDto.getCustomer_name()),
                escapeHtml(getPaymentMethodName(orderDto.getPay_method())),
                escapeHtml(orderDto.getRemark()),
                rows.toString(),
                formatMoney(productTotal),
                formatMoney(orderDto.getSend_cost()),
                discountRow,
                formatMoney(pledge),
                formatMoney(balance),
                formatMoney(orderDto.getNet())));
    }

    private String getPaymentMethodName(Integer payMethod) {
        if (Integer.valueOf(1).equals(payMethod)) {
            return "จ่ายเต็ม";
        }
        if (Integer.valueOf(2).equals(payMethod)) {
            return "มัดจำ";
        }
        return "";
    }

    private LocalDate parseRequiredOrderDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Order date is required");
        }
        return LocalDate.parse(value);
    }

    private LocalDate getReceiptDate(OrderDto orderDto) {
        try {
            return parseRequiredOrderDate(orderDto.getOrder_date());
        } catch (RuntimeException e) {
            return LocalDate.now();
        }
    }

    private String escapeHtml(Object value) {
        return toDisplay(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatMoney(String value) {
        if (value == null || value.isBlank()) {
            return "0.00";
        }
        try {
            return MONEY_FORMAT.format(new BigDecimal(value.replace(",", "")));
        } catch (Exception e) {
            return value;
        }
    }

    private void saveOrderIncome(Order order, LocalDate incomeDate) {
        Integer payMethod = order.getPay_method();
        if (Integer.valueOf(1).equals(payMethod)) {
            saveIncome(incomeDate, order.getCustomer_name(), 1, order.getNet(), "จ่ายเต็ม", order.getId());
        } else if (Integer.valueOf(2).equals(payMethod)) {
            saveIncome(incomeDate, order.getCustomer_name(), 2, order.getPrice_pledge(), "จ่ายมัดจำ", order.getId());
        }
    }

    private void saveIncome(LocalDate createDate, String customerName, Integer typeIncome, double price, String note, Integer orderId) {
        Income income = new Income();
        income.setCreateDate(createDate.toString());
        income.setCustomerName(customerName);
        income.setTypeIncome(typeIncome);
        income.setPrice(formatIncomePrice(price));
        income.setNote(note);
        income.setDelete("A");
        income.setOrder(orderId);
        incomeRepository.save(income);
    }

    private String formatIncomePrice(double price) {
        return BigDecimal.valueOf(price).stripTrailingZeros().toPlainString();
    }

    private String formatMoney(double value) {
        return MONEY_FORMAT.format(BigDecimal.valueOf(value));
    }

    private String toDisplay(Object value) {
        return value == null ? "" : value.toString();
    }

    private String displayScheduleDate(String value) {
        return value == null || value.isBlank() ? "ไม่มีกำหนด" : value;
    }

    private String getProductStatusTagClass(Integer statusId) {
        if (Integer.valueOf(1).equals(statusId)) {
            return "is-open";
        }
        if (Integer.valueOf(2).equals(statusId)) {
            return "is-closed";
        }
        return "is-unknown";
    }

    private String getDaysLeftLabel(String endDateValue) {
        if (endDateValue == null || endDateValue.isBlank()) {
            return "ไม่มีกำหนด";
        }
        try {
            LocalDate endDate = LocalDate.parse(endDateValue, DISPLAY_DATE_FORMAT);
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
            if (daysLeft < 0) {
                return "หมดเวลา";
            }
            if (daysLeft == 0) {
                return "วันนี้";
            }
            return "เหลือ " + daysLeft + " วัน";
        } catch (Exception e) {
            return "-";
        }
    }
}
