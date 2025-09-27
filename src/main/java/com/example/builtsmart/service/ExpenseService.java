package com.example.builtsmart.service;

import com.example.builtsmart.entity.Expense;
import com.example.builtsmart.entity.Project;
import com.example.builtsmart.entity.User;
import com.example.builtsmart.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    
    private final ExpenseRepository expenseRepository;
    
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }
    
    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }
    
    public Expense saveExpense(Expense expense) {
        if (expense.getId() == null) {
            expense.setCreatedAt(LocalDateTime.now());
        }
        return expenseRepository.save(expense);
    }
    
    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }
    
    public List<Expense> getExpensesByProject(Project project) {
        return expenseRepository.findByProject(project);
    }
    
    public List<Expense> getExpensesByUser(User user) {
        return expenseRepository.findBySubmittedBy(user);
    }
    
    public List<Expense> getExpensesByStatus(Expense.ExpenseStatus status) {
        return expenseRepository.findByStatus(status);
    }
    
    public List<Expense> getPendingExpenses() {
        return expenseRepository.findByStatus(Expense.ExpenseStatus.PENDING);
    }
    
    public BigDecimal getTotalApprovedExpensesByProject(Long projectId) {
        BigDecimal total = expenseRepository.getTotalApprovedExpensesByProject(projectId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public Expense approveExpense(Long expenseId, User approvedBy) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expense.setStatus(Expense.ExpenseStatus.APPROVED);
        expense.setApprovedBy(approvedBy);
        expense.setApprovedAt(LocalDateTime.now());
        return expenseRepository.save(expense);
    }
    
    public Expense rejectExpense(Long expenseId, User approvedBy) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expense.setStatus(Expense.ExpenseStatus.REJECTED);
        expense.setApprovedBy(approvedBy);
        expense.setApprovedAt(LocalDateTime.now());
        return expenseRepository.save(expense);
    }
    
    public List<Expense> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByExpenseDateBetween(startDate, endDate);
    }
    
    public List<Expense> getExpensesByCategory(Expense.ExpenseCategory category) {
        return expenseRepository.findByCategory(category);
    }
}
