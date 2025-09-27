package com.example.builtsmart.controller;

import com.example.builtsmart.entity.User;
import com.example.builtsmart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    
    private final UserService userService;
    
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", user);
        
        switch (user.getRole()) {
            case PROJECT_MANAGER:
                return "redirect:/manager/dashboard";
            case SITE_ENGINEER:
                return "dashboard/engineer";
            case HR_EXECUTIVE:
                return "dashboard/hr";
            case FINANCE_OFFICER:
                return "dashboard/finance";
            case CLIENT_REPRESENTATIVE:
                return "dashboard/client";
            case WORKER:
                return "dashboard/worker";
            default:
                return "dashboard/default";
        }
    }
    
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
    
    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }
} 