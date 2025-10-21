package com.example.builtsmart.controller;

import com.example.builtsmart.entity.User;
import com.example.builtsmart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RegistrationController {
    
    private final UserService userService;
    
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }
    
    @PostMapping("/register")
    public String registerUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String confirmPassword,
            @RequestParam String phone,
            @RequestParam String address,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        // Validation: Check if email already exists
        if (userService.existsByEmail(email)) {
            model.addAttribute("error", "An account with this email already exists.");
            model.addAttribute("name", name);
            model.addAttribute("phone", phone);
            model.addAttribute("address", address);
            return "auth/register";
        }
        
        // Validation: Check password length
        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters long.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("address", address);
            return "auth/register";
        }
        
        // Validation: Check if passwords match (if confirmPassword is provided)
        if (confirmPassword != null && !password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("address", address);
            return "auth/register";
        }
        
        try {
            // Create new user
            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setPassword(password); // Will be encoded by UserService
            newUser.setPhone(phone);
            newUser.setAddress(address);
            newUser.setRole(User.UserRole.WORKER); // Default role
            newUser.setActive(false); // Inactive until admin approves
            
            // Save user to database
            userService.createUser(newUser);
            
            // Success: Redirect to login with success message
            redirectAttributes.addFlashAttribute("registrationSuccess", true);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Account created successfully! Please wait for an administrator to activate your account before logging in.");
            
            return "redirect:/login?registered=true";
            
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed. Please try again. " + e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("address", address);
            return "auth/register";
        }
    }
}
