package com.sriundee.preorder.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
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
    		@RequestParam(value = "website", required = false) Integer website,
    		@RequestParam(value = "lot", required = false) String lot) {
    	return ResponseEntity.ok(buildOrderDetailRows(orderStatus, artist, website, lot));
    }

    @PostMapping("/change/status/update")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> updateStatus(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) payload.get("ids");
            Integer orderStatus = toInteger(payload.get("orderStatus"));
            LocalDate recordDate = parseDate(toStringValue(payload.get("recordDate")));

            if (ids == null || ids.isEmpty() || orderStatus == null || recordDate == null) {
                return ResponseEntity.badRequest().body("Invalid data");
            }

            List<OrderDetail> orderDetails = orderDetailRepository.findAllById(ids);
            boolean sent = orderDetails.stream().anyMatch(orderDetail -> Integer.valueOf(5).equals(orderDetail.getOrder_status()));
            if (sent) {
                return ResponseEntity.badRequest().body("Sent status cannot be changed");
            }

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

            boolean arrivedStore = orderDetails.stream().anyMatch(orderDetail -> Integer.valueOf(4).equals(orderDetail.getOrder_status()));
            if (arrivedStore && !Integer.valueOf(5).equals(orderStatus)) {
                return ResponseEntity.badRequest().body("Arrived store status can only change to sent status");
            }

            String costPrice = toStringValue(payload.get("costPrice"));
            if (waitingPress && costPrice.isBlank()) {
                return ResponseEntity.badRequest().body("Cost price is required");
            }

            String lotNumber = toStringValue(payload.get("l_lot_number"));
            if (pressed && lotNumber.isBlank()) {
                return ResponseEntity.badRequest().body("Lot number is required");
            }
            String lotStartDate = toStringValue(payload.get("l_start_date"));
            String lotEndDate = toStringValue(payload.get("l_end_date"));
            if (pressed && (lotStartDate.isBlank() || lotEndDate.isBlank())) {
                return ResponseEntity.badRequest().body("Lot expected arrival date range is required");
            }
            if (pressed) {
            	LocalDate startDate = parseDate(lotStartDate);
            	LocalDate endDate = parseDate(lotEndDate);
            	if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            		return ResponseEntity.badRequest().body("Invalid lot expected arrival date range");
            	}
            }

            String shippingPrice = toStringValue(payload.get("shippingPrice"));
            if (waitingStore && shippingPrice.isBlank()) {
                return ResponseEntity.badRequest().body("Shipping price is required");
            }
            String lotArriveDate = toStringValue(payload.get("l_arrive_date"));
            if (waitingStore && lotArriveDate.isBlank()) {
                return ResponseEntity.badRequest().body("Lot arrive date is required");
            }
            if (waitingStore && parseDate(lotArriveDate) == null) {
                return ResponseEntity.badRequest().body("Invalid lot arrive date");
            }

            String postalPrice = toStringValue(payload.get("postalPrice"));
            if (arrivedStore && postalPrice.isBlank()) {
                return ResponseEntity.badRequest().body("Postal price is required");
            }

            for (OrderDetail orderDetail : orderDetails) {
                orderDetail.setOrder_status(orderStatus);
            }
            orderDetailRepository.saveAll(orderDetails);

            if (waitingPress) {
            	Cost cost = new Cost();
            	cost.setCreate_date(recordDate.toString());
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
            	saveCost(orderDetails, 2, shippingPrice, toStringValue(payload.get("shippingNote")), recordDate);
            	updateLotArriveDate(ids, lotArriveDate);
            }

            if (arrivedStore) {
            	saveCost(orderDetails, 5, postalPrice, toStringValue(payload.get("postalNote")), recordDate);
            }

            if (pressed) {
            	Lot lot = lotRepository.getActiveByLotNumber(lotNumber);
            	if (lot == null) {
            		lot = new Lot();
            		lot.setLot_number(lotNumber);
            		lot.setCreate_date(recordDate.toString());
            	}
            	lot.setStart_date(lotStartDate);
            	lot.setEnd_date(lotEndDate);
            	lot.setDelete("A");
            	lotRepository.save(lot);

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

    private LocalDate parseDate(String value) {
    	if (value == null || value.isBlank()) {
    		return null;
    	}
    	try {
    		return LocalDate.parse(value);
    	} catch (RuntimeException e) {
    		return null;
    	}
    }

    private void saveCost(List<OrderDetail> orderDetails, Integer typeCost, String price, String note, LocalDate recordDate) {
    	Cost cost = new Cost();
    	cost.setCreate_date(recordDate.toString());
    	cost.setType_cost(typeCost);
    	cost.setPrice(price);
    	cost.setNote(note);
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

    private void updateLotArriveDate(List<Integer> orderDetailIds, String arriveDate) {
    	if (orderDetailIds == null || orderDetailIds.isEmpty()) {
    		return;
    	}
    	String placeholders = orderDetailIds.stream().map(id -> "?").collect(Collectors.joining(","));
    	jdbcTemplate.update("""
    			UPDATE t_lot l
    			SET l.l_arrive_date = ?
    			FROM t_lot_detail ld
    			WHERE l.l_delete = 'A'
    			  AND ld.ID_lot = l.ID_lot
    			  AND ld.ID_order_detail IN (%s)
    			""".formatted(placeholders), buildArriveDateParams(orderDetailIds, arriveDate));
    }

    private Object[] buildArriveDateParams(List<Integer> orderDetailIds, String arriveDate) {
    	List<Object> params = new java.util.ArrayList<>();
    	params.add(arriveDate);
    	params.addAll(orderDetailIds);
    	return params.toArray();
    }

    private String buildWebsiteFilterOptions() {
    	return "<option value=''>ทั้งหมด</option>" + websiteController.getDataList();
    }

    private String buildOrderDetailRows(Integer orderStatus, String artist, Integer website) {
    	return buildOrderDetailRows(orderStatus, artist, website, null);
    }

    private String buildOrderDetailRows(Integer orderStatus, String artist, Integer website, String lot) {
    	StringBuilder ListDetail = new StringBuilder();
		List<OrderDetailBean> orderdetailList = orderDetailRepository.getDataByFilter(orderStatus, artist, website, lot);
		Map<Integer, String> lotNumbersByOrderDetail = getLotNumbersByOrderDetail(orderdetailList);
		for (OrderDetailBean od : orderdetailList) {
			boolean sent = Integer.valueOf(5).equals(od.getID_order_status());
			ListDetail.append("<tr class='" + (sent ? "change-locked-row" : "change-selectable-row") + "'" + (sent ? "" : " onclick='toggle_row_from_click(this, event)'") + ">");
			ListDetail.append("<td>");
			if (sent) {
				ListDetail.append("<input class='form-check-input row-check' type='checkbox' value='" + od.getID_order_detail() + "' data-status-id='" + od.getID_order_status() + "' data-status-name='" + od.getos_name() + "' disabled title='จัดส่งสำเร็จแล้ว ไม่สามารถแก้ไขสถานะได้'>");
			} else {
				ListDetail.append("<input class='form-check-input row-check' type='checkbox' value='" + od.getID_order_detail() + "' data-status-id='" + od.getID_order_status() + "' data-status-name='" + od.getos_name() + "' onclick='event.stopPropagation()' onchange='toggle_row_check(this)'>");
			}
			ListDetail.append("</td>");
			ListDetail.append("<td>" + od.geto_customer_name() + "</td>");
			ListDetail.append("<td>" + od.geta_name() + "</td>");
			ListDetail.append("<td>" + od.getp_name() + "</td>");
			ListDetail.append("<td>" + od.getw_name() + "</td>");
			ListDetail.append("<td>" + od.getv_name() + "</td>");
			ListDetail.append("<td>" + od.getc_name() + "</td>");
			ListDetail.append("<td>" + od.getod_qty() + "</td>");
			ListDetail.append("<td>" + buildLotBadge(lotNumbersByOrderDetail.get(od.getID_order_detail())) + "</td>");
			ListDetail.append("<td>");
		    ListDetail.append("<span class='badge order-status-badge bg-" + od.getos_color() + "'>");
		    ListDetail.append(od.getos_name());
		    ListDetail.append("</span>");
			ListDetail.append("</td>");
			ListDetail.append("</tr>");
		}
		
	    return ListDetail.toString();
    }

	private Map<Integer, String> getLotNumbersByOrderDetail(List<OrderDetailBean> orderdetailList) {
		if (orderdetailList == null || orderdetailList.isEmpty()) {
			return Map.of();
		}
		List<Integer> ids = orderdetailList.stream()
				.map(OrderDetailBean::getID_order_detail)
				.toList();
		String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
		List<Object> params = ids.stream().map(id -> (Object) id).toList();
		Map<Integer, String> lotMap = new HashMap<>();
		jdbcTemplate.query("""
				SELECT ld.ID_order_detail,
				       STRING_AGG(DISTINCT l.l_lot_number, ', ') AS lot_numbers
				FROM t_lot_detail ld
				JOIN t_lot l ON l.ID_lot = ld.ID_lot
				WHERE l.l_delete = 'A'
				  AND ld.ID_order_detail IN (%s)
				GROUP BY ld.ID_order_detail
				""".formatted(placeholders), rs -> {
			lotMap.put(rs.getInt("ID_order_detail"), rs.getString("lot_numbers"));
		}, params.toArray());
		return lotMap;
	}

	private String buildLotBadge(String lotNumbers) {
		if (lotNumbers == null || lotNumbers.isBlank()) {
			return "";
		}
		return "<span class='badge bg-success lot-number-badge'>LOT " + lotNumbers + "</span>";
	}
}
