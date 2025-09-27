package com.example.builtsmart.repository;

import com.example.builtsmart.entity.Expense;
import com.example.builtsmart.entity.Project;
import com.example.builtsmart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    List<Expense> findByProject(Project project);
    
    List<Expense> findBySubmittedBy(User submittedBy);
    
    List<Expense> findByStatus(Expense.ExpenseStatus status);
    
    List<Expense> findByCategory(Expense.ExpenseCategory category);
    
    @Query("SELECT e FROM Expense e WHERE e.project.id = :projectId AND e.status = :status")
    List<Expense> findByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") Expense.ExpenseStatus status);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.project.id = :projectId AND e.status = 'APPROVED'")
    BigDecimal getTotalApprovedExpensesByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    List<Expense> findByExpenseDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e FROM Expense e WHERE e.project.id = :projectId ORDER BY e.createdAt DESC")
    List<Expense> findByProjectOrderByCreatedAtDesc(@Param("projectId") Long projectId);
}
