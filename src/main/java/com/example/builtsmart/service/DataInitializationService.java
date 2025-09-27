package com.example.builtsmart.service;

import com.example.builtsmart.entity.User;
import com.example.builtsmart.entity.Project;
import com.example.builtsmart.entity.Task;
import com.example.builtsmart.entity.Log;
import com.example.builtsmart.repository.UserRepository;
import com.example.builtsmart.repository.ProjectRepository;
import com.example.builtsmart.repository.TaskRepository;
import com.example.builtsmart.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final LogRepository logRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Initializing database with sample data...");
            initializeUsers();
            initializeProjects();
            initializeTasks();
            initializeLogs();
            log.info("Database initialization completed!");
        }
    }
    
    private void initializeUsers() {
        List<User> users = Arrays.asList(
            // Project Manager - Mr. Nuwan Perera
            createUser("Nuwan Perera", "nuwan@builtsmart.lk", "manager123", 
                     User.UserRole.PROJECT_MANAGER, "+94 11 234 5678", "Colombo", "Project Management"),
            
            // Site Engineer - Ms. Shanika Jayawardena
            createUser("Shanika Jayawardena", "shanika@builtsmart.lk", "engineer123", 
                     User.UserRole.SITE_ENGINEER, "+94 11 234 5679", "Kandy", "Engineering"),
            
            // HR Executive - Mr. Chamika Silva
            createUser("Chamika Silva", "chamika@builtsmart.lk", "hr123", 
                     User.UserRole.HR_EXECUTIVE, "+94 11 234 5680", "Colombo", "Human Resources"),
            
            // Finance Officer - Ms. Tharushi Fernando
            createUser("Tharushi Fernando", "tharushi@builtsmart.lk", "finance123", 
                     User.UserRole.FINANCE_OFFICER, "+94 11 234 5681", "Colombo", "Finance"),
            
            // Client Representative - Mr. Ramesh Dissanayake
            createUser("Ramesh Dissanayake", "ramesh@client.com", "client123", 
                     User.UserRole.CLIENT_REPRESENTATIVE, "+94 11 234 5682", "Colombo", "Client"),
            
            // Worker
            createUser("Worker", "worker@builtsmart.lk", "worker123", 
                     User.UserRole.WORKER, "+94 11 234 5683", "Colombo", "Construction")
        );
        
        userRepository.saveAll(users);
        log.info("Created {} sample users", users.size());
    }
    
    private void initializeProjects() {
        User projectManager = userRepository.findByEmail("nuwan@builtsmart.lk").orElse(null);
        User client = userRepository.findByEmail("ramesh@client.com").orElse(null);
        
        if (projectManager != null && client != null) {
            List<Project> projects = Arrays.asList(
                createProject("Colombo Office Complex", "Modern office building in Colombo", "Colombo", 
                            LocalDate.now().minusDays(30), LocalDate.now().plusDays(90), 
                            Project.ProjectStatus.IN_PROGRESS, projectManager, client),
                createProject("Kandy Residential Tower", "Luxury residential complex", "Kandy", 
                            LocalDate.now().minusDays(15), LocalDate.now().plusDays(120), 
                            Project.ProjectStatus.IN_PROGRESS, projectManager, client),
                createProject("Galle Shopping Mall", "Modern shopping center", "Galle", 
                            LocalDate.now().plusDays(10), LocalDate.now().plusDays(180), 
                            Project.ProjectStatus.PLANNING, projectManager, client)
            );
            
            projectRepository.saveAll(projects);
            log.info("Created {} sample projects", projects.size());
        }
    }
    
    private void initializeTasks() {
        List<Project> projects = projectRepository.findAll();
        List<User> workers = userRepository.findByRole(User.UserRole.WORKER);
        
        if (!projects.isEmpty() && !workers.isEmpty()) {
            List<Task> tasks = Arrays.asList(
                createTask("Foundation work", projects.get(0), workers.get(0), 
                          LocalDate.now().plusDays(5), Task.TaskStatus.IN_PROGRESS),
                createTask("Electrical installation", projects.get(1), workers.get(0), 
                          LocalDate.now().plusDays(3), Task.TaskStatus.PENDING),
                createTask("Plumbing work", projects.get(0), workers.get(0), 
                          LocalDate.now().plusDays(7), Task.TaskStatus.PENDING)
            );
            
            taskRepository.saveAll(tasks);
            log.info("Created {} sample tasks", tasks.size());
        }
    }
    
    private void initializeLogs() {
        List<Project> projects = projectRepository.findAll();
        User siteEngineer = userRepository.findByEmail("shanika@builtsmart.lk").orElse(null);
        
        if (!projects.isEmpty() && siteEngineer != null) {
            List<Log> logs = Arrays.asList(
                createLog(projects.get(0), siteEngineer, "Foundation work completed successfully", 
                         Log.LogType.PROGRESS_UPDATE),
                createLog(projects.get(1), siteEngineer, "Steel and cement delivered to site", 
                         Log.LogType.MATERIAL_DELIVERY),
                createLog(projects.get(0), siteEngineer, "Heavy rain causing delays", 
                         Log.LogType.ISSUE_REPORT)
            );
            
            logRepository.saveAll(logs);
            log.info("Created {} sample logs", logs.size());
        }
    }
    
    private Project createProject(String name, String description, String location, 
                                LocalDate startDate, LocalDate endDate, Project.ProjectStatus status, 
                                User projectManager, User client) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setLocation(location);
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setStatus(status);
        project.setProjectManager(projectManager);
        project.setClient(client);
        project.setActive(true);
        return project;
    }
    
    private Task createTask(String description, Project project, User assignedTo, 
                           LocalDate deadline, Task.TaskStatus status) {
        Task task = new Task();
        task.setDescription(description);
        task.setProject(project);
        task.setAssignedTo(assignedTo);
        task.setAssignedBy(project.getProjectManager());
        task.setDeadline(deadline);
        task.setStatus(status);
        task.setPriority(Task.TaskPriority.MEDIUM);
        return task;
    }
    
    private Log createLog(Project project, User submittedBy, String description, Log.LogType type) {
        Log log = new Log();
        log.setProject(project);
        log.setSubmittedBy(submittedBy);
        log.setDescription(description);
        log.setType(type);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
    
    private User createUser(String name, String email, String password, 
                           User.UserRole role, String phone, String address, String department) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setPhone(phone);
        user.setAddress(address);
        user.setDepartment(department);
        user.setActive(true);
        return user;
    }
} 