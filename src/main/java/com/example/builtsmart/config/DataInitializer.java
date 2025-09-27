package com.example.builtsmart.config;

import com.example.builtsmart.entity.*;
import com.example.builtsmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final LogRepository logRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Only initialize if no projects exist (to avoid duplicates)
        if (projectRepository.count() == 0) {
            initializeUsers();
            initializeProjects();
            initializeTasks();
            initializeLogs();
        }
    }

    private void initializeUsers() {
        // Check if manager already exists, if not create one
        User manager = userRepository.findByEmail("nuwan@builtsmart.lk").orElse(null);
        if (manager == null) {
            manager = new User();
            manager.setName("Nuwan Silva");
            manager.setEmail("nuwan@builtsmart.lk");
            manager.setPassword(passwordEncoder.encode("manager123"));
            manager.setRole(User.UserRole.PROJECT_MANAGER);
            manager.setPhone("+94771234567");
            manager.setAddress("Colombo, Sri Lanka");
            manager.setDepartment("Construction Management");
            manager.setActive(true);
            userRepository.save(manager);
        }

        // Create client
        User client = new User();
        client.setName("ABC Construction Ltd");
        client.setEmail("client@abc.lk");
        client.setPassword(passwordEncoder.encode("client123"));
        client.setRole(User.UserRole.CLIENT_REPRESENTATIVE);
        client.setPhone("+94112345678");
        client.setAddress("Kandy, Sri Lanka");
        client.setDepartment("Client Relations");
        client.setActive(true);
        userRepository.save(client);

        // Create workers
        for (int i = 1; i <= 5; i++) {
            User worker = new User();
            worker.setName("Worker " + i);
            worker.setEmail("worker" + i + "@buildsmart.lk");
            worker.setPassword(passwordEncoder.encode("worker123"));
            worker.setRole(User.UserRole.WORKER);
            worker.setPhone("+9477123456" + i);
            worker.setAddress("Site Location " + i);
            worker.setDepartment("Construction");
            worker.setActive(true);
            userRepository.save(worker);
        }
    }

    private void initializeProjects() {
        User manager = userRepository.findByEmail("nuwan@builtsmart.lk").orElse(null);
        User client = userRepository.findByEmail("client@abc.lk").orElse(null);

        // If manager doesn't exist, use any existing project manager
        if (manager == null) {
            manager = userRepository.findByRole(User.UserRole.PROJECT_MANAGER)
                    .stream().findFirst().orElse(null);
        }
        
        if (manager != null && client != null) {
            // Project 1 - Completed
            Project project1 = new Project();
            project1.setName("Residential Complex Phase 1");
            project1.setDescription("Construction of 50 residential units");
            project1.setLocation("Colombo 07");
            project1.setStartDate(LocalDate.now().minusMonths(12));
            project1.setEndDate(LocalDate.now().minusMonths(1));
            project1.setStatus(Project.ProjectStatus.COMPLETED);
            project1.setTotalBudget(new BigDecimal("15000000"));
            project1.setProjectManager(manager);
            project1.setClient(client);
            project1.setActive(true);
            projectRepository.save(project1);

            // Project 2 - In Progress
            Project project2 = new Project();
            project2.setName("Commercial Building Tower");
            project2.setDescription("15-story commercial building construction");
            project2.setLocation("Colombo 03");
            project2.setStartDate(LocalDate.now().minusMonths(6));
            project2.setEndDate(LocalDate.now().plusMonths(8));
            project2.setStatus(Project.ProjectStatus.IN_PROGRESS);
            project2.setTotalBudget(new BigDecimal("25000000"));
            project2.setProjectManager(manager);
            project2.setClient(client);
            project2.setActive(true);
            projectRepository.save(project2);

            // Project 3 - Planning
            Project project3 = new Project();
            project3.setName("Shopping Mall Development");
            project3.setDescription("Large scale shopping mall with parking");
            project3.setLocation("Kandy");
            project3.setStartDate(LocalDate.now().plusMonths(2));
            project3.setEndDate(LocalDate.now().plusMonths(18));
            project3.setStatus(Project.ProjectStatus.PLANNING);
            project3.setTotalBudget(new BigDecimal("35000000"));
            project3.setProjectManager(manager);
            project3.setClient(client);
            project3.setActive(true);
            projectRepository.save(project3);

            // Project 4 - On Hold
            Project project4 = new Project();
            project4.setName("Bridge Construction");
            project4.setDescription("Highway bridge construction project");
            project4.setLocation("Galle");
            project4.setStartDate(LocalDate.now().minusMonths(3));
            project4.setEndDate(LocalDate.now().plusMonths(12));
            project4.setStatus(Project.ProjectStatus.ON_HOLD);
            project4.setTotalBudget(new BigDecimal("12000000"));
            project4.setProjectManager(manager);
            project4.setClient(client);
            project4.setActive(true);
            projectRepository.save(project4);
        }
    }

    private void initializeTasks() {
        Project inProgressProject = projectRepository.findByStatus(Project.ProjectStatus.IN_PROGRESS)
                .stream().findFirst().orElse(null);
        User worker = userRepository.findByRole(User.UserRole.WORKER)
                .stream().findFirst().orElse(null);
        User manager = userRepository.findByRole(User.UserRole.PROJECT_MANAGER)
                .stream().findFirst().orElse(null);

        if (inProgressProject != null && worker != null && manager != null) {
            // Task 1 - Pending
            Task task1 = new Task();
            task1.setDescription("Foundation excavation work");
            task1.setProject(inProgressProject);
            task1.setAssignedTo(worker);
            task1.setAssignedBy(manager);
            task1.setDeadline(LocalDate.now().plusDays(7));
            task1.setStatus(Task.TaskStatus.PENDING);
            task1.setPriority(Task.TaskPriority.HIGH);
            task1.setNotes("Ensure proper safety measures");
            taskRepository.save(task1);

            // Task 2 - In Progress
            Task task2 = new Task();
            task2.setDescription("Steel reinforcement installation");
            task2.setProject(inProgressProject);
            task2.setAssignedTo(worker);
            task2.setAssignedBy(manager);
            task2.setDeadline(LocalDate.now().plusDays(14));
            task2.setStatus(Task.TaskStatus.IN_PROGRESS);
            task2.setPriority(Task.TaskPriority.MEDIUM);
            task2.setNotes("Follow structural drawings");
            taskRepository.save(task2);

            // Task 3 - Completed
            Task task3 = new Task();
            task3.setDescription("Site preparation and clearing");
            task3.setProject(inProgressProject);
            task3.setAssignedTo(worker);
            task3.setAssignedBy(manager);
            task3.setDeadline(LocalDate.now().minusDays(5));
            task3.setStatus(Task.TaskStatus.COMPLETED);
            task3.setPriority(Task.TaskPriority.HIGH);
            task3.setNotes("Completed ahead of schedule");
            taskRepository.save(task3);
        }
    }

    private void initializeLogs() {
        Project project = projectRepository.findByStatus(Project.ProjectStatus.IN_PROGRESS)
                .stream().findFirst().orElse(null);
        User manager = userRepository.findByRole(User.UserRole.PROJECT_MANAGER)
                .stream().findFirst().orElse(null);

        if (project != null && manager != null) {
            // Progress Update Log
            Log log1 = new Log();
            log1.setProject(project);
            log1.setSubmittedBy(manager);
            log1.setDescription("Foundation work completed successfully. Moving to next phase.");
            log1.setType(Log.LogType.PROGRESS_UPDATE);
            log1.setCreatedAt(LocalDateTime.now().minusDays(2));
            log1.setLocation("Site Office");
            log1.setWorkerCount(15);
            log1.setHoursWorked(8);
            logRepository.save(log1);

            // Issue Report Log
            Log log2 = new Log();
            log2.setProject(project);
            log2.setSubmittedBy(manager);
            log2.setDescription("Delay in material delivery affecting schedule");
            log2.setType(Log.LogType.ISSUE_REPORT);
            log2.setCreatedAt(LocalDateTime.now().minusDays(1));
            log2.setLocation("Site Office");
            logRepository.save(log2);

            // Material Delivery Log
            Log log3 = new Log();
            log3.setProject(project);
            log3.setSubmittedBy(manager);
            log3.setDescription("Steel reinforcement bars delivered and inspected");
            log3.setType(Log.LogType.MATERIAL_DELIVERY);
            log3.setCreatedAt(LocalDateTime.now());
            log3.setLocation("Site Storage");
            logRepository.save(log3);
        }
    }
}
