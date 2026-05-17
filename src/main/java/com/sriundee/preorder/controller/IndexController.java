package com.sriundee.preorder.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
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
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class IndexController {

	private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,##0.00");
	private static final DateTimeFormatter CHART_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	
	@Autowired
    private MenuController menuService;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@GetMapping("/")
	public String index(Model model,
			@RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate) {
		String menuList = menuService.getMenuList(1,null);
		LocalDate[] selectedRange = getSelectedRange(startDate, endDate);
		LocalDate selectedStartDate = selectedRange[0];
		LocalDate selectedEndDate = selectedRange[1];
		LocalDate[] chartRange = getChartRange(selectedStartDate, selectedEndDate);
	    model.addAttribute("mainMenus", menuList);
	    model.addAttribute("selectedStartDate", selectedStartDate);
	    model.addAttribute("selectedEndDate", selectedEndDate);
	    model.addAttribute("dashboardPeriod", formatDate(selectedStartDate) + " - " + formatDate(selectedEndDate));
	    model.addAttribute("totalOrders", getLong("""
	    		SELECT COUNT(*)
	    		FROM q_order
	    		WHERE o_order_date BETWEEN ? AND ?
	    		""", selectedStartDate, selectedEndDate));
	    model.addAttribute("totalItems", getLong("""
	    		SELECT COUNT(*)
	    		FROM t_order_detail od
	    		JOIN t_order o ON o.ID_order = od.ID_order
	    		WHERE o.o_order_date BETWEEN ? AND ?
	    		""", selectedStartDate, selectedEndDate));
	    BigDecimal totalSales = getBigDecimal("""
	    		SELECT COALESCE(SUM(o_net), 0)
	    		FROM q_order
	    		WHERE o_order_date BETWEEN ? AND ?
	    		""", selectedStartDate, selectedEndDate);
	    BigDecimal totalBalance = getBigDecimal("""
	    		SELECT COALESCE(SUM(o_price_balance), 0)
	    		FROM q_order
	    		WHERE o_order_date BETWEEN ? AND ?
	    		""", selectedStartDate, selectedEndDate);
	    BigDecimal totalReceived = totalSales.subtract(totalBalance);
	    model.addAttribute("totalSales", formatMoney(totalSales));
	    model.addAttribute("totalReceived", formatMoney(totalReceived));
	    model.addAttribute("totalBalance", formatMoney(totalBalance));
	    model.addAttribute("totalCost", formatMoney(getBigDecimal("""
	    		SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
	    		FROM q_cost
	    		WHERE c_delete = 'A'
	    		  AND c_create_date BETWEEN ? AND ?
	    		""", selectedStartDate, selectedEndDate)));
	    model.addAttribute("costByType", getCostByType(selectedStartDate, selectedEndDate));
	    model.addAttribute("salesTrendChart", toJson(getSalesTrendChart(chartRange[0], chartRange[1])));
	    return "index";
	}

	private List<Map<String, Object>> getCostByType(LocalDate startDate, LocalDate endDate) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				SELECT tc_name AS label,
				       COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0) AS value,
				       CASE ID_type_cost
				           WHEN 1 THEN 'cost-press'
				           WHEN 2 THEN 'cost-shipping'
				           WHEN 3 THEN 'cost-other'
				           WHEN 4 THEN 'cost-danger'
				           WHEN 5 THEN 'cost-success'
				           ELSE 'cost-default'
				       END AS tone
				FROM q_cost
				WHERE c_delete = 'A'
				  AND c_create_date BETWEEN ? AND ?
				GROUP BY ID_type_cost, tc_name
				ORDER BY ID_type_cost
				""", startDate, endDate);
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			result.add(Map.of(
					"label", row.get("label"),
					"value", formatMoney(toBigDecimal(row.get("value"))),
					"tone", row.get("tone")));
		}
		return result;
	}

	private List<Map<String, Object>> getSalesTrendChart(LocalDate startDate, LocalDate endDate) {
		List<Map<String, Object>> salesRows = jdbcTemplate.queryForList("""
				SELECT o_order_date AS orderDate,
				       COALESCE(SUM(o_net), 0) AS amount,
				       COALESCE(SUM(CASE WHEN ID_pay_method = 1 THEN o_net ELSE 0 END), 0) AS fullPaid,
				       COALESCE(SUM(CASE WHEN ID_pay_method = 2 THEN o_price_pledge ELSE 0 END), 0) AS pledgePaid
				FROM q_order
				WHERE o_order_date BETWEEN ? AND ?
				GROUP BY o_order_date
				ORDER BY o_order_date
				""", startDate, endDate);
		Map<LocalDate, Map<String, Object>> salesByDate = new HashMap<>();
		for (Map<String, Object> row : salesRows) {
			Object orderDate = row.get("orderDate");
			if (orderDate instanceof java.sql.Date sqlDate) {
				salesByDate.put(sqlDate.toLocalDate(), row);
			} else if (orderDate instanceof LocalDate localDate) {
				salesByDate.put(localDate, row);
			}
		}
		return startDate.datesUntil(endDate.plusDays(1))
				.map(date -> {
					Map<String, Object> row = salesByDate.get(date);
					return Map.of(
							"label", date.format(CHART_DATE_FORMAT),
							"amount", row == null ? BigDecimal.ZERO : row.get("amount"),
							"fullPaid", row == null ? BigDecimal.ZERO : row.get("fullPaid"),
							"pledgePaid", row == null ? BigDecimal.ZERO : row.get("pledgePaid"));
				})
				.toList();
	}

	private Long getLong(String sql, Object... args) {
		Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
		return value == null ? 0L : value;
	}

	private BigDecimal getBigDecimal(String sql, Object... args) {
		BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
		return value == null ? BigDecimal.ZERO : value;
	}

	private BigDecimal toBigDecimal(Object value) {
		if (value == null) {
			return BigDecimal.ZERO;
		}
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal;
		}
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		try {
			return new BigDecimal(value.toString());
		} catch (RuntimeException e) {
			return BigDecimal.ZERO;
		}
	}

	private LocalDate[] getSelectedRange(String startDate, String endDate) {
		LocalDate today = LocalDate.now();
		LocalDate selectedStartDate = parseDate(startDate, today.withDayOfMonth(1));
		LocalDate selectedEndDate = parseDate(endDate, today.withDayOfMonth(today.lengthOfMonth()));
		if (selectedEndDate.isBefore(selectedStartDate)) {
			selectedEndDate = selectedStartDate;
		}
		return new LocalDate[] { selectedStartDate, selectedEndDate };
	}

	private LocalDate[] getChartRange(LocalDate startDate, LocalDate endDate) {
		YearMonth startMonth = YearMonth.from(startDate);
		YearMonth endMonth = YearMonth.from(endDate);
		if (!startMonth.equals(endMonth)) {
			return new LocalDate[] {
					startMonth.atDay(1),
					endMonth.atEndOfMonth()
			};
		}
		return new LocalDate[] { startDate, endDate };
	}

	private LocalDate parseDate(String value, LocalDate fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return LocalDate.parse(value);
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	private String formatDate(LocalDate date) {
		return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
	}

	private String formatMoney(BigDecimal value) {
		return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
	}

	private String toJson(Object value) {
		try {
			return new ObjectMapper().writeValueAsString(value);
		} catch (JsonProcessingException e) {
			return "[]";
		}
	}
}
