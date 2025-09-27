package com.example.builtsmart.repository;

import com.example.builtsmart.entity.Log;
import com.example.builtsmart.entity.Project;
import com.example.builtsmart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {
    
    List<Log> findByProject(Project project);
    
    List<Log> findBySubmittedBy(User submittedBy);
    
    List<Log> findByProjectAndType(Project project, Log.LogType type);
    
    List<Log> findByCreatedAtBetween(LocalDate startDate, LocalDate endDate);
    
    List<Log> findByProjectAndCreatedAtBetween(Project project, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT l FROM Log l WHERE l.project.id = :projectId ORDER BY l.createdAt DESC")
    List<Log> findLogsByProjectIdOrderByCreatedAtDesc(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(l) FROM Log l WHERE l.project.id = :projectId")
    long countLogsByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT l FROM Log l WHERE l.type = :type ORDER BY l.createdAt DESC")
    List<Log> findByTypeOrderByCreatedAtDesc(@Param("type") Log.LogType type);
    
    List<Log> findTop5ByOrderByCreatedAtDesc();
} 