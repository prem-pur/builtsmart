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
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class FinanceController {
    
    private final UserService userService;
    private final ProjectService projectService;
    private final ExpenseService expenseService;
    private final ProjectRepository projectRepository;
    private final ExpenseRepository expenseRepository;
    
    @GetMapping("/expenses")
    public String expenses(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Expense> allExpenses = expenseService.getAllExpenses();
        List<Expense> pendingExpenses = expenseService.getPendingExpenses();
        List<Project> projects = projectRepository.findAll();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("allExpenses", allExpenses);
        model.addAttribute("pendingExpenses", pendingExpenses);
        model.addAttribute("projects", projects);
        model.addAttribute("expenseCategories", Expense.ExpenseCategory.values());
        model.addAttribute("expenseStatuses", Expense.ExpenseStatus.values());
        
        return "manager/expenses";
    }
    
    @GetMapping("/expenses/new")
    public String newExpense(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Project> projects = projectRepository.findAll();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("expense", new Expense());
        model.addAttribute("projects", projects);
        model.addAttribute("expenseCategories", Expense.ExpenseCategory.values());
        
        return "manager/expense-form";
    }
    
    @PostMapping("/expenses")
    public String createExpense(@ModelAttribute Expense expense, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        expense.setSubmittedBy(currentUser);
        expense.setStatus(Expense.ExpenseStatus.PENDING);
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }
        
        expenseService.saveExpense(expense);
        return "redirect:/manager/expenses";
    }
    
    @GetMapping("/expenses/{id}/edit")
    public String editExpense(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Expense expense = expenseService.getExpenseById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        List<Project> projects = projectRepository.findAll();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("expense", expense);
        model.addAttribute("projects", projects);
        model.addAttribute("expenseCategories", Expense.ExpenseCategory.values());
        model.addAttribute("expenseStatuses", Expense.ExpenseStatus.values());
        
        return "manager/expense-form";
    }
    
    @PostMapping("/expenses/{id}")
    public String updateExpense(@PathVariable Long id, @ModelAttribute Expense expense, Authentication authentication) {
        Expense existingExpense = expenseService.getExpenseById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        existingExpense.setDescription(expense.getDescription());
        existingExpense.setAmount(expense.getAmount());
        existingExpense.setCategory(expense.getCategory());
        existingExpense.setExpenseDate(expense.getExpenseDate());
        existingExpense.setNotes(expense.getNotes());
        existingExpense.setProject(expense.getProject());
        
        expenseService.saveExpense(existingExpense);
        return "redirect:/manager/expenses";
    }
    
    @PostMapping("/expenses/{id}/approve")
    public String approveExpense(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        expenseService.approveExpense(id, currentUser);
        return "redirect:/manager/expenses";
    }
    
    @PostMapping("/expenses/{id}/reject")
    public String rejectExpense(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        expenseService.rejectExpense(id, currentUser);
        return "redirect:/manager/expenses";
    }
    
    @PostMapping("/expenses/{id}/delete")
    public String deleteExpense(@PathVariable Long id, Authentication authentication) {
        expenseService.deleteExpense(id);
        return "redirect:/manager/expenses";
    }
    
    @GetMapping("/budget")
    public String budget(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Project> projects = projectRepository.findAll();
        
        // Calculate budget vs actual for each project
        for (Project project : projects) {
            BigDecimal totalExpenses = expenseService.getTotalApprovedExpensesByProject(project.getId());
            project.setCurrentExpense(totalExpenses);
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("projects", projects);
        
        return "manager/budget";
    }
}
