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
			Model model) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
		model.addAttribute("mainMenus", menuService.getMenuList(10, null));
	    model.addAttribute("mainLot", buildLotRows(dateRange.startDate(), dateRange.endDate(), status));
	    model.addAttribute("startDate", dateRange.startDate());
	    model.addAttribute("endDate", dateRange.endDate());
	    model.addAttribute("status", toDisplay(status));
	    return "lot/index";
	}

	@GetMapping("/lot/search")
	@ResponseBody
	public ResponseEntity<String> search(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate,
			@RequestParam(value = "status", required = false) String status) {
		DateRange dateRange = defaultDateRange(startDate, endDate);
		return ResponseEntity.ok(buildLotRows(dateRange.startDate(), dateRange.endDate(), status));
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
			if (lotNumber == null || lotNumber.isBlank()) {
				return ResponseEntity.badRequest().body("Lot number is required");
			}

			Lot lot = lotRepository.findById(id).orElseThrow(() -> new RuntimeException("Lot not found"));
			lot.setLot_number(lotNumber.trim());
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

	private String buildLotRows(String startDate, String endDate, String status) {
		List<LotBean> lotList = lotRepository.getLotAll(startDate, endDate, status);
		StringBuilder strLot = new StringBuilder();
		Integer rowId = 0;
		for (LotBean lot : lotList) {
			rowId += 1;
			strLot.append("<tr class='lot-row' onclick='open_lot_detail(" + lot.getID_lot() + ")'>");
			strLot.append("<td class='lot-cancel-col'>" + buildCancelButton(lot) + "</td>");
			strLot.append("<td class='lot-action-col'>" + buildEditButton(lot) + "</td>");
			strLot.append("<td>" + rowId + "</td>");
			strLot.append("<td class='lot-date-col'>" + formatDate(lot.getl_create_date()) + "</td>");
			strLot.append("<td>" + toDisplay(lot.getl_lot_number()) + "</td>");
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
			strDetail.append("<td>" + buildPaymentBadge(detail.getID_pay_method()) + "</td>");
			strDetail.append("</tr>");
		}

		if (detailList.isEmpty()) {
			strDetail.append("<tr><td colspan='10' class='text-center text-muted'>ไม่พบรายละเอียดสินค้า</td></tr>");
		} else {
			strDetail.append(buildSummaryRow(summary));
		}
		return strDetail.toString();
	}

	private String buildPaymentBadge(Integer payMethod) {
		if (Integer.valueOf(1).equals(payMethod)) {
			return "<span class='badge bg-info detail-payment-badge'>จ่ายเต็ม</span>";
		}
		return "<span class='badge bg-warning detail-payment-badge'>มัดจำ</span>";
	}

	private String buildSummaryRow(DetailSummary summary) {
		StringBuilder row = new StringBuilder();
		row.append("<tr><td colspan='10' class='detail-summary-cell'>");
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
		YearMonth currentMonth = YearMonth.now();
		String defaultStartDate = currentMonth.atDay(1).toString();
		String defaultEndDate = currentMonth.atEndOfMonth().toString();
		return new DateRange(
				startDate == null || startDate.isBlank() ? defaultStartDate : startDate,
				endDate == null || endDate.isBlank() ? defaultEndDate : endDate);
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
		String lotNumber = escapeJs(toDisplay(lot.getl_lot_number()));
		return "<button type='button' class='btn icon btn-warning' onclick=\"edit_lot_number(" + lot.getID_lot() + ", '" + lotNumber + "', event)\"><i data-feather='edit-2'></i></button>";
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
			if (Integer.valueOf(1).equals(payMethod)) {
				fullPaid = fullPaid.add(total);
			} else {
				pledgePaid = pledgePaid.add(pledge);
				balance = balance.add(remaining);
			}
		}
	}
}
