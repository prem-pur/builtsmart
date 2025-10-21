package com.example.builtsmart.controller;

import com.example.builtsmart.entity.*;
import com.example.builtsmart.repository.*;
import com.example.builtsmart.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceOfficerController {
    
    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentReminderRepository paymentReminderRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    
    // ==================== DASHBOARD ====================
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        // Expense statistics
        List<Expense> allExpenses = expenseRepository.findAll();
        List<Expense> pendingExpenses = expenseRepository.findByStatus(Expense.ExpenseStatus.PENDING);
        List<Expense> approvedExpenses = expenseRepository.findByStatus(Expense.ExpenseStatus.APPROVED);
        List<Expense> paidExpenses = expenseRepository.findByStatus(Expense.ExpenseStatus.PAID);
        
        BigDecimal totalExpenses = allExpenses.stream()
                .filter(e -> e.getStatus() == Expense.ExpenseStatus.APPROVED || e.getStatus() == Expense.ExpenseStatus.PAID)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal pendingAmount = pendingExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Payment reminders
        List<PaymentReminder> allReminders = paymentReminderRepository.findAll();
        List<PaymentReminder> overdueReminders = paymentReminderRepository.findOverdueReminders(LocalDate.now());
        List<PaymentReminder> upcomingReminders = paymentReminderRepository.findUpcomingReminders(
                LocalDate.now(), LocalDate.now().plusDays(7));
        
        BigDecimal totalPendingPayments = paymentReminderRepository.getTotalPendingAmount();
        if (totalPendingPayments == null) totalPendingPayments = BigDecimal.ZERO;
        
        // Budget overview
        List<Project> projects = projectRepository.findAll();
        BigDecimal totalBudget = projects.stream()
                .filter(p -> p.getTotalBudget() != null)
                .map(Project::getTotalBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Recent expenses
        List<Expense> recentExpenses = allExpenses.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
        
        // Recent payment reminders
        List<PaymentReminder> recentReminders = allReminders.stream()
                .filter(r -> r.getStatus() == PaymentReminder.ReminderStatus.PENDING)
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .limit(5)
                .collect(Collectors.toList());
        
        // Client-submitted reminders count
        long clientRemindersCount = paymentReminderRepository.countPendingClientReminders();
        
        // Pending payment confirmations
        long pendingConfirmationsCount = paymentReminderRepository.countPendingPaymentConfirmations();
        
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("pendingExpensesCount", pendingExpenses.size());
        model.addAttribute("pendingAmount", pendingAmount);
        model.addAttribute("totalPendingPayments", totalPendingPayments);
        model.addAttribute("overdueRemindersCount", overdueReminders.size());
        model.addAttribute("clientRemindersCount", clientRemindersCount);
        model.addAttribute("pendingConfirmationsCount", pendingConfirmationsCount);
        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("recentExpenses", recentExpenses);
        model.addAttribute("recentReminders", recentReminders);
        
        return "finance/dashboard";
    }
    
    // ==================== EXPENSE MANAGEMENT ====================
    
    @GetMapping("/expenses")
    public String listExpenses(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        List<Expense> expenses = expenseRepository.findAll();
        expenses.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        model.addAttribute("expenses", expenses);
        
        return "finance/expenses";
    }
    
    @GetMapping("/expenses/new")
    public String newExpenseForm(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        List<Project> projects = projectRepository.findAll();
        
        model.addAttribute("expense", new Expense());
        model.addAttribute("projects", projects);
        model.addAttribute("expenseCategories", Expense.ExpenseCategory.values());
        
        return "finance/expense-form";
    }
    
    @PostMapping("/expenses")
    public String createExpense(@ModelAttribute Expense expense, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate amount is not negative
        if (expense.getAmount() != null && expense.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "Amount cannot be negative");
            model.addAttribute("user", currentUser);
            model.addAttribute("expense", expense);
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("expenseCategories", Expense.ExpenseCategory.values());
            return "finance/expense-form";
        }
        
        expense.setSubmittedBy(currentUser);
        expense.setCreatedAt(LocalDateTime.now());
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }
        if (expense.getStatus() == null) {
            expense.setStatus(Expense.ExpenseStatus.PENDING);
        }
        
        expenseRepository.save(expense);
        return "redirect:/finance/expenses";
    }
    
    @GetMapping("/expenses/{id}")
    public String viewExpense(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        model.addAttribute("expense", expense);
        model.addAttribute("expenseStatuses", Expense.ExpenseStatus.values());
        
        return "finance/expense-detail";
    }
    
    @GetMapping("/expenses/{id}/edit")
    public String editExpenseForm(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        List<Project> projects = projectRepository.findAll();
        
        model.addAttribute("expense", expense);
        model.addAttribute("projects", projects);
        model.addAttribute("expenseCategories", Expense.ExpenseCategory.values());
        model.addAttribute("expenseStatuses", Expense.ExpenseStatus.values());
        
        return "finance/expense-form";
    }
    
    @PostMapping("/expenses/{id}")
    public String updateExpense(@PathVariable Long id, @ModelAttribute Expense expense, 
                               Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Expense existingExpense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        // Validate amount is not negative
        if (expense.getAmount() != null && expense.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "Amount cannot be negative");
            model.addAttribute("user", currentUser);
            model.addAttribute("expense", expense);
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("expenseCategories", Expense.ExpenseCategory.values());
            model.addAttribute("expenseStatuses", Expense.ExpenseStatus.values());
            return "finance/expense-form";
        }
        
        existingExpense.setProject(expense.getProject());
        existingExpense.setDescription(expense.getDescription());
        existingExpense.setAmount(expense.getAmount());
        existingExpense.setCategory(expense.getCategory());
        existingExpense.setExpenseDate(expense.getExpenseDate());
        existingExpense.setReceiptUrl(expense.getReceiptUrl());
        existingExpense.setNotes(expense.getNotes());
        existingExpense.setStatus(expense.getStatus());
        
        expenseRepository.save(existingExpense);
        return "redirect:/finance/expenses/" + id;
    }
    
    @PostMapping("/expenses/{id}/approve")
    public String approveExpense(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        expense.setStatus(Expense.ExpenseStatus.APPROVED);
        expense.setApprovedBy(currentUser);
        expense.setApprovedAt(LocalDateTime.now());
        
        expenseRepository.save(expense);
        return "redirect:/finance/expenses/" + id;
    }
    
    @PostMapping("/expenses/{id}/mark-paid")
    public String markExpensePaid(@PathVariable Long id, Authentication authentication) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        expense.setStatus(Expense.ExpenseStatus.PAID);
        expenseRepository.save(expense);
        
        return "redirect:/finance/expenses/" + id;
    }
    
    @PostMapping("/expenses/{id}/delete")
    public String deleteExpense(@PathVariable Long id, Authentication authentication) {
        expenseRepository.deleteById(id);
        return "redirect:/finance/expenses";
    }
    
    // ==================== BUDGET MANAGEMENT ====================
    
    @GetMapping("/budgets")
    public String listBudgets(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        List<Project> projects = projectRepository.findAll();
        
        // Calculate budget utilization for each project
        for (Project project : projects) {
            BigDecimal totalExpenses = expenseRepository.getTotalApprovedExpensesByProject(project.getId());
            if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
            project.setCurrentExpense(totalExpenses);
        }
        
        model.addAttribute("projects", projects);
        
        return "finance/budgets";
    }
    
    @GetMapping("/budgets/{projectId}")
    public String viewBudgetDetail(@PathVariable Long projectId, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        List<Expense> projectExpenses = expenseRepository.findByProjectOrderByCreatedAtDesc(projectId);
        
        BigDecimal totalExpenses = expenseRepository.getTotalApprovedExpensesByProject(projectId);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        
        BigDecimal remainingBudget = BigDecimal.ZERO;
        BigDecimal utilizationPercentage = BigDecimal.ZERO;
        
        if (project.getTotalBudget() != null) {
            remainingBudget = project.getTotalBudget().subtract(totalExpenses);
            if (project.getTotalBudget().compareTo(BigDecimal.ZERO) > 0) {
                utilizationPercentage = totalExpenses
                        .divide(project.getTotalBudget(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        
        // Group expenses by category
        Map<Expense.ExpenseCategory, BigDecimal> expensesByCategory = projectExpenses.stream()
                .filter(e -> e.getStatus() == Expense.ExpenseStatus.APPROVED || e.getStatus() == Expense.ExpenseStatus.PAID)
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
        
        model.addAttribute("project", project);
        model.addAttribute("projectExpenses", projectExpenses);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("remainingBudget", remainingBudget);
        model.addAttribute("utilizationPercentage", utilizationPercentage);
        model.addAttribute("expensesByCategory", expensesByCategory);
        
        return "finance/budget-detail";
    }
    
    // ==================== PAYMENT REMINDERS ====================
    
    @GetMapping("/reminders")
    public String listReminders(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        List<PaymentReminder> reminders = paymentReminderRepository.findAll();
        reminders.sort((a, b) -> a.getDueDate().compareTo(b.getDueDate()));
        
        // Update overdue status
        LocalDate today = LocalDate.now();
        for (PaymentReminder reminder : reminders) {
            if (reminder.getStatus() == PaymentReminder.ReminderStatus.PENDING && 
                reminder.getDueDate().isBefore(today)) {
                reminder.setStatus(PaymentReminder.ReminderStatus.OVERDUE);
                paymentReminderRepository.save(reminder);
            }
        }
        
        model.addAttribute("reminders", reminders);
        
        return "finance/reminders";
    }
    
    @GetMapping("/reminders/new")
    public String newReminderForm(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        List<Project> projects = projectRepository.findAll();
        List<Expense> approvedExpenses = expenseRepository.findByStatus(Expense.ExpenseStatus.APPROVED);
        
        model.addAttribute("reminder", new PaymentReminder());
        model.addAttribute("projects", projects);
        model.addAttribute("expenses", approvedExpenses);
        model.addAttribute("priorities", PaymentReminder.Priority.values());
        
        return "finance/reminder-form";
    }
    
    @PostMapping("/reminders")
    public String createReminder(@ModelAttribute PaymentReminder reminder, 
                                @RequestParam(required = false) Long expenseId,
                                Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate amount is not negative
        if (reminder.getAmount() != null && reminder.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "Amount cannot be negative");
            model.addAttribute("user", currentUser);
            model.addAttribute("reminder", reminder);
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("expenses", expenseRepository.findByStatus(Expense.ExpenseStatus.APPROVED));
            model.addAttribute("priorities", PaymentReminder.Priority.values());
            return "finance/reminder-form";
        }
        
        // Validate due date is not in the past
        if (reminder.getDueDate() != null && reminder.getDueDate().isBefore(LocalDate.now())) {
            model.addAttribute("error", "Due date cannot be in the past");
            model.addAttribute("user", currentUser);
            model.addAttribute("reminder", reminder);
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("expenses", expenseRepository.findByStatus(Expense.ExpenseStatus.APPROVED));
            model.addAttribute("priorities", PaymentReminder.Priority.values());
            return "finance/reminder-form";
        }
        
        // Handle expense relationship properly
        if (expenseId != null && expenseId > 0) {
            Expense expense = expenseRepository.findById(expenseId).orElse(null);
            reminder.setExpense(expense);
        } else {
            reminder.setExpense(null);
        }
        
        reminder.setCreatedBy(currentUser);
        reminder.setCreatedAt(LocalDateTime.now());
        if (reminder.getStatus() == null) {
            reminder.setStatus(PaymentReminder.ReminderStatus.PENDING);
        }
        
        paymentReminderRepository.save(reminder);
        return "redirect:/finance/reminders";
    }
    
    @GetMapping("/reminders/{id}")
    public String viewReminder(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        PaymentReminder reminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment reminder not found"));
        
        model.addAttribute("reminder", reminder);
        model.addAttribute("reminderStatuses", PaymentReminder.ReminderStatus.values());
        
        return "finance/reminder-detail";
    }
    
    @GetMapping("/reminders/{id}/edit")
    public String editReminderForm(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        PaymentReminder reminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment reminder not found"));
        
        List<Project> projects = projectRepository.findAll();
        List<Expense> approvedExpenses = expenseRepository.findByStatus(Expense.ExpenseStatus.APPROVED);
        
        model.addAttribute("reminder", reminder);
        model.addAttribute("projects", projects);
        model.addAttribute("expenses", approvedExpenses);
        model.addAttribute("priorities", PaymentReminder.Priority.values());
        model.addAttribute("reminderStatuses", PaymentReminder.ReminderStatus.values());
        
        return "finance/reminder-form";
    }
    
    @PostMapping("/reminders/{id}")
    public String updateReminder(@PathVariable Long id, @ModelAttribute PaymentReminder reminder,
                                @RequestParam(required = false) Long expenseId,
                                Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PaymentReminder existingReminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment reminder not found"));
        
        // Validate amount is not negative
        if (reminder.getAmount() != null && reminder.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "Amount cannot be negative");
            model.addAttribute("user", currentUser);
            model.addAttribute("reminder", reminder);
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("expenses", expenseRepository.findByStatus(Expense.ExpenseStatus.APPROVED));
            model.addAttribute("priorities", PaymentReminder.Priority.values());
            model.addAttribute("reminderStatuses", PaymentReminder.ReminderStatus.values());
            return "finance/reminder-form";
        }
        
        // Handle expense relationship properly
        if (expenseId != null && expenseId > 0) {
            Expense expense = expenseRepository.findById(expenseId).orElse(null);
            existingReminder.setExpense(expense);
        } else {
            existingReminder.setExpense(null);
        }
        
        existingReminder.setProject(reminder.getProject());
        existingReminder.setDescription(reminder.getDescription());
        existingReminder.setAmount(reminder.getAmount());
        existingReminder.setDueDate(reminder.getDueDate());
        existingReminder.setRecipient(reminder.getRecipient());
        existingReminder.setRecipientContact(reminder.getRecipientContact());
        existingReminder.setStatus(reminder.getStatus());
        existingReminder.setPriority(reminder.getPriority());
        existingReminder.setNotes(reminder.getNotes());
        
        paymentReminderRepository.save(existingReminder);
        return "redirect:/finance/reminders/" + id;
    }
    
    @PostMapping("/reminders/{id}/mark-paid")
    public String markReminderPaid(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PaymentReminder reminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment reminder not found"));
        
        reminder.setStatus(PaymentReminder.ReminderStatus.PAID);
        reminder.setPaidAt(LocalDateTime.now());
        reminder.setPaidBy(currentUser);
        
        paymentReminderRepository.save(reminder);
        return "redirect:/finance/reminders/" + id;
    }
    
    @PostMapping("/reminders/{id}/delete")
    public String deleteReminder(@PathVariable Long id, Authentication authentication) {
        paymentReminderRepository.deleteById(id);
        return "redirect:/finance/reminders";
    }
    
    // ==================== REPORTS ====================
    
    @GetMapping("/reports")
    public String viewReports(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        // Expense statistics
        List<Expense> allExpenses = expenseRepository.findAll();
        
        BigDecimal totalExpenses = allExpenses.stream()
                .filter(e -> e.getStatus() == Expense.ExpenseStatus.APPROVED || e.getStatus() == Expense.ExpenseStatus.PAID)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long pendingCount = allExpenses.stream()
                .filter(e -> e.getStatus() == Expense.ExpenseStatus.PENDING)
                .count();
        
        long approvedCount = allExpenses.stream()
                .filter(e -> e.getStatus() == Expense.ExpenseStatus.APPROVED)
                .count();
        
        long paidCount = allExpenses.stream()
                .filter(e -> e.getStatus() == Expense.ExpenseStatus.PAID)
                .count();
        
        // Expenses by category
        Map<Expense.ExpenseCategory, BigDecimal> expensesByCategory = allExpenses.stream()
                .filter(e -> e.getStatus() == Expense.ExpenseStatus.APPROVED || e.getStatus() == Expense.ExpenseStatus.PAID)
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
        
        // Calculate category percentages
        Map<Expense.ExpenseCategory, Double> categoryPercentages = expensesByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> totalExpenses.compareTo(BigDecimal.ZERO) > 0 
                                ? entry.getValue().divide(totalExpenses, 4, RoundingMode.HALF_UP)
                                        .multiply(new BigDecimal("100"))
                                        .setScale(1, RoundingMode.HALF_UP)
                                        .doubleValue()
                                : 0.0
                ));
        
        // Budget statistics
        List<Project> projects = projectRepository.findAll();
        BigDecimal totalBudget = projects.stream()
                .filter(p -> p.getTotalBudget() != null)
                .map(Project::getTotalBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalUtilized = projects.stream()
                .map(p -> {
                    BigDecimal exp = expenseRepository.getTotalApprovedExpensesByProject(p.getId());
                    return exp != null ? exp : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Payment reminder statistics
        List<PaymentReminder> allReminders = paymentReminderRepository.findAll();
        long pendingReminders = allReminders.stream()
                .filter(r -> r.getStatus() == PaymentReminder.ReminderStatus.PENDING)
                .count();
        
        long overdueReminders = paymentReminderRepository.countOverdueReminders(LocalDate.now());
        
        BigDecimal totalPendingPayments = paymentReminderRepository.getTotalPendingAmount();
        if (totalPendingPayments == null) totalPendingPayments = BigDecimal.ZERO;
        
        // Calculate budget utilization percentage
        double budgetUtilizationPercentage = 0.0;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            budgetUtilizationPercentage = totalUtilized
                    .divide(totalBudget, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("pendingExpensesCount", pendingCount);
        model.addAttribute("approvedExpensesCount", approvedCount);
        model.addAttribute("paidExpensesCount", paidCount);
        model.addAttribute("expensesByCategory", expensesByCategory);
        model.addAttribute("categoryPercentages", categoryPercentages);
        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("totalUtilized", totalUtilized);
        model.addAttribute("budgetUtilizationPercentage", budgetUtilizationPercentage);
        model.addAttribute("pendingRemindersCount", pendingReminders);
        model.addAttribute("overdueRemindersCount", overdueReminders);
        model.addAttribute("totalPendingPayments", totalPendingPayments);
        
        return "finance/reports";
    }
    
    @PostMapping("/reminders/{id}/respond")
    public String respondToClientReminder(@PathVariable Long id,
                                         @RequestParam String responseText,
                                         Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PaymentReminder reminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment reminder not found"));
        
        reminder.setResponseText(responseText);
        reminder.setRespondedAt(LocalDateTime.now());
        reminder.setRespondedBy(currentUser.getName());
        
        paymentReminderRepository.save(reminder);
        return "redirect:/finance/reminders/" + id;
    }
    
    @PostMapping("/reminders/{id}/confirm-payment")
    public String confirmPayment(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PaymentReminder reminder = paymentReminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment reminder not found"));
        
        reminder.setPaymentConfirmed(true);
        reminder.setPaymentConfirmedAt(LocalDateTime.now());
        reminder.setPaymentConfirmedBy(currentUser.getName());
        reminder.setStatus(PaymentReminder.ReminderStatus.PAID);
        reminder.setPaidAt(LocalDateTime.now());
        reminder.setPaidBy(currentUser);
        
        paymentReminderRepository.save(reminder);
        return "redirect:/finance/reminders/" + id;
    }
    
    // ==================== LEAVE REQUESTS ====================
    
    @GetMapping("/leave")
    public String leave(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<LeaveRequest> myLeaveRequests = leaveRequestRepository.findByEmployeeOrderByCreatedAtDesc(currentUser);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("leaveRequests", myLeaveRequests);
        model.addAttribute("leaveTypes", LeaveRequest.LeaveType.values());
        
        return "finance/leave";
    }
    
    @PostMapping("/leave/request")
    public String submitLeaveRequest(@RequestParam LocalDate startDate,
                                    @RequestParam LocalDate endDate,
                                    @RequestParam LeaveRequest.LeaveType type,
                                    @RequestParam String reason,
                                    Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(currentUser);
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setType(type);
        leaveRequest.setReason(reason);
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);
        leaveRequest.setCreatedAt(LocalDateTime.now());
        leaveRequest.setRequestDate(LocalDate.now());
        
        leaveRequestRepository.save(leaveRequest);
        
        return "redirect:/finance/leave?success=true";
    }
    
    @PostMapping("/leave/{id}/delete")
    public String deleteLeaveRequest(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        // Only allow deletion if the request belongs to the current user and is still PENDING
        if (leaveRequest.getEmployee().getId().equals(currentUser.getId()) && 
            leaveRequest.getStatus() == LeaveRequest.LeaveStatus.PENDING) {
            leaveRequestRepository.deleteById(id);
        }
        
        return "redirect:/finance/leave";
    }
    
    @GetMapping("/attendance")
    public String attendance(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get today's attendance
        LocalDate today = LocalDate.now();
        Attendance todayAttendance = attendanceRepository.findByUserAndDate(currentUser, today).orElse(null);
        
        // Get recent attendance records
        List<Attendance> recentAttendance = attendanceRepository.findByUserOrderByDateDesc(currentUser)
                .stream().limit(10).toList();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("recentAttendance", recentAttendance);
        model.addAttribute("currentTime", java.time.LocalTime.now());
        
        return "finance/attendance";
    }
    
    @PostMapping("/attendance/mark")
    public String markAttendance(Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        
        if (!attendanceRepository.existsByUserAndDate(currentUser, today)) {
            Attendance attendance = new Attendance();
            attendance.setUser(currentUser);
            attendance.setDate(today);
            attendance.setCheckIn(now);
            
            // Set status: LATE if after 9:30 AM, otherwise PRESENT
            java.time.LocalTime cutoffTime = java.time.LocalTime.of(9, 30);
            if (now.isAfter(cutoffTime)) {
                attendance.setStatus(Attendance.AttendanceStatus.LATE);
            } else {
                attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
            }
            
            attendance.setCreatedAt(LocalDateTime.now());
            attendanceRepository.save(attendance);
        }
        
        return "redirect:/finance/attendance";
    }
    
    @PostMapping("/attendance/checkout")
    public String checkoutAttendance(Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        
        // Find today's attendance
        Attendance attendance = attendanceRepository.findByUserAndDate(currentUser, today)
                .orElseThrow(() -> new RuntimeException("No check-in record found for today"));
        
        // Mark checkout
        attendance.setCheckOut(now);
        attendance.setHoursWorked(attendance.calculateHoursWorked());
        attendance.setStatus(Attendance.AttendanceStatus.DEPARTED);
        attendance.setUpdatedAt(LocalDateTime.now());
        attendanceRepository.save(attendance);
        
        return "redirect:/finance/attendance?checkout=true";
    }
}
