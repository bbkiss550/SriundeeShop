package com.sriundee.preorder.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.sriundee.preorder.entity.Menu;
import com.sriundee.preorder.entity.Setting;
import com.sriundee.preorder.repository.MenuRepository;
import com.sriundee.preorder.repository.SettingRepository;

import jakarta.transaction.Transactional;

@Controller
@ControllerAdvice
public class SettingController {

    private static final String THEME_KEY = "theme_mode";
    private static final String DEFAULT_THEME = "light";
    private static final String SCHEDULE_SHOW_COMPLETED_KEY = "schedule_show_completed";
    private static final String DEFAULT_SCHEDULE_SHOW_COMPLETED = "false";
    private static final String ORDER_SHOW_CLOSED_PRODUCTS_KEY = "order_show_closed_products";
    private static final String DEFAULT_ORDER_SHOW_CLOSED_PRODUCTS = "false";
    private static final String DASHBOARD_CHART_SERIES_KEY = "dashboard_chart_series";
    private static final String DEFAULT_DASHBOARD_CHART_SERIES = "amount,receivedPaid,pledgePaid,pressCost,shippingCost";
    private static final String DASHBOARD_CHART_GRANULARITY_KEY = "dashboard_chart_granularity";
    private static final String DEFAULT_DASHBOARD_CHART_GRANULARITY = "month";
    private static final Set<String> DASHBOARD_CHART_SERIES = Set.of("amount", "receivedPaid", "pledgePaid", "pressCost", "shippingCost");

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private MenuRepository menuRepository;

    @ModelAttribute("appTheme")
    public String appTheme() {
        return getThemeMode();
    }

    @ModelAttribute("appUsername")
    public String appUsername() {
        String username = currentUsername();
        return username == null ? "" : username;
    }

    @ModelAttribute("appLoginDate")
    public String appLoginDate() {
        String loginDate = currentLoginDate();
        return loginDate == null ? "" : loginDate;
    }

    @GetMapping("/settings/theme")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getTheme() {
        return ResponseEntity.ok(Map.of("theme", getThemeMode()));
    }

    @PostMapping("/settings/theme")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveTheme(@RequestBody Map<String, String> payload) {
        String theme = normalizeTheme(payload.get("theme"));
        saveSetting(THEME_KEY, theme);
        return ResponseEntity.ok(Map.of("theme", theme));
    }

    @GetMapping("/settings/schedule/show-completed")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> getScheduleShowCompleted() {
        return ResponseEntity.ok(Map.of("showCompleted", getScheduleShowCompletedValue()));
    }

    @PostMapping("/settings/schedule/show-completed")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> saveScheduleShowCompleted(@RequestBody Map<String, Boolean> payload) {
        boolean showCompleted = Boolean.TRUE.equals(payload.get("showCompleted"));
        saveSetting(SCHEDULE_SHOW_COMPLETED_KEY, Boolean.toString(showCompleted));
        return ResponseEntity.ok(Map.of("showCompleted", showCompleted));
    }

    @GetMapping("/settings/order/show-closed-products")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> getOrderShowClosedProducts() {
        return ResponseEntity.ok(Map.of("showClosedProducts", getOrderShowClosedProductsValue()));
    }

    @PostMapping("/settings/order/show-closed-products")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> saveOrderShowClosedProducts(@RequestBody Map<String, Boolean> payload) {
        boolean showClosedProducts = Boolean.TRUE.equals(payload.get("showClosedProducts"));
        saveSetting(ORDER_SHOW_CLOSED_PRODUCTS_KEY, Boolean.toString(showClosedProducts));
        return ResponseEntity.ok(Map.of("showClosedProducts", showClosedProducts));
    }

    @GetMapping("/settings/dashboard/chart")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getDashboardChartSettings() {
        return ResponseEntity.ok(Map.of(
                "series", getDashboardChartSeriesValue(),
                "granularity", getDashboardChartGranularityValue()));
    }

    @PostMapping("/settings/dashboard/chart")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveDashboardChartSettings(@RequestBody Map<String, String> payload) {
        String series = normalizeDashboardChartSeries(payload.get("series"));
        String granularity = normalizeDashboardChartGranularity(payload.get("granularity"));
        saveGlobalSetting(DASHBOARD_CHART_SERIES_KEY, series);
        saveGlobalSetting(DASHBOARD_CHART_GRANULARITY_KEY, granularity);
        return ResponseEntity.ok(Map.of("series", series, "granularity", granularity));
    }

    @GetMapping("/settings/menu-order")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMenuOrderSettings() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Menu menu : menuRepository.getMenuOrderSettings()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", menu.getId());
            row.put("name", menu.getName());
            row.put("order", menu.getOrder() == null ? menu.getId() : menu.getOrder());
            rows.add(row);
        }
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/settings/menu-order")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> saveMenuOrderSettings(@RequestBody List<MenuOrderValue> payload) {
        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Menu order is required"));
        }

        Map<Integer, Integer> requestedOrders = new HashMap<>();
        for (MenuOrderValue item : payload) {
            if (item == null || item.id() == null || item.order() == null || item.order() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid menu order"));
            }
            requestedOrders.put(item.id(), item.order());
        }

        List<Menu> menus = menuRepository.findAllById(requestedOrders.keySet());
        if (menus.size() != requestedOrders.size()
                || menus.stream().anyMatch(menu -> menu.getIdmenu() != null)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid menu"));
        }

        menus.sort(Comparator.comparing(Menu::getId));
        for (Menu menu : menus) {
            menu.setOrder(requestedOrders.get(menu.getId()));
        }
        menuRepository.saveAll(menus);
        return ResponseEntity.ok(Map.of("message", "Success"));
    }

    private String getThemeMode() {
        return normalizeTheme(getSettingValue(THEME_KEY, DEFAULT_THEME));
    }

    private String normalizeTheme(String theme) {
        return "dark".equals(theme) ? "dark" : DEFAULT_THEME;
    }

    public boolean getScheduleShowCompletedValue() {
        return Boolean.parseBoolean(getSettingValue(SCHEDULE_SHOW_COMPLETED_KEY, DEFAULT_SCHEDULE_SHOW_COMPLETED));
    }

    public boolean getOrderShowClosedProductsValue() {
        return Boolean.parseBoolean(getSettingValue(ORDER_SHOW_CLOSED_PRODUCTS_KEY, DEFAULT_ORDER_SHOW_CLOSED_PRODUCTS));
    }

    public String getDashboardChartSeriesValue() {
        return normalizeDashboardChartSeries(getGlobalSettingValue(DASHBOARD_CHART_SERIES_KEY, DEFAULT_DASHBOARD_CHART_SERIES));
    }

    public String getDashboardChartGranularityValue() {
        return normalizeDashboardChartGranularity(getGlobalSettingValue(DASHBOARD_CHART_GRANULARITY_KEY, DEFAULT_DASHBOARD_CHART_GRANULARITY));
    }

    private String getSettingValue(String key, String defaultValue) {
        Setting setting = findCurrentUserSetting(key);
        if (setting == null) {
            setting = settingRepository.findFirstByKeyAndUserIdIsNull(key);
        }
        return setting == null || setting.getValue() == null ? defaultValue : setting.getValue();
    }

    private String getGlobalSettingValue(String key, String defaultValue) {
        Setting setting = settingRepository.findFirstByKeyAndUserIdIsNull(key);
        return setting == null || setting.getValue() == null ? defaultValue : setting.getValue();
    }

    private void saveSetting(String key, String value) {
        Integer userId = currentUserId();
        Setting setting = userId == null
                ? settingRepository.findFirstByKeyAndUserIdIsNull(key)
                : settingRepository.findFirstByKeyAndUserId(key, userId);
        if (setting == null) {
            setting = new Setting();
            setting.setKey(key);
            setting.setUserId(userId);
        }
        setting.setValue(value);
        settingRepository.save(setting);
    }

    private void saveGlobalSetting(String key, String value) {
        Setting setting = settingRepository.findFirstByKeyAndUserIdIsNull(key);
        if (setting == null) {
            setting = new Setting();
            setting.setKey(key);
            setting.setUserId(null);
        }
        setting.setValue(value);
        settingRepository.save(setting);
    }

    private Setting findCurrentUserSetting(String key) {
        Integer userId = currentUserId();
        if (userId == null) {
            return null;
        }
        return settingRepository.findFirstByKeyAndUserId(key, userId);
    }

    public Integer currentUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        Object userId = servletAttributes.getRequest().getSession(false) == null
                ? null
                : servletAttributes.getRequest().getSession(false).getAttribute(LoginController.SESSION_USER_ID);
        if (userId instanceof Integer id) {
            return id;
        }
        if (userId != null) {
            try {
                return Integer.parseInt(userId.toString());
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    private String currentUsername() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)
                || servletAttributes.getRequest().getSession(false) == null) {
            return null;
        }
        Object username = servletAttributes.getRequest().getSession(false).getAttribute(LoginController.SESSION_USERNAME);
        return username == null ? null : username.toString();
    }

    private String currentLoginDate() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)
                || servletAttributes.getRequest().getSession(false) == null) {
            return null;
        }
        Object loginDate = servletAttributes.getRequest().getSession(false).getAttribute(LoginController.SESSION_LOGIN_DATE);
        return loginDate == null ? null : loginDate.toString();
    }

    private String normalizeDashboardChartSeries(String series) {
        String selected = Stream.of((series == null ? "" : series).split(","))
                .map(String::trim)
                .filter(DASHBOARD_CHART_SERIES::contains)
                .distinct()
                .collect(Collectors.joining(","));
        return selected;
    }

    private String normalizeDashboardChartGranularity(String granularity) {
        return Set.of("month", "week", "day").contains(granularity) ? granularity : DEFAULT_DASHBOARD_CHART_GRANULARITY;
    }

    private record MenuOrderValue(Integer id, Integer order) {
    }
}
