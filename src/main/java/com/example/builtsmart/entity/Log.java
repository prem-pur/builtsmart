package com.example.builtsmart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Log {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;
    
    @NotBlank(message = "Log description is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "photo_url")
    private String photoUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogType type = LogType.PROGRESS_UPDATE;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private String location;
    
    @Column
    private String weather;
    
    @Column(name = "worker_count")
    private Integer workerCount;
    
    @Column(name = "hours_worked")
    private Integer hoursWorked;
    
    public enum LogType {
        PROGRESS_UPDATE,
        ISSUE_REPORT,
        SAFETY_INCIDENT,
        MATERIAL_DELIVERY,
        QUALITY_CHECK
    }
} 