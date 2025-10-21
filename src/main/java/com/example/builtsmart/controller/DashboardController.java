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
        return "landing";
    }
    
    @GetMapping("/home")
    public String landing() {
        return "landing";
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
                return "redirect:/engineer/dashboard";
            case FINANCE_OFFICER:
                return "redirect:/finance/dashboard";
            case HR_EXECUTIVE:
                return "redirect:/hr/dashboard";
            case CLIENT_REPRESENTATIVE:
                return "redirect:/client/dashboard";
            case WORKER:
                return "redirect:/worker/dashboard";
            default:
                return "dashboard/default";
        }
    }
    
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
} 