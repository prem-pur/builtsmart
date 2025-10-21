package com.example.builtsmart.controller;

import com.example.builtsmart.entity.*;
import com.example.builtsmart.repository.*;
import com.example.builtsmart.service.ProjectService;
import com.example.builtsmart.service.TaskService;
import com.example.builtsmart.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ProjectManagerController {
    
    private final UserService userService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final LogRepository logRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        try {
            User currentUser = userService.getUserByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            model.addAttribute("user", currentUser);
            
            // Dashboard statistics with error handling
            List<Project> activeProjects = projectRepository.findByStatus(Project.ProjectStatus.IN_PROGRESS);
            List<Task> pendingTasks = taskRepository.findByStatus(Task.TaskStatus.PENDING);
            List<Log> recentLogs = logRepository.findTop5ByOrderByCreatedAtDesc();
            List<User> totalWorkers = userRepository.findByRole(User.UserRole.WORKER);
            
            // Count active workers (workers with recent attendance) - with fallback
            long activeWorkers = 0;
            try {
                activeWorkers = attendanceRepository.findByDate(LocalDate.now()).size();
            } catch (Exception e) {
                // Fallback to 0 if attendance data is not available
                activeWorkers = 0;
            }
            
            model.addAttribute("activeProjects", activeProjects);
            model.addAttribute("activeProjectsCount", activeProjects.size());
            model.addAttribute("pendingTasks", pendingTasks);
            model.addAttribute("pendingTasksCount", pendingTasks.size());
            model.addAttribute("totalWorkers", totalWorkers.size());
            model.addAttribute("activeWorkers", activeWorkers);
            model.addAttribute("recentLogs", recentLogs);
            model.addAttribute("recentLogsCount", recentLogs.size());
            
            // Project status distribution for chart
            Map<Project.ProjectStatus, Long> projectStatusCount = projectRepository.findAll().stream()
                    .collect(Collectors.groupingBy(Project::getStatus, Collectors.counting()));
            
            // Create individual counts for easier template access
            model.addAttribute("completedCount", projectStatusCount.getOrDefault(Project.ProjectStatus.COMPLETED, 0L));
            model.addAttribute("inProgressCount", projectStatusCount.getOrDefault(Project.ProjectStatus.IN_PROGRESS, 0L));
            model.addAttribute("planningCount", projectStatusCount.getOrDefault(Project.ProjectStatus.PLANNING, 0L));
            model.addAttribute("onHoldCount", projectStatusCount.getOrDefault(Project.ProjectStatus.ON_HOLD, 0L));
            model.addAttribute("cancelledCount", projectStatusCount.getOrDefault(Project.ProjectStatus.CANCELLED, 0L));
            
            model.addAttribute("projectStatusCount", projectStatusCount);
            
            // Upcoming deadlines (tasks due in next 7 days) - with fallback
            List<Task> upcomingDeadlines;
            try {
                LocalDate weekFromNow = LocalDate.now().plusDays(7);
                upcomingDeadlines = taskRepository.findTasksByDeadlineBetween(LocalDate.now(), weekFromNow);
            } catch (Exception e) {
                upcomingDeadlines = java.util.Collections.emptyList();
            }
            model.addAttribute("upcomingDeadlines", upcomingDeadlines);
            
            // Recent issues (logs with ISSUE_REPORT type) - with fallback
            List<Log> recentIssues;
            try {
                recentIssues = logRepository.findByTypeOrderByCreatedAtDesc(Log.LogType.ISSUE_REPORT);
            } catch (Exception e) {
                recentIssues = java.util.Collections.emptyList();
            }
            model.addAttribute("recentIssues", recentIssues);

            // Projects widget: show up to 3 most recently updated projects based on recent logs
            java.util.List<Project> recentProjects = recentLogs.stream()
                    .map(Log::getProject)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .limit(3)
                    .collect(java.util.stream.Collectors.toList());
            if (recentProjects.isEmpty()) {
                // Fallback: first 3 projects
                recentProjects = projectRepository.findAll().stream().limit(3).collect(java.util.stream.Collectors.toList());
            }
            model.addAttribute("recentProjects", recentProjects);

            return "dashboard/manager";
        } catch (Exception e) {
            // Log the error and redirect to a safe page
            model.addAttribute("errorMessage", "Dashboard temporarily unavailable: " + e.getMessage());
            return "error/error";
        }
    }
    
    @GetMapping("/projects")
    public String projects(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // For now, show all projects instead of filtering by manager
        // This ensures sample data is visible
        List<Project> projects = projectService.getAllProjects();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("projects", projects);
        
        return "manager/projects";
    }
    
    @GetMapping("/projects/new")
    public String newProject(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<User> clients = userRepository.findByRole(User.UserRole.CLIENT_REPRESENTATIVE);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("project", new Project());
        model.addAttribute("clients", clients);
        model.addAttribute("projectStatuses", Project.ProjectStatus.values());
        
        return "manager/project-form";
    }
    
    @PostMapping("/projects")
    public String createProject(@ModelAttribute Project project, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate end date is after start date
        if (project.getEndDate() != null && project.getStartDate() != null) {
            if (project.getEndDate().isBefore(project.getStartDate())) {
                model.addAttribute("error", "End date must be after start date");
                model.addAttribute("user", currentUser);
                model.addAttribute("project", project);
                model.addAttribute("clients", userRepository.findByRole(User.UserRole.CLIENT_REPRESENTATIVE));
                model.addAttribute("projectStatuses", Project.ProjectStatus.values());
                return "manager/project-form";
            }
        }
        
        // Validate budget is not negative
        if (project.getTotalBudget() != null && project.getTotalBudget().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "Budget cannot be negative");
            model.addAttribute("user", currentUser);
            model.addAttribute("project", project);
            model.addAttribute("clients", userRepository.findByRole(User.UserRole.CLIENT_REPRESENTATIVE));
            model.addAttribute("projectStatuses", Project.ProjectStatus.values());
            return "manager/project-form";
        }
        
        project.setProjectManager(currentUser);
        project.setActive(true);
        
        projectService.saveProject(project);
        
        return "redirect:/manager/projects";
    }

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id, Authentication authentication) {
        // Optional: add ownership/role checks here if needed
        projectService.deleteProject(id);
        return "redirect:/manager/projects";
    }
    
    @PostMapping("/projects/{id}/reactivate")
    public String reactivateProject(@PathVariable Long id, Authentication authentication) {
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Reactivate the project
        project.setActive(true);
        projectService.saveProject(project);
        
        return "redirect:/manager/projects?reactivated=true";
    }
    
    @GetMapping("/projects/{id}")
    public String viewProject(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Get project team members (users assigned to project tasks)
        List<Task> projectTasks = taskRepository.findByProjectId(id);
        List<User> teamMembers = projectTasks.stream()
                .map(Task::getAssignedTo)
                .filter(user -> user != null)
                .distinct()
                .collect(Collectors.toList());
        
        model.addAttribute("user", currentUser);
        model.addAttribute("project", project);
        model.addAttribute("teamMembers", teamMembers);
        
        return "manager/project-detail";
    }
    
    @GetMapping("/projects/{id}/edit")
    public String editProject(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        List<User> clients = userRepository.findByRole(User.UserRole.CLIENT_REPRESENTATIVE);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("project", project);
        model.addAttribute("clients", clients);
        model.addAttribute("projectStatuses", Project.ProjectStatus.values());
        
        return "manager/project-form";
    }
    
    @PostMapping("/projects/{id}")
    public String updateProject(@PathVariable Long id, @ModelAttribute Project project, 
                              Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Project existingProject = projectService.getProjectById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Validate end date is after start date
        if (project.getEndDate() != null && project.getStartDate() != null) {
            if (project.getEndDate().isBefore(project.getStartDate())) {
                model.addAttribute("error", "End date must be after start date");
                model.addAttribute("user", currentUser);
                model.addAttribute("project", existingProject);
                model.addAttribute("clients", userRepository.findByRole(User.UserRole.CLIENT_REPRESENTATIVE));
                model.addAttribute("projectStatuses", Project.ProjectStatus.values());
                return "manager/project-form";
            }
        }
        
        // Validate budget is not negative
        if (project.getTotalBudget() != null && project.getTotalBudget().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "Budget cannot be negative");
            model.addAttribute("user", currentUser);
            model.addAttribute("project", existingProject);
            model.addAttribute("clients", userRepository.findByRole(User.UserRole.CLIENT_REPRESENTATIVE));
            model.addAttribute("projectStatuses", Project.ProjectStatus.values());
            return "manager/project-form";
        }
        
        // Update fields
        existingProject.setName(project.getName());
        existingProject.setDescription(project.getDescription());
        existingProject.setLocation(project.getLocation());
        existingProject.setStartDate(project.getStartDate());
        existingProject.setEndDate(project.getEndDate());
        existingProject.setStatus(project.getStatus());
        existingProject.setTotalBudget(project.getTotalBudget());
        existingProject.setClient(project.getClient());
        
        projectService.saveProject(existingProject);
        
        return "redirect:/manager/projects";
    }
    
    @GetMapping("/tasks")
    public String tasks(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Task> allTasks = taskRepository.findAll();
        List<Task> pendingTasks = taskRepository.findByStatus(Task.TaskStatus.PENDING);
        List<Task> inProgressTasks = taskRepository.findByStatus(Task.TaskStatus.IN_PROGRESS);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("allTasks", allTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("inProgressTasks", inProgressTasks);
        
        return "manager/tasks";
    }
    
    @GetMapping("/workers")
    public String workers(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<User> workers = userRepository.findByRole(User.UserRole.WORKER);
        List<User> engineers = userRepository.findByRole(User.UserRole.SITE_ENGINEER);
        List<User> allUsers = userRepository.findAll();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("workers", workers);
        model.addAttribute("engineers", engineers);
        model.addAttribute("allUsers", allUsers);
        
        return "manager/workers";
    }

    @GetMapping("/workers/{id}")
    public String viewWorker(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        User worker = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Tasks assigned to this user
        List<Task> assignedTasks = taskRepository.findByAssignedTo(worker);

        model.addAttribute("user", currentUser);
        model.addAttribute("worker", worker);
        model.addAttribute("assignedTasks", assignedTasks);

        return "manager/worker-detail";
    }
    
    // User Management Functions
    @GetMapping("/users")
    public String users(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<User> allUsers = userRepository.findAll();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("allUsers", allUsers);
        
        return "manager/users";
    }
    
    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        User userToEdit = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", currentUser);
        model.addAttribute("userToEdit", userToEdit);
        model.addAttribute("userRoles", User.UserRole.values());
        
        return "manager/user-form";
    }
    
    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute("userToEdit") User userToUpdate, Authentication authentication) {
        User existingUser = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        existingUser.setName(userToUpdate.getName());
        existingUser.setEmail(userToUpdate.getEmail());
        existingUser.setRole(userToUpdate.getRole());
        existingUser.setPhone(userToUpdate.getPhone());
        existingUser.setAddress(userToUpdate.getAddress());
        existingUser.setDepartment(userToUpdate.getDepartment());
        existingUser.setActive(userToUpdate.isActive());
        
        userService.updateUser(existingUser);
        return "redirect:/manager/users";
    }
    
    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setActive(!user.isActive());
        userService.updateUser(user);
        
        return "redirect:/manager/users";
    }
    
    
    
    @GetMapping("/tasks/new")
    public String newTask(@RequestParam(required = false) Long projectId, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Task task = new Task();
        if (projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null) {
                task.setProject(project);
            }
        }
        
        List<Project> projects = projectService.getProjectsByManager(currentUser);
        List<User> workers = userRepository.findByRole(User.UserRole.WORKER);
        List<User> engineers = userRepository.findByRole(User.UserRole.SITE_ENGINEER);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("task", task);
        model.addAttribute("projects", projects);
        model.addAttribute("workers", workers);
        model.addAttribute("engineers", engineers);
        model.addAttribute("taskStatuses", Task.TaskStatus.values());
        model.addAttribute("selectedProjectId", projectId);
        model.addAttribute("taskPriorities", Task.TaskPriority.values());
        
        return "manager/task-form";
    }
    
    @PostMapping("/tasks")
    public String createTask(@ModelAttribute Task task, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate deadline is not in the past
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDate.now())) {
            model.addAttribute("error", "Deadline cannot be in the past");
            model.addAttribute("user", currentUser);
            model.addAttribute("task", task);
            model.addAttribute("projects", projectService.getProjectsByManager(currentUser));
            model.addAttribute("workers", userRepository.findByRole(User.UserRole.WORKER));
            model.addAttribute("engineers", userRepository.findByRole(User.UserRole.SITE_ENGINEER));
            model.addAttribute("taskStatuses", Task.TaskStatus.values());
            model.addAttribute("taskPriorities", Task.TaskPriority.values());
            return "manager/task-form";
        }
        
        task.setAssignedBy(currentUser);
        taskService.saveTask(task);
        
        return "redirect:/manager/dashboard";
    }
    
    @GetMapping("/tasks/{id}/edit")
    public String editTask(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Task task = taskService.getTaskById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        List<Project> projects = projectService.getProjectsByManager(currentUser);
        List<User> workers = userRepository.findByRole(User.UserRole.WORKER);
        List<User> engineers = userRepository.findByRole(User.UserRole.SITE_ENGINEER);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("task", task);
        model.addAttribute("projects", projects);
        model.addAttribute("workers", workers);
        model.addAttribute("engineers", engineers);
        model.addAttribute("taskStatuses", Task.TaskStatus.values());
        model.addAttribute("taskPriorities", Task.TaskPriority.values());
        
        return "manager/task-form";
    }
    
    @PostMapping("/tasks/{id}")
    public String updateTask(@PathVariable Long id, 
                           @ModelAttribute("task") Task taskDetails,
                           @RequestParam(value = "project.id", required = false) Long projectId,
                           @RequestParam(value = "assignedTo.id", required = false) Long assignedToId,
                           Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Task existingTask = taskService.getTaskById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // Update task fields
        existingTask.setDescription(taskDetails.getDescription());
        existingTask.setDeadline(taskDetails.getDeadline());
        existingTask.setStatus(taskDetails.getStatus());
        existingTask.setPriority(taskDetails.getPriority());
        existingTask.setNotes(taskDetails.getNotes());
        
        // Handle project assignment
        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            existingTask.setProject(project);
        }
        
        // Handle assignee
        if (assignedToId != null) {
            User assignee = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            existingTask.setAssignedTo(assignee);
        } else {
            existingTask.setAssignedTo(null);
        }
        
        // If task is being marked as completed, set the completedAt timestamp
        if (existingTask.getStatus() == Task.TaskStatus.COMPLETED && existingTask.getCompletedAt() == null) {
            existingTask.setCompletedAt(LocalDateTime.now());
        } else if (existingTask.getStatus() != Task.TaskStatus.COMPLETED) {
            existingTask.setCompletedAt(null);
        }
        
        taskService.saveTask(existingTask);
        
        return "redirect:/manager/tasks";
    }
    
    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, Authentication authentication) {
        taskService.deleteTask(id);
        return "redirect:/manager/tasks";
    }
    
    // Log Management Functions
    @GetMapping("/projects/logs/new")
    public String newProjectLog(@RequestParam(required = false) Long projectId, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Log log = new Log();
        if (projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null) {
                log.setProject(project);
            }
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("log", log);
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("logTypes", Log.LogType.values());
        model.addAttribute("selectedProjectId", projectId);
        
        return "manager/log-form";
    }
    
    @PostMapping("/projects/logs")
    public String createProjectLog(@ModelAttribute Log log, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        log.setSubmittedBy(currentUser);
        log.setCreatedAt(LocalDateTime.now());
        logRepository.save(log);
        
        if (log.getProject() != null) {
            return "redirect:/manager/projects/" + log.getProject().getId();
        }
        return "redirect:/manager/dashboard";
    }
    
    // Expense Management Functions
    @GetMapping("/projects/expenses/new")
    public String newProjectExpense(@RequestParam(required = false) Long projectId, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Expense expense = new Expense();
        if (projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null) {
                expense.setProject(project);
            }
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("expense", expense);
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("expenseCategories", Expense.ExpenseCategory.values());
        model.addAttribute("selectedProjectId", projectId);
        
        return "manager/expense-form";
    }
    
    @PostMapping("/projects/expenses")
    public String createProjectExpense(@ModelAttribute Expense expense, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate amount is not negative
        if (expense.getAmount() != null && expense.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "Expense amount cannot be negative");
            model.addAttribute("user", currentUser);
            model.addAttribute("expense", expense);
            model.addAttribute("projects", projectRepository.findAll());
            model.addAttribute("expenseCategories", Expense.ExpenseCategory.values());
            return "manager/expense-form";
        }
        
        expense.setSubmittedBy(currentUser);
        expense.setCreatedAt(LocalDateTime.now());
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }
        expenseRepository.save(expense);
        
        if (expense.getProject() != null) {
            return "redirect:/manager/projects/" + expense.getProject().getId();
        }
        return "redirect:/manager/dashboard";
    }
    
    
    
    // Profile Management
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", currentUser);
        return "manager/profile";
    }
    
    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User user, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update only allowed fields
        currentUser.setName(user.getName());
        currentUser.setPhone(user.getPhone());
        userRepository.save(currentUser);
        
        return "redirect:/manager/profile?success";
    }
    
    @GetMapping("/tasks/{id}")
    public String viewTask(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Task task = taskService.getTaskById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        model.addAttribute("user", currentUser);
        model.addAttribute("task", task);
        
        return "manager/task-detail";
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
        
        return "manager/leave";
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
        
        return "redirect:/manager/leave?success=true";
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
        
        return "redirect:/manager/leave";
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
        model.addAttribute("currentTime", java.time.LocalTime.now());
        
        return "manager/attendance";
    }
    
    @PostMapping("/attendance/mark")
    public String markAttendance(Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        
        // Check if already marked
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
        
        return "redirect:/manager/attendance";
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
        
        return "redirect:/manager/attendance?checkout=true";
    }
}