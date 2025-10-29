package com.example.Smart.Workplace.Management.Portal.service;

import com.example.Smart.Workplace.Management.Portal.dto.ComplaintDto;
import com.example.Smart.Workplace.Management.Portal.model.Complaint;
import com.example.Smart.Workplace.Management.Portal.model.ComplaintStatus;
import com.example.Smart.Workplace.Management.Portal.model.Role;
import com.example.Smart.Workplace.Management.Portal.model.User;
import com.example.Smart.Workplace.Management.Portal.repository.ComplaintRepository;
import com.example.Smart.Workplace.Management.Portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    /**
     * Submit a new complaint
     */
    @Transactional
    public ComplaintDto submitComplaint(ComplaintDto dto, String username) {
        log.info("Submitting complaint for user: {}", username);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Complaint complaint = Complaint.builder()
                .user(user)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .priority(dto.getPriority() != null ? dto.getPriority() : null)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        log.info("Complaint created with ID: {}", saved.getId());

        return mapToDto(saved);
    }

    /**
     * Get all complaints for authenticated user
     */
    public List<ComplaintDto> getMyComplaints(String username) {
        log.info("Fetching complaints for user: {}", username);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return complaintRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all complaints (MANAGER/ADMIN only)
     */
    public List<ComplaintDto> getAllComplaints() {
        log.info("Fetching all complaints");
        return complaintRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get complaints assigned to staff member
     */
    public List<ComplaintDto> getAssignedComplaints(String username) {
        log.info("Fetching assigned complaints for: {}", username);

        User staff = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return complaintRepository.findByAssignedToId(staff.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get unassigned complaints (MANAGER/ADMIN only)
     */
    public List<ComplaintDto> getUnassignedComplaints() {
        log.info("Fetching unassigned complaints");
        return complaintRepository.findByAssignedToIsNull()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Assign complaint to staff member
     */
    @Transactional
    public ComplaintDto assignComplaint(Long complaintId, Long staffId, String managerUsername) {
        log.info("Assigning complaint {} to staff {}", complaintId, staffId);

        // Verify manager permissions
        User manager = userRepository.findByEmail(managerUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Manager not found"));

        if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only managers/admins can assign complaints");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found"));

        complaint.setAssignedTo(staff);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);

        return mapToDto(complaintRepository.save(complaint));
    }

    /**
     * Update complaint status and resolution
     */
    @Transactional
    public ComplaintDto updateComplaint(Long complaintId, ComplaintDto dto, String username) {
        log.info("Updating complaint {} by user: {}", complaintId, username);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if user is assigned to this complaint or is admin
        if (complaint.getAssignedTo() == null ||
                (!complaint.getAssignedTo().getId().equals(user.getId()) &&
                        user.getRole() != Role.ADMIN)) {
            throw new AccessDeniedException("You are not authorized to update this complaint");
        }

        if (dto.getStatus() != null) {
            complaint.setStatus(dto.getStatus());
        }

        if (dto.getResolution() != null) {
            complaint.setResolution(dto.getResolution());
        }

        return mapToDto(complaintRepository.save(complaint));
    }

    /**
     * Delete complaint (ADMIN only)
     */
    @Transactional
    public void deleteComplaint(Long complaintId, String adminUsername) {
        log.info("Deleting complaint {} by admin: {}", complaintId, adminUsername);

        User admin = userRepository.findByEmail(adminUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can delete complaints");
        }

        complaintRepository.deleteById(complaintId);
        log.info("Complaint {} deleted successfully", complaintId);
    }

    /**
     * Map entity to DTO
     */
    private ComplaintDto mapToDto(Complaint complaint) {
        ComplaintDto dto = ComplaintDto.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .priority(complaint.getPriority())
                .status(complaint.getStatus())
                .userId(complaint.getUser().getId())
                .userName(complaint.getUser().getFullName())
                .resolution(complaint.getResolution())
                .submittedAt(complaint.getSubmittedAt())
                .updatedAt(complaint.getUpdatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .build();

        if (complaint.getAssignedTo() != null) {
            dto.setAssignedToId(complaint.getAssignedTo().getId());
            dto.setAssignedToName(complaint.getAssignedTo().getFullName());
        }

        return dto;
    }
}
