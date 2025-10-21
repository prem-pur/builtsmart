package com.example.builtsmart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReminder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;
    
    @NotBlank(message = "Description is required")
    @Column(nullable = false)
    private String description;
    
    @NotNull(message = "Amount is required")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "recipient")
    private String recipient;
    
    @Column(name = "recipient_contact")
    private String recipientContact;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus status = ReminderStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;
    
    @Column
    private String notes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private User paidBy;
    
    // Client-submitted reminder fields
    @Column(name = "client_name")
    private String clientName;
    
    @Column(name = "client_email")
    private String clientEmail;
    
    @Column(name = "is_client_submitted")
    private Boolean isClientSubmitted = false;
    
    @Column(name = "response_text", length = 4000)
    private String responseText;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    @Column(name = "responded_by")
    private String respondedBy;
    
    // Payment submission tracking
    @Column(name = "payment_submitted")
    private Boolean paymentSubmitted = false;
    
    @Column(name = "payment_submitted_at")
    private LocalDateTime paymentSubmittedAt;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "payment_proof_url")
    private String paymentProofUrl;
    
    @Column(name = "payment_notes")
    private String paymentNotes;
    
    @Column(name = "payment_confirmed")
    private Boolean paymentConfirmed = false;
    
    @Column(name = "payment_confirmed_at")
    private LocalDateTime paymentConfirmedAt;
    
    @Column(name = "payment_confirmed_by")
    private String paymentConfirmedBy;
    
    public enum ReminderStatus {
        PENDING,
        AWAITING_CONFIRMATION,
        OVERDUE,
        PAID,
        CANCELLED
    }
    
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
}
