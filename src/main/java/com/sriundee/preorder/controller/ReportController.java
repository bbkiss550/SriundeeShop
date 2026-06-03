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

    private static final int REPORT_MENU_ID = 15;
    private static final int PT00_MENU_ID = 19;
    private static final int PT01_MENU_ID = 20;
    private static final int PT02_MENU_ID = 21;
    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.of(2026, 12, 31);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,##0.00");

    @Autowired
    private MenuController menuService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping({"/reports", "/reports/PT00"})
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
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                """, start, end);
        BigDecimal expense = money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_cost
                WHERE c_delete = 'A'
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
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

        model.addAttribute("mainMenus", menuService.getMenuList(PT00_MENU_ID, REPORT_MENU_ID));
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("reportPeriod", displayDate(start) + " - " + displayDate(end));
        model.addAttribute("financeIncome", displayMoney(income));
        model.addAttribute("financeExpense", displayMoney(expense));
        model.addAttribute("financeReceivable", displayMoney(receivable));
        model.addAttribute("financeProfit", displayMoney(income.subtract(expense)));
        model.addAttribute("financeRows", financeRows(start, end));
        model.addAttribute("taxLedgerIncome", displayMoney(income));
        model.addAttribute("taxLedgerExpense", displayMoney(expense));
        model.addAttribute("taxLedgerNet", displayMoney(income.subtract(expense)));
        model.addAttribute("taxLedgerRows", taxLedgerRows(start, end));

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
        model.addAttribute("orderProfitRows", orderProfitRows(start, end));

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

    @GetMapping("/reports/PT01")
    public String pt01(
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
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                """, start, end);
        BigDecimal expense = money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_cost
                WHERE c_delete = 'A'
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                """, start, end);

        model.addAttribute("mainMenus", menuService.getMenuList(PT01_MENU_ID, REPORT_MENU_ID));
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("reportPeriod", displayDate(start) + " - " + displayDate(end));
        model.addAttribute("ledgerIncome", displayMoney(income));
        model.addAttribute("ledgerExpense", displayMoney(expense));
        model.addAttribute("ledgerNet", displayMoney(income.subtract(expense)));
        model.addAttribute("journalRows", journalRows(start, end));
        model.addAttribute("cashBookRows", cashBookRows(start, end));
        return "report/PT01";
    }

    @GetMapping("/reports/PT02")
    public String pt02(
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
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                """, start, end);
        BigDecimal expense = money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_cost
                WHERE c_delete = 'A'
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                """, start, end);
        BigDecimal profit = income.subtract(expense);

        model.addAttribute("mainMenus", menuService.getMenuList(PT02_MENU_ID, REPORT_MENU_ID));
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("reportPeriod", displayDate(start) + " - " + displayDate(end));
        model.addAttribute("profitLossIncome", displayMoney(income));
        model.addAttribute("profitLossExpense", displayMoney(expense));
        model.addAttribute("profitLossNet", displayMoney(profit));
        model.addAttribute("profitLossMargin", displayPercent(income, profit));
        model.addAttribute("profitLossRows", profitLossRows(start, end));
        model.addAttribute("profitLossMonthlyRows", profitLossMonthlyRows(start, end));
        model.addAttribute("orderProfitRows", orderProfitRows(start, end));
        return "report/PT02";
    }

    private List<Map<String, Object>> journalRows(LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *
                FROM (
                    SELECT CAST(i.c_create_date AS DATE) AS tx_date,
                           1 AS sort_type,
                           i.ID_income AS ref_id,
                           COALESCE(i.ti_name, 'รายรับ') AS category,
                           COALESCE(o.o_order_code, '-') AS document_no,
                           COALESCE(i.c_customer_name, '') AS party,
                           COALESCE(i.c_note, '') AS detail,
                           COALESCE(CAST(NULLIF(REPLACE(i.c_price, ',', ''), '') AS NUMERIC(14,2)), 0) AS amount
                    FROM q_income i
                    LEFT JOIN t_order o ON o.ID_order = i.ID_order
                    WHERE i.c_delete = 'A'
                      AND CAST(i.c_create_date AS DATE) BETWEEN ? AND ?
                    UNION ALL
                    SELECT CAST(c.c_create_date AS DATE) AS tx_date,
                           2 AS sort_type,
                           c.ID_cost AS ref_id,
                           COALESCE(c.tc_name, 'รายจ่าย') AS category,
                           '-' AS document_no,
                           '' AS party,
                           COALESCE(c.c_note, '') AS detail,
                           COALESCE(CAST(NULLIF(REPLACE(c.c_price, ',', ''), '') AS NUMERIC(14,2)), 0) AS amount
                    FROM q_cost c
                    WHERE c.c_delete = 'A'
                      AND CAST(c.c_create_date AS DATE) BETWEEN ? AND ?
                ) journal
                ORDER BY tx_date, sort_type, ref_id
                """, start, end, start, end);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            boolean income = Integer.parseInt(row.get("sort_type").toString()) == 1;
            String date = displayDate(row.get("tx_date"));
            String document = defaultText(row.get("document_no"), "-");
            String party = defaultText(row.get("party"), "-");
            String category = text(row.get("category"));
            String detail = defaultText(row.get("detail"), "-");
            String amount = displayMoney(row.get("amount"));
            if (income) {
                result.add(map(
                        "date", date,
                        "document", document,
                        "party", party,
                        "account", "เงินสด/เงินฝาก",
                        "detail", detail,
                        "debit", amount,
                        "credit", "-"));
                result.add(map(
                        "date", "",
                        "document", "",
                        "party", "",
                        "account", "รายรับ - " + category,
                        "detail", detail,
                        "debit", "-",
                        "credit", amount));
            } else {
                result.add(map(
                        "date", date,
                        "document", document,
                        "party", party,
                        "account", "ค่าใช้จ่าย - " + category,
                        "detail", detail,
                        "debit", amount,
                        "credit", "-"));
                result.add(map(
                        "date", "",
                        "document", "",
                        "party", "",
                        "account", "เงินสด/เงินฝาก",
                        "detail", detail,
                        "debit", "-",
                        "credit", amount));
            }
        }
        return result;
    }

    private List<Map<String, Object>> cashBookRows(LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *
                FROM (
                    SELECT CAST(i.c_create_date AS DATE) AS tx_date,
                           1 AS sort_type,
                           i.ID_income AS ref_id,
                           COALESCE(i.ti_name, 'รายรับ') AS category,
                           COALESCE(o.o_order_code, '-') AS document_no,
                           COALESCE(i.c_customer_name, '') AS party,
                           COALESCE(i.c_note, '') AS detail,
                           COALESCE(CAST(NULLIF(REPLACE(i.c_price, ',', ''), '') AS NUMERIC(14,2)), 0) AS income_amount,
                           CAST(0 AS NUMERIC(14,2)) AS expense_amount
                    FROM q_income i
                    LEFT JOIN t_order o ON o.ID_order = i.ID_order
                    WHERE i.c_delete = 'A'
                      AND CAST(i.c_create_date AS DATE) BETWEEN ? AND ?
                    UNION ALL
                    SELECT CAST(c.c_create_date AS DATE) AS tx_date,
                           2 AS sort_type,
                           c.ID_cost AS ref_id,
                           COALESCE(c.tc_name, 'รายจ่าย') AS category,
                           '-' AS document_no,
                           '' AS party,
                           COALESCE(c.c_note, '') AS detail,
                           CAST(0 AS NUMERIC(14,2)) AS income_amount,
                           COALESCE(CAST(NULLIF(REPLACE(c.c_price, ',', ''), '') AS NUMERIC(14,2)), 0) AS expense_amount
                    FROM q_cost c
                    WHERE c.c_delete = 'A'
                      AND CAST(c.c_create_date AS DATE) BETWEEN ? AND ?
                ) cash_book
                ORDER BY tx_date, sort_type, ref_id
                """, start, end, start, end);

        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal income = decimal(row.get("income_amount"));
            BigDecimal expense = decimal(row.get("expense_amount"));
            balance = balance.add(income).subtract(expense);
            result.add(map(
                    "date", displayDate(row.get("tx_date")),
                    "document", defaultText(row.get("document_no"), "-"),
                    "party", defaultText(row.get("party"), "-"),
                    "category", text(row.get("category")),
                    "detail", defaultText(row.get("detail"), "-"),
                    "income", displayMoney(income),
                    "expense", displayMoney(expense),
                    "balance", displayMoney(balance)));
        }
        return result;
    }

    private List<Map<String, Object>> financeRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT period,
                       SUM(sales) AS sales,
                       SUM(income) AS income,
                       SUM(expense) AS expense,
                       SUM(receivable) AS receivable
                FROM (
                    SELECT TO_CHAR(o_order_date, 'MM/YYYY') AS period,
                           TO_CHAR(o_order_date, 'YYYY-MM') AS sort_period,
                           SUM(o_net) AS sales, 0 AS income, 0 AS expense,
                           SUM(CASE WHEN ID_pay_method = 2 THEN o_price_balance ELSE 0 END) AS receivable
                    FROM q_order
                    WHERE o_order_date BETWEEN ? AND ?
                    GROUP BY TO_CHAR(o_order_date, 'MM/YYYY'), TO_CHAR(o_order_date, 'YYYY-MM')
                    UNION ALL
                    SELECT TO_CHAR(CAST(c_create_date AS DATE), 'MM/YYYY'), TO_CHAR(CAST(c_create_date AS DATE), 'YYYY-MM'),
                           0, SUM(CAST(REPLACE(c_price, ',', '') AS NUMERIC(14,2))), 0, 0
                    FROM q_income
                    WHERE c_delete = 'A' AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                    GROUP BY TO_CHAR(CAST(c_create_date AS DATE), 'MM/YYYY'), TO_CHAR(CAST(c_create_date AS DATE), 'YYYY-MM')
                    UNION ALL
                    SELECT TO_CHAR(CAST(c_create_date AS DATE), 'MM/YYYY'), TO_CHAR(CAST(c_create_date AS DATE), 'YYYY-MM'),
                           0, 0, SUM(CAST(REPLACE(c_price, ',', '') AS NUMERIC(14,2))), 0
                    FROM q_cost
                    WHERE c_delete = 'A' AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                    GROUP BY TO_CHAR(CAST(c_create_date AS DATE), 'MM/YYYY'), TO_CHAR(CAST(c_create_date AS DATE), 'YYYY-MM')
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

    private List<Map<String, Object>> profitLossRows(LocalDate start, LocalDate end) {
        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal incomeTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;

        List<Map<String, Object>> incomeRows = jdbcTemplate.queryForList("""
                SELECT COALESCE(ti_name, 'รายรับ') AS category,
                       COALESCE(SUM(CAST(NULLIF(REPLACE(c_price, ',', ''), '') AS NUMERIC(14,2))), 0) AS amount
                FROM q_income
                WHERE c_delete = 'A'
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                GROUP BY ti_name
                ORDER BY category
                """, start, end);
        for (Map<String, Object> row : incomeRows) {
            BigDecimal amount = decimal(row.get("amount"));
            incomeTotal = incomeTotal.add(amount);
            result.add(map(
                    "section", "รายได้",
                    "item", text(row.get("category")),
                    "amount", displayMoney(amount),
                    "rowClass", ""));
        }
        result.add(map(
                "section", "",
                "item", "รวมรายได้",
                "amount", displayMoney(incomeTotal),
                "rowClass", "table-success fw-bold"));

        List<Map<String, Object>> expenseRows = jdbcTemplate.queryForList("""
                SELECT COALESCE(tc_name, 'รายจ่าย') AS category,
                       COALESCE(SUM(CAST(NULLIF(REPLACE(c_price, ',', ''), '') AS NUMERIC(14,2))), 0) AS amount
                FROM q_cost
                WHERE c_delete = 'A'
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                GROUP BY tc_name
                ORDER BY category
                """, start, end);
        for (Map<String, Object> row : expenseRows) {
            BigDecimal amount = decimal(row.get("amount"));
            expenseTotal = expenseTotal.add(amount);
            result.add(map(
                    "section", "ค่าใช้จ่าย",
                    "item", text(row.get("category")),
                    "amount", displayMoney(amount),
                    "rowClass", ""));
        }
        result.add(map(
                "section", "",
                "item", "รวมค่าใช้จ่าย",
                "amount", displayMoney(expenseTotal),
                "rowClass", "table-danger fw-bold"));
        result.add(map(
                "section", "",
                "item", incomeTotal.subtract(expenseTotal).compareTo(BigDecimal.ZERO) >= 0 ? "กำไรสุทธิ" : "ขาดทุนสุทธิ",
                "amount", displayMoney(incomeTotal.subtract(expenseTotal)),
                "rowClass", "table-primary fw-bold"));
        return result;
    }

    private List<Map<String, Object>> profitLossMonthlyRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT period,
                       SUM(income) AS income,
                       SUM(expense) AS expense
                FROM (
                    SELECT TO_CHAR(CAST(c_create_date AS DATE), 'MM/YYYY') AS period,
                           TO_CHAR(CAST(c_create_date AS DATE), 'YYYY-MM') AS sort_period,
                           SUM(CAST(REPLACE(c_price, ',', '') AS NUMERIC(14,2))) AS income,
                           0 AS expense
                    FROM q_income
                    WHERE c_delete = 'A'
                      AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                    GROUP BY TO_CHAR(CAST(c_create_date AS DATE), 'MM/YYYY'), TO_CHAR(CAST(c_create_date AS DATE), 'YYYY-MM')
                    UNION ALL
                    SELECT TO_CHAR(CAST(c_create_date AS DATE), 'MM/YYYY') AS period,
                           TO_CHAR(CAST(c_create_date AS DATE), 'YYYY-MM') AS sort_period,
                           0 AS income,
                           SUM(CAST(REPLACE(c_price, ',', '') AS NUMERIC(14,2))) AS expense
                    FROM q_cost
                    WHERE c_delete = 'A'
                      AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                    GROUP BY TO_CHAR(CAST(c_create_date AS DATE), 'MM/YYYY'), TO_CHAR(CAST(c_create_date AS DATE), 'YYYY-MM')
                ) profit_month
                GROUP BY period, sort_period
                ORDER BY sort_period
                """, start, end, start, end), row -> {
            BigDecimal income = decimal(row.get("income"));
            BigDecimal expense = decimal(row.get("expense"));
            BigDecimal profit = income.subtract(expense);
            return map(
                    "period", text(row.get("period")),
                    "income", displayMoney(income),
                    "expense", displayMoney(expense),
                    "profit", displayMoney(profit),
                    "margin", displayPercent(income, profit));
        });
    }

    private List<Map<String, Object>> taxLedgerRows(LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *
                FROM (
                    SELECT CAST(i.c_create_date AS DATE) AS tx_date,
                           1 AS sort_type,
                           i.ID_income AS ref_id,
                           COALESCE(i.ti_name, 'รายรับ') AS category,
                           COALESCE(o.o_order_code, '-') AS document_no,
                           COALESCE(i.c_customer_name, '') AS party,
                           COALESCE(i.c_note, '') AS detail,
                           COALESCE(CAST(NULLIF(REPLACE(i.c_price, ',', ''), '') AS NUMERIC(14,2)), 0) AS income_amount,
                           CAST(0 AS NUMERIC(14,2)) AS expense_amount
                    FROM q_income i
                    LEFT JOIN t_order o ON o.ID_order = i.ID_order
                    WHERE i.c_delete = 'A'
                      AND CAST(i.c_create_date AS DATE) BETWEEN ? AND ?
                    UNION ALL
                    SELECT CAST(c.c_create_date AS DATE) AS tx_date,
                           2 AS sort_type,
                           c.ID_cost AS ref_id,
                           COALESCE(c.tc_name, 'รายจ่าย') AS category,
                           '-' AS document_no,
                           '' AS party,
                           COALESCE(c.c_note, '') AS detail,
                           CAST(0 AS NUMERIC(14,2)) AS income_amount,
                           COALESCE(CAST(NULLIF(REPLACE(c.c_price, ',', ''), '') AS NUMERIC(14,2)), 0) AS expense_amount
                    FROM q_cost c
                    WHERE c.c_delete = 'A'
                      AND CAST(c.c_create_date AS DATE) BETWEEN ? AND ?
                ) ledger
                ORDER BY tx_date, sort_type, ref_id
                """, start, end, start, end);

        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal income = decimal(row.get("income_amount"));
            BigDecimal expense = decimal(row.get("expense_amount"));
            balance = balance.add(income).subtract(expense);
            result.add(map(
                    "date", displayDate(row.get("tx_date")),
                    "category", text(row.get("category")),
                    "document", defaultText(row.get("document_no"), "-"),
                    "party", defaultText(row.get("party"), "-"),
                    "detail", defaultText(row.get("detail"), "-"),
                    "income", displayMoney(income),
                    "expense", displayMoney(expense),
                    "balance", displayMoney(balance)));
        }
        return result;
    }

    private List<Map<String, Object>> incomeRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT i.c_create_date, o.o_order_code, i.c_customer_name, i.ti_name, i.c_note, i.c_price
                FROM q_income i
                LEFT JOIN t_order o ON o.ID_order = i.ID_order
                WHERE i.c_delete = 'A'
                  AND CAST(i.c_create_date AS DATE) BETWEEN ? AND ?
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
                       COALESCE(STRING_AGG(DISTINCT os.os_name, ', '), '-') AS statuses
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
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
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

    private List<Map<String, Object>> orderProfitRows(LocalDate start, LocalDate end) {
        return transform(jdbcTemplate.queryForList("""
                SELECT o.ID_order,
                       o.o_order_code,
                       o.o_order_date,
                       o.o_customer_name,
                       COALESCE(STRING_AGG(DISTINCT os.os_name, ', '), '-') AS order_statuses,
                       COALESCE(o.o_net, 0) AS sales,
                       COALESCE(c.press_cost, 0) AS press_cost,
                       COALESCE(c.shipping_cost, 0) AS shipping_cost,
                       COALESCE(o.o_net, 0) - COALESCE(c.press_cost, 0) - COALESCE(c.shipping_cost, 0) AS profit
                FROM t_order o
                LEFT JOIN t_order_detail od ON od.ID_order = o.ID_order
                LEFT JOIN t_order_status os ON os.ID_order_status = od.ID_order_status
                LEFT JOIN (
                    SELECT ID_order,
                           SUM(CASE WHEN ID_type_cost = 1 THEN allocated_cost ELSE 0 END) AS press_cost,
                           SUM(CASE WHEN ID_type_cost = 2 THEN allocated_cost ELSE 0 END) AS shipping_cost
                    FROM (
                        SELECT qod.ID_order,
                               qc.ID_type_cost,
                               CASE
                                   WHEN SUM(CAST(NULLIF(REPLACE(qod.od_price_total, ',', ''), '') AS NUMERIC(14,2))) OVER (PARTITION BY qc.ID_cost) > 0 THEN
                                       CAST(NULLIF(REPLACE(qc.c_price, ',', ''), '') AS NUMERIC(14,2))
                                       * CAST(NULLIF(REPLACE(qod.od_price_total, ',', ''), '') AS NUMERIC(14,2))
                                       / SUM(CAST(NULLIF(REPLACE(qod.od_price_total, ',', ''), '') AS NUMERIC(14,2))) OVER (PARTITION BY qc.ID_cost)
                                   ELSE
                                       CAST(NULLIF(REPLACE(qc.c_price, ',', ''), '') AS NUMERIC(14,2))
                                       / COUNT(*) OVER (PARTITION BY qc.ID_cost)
                               END AS allocated_cost
                        FROM q_cost_detail cd
                        JOIN q_cost qc ON qc.ID_cost = cd.ID_cost
                        JOIN q_order_detail qod ON qod.ID_order_detail = cd.ID_order_detail
                        WHERE qc.c_delete = 'A'
                          AND qc.ID_type_cost IN (1, 2)
                    ) allocated
                    GROUP BY ID_order
                ) c ON c.ID_order = o.ID_order
                WHERE o.o_order_date BETWEEN ? AND ?
                GROUP BY o.ID_order, o.o_order_code, o.o_order_date, o.o_customer_name, o.o_net, o.o_send_cost,
                         c.press_cost, c.shipping_cost
                ORDER BY o.o_order_code DESC, o.ID_order DESC
                """, start, end), row -> map(
                "code", text(row.get("o_order_code")),
                "date", displayDate(row.get("o_order_date")),
                "customer", text(row.get("o_customer_name")),
                "statuses", text(row.get("order_statuses")),
                "sales", displayMoney(row.get("sales")),
                "pressCost", displayMoney(row.get("press_cost")),
                "shippingCost", displayMoney(row.get("shipping_cost")),
                "profit", displayMoney(row.get("profit"))));
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
                  AND CAST(l.l_create_date AS DATE) BETWEEN ? AND ?
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
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                """, type, start, end);
    }

    private BigDecimal costByType(int type, LocalDate start, LocalDate end) {
        return money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_cost
                WHERE c_delete = 'A'
                  AND ID_type_cost = ?
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
                """, type, start, end);
    }

    private BigDecimal costAll(LocalDate start, LocalDate end) {
        return money("""
                SELECT COALESCE(SUM(CAST(REPLACE(c_price, ',', '') AS DECIMAL(14,2))), 0)
                FROM q_cost
                WHERE c_delete = 'A'
                  AND CAST(c_create_date AS DATE) BETWEEN ? AND ?
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

