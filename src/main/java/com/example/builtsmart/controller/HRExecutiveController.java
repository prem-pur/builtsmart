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
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HRExecutiveController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Leave statistics
        List<LeaveRequest> allLeaveRequests = leaveRequestRepository.findAll();
        long pendingLeaves = leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.PENDING).size();
        long approvedLeaves = leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.APPROVED).size();
        long rejectedLeaves = leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.REJECTED).size();
        
        // Attendance statistics
        LocalDate today = LocalDate.now();
        List<Attendance> todayAttendance = attendanceRepository.findByDate(today);
        long presentToday = attendanceRepository.countByDateAndStatus(today, Attendance.AttendanceStatus.PRESENT);
        long totalEmployees = userRepository.count();
        long absentToday = totalEmployees - presentToday;
        
        // Recent leave requests
        List<LeaveRequest> recentLeaveRequests = leaveRequestRepository.findPendingLeaveRequests()
                .stream().limit(5).toList();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("totalLeaveRequests", allLeaveRequests.size());
        model.addAttribute("pendingLeaves", pendingLeaves);
        model.addAttribute("approvedLeaves", approvedLeaves);
        model.addAttribute("rejectedLeaves", rejectedLeaves);
        model.addAttribute("presentToday", presentToday);
        model.addAttribute("absentToday", absentToday);
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("recentLeaveRequests", recentLeaveRequests);
        model.addAttribute("todayAttendance", todayAttendance);
        
        return "hr/dashboard";
    }
    
    @GetMapping("/leave-requests")
    public String leaveRequests(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<LeaveRequest> allLeaveRequests = leaveRequestRepository.findAll();
        List<LeaveRequest> pendingRequests = leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.PENDING);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("allLeaveRequests", allLeaveRequests);
        model.addAttribute("pendingRequests", pendingRequests);
        
        return "hr/leave-requests";
    }
    
    @GetMapping("/leave-requests/{id}")
    public String leaveRequestDetails(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        model.addAttribute("user", currentUser);
        model.addAttribute("leaveRequest", leaveRequest);
        
        return "hr/leave-request-details";
    }
    
    @PostMapping("/leave-requests/{id}/approve")
    public String approveLeaveRequest(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(currentUser);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setApprovedDate(LocalDate.now());
        
        leaveRequestRepository.save(leaveRequest);
        
        return "redirect:/hr/leave-requests?approved=true";
    }
    
    @PostMapping("/leave-requests/{id}/reject")
    public String rejectLeaveRequest(@PathVariable Long id,
                                    @RequestParam String rejectionReason,
                                    Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(currentUser);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setRejectionReason(rejectionReason);
        
        leaveRequestRepository.save(leaveRequest);
        
        return "redirect:/hr/leave-requests?rejected=true";
    }
    
    @GetMapping("/attendance")
    public String attendance(@RequestParam(required = false) String date,
                           Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate selectedDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<Attendance> attendanceRecords = attendanceRepository.findByDate(selectedDate);
        
        // Get all employees
        List<User> allEmployees = userRepository.findAll();
        
        // Calculate statistics
        long presentCount = attendanceRepository.countByDateAndStatus(selectedDate, Attendance.AttendanceStatus.PRESENT);
        long lateCount = attendanceRepository.countByDateAndStatus(selectedDate, Attendance.AttendanceStatus.LATE);
        long absentCount = allEmployees.size() - attendanceRecords.size();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("attendanceRecords", attendanceRecords);
        model.addAttribute("allEmployees", allEmployees);
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("lateCount", lateCount);
        model.addAttribute("absentCount", absentCount);
        
        return "hr/attendance";
    }
    
    @GetMapping("/employees")
    public String employees(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<User> allEmployees = userRepository.findAll();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("employees", allEmployees);
        model.addAttribute("roles", User.UserRole.values());
        
        return "hr/employees";
    }
    
    @PostMapping("/employees/create")
    public String createEmployee(@ModelAttribute User employee, @RequestParam String password) {
        // Encode password
        employee.setPassword(passwordEncoder.encode(password));
        employee.setActive(true);
        
        userRepository.save(employee);
        return "redirect:/hr/employees";
    }
    
    @PostMapping("/employees/{id}/update")
    public String updateEmployee(@PathVariable Long id, 
                                @RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String department) {
        User employee = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        employee.setName(name);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setAddress(address);
        employee.setDepartment(department);
        
        userRepository.save(employee);
        return "redirect:/hr/employees";
    }
    
    @PostMapping("/employees/{id}/update-role")
    public String updateEmployeeRole(@PathVariable Long id, @RequestParam User.UserRole role) {
        User employee = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        employee.setRole(role);
        userRepository.save(employee);
        
        return "redirect:/hr/employees";
    }
    
    @PostMapping("/employees/{id}/delete")
    public String deleteEmployee(@PathVariable Long id) {
        // Soft delete - just deactivate the user
        User employee = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        employee.setActive(false);
        userRepository.save(employee);
        
        return "redirect:/hr/employees";
    }
    
    @PostMapping("/employees/{id}/activate")
    public String activateEmployee(@PathVariable Long id) {
        User employee = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        employee.setActive(true);
        userRepository.save(employee);
        
        return "redirect:/hr/employees";
    }
}
