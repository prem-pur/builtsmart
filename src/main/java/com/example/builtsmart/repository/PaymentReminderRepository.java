package com.example.builtsmart.repository;

import com.example.builtsmart.entity.PaymentReminder;
import com.example.builtsmart.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentReminderRepository extends JpaRepository<PaymentReminder, Long> {
    
    List<PaymentReminder> findByProject(Project project);
    
    List<PaymentReminder> findByStatus(PaymentReminder.ReminderStatus status);
    
    List<PaymentReminder> findByPriority(PaymentReminder.Priority priority);
    
    @Query("SELECT p FROM PaymentReminder p WHERE p.dueDate < :today AND p.status = 'PENDING'")
    List<PaymentReminder> findOverdueReminders(@Param("today") LocalDate today);
    
    @Query("SELECT p FROM PaymentReminder p WHERE p.dueDate BETWEEN :startDate AND :endDate AND p.status = 'PENDING'")
    List<PaymentReminder> findUpcomingReminders(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT p FROM PaymentReminder p WHERE p.project.id = :projectId ORDER BY p.dueDate ASC")
    List<PaymentReminder> findByProjectOrderByDueDateAsc(@Param("projectId") Long projectId);
    
    @Query("SELECT SUM(p.amount) FROM PaymentReminder p WHERE p.status = 'PENDING'")
    BigDecimal getTotalPendingAmount();
    
    @Query("SELECT SUM(pr.amount) FROM PaymentReminder pr WHERE pr.status = 'OVERDUE'")
    BigDecimal getTotalOverdueAmount();
    
    // Client-specific queries (client-submitted reminders)
    List<PaymentReminder> findByClientEmailOrderByCreatedAtDesc(String clientEmail);
    
    @Query("SELECT pr FROM PaymentReminder pr WHERE pr.isClientSubmitted = true ORDER BY pr.createdAt DESC")
    List<PaymentReminder> findAllClientSubmittedReminders();
    
    @Query("SELECT COUNT(pr) FROM PaymentReminder pr WHERE pr.isClientSubmitted = true AND pr.status = 'PENDING'")
    long countPendingClientReminders();
    
    // Find reminders for projects where user is client
    @Query("SELECT pr FROM PaymentReminder pr WHERE pr.project.client.email = :clientEmail AND pr.isClientSubmitted = false ORDER BY pr.dueDate ASC")
    List<PaymentReminder> findRemindersByClientEmail(@Param("clientEmail") String clientEmail);
    
    @Query("SELECT pr FROM PaymentReminder pr WHERE pr.paymentSubmitted = true AND pr.paymentConfirmed = false ORDER BY pr.paymentSubmittedAt DESC")
    List<PaymentReminder> findPendingPaymentConfirmations();
    
    @Query("SELECT COUNT(pr) FROM PaymentReminder pr WHERE pr.paymentSubmitted = true AND pr.paymentConfirmed = false")
    long countPendingPaymentConfirmations();
    
    @Query("SELECT COUNT(p) FROM PaymentReminder p WHERE p.dueDate < :today AND p.status = 'PENDING'")
    long countOverdueReminders(@Param("today") LocalDate today);
    
    List<PaymentReminder> findByStatusOrderByDueDateAsc(PaymentReminder.ReminderStatus status);
}
