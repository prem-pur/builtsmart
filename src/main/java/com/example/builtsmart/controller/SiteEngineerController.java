package com.example.builtsmart.controller;

import com.example.builtsmart.entity.*;
import com.example.builtsmart.repository.*;
import com.example.builtsmart.service.ProjectService;
import com.example.builtsmart.service.TaskService;
import com.example.builtsmart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/engineer")
@RequiredArgsConstructor
public class SiteEngineerController {
    
    private final UserService userService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final LogRepository logRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    
    // ==================== DASHBOARD ====================
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        // Get engineer's assigned tasks
        List<Task> myTasks = taskRepository.findByAssignedTo(currentUser);
        List<Task> myPendingTasks = myTasks.stream()
                .filter(t -> t.getStatus() != Task.TaskStatus.COMPLETED)
                .collect(Collectors.toList());
        
        // Get projects where engineer has tasks
        List<Project> myProjects = myTasks.stream()
                .map(Task::getProject)
                .distinct()
                .filter(p -> p.getStatus() == Project.ProjectStatus.IN_PROGRESS)
                .collect(Collectors.toList());
        
        // Get engineer's logs
        List<Log> myLogs = logRepository.findBySubmittedBy(currentUser);
        
        // Get recent issues (ISSUE_REPORT logs)
        List<Log> myIssues = myLogs.stream()
                .filter(log -> log.getType() == Log.LogType.ISSUE_REPORT)
                .collect(Collectors.toList());
        
        // Get recent logs (last 5)
        List<Log> recentLogs = myLogs.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
        
        // Get recent tasks (last 3)
        List<Task> recentTasks = myTasks.stream()
                .sorted((a, b) -> {
                    if (b.getCreatedAt() == null) return -1;
                    if (a.getCreatedAt() == null) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(3)
                .collect(Collectors.toList());
        
        model.addAttribute("activeProjectsCount", myProjects.size());
        model.addAttribute("myTasksCount", myPendingTasks.size());
        model.addAttribute("siteLogsCount", myLogs.size());
        model.addAttribute("issuesCount", myIssues.size());
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("recentTasks", recentTasks);
        
        return "engineer/dashboard";
    }
    
    // ==================== SITE LOGS MANAGEMENT ====================
    
    @GetMapping("/logs")
    public String listLogs(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        List<Log> logs = logRepository.findBySubmittedByOrderByCreatedAtDesc(currentUser);
        model.addAttribute("logs", logs);
        
        return "engineer/logs";
    }
    
    @GetMapping("/logs/new")
    public String newLogForm(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        // Get projects where engineer has tasks
        List<Task> myTasks = taskRepository.findByAssignedTo(currentUser);
        List<Project> myProjects = myTasks.stream()
                .map(Task::getProject)
                .distinct()
                .collect(Collectors.toList());
        
        model.addAttribute("log", new Log());
        model.addAttribute("projects", myProjects);
        model.addAttribute("logTypes", Log.LogType.values());
        
        return "engineer/log-form";
    }
    
    @PostMapping("/logs")
    public String createLog(@ModelAttribute Log log, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        log.setSubmittedBy(currentUser);
        log.setCreatedAt(LocalDateTime.now());
        logRepository.save(log);
        
        return "redirect:/engineer/logs";
    }
    
    @GetMapping("/logs/{id}")
    public String viewLog(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        Log log = logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found"));
        
        // Verify the log belongs to the current engineer
        if (!log.getSubmittedBy().getId().equals(currentUser.getId())) {
            return "redirect:/engineer/logs";
        }
        
        model.addAttribute("log", log);
        return "engineer/log-detail";
    }
    
    @GetMapping("/logs/{id}/edit")
    public String editLogForm(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        Log log = logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found"));
        
        // Verify the log belongs to the current engineer
        if (!log.getSubmittedBy().getId().equals(currentUser.getId())) {
            return "redirect:/engineer/logs";
        }
        
        // Get projects where engineer has tasks
        List<Task> myTasks = taskRepository.findByAssignedTo(currentUser);
        List<Project> myProjects = myTasks.stream()
                .map(Task::getProject)
                .distinct()
                .collect(Collectors.toList());
        
        model.addAttribute("log", log);
        model.addAttribute("projects", myProjects);
        model.addAttribute("logTypes", Log.LogType.values());
        
        return "engineer/log-form";
    }
    
    @PostMapping("/logs/{id}")
    public String updateLog(@PathVariable Long id, @ModelAttribute Log log, 
                           Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Log existingLog = logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found"));
        
        // Verify the log belongs to the current engineer
        if (!existingLog.getSubmittedBy().getId().equals(currentUser.getId())) {
            return "redirect:/engineer/logs";
        }
        
        existingLog.setProject(log.getProject());
        existingLog.setType(log.getType());
        existingLog.setDescription(log.getDescription());
        existingLog.setPhotoUrl(log.getPhotoUrl());
        existingLog.setLocation(log.getLocation());
        existingLog.setWeather(log.getWeather());
        existingLog.setWorkerCount(log.getWorkerCount());
        existingLog.setHoursWorked(log.getHoursWorked());
        
        logRepository.save(existingLog);
        
        return "redirect:/engineer/logs/" + id;
    }
    
    @PostMapping("/logs/{id}/delete")
    public String deleteLog(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Log log = logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found"));
        
        // Verify the log belongs to the current engineer
        if (!log.getSubmittedBy().getId().equals(currentUser.getId())) {
            return "redirect:/engineer/logs";
        }
        
        logRepository.delete(log);
        return "redirect:/engineer/logs";
    }
    
    // ==================== ISSUES MANAGEMENT ====================
    
    @GetMapping("/issues")
    public String listIssues(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        List<Log> issues = logRepository.findBySubmittedByAndTypeOrderByCreatedAtDesc(
                currentUser, Log.LogType.ISSUE_REPORT);
        model.addAttribute("issues", issues);
        
        return "engineer/issues";
    }
    
    @GetMapping("/issues/new")
    public String newIssueForm(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        // Get projects where engineer has tasks
        List<Task> myTasks = taskRepository.findByAssignedTo(currentUser);
        List<Project> myProjects = myTasks.stream()
                .map(Task::getProject)
                .distinct()
                .collect(Collectors.toList());
        
        Log issue = new Log();
        issue.setType(Log.LogType.ISSUE_REPORT);
        
        model.addAttribute("issue", issue);
        model.addAttribute("projects", myProjects);
        
        return "engineer/issue-form";
    }
    
    @PostMapping("/issues")
    public String createIssue(@ModelAttribute Log issue, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        issue.setSubmittedBy(currentUser);
        issue.setType(Log.LogType.ISSUE_REPORT);
        issue.setCreatedAt(LocalDateTime.now());
        logRepository.save(issue);
        
        return "redirect:/engineer/issues";
    }
    
    @GetMapping("/issues/{id}")
    public String viewIssue(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        Log issue = logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
        
        // Verify the issue belongs to the current engineer
        if (!issue.getSubmittedBy().getId().equals(currentUser.getId())) {
            return "redirect:/engineer/issues";
        }
        
        model.addAttribute("issue", issue);
        return "engineer/issue-detail";
    }
    
    // ==================== TASKS VIEW ====================
    
    @GetMapping("/tasks")
    public String listTasks(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        List<Task> myTasks = taskRepository.findByAssignedToOrderByDeadlineAsc(currentUser);
        model.addAttribute("tasks", myTasks);
        
        // Group tasks by status
        List<Task> pendingTasks = myTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.PENDING)
                .collect(Collectors.toList());
        List<Task> inProgressTasks = myTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS)
                .collect(Collectors.toList());
        List<Task> completedTasks = myTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED)
                .collect(Collectors.toList());
        
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("inProgressTasks", inProgressTasks);
        model.addAttribute("completedTasks", completedTasks);
        
        return "engineer/tasks";
    }
    
    @GetMapping("/tasks/{id}")
    public String viewTask(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Verify the task is assigned to the current engineer
        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
            return "redirect:/engineer/tasks";
        }
        
        model.addAttribute("task", task);
        model.addAttribute("taskStatuses", Task.TaskStatus.values());
        
        return "engineer/task-detail";
    }
    
    @PostMapping("/tasks/{id}/update-status")
    public String updateTaskStatus(@PathVariable Long id, @RequestParam Task.TaskStatus status,
                                   Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Verify the task is assigned to the current engineer
        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
            return "redirect:/engineer/tasks";
        }
        
        task.setStatus(status);
        if (status == Task.TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }
        taskRepository.save(task);
        
        return "redirect:/engineer/tasks/" + id;
    }
    
    // ==================== PROGRESS REPORTS ====================
    
    @GetMapping("/progress")
    public String listProgressReports(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        List<Log> progressReports = logRepository.findBySubmittedByAndTypeOrderByCreatedAtDesc(
                currentUser, Log.LogType.PROGRESS_UPDATE);
        model.addAttribute("progressReports", progressReports);
        
        return "engineer/progress";
    }
    
    @GetMapping("/progress/new")
    public String newProgressForm(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        // Get projects where engineer has tasks
        List<Task> myTasks = taskRepository.findByAssignedTo(currentUser);
        List<Project> myProjects = myTasks.stream()
                .map(Task::getProject)
                .distinct()
                .collect(Collectors.toList());
        
        Log progress = new Log();
        progress.setType(Log.LogType.PROGRESS_UPDATE);
        
        model.addAttribute("progress", progress);
        model.addAttribute("projects", myProjects);
        
        return "engineer/progress-form";
    }
    
    @PostMapping("/progress")
    public String createProgress(@ModelAttribute Log progress, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        progress.setSubmittedBy(currentUser);
        progress.setType(Log.LogType.PROGRESS_UPDATE);
        progress.setCreatedAt(LocalDateTime.now());
        logRepository.save(progress);
        
        return "redirect:/engineer/progress";
    }
    
    // ==================== REPORTS VIEW ====================
    
    @GetMapping("/reports")
    public String viewReports(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        
        // Get all logs by engineer
        List<Log> allLogs = logRepository.findBySubmittedBy(currentUser);
        
        // Group by type
        long progressCount = allLogs.stream()
                .filter(log -> log.getType() == Log.LogType.PROGRESS_UPDATE)
                .count();
        long issueCount = allLogs.stream()
                .filter(log -> log.getType() == Log.LogType.ISSUE_REPORT)
                .count();
        long safetyCount = allLogs.stream()
                .filter(log -> log.getType() == Log.LogType.SAFETY_INCIDENT)
                .count();
        long materialCount = allLogs.stream()
                .filter(log -> log.getType() == Log.LogType.MATERIAL_DELIVERY)
                .count();
        long qualityCount = allLogs.stream()
                .filter(log -> log.getType() == Log.LogType.QUALITY_CHECK)
                .count();
        
        model.addAttribute("progressCount", progressCount);
        model.addAttribute("issueCount", issueCount);
        model.addAttribute("safetyCount", safetyCount);
        model.addAttribute("materialCount", materialCount);
        model.addAttribute("qualityCount", qualityCount);
        model.addAttribute("totalLogs", allLogs.size());
        
        // Get tasks statistics
        List<Task> myTasks = taskRepository.findByAssignedTo(currentUser);
        long completedTasksCount = myTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED)
                .count();
        long pendingTasksCount = myTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.PENDING)
                .count();
        long inProgressTasksCount = myTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS)
                .count();
        
        model.addAttribute("completedTasksCount", completedTasksCount);
        model.addAttribute("pendingTasksCount", pendingTasksCount);
        model.addAttribute("inProgressTasksCount", inProgressTasksCount);
        model.addAttribute("totalTasks", myTasks.size());
        
        return "engineer/reports";
    }
    
    // ==================== LEAVE REQUESTS ====================
    
    @GetMapping("/leave")
    public String leave(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<LeaveRequest> myLeaveRequests = leaveRequestRepository.findByEmployeeOrderByCreatedAtDesc(currentUser);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("leaveRequests", myLeaveRequests);
        model.addAttribute("leaveTypes", LeaveRequest.LeaveType.values());
        
        return "engineer/leave";
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
        
        return "redirect:/engineer/leave?success=true";
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
        
        return "redirect:/engineer/leave";
    }
    
    @GetMapping("/attendance")
    public String attendance(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate today = LocalDate.now();
        Attendance todayAttendance = attendanceRepository.findByUserAndDate(currentUser, today).orElse(null);
        boolean attendanceMarked = todayAttendance != null;
        
        List<Attendance> attendanceHistory = attendanceRepository.findByUserOrderByDateDesc(currentUser);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("attendanceMarked", attendanceMarked);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("attendanceHistory", attendanceHistory);
        
        return "engineer/attendance";
    }
    
    @PostMapping("/attendance/mark")
    public String markAttendance(Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        
        if (!attendanceRepository.existsByUserAndDate(currentUser, today)) {
            Attendance attendance = new Attendance();
            attendance.setUser(currentUser);
            attendance.setDate(today);
            attendance.setCheckIn(now);
            
            // Set status: LATE if after 9:30 AM, otherwise PRESENT
            java.time.LocalTime cutoffTime = java.time.LocalTime.of(9, 30);
            if (now.isAfter(cutoffTime)) {
                attendance.setStatus(Attendance.AttendanceStatus.LATE);
            } else {
                attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
            }
            
            attendance.setCreatedAt(LocalDateTime.now());
            attendanceRepository.save(attendance);
        }
        
        return "redirect:/engineer/attendance";
    }
    
    @PostMapping("/attendance/checkout")
    public String checkoutAttendance(Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        
        // Find today's attendance
        Attendance attendance = attendanceRepository.findByUserAndDate(currentUser, today)
                .orElseThrow(() -> new RuntimeException("No check-in record found for today"));
        
        // Mark checkout
        attendance.setCheckOut(now);
        attendance.setHoursWorked(attendance.calculateHoursWorked());
        attendance.setStatus(Attendance.AttendanceStatus.DEPARTED);
        attendance.setUpdatedAt(LocalDateTime.now());
        attendanceRepository.save(attendance);
        
        return "redirect:/engineer/attendance?checkout=true";
    }
}
