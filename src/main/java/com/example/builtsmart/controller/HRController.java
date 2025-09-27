package com.example.builtsmart.controller;

import com.example.builtsmart.entity.*;
import com.example.builtsmart.repository.*;
import com.example.builtsmart.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class HRController {
    
    private final UserService userService;
    private final AttendanceService attendanceService;
    private final LeaveRequestService leaveRequestService;
    private final UserRepository userRepository;
    
    @GetMapping("/attendance")
    public String attendance(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Attendance> todayAttendance = attendanceService.getTodayAttendance();
        List<User> allWorkers = userRepository.findByRole(User.UserRole.WORKER);
        long presentCount = attendanceService.countPresentByDate(LocalDate.now());
        long absentCount = attendanceService.countAbsentByDate(LocalDate.now());
        
        model.addAttribute("user", currentUser);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("allWorkers", allWorkers);
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("attendanceStatuses", Attendance.AttendanceStatus.values());
        
        return "manager/attendance";
    }
    
    @PostMapping("/attendance/mark")
    public String markAttendance(@RequestParam Long userId, 
                                @RequestParam Attendance.AttendanceStatus status,
                                @RequestParam(required = false) LocalDate date,
                                Authentication authentication) {
        User worker = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate attendanceDate = date != null ? date : LocalDate.now();
        attendanceService.markAttendance(worker, attendanceDate, status);
        
        return "redirect:/manager/attendance";
    }
    
    @GetMapping("/leave-requests")
    public String leaveRequests(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<LeaveRequest> allLeaveRequests = leaveRequestService.getAllLeaveRequests();
        List<LeaveRequest> pendingRequests = leaveRequestService.getPendingLeaveRequests();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("allLeaveRequests", allLeaveRequests);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("leaveStatuses", LeaveRequest.LeaveStatus.values());
        model.addAttribute("leaveTypes", LeaveRequest.LeaveType.values());
        
        return "manager/leave-requests";
    }
    
    @GetMapping("/leave-requests/new")
    public String newLeaveRequest(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<User> employees = userRepository.findAll();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("leaveRequest", new LeaveRequest());
        model.addAttribute("employees", employees);
        model.addAttribute("leaveTypes", LeaveRequest.LeaveType.values());
        
        return "manager/leave-request-form";
    }
    
    @PostMapping("/leave-requests")
    public String createLeaveRequest(@ModelAttribute LeaveRequest leaveRequest, Authentication authentication) {
        leaveRequestService.saveLeaveRequest(leaveRequest);
        return "redirect:/manager/leave-requests";
    }
    
    @PostMapping("/leave-requests/{id}/approve")
    public String approveLeaveRequest(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        leaveRequestService.approveLeaveRequest(id, currentUser);
        return "redirect:/manager/leave-requests";
    }
    
    @PostMapping("/leave-requests/{id}/reject")
    public String rejectLeaveRequest(@PathVariable Long id, 
                                   @RequestParam String reason,
                                   Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        leaveRequestService.rejectLeaveRequest(id, currentUser, reason);
        return "redirect:/manager/leave-requests";
    }
}
