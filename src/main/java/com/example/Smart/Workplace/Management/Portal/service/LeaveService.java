package com.example.Smart.Workplace.Management.Portal.service;

import com.example.Smart.Workplace.Management.Portal.dto.LeaveRequestDto;
import com.example.Smart.Workplace.Management.Portal.model.LeaveRequest;
import com.example.Smart.Workplace.Management.Portal.model.LeaveStatus;
import com.example.Smart.Workplace.Management.Portal.model.Role;
import com.example.Smart.Workplace.Management.Portal.model.User;
import com.example.Smart.Workplace.Management.Portal.repository.LeaveRequestRepository;
import com.example.Smart.Workplace.Management.Portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing leave requests
 * Handles business logic for leave submission, approval, and rejection
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;

    /**
     * Submit a new leave request
     *
     * @param dto Leave request details
     * @param username Email of the employee submitting the request
     * @return Created leave request DTO
     * @throws UsernameNotFoundException if user not found
     * @throws IllegalArgumentException if date validation fails
     */
    @Transactional
    public LeaveRequestDto submitLeaveRequest(LeaveRequestDto dto, String username) {
        log.info("Submitting leave request for user: {}", username);

        // Validate dates
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            log.error("Invalid date range: end date {} is before start date {}",
                    dto.getEndDate(), dto.getStartDate());
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }

        // Find employee
        User employee = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        // Create leave request entity
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);

        // Save and return
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request created successfully with ID: {}", savedRequest.getId());

        return mapToDto(savedRequest);
    }

    /**
     * Get all leave requests for the authenticated employee
     *
     * @param username Email of the employee
     * @return List of leave request DTOs
     * @throws UsernameNotFoundException if user not found
     */
    public List<LeaveRequestDto> getMyLeaveRequests(String username) {
        log.info("Fetching leave requests for user: {}", username);

        User employee = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        List<LeaveRequestDto> requests = leaveRequestRepository.findByEmployeeId(employee.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        log.info("Found {} leave requests for user: {}", requests.size(), username);
        return requests;
    }

    /**
     * Get all leave requests in the system
     * Only accessible by MANAGER and ADMIN roles (enforced by @PreAuthorize in controller)
     *
     * @return List of all leave request DTOs
     */
    public List<LeaveRequestDto> getAllLeaveRequests() {
        log.info("Fetching all leave requests");

        List<LeaveRequestDto> allRequests = leaveRequestRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        log.info("Found {} total leave requests", allRequests.size());
        return allRequests;
    }

    /**
     * Update leave request status (approve or reject)
     *
     * @param leaveId ID of the leave request
     * @param status New status (APPROVED or REJECTED)
     * @param managerUsername Email of the manager performing the action
     * @return Updated leave request DTO
     * @throws UsernameNotFoundException if manager not found
     * @throws AccessDeniedException if user doesn't have manager/admin role
     * @throws IllegalArgumentException if leave request not found
     */
    @Transactional
    public LeaveRequestDto updateLeaveStatus(Long leaveId, LeaveStatus status, String managerUsername) {
        log.info("Updating leave request {} to status {} by manager: {}",
                leaveId, status, managerUsername);

        // Find manager
        User manager = userRepository.findByEmail(managerUsername)
                .orElseThrow(() -> {
                    log.error("Manager not found with email: {}", managerUsername);
                    return new UsernameNotFoundException("Manager not found: " + managerUsername);
                });

        // Verify manager has appropriate role
        if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.ADMIN) {
            log.error("User {} with role {} attempted to update leave status",
                    managerUsername, manager.getRole());
            throw new AccessDeniedException("You do not have permission to approve or reject leave requests");
        }

        // Find leave request
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> {
                    log.error("Leave request not found with ID: {}", leaveId);
                    return new IllegalArgumentException("Leave request not found with ID: " + leaveId);
                });

        // Prevent updating already processed requests
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            log.error("Attempted to update leave request {} which is already in {} status",
                    leaveId, leaveRequest.getStatus());
            throw new IllegalArgumentException(
                    "Cannot update leave request that is already " + leaveRequest.getStatus()
            );
        }

        // Update status and assign manager
        leaveRequest.setStatus(status);
        leaveRequest.setManager(manager);

        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} successfully updated to status {}", leaveId, status);

        return mapToDto(updatedRequest);
    }

    /**
     * Map LeaveRequest entity to LeaveRequestDto
     *
     * @param leaveRequest Entity to map
     * @return Mapped DTO
     */
    private LeaveRequestDto mapToDto(LeaveRequest leaveRequest) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setId(leaveRequest.getId());
        dto.setStartDate(leaveRequest.getStartDate());
        dto.setEndDate(leaveRequest.getEndDate());
        dto.setReason(leaveRequest.getReason());
        dto.setStatus(leaveRequest.getStatus());
        dto.setEmployeeId(leaveRequest.getEmployee().getId());
        dto.setEmployeeName(
                leaveRequest.getEmployee().getFirstName() + " " +
                        leaveRequest.getEmployee().getLastName()
        );

        // Set manager details if available
        if (leaveRequest.getManager() != null) {
            dto.setManagerName(
                    leaveRequest.getManager().getFirstName() + " " +
                            leaveRequest.getManager().getLastName()
            );
        }

        return dto;
    }
}
