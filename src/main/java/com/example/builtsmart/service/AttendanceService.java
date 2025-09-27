package com.example.builtsmart.service;

import com.example.builtsmart.entity.Attendance;
import com.example.builtsmart.entity.User;
import com.example.builtsmart.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }
    
    public Optional<Attendance> getAttendanceById(Long id) {
        return attendanceRepository.findById(id);
    }
    
    public Attendance saveAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }
    
    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }
    
    public List<Attendance> getAttendanceByUser(User user) {
        return attendanceRepository.findByUser(user);
    }
    
    public List<Attendance> getAttendanceByUserAndDateRange(User user, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByUserAndDateBetween(user, startDate, endDate);
    }
    
    public Attendance markAttendance(User user, LocalDate date, Attendance.AttendanceStatus status) {
        Optional<Attendance> existingAttendance = attendanceRepository.findByUserAndDate(user, date);
        
        Attendance attendance;
        if (existingAttendance.isPresent()) {
            attendance = existingAttendance.get();
            attendance.setStatus(status);
            attendance.setUpdatedAt(LocalDateTime.now());
        } else {
            attendance = new Attendance();
            attendance.setUser(user);
            attendance.setDate(date);
            attendance.setStatus(status);
            attendance.setCreatedAt(LocalDateTime.now());
        }
        
        return attendanceRepository.save(attendance);
    }
    
    public long countPresentByDate(LocalDate date) {
        return attendanceRepository.countByDateAndStatus(date, Attendance.AttendanceStatus.PRESENT);
    }
    
    public long countAbsentByDate(LocalDate date) {
        return attendanceRepository.countByDateAndStatus(date, Attendance.AttendanceStatus.ABSENT);
    }
    
    public List<Attendance> getTodayAttendance() {
        return getAttendanceByDate(LocalDate.now());
    }
}
