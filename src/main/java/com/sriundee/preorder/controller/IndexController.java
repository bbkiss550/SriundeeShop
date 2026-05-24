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
	private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2026, 1, 1);
	private static final LocalDate DEFAULT_END_DATE = LocalDate.of(2026, 12, 31);
	
	@Autowired
    private MenuController menuService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SettingController settingController;

	@Autowired
	private DashboardManageController dashboardManageController;
	
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
	    		SELECT COALESCE(SUM(CASE WHEN ID_pay_method = 2 THEN o_price_balance ELSE 0 END), 0)
	    		FROM q_order
	    		WHERE o_order_date BETWEEN ? AND ?
	    		""", selectedStartDate, selectedEndDate);
	    BigDecimal totalFullPaid = getBigDecimal("""
	    		SELECT COALESCE(SUM(CASE WHEN ID_pay_method = 1 THEN o_net ELSE 0 END), 0)
	    		FROM q_order
	    		WHERE o_order_date BETWEEN ? AND ?
	    		""", selectedStartDate, selectedEndDate);
	    BigDecimal totalPledgePaid = getBigDecimal("""
	    		SELECT COALESCE(SUM(CASE WHEN ID_pay_method IN (2, 3) THEN o_price_pledge ELSE 0 END), 0)
	    		FROM q_order
	    		WHERE o_order_date BETWEEN ? AND ?
	    		""", selectedStartDate, selectedEndDate);
	    model.addAttribute("totalSales", formatMoney(totalSales));
	    model.addAttribute("totalFullPaid", formatMoney(totalFullPaid));
	    model.addAttribute("totalBalance", formatMoney(totalBalance));
	    model.addAttribute("totalPledgePaid", formatMoney(totalPledgePaid));
	    model.addAttribute("totalCost", formatMoney(getBigDecimal("""
	    		SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
	    		FROM q_cost
	    		WHERE c_delete = 'A'
	    		  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
	    		""", selectedStartDate, selectedEndDate)));
	    model.addAttribute("costByType", getCostByType(selectedStartDate, selectedEndDate));
	    model.addAttribute("salesTrendChart", toJson(getSalesTrendChart(chartRange[0], chartRange[1])));
	    model.addAttribute("artistSalesShareChart", toJson(getSalesShareChart(
	    		"COALESCE(q.a_name, 'ไม่ระบุศิลปิน')", "q.ID_art", selectedStartDate, selectedEndDate)));
	    model.addAttribute("typeSalesShareChart", toJson(getSalesShareChart(
	    		"COALESCE(q.t_name, 'ไม่ระบุประเภท')", "q.ID_type", selectedStartDate, selectedEndDate)));
	    model.addAttribute("dashboardChartSeries", settingController.getDashboardChartSeriesValue());
	    model.addAttribute("dashboardChartGranularity", settingController.getDashboardChartGranularityValue());
	    model.addAttribute("dashboardConfig", dashboardManageController.getDashboardWidgetsValue());
	    model.addAttribute("dashboardData", toJson(dashboardManageController.buildDashboardData(selectedStartDate, selectedEndDate)));
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
				  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
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
				       COALESCE(SUM(CASE WHEN ID_pay_method IN (1, 3) THEN o_net WHEN ID_pay_method = 2 THEN o_price_pledge ELSE 0 END), 0) AS receivedPaid,
				       COALESCE(SUM(CASE WHEN ID_pay_method = 2 THEN o_price_pledge ELSE 0 END), 0) AS pledgePaid
				FROM q_order
				WHERE o_order_date BETWEEN ? AND ?
				GROUP BY o_order_date
				ORDER BY o_order_date
				""", startDate, endDate);
		List<Map<String, Object>> costRows = jdbcTemplate.queryForList("""
				SELECT c_create_date AS costDate,
				       ID_type_cost AS costType,
				       COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0) AS costAmount
				FROM q_cost
				WHERE c_delete = 'A'
				  AND ID_type_cost IN (1, 2)
				  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
				GROUP BY c_create_date, ID_type_cost
				ORDER BY c_create_date, ID_type_cost
				""", startDate, endDate);
		Map<LocalDate, Map<String, Object>> salesByDate = new HashMap<>();
		for (Map<String, Object> row : salesRows) {
			LocalDate orderDate = toLocalDate(row.get("orderDate"));
			if (orderDate != null) {
				salesByDate.put(orderDate, row);
			}
		}
		Map<LocalDate, BigDecimal> pressCostByDate = new HashMap<>();
		Map<LocalDate, BigDecimal> shippingCostByDate = new HashMap<>();
		for (Map<String, Object> row : costRows) {
			LocalDate costDate = toLocalDate(row.get("costDate"));
			Integer costType = toInteger(row.get("costType"));
			BigDecimal costAmount = toBigDecimal(row.get("costAmount"));
			if (costDate == null || costType == null) {
				continue;
			}
			if (costType == 1) {
				pressCostByDate.put(costDate, costAmount);
			} else if (costType == 2) {
				shippingCostByDate.put(costDate, costAmount);
			}
		}
		return startDate.datesUntil(endDate.plusDays(1))
				.map(date -> {
					Map<String, Object> row = salesByDate.get(date);
					return Map.of(
							"label", date.format(CHART_DATE_FORMAT),
							"amount", row == null ? BigDecimal.ZERO : row.get("amount"),
							"receivedPaid", row == null ? BigDecimal.ZERO : row.get("receivedPaid"),
							"pledgePaid", row == null ? BigDecimal.ZERO : row.get("pledgePaid"),
							"pressCost", pressCostByDate.getOrDefault(date, BigDecimal.ZERO),
							"shippingCost", shippingCostByDate.getOrDefault(date, BigDecimal.ZERO));
				})
				.toList();
	}

	private List<Map<String, Object>> getSalesShareChart(String labelExpression, String groupExpression,
			LocalDate startDate, LocalDate endDate) {
		return jdbcTemplate.queryForList("""
				SELECT %s AS label,
				       COALESCE(SUM(q.od_qty), 0) AS value
				FROM q_order_detail q
				JOIN t_order o ON o.ID_order = q.ID_order
				WHERE o.o_order_date BETWEEN ? AND ?
				GROUP BY %s, %s
				ORDER BY value DESC, label
				""".formatted(labelExpression, groupExpression, labelExpression), startDate, endDate);
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

	private Integer toInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		try {
			return Integer.valueOf(value.toString());
		} catch (RuntimeException e) {
			return null;
		}
	}

	private LocalDate toLocalDate(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof java.sql.Date sqlDate) {
			return sqlDate.toLocalDate();
		}
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		try {
			return LocalDate.parse(value.toString());
		} catch (RuntimeException e) {
			return null;
		}
	}

	private LocalDate[] getSelectedRange(String startDate, String endDate) {
		LocalDate selectedStartDate = parseDate(startDate, DEFAULT_START_DATE);
		LocalDate selectedEndDate = parseDate(endDate, DEFAULT_END_DATE);
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

