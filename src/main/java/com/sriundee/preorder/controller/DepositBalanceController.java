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

import com.sriundee.preorder.bean.OrderDetailBean;
import com.sriundee.preorder.bean.OrderListBean;
import com.sriundee.preorder.entity.Income;
import com.sriundee.preorder.repository.IncomeRepository;
import com.sriundee.preorder.repository.OrderDetailRepository;
import com.sriundee.preorder.repository.OrderRepository;

import jakarta.transaction.Transactional;

@Controller
public class DepositBalanceController {

	private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,##0.00");
	private static final String DEFAULT_START_DATE = "2026-01-01";
	private static final String DEFAULT_END_DATE = "2026-12-31";

	@Autowired
	private MenuController menuService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderDetailRepository orderDetailRepository;

	@Autowired
	private IncomeRepository incomeRepository;

	@GetMapping("/deposit-balance")
	public String index(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "customerName", required = false) String customerName,
			Model model) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
		model.addAttribute("mainMenus", menuService.getMenuList(13, null));
		model.addAttribute("depositRows", buildDepositRows(dateRange.startDate(), dateRange.endDate(), customerName));
		model.addAttribute("startDate", dateRange.startDate());
		model.addAttribute("endDate", dateRange.endDate());
		model.addAttribute("customerName", toDisplay(customerName));
		return "deposit/balance";
	}

	@GetMapping("/deposit-balance/search")
	@ResponseBody
	public ResponseEntity<String> search(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "customerName", required = false) String customerName) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
		return ResponseEntity.ok(buildDepositRows(dateRange.startDate(), dateRange.endDate(), customerName));
	}

	@GetMapping("/deposit-balance/{id}/details")
	@ResponseBody
	public ResponseEntity<String> details(@PathVariable("id") Integer id) {
		OrderListBean order = orderRepository.getOrderReceipt(id);
		if (order == null || !"A".equalsIgnoreCase(order.getid_active_status())) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(buildDetailRows(id));
	}

	@PostMapping("/deposit-balance/{id}/receive")
	@ResponseBody
	@Transactional
	public ResponseEntity<String> receive(
			@PathVariable("id") Integer id,
			@RequestParam("recordDate") String recordDate) {
		LocalDate receiveDate = parseDate(recordDate);
		if (receiveDate == null) {
			return ResponseEntity.badRequest().body("Record date is required");
		}
		OrderListBean order = orderRepository.getOrderReceipt(id);
		if (order == null || !"A".equalsIgnoreCase(order.getid_active_status()) || !Integer.valueOf(2).equals(order.getID_pay_method())) {
			return ResponseEntity.badRequest().body("Order is not a deposit payment or already received");
		}
		BigDecimal balance = parseMoney(order.geto_price_balance());
		if (balance.compareTo(BigDecimal.ZERO) <= 0) {
			return ResponseEntity.badRequest().body("Order balance is empty");
		}
		int updatedRows = orderRepository.receiveDepositBalance(id);
		if (updatedRows == 0) {
			return ResponseEntity.badRequest().body("Order is not a deposit payment or already received");
		}
		saveIncome(receiveDate, order.geto_customer_name(), balance, id);
		return ResponseEntity.ok("Success");
	}

	private void saveIncome(LocalDate createDate, String customerName, BigDecimal price, Integer orderId) {
		Income income = new Income();
		income.setCreateDate(createDate.toString());
		income.setCustomerName(customerName);
		income.setTypeIncome(3);
		income.setPrice(price.stripTrailingZeros().toPlainString());
		income.setNote("จ่ายมัดจำที่เหลือ");
		income.setDelete("A");
		income.setOrder(orderId);
		income.setActiveStatus("A");
		incomeRepository.save(income);
	}

	private String buildDepositRows(String startDate, String endDate, String customerName) {
		List<OrderListBean> orderList = orderRepository.getDepositBalanceList(startDate, endDate, customerName);
		StringBuilder rows = new StringBuilder();
		for (OrderListBean order : orderList) {
			rows.append("<tr class='deposit-balance-row' onclick='open_deposit_detail(" + order.getID_order() + ")'>");
			rows.append("<td class='deposit-action-col'>" + buildReceiveButton(order) + "</td>");
			rows.append("<td class='deposit-code-col'>" + toDisplay(order.geto_order_code()) + "</td>");
			rows.append("<td class='deposit-date-col'>" + formatDate(order.geto_order_date()) + "</td>");
			rows.append("<td>" + toDisplay(order.geto_customer_name()) + "</td>");
			rows.append("<td class='text-end deposit-money-col'>" + formatMoney(order.geto_price_total()) + "</td>");
			rows.append("<td class='text-end deposit-money-col'>" + formatMoney(order.geto_price_pledge()) + "</td>");
			rows.append("<td class='text-end deposit-money-col text-danger fw-bold'>" + formatMoney(order.geto_price_balance()) + "</td>");
			rows.append("<td>" + buildPaymentBadge(order.getID_pay_method(), order.getpm_name()) + "</td>");
			rows.append("</tr>");
		}
		return rows.toString();
	}

	private String buildReceiveButton(OrderListBean order) {
		return "<button type='button' class='btn btn-success' onclick='receive_deposit_balance(" + order.getID_order() + ", event)'>"
				+ "<i data-feather='dollar-sign'></i> รับเงิน</button>";
	}

	private String buildDetailRows(Integer orderId) {
		List<OrderDetailBean> detailList = orderDetailRepository.getDataByOrder(orderId);
		StringBuilder rows = new StringBuilder();
		DetailSummary summary = new DetailSummary();
		int rowNumber = 0;
		for (OrderDetailBean detail : detailList) {
			rowNumber++;
			summary.add(detail.getID_pay_method(), detail.getod_price_total(), detail.getod_price_pledge(), detail.getod_price_balance());
			rows.append("<tr>");
			rows.append("<td>" + rowNumber + "</td>");
			rows.append("<td>" + toDisplay(detail.geto_customer_name()) + "</td>");
			rows.append("<td>" + toDisplay(detail.geta_name()) + "</td>");
			rows.append("<td>" + toDisplay(detail.getp_name()) + "</td>");
			rows.append("<td>" + toDisplay(detail.getw_name()) + "</td>");
			rows.append("<td>" + toDisplay(detail.getv_name()) + "</td>");
			rows.append("<td>" + toDisplay(detail.getc_name()) + "</td>");
			rows.append("<td class='text-end'>" + toDisplay(detail.getod_qty()) + "</td>");
			rows.append("<td class='text-end'>" + formatMoney(unitPrice(detail.getod_price_total(), detail.getod_qty())) + "</td>");
			rows.append("<td class='text-end'>" + formatMoney(unitPrice(detail.getod_price_pledge(), detail.getod_qty())) + "</td>");
			rows.append("<td class='text-end'>" + formatMoney(detail.getod_price_total()) + "</td>");
			rows.append("<td class='text-end'>" + formatMoney(detail.getod_price_pledge()) + "</td>");
			rows.append("<td class='text-end'>" + formatMoney(detail.getod_price_balance()) + "</td>");
			rows.append("<td>" + buildPaymentBadge(detail.getID_pay_method(), null) + "</td>");
			rows.append("</tr>");
		}
		if (detailList.isEmpty()) {
			rows.append("<tr><td colspan='14' class='text-center text-muted'>ไม่พบรายละเอียดสินค้า</td></tr>");
		} else {
			rows.append(buildSummaryRow(summary));
		}
		return rows.toString();
	}

	private String buildPaymentBadge(Integer payMethod, String payMethodName) {
		if (Integer.valueOf(1).equals(payMethod)) {
			return "<span class='badge bg-info deposit-payment-badge'>" + labelOrDefault(payMethodName, "จ่ายเต็ม") + "</span>";
		}
		if (Integer.valueOf(3).equals(payMethod)) {
			return "<span class='badge bg-success deposit-payment-badge'>" + labelOrDefault(payMethodName, "แบ่งชำระ") + "</span>";
		}
		return "<span class='badge bg-warning deposit-payment-badge'>" + labelOrDefault(payMethodName, "มัดจำ") + "</span>";
	}

	private String buildSummaryRow(DetailSummary summary) {
		StringBuilder row = new StringBuilder();
		row.append("<tr><td colspan='14' class='detail-summary-cell'>");
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

	private DateRange defaultDateRange(String startDate, String endDate) {
		return new DateRange(
				startDate == null || startDate.isBlank() ? DEFAULT_START_DATE : startDate,
				endDate == null || endDate.isBlank() ? DEFAULT_END_DATE : endDate);
	}

	private String labelOrDefault(String label, String fallback) {
		return label == null || label.isBlank() ? fallback : label;
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
			return "0.00";
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

	private String toDisplay(Object value) {
		return value == null ? "" : value.toString();
	}

	private record DateRange(String startDate, String endDate) {
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
