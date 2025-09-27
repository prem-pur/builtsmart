package com.example.builtsmart.service;

import com.example.builtsmart.entity.LeaveRequest;
import com.example.builtsmart.entity.User;
import com.example.builtsmart.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {
    
    private final LeaveRequestRepository leaveRequestRepository;
    
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }
    
    public Optional<LeaveRequest> getLeaveRequestById(Long id) {
        return leaveRequestRepository.findById(id);
    }
    
    public LeaveRequest saveLeaveRequest(LeaveRequest leaveRequest) {
        if (leaveRequest.getId() == null) {
            leaveRequest.setRequestDate(LocalDate.now());
            leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);
        }
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public List<LeaveRequest> getLeaveRequestsByEmployee(User employee) {
        return leaveRequestRepository.findByEmployee(employee);
    }
    
    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequestRepository.findPendingLeaveRequests();
    }
    
    public List<LeaveRequest> getLeaveRequestsByStatus(LeaveRequest.LeaveStatus status) {
        return leaveRequestRepository.findByStatus(status);
    }
    
    public LeaveRequest approveLeaveRequest(Long id, User approvedBy) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(approvedBy);
        leaveRequest.setApprovedDate(LocalDate.now());
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public LeaveRequest rejectLeaveRequest(Long id, User approvedBy, String reason) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(approvedBy);
        leaveRequest.setApprovedDate(LocalDate.now());
        leaveRequest.setRejectionReason(reason);
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public List<LeaveRequest> getOverlappingLeaves(LocalDate startDate, LocalDate endDate) {
        return leaveRequestRepository.findOverlappingLeaves(startDate, endDate);
    }
    
    public boolean hasOverlappingLeave(User employee, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingLeaves(startDate, endDate);
        return overlapping.stream().anyMatch(lr -> lr.getEmployee().equals(employee) 
                && lr.getStatus() == LeaveRequest.LeaveStatus.APPROVED);
    }
}
