package com.example.builtsmart.controller;

import com.example.builtsmart.entity.*;
import com.example.builtsmart.repository.*;
import com.example.builtsmart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {
    
    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentReminderRepository paymentReminderRepository;
    private final LogRepository logRepository;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get client's projects
        List<Project> clientProjects = projectRepository.findByClientEmail(currentUser.getEmail());
        
        // Project statistics
        long totalProjects = clientProjects.size();
        long inProgressProjects = clientProjects.stream()
                .filter(p -> p.getStatus() == Project.ProjectStatus.IN_PROGRESS)
                .count();
        long completedProjects = clientProjects.stream()
                .filter(p -> p.getStatus() == Project.ProjectStatus.COMPLETED)
                .count();
        
        // Budget statistics
        BigDecimal totalBudget = clientProjects.stream()
                .filter(p -> p.getTotalBudget() != null)
                .map(Project::getTotalBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (Project project : clientProjects) {
            BigDecimal projectExpenses = expenseRepository.getTotalApprovedExpensesByProject(project.getId());
            if (projectExpenses != null) {
                totalSpent = totalSpent.add(projectExpenses);
            }
        }
        
        // Payment reminders
        List<PaymentReminder> pendingPayments = paymentReminderRepository.findRemindersByClientEmail(currentUser.getEmail())
                .stream()
                .filter(r -> r.getStatus() == PaymentReminder.ReminderStatus.PENDING || 
                            r.getStatus() == PaymentReminder.ReminderStatus.AWAITING_CONFIRMATION)
                .collect(Collectors.toList());
        
        long overduePayments = pendingPayments.stream()
                .filter(r -> r.getDueDate().isBefore(LocalDate.now()) && 
                            r.getStatus() == PaymentReminder.ReminderStatus.PENDING)
                .count();
        
        // Recent activities (logs from client's projects)
        List<Log> recentActivities = clientProjects.stream()
                .flatMap(project -> logRepository.findByProjectOrderByCreatedAtDesc(project).stream())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList());
        
        // Project progress data
        List<Project> activeProjects = clientProjects.stream()
                .filter(p -> p.getStatus() == Project.ProjectStatus.IN_PROGRESS)
                .limit(5)
                .collect(Collectors.toList());
        
        // Calculate completion percentage for each project
        for (Project project : activeProjects) {
            List<Task> projectTasks = taskRepository.findByProjectOrderByCreatedAtDesc(project);
            if (!projectTasks.isEmpty()) {
                long completedTasks = projectTasks.stream()
                        .filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED)
                        .count();
                int completionPercentage = (int) ((completedTasks * 100.0) / projectTasks.size());
                project.setCompletionPercentage(completionPercentage);
            }
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("inProgressProjects", inProgressProjects);
        model.addAttribute("completedProjects", completedProjects);
        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("pendingPaymentsCount", pendingPayments.size());
        model.addAttribute("overduePayments", overduePayments);
        model.addAttribute("recentActivities", recentActivities);
        model.addAttribute("activeProjects", activeProjects);
        model.addAttribute("clientProjects", clientProjects);
        
        return "client/dashboard";
    }
    
    @GetMapping("/projects")
    public String projects(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Project> clientProjects = projectRepository.findByClientEmail(currentUser.getEmail());
        
        // Add task counts and completion percentages
        for (Project project : clientProjects) {
            List<Task> projectTasks = taskRepository.findByProjectOrderByCreatedAtDesc(project);
            project.setTaskCount(projectTasks.size());
            
            if (!projectTasks.isEmpty()) {
                long completedTasks = projectTasks.stream()
                        .filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED)
                        .count();
                int completionPercentage = (int) ((completedTasks * 100.0) / projectTasks.size());
                project.setCompletionPercentage(completionPercentage);
            }
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("projects", clientProjects);
        
        return "client/projects";
    }
    
    @GetMapping("/projects/{id}")
    public String projectDetails(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Verify client owns this project
        if (!project.getClient().getEmail().equals(currentUser.getEmail())) {
            return "redirect:/client/projects";
        }
        
        List<Task> tasks = taskRepository.findByProjectOrderByCreatedAtDesc(project);
        List<Log> logs = logRepository.findByProjectOrderByCreatedAtDesc(project);
        
        // Calculate project expenses
        BigDecimal totalExpenses = expenseRepository.getTotalApprovedExpensesByProject(project.getId());
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        
        // Task statistics
        long totalTasks = tasks.size();
        long completedTasks = tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED).count();
        long inProgressTasks = tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS).count();
        long pendingTasks = tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.PENDING).count();
        
        int completionPercentage = 0;
        if (totalTasks > 0) {
            completionPercentage = (int) ((completedTasks * 100.0) / totalTasks);
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("logs", logs);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("inProgressTasks", inProgressTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("completionPercentage", completionPercentage);
        
        return "client/project-details";
    }
    
    // ==================== REMINDERS (Pending Reminders from Finance Officer - Pay Now) ====================
    
    @GetMapping("/reminders")
    public String reminders(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get pending reminders from finance officer
        List<PaymentReminder> pendingReminders = paymentReminderRepository.findRemindersByClientEmail(currentUser.getEmail());
        
        model.addAttribute("user", currentUser);
        model.addAttribute("reminders", pendingReminders);
        
        return "client/reminders";
    }
    
    @GetMapping("/payment-portal/{id}")
    public String paymentPortal(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PaymentReminder reminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment reminder not found"));
        
        model.addAttribute("user", currentUser);
        model.addAttribute("reminder", reminder);
        
        return "client/payment-portal";
    }
    
    @PostMapping("/payment-portal/{id}/submit")
    public String submitPayment(@PathVariable Long id,
                               @RequestParam String paymentMethod,
                               @RequestParam String transactionId,
                               @RequestParam(required = false) String paymentNotes,
                               Authentication authentication) {
        PaymentReminder reminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment reminder not found"));
        
        reminder.setPaymentSubmitted(true);
        reminder.setPaymentSubmittedAt(java.time.LocalDateTime.now());
        reminder.setPaymentMethod(paymentMethod);
        reminder.setTransactionId(transactionId);
        reminder.setPaymentNotes(paymentNotes);
        // Keep status as PENDING, use paymentSubmitted flag to track submission
        // Status will change to PAID only after finance officer confirms
        
        paymentReminderRepository.save(reminder);
        return "redirect:/client/reminders?success=true";
    }
    
    // ==================== PAYMENTS (Payment History) ====================
    
    @GetMapping("/payments")
    public String payments(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get all payment history
        List<PaymentReminder> allReminders = paymentReminderRepository.findRemindersByClientEmail(currentUser.getEmail());
        
        model.addAttribute("user", currentUser);
        model.addAttribute("reminders", allReminders);
        
        return "client/payments";
    }
}
