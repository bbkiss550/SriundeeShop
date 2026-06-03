package com.sriundee.preorder.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sriundee.preorder.bean.CostPressBean;
import com.sriundee.preorder.entity.Cost;
import com.sriundee.preorder.repository.CostRepository;
import com.sriundee.preorder.service.CostCodeService;

import jakarta.transaction.Transactional;

@Controller
public class CostExpenseController {

    private static final int MENU_ID = 16;
    private static final String DEFAULT_START_DATE = "2026-01-01";
    private static final String DEFAULT_END_DATE = "2026-12-31";
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,##0.00");
    private static final Set<Integer> ALLOWED_COST_TYPES = Set.of(3, 4, 5, 99);
    private static final Map<Integer, String> COST_TYPE_LABELS = Map.of(
            3, "ค่าสั่งกล่อง",
            4, "ค่าสั่งทำของแถม",
            5, "ค่าส่งไปรษณีย์",
            99, "อื่น ๆ");

    @Autowired
    private MenuController menuService;

    @Autowired
    private CostRepository costRepository;

    @Autowired
    private CostCodeService costCodeService;

    @GetMapping("/cost/expense")
    public String index(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "typeCost", required = false) Integer typeCost,
            Model model) {
        DateRange dateRange = defaultDateRange(startDate, endDate);
        Integer selectedType = normalizeTypeFilter(typeCost);

        model.addAttribute("mainMenus", menuService.getMenuList(MENU_ID, null));
        model.addAttribute("costTypes", costTypes());
        model.addAttribute("startDate", dateRange.startDate());
        model.addAttribute("endDate", dateRange.endDate());
        model.addAttribute("typeCost", selectedType);
        model.addAttribute("expenseRows", buildExpenseRows(dateRange.startDate(), dateRange.endDate(), selectedType));
        return "cost/expense";
    }

    @PostMapping("/cost/expense")
    @Transactional
    public synchronized String save(
            @RequestParam("recordDate") String recordDate,
            @RequestParam("typeCost") Integer typeCost,
            @RequestParam("price") String price,
            @RequestParam(value = "note", required = false) String note,
            RedirectAttributes redirectAttributes) {
        LocalDate selectedDate = parseDate(recordDate);
        BigDecimal selectedPrice = parseMoney(price);

        if (selectedDate == null) {
            redirectAttributes.addFlashAttribute("expenseError", "กรุณาเลือกวันที่บันทึก");
            return "redirect:/cost/expense";
        }
        if (!ALLOWED_COST_TYPES.contains(typeCost)) {
            redirectAttributes.addFlashAttribute("expenseError", "กรุณาเลือกประเภทค่าใช้จ่ายที่ถูกต้อง");
            return "redirect:/cost/expense";
        }
        if (selectedPrice == null || selectedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("expenseError", "กรุณาระบุจำนวนเงินมากกว่า 0");
            return "redirect:/cost/expense";
        }

        Cost cost = new Cost();
        cost.setCreate_date(selectedDate.toString());
        cost.setCost_code(costCodeService.nextCode(selectedDate));
        cost.setType_cost(typeCost);
        cost.setPrice(selectedPrice.stripTrailingZeros().toPlainString());
        cost.setNote(trimToEmpty(note));
        cost.setDelete("A");
        costRepository.save(cost);

        redirectAttributes.addFlashAttribute("expenseSuccess", "บันทึกค่าใช้จ่ายเรียบร้อย");
        return "redirect:/cost/expense";
    }

    @PostMapping("/cost/expense/{id}")
    public String update(
            @PathVariable("id") Integer id,
            @RequestParam("recordDate") String recordDate,
            @RequestParam("typeCost") Integer typeCost,
            @RequestParam("price") String price,
            @RequestParam(value = "note", required = false) String note,
            RedirectAttributes redirectAttributes) {
        LocalDate selectedDate = parseDate(recordDate);
        BigDecimal selectedPrice = parseMoney(price);
        Cost cost = costRepository.findById(id).orElse(null);

        if (cost == null || !ALLOWED_COST_TYPES.contains(cost.getType_cost())) {
            redirectAttributes.addFlashAttribute("expenseError", "ไม่พบข้อมูลค่าใช้จ่ายที่แก้ไขได้");
            return "redirect:/cost/expense";
        }
        if (!"A".equals(cost.getDelete())) {
            redirectAttributes.addFlashAttribute("expenseError", "รายการที่ยกเลิกแล้วไม่สามารถแก้ไขได้");
            return "redirect:/cost/expense";
        }
        if (selectedDate == null) {
            redirectAttributes.addFlashAttribute("expenseError", "กรุณาเลือกวันที่บันทึก");
            return "redirect:/cost/expense";
        }
        if (!ALLOWED_COST_TYPES.contains(typeCost)) {
            redirectAttributes.addFlashAttribute("expenseError", "กรุณาเลือกประเภทค่าใช้จ่ายที่ถูกต้อง");
            return "redirect:/cost/expense";
        }
        if (selectedPrice == null || selectedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("expenseError", "กรุณาระบุจำนวนเงินมากกว่า 0");
            return "redirect:/cost/expense";
        }

        cost.setCreate_date(selectedDate.toString());
        cost.setType_cost(typeCost);
        cost.setPrice(selectedPrice.stripTrailingZeros().toPlainString());
        cost.setNote(trimToEmpty(note));
        costRepository.save(cost);

        redirectAttributes.addFlashAttribute("expenseSuccess", "แก้ไขค่าใช้จ่ายเรียบร้อย");
        return "redirect:/cost/expense";
    }

    private List<CostTypeOption> costTypes() {
        return List.of(
                new CostTypeOption(3, COST_TYPE_LABELS.get(3)),
                new CostTypeOption(4, COST_TYPE_LABELS.get(4)),
                new CostTypeOption(5, COST_TYPE_LABELS.get(5)),
                new CostTypeOption(99, COST_TYPE_LABELS.get(99)));
    }

    private String buildExpenseRows(String startDate, String endDate, Integer typeCost) {
        List<CostPressBean> costs = costRepository.getRemainingCostAll(startDate, endDate, typeCost);
        StringBuilder rows = new StringBuilder();
        for (CostPressBean cost : costs) {
            rows.append("<tr>");
            rows.append("<td class='expense-action-col'>").append(buildEditButton(cost)).append("</td>");
            rows.append("<td class='expense-date-col'>").append(formatDate(cost.getc_create_date())).append("</td>");
            rows.append("<td class='expense-type-col'>").append(escapeHtml(labelFor(cost))).append("</td>");
            rows.append("<td class='expense-money-col text-end'>").append(formatMoney(cost.getc_price())).append("</td>");
            rows.append("<td>").append(escapeHtml(trimToEmpty(cost.getc_note()))).append("</td>");
            rows.append("<td class='expense-status-col'>").append(buildStatusBadge(cost.getc_delete())).append("</td>");
            rows.append("</tr>");
        }
        return rows.toString();
    }

    private String buildEditButton(CostPressBean cost) {
        if (!"A".equals(cost.getc_delete())) {
            return "<button type='button' class='btn icon btn-secondary' disabled><i data-feather='edit-2'></i></button>";
        }
        return "<button type='button' class='btn icon btn-warning' onclick=\"open_expense_modal(this)\""
                + " data-expense-id='" + cost.getID_cost() + "'"
                + " data-expense-date='" + escapeHtml(trimToEmpty(cost.getc_create_date())) + "'"
                + " data-expense-type='" + cost.getID_type_cost() + "'"
                + " data-expense-price='" + escapeHtml(formatMoney(cost.getc_price())) + "'"
                + " data-expense-note='" + escapeHtml(trimToEmpty(cost.getc_note())) + "'>"
                + "<i data-feather='edit-2'></i></button>";
    }

    private Integer normalizeTypeFilter(Integer typeCost) {
        return typeCost != null && ALLOWED_COST_TYPES.contains(typeCost) ? typeCost : null;
    }

    private String labelFor(CostPressBean cost) {
        if (cost.gettc_name() != null && !cost.gettc_name().isBlank()) {
            return cost.gettc_name();
        }
        return COST_TYPE_LABELS.getOrDefault(cost.getID_type_cost(), "ไม่ระบุ");
    }

    private String buildStatusBadge(String status) {
        if ("A".equals(status)) {
            return "<span class='badge bg-success expense-status-badge'>สำเร็จ</span>";
        }
        if ("D".equals(status)) {
            return "<span class='badge bg-danger expense-status-badge'>ยกเลิก</span>";
        }
        return "<span class='badge bg-secondary expense-status-badge'>ไม่ระบุ</span>";
    }

    private DateRange defaultDateRange(String startDate, String endDate) {
        return new DateRange(
                startDate == null || startDate.isBlank() ? DEFAULT_START_DATE : startDate,
                endDate == null || endDate.isBlank() ? DEFAULT_END_DATE : endDate);
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private BigDecimal parseMoney(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value.replace(",", "").trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String formatDate(String date) {
        try {
            return date == null || date.isBlank() ? "" : LocalDate.parse(date).format(DISPLAY_DATE_FORMAT);
        } catch (RuntimeException e) {
            return trimToEmpty(date);
        }
    }

    private String formatMoney(String value) {
        BigDecimal money = parseMoney(value);
        return money == null ? "0.00" : MONEY_FORMAT.format(money);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record CostTypeOption(Integer id, String label) {
    }

    private record DateRange(String startDate, String endDate) {
    }
}
