package com.example.builtsmart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Project name is required")
    @Column(nullable = false)
    private String name;
    
    @Column
    private String description;
    
    @Column(nullable = false)
    private String location;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.PLANNING;
    
    @Column(name = "total_budget", precision = 15, scale = 2)
    private BigDecimal totalBudget;
    
    @Column(name = "current_expense", precision = 15, scale = 2)
    private BigDecimal currentExpense = BigDecimal.ZERO;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_manager_id")
    private User projectManager;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Task> tasks;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Log> logs;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Expense> expenses;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentReminder> paymentReminders;
    
    @Column(name = "created_at")
    private LocalDate createdAt = LocalDate.now();
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // Transient fields for UI display
    @Transient
    private Integer completionPercentage;
    
    @Transient
    private Integer taskCount;
    
    public enum ProjectStatus {
        PLANNING,
        IN_PROGRESS,
        ON_HOLD,
        COMPLETED,
        CANCELLED
    }
    
    public BigDecimal getProgressPercentage() {
        // Calculate based on task completion instead of budget
        if (tasks == null || tasks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        long completedTasks = tasks.stream()
                .filter(task -> task.getStatus() == Task.TaskStatus.COMPLETED)
                .count();
        
        BigDecimal percentage = BigDecimal.valueOf(completedTasks)
                .divide(BigDecimal.valueOf(tasks.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        return percentage;
    }
} 