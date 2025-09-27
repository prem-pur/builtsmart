package com.example.builtsmart.repository;

import com.example.builtsmart.entity.Attendance;
import com.example.builtsmart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    List<Attendance> findByUser(User user);
    
    List<Attendance> findByDate(LocalDate date);
    
    List<Attendance> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    Optional<Attendance> findByUserAndDate(User user, LocalDate date);
    
    long countByDateAndStatus(LocalDate date, Attendance.AttendanceStatus status);
    
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.status = :status")
    List<Attendance> findByDateAndStatus(@Param("date") LocalDate date, @Param("status") Attendance.AttendanceStatus status);
    
    @Query("SELECT a FROM Attendance a WHERE a.date = :date ORDER BY a.user.name")
    List<Attendance> findByDateOrderByUserName(@Param("date") LocalDate date);
}