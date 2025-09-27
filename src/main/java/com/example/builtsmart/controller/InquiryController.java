package com.example.builtsmart.controller;

import com.example.builtsmart.entity.Inquiry;
import com.example.builtsmart.repository.InquiryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class InquiryController {

    private final InquiryRepository inquiryRepository;

    public InquiryController(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    // Public: Show inquiry form
    @GetMapping("/inquiries")
    public String showInquiryForm(Model model) {
        model.addAttribute("inquiry", new Inquiry());
        return "public/inquiry-form";
    }

    // Public: Submit inquiry
    @PostMapping("/inquiries")
    public String submitInquiry(@ModelAttribute Inquiry inquiry, Model model) {
        inquiry.setStatus(Inquiry.Status.OPEN);
        inquiry.setCreatedAt(LocalDateTime.now());
        inquiryRepository.save(inquiry);
        model.addAttribute("success", true);
        model.addAttribute("inquiry", new Inquiry());
        return "public/inquiry-form";
    }

    // Manager: List inquiries
    @GetMapping("/manager/inquiries")
    public String listInquiries(Model model, @SessionAttribute(name = "user", required = false) Object user) {
        model.addAttribute("inquiries", inquiryRepository.findAll());
        model.addAttribute("user", user);
        return "manager/inquiries";
    }

    // Manager: Inquiry detail
    @GetMapping("/manager/inquiries/{id}")
    public String viewInquiry(@PathVariable Long id, Model model, @SessionAttribute(name = "user", required = false) Object user) {
        Optional<Inquiry> inquiry = inquiryRepository.findById(id);
        if (inquiry.isEmpty()) {
            return "redirect:/manager/inquiries";
        }
        model.addAttribute("inquiry", inquiry.get());
        model.addAttribute("user", user);
        return "manager/inquiry-detail";
    }

    // Manager: Reply to inquiry
    @PostMapping("/manager/inquiries/{id}/reply")
    public String replyInquiry(@PathVariable Long id,
                               @RequestParam("responseText") String responseText,
                               @SessionAttribute(name = "user", required = false) Object user) {
        Optional<Inquiry> opt = inquiryRepository.findById(id);
        if (opt.isPresent()) {
            Inquiry inq = opt.get();
            inq.setResponseText(responseText);
            inq.setRespondedAt(LocalDateTime.now());
            inq.setStatus(Inquiry.Status.RESPONDED);
            // Attempt to capture a display name if available
            if (user != null) {
                inq.setRespondedBy(user.toString());
            }
            inquiryRepository.save(inq);
        }
        return "redirect:/manager/inquiries/" + id;
    }

    // CLIENT: My inquiries dashboard
    @GetMapping("/client/inquiries")
    public String clientInquiries(Model model, @SessionAttribute(name = "user", required = false) Object user) {
        String email = null;
        String name = null;
        if (user != null) {
            try {
                // Reflectively try common getters to avoid tight coupling
                var clazz = user.getClass();
                var getEmail = clazz.getMethod("getEmail");
                var getName = clazz.getMethod("getName");
                email = (String) getEmail.invoke(user);
                name = (String) getName.invoke(user);
            } catch (Exception ignored) {}
        }
        if (email != null) {
            model.addAttribute("myInquiries", inquiryRepository.findByClientEmailOrderByCreatedAtDesc(email));
        } else {
            model.addAttribute("myInquiries", inquiryRepository.findAll()); // fallback for testing
        }
        model.addAttribute("prefillName", name);
        model.addAttribute("prefillEmail", email);
        return "client/inquiries";
    }

    // CLIENT: Submit inquiry from client dashboard
    @PostMapping("/client/inquiries")
    public String clientSubmitInquiry(@RequestParam String subject,
                                      @RequestParam String message,
                                      @SessionAttribute(name = "user", required = false) Object user) {
        String email = null;
        String name = null;
        if (user != null) {
            try {
                var clazz = user.getClass();
                var getEmail = clazz.getMethod("getEmail");
                var getName = clazz.getMethod("getName");
                email = (String) getEmail.invoke(user);
                name = (String) getName.invoke(user);
            } catch (Exception ignored) {}
        }
        Inquiry inquiry = new Inquiry();
        inquiry.setClientEmail(email != null ? email : "unknown@example.com");
        inquiry.setClientName(name != null ? name : "Client");
        inquiry.setSubject(subject);
        inquiry.setMessage(message);
        inquiry.setStatus(Inquiry.Status.OPEN);
        inquiry.setCreatedAt(LocalDateTime.now());
        inquiryRepository.save(inquiry);
        return "redirect:/client/inquiries";
    }
}
