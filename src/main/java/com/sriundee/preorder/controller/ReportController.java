package com.sriundee.preorder.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportController {

    private static final int MENU_ID = 15;
    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.of(2026, 12, 31);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,##0.00");

    @Autowired
    private MenuController menuService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/reports")
    public String index(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        LocalDate start = parseDate(startDate, DEFAULT_START_DATE);
        LocalDate end = parseDate(endDate, DEFAULT_END_DATE);
        if (end.isBefore(start)) {
            end = start;
        }

        BigDecimal income = money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_income
                WHERE c_delete = 'A'
                  AND c_create_date BETWEEN ? AND ?
                """, start, end);
        BigDecimal expense = money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_cost
                WHERE c_delete = 'A'
                  AND c_create_date BETWEEN ? AND ?
                """, start, end);
        BigDecimal receivable = money("""
                SELECT COALESCE(SUM(CASE WHEN ID_pay_method = 2 THEN o_price_balance ELSE 0 END), 0)
                FROM q_order
                WHERE o_order_date BETWEEN ? AND ?
                """, start, end);
        BigDecimal sales = money("""
                SELECT COALESCE(SUM(o_net), 0)
                FROM q_order
                WHERE o_order_date BETWEEN ? AND ?
                """, start, end);
        long receivableOrders = count("""
                SELECT COUNT(*)
                FROM q_order
                WHERE ID_pay_method = 2
                  AND COALESCE(o_price_balance, 0) > 0
                  AND o_order_date BETWEEN ? AND ?
                """, start, end);

        model.addAttribute("mainMenus", menuService.getMenuList(MENU_ID, null));
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("reportPeriod", displayDate(start) + " - " + displayDate(end));
        model.addAttribute("financeIncome", displayMoney(income));
        model.addAttribute("financeExpense", displayMoney(expense));
        model.addAttribute("financeReceivable", displayMoney(receivable));
        model.addAttribute("financeProfit", displayMoney(income.subtract(expense)));
        model.addAttribute("financeRows", financeRows(start, end));

        model.addAttribute("incomeFull", displayMoney(incomeByType(1, start, end)));
        model.addAttribute("incomePledge", displayMoney(incomeByType(2, start, end)));
        model.addAttribute("incomeBalance", displayMoney(incomeByType(3, start, end)));
        model.addAttribute("incomeRows", incomeRows(start, end));

        model.addAttribute("receivableTotal", displayMoney(receivable));
        model.addAttribute("receivableOrders", receivableOrders);
        model.addAttribute("receivableRows", receivableRows(start, end));

        model.addAttribute("expensePress", displayMoney(costByType(1, start, end)));
        model.addAttribute("expenseShipping", displayMoney(costByType(2, start, end)));
        model.addAttribute("expensePostal", displayMoney(costByType(5, start, end)));
        model.addAttribute("expenseRows", expenseRows(start, end));

        model.addAttribute("profitIncome", displayMoney(income));
        model.addAttribute("profitExpense", displayMoney(expense));
        model.addAttribute("profitValue", displayMoney(income.subtract(expense)));
        model.addAttribute("profitMargin", displayPercent(income, income.subtract(expense)));
        model.addAttribute("profitRows", profitRows(start, end));

        model.addAttribute("productQty", count("""
                SELECT COALESCE(SUM(od.od_qty), 0)
                FROM t_order_detail od
                JOIN t_order o ON o.ID_order = od.ID_order
                WHERE o.o_order_date BETWEEN ? AND ?
                """, start, end));
        model.addAttribute("productSales", displayMoney(sales));
        model.addAttribute("productRows", productRows(start, end));
        model.addAttribute("productDetailRows", productDetailRows(start, end));
        model.addAttribute("lotRows", lotRows(start, end));
        model.addAttribute("statusRows", statusRows(start, end));
        return "report/index";
    }

    private List<Map<String, Object>> financeRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT period,
                       SUM(sales) AS sales,
                       SUM(income) AS income,
                       SUM(expense) AS expense,
                       SUM(receivable) AS receivable
                FROM (
                    SELECT DATE_FORMAT(o_order_date, '%m/%Y') AS period,
                           DATE_FORMAT(o_order_date, '%Y-%m') AS sort_period,
                           SUM(o_net) AS sales, 0 AS income, 0 AS expense,
                           SUM(CASE WHEN ID_pay_method = 2 THEN o_price_balance ELSE 0 END) AS receivable
                    FROM q_order
                    WHERE o_order_date BETWEEN ? AND ?
                    GROUP BY DATE_FORMAT(o_order_date, '%m/%Y'), DATE_FORMAT(o_order_date, '%Y-%m')
                    UNION ALL
                    SELECT DATE_FORMAT(c_create_date, '%m/%Y'), DATE_FORMAT(c_create_date, '%Y-%m'),
                           0, SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0, 0
                    FROM q_income
                    WHERE c_delete = 'A' AND c_create_date BETWEEN ? AND ?
                    GROUP BY DATE_FORMAT(c_create_date, '%m/%Y'), DATE_FORMAT(c_create_date, '%Y-%m')
                    UNION ALL
                    SELECT DATE_FORMAT(c_create_date, '%m/%Y'), DATE_FORMAT(c_create_date, '%Y-%m'),
                           0, 0, SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0
                    FROM q_cost
                    WHERE c_delete = 'A' AND c_create_date BETWEEN ? AND ?
                    GROUP BY DATE_FORMAT(c_create_date, '%m/%Y'), DATE_FORMAT(c_create_date, '%Y-%m')
                ) report_month
                GROUP BY period, sort_period
                ORDER BY sort_period
                """, start, end, start, end, start, end), row -> map(
                "period", text(row.get("period")),
                "sales", displayMoney(row.get("sales")),
                "income", displayMoney(row.get("income")),
                "expense", displayMoney(row.get("expense")),
                "receivable", displayMoney(row.get("receivable")),
                "profit", displayMoney(decimal(row.get("income")).subtract(decimal(row.get("expense"))))));
    }

    private List<Map<String, Object>> incomeRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT i.c_create_date, o.o_order_code, i.c_customer_name, i.ti_name, i.c_note, i.c_price
                FROM q_income i
                LEFT JOIN t_order o ON o.ID_order = i.ID_order
                WHERE i.c_delete = 'A'
                  AND i.c_create_date BETWEEN ? AND ?
                ORDER BY i.c_create_date DESC, i.ID_income DESC
                """, start, end), row -> map(
                "date", displayDate(row.get("c_create_date")),
                "code", defaultText(row.get("o_order_code"), "-"),
                "customer", text(row.get("c_customer_name")),
                "type", text(row.get("ti_name")),
                "note", defaultText(row.get("c_note"), "-"),
                "price", displayMoney(row.get("c_price"))));
    }

    private List<Map<String, Object>> receivableRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT o.o_order_code, o.o_order_date, o.o_customer_name, o.o_net,
                       o.o_price_pledge, o.o_price_balance,
                       COALESCE(GROUP_CONCAT(DISTINCT os.os_name ORDER BY os.ID_order_status SEPARATOR ', '), '-') AS statuses
                FROM q_order o
                LEFT JOIN t_order_detail od ON od.ID_order = o.ID_order
                LEFT JOIN t_order_status os ON os.ID_order_status = od.ID_order_status
                WHERE o.ID_pay_method = 2
                  AND COALESCE(o.o_price_balance, 0) > 0
                  AND o.o_order_date BETWEEN ? AND ?
                GROUP BY o.ID_order, o.o_order_code, o.o_order_date, o.o_customer_name,
                         o.o_net, o.o_price_pledge, o.o_price_balance
                ORDER BY o.o_order_date DESC, o.ID_order DESC
                """, start, end), row -> map(
                "code", text(row.get("o_order_code")),
                "date", displayDate(row.get("o_order_date")),
                "customer", text(row.get("o_customer_name")),
                "statuses", text(row.get("statuses")),
                "net", displayMoney(row.get("o_net")),
                "pledge", displayMoney(row.get("o_price_pledge")),
                "balance", displayMoney(row.get("o_price_balance"))));
    }

    private List<Map<String, Object>> expenseRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT c_create_date, tc_name, c_note, c_delete, c_price
                FROM q_cost
                WHERE c_delete = 'A'
                  AND c_create_date BETWEEN ? AND ?
                ORDER BY c_create_date DESC, ID_cost DESC
                """, start, end), row -> map(
                "date", displayDate(row.get("c_create_date")),
                "type", text(row.get("tc_name")),
                "note", defaultText(row.get("c_note"), "-"),
                "status", "สำเร็จ",
                "price", displayMoney(row.get("c_price"))));
    }

    private List<Map<String, Object>> profitRows(LocalDate start, LocalDate end) {
        return List.of(
                map("name", "รายรับจากจ่ายเต็ม", "value", displayMoney(incomeByType(1, start, end)), "note", "รายรับจริงจาก t_income"),
                map("name", "รายรับจากมัดจำ", "value", displayMoney(incomeByType(2, start, end)), "note", "รายรับมัดจำจาก t_income"),
                map("name", "รายรับยอดมัดจำที่เหลือ", "value", displayMoney(incomeByType(3, start, end)), "note", "ยอดที่รับเพิ่มจาก t_income"),
                map("name", "ค่าใช้จ่ายรวม", "value", "-" + displayMoney(costAll(start, end)), "note", "ต้นทุนและค่าใช้จ่ายจาก t_cost"));
    }

    private List<Map<String, Object>> productRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT p.p_name, a.a_name, ty.t_name,
                       SUM(od.od_qty) AS qty,
                       SUM(od.od_price_total) AS sales,
                       SUM(od.od_price_pledge) AS pledge,
                       SUM(CASE WHEN o.ID_pay_method = 2 THEN od.od_price_balance ELSE 0 END) AS balance
                FROM t_order_detail od
                JOIN t_order o ON o.ID_order = od.ID_order
                JOIN t_cover c ON c.ID_cover = od.ID_cover
                JOIN t_product p ON p.ID_product = c.ID_pro
                JOIN t_artist a ON a.ID_art = p.ID_art
                JOIN t_type ty ON ty.ID_type = p.ID_type
                WHERE o.o_order_date BETWEEN ? AND ?
                GROUP BY p.ID_product, p.p_name, a.a_name, ty.t_name
                ORDER BY qty DESC, p.p_name
                """, start, end), row -> map(
                "product", text(row.get("p_name")),
                "artist", text(row.get("a_name")),
                "type", text(row.get("t_name")),
                "qty", text(row.get("qty")),
                "sales", displayMoney(row.get("sales")),
                "pledge", displayMoney(row.get("pledge")),
                "balance", displayMoney(row.get("balance"))));
    }

    private List<Map<String, Object>> productDetailRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT p.p_name, w.w_name, v.v_name, c.c_name,
                       SUM(od.od_qty) AS ordered,
                       SUM(CASE WHEN od.ID_order_status >= 2 THEN od.od_qty ELSE 0 END) AS pressed,
                       SUM(CASE WHEN od.ID_order_status >= 3 THEN od.od_qty ELSE 0 END) AS in_lot,
                       SUM(CASE WHEN od.ID_order_status >= 4 THEN od.od_qty ELSE 0 END) AS arrived,
                       SUM(CASE WHEN od.ID_order_status = 5 THEN od.od_qty ELSE 0 END) AS sent
                FROM t_order_detail od
                JOIN t_order o ON o.ID_order = od.ID_order
                JOIN t_cover c ON c.ID_cover = od.ID_cover
                JOIN t_product p ON p.ID_product = c.ID_pro
                JOIN t_website w ON w.ID_web = c.ID_web
                JOIN t_version v ON v.ID_ver = c.ID_ver
                WHERE o.o_order_date BETWEEN ? AND ?
                GROUP BY c.ID_cover, p.p_name, w.w_name, v.v_name, c.c_name
                ORDER BY p.p_name, w.w_name, v.v_name, c.c_name
                """, start, end), row -> map(
                "product", text(row.get("p_name")),
                "website", text(row.get("w_name")),
                "version", text(row.get("v_name")),
                "cover", text(row.get("c_name")),
                "ordered", text(row.get("ordered")),
                "pressed", text(row.get("pressed")),
                "lot", text(row.get("in_lot")),
                "arrived", text(row.get("arrived")),
                "sent", text(row.get("sent"))));
    }

    private List<Map<String, Object>> lotRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT l.l_lot_number, l.l_create_date, l.l_start_date, l.l_end_date, l.l_arrive_date,
                       COUNT(DISTINCT ld.ID_lot_detail) AS detail_count,
                       COALESCE(SUM(od.od_qty), 0) AS qty,
                       COALESCE(SUM(od.od_price_total), 0) AS value
                FROM t_lot l
                LEFT JOIN t_lot_detail ld ON ld.ID_lot = l.ID_lot
                LEFT JOIN t_order_detail od ON od.ID_order_detail = ld.ID_order_detail
                WHERE l.l_delete = 'A'
                  AND l.l_create_date BETWEEN ? AND ?
                GROUP BY l.ID_lot, l.l_lot_number, l.l_create_date, l.l_start_date, l.l_end_date, l.l_arrive_date
                ORDER BY l.l_create_date DESC, l.ID_lot DESC
                """, start, end), row -> map(
                "lot", text(row.get("l_lot_number")),
                "created", displayDate(row.get("l_create_date")),
                "range", displayRange(row.get("l_start_date"), row.get("l_end_date")),
                "arrive", defaultText(displayDate(row.get("l_arrive_date")), "-"),
                "details", text(row.get("detail_count")),
                "qty", text(row.get("qty")),
                "value", displayMoney(row.get("value"))));
    }

    private List<Map<String, Object>> statusRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT os.os_name, os.os_color,
                       COUNT(od.ID_order_detail) AS details,
                       COALESCE(SUM(od.od_qty), 0) AS qty,
                       COALESCE(SUM(od.od_price_total), 0) AS value,
                       COALESCE(SUM(CASE WHEN o.ID_pay_method = 2 THEN od.od_price_balance ELSE 0 END), 0) AS balance
                FROM t_order_detail od
                JOIN t_order o ON o.ID_order = od.ID_order
                JOIN t_order_status os ON os.ID_order_status = od.ID_order_status
                WHERE o.o_order_date BETWEEN ? AND ?
                GROUP BY os.ID_order_status, os.os_name, os.os_color
                ORDER BY os.ID_order_status
                """, start, end), row -> map(
                "name", text(row.get("os_name")),
                "color", text(row.get("os_color")),
                "details", text(row.get("details")),
                "qty", text(row.get("qty")),
                "value", displayMoney(row.get("value")),
                "balance", displayMoney(row.get("balance"))));
    }

    private BigDecimal incomeByType(int type, LocalDate start, LocalDate end) {
        return money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_income
                WHERE c_delete = 'A'
                  AND ID_type_income = ?
                  AND c_create_date BETWEEN ? AND ?
                """, type, start, end);
    }

    private BigDecimal costByType(int type, LocalDate start, LocalDate end) {
        return money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_cost
                WHERE c_delete = 'A'
                  AND ID_type_cost = ?
                  AND c_create_date BETWEEN ? AND ?
                """, type, start, end);
    }

    private BigDecimal costAll(LocalDate start, LocalDate end) {
        return money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_cost
                WHERE c_delete = 'A'
                  AND c_create_date BETWEEN ? AND ?
                """, start, end);
    }

    private BigDecimal money(String sql, Object... args) {
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return result == null ? BigDecimal.ZERO : result;
    }

    private long count(String sql, Object... args) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    private List<Map<String, Object>> transform(List<Map<String, Object>> rows, RowMapper mapper) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapper.map(row));
        }
        return result;
    }

    private Map<String, Object> map(Object... values) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(values[i].toString(), values[i + 1]);
        }
        return result;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        try {
            return new BigDecimal(value.toString().replace(",", ""));
        } catch (RuntimeException e) {
            return BigDecimal.ZERO;
        }
    }

    private String displayMoney(Object value) {
        return MONEY_FORMAT.format(decimal(value));
    }

    private String displayDate(Object value) {
        if (value == null || value.toString().isBlank()) {
            return "";
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().format(DATE_FORMAT);
        }
        try {
            return LocalDate.parse(value.toString()).format(DATE_FORMAT);
        } catch (RuntimeException e) {
            return value.toString();
        }
    }

    private String displayRange(Object start, Object end) {
        String displayStart = displayDate(start);
        String displayEnd = displayDate(end);
        if (displayStart.isBlank() && displayEnd.isBlank()) {
            return "-";
        }
        return displayStart + " ถึง " + displayEnd;
    }

    private String displayPercent(BigDecimal base, BigDecimal value) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return "0.00%";
        }
        return MONEY_FORMAT.format(value.multiply(BigDecimal.valueOf(100)).divide(base, 2, java.math.RoundingMode.HALF_UP)) + "%";
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String defaultText(Object value, String fallback) {
        String result = text(value);
        return result.isBlank() ? fallback : result;
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

    @FunctionalInterface
    private interface RowMapper {
        Map<String, Object> map(Map<String, Object> row);
    }
}
