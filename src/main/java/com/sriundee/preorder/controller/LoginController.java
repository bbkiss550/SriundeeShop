package com.sriundee.preorder.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sriundee.preorder.entity.User;
import com.sriundee.preorder.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    public static final String SESSION_USER_ID = "loginUserId";
    public static final String SESSION_USERNAME = "loginUsername";
    public static final String SESSION_LOGIN_DATE = "loginDate";
    private static final DateTimeFormatter LOGIN_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute(SESSION_USER_ID) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam(required = false) String username, HttpSession session, Model model) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isBlank()) {
            model.addAttribute("error", "กรุณาระบุ username");
            model.addAttribute("username", "");
            return "login";
        }

        User user = userRepository.findFirstByUsernameIgnoreCaseAndDelete(normalizedUsername, "A");
        if (user == null) {
            user = new User();
            user.setUsername(normalizedUsername);
            user.setDelete("A");
            user = userRepository.save(user);
        }

        session.setMaxInactiveInterval(-1);
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_USERNAME, user.getUsername());
        session.setAttribute(SESSION_LOGIN_DATE, LocalDateTime.now().format(LOGIN_DATE_FORMATTER));
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
