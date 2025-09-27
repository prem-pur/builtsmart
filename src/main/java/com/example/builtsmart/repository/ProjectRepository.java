package com.example.builtsmart.repository;

import com.example.builtsmart.entity.Project;
import com.example.builtsmart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    List<Project> findByStatus(Project.ProjectStatus status);
    
    List<Project> findByProjectManager(User projectManager);
    
    List<Project> findByClient(User client);
    
    @Query("SELECT p FROM Project p WHERE p.status = :status AND p.active = true")
    List<Project> findActiveProjectsByStatus(@Param("status") Project.ProjectStatus status);
    
    @Query("SELECT p FROM Project p WHERE p.startDate >= :startDate AND p.endDate <= :endDate")
    List<Project> findProjectsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT p FROM Project p WHERE p.location LIKE %:location%")
    List<Project> findProjectsByLocation(@Param("location") String location);
    
    @Query("SELECT COUNT(p) FROM Project p WHERE p.status = :status")
    long countProjectsByStatus(@Param("status") Project.ProjectStatus status);
    
    @Query("SELECT p FROM Project p WHERE p.projectManager.id = :managerId")
    List<Project> findProjectsByManagerId(@Param("managerId") Long managerId);
    
    @Query("SELECT p FROM Project p WHERE p.client.id = :clientId")
    List<Project> findProjectsByClientId(@Param("clientId") Long clientId);
} 