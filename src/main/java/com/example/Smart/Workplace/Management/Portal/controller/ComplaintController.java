package com.example.Smart.Workplace.Management.Portal.controller;

import com.example.Smart.Workplace.Management.Portal.dto.ComplaintDto;
import com.example.Smart.Workplace.Management.Portal.model.ComplaintStatus;
import com.example.Smart.Workplace.Management.Portal.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@CrossOrigin(origins = {"smart-workplace-management-portal.vercel.app","http://localhost:5173/"})
public class ComplaintController {

    private final ComplaintService complaintService;

    /**
     * Submit a new complaint (All authenticated users)
     */
    @PostMapping
    public ResponseEntity<ComplaintDto> submitComplaint(
            @Valid @RequestBody ComplaintDto complaintDto,
            Authentication authentication) {
        ComplaintDto created = complaintService.submitComplaint(complaintDto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get my complaints
     */
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintDto>> getMyComplaints(Authentication authentication) {
        List<ComplaintDto> complaints = complaintService.getMyComplaints(authentication.getName());
        return ResponseEntity.ok(complaints);
    }

    /**
     * Get all complaints (MANAGER/ADMIN only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<ComplaintDto>> getAllComplaints() {
        List<ComplaintDto> complaints = complaintService.getAllComplaints();
        return ResponseEntity.ok(complaints);
    }

    /**
     * Get complaints assigned to me (MANAGER only)
     */
    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<ComplaintDto>> getAssignedComplaints(Authentication authentication) {
        List<ComplaintDto> complaints = complaintService.getAssignedComplaints(authentication.getName());
        return ResponseEntity.ok(complaints);
    }

    /**
     * Get unassigned complaints (MANAGER/ADMIN only)
     */
    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<ComplaintDto>> getUnassignedComplaints() {
        List<ComplaintDto> complaints = complaintService.getUnassignedComplaints();
        return ResponseEntity.ok(complaints);
    }

    /**
     * Assign complaint to staff
     */
    @PatchMapping("/{id}/assign/{staffId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> assignComplaint(
            @PathVariable Long id,
            @PathVariable Long staffId,
            Authentication authentication) {
        complaintService.assignComplaint(id, staffId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    /**
     * Update complaint status/resolution
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintDto> updateComplaint(
            @PathVariable Long id,
            @RequestParam ComplaintStatus status,
            Authentication authentication) {
        complaintService.updateComplaint(id, status, authentication.getName());
        return ResponseEntity.ok().build();
    }

    /**
     * Delete complaint (ADMIN only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long id,
            Authentication authentication) {
        complaintService.deleteComplaint(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
