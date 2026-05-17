package com.sriundee.preorder.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.entity.Setting;
import com.sriundee.preorder.repository.SettingRepository;

@Controller
@ControllerAdvice
public class SettingController {

    private static final String THEME_KEY = "theme_mode";
    private static final String DEFAULT_THEME = "light";

    @Autowired
    private SettingRepository settingRepository;

    @ModelAttribute("appTheme")
    public String appTheme() {
        return getThemeMode();
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
        Setting setting = settingRepository.findByKey(THEME_KEY);
        if (setting == null) {
            setting = new Setting();
            setting.setKey(THEME_KEY);
        }
        setting.setValue(theme);
        settingRepository.save(setting);
        return ResponseEntity.ok(Map.of("theme", theme));
    }

    private String getThemeMode() {
        Setting setting = settingRepository.findByKey(THEME_KEY);
        if (setting == null) {
            return DEFAULT_THEME;
        }
        return normalizeTheme(setting.getValue());
    }

    private String normalizeTheme(String theme) {
        return "dark".equals(theme) ? "dark" : DEFAULT_THEME;
    }
}
