package com.example.builtsmart.controller;

import com.example.builtsmart.entity.*;
import com.example.builtsmart.repository.*;
import com.example.builtsmart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final LogRepository logRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;

    @GetMapping("/manager-stats")
    public Map<String, Object> getManagerStats(Authentication authentication) {
        Map<String, Object> stats = new HashMap<>();
        
        // Dashboard statistics
        List<Project> activeProjects = projectRepository.findByStatus(Project.ProjectStatus.IN_PROGRESS);
        List<Task> pendingTasks = taskRepository.findByStatus(Task.TaskStatus.PENDING);
        List<Log> recentLogs = logRepository.findTop5ByOrderByCreatedAtDesc();
        List<User> totalWorkers = userRepository.findByRole(User.UserRole.WORKER);
        
        // Count active workers (workers with recent attendance)
        long activeWorkers = attendanceRepository.findByDate(LocalDate.now()).size();
        
        stats.put("activeProjectsCount", activeProjects.size());
        stats.put("pendingTasksCount", pendingTasks.size());
        stats.put("totalWorkers", totalWorkers.size());
        stats.put("activeWorkers", activeWorkers);
        stats.put("recentLogsCount", recentLogs.size());

        // Project status distribution for charts (live)
        long completedCount = projectRepository.findByStatus(Project.ProjectStatus.COMPLETED).size();
        long inProgressCount = projectRepository.findByStatus(Project.ProjectStatus.IN_PROGRESS).size();
        long planningCount = projectRepository.findByStatus(Project.ProjectStatus.PLANNING).size();
        long onHoldCount = projectRepository.findByStatus(Project.ProjectStatus.ON_HOLD).size();
        long cancelledCount = projectRepository.findByStatus(Project.ProjectStatus.CANCELLED).size();

        stats.put("completedCount", completedCount);
        stats.put("inProgressCount", inProgressCount);
        stats.put("planningCount", planningCount);
        stats.put("onHoldCount", onHoldCount);
        stats.put("cancelledCount", cancelledCount);
        
        return stats;
    }
}
