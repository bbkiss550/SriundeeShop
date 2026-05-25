package com.sriundee.preorder.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sriundee.preorder.bean.LotBean;
import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.repository.LotRepository;
import com.sriundee.preorder.repository.ProductRepository;

@Controller
public class ScheduleCalendarController {

	private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final int MENU_ID = 14;

	@Autowired
	private MenuController menuService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private LotRepository lotRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SettingController settingController;

	@GetMapping("/schedule-calendar")
	public String index(Model model) {
		List<ProductBean> products = productRepository.getScheduleProducts();
		List<LotBean> lots = lotRepository.getActiveScheduleLots();
		YearMonth initialMonth = getInitialMonth(products, lots);
		model.addAttribute("mainMenus", menuService.getMenuList(MENU_ID, null));
		model.addAttribute("scheduleEvents", toJson(buildEvents(products, lots)));
		model.addAttribute("initialMonth", initialMonth.toString());
		model.addAttribute("showCompletedSchedule", settingController.getScheduleShowCompletedValue());
		return "schedule/calendar";
	}

	private YearMonth getInitialMonth(List<ProductBean> products, List<LotBean> lots) {
		YearMonth currentMonth = YearMonth.now();
		List<LocalDate> eventDates = new ArrayList<>();
		products.forEach(product -> eventDates.addAll(getProductDates(product)));
		lots.forEach(lot -> {
			LocalDate startDate = parseIsoDate(lot.getl_start_date());
			LocalDate endDate = parseIsoDate(lot.getl_end_date());
			if (startDate != null) {
				eventDates.add(startDate);
			}
			if (endDate != null) {
				eventDates.add(endDate);
			}
		});
		boolean hasCurrentMonthEvent = eventDates.stream()
				.anyMatch(date -> YearMonth.from(date).equals(currentMonth));
		if (hasCurrentMonthEvent) {
			return currentMonth;
		}
		return eventDates.stream()
				.map(YearMonth::from)
				.findFirst()
				.orElse(currentMonth);
	}

	private List<LocalDate> getProductDates(ProductBean product) {
		List<LocalDate> dates = new ArrayList<>();
		LocalDate endDate = parseDate(product.getp_end_date());
		LocalDate sendDate = parseDate(product.getp_send_date());
		if (endDate != null) {
			dates.add(endDate);
		}
		if (sendDate != null) {
			dates.add(sendDate);
		}
		return dates;
	}

	private List<Map<String, Object>> buildEvents(List<ProductBean> products, List<LotBean> lots) {
		List<Map<String, Object>> events = new ArrayList<>();
		for (ProductBean product : products) {
			addEvent(events, product, product.getp_end_date(), "close", "ปิดพรี");
			addEvent(events, product, product.getp_send_date(), "send", "กำหนดส่ง");
		}
		for (LotBean lot : lots) {
			addLotRangeEvent(events, lot);
		}
		return events;
	}

	private void addLotRangeEvent(List<Map<String, Object>> events, LotBean lot) {
		LocalDate arriveDate = parseIsoDate(lot.getl_arrive_date());
		if (arriveDate != null) {
			addLotArrivedEvent(events, lot, arriveDate);
			return;
		}

		LocalDate startDate = parseIsoDate(lot.getl_start_date());
		LocalDate endDate = parseIsoDate(lot.getl_end_date());
		if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
			return;
		}
		Map<String, Object> event = new HashMap<>();
		event.put("date", startDate.toString());
		event.put("startDate", startDate.toString());
		event.put("endDate", endDate.toString());
		event.put("displayDate", startDate.format(DISPLAY_DATE_FORMAT));
		event.put("displayEndDate", endDate.format(DISPLAY_DATE_FORMAT));
		event.put("type", "arrivalRange");
		event.put("label", "คาดว่าจะถึงร้าน");
		event.put("productName", "LOT " + toDisplay(lot.getl_lot_number()));
		event.put("artistName", "จำนวนรายการ " + (lot.getdetail_count() == null ? 0 : lot.getdetail_count()));
		event.put("productStatus", "รอของถึงร้าน");
		event.put("completed", false);
		event.put("details", getLotOrderGroups(lot.getID_lot()));
		events.add(event);
	}

	private void addLotArrivedEvent(List<Map<String, Object>> events, LotBean lot, LocalDate arriveDate) {
		Map<String, Object> event = new HashMap<>();
		event.put("date", arriveDate.toString());
		event.put("displayDate", arriveDate.format(DISPLAY_DATE_FORMAT));
		event.put("type", "lotArrived");
		event.put("label", "ถึงไทย");
		event.put("productName", "LOT " + toDisplay(lot.getl_lot_number()));
		event.put("artistName", "จำนวนรายการ " + (lot.getdetail_count() == null ? 0 : lot.getdetail_count()));
		event.put("productStatus", "ถึงไทยแล้ว");
		event.put("completed", true);
		event.put("details", getLotOrderGroups(lot.getID_lot()));
		events.add(event);
	}

	private void addEvent(List<Map<String, Object>> events, ProductBean product, String dateText, String type, String label) {
		LocalDate date = parseDate(dateText);
		if (date == null) {
			return;
		}
		Map<String, Object> event = new HashMap<>();
		event.put("date", date.toString());
		event.put("displayDate", date.format(DISPLAY_DATE_FORMAT));
		event.put("type", type);
		event.put("label", label);
		event.put("productName", toDisplay(product.getp_name()));
		event.put("artistName", toDisplay(product.geta_name()));
		event.put("productStatus", toDisplay(product.getps_name()));
		event.put("completed", isCompletedProductEvent(product, date, type));
		event.put("details", getOrderGroups(product.getID_product()));
		events.add(event);
	}

	private boolean isCompletedProductEvent(ProductBean product, LocalDate date, String type) {
		if ("close".equals(type)) {
			return Integer.valueOf(2).equals(product.getID_pro_status());
		}
		return "send".equals(type) && date.isBefore(LocalDate.now());
	}

	private List<Map<String, Object>> getOrderGroups(Integer productId) {
		if (productId == null) {
			return List.of();
		}
		return jdbcTemplate.queryForList("""
				SELECT COALESCE(w_name, 'ไม่ระบุ') AS website,
				       COALESCE(v_name, 'ไม่ระบุ') AS version,
				       COALESCE(c_name, 'ไม่ระบุ') AS cover,
				       COALESCE(SUM(od_qty), 0) AS qty
				FROM q_order_detail
				WHERE ID_pro = ?
				GROUP BY ID_web, w_name, ID_ver, v_name, ID_cover, c_name
				ORDER BY w_name, v_name, c_name
				""", productId);
	}

	private List<Map<String, Object>> getLotOrderGroups(Integer lotId) {
		if (lotId == null) {
			return List.of();
		}
		return jdbcTemplate.queryForList("""
				SELECT COALESCE(q.p_name, 'ไม่ระบุ') AS product,
				       COALESCE(q.w_name, 'ไม่ระบุ') AS website,
				       COALESCE(q.v_name, 'ไม่ระบุ') AS version,
				       COALESCE(q.c_name, 'ไม่ระบุ') AS cover,
				       COALESCE(SUM(q.od_qty), 0) AS qty
				FROM t_lot_detail ld
				JOIN q_order_detail q ON q.ID_order_detail = ld.ID_order_detail
				WHERE ld.ID_lot = ?
				GROUP BY q.ID_pro, q.p_name, q.ID_web, q.w_name, q.ID_ver, q.v_name, q.ID_cover, q.c_name
				ORDER BY q.p_name, q.w_name, q.v_name, q.c_name
				""", lotId);
	}

	private LocalDate parseDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(value, DISPLAY_DATE_FORMAT);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private LocalDate parseIsoDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private String toDisplay(String value) {
		return value == null ? "" : value;
	}

	private String toJson(Object value) {
		try {
			return new ObjectMapper().writeValueAsString(value);
		} catch (JsonProcessingException e) {
			return "[]";
		}
	}
}
