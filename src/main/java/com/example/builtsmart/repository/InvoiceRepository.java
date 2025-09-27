package com.example.builtsmart.repository;

import com.example.builtsmart.entity.Invoice;
import com.example.builtsmart.entity.Project;
import com.example.builtsmart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    List<Invoice> findByProject(Project project);
    
    List<Invoice> findByIssuedTo(User issuedTo);
    
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);
    
    @Query("SELECT i FROM Invoice i WHERE i.project.id = :projectId ORDER BY i.createdAt DESC")
    List<Invoice> findByProjectOrderByCreatedAtDesc(@Param("projectId") Long projectId);
    
    @Query("SELECT i FROM Invoice i WHERE i.dueDate BETWEEN :startDate AND :endDate")
    List<Invoice> findByDueDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :currentDate AND i.status != 'PAID'")
    List<Invoice> findOverdueInvoices(@Param("currentDate") LocalDate currentDate);
}
