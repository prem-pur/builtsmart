package com.example.builtsmart.controller;

import com.example.builtsmart.entity.*;
import com.example.builtsmart.repository.*;
import com.example.builtsmart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/worker")
@RequiredArgsConstructor
public class WorkerController {
    
    private final UserService userService;
    private final TaskRepository taskRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get worker's assigned tasks
        List<Task> assignedTasks = taskRepository.findByAssignedTo(currentUser);
        
        // Task statistics
        long totalTasks = assignedTasks.size();
        long completedTasks = assignedTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED)
                .count();
        long inProgressTasks = assignedTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS)
                .count();
        long pendingTasks = assignedTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.PENDING)
                .count();
        
        // Overdue tasks
        long overdueTasks = assignedTasks.stream()
                .filter(t -> t.getDeadline() != null && 
                            t.getDeadline().isBefore(LocalDate.now()) && 
                            t.getStatus() != Task.TaskStatus.COMPLETED)
                .count();
        
        // Get today's tasks
        List<Task> todayTasks = assignedTasks.stream()
                .filter(t -> t.getDeadline() != null && 
                            t.getDeadline().equals(LocalDate.now()) &&
                            t.getStatus() != Task.TaskStatus.COMPLETED)
                .toList();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("inProgressTasks", inProgressTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("todayTasks", todayTasks);
        model.addAttribute("recentTasks", assignedTasks.stream().limit(5).toList());
        
        return "worker/dashboard";
    }
    
    @GetMapping("/tasks")
    public String tasks(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Task> assignedTasks = taskRepository.findByAssignedToOrderByDeadlineAsc(currentUser);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("tasks", assignedTasks);
        
        return "worker/tasks";
    }
    
    @GetMapping("/attendance")
    public String attendance(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get today's attendance
        LocalDate today = LocalDate.now();
        Attendance todayAttendance = attendanceRepository.findByUserAndDate(currentUser, today).orElse(null);
        
        // Get recent attendance records
        List<Attendance> recentAttendance = attendanceRepository.findByUserOrderByDateDesc(currentUser)
                .stream().limit(10).toList();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("recentAttendance", recentAttendance);
        model.addAttribute("currentTime", LocalTime.now());
        
        return "worker/attendance";
    }
    
    @PostMapping("/attendance/mark")
    public String markAttendance(Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        // Check if already marked today
        Attendance existing = attendanceRepository.findByUserAndDate(currentUser, today).orElse(null);
        
        if (existing == null) {
            // Create new attendance record
            Attendance attendance = new Attendance();
            attendance.setUser(currentUser);
            attendance.setDate(today);
            attendance.setCheckIn(now);
            
            // Set status: LATE if after 9:30 AM, otherwise PRESENT
            LocalTime cutoffTime = LocalTime.of(9, 30);
            if (now.isAfter(cutoffTime)) {
                attendance.setStatus(Attendance.AttendanceStatus.LATE);
            } else {
                attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
            }
            
            attendance.setCreatedAt(LocalDateTime.now());
            attendanceRepository.save(attendance);
        }
        
        return "redirect:/worker/attendance?success=true";
    }
    
    @PostMapping("/attendance/checkout")
    public String checkoutAttendance(Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        // Find today's attendance
        Attendance attendance = attendanceRepository.findByUserAndDate(currentUser, today)
                .orElseThrow(() -> new RuntimeException("No check-in record found for today"));
        
        // Mark checkout
        attendance.setCheckOut(now);
        attendance.setHoursWorked(attendance.calculateHoursWorked());
        attendance.setStatus(Attendance.AttendanceStatus.DEPARTED);
        attendance.setUpdatedAt(LocalDateTime.now());
        attendanceRepository.save(attendance);
        
        return "redirect:/worker/attendance?checkout=true";
    }
    
    @GetMapping("/leave")
    public String leave(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<LeaveRequest> myLeaveRequests = leaveRequestRepository.findByEmployeeOrderByCreatedAtDesc(currentUser);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("leaveRequests", myLeaveRequests);
        model.addAttribute("leaveTypes", LeaveRequest.LeaveType.values());
        
        return "worker/leave";
    }
    
    @PostMapping("/leave/request")
    public String submitLeaveRequest(@RequestParam LocalDate startDate,
                                    @RequestParam LocalDate endDate,
                                    @RequestParam LeaveRequest.LeaveType type,
                                    @RequestParam String reason,
                                    Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(currentUser);
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setType(type);
        leaveRequest.setReason(reason);
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);
        leaveRequest.setCreatedAt(LocalDateTime.now());
        leaveRequest.setRequestDate(LocalDate.now());
        
        leaveRequestRepository.save(leaveRequest);
        
        return "redirect:/worker/leave?success=true";
    }
    
    @PostMapping("/leave/{id}/delete")
    public String deleteLeaveRequest(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        // Only allow deletion if the request belongs to the current user and is still PENDING
        if (leaveRequest.getEmployee().getId().equals(currentUser.getId()) && 
            leaveRequest.getStatus() == LeaveRequest.LeaveStatus.PENDING) {
            leaveRequestRepository.deleteById(id);
        }
        
        return "redirect:/worker/leave";
    }
    
    @PostMapping("/tasks/{id}/update-status")
    public String updateTaskStatus(@PathVariable Long id,
                                   @RequestParam String status,
                                   Authentication authentication) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        task.setStatus(Task.TaskStatus.valueOf(status));
        taskRepository.save(task);
        
        return "redirect:/worker/tasks";
    }
}
