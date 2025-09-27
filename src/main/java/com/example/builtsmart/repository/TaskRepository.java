package com.example.builtsmart.repository;

import com.example.builtsmart.entity.Task;
import com.example.builtsmart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByProjectId(Long projectId);
    
    List<Task> findByAssignedTo(User assignedTo);
    
    List<Task> findByStatus(Task.TaskStatus status);
    
    List<Task> findByPriority(Task.TaskPriority priority);
    
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId AND t.status = :status")
    List<Task> findTasksByUserAndStatus(@Param("userId") Long userId, @Param("status") Task.TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.deadline <= :deadline")
    List<Task> findTasksByProjectAndDeadline(@Param("projectId") Long projectId, @Param("deadline") LocalDate deadline);
    
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId AND t.deadline < :today")
    List<Task> findOverdueTasksByUser(@Param("userId") Long userId, @Param("today") LocalDate today);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    long countTasksByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") Task.TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId ORDER BY t.deadline ASC")
    List<Task> findTasksByUserOrderByDeadline(@Param("userId") Long userId);
    
    @Query("SELECT t FROM Task t WHERE t.deadline BETWEEN :startDate AND :endDate ORDER BY t.deadline")
    List<Task> findTasksByDeadlineBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
} 