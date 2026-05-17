package com.sriundee.preorder.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.OrderDetailBean;
import com.sriundee.preorder.entity.Cost;
import com.sriundee.preorder.entity.CostDetail;
import com.sriundee.preorder.entity.Lot;
import com.sriundee.preorder.entity.LotDetail;
import com.sriundee.preorder.entity.OrderDetail;
import com.sriundee.preorder.repository.CostDetailRepository;
import com.sriundee.preorder.repository.CostRepository;
import com.sriundee.preorder.repository.LotDetailRepository;
import com.sriundee.preorder.repository.LotRepository;
import com.sriundee.preorder.repository.OrderDetailRepository;

import jakarta.transaction.Transactional;

@Controller
public class ChangeController {

	@Autowired
    private MenuController menuService;

	@Autowired
	private OrderStatusController orderStatusController;

	@Autowired
	private WebsiteController websiteController;

	@Autowired
	private OrderDetailRepository orderDetailRepository;

	@Autowired
	private CostRepository costRepository;

	@Autowired
	private CostDetailRepository costDetailRepository;

	@Autowired
	private LotRepository lotRepository;

	@Autowired
	private LotDetailRepository lotDetailRepository;
	
    @GetMapping("/change")
    public String index(Model model) {
		String menuList = menuService.getMenuList(8,null);
	    model.addAttribute("mainMenus", menuList);

		String osCheckList = orderStatusController.getDataCheckList();
	    model.addAttribute("osCheckList", osCheckList);
	    model.addAttribute("orderStatusList", orderStatusController.getDataList());
	    model.addAttribute("websiteList", buildWebsiteFilterOptions());

	    model.addAttribute("listOrderDeatil", buildOrderDetailRows(null, null, null));
	    
        return "change";
    }

    @GetMapping("/change/search")
    @ResponseBody
    public ResponseEntity<String> search(
    		@RequestParam(value = "orderStatus", required = false) Integer orderStatus,
    		@RequestParam(value = "artist", required = false) String artist,
    		@RequestParam(value = "website", required = false) Integer website) {
    	return ResponseEntity.ok(buildOrderDetailRows(orderStatus, artist, website));
    }

    @PostMapping("/change/status/update")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> updateStatus(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) payload.get("ids");
            Integer orderStatus = toInteger(payload.get("orderStatus"));

            if (ids == null || ids.isEmpty() || orderStatus == null) {
                return ResponseEntity.badRequest().body("Invalid data");
            }

            List<OrderDetail> orderDetails = orderDetailRepository.findAllById(ids);
            boolean waitingPress = orderDetails.stream().anyMatch(orderDetail -> Integer.valueOf(1).equals(orderDetail.getOrder_status()));
            if (waitingPress && !Integer.valueOf(2).equals(orderStatus)) {
                return ResponseEntity.badRequest().body("Waiting press status can only change to pressed status");
            }

            boolean pressed = orderDetails.stream().anyMatch(orderDetail -> Integer.valueOf(2).equals(orderDetail.getOrder_status()));
            if (pressed && !Integer.valueOf(3).equals(orderStatus)) {
                return ResponseEntity.badRequest().body("Pressed status can only change to warehouse waiting status");
            }

            boolean waitingStore = orderDetails.stream().anyMatch(orderDetail -> Integer.valueOf(3).equals(orderDetail.getOrder_status()));
            if (waitingStore && !Integer.valueOf(4).equals(orderStatus)) {
                return ResponseEntity.badRequest().body("Waiting store status can only change to arrived store status");
            }

            String costPrice = toStringValue(payload.get("costPrice"));
            if (waitingPress && costPrice.isBlank()) {
                return ResponseEntity.badRequest().body("Cost price is required");
            }

            String lotNumber = toStringValue(payload.get("l_lot_number"));
            if (pressed && lotNumber.isBlank()) {
                return ResponseEntity.badRequest().body("Lot number is required");
            }

            String shippingPrice = toStringValue(payload.get("shippingPrice"));
            if (waitingStore && shippingPrice.isBlank()) {
                return ResponseEntity.badRequest().body("Shipping price is required");
            }

            for (OrderDetail orderDetail : orderDetails) {
                orderDetail.setOrder_status(orderStatus);
            }
            orderDetailRepository.saveAll(orderDetails);

            if (waitingPress) {
            	Cost cost = new Cost();
            	cost.setCreate_date(LocalDate.now().toString());
            	cost.setType_cost(1);
            	cost.setPrice(costPrice);
            	cost.setNote(toStringValue(payload.get("costNote")));
            	cost.setDelete("A");
            	costRepository.save(cost);

            	List<CostDetail> costDetails = orderDetails.stream().map(orderDetail -> {
            		CostDetail costDetail = new CostDetail();
            		costDetail.setCost(cost.getId());
            		costDetail.setOrder_detail(orderDetail.getId());
            		return costDetail;
            	}).toList();
            	costDetailRepository.saveAll(costDetails);
            }

            if (waitingStore) {
            	Cost cost = new Cost();
            	cost.setCreate_date(LocalDate.now().toString());
            	cost.setType_cost(2);
            	cost.setPrice(shippingPrice);
            	cost.setNote(toStringValue(payload.get("shippingNote")));
            	cost.setDelete("A");
            	costRepository.save(cost);

            	List<CostDetail> costDetails = orderDetails.stream().map(orderDetail -> {
            		CostDetail costDetail = new CostDetail();
            		costDetail.setCost(cost.getId());
            		costDetail.setOrder_detail(orderDetail.getId());
            		return costDetail;
            	}).toList();
            	costDetailRepository.saveAll(costDetails);
            }

            if (pressed) {
            	Lot lot = lotRepository.getActiveByLotNumber(lotNumber);
            	if (lot == null) {
            		lot = new Lot();
            		lot.setLot_number(lotNumber);
            		lot.setCreate_date(LocalDate.now().toString());
            		lot.setDelete("A");
            		lotRepository.save(lot);
            	}

            	final Integer lotId = lot.getId();
            	List<LotDetail> lotDetails = orderDetails.stream().map(orderDetail -> {
            		LotDetail lotDetail = new LotDetail();
            		lotDetail.setLot(lotId);
            		lotDetail.setOrder_detail(orderDetail.getId());
            		return lotDetail;
            	}).toList();
            	lotDetailRepository.saveAll(lotDetails);
            }

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private Integer toInteger(Object value) {
    	if (value instanceof Number number) {
    		return number.intValue();
    	}
    	if (value instanceof String string && !string.isBlank()) {
    		return Integer.parseInt(string);
    	}
    	return null;
    }

    private String toStringValue(Object value) {
    	return value == null ? "" : value.toString();
    }

    private String buildWebsiteFilterOptions() {
    	return "<option value=''>ทั้งหมด</option>" + websiteController.getDataList();
    }

    private String buildOrderDetailRows(Integer orderStatus, String artist, Integer website) {
    	StringBuilder ListDetail = new StringBuilder();
		List<OrderDetailBean> orderdetailList = orderDetailRepository.getDataByFilter(orderStatus, artist, website);
		for (OrderDetailBean od : orderdetailList) {
			ListDetail.append("<tr class='change-selectable-row' onclick='toggle_row_from_click(this, event)'>");
			ListDetail.append("<td><input class='form-check-input row-check' type='checkbox' value='" + od.getID_order_detail() + "' data-status-id='" + od.getID_order_status() + "' data-status-name='" + od.getos_name() + "' onclick='event.stopPropagation()' onchange='toggle_row_check(this)'></td>");
			ListDetail.append("<td>" + od.geto_customer_name() + "</td>");
			ListDetail.append("<td>" + od.geta_name() + "</td>");
			ListDetail.append("<td>" + od.getp_name() + "</td>");
			ListDetail.append("<td>" + od.getw_name() + "</td>");
			ListDetail.append("<td>" + od.getv_name() + "</td>");
			ListDetail.append("<td>" + od.getc_name() + "</td>");
			ListDetail.append("<td>" + od.getod_qty() + "</td>");
			ListDetail.append("<td>");
		    ListDetail.append("<span class='badge order-status-badge bg-" + od.getos_color() + "'>");
		    ListDetail.append(od.getos_name());
		    ListDetail.append("</span>");
			ListDetail.append("</td>");
			ListDetail.append("</tr>");
		}
		
	    return ListDetail.toString();
    }
}
