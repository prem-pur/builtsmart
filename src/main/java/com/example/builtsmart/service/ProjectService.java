package com.example.builtsmart.service;

import com.example.builtsmart.entity.Project;
import com.example.builtsmart.entity.User;
import com.example.builtsmart.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }
    
    public List<Project> getProjectsByManager(User projectManager) {
        return projectRepository.findByProjectManager(projectManager);
    }
    
    public List<Project> getProjectsByStatus(Project.ProjectStatus status) {
        return projectRepository.findByStatus(status);
    }
    
    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }
    
    public Project saveProject(Project project) {
        if (project.getCreatedAt() == null) {
            project.setCreatedAt(LocalDate.now());
        }
        if (project.getActive() == null) {
            project.setActive(true);
        }
        return projectRepository.save(project);
    }
    
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
    
    public List<Project> getActiveProjects() {
        return projectRepository.findByStatus(Project.ProjectStatus.IN_PROGRESS);
    }
    
    public long countProjectsByStatus(Project.ProjectStatus status) {
        return projectRepository.countProjectsByStatus(status);
    }
    
    public List<Project> getProjectsByLocation(String location) {
        return projectRepository.findProjectsByLocation(location);
    }
    
    public List<Project> getProjectsByDateRange(LocalDate startDate, LocalDate endDate) {
        return projectRepository.findProjectsByDateRange(startDate, endDate);
    }
} 