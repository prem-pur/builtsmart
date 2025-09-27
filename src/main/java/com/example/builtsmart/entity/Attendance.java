package com.example.builtsmart.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "check_in")
    private LocalTime checkIn;
    
    @Column(name = "check_out")
    private LocalTime checkOut;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.PRESENT;
    
    @Column
    private String notes;
    
    @Column(name = "hours_worked")
    private Double hoursWorked;
    
    @Column(name = "overtime_hours")
    private Double overtimeHours;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum AttendanceStatus {
        PRESENT,
        ABSENT,
        LATE,
        HALF_DAY,
        ON_LEAVE
    }
    
    public Double calculateHoursWorked() {
        if (checkIn != null && checkOut != null) {
            long minutes = java.time.Duration.between(checkIn, checkOut).toMinutes();
            return minutes / 60.0;
        }
        return 0.0;
    }
} 