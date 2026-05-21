package com.sriundee.preorder.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.entity.LogVersion;
import com.sriundee.preorder.entity.Setting;
import com.sriundee.preorder.repository.LogVersionRepository;
import com.sriundee.preorder.repository.SettingRepository;

@Controller
@ControllerAdvice
public class SettingController {

    private static final String THEME_KEY = "theme_mode";
    private static final String DEFAULT_THEME = "light";
    private static final String SCHEDULE_SHOW_COMPLETED_KEY = "schedule_show_completed";
    private static final String DEFAULT_SCHEDULE_SHOW_COMPLETED = "false";
    private static final String DASHBOARD_CHART_SERIES_KEY = "dashboard_chart_series";
    private static final String DEFAULT_DASHBOARD_CHART_SERIES = "amount,receivedPaid,pledgePaid,pressCost,shippingCost";
    private static final String DASHBOARD_CHART_GRANULARITY_KEY = "dashboard_chart_granularity";
    private static final String DEFAULT_DASHBOARD_CHART_GRANULARITY = "month";
    private static final Set<String> DASHBOARD_CHART_SERIES = Set.of("amount", "receivedPaid", "pledgePaid", "pressCost", "shippingCost");

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private LogVersionRepository logVersionRepository;

    @ModelAttribute("appTheme")
    public String appTheme() {
        return getThemeMode();
    }

    @ModelAttribute("appVersion")
    public LogVersion appVersion() {
        try {
            LogVersion version = logVersionRepository.findFirstByOrderByIdDesc();
            return version == null ? defaultVersion() : version;
        } catch (DataAccessException exception) {
            return defaultVersion();
        }
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
        saveSetting(DASHBOARD_CHART_SERIES_KEY, series);
        saveSetting(DASHBOARD_CHART_GRANULARITY_KEY, granularity);
        return ResponseEntity.ok(Map.of("series", series, "granularity", granularity));
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

    public String getDashboardChartSeriesValue() {
        return normalizeDashboardChartSeries(getSettingValue(DASHBOARD_CHART_SERIES_KEY, DEFAULT_DASHBOARD_CHART_SERIES));
    }

    public String getDashboardChartGranularityValue() {
        return normalizeDashboardChartGranularity(getSettingValue(DASHBOARD_CHART_GRANULARITY_KEY, DEFAULT_DASHBOARD_CHART_GRANULARITY));
    }

    private String getSettingValue(String key, String defaultValue) {
        Setting setting = settingRepository.findByKey(key);
        return setting == null || setting.getValue() == null ? defaultValue : setting.getValue();
    }

    private void saveSetting(String key, String value) {
        Setting setting = settingRepository.findByKey(key);
        if (setting == null) {
            setting = new Setting();
            setting.setKey(key);
        }
        setting.setValue(value);
        settingRepository.save(setting);
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

    private LogVersion defaultVersion() {
        LogVersion version = new LogVersion();
        version.setVersion("1.0.0");
        version.setDate("2026-05-21");
        return version;
    }
}
