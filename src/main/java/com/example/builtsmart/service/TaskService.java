package com.example.builtsmart.service;

import com.example.builtsmart.entity.Task;
import com.example.builtsmart.entity.User;
import com.example.builtsmart.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
    
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }
    
    public Task saveTask(Task task) {
        if (task.getId() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }
        return taskRepository.save(task);
    }
    
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }
    
    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }
    
    public List<Task> getTasksByUser(User user) {
        return taskRepository.findByAssignedTo(user);
    }
    
    public List<Task> getTasksByStatus(Task.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }
    
    public List<Task> getPendingTasks() {
        return taskRepository.findByStatus(Task.TaskStatus.PENDING);
    }
    
    public List<Task> getInProgressTasks() {
        return taskRepository.findByStatus(Task.TaskStatus.IN_PROGRESS);
    }
    
    public List<Task> getOverdueTasks(User user) {
        return taskRepository.findOverdueTasksByUser(user.getId(), LocalDate.now());
    }
    
    public List<Task> getUpcomingDeadlines(LocalDate startDate, LocalDate endDate) {
        return taskRepository.findTasksByDeadlineBetween(startDate, endDate);
    }
    
    public Task markTaskAsCompleted(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(Task.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }
    
    public Task updateTaskStatus(Long taskId, Task.TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(status);
        if (status == Task.TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }
        return taskRepository.save(task);
    }
}
