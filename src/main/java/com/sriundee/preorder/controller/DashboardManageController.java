package com.sriundee.preorder.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sriundee.preorder.entity.Setting;
import com.sriundee.preorder.repository.SettingRepository;

@Controller
public class DashboardManageController {

    private static final int MENU_ID = 18;
    private static final String DASHBOARD_WIDGETS_KEY = "dashboard_widgets";
    private static final String DEFAULT_WIDGETS = "[]";
    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.of(2026, 12, 31);
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,##0.00");
    private static final Set<String> WIDGET_TYPES = Set.of("metric", "line", "bar", "area", "pie", "donut");
    private static final Set<String> DATASETS = Set.of(
            "totalSales", "totalFullPaid", "totalPledgePaid", "totalBalance", "totalCost", "totalOrders", "totalItems",
            "salesTrend", "artistSalesAmount", "typeSalesAmount", "artistSalesShare", "typeSalesShare",
            "orderStatusShare", "costByType");

    @Autowired
    private MenuController menuService;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/dashboard/manage")
    public String index(Model model) {
        model.addAttribute("mainMenus", menuService.getMenuList(MENU_ID, null));
        model.addAttribute("dashboardConfig", getDashboardWidgetsValue());
        model.addAttribute("dashboardCatalog", toJson(buildCatalog()));
        return "dashboard/manage";
    }

    @GetMapping("/settings/dashboard/widgets")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getDashboardWidgets() {
        return ResponseEntity.ok(Map.of("widgets", getDashboardWidgetsValue()));
    }

    @PostMapping("/settings/dashboard/widgets")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveDashboardWidgets(@RequestBody Map<String, Object> payload) {
        Object widgets = payload == null ? null : payload.get("widgets");
        String normalized = normalizeWidgets(widgets);
        saveSetting(DASHBOARD_WIDGETS_KEY, normalized);
        return ResponseEntity.ok(Map.of("widgets", normalized));
    }

    @GetMapping("/api/dashboard/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> dashboardData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = parseDate(startDate, DEFAULT_START_DATE);
        LocalDate end = parseDate(endDate, DEFAULT_END_DATE);
        if (end.isBefore(start)) {
            end = start;
        }
        return ResponseEntity.ok(buildDashboardData(start, end));
    }

    public Map<String, Object> buildDashboardData(LocalDate start, LocalDate end) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metrics", buildMetrics(start, end));
        data.put("series", buildSeries(start, end));
        data.put("shares", buildShares(start, end));
        data.put("period", formatDate(start) + " - " + formatDate(end));
        return data;
    }

    public String getDashboardWidgetsValue() {
        return getSettingValue(DASHBOARD_WIDGETS_KEY, DEFAULT_WIDGETS);
    }

    private Map<String, Object> buildCatalog() {
        List<Map<String, String>> metrics = List.of(
                metric("totalSales", "ยอดขายรวม", "money"),
                metric("totalFullPaid", "ยอดจ่ายเต็ม", "money"),
                metric("totalPledgePaid", "ยอดมัดจำที่จ่ายแล้ว", "money"),
                metric("totalBalance", "ยอดคงเหลือ", "money"),
                metric("totalCost", "ค่าใช้จ่ายรวม", "money"),
                metric("totalOrders", "จำนวนคำสั่งซื้อ", "count"),
                metric("totalItems", "จำนวนสินค้าที่ขายได้", "count"));
        List<Map<String, String>> charts = List.of(
                chart("salesTrend", "ยอดขายตามช่วงวันที่", "series"),
                chart("artistSalesAmount", "ยอดขายตามศิลปิน", "moneyShare"),
                chart("typeSalesAmount", "ยอดขายตามประเภท", "moneyShare"),
                chart("artistSalesShare", "สัดส่วนศิลปินที่ขายได้", "share"),
                chart("typeSalesShare", "สัดส่วนประเภทสินค้าที่ขายได้", "share"),
                chart("orderStatusShare", "สัดส่วนสถานะออร์เดอร์", "share"),
                chart("costByType", "ค่าใช้จ่ายตามประเภท", "share"));
        return Map.of("metrics", metrics, "charts", charts);
    }

    private Map<String, String> metric(String key, String label, String valueType) {
        return Map.of("key", key, "label", label, "valueType", valueType);
    }

    private Map<String, String> chart(String key, String label, String group) {
        return Map.of("key", key, "label", label, "group", group);
    }

    private Map<String, Object> buildMetrics(LocalDate start, LocalDate end) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        putMoneyMetric(metrics, "totalSales", "ยอดขายรวม", getBigDecimal("""
                SELECT COALESCE(SUM(o_net), 0)
                FROM q_order
                WHERE o_order_date BETWEEN ? AND ?
                """, start, end));
        putMoneyMetric(metrics, "totalFullPaid", "ยอดจ่ายเต็ม", getBigDecimal("""
                SELECT COALESCE(SUM(CASE WHEN ID_pay_method = 1 THEN o_net ELSE 0 END), 0)
                FROM q_order
                WHERE o_order_date BETWEEN ? AND ?
                """, start, end));
        putMoneyMetric(metrics, "totalPledgePaid", "ยอดมัดจำที่จ่ายแล้ว", getBigDecimal("""
                SELECT COALESCE(SUM(CASE WHEN ID_pay_method IN (2, 3) THEN o_price_pledge ELSE 0 END), 0)
                FROM q_order
                WHERE o_order_date BETWEEN ? AND ?
                """, start, end));
        putMoneyMetric(metrics, "totalBalance", "ยอดคงเหลือ", getBigDecimal("""
                SELECT COALESCE(SUM(CASE WHEN ID_pay_method = 2 THEN o_price_balance ELSE 0 END), 0)
                FROM q_order
                WHERE o_order_date BETWEEN ? AND ?
                """, start, end));
        putMoneyMetric(metrics, "totalCost", "ค่าใช้จ่ายรวม", getBigDecimal("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_cost
                WHERE c_delete = 'A'
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                """, start, end));
        putCountMetric(metrics, "totalOrders", "จำนวนคำสั่งซื้อ", getLong("""
                SELECT COUNT(*)
                FROM q_order
                WHERE o_order_date BETWEEN ? AND ?
                """, start, end));
        putCountMetric(metrics, "totalItems", "จำนวนสินค้าที่ขายได้", getLong("""
                SELECT COALESCE(SUM(od.od_qty), 0)
                FROM t_order_detail od
                JOIN t_order o ON o.ID_order = od.ID_order
                WHERE o.o_order_date BETWEEN ? AND ?
                """, start, end));
        return metrics;
    }

    private void putMoneyMetric(Map<String, Object> metrics, String key, String label, BigDecimal value) {
        metrics.put(key, Map.of("label", label, "value", value, "display", MONEY_FORMAT.format(value), "valueType", "money"));
    }

    private void putCountMetric(Map<String, Object> metrics, String key, String label, Long value) {
        metrics.put(key, Map.of("label", label, "value", value, "display", String.valueOf(value), "valueType", "count"));
    }

    private Map<String, Object> buildSeries(LocalDate start, LocalDate end) {
        Map<String, Object> series = new LinkedHashMap<>();
        series.put("salesTrend", jdbcTemplate.queryForList("""
                SELECT TO_CHAR(o_order_date, 'DD/MM/YYYY') AS label,
                       COALESCE(SUM(o_net), 0) AS amount,
                       COALESCE(SUM(CASE WHEN ID_pay_method IN (1, 3) THEN o_net WHEN ID_pay_method = 2 THEN o_price_pledge ELSE 0 END), 0) AS receivedPaid,
                       COALESCE(SUM(CASE WHEN ID_pay_method = 2 THEN o_price_pledge ELSE 0 END), 0) AS pledgePaid,
                       COALESCE(SUM(CASE WHEN ID_pay_method = 2 THEN o_price_balance ELSE 0 END), 0) AS balance
                FROM q_order
                WHERE o_order_date BETWEEN ? AND ?
                GROUP BY o_order_date
                ORDER BY o_order_date
                """, start, end));
        return series;
    }

    private Map<String, Object> buildShares(LocalDate start, LocalDate end) {
        Map<String, Object> shares = new LinkedHashMap<>();
        shares.put("artistSalesAmount", shareRows("""
                SELECT COALESCE(q.a_name, 'ไม่ระบุศิลปิน') AS label,
                       COALESCE(SUM(CAST(REPLACE(q.od_price_total, ',', '') AS DECIMAL(14,2))), 0) AS value
                FROM q_order_detail q
                JOIN t_order o ON o.ID_order = q.ID_order
                WHERE o.o_order_date BETWEEN ? AND ?
                GROUP BY q.ID_art, q.a_name
                ORDER BY value DESC, label
                """, start, end));
        shares.put("typeSalesAmount", shareRows("""
                SELECT COALESCE(q.t_name, 'ไม่ระบุประเภท') AS label,
                       COALESCE(SUM(CAST(REPLACE(q.od_price_total, ',', '') AS DECIMAL(14,2))), 0) AS value
                FROM q_order_detail q
                JOIN t_order o ON o.ID_order = q.ID_order
                WHERE o.o_order_date BETWEEN ? AND ?
                GROUP BY q.ID_type, q.t_name
                ORDER BY value DESC, label
                """, start, end));
        shares.put("artistSalesShare", shareRows("""
                SELECT COALESCE(q.a_name, 'ไม่ระบุศิลปิน') AS label,
                       COALESCE(SUM(q.od_qty), 0) AS value
                FROM q_order_detail q
                JOIN t_order o ON o.ID_order = q.ID_order
                WHERE o.o_order_date BETWEEN ? AND ?
                GROUP BY q.ID_art, q.a_name
                ORDER BY value DESC, label
                """, start, end));
        shares.put("typeSalesShare", shareRows("""
                SELECT COALESCE(q.t_name, 'ไม่ระบุประเภท') AS label,
                       COALESCE(SUM(q.od_qty), 0) AS value
                FROM q_order_detail q
                JOIN t_order o ON o.ID_order = q.ID_order
                WHERE o.o_order_date BETWEEN ? AND ?
                GROUP BY q.ID_type, q.t_name
                ORDER BY value DESC, label
                """, start, end));
        shares.put("orderStatusShare", shareRows("""
                SELECT COALESCE(os_name, 'ไม่ระบุสถานะ') AS label,
                       COUNT(*) AS value
                FROM q_order_detail q
                JOIN t_order o ON o.ID_order = q.ID_order
                WHERE o.o_order_date BETWEEN ? AND ?
                GROUP BY ID_order_status, os_name
                ORDER BY value DESC, label
                """, start, end));
        shares.put("costByType", shareRows("""
                SELECT COALESCE(tc_name, 'ไม่ระบุประเภท') AS label,
                       COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0) AS value
                FROM q_cost
                WHERE c_delete = 'A'
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                GROUP BY ID_type_cost, tc_name
                ORDER BY ID_type_cost
                """, start, end));
        return shares;
    }

    private List<Map<String, Object>> shareRows(String sql, LocalDate start, LocalDate end) {
        return jdbcTemplate.queryForList(sql, start, end);
    }

    @SuppressWarnings("unchecked")
    private String normalizeWidgets(Object widgetsValue) {
        List<Map<String, Object>> source;
        if (widgetsValue instanceof List<?> list) {
            source = (List<Map<String, Object>>) list;
        } else if (widgetsValue instanceof String text) {
            try {
                source = new ObjectMapper().readValue(text, List.class);
            } catch (RuntimeException | JsonProcessingException e) {
                source = List.of();
            }
        } else {
            source = List.of();
        }

        List<Map<String, Object>> widgets = new ArrayList<>();
        int order = 1;
        for (Map<String, Object> item : source) {
            String type = text(item.get("type"));
            String dataset = text(item.get("dataset"));
            if (!WIDGET_TYPES.contains(type) || !DATASETS.contains(dataset)) {
                continue;
            }
            Map<String, Object> widget = new LinkedHashMap<>();
            widget.put("id", text(item.get("id")).isBlank() ? "widget-" + order : text(item.get("id")));
            widget.put("title", text(item.get("title")).isBlank() ? dataset : text(item.get("title")));
            widget.put("type", type);
            widget.put("dataset", dataset);
            widget.put("width", normalizeWidth(text(item.get("width"))));
            widget.put("tone", normalizeTone(text(item.get("tone"))));
            widget.put("order", order++);
            widgets.add(widget);
        }
        return toJson(widgets);
    }

    private String normalizeWidth(String width) {
        return Set.of("25", "33", "50", "66", "75", "100").contains(width) ? width : "50";
    }

    private String normalizeTone(String tone) {
        return Set.of("primary", "success", "info", "warning", "danger", "orange").contains(tone) ? tone : "primary";
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private Long getLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private BigDecimal getBigDecimal(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
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

    private String getSettingValue(String key, String defaultValue) {
        Setting setting = settingRepository.findFirstByKeyAndUserIdIsNull(key);
        return setting == null || setting.getValue() == null ? defaultValue : setting.getValue();
    }

    private void saveSetting(String key, String value) {
        Setting setting = settingRepository.findFirstByKeyAndUserIdIsNull(key);
        if (setting == null) {
            setting = new Setting();
            setting.setKey(key);
            setting.setUserId(null);
        }
        setting.setValue(value);
        settingRepository.save(setting);
    }

    private String toJson(Object value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}

