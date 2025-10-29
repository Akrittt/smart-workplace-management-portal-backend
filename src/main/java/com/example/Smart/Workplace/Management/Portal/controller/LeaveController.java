package com.example.Smart.Workplace.Management.Portal.controller;

import com.example.Smart.Workplace.Management.Portal.dto.LeaveRequestDto;
import com.example.Smart.Workplace.Management.Portal.model.LeaveStatus;
import com.example.Smart.Workplace.Management.Portal.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor  // Better than @Autowired
public class LeaveController {

    private final LeaveService leaveService;

    /**
     * Submit a new leave request
     * Accessible by all authenticated users
     */
    @PostMapping("/submit")
    public ResponseEntity<LeaveRequestDto> submitLeave(
            @Valid @RequestBody LeaveRequestDto leaveRequestDto,
            Authentication authentication) {
        String username = authentication.getName();
        LeaveRequestDto submittedRequest = leaveService.submitLeaveRequest(leaveRequestDto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(submittedRequest);
    }

    /**
     * Get all leave requests for the authenticated user
     * Accessible by all authenticated users
     */
    @GetMapping("/my-requests")
    public ResponseEntity<List<LeaveRequestDto>> getMyLeaveRequests(Authentication authentication) {
        String username = authentication.getName();
        List<LeaveRequestDto> requests = leaveService.getMyLeaveRequests(username);
        return ResponseEntity.ok(requests);
    }

    /**
     * Get all leave requests in the system
     * Accessible only by MANAGER and ADMIN roles
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<LeaveRequestDto>> getAllLeaveRequests() {
        List<LeaveRequestDto> allRequests = leaveService.getAllLeaveRequests();
        return ResponseEntity.ok(allRequests);
    }

    /**
     * Approve a leave request
     * Accessible only by MANAGER and ADMIN roles
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveRequestDto> approveLeave(
            @PathVariable Long id,
            Authentication authentication) {
        String managerUsername = authentication.getName();
        LeaveRequestDto approvedRequest = leaveService.updateLeaveStatus(id, LeaveStatus.APPROVED, managerUsername);
        return ResponseEntity.ok(approvedRequest);
    }

    /**
     * Reject a leave request
     * Accessible only by MANAGER and ADMIN roles
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveRequestDto> rejectLeave(
            @PathVariable Long id,
            Authentication authentication) {
        String managerUsername = authentication.getName();
        LeaveRequestDto rejectedRequest = leaveService.updateLeaveStatus(id, LeaveStatus.REJECTED, managerUsername);
        return ResponseEntity.ok(rejectedRequest);
    }
}
