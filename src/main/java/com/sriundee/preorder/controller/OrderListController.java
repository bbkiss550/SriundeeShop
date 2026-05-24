package com.sriundee.preorder.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.OrderDetailBean;
import com.sriundee.preorder.bean.OrderListBean;
import com.sriundee.preorder.dto.OrderDto;
import com.sriundee.preorder.entity.Income;
import com.sriundee.preorder.entity.Order;
import com.sriundee.preorder.entity.OrderStatus;
import com.sriundee.preorder.entity.PaymentMethod;
import com.sriundee.preorder.repository.IncomeRepository;
import com.sriundee.preorder.repository.OrderDetailRepository;
import com.sriundee.preorder.repository.OrderRepository;
import com.sriundee.preorder.repository.OrderStatusRepository;
import com.sriundee.preorder.repository.PaymentMethodRepository;

import jakarta.transaction.Transactional;

@Controller
public class OrderListController {

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
	private PaymentMethodRepository paymentMethodRepository;

	@Autowired
	private OrderStatusRepository orderStatusRepository;

	@Autowired
	private IncomeRepository incomeRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@GetMapping("/orders")
	public String index(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "customerName", required = false) String customerName,
			@RequestParam(value = "payMethod", required = false) Integer payMethod,
			@RequestParam(value = "orderStatus", required = false) Integer orderStatus,
			Model model) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
	    model.addAttribute("mainMenus", menuService.getMenuList(12, null));
	    model.addAttribute("orderRows", buildOrderRows(dateRange.startDate(), dateRange.endDate(), customerName, payMethod, orderStatus));
	    model.addAttribute("startDate", dateRange.startDate());
	    model.addAttribute("endDate", dateRange.endDate());
	    model.addAttribute("customerName", toDisplay(customerName));
	    model.addAttribute("payMethod", payMethod);
	    model.addAttribute("orderStatus", orderStatus);
	    model.addAttribute("paymentMethodList", buildPaymentMethodOptions(payMethod, true));
	    model.addAttribute("editPaymentMethodList", buildPaymentMethodOptions(null, false));
	    model.addAttribute("orderStatusList", buildOrderStatusOptions(orderStatus));
	    return "order/list";
	}

	@GetMapping("/orders/search")
	@ResponseBody
	public ResponseEntity<String> search(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "customerName", required = false) String customerName,
			@RequestParam(value = "payMethod", required = false) Integer payMethod,
			@RequestParam(value = "orderStatus", required = false) Integer orderStatus) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
		return ResponseEntity.ok(buildOrderRows(dateRange.startDate(), dateRange.endDate(), customerName, payMethod, orderStatus));
	}

	@GetMapping("/orders/{id}/details")
	@ResponseBody
	public ResponseEntity<String> details(@PathVariable("id") Integer id) {
		return ResponseEntity.ok(buildDetailRows(id));
	}

	@GetMapping("/orders/{id}")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getOrder(@PathVariable("id") Integer id) {
		return orderRepository.findById(id)
				.map(order -> ResponseEntity.ok(toOrderMap(order)))
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/orders/{id}/update")
	@ResponseBody
	@Transactional
	public ResponseEntity<String> updateOrder(@PathVariable("id") Integer id, @RequestBody OrderDto orderDto) {
		return orderRepository.findById(id).map(order -> {
			LocalDate orderDate = parseRequiredDate(orderDto.getOrder_date());
			order.setOrder_date(java.sql.Date.valueOf(orderDate));
			order.setCustomer_name(toDisplay(orderDto.getCustomer_name()));
			order.setPay_method(orderDto.getPay_method());
			order.setPay_type(orderDto.getPay_type());
			order.setLast_pay_date(parseOptionalSqlDate(orderDto.getLast_pay_date()));
			order.setSend_cost(orderDto.getSend_cost());
			order.setDiscount(orderDto.getDiscount());
			order.setPrice_pledge(orderDto.getPrice_pledge());
			order.setPrice_balance(Integer.valueOf(1).equals(orderDto.getPay_method()) ? 0 : orderDto.getPrice_balance());
			order.setNet(orderDto.getNet());
			order.setRemark(orderDto.getRemark());
			orderRepository.save(order);
			refreshOrderIncome(order, orderDate);
			return ResponseEntity.ok("Success");
		}).orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/orders/{id}/delete")
	@ResponseBody
	@Transactional
	public ResponseEntity<String> deleteOrder(@PathVariable("id") Integer id) {
		if (!orderRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		jdbcTemplate.update("""
				DELETE FROM t_cost_detail
				WHERE ID_order_detail IN (SELECT ID_order_detail FROM t_order_detail WHERE ID_order = ?)
				""", id);
		jdbcTemplate.update("""
				DELETE FROM t_lot_detail
				WHERE ID_order_detail IN (SELECT ID_order_detail FROM t_order_detail WHERE ID_order = ?)
				""", id);
		incomeRepository.deleteByOrderId(id);
		jdbcTemplate.update("DELETE FROM t_order_detail WHERE ID_order = ?", id);
		orderRepository.deleteById(id);
		return ResponseEntity.ok("Success");
	}

	@GetMapping(value = "/orders/{id}/receipt", produces = "text/html; charset=UTF-8")
	@ResponseBody
	public ResponseEntity<String> receipt(@PathVariable("id") Integer id) {
		OrderListBean order = orderRepository.getOrderReceipt(id);
		if (order == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(buildReceiptHtml(order, orderDetailRepository.getDataByOrder(id)));
	}

	@GetMapping(value = "/orders/{id}/receipt-fragment", produces = "text/html; charset=UTF-8")
	@ResponseBody
	public ResponseEntity<String> receiptFragment(@PathVariable("id") Integer id) {
		OrderListBean order = orderRepository.getOrderReceipt(id);
		if (order == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(buildReceiptContent(order, orderDetailRepository.getDataByOrder(id)));
	}

	private String buildOrderRows(String startDate, String endDate, String customerName, Integer payMethod, Integer orderStatus) {
		List<OrderListBean> orderList = orderRepository.getOrderList(startDate, endDate, customerName, payMethod, orderStatus);
		StringBuilder rows = new StringBuilder();
		for (OrderListBean order : orderList) {
			rows.append("<tr class='order-list-row' onclick='open_order_detail(" + order.getID_order() + ")'>");
			rows.append("<td class='order-action-col'>"
					+ "<button type='button' class='btn icon btn-warning' onclick='edit_order(" + order.getID_order() + ", event)'><i data-feather='edit-2'></i></button>"
					+ "<button type='button' class='btn icon btn-danger ms-1' onclick='delete_order(" + order.getID_order() + ", event)'><i data-feather='trash-2'></i></button>"
					+ "</td>");
			rows.append("<td class='order-code-col'>" + toDisplay(order.geto_order_code()) + "</td>");
			rows.append("<td class='order-date-col'>" + formatDate(order.geto_order_date()) + "</td>");
			rows.append("<td>" + toDisplay(order.geto_customer_name()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_price_total()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_price_pledge()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_price_balance()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_send_cost()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_net()) + "</td>");
			rows.append("<td class='order-payment-col'>" + buildPaymentBadge(order.getID_pay_method(), order.getpm_name()) + "</td>");
			rows.append("<td>" + buildOrderStatusBadges(order.getorder_status_names(), order.getorder_status_colors()) + "</td>");
			rows.append("</tr>");
		}
		return rows.toString();
	}

	private DateRange defaultDateRange(String startDate, String endDate) {
		String resolvedStartDate = startDate == null || startDate.isBlank() ? DEFAULT_START_DATE : startDate;
		String resolvedEndDate = endDate == null || endDate.isBlank() ? DEFAULT_END_DATE : endDate;
		return new DateRange(resolvedStartDate, resolvedEndDate);
	}

	private Map<String, Object> toOrderMap(Order order) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("id", order.getId());
		data.put("order_date", formatSqlDate(order.getOrder_date()));
		data.put("customer_name", toDisplay(order.getCustomer_name()));
		data.put("pay_method", order.getPay_method());
		data.put("pay_type", order.getPay_type());
		data.put("last_pay_date", formatSqlDate(order.getLast_pay_date()));
		data.put("send_cost", order.getSend_cost());
		data.put("discount", order.getDiscount());
		data.put("price_total", order.getPrice_total());
		data.put("price_pledge", order.getPrice_pledge());
		data.put("price_balance", order.getPrice_balance());
		data.put("net", order.getNet());
		data.put("remark", toDisplay(order.getRemark()));
		return data;
	}

	private LocalDate parseRequiredDate(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Date is required");
		}
		return LocalDate.parse(value);
	}

	private java.sql.Date parseOptionalSqlDate(String value) {
		return value == null || value.isBlank() ? null : java.sql.Date.valueOf(LocalDate.parse(value));
	}

	private String formatSqlDate(java.util.Date value) {
		if (value == null) {
			return "";
		}
		return new java.sql.Date(value.getTime()).toLocalDate().toString();
	}

	private void refreshOrderIncome(Order order, LocalDate incomeDate) {
		incomeRepository.deleteByOrderId(order.getId());
		Integer payMethod = order.getPay_method();
		if (Integer.valueOf(1).equals(payMethod)) {
			saveIncome(incomeDate, order.getCustomer_name(), 1, order.getNet(), "จ่ายเต็ม", order.getId());
		} else if (Integer.valueOf(2).equals(payMethod)) {
			saveIncome(incomeDate, order.getCustomer_name(), 2, order.getPrice_pledge(), "จ่ายมัดจำ", order.getId());
		} else if (Integer.valueOf(3).equals(payMethod)) {
			saveIncome(incomeDate, order.getCustomer_name(), 2, order.getPrice_pledge(), "จ่ายมัดจำ", order.getId());
			saveIncome(incomeDate, order.getCustomer_name(), 3, order.getPrice_balance(), "จ่ายมัดจำที่เหลือ", order.getId());
		}
	}

	private void saveIncome(LocalDate createDate, String customerName, Integer typeIncome, double price, String note, Integer orderId) {
		Income income = new Income();
		income.setCreateDate(createDate.toString());
		income.setCustomerName(customerName);
		income.setTypeIncome(typeIncome);
		income.setPrice(BigDecimal.valueOf(price).stripTrailingZeros().toPlainString());
		income.setNote(note);
		income.setDelete("A");
		income.setOrder(orderId);
		incomeRepository.save(income);
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
			rows.append("<td>" + buildDetailStatusBadge(detail.getos_name(), detail.getos_color()) + "</td>");
			rows.append("</tr>");
		}
		if (detailList.isEmpty()) {
			rows.append("<tr><td colspan='14' class='text-center text-muted'>ไม่พบรายละเอียดสินค้า</td></tr>");
		} else {
			rows.append(buildSummaryRow(summary));
		}
		return rows.toString();
	}

	private String buildPaymentMethodOptions(Integer selectedId, boolean includeAllOption) {
		StringBuilder options = new StringBuilder();
		if (includeAllOption) {
			options.append("<option value=''>ทั้งหมด</option>");
		}
		for (PaymentMethod paymentMethod : paymentMethodRepository.getDataAll()) {
			String selected = paymentMethod.getId().equals(selectedId) ? " selected" : "";
			options.append("<option value='" + paymentMethod.getId() + "'" + selected + ">" + paymentMethod.getName() + "</option>");
		}
		return options.toString();
	}

	private String buildOrderStatusOptions(Integer selectedId) {
		StringBuilder options = new StringBuilder("<option value=''>ทั้งหมด</option>");
		for (OrderStatus orderStatus : orderStatusRepository.getDataAll()) {
			String selected = orderStatus.getId().equals(selectedId) ? " selected" : "";
			options.append("<option value='" + orderStatus.getId() + "'" + selected + ">" + orderStatus.getName() + "</option>");
		}
		return options.toString();
	}

	private String buildPaymentBadge(Integer payMethod, String payMethodName) {
		String color = Integer.valueOf(1).equals(payMethod) ? "bg-info" : Integer.valueOf(3).equals(payMethod) ? "bg-success" : "bg-warning";
		return "<span class='badge " + color + " order-list-badge'>" + toDisplay(payMethodName) + "</span>";
	}

	private String buildDetailStatusBadge(String statusName, String statusColor) {
		String color = statusColor == null || statusColor.isBlank() ? "secondary" : statusColor;
		return "<span class='badge bg-" + color + " detail-payment-badge'>" + toDisplay(statusName) + "</span>";
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

	private String buildOrderStatusBadges(String namesValue, String colorsValue) {
		if (namesValue == null || namesValue.isBlank()) {
			return "<span class='badge bg-secondary order-list-badge'>ไม่ระบุ</span>";
		}
		String[] names = namesValue.split("\\|\\|", -1);
		String[] colors = colorsValue == null ? new String[0] : colorsValue.split("\\|\\|", -1);
		StringBuilder badges = new StringBuilder("<div class='order-status-list'>");
		for (int i = 0; i < names.length; i++) {
			String color = i < colors.length && !colors[i].isBlank() ? colors[i] : "secondary";
			badges.append("<span class='badge bg-" + color + " order-list-badge'>" + toDisplay(names[i]) + "</span>");
		}
		badges.append("</div>");
		return badges.toString();
	}

	private String buildReceiptContent(OrderListBean order, List<OrderDetailBean> detailList) {
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
		String discountRow = "";
		if (toBigDecimal(order.geto_discount()).compareTo(BigDecimal.ZERO) > 0) {
			discountRow = """
						<div class="summary-row"><span>ส่วนลด</span><strong class="right">%s</strong></div>
					""".formatted(formatMoney(order.geto_discount()));
		}
		return """
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
							<div><strong>เลขที่คำสั่งซื้อ:</strong> %s</div>
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
				escapeHtml(order.geto_order_code()),
				formatDate(order.geto_order_date()),
				escapeHtml(order.geto_customer_name()),
				escapeHtml(order.getpm_name()),
				escapeHtml(order.geto_remark()),
				rows.toString(),
				formatMoney(order.geto_price_total()),
				formatMoney(order.geto_send_cost()),
				discountRow,
				formatMoney(order.geto_price_pledge()),
				formatMoney(order.geto_price_balance()),
				formatMoney(order.geto_net()));
	}

	private String buildReceiptHtml(OrderListBean order, List<OrderDetailBean> detailList) {
		return """
				<!DOCTYPE html>
				<html lang="th">
				<head>
					<meta charset="UTF-8">
					<title>Receipt %s</title>
					<style>
						* { box-sizing: border-box; }
						body { margin: 0; padding: 28px; color: #111827; font-family: Arial, Tahoma, sans-serif; background: #f8fafc; }
						.receipt { max-width: 880px; margin: 0 auto; padding: 28px; background: #ffffff; border: 1px solid #e5e7eb; }
						.header { display: flex; justify-content: space-between; gap: 24px; border-bottom: 2px solid #111827; padding-bottom: 18px; margin-bottom: 20px; }
						.receipt-brand { display: flex; align-items: center; gap: 16px; min-width: 0; }
						.receipt-title { min-width: 0; }
						.receipt-logo { display: block; width: 132px; height: auto; margin: 0; object-fit: contain; }
						h1 { margin: 0; color: #111827 !important; font-size: 28px; }
						.shop { font-size: 18px; font-weight: 700; margin-top: 6px; }
						.meta { text-align: right; line-height: 1.7; }
						.info { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 24px; margin-bottom: 18px; line-height: 1.6; }
						.info > div { text-align: left; }
						table { width: 100%%; border-collapse: collapse; margin-top: 12px; }
						th, td { border-bottom: 1px solid #e5e7eb; padding: 10px 8px; vertical-align: top; }
						th { text-align: left; background: #f1f5f9; }
						.right { text-align: right; white-space: nowrap; }
						.product-cell { text-align: left; }
						.muted { color: #64748b; font-size: 12px; margin-top: 4px; }
						.summary { margin-left: auto; margin-top: 18px; width: 340px; }
						.summary-row { display: grid; grid-template-columns: 1fr 140px; padding: 8px 0; border-bottom: 1px solid #e5e7eb; }
						.summary-row span:first-child { text-align: left; padding-right: 0; }
						.summary-row.total { font-size: 20px; font-weight: 800; border-bottom: 2px solid #111827; }
						.actions { max-width: 880px; margin: 14px auto 0; text-align: right; }
						button { padding: 10px 18px; border: 0; border-radius: 6px; color: #fff; background: #435ebe; font-weight: 700; cursor: pointer; }
						@media print { body { padding: 0; background: #fff; } .receipt { border: 0; max-width: none; } .actions { display: none; } }
					</style>
				</head>
				<body>
					%s
					<div class="actions"><button type="button" onclick="window.print()">พิมพ์ใบเสร็จ</button></div>
				</body>
				</html>
				""".formatted(
				escapeHtml(order.geto_order_code()),
				buildReceiptContent(order, detailList));
	}

	private String escapeHtml(Object value) {
		return toDisplay(value)
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
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

	private BigDecimal toBigDecimal(String value) {
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
		BigDecimal qty = toBigDecimal(toDisplay(qtyValue));
		if (qty.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return toBigDecimal(total).divide(qty, 2, java.math.RoundingMode.HALF_UP);
	}

	private String toDisplay(Object value) {
		return value == null ? "" : value.toString();
	}

	private class DetailSummary {
		private BigDecimal totalProduct = BigDecimal.ZERO;
		private BigDecimal fullPaid = BigDecimal.ZERO;
		private BigDecimal pledgePaid = BigDecimal.ZERO;
		private BigDecimal balance = BigDecimal.ZERO;

		private void add(Integer payMethod, String total, String pledge, String balanceValue) {
			BigDecimal totalAmount = toBigDecimal(total);
			totalProduct = totalProduct.add(totalAmount);
			if (Integer.valueOf(1).equals(payMethod) || Integer.valueOf(3).equals(payMethod)) {
				fullPaid = fullPaid.add(totalAmount);
			} else {
				pledgePaid = pledgePaid.add(toBigDecimal(pledge));
				balance = balance.add(toBigDecimal(balanceValue));
			}
		}
	}

	private record DateRange(String startDate, String endDate) {
	}
}
