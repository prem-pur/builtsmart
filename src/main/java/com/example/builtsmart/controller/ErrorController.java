package com.example.builtsmart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Prefer Jakarta attributes (Spring Boot 3), fall back to legacy javax
        Object jakartaStatus = request.getAttribute("jakarta.servlet.error.status_code");
        Object javaxStatus = request.getAttribute("javax.servlet.error.status_code");
        Integer statusCode = jakartaStatus instanceof Integer ? (Integer) jakartaStatus
                : (javaxStatus instanceof Integer ? (Integer) javaxStatus : 500);

        Object jakartaEx = request.getAttribute("jakarta.servlet.error.exception");
        Object javaxEx = request.getAttribute("javax.servlet.error.exception");
        Exception exception = jakartaEx instanceof Exception ? (Exception) jakartaEx
                : (javaxEx instanceof Exception ? (Exception) javaxEx : null);
        
        // Read error message attribute if exception is absent
        Object jakartaMessage = request.getAttribute("jakarta.servlet.error.message");
        Object javaxMessage = request.getAttribute("javax.servlet.error.message");
        String message = exception != null ? exception.getMessage()
                : (jakartaMessage instanceof String && !((String) jakartaMessage).isBlank() ? (String) jakartaMessage
                : (javaxMessage instanceof String && !((String) javaxMessage).isBlank() ? (String) javaxMessage : "Unknown error"));

        // Read request URI for context
        Object jakartaUri = request.getAttribute("jakarta.servlet.error.request_uri");
        Object javaxUri = request.getAttribute("javax.servlet.error.request_uri");
        String requestUri = jakartaUri instanceof String ? (String) jakartaUri
                : (javaxUri instanceof String ? (String) javaxUri : "");
        
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", message);
        model.addAttribute("requestUri", requestUri);
        
        return "error/error";
    }
}
