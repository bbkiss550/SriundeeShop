package com.sriundee.preorder.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.LotBean;
import com.sriundee.preorder.bean.LotDetailBean;
import com.sriundee.preorder.entity.Lot;
import com.sriundee.preorder.entity.LotDetail;
import com.sriundee.preorder.entity.OrderDetail;
import com.sriundee.preorder.repository.LotDetailRepository;
import com.sriundee.preorder.repository.LotRepository;
import com.sriundee.preorder.repository.OrderDetailRepository;

import jakarta.transaction.Transactional;

@Controller
public class LotController {

	private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,##0.00");
	private static final String DEFAULT_START_DATE = "2026-01-01";
	private static final String DEFAULT_END_DATE = "2026-12-31";

	@Autowired
    private MenuController menuService;

	@Autowired
	private LotRepository lotRepository;

	@Autowired
	private LotDetailRepository lotDetailRepository;

	@Autowired
	private OrderDetailRepository orderDetailRepository;

	@GetMapping("/lot")
	public String index(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "lotNumber", required = false) String lotNumber,
			Model model) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
		model.addAttribute("mainMenus", menuService.getMenuList(10, null));
	    model.addAttribute("mainLot", buildLotRows(dateRange.startDate(), dateRange.endDate(), status, lotNumber));
	    model.addAttribute("startDate", dateRange.startDate());
	    model.addAttribute("endDate", dateRange.endDate());
	    model.addAttribute("status", toDisplay(status));
	    model.addAttribute("lotNumber", toDisplay(lotNumber));
	    return "lot/index";
	}

	@GetMapping("/lot/search")
	@ResponseBody
	public ResponseEntity<String> search(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "lotNumber", required = false) String lotNumber) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
		return ResponseEntity.ok(buildLotRows(dateRange.startDate(), dateRange.endDate(), status, lotNumber));
	}

	@GetMapping("/lot/detail/{id}")
	@ResponseBody
	public ResponseEntity<String> getDetail(@PathVariable Integer id) {
		return ResponseEntity.ok(buildDetailRows(id));
	}

	@PostMapping("/lot/cancel/{id}")
	@ResponseBody
	@Transactional
	public ResponseEntity<String> cancelLot(@PathVariable Integer id) {
		try {
			Lot lot = lotRepository.findById(id).orElseThrow(() -> new RuntimeException("Lot not found"));
			if (!"A".equals(lot.getDelete())) {
				return ResponseEntity.badRequest().body("Lot already canceled");
			}

			List<LotDetail> lotDetails = lotDetailRepository.findByLot(id);
			List<Integer> orderDetailIds = lotDetails.stream()
					.map(LotDetail::getOrder_detail)
					.toList();
			List<OrderDetail> orderDetails = orderDetailRepository.findAllById(orderDetailIds);
			for (OrderDetail orderDetail : orderDetails) {
				orderDetail.setOrder_status(2);
			}
			orderDetailRepository.saveAll(orderDetails);

			lot.setDelete("D");
			lotRepository.save(lot);

			return ResponseEntity.ok("Success");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}

	@PostMapping("/lot/update-number/{id}")
	@ResponseBody
	@Transactional
	public ResponseEntity<String> updateLotNumber(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
		try {
			String lotNumber = payload.get("lotNumber");
			String startDate = payload.get("startDate");
			String endDate = payload.get("endDate");
			String arriveDate = payload.get("arriveDate");
			if (lotNumber == null || lotNumber.isBlank()) {
				return ResponseEntity.badRequest().body("Lot number is required");
			}
			if (!isValidDateRange(startDate, endDate)) {
				return ResponseEntity.badRequest().body("Invalid lot date range");
			}

			Lot lot = lotRepository.findById(id).orElseThrow(() -> new RuntimeException("Lot not found"));
			if ("D".equals(lot.getDelete())) {
				return ResponseEntity.badRequest().body("Cannot edit canceled lot");
			}
			lot.setLot_number(lotNumber.trim());
			lot.setStart_date(blankToNull(startDate));
			lot.setEnd_date(blankToNull(endDate));
			lot.setArrive_date(blankToNull(arriveDate));
			lotRepository.save(lot);

			return ResponseEntity.ok("Success");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}

	@PostMapping("/lot/detail/delete/{id}")
	@ResponseBody
	@Transactional
	public ResponseEntity<String> deleteLotDetail(@PathVariable Integer id) {
		try {
			LotDetail lotDetail = lotDetailRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Lot detail not found"));
			Integer lotId = lotDetail.getLot();
			Integer orderDetailId = lotDetail.getOrder_detail();

			OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
					.orElseThrow(() -> new RuntimeException("Order detail not found"));
			orderDetail.setOrder_status(2);
			orderDetailRepository.save(orderDetail);

			lotDetailRepository.delete(lotDetail);

			if (lotDetailRepository.countByLot(lotId) == 0) {
				Lot lot = lotRepository.findById(lotId).orElse(null);
				if (lot != null) {
					lot.setDelete("D");
					lotRepository.save(lot);
				}
			}

			return ResponseEntity.ok("Success");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}

	private String buildLotRows(String startDate, String endDate, String status, String lotNumber) {
		List<LotBean> lotList = lotRepository.getLotAll(startDate, endDate, status, lotNumber);
		StringBuilder strLot = new StringBuilder();
		Integer rowId = 0;
		for (LotBean lot : lotList) {
			rowId += 1;
			strLot.append("<tr class='lot-row' onclick='open_lot_detail(" + lot.getID_lot() + ")'>");
			strLot.append("<td class='lot-action-col'><div class='lot-action-buttons'>"
					+ buildCancelButton(lot)
					+ buildEditButton(lot)
					+ "</div></td>");
			strLot.append("<td>" + rowId + "</td>");
			strLot.append("<td class='lot-date-col'>" + formatDate(lot.getl_create_date()) + "</td>");
			strLot.append("<td>" + toDisplay(lot.getl_lot_number()) + "</td>");
			strLot.append("<td class='lot-range-col'>" + formatDateRange(lot.getl_start_date(), lot.getl_end_date()) + "</td>");
			strLot.append("<td class='lot-date-col'>" + formatDate(lot.getl_arrive_date()) + "</td>");
			strLot.append("<td class='text-end lot-count-col'>" + toDisplay(lot.getdetail_count()) + "</td>");
			strLot.append("<td>" + buildStatusBadge(lot.getl_delete()) + "</td>");
			strLot.append("</tr>");
		}
		return strLot.toString();
	}

	private String buildDetailRows(Integer id) {
		List<LotDetailBean> detailList = lotDetailRepository.getDataByLot(id);
		StringBuilder strDetail = new StringBuilder();
		Integer rowId = 0;
		DetailSummary summary = new DetailSummary();
		for (LotDetailBean detail : detailList) {
			rowId += 1;
			summary.add(detail.getID_pay_method(), detail.getod_price_total(), detail.getod_price_pledge(), detail.getod_price_balance());
			strDetail.append("<tr>");
			strDetail.append("<td><button type='button' class='btn icon btn-danger' onclick='delete_lot_detail(" + detail.getID_lot_detail() + ")'><i data-feather='trash-2'></i></button></td>");
			strDetail.append("<td>" + rowId + "</td>");
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
			strDetail.append("<tr><td colspan='12' class='text-center text-muted'>ไม่พบรายละเอียดสินค้า</td></tr>");
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

	private DateRange defaultDateRange(String startDate, String endDate) {
		return new DateRange(
				startDate == null || startDate.isBlank() ? DEFAULT_START_DATE : startDate,
				endDate == null || endDate.isBlank() ? DEFAULT_END_DATE : endDate);
	}

	private record DateRange(String startDate, String endDate) {
	}

	private String buildStatusBadge(String status) {
		if ("A".equals(status)) {
			return "<span class='badge bg-success lot-status-badge'>สำเร็จ</span>";
		}
		if ("D".equals(status)) {
			return "<span class='badge bg-danger lot-status-badge'>ยกเลิก</span>";
		}
		return "<span class='badge bg-secondary lot-status-badge'>ไม่ระบุ</span>";
	}

	private String buildCancelButton(LotBean lot) {
		if (!"A".equals(lot.getl_delete())) {
			return "<button type='button' class='btn icon btn-secondary' disabled><i data-feather='x-circle'></i></button>";
		}
		return "<button type='button' class='btn icon btn-danger' onclick='cancel_lot(" + lot.getID_lot() + ", event)'><i data-feather='x-circle'></i></button>";
	}

	private String buildEditButton(LotBean lot) {
		if ("D".equals(lot.getl_delete())) {
			return "<button type='button' class='btn icon btn-secondary' disabled><i data-feather='edit-2'></i></button>";
		}
		String lotNumber = escapeJs(toDisplay(lot.getl_lot_number()));
		String startDate = escapeJs(toDisplay(lot.getl_start_date()));
		String endDate = escapeJs(toDisplay(lot.getl_end_date()));
		String arriveDate = escapeJs(toDisplay(lot.getl_arrive_date()));
		return "<button type='button' class='btn icon btn-warning' onclick=\"edit_lot_number(" + lot.getID_lot() + ", '" + lotNumber + "', '" + startDate + "', '" + endDate + "', '" + arriveDate + "', event)\"><i data-feather='edit-2'></i></button>";
	}

	private boolean isValidDateRange(String startDate, String endDate) {
		if ((startDate == null || startDate.isBlank()) && (endDate == null || endDate.isBlank())) {
			return true;
		}
		if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
			return false;
		}
		try {
			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(endDate);
			return !end.isBefore(start);
		} catch (Exception e) {
			return false;
		}
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String escapeJs(String value) {
		return value.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\"", "\\\"")
				.replace("\r", "")
				.replace("\n", "");
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

	private String formatDateRange(String startDate, String endDate) {
		String start = formatDate(startDate);
		String end = formatDate(endDate);
		if (start.isBlank() && end.isBlank()) {
			return "";
		}
		if (start.isBlank()) {
			return end;
		}
		if (end.isBlank()) {
			return start;
		}
		return start + " ถึง " + end;
	}

	private String toDisplay(Object value) {
		return value == null ? "" : value.toString();
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
