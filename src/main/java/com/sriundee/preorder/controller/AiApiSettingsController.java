package com.sriundee.preorder.controller;

import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sriundee.preorder.service.AiApiSettingsService;
import com.sriundee.preorder.service.AiApiSettingsService.*;

@Controller
@RequestMapping("/settings/ai")
public class AiApiSettingsController {
    private final AiApiSettingsService service;private final MenuController menus;
    public AiApiSettingsController(AiApiSettingsService service,MenuController menus){this.service=service;this.menus=menus;}
    @GetMapping public String page(Model model){model.addAttribute("mainMenus",menus.getMenuList(null,null));return "settings/ai";}
    @GetMapping("/config") @ResponseBody public View get(HttpSession session){return service.get(user(session));}
    @PostMapping("/config") @ResponseBody public View save(@RequestBody Draft draft,HttpSession session){return service.save(user(session),draft);}
    @PostMapping("/test") @ResponseBody public Connection test(@RequestBody Draft draft,HttpSession session){return service.test(user(session),draft);}
    private int user(HttpSession session){Object id=session.getAttribute(LoginController.SESSION_USER_ID);if(id instanceof Number number)return number.intValue();throw new IllegalArgumentException("กรุณาเข้าสู่ระบบใหม่");}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseBody public ResponseEntity<?> invalid(IllegalArgumentException error){return ResponseEntity.badRequest().body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(IllegalStateException.class) @ResponseBody public ResponseEntity<?> failed(IllegalStateException error){return ResponseEntity.internalServerError().body(Map.of("message",error.getMessage()));}
}
