package com.sriundee.preorder.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.CostDetailBean;
import com.sriundee.preorder.bean.CostPressBean;
import com.sriundee.preorder.entity.Cost;
import com.sriundee.preorder.entity.OrderDetail;
import com.sriundee.preorder.repository.CostDetailRepository;
import com.sriundee.preorder.repository.CostRepository;
import com.sriundee.preorder.repository.OrderDetailRepository;

import jakarta.transaction.Transactional;

@Controller
public class CostPressController {

	private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,##0.00");
	private static final String DEFAULT_START_DATE = "2026-01-01";
	private static final String DEFAULT_END_DATE = "2026-12-31";

	@Autowired
    private MenuController menuService;

	@Autowired
	private CostRepository costRepository;

	@Autowired
	private CostDetailRepository costDetailRepository;

	@Autowired
	private OrderDetailRepository orderDetailRepository;

	@GetMapping("/check_status_2")
	public String index(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "status", required = false) String status,
			Model model) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
		String menuList = menuService.getMenuList(9, null);
	    model.addAttribute("mainMenus", menuList);
	    model.addAttribute("mainCostPress", buildCostRows(dateRange.startDate(), dateRange.endDate(), status));
	    model.addAttribute("startDate", dateRange.startDate());
	    model.addAttribute("endDate", dateRange.endDate());
	    model.addAttribute("status", toDisplay(status));
	    return "cost/press";
	}

	@GetMapping("/cost/press/detail/{id}")
	@ResponseBody
	public ResponseEntity<String> getDetail(@PathVariable Integer id) {
		return ResponseEntity.ok(buildDetailRows(id));
	}

	@GetMapping("/cost/press/search")
	@ResponseBody
	public ResponseEntity<String> search(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "status", required = false) String status) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
		return ResponseEntity.ok(buildCostRows(dateRange.startDate(), dateRange.endDate(), status));
	}

	@PostMapping("/cost/press/cancel/{id}")
	@ResponseBody
	@Transactional
	public ResponseEntity<String> cancel(@PathVariable Integer id) {
		try {
			Cost cost = costRepository.findById(id).orElseThrow(() -> new RuntimeException("Cost not found"));
			if (!"A".equals(cost.getDelete()) || !canCancelCost(id)) {
				return ResponseEntity.badRequest().body("Cannot cancel cost press after order status moved");
			}

			cost.setDelete("D");
			costRepository.save(cost);

			List<Integer> orderDetailIds = costDetailRepository.getDataByCost(id).stream()
					.map(CostDetailBean::getID_order_detail)
					.toList();
			List<OrderDetail> orderDetails = orderDetailRepository.findAllById(orderDetailIds);
			for (OrderDetail orderDetail : orderDetails) {
				orderDetail.setOrder_status(1);
			}
			orderDetailRepository.saveAll(orderDetails);

			return ResponseEntity.ok("Success");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}

	private String buildCostRows(String startDate, String endDate, String status) {
		List<CostPressBean> costList = costRepository.getPressCostAll(startDate, endDate, status);
		StringBuilder strCost = new StringBuilder();
		Integer row_id = 0;
		for (CostPressBean cost : costList) {
			row_id += 1;
			strCost.append("<tr class='cost-press-row' onclick='open_cost_detail(" + cost.getID_cost() + ")'>");
			strCost.append("<td class='cost-cancel-col'>" + buildCancelButton(cost, canCancelCost(cost.getID_cost())) + "</td>");
			strCost.append("<td class='cost-date-col'>" + formatDate(cost.getc_create_date()) + "</td>");
			strCost.append("<td class='cost-price-col text-end'>" + formatMoney(cost.getc_price()) + "</td>");
			strCost.append("<td>" + toDisplay(cost.getc_note()) + "</td>");
			strCost.append("<td>" + buildStatusBadge(cost.getc_delete()) + "</td>");
			strCost.append("</tr>");
		}
		return strCost.toString();
	}

	private DateRange defaultDateRange(String startDate, String endDate) {
		return new DateRange(
				startDate == null || startDate.isBlank() ? DEFAULT_START_DATE : startDate,
				endDate == null || endDate.isBlank() ? DEFAULT_END_DATE : endDate);
	}

	private record DateRange(String startDate, String endDate) {
	}

	private String buildDetailRows(Integer id) {
		List<CostDetailBean> detailList = costDetailRepository.getDataByCost(id);
		StringBuilder strDetail = new StringBuilder();
		Integer row_id = 0;
		DetailSummary summary = new DetailSummary();
		for (CostDetailBean detail : detailList) {
			row_id += 1;
			summary.add(detail.getID_pay_method(), detail.getod_price_total(), detail.getod_price_pledge(), detail.getod_price_balance());
			strDetail.append("<tr>");
			strDetail.append("<td>" + row_id + "</td>");
			strDetail.append("<td>" + toDisplay(detail.geto_customer_name()) + "</td>");
			strDetail.append("<td>" + toDisplay(detail.geta_name()) + "</td>");
			strDetail.append("<td>" + toDisplay(detail.getp_name()) + "</td>");
			strDetail.append("<td>" + toDisplay(detail.getw_name()) + "</td>");
			strDetail.append("<td>" + toDisplay(detail.getv_name()) + "</td>");
			strDetail.append("<td>" + toDisplay(detail.getc_name()) + "</td>");
			strDetail.append("<td class='text-end'>" + toDisplay(detail.getod_qty()) + "</td>");
			strDetail.append("<td class='text-end'>" + formatMoney(unitPrice(detail.getod_price_total(), detail.getod_qty())) + "</td>");
			strDetail.append("<td class='text-end'>" + formatMoney(unitPrice(detail.getod_price_pledge(), detail.getod_qty())) + "</td>");
			strDetail.append("<td>" + buildPaymentBadge(detail.getID_pay_method()) + "</td>");
			strDetail.append("</tr>");
		}

		if (detailList.isEmpty()) {
			strDetail.append("<tr><td colspan='11' class='text-center text-muted'>ไม่พบรายละเอียดสินค้า</td></tr>");
		} else {
			strDetail.append(buildSummaryRow(summary));
		}
		return strDetail.toString();
	}

	private String buildPaymentBadge(Integer payMethod) {
		if (Integer.valueOf(1).equals(payMethod)) {
			return "<span class='badge bg-info detail-payment-badge'>จ่ายเต็ม</span>";
		}
		if (Integer.valueOf(3).equals(payMethod)) {
			return "<span class='badge bg-success detail-payment-badge'>แบ่งชำระ</span>";
		}
		return "<span class='badge bg-warning detail-payment-badge'>มัดจำ</span>";
	}

	private String buildSummaryRow(DetailSummary summary) {
		StringBuilder row = new StringBuilder();
		row.append("<tr><td colspan='11' class='detail-summary-cell'>");
		row.append("<div class='detail-summary-grid'>");
		row.append(buildSummaryBox("มูลค่าสินค้ารวม", summary.totalProduct, "summary-total"));
		row.append(buildSummaryBox("มูลค่าที่จ่ายเต็ม", summary.fullPaid, "summary-full"));
		row.append(buildSummaryBox("มูลค่าที่จ่ายมัดจำ", summary.pledgePaid, "summary-pledge"));
		row.append(buildSummaryBox("ยอดที่เหลือ", summary.balance, "summary-balance"));
		row.append("</div>");
		row.append("</td></tr>");
		return row.toString();
	}

	private String buildSummaryBox(String label, BigDecimal value, String cssClass) {
		return "<div class='detail-summary-box " + cssClass + "'>"
				+ "<span class='detail-summary-label'>" + label + "</span>"
				+ "<span class='detail-summary-value'>" + MONEY_FORMAT.format(value) + "</span>"
				+ "<span class='detail-summary-unit'>บาท</span>"
				+ "</div>";
	}

	private String buildStatusBadge(String status) {
		if ("A".equals(status)) {
			return "<span class='badge bg-success cost-status-badge'>สำเร็จ</span>";
		}
		if ("D".equals(status)) {
			return "<span class='badge bg-danger cost-status-badge'>ยกเลิก</span>";
		}
		return "<span class='badge bg-secondary cost-status-badge'>ไม่ระบุ</span>";
	}

	private String buildCancelButton(CostPressBean cost, boolean canCancel) {
		if ("D".equals(cost.getc_delete()) || !canCancel) {
			return "<button type='button' class='btn icon btn-secondary' disabled><i data-feather='x-circle'></i></button>";
		}
		return "<button type='button' class='btn icon btn-danger' onclick='cancel_cost_press(" + cost.getID_cost() + ", event)'><i data-feather='x-circle'></i></button>";
	}

	private boolean canCancelCost(Integer costId) {
		Integer detailCount = costDetailRepository.countDataByCost(costId);
		Integer notPressedCount = costDetailRepository.countNotPressedByCost(costId);
		return detailCount != null && detailCount > 0 && (notPressedCount == null || notPressedCount == 0);
	}

	private String formatDate(String date) {
		if (date == null || date.isBlank()) {
			return "";
		}
		try {
			return LocalDate.parse(date).format(DISPLAY_DATE_FORMAT);
		} catch (Exception e) {
			return date;
		}
	}

	private String formatMoney(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		try {
			return MONEY_FORMAT.format(new BigDecimal(value.replace(",", "")));
		} catch (Exception e) {
			return value;
		}
	}

	private String formatMoney(BigDecimal value) {
		return MONEY_FORMAT.format(value);
	}

	private String toDisplay(Object value) {
		return value == null ? "" : value.toString();
	}

	private BigDecimal parseMoney(String value) {
		if (value == null || value.isBlank()) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(value.replace(",", ""));
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private BigDecimal unitPrice(String total, Object qtyValue) {
		BigDecimal qty = parseMoney(toDisplay(qtyValue));
		if (qty.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return parseMoney(total).divide(qty, 2, java.math.RoundingMode.HALF_UP);
	}

	private class DetailSummary {
		private BigDecimal totalProduct = BigDecimal.ZERO;
		private BigDecimal fullPaid = BigDecimal.ZERO;
		private BigDecimal pledgePaid = BigDecimal.ZERO;
		private BigDecimal balance = BigDecimal.ZERO;

		private void add(Integer payMethod, String totalValue, String pledgeValue, String balanceValue) {
			BigDecimal total = parseMoney(totalValue);
			BigDecimal pledge = parseMoney(pledgeValue);
			BigDecimal remaining = parseMoney(balanceValue);
			totalProduct = totalProduct.add(total);
			if (Integer.valueOf(1).equals(payMethod) || Integer.valueOf(3).equals(payMethod)) {
				fullPaid = fullPaid.add(total);
			} else {
				pledgePaid = pledgePaid.add(pledge);
				balance = balance.add(remaining);
			}
		}
	}
}
