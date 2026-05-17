package com.sriundee.preorder.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.OrderDetailBean;
import com.sriundee.preorder.bean.OrderListBean;
import com.sriundee.preorder.entity.OrderStatus;
import com.sriundee.preorder.entity.PaymentMethod;
import com.sriundee.preorder.repository.OrderDetailRepository;
import com.sriundee.preorder.repository.OrderRepository;
import com.sriundee.preorder.repository.OrderStatusRepository;
import com.sriundee.preorder.repository.PaymentMethodRepository;

@Controller
public class OrderListController {

	private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,##0.00");

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

	@GetMapping("/orders")
	public String index(
			@RequestParam(value = "orderDate", required = false) String orderDate,
			@RequestParam(value = "customerName", required = false) String customerName,
			@RequestParam(value = "payMethod", required = false) Integer payMethod,
			@RequestParam(value = "orderStatus", required = false) Integer orderStatus,
			Model model) {
	    model.addAttribute("mainMenus", menuService.getMenuList(12, null));
	    model.addAttribute("orderRows", buildOrderRows(orderDate, customerName, payMethod, orderStatus));
	    model.addAttribute("orderDate", toDisplay(orderDate));
	    model.addAttribute("customerName", toDisplay(customerName));
	    model.addAttribute("payMethod", payMethod);
	    model.addAttribute("orderStatus", orderStatus);
	    model.addAttribute("paymentMethodList", buildPaymentMethodOptions(payMethod));
	    model.addAttribute("orderStatusList", buildOrderStatusOptions(orderStatus));
	    return "order/list";
	}

	@GetMapping("/orders/search")
	@ResponseBody
	public ResponseEntity<String> search(
			@RequestParam(value = "orderDate", required = false) String orderDate,
			@RequestParam(value = "customerName", required = false) String customerName,
			@RequestParam(value = "payMethod", required = false) Integer payMethod,
			@RequestParam(value = "orderStatus", required = false) Integer orderStatus) {
		return ResponseEntity.ok(buildOrderRows(orderDate, customerName, payMethod, orderStatus));
	}

	@GetMapping("/orders/{id}/details")
	@ResponseBody
	public ResponseEntity<String> details(@PathVariable("id") Integer id) {
		return ResponseEntity.ok(buildDetailRows(id));
	}

	private String buildOrderRows(String orderDate, String customerName, Integer payMethod, Integer orderStatus) {
		List<OrderListBean> orderList = orderRepository.getOrderList(orderDate, customerName, payMethod, orderStatus);
		StringBuilder rows = new StringBuilder();
		for (OrderListBean order : orderList) {
			rows.append("<tr class='order-list-row' onclick='open_order_detail(" + order.getID_order() + ")'>");
			rows.append("<td class='order-code-col'>" + toDisplay(order.geto_order_code()) + "</td>");
			rows.append("<td class='order-date-col'>" + formatDate(order.geto_order_date()) + "</td>");
			rows.append("<td>" + toDisplay(order.geto_customer_name()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_price_total()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_price_pledge()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_price_balance()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_send_cost()) + "</td>");
			rows.append("<td class='text-end order-money-col'>" + formatMoney(order.geto_net()) + "</td>");
			rows.append("<td>" + buildPaymentBadge(order.getID_pay_method(), order.getpm_name()) + "</td>");
			rows.append("<td>" + buildOrderStatusBadges(order.getorder_status_names(), order.getorder_status_colors()) + "</td>");
			rows.append("</tr>");
		}
		return rows.toString();
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
			rows.append("<td class='text-end'>" + formatMoney(detail.getod_price_total()) + "</td>");
			rows.append("<td class='text-end'>" + formatMoney(detail.getod_price_pledge()) + "</td>");
			rows.append("<td class='text-end'>" + formatMoney(detail.getod_price_balance()) + "</td>");
			rows.append("<td>" + buildDetailStatusBadge(detail.getos_name(), detail.getos_color()) + "</td>");
			rows.append("</tr>");
		}
		if (detailList.isEmpty()) {
			rows.append("<tr><td colspan='12' class='text-center text-muted'>ไม่พบรายละเอียดสินค้า</td></tr>");
		} else {
			rows.append(buildSummaryRow(summary));
		}
		return rows.toString();
	}

	private String buildPaymentMethodOptions(Integer selectedId) {
		StringBuilder options = new StringBuilder("<option value=''>ทั้งหมด</option>");
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
		String color = Integer.valueOf(1).equals(payMethod) ? "bg-info" : "bg-warning";
		return "<span class='badge " + color + " order-list-badge'>" + toDisplay(payMethodName) + "</span>";
	}

	private String buildDetailStatusBadge(String statusName, String statusColor) {
		String color = statusColor == null || statusColor.isBlank() ? "secondary" : statusColor;
		return "<span class='badge bg-" + color + " detail-payment-badge'>" + toDisplay(statusName) + "</span>";
	}

	private String buildSummaryRow(DetailSummary summary) {
		StringBuilder row = new StringBuilder();
		row.append("<tr><td colspan='12' class='detail-summary-cell'>");
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
			if (Integer.valueOf(1).equals(payMethod)) {
				fullPaid = fullPaid.add(totalAmount);
			} else {
				pledgePaid = pledgePaid.add(toBigDecimal(pledge));
				balance = balance.add(toBigDecimal(balanceValue));
			}
		}
	}
}
