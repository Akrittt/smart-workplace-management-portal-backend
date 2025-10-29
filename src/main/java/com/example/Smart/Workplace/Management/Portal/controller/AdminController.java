package com.example.Smart.Workplace.Management.Portal.controller;

import com.example.Smart.Workplace.Management.Portal.model.ComplaintStatus;
import com.example.Smart.Workplace.Management.Portal.model.LeaveStatus;
import com.example.Smart.Workplace.Management.Portal.model.Role;
import com.example.Smart.Workplace.Management.Portal.model.User;
import com.example.Smart.Workplace.Management.Portal.repository.ComplaintRepository;
import com.example.Smart.Workplace.Management.Portal.repository.LeaveRequestRepository;
import com.example.Smart.Workplace.Management.Portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Controller
 * Handles all admin-specific operations
 * Only accessible by users with ADMIN role
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AdminController {

    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ComplaintRepository complaintRepository;

    // ============= USER MANAGEMENT =============

    /**
     * Get all users
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        log.info("Admin fetching all users");

        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());

        log.info("Returning {} users", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        log.info("Admin fetching user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        return ResponseEntity.ok(mapUserToDto(user));
    }

    /**
     * Toggle user active status
     */
    @PutMapping("/users/{id}/toggle-active")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable Long id) {
        log.info("Admin toggling active status for user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        user.setActive(!user.getActive());
        User updatedUser = userRepository.save(user);

        log.info("User {} is now {}", id, updatedUser.getActive() ? "active" : "inactive");
        return ResponseEntity.ok(mapUserToDto(updatedUser));
    }

    /**
     * Update user role
     */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestParam Role role) {

        log.info("Admin updating role for user ID: {} to {}", id, role);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        user.setRole(role);
        User updatedUser = userRepository.save(user);

        log.info("User {} role updated to {}", id, role);
        return ResponseEntity.ok(mapUserToDto(updatedUser));
    }

    /**
     * Delete user (soft delete - set inactive)
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        log.info("Admin deleting user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Soft delete - just deactivate
        user.setActive(false);
        userRepository.save(user);

        // For hard delete, uncomment:
        // userRepository.delete(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");

        log.info("User {} deleted successfully", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user statistics by role
     */
    @GetMapping("/users/count")
    public ResponseEntity<Map<String, Object>> getUserCount() {
        log.info("Admin fetching user count statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", userRepository.count());
        stats.put("employees", userRepository.countByRole(Role.EMPLOYEE));
        stats.put("managers", userRepository.countByRole(Role.MANAGER));
        stats.put("admins", userRepository.countByRole(Role.ADMIN));
        stats.put("active", userRepository.countByActive(true));
        stats.put("inactive", userRepository.countByActive(false));

        return ResponseEntity.ok(stats);
    }

    // ============= LEAVE STATISTICS =============

    /**
     * Get leave statistics
     */
    @GetMapping("/leaves/statistics")
    public ResponseEntity<Map<String, Object>> getLeaveStatistics() {
        log.info("Admin fetching leave statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", leaveRequestRepository.count());
        stats.put("pending", leaveRequestRepository.countByStatus(LeaveStatus.PENDING));
        stats.put("approved", leaveRequestRepository.countByStatus(LeaveStatus.APPROVED));
        stats.put("rejected", leaveRequestRepository.countByStatus(LeaveStatus.REJECTED));

        return ResponseEntity.ok(stats);
    }

    /**
     * Get leave statistics by status
     */
    @GetMapping("/leaves/statistics-by-status")
    public ResponseEntity<List<Map<String, Object>>> getLeaveStatisticsByStatus() {
        log.info("Admin fetching leave statistics by status");

        List<Object[]> results = leaveRequestRepository.getLeaveStatisticsByStatus();

        List<Map<String, Object>> stats = results.stream()
                .map(result -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("status", result[0]);
                    stat.put("count", result[1]);
                    return stat;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(stats);
    }

    // ============= COMPLAINT STATISTICS =============

    /**
     * Get complaint statistics
     */
    @GetMapping("/complaints/statistics")
    public ResponseEntity<Map<String, Object>> getComplaintStatistics() {
        log.info("Admin fetching complaint statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", complaintRepository.count());
        stats.put("open", complaintRepository.countByStatus(ComplaintStatus.OPEN));
        stats.put("inProgress", complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS));
        stats.put("resolved", complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        stats.put("closed", complaintRepository.countByStatus(ComplaintStatus.CLOSED));

        return ResponseEntity.ok(stats);
    }

    // ============= ANALYTICS =============

    /**
     * Get analytics for leaves
     */
    @GetMapping("/analytics/leaves")
    public ResponseEntity<Map<String, Object>> getLeaveAnalytics(
            @RequestParam(defaultValue = "month") String range) {

        log.info("Admin fetching leave analytics for range: {}", range);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("total", leaveRequestRepository.count());
        analytics.put("pending", leaveRequestRepository.countByStatus(LeaveStatus.PENDING));
        analytics.put("approved", leaveRequestRepository.countByStatus(LeaveStatus.APPROVED));
        analytics.put("rejected", leaveRequestRepository.countByStatus(LeaveStatus.REJECTED));
        analytics.put("activeUsers", userRepository.countByActive(true));

        // Add monthly statistics for current year
        int currentYear = java.time.Year.now().getValue();
        List<Object[]> monthlyStats = leaveRequestRepository.getMonthlyLeaveStatistics(currentYear);

        List<Map<String, Object>> monthlyData = monthlyStats.stream()
                .map(result -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("month", result[0]);
                    data.put("count", result[1]);
                    return data;
                })
                .collect(Collectors.toList());

        analytics.put("monthlyData", monthlyData);

        return ResponseEntity.ok(analytics);
    }

    /**
     * Get analytics for complaints
     */
    @GetMapping("/analytics/complaints")
    public ResponseEntity<Map<String, Object>> getComplaintAnalytics(
            @RequestParam(defaultValue = "month") String range) {

        log.info("Admin fetching complaint analytics for range: {}", range);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("total", complaintRepository.count());
        analytics.put("open", complaintRepository.countByStatus(ComplaintStatus.OPEN));
        analytics.put("inProgress", complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS));
        analytics.put("resolved", complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        analytics.put("closed", complaintRepository.countByStatus(ComplaintStatus.CLOSED));

        // Calculate resolution rate
        long total = complaintRepository.count();
        long resolved = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);
        long closed = complaintRepository.countByStatus(ComplaintStatus.CLOSED);
        double resolutionRate = total > 0 ? ((resolved + closed) * 100.0 / total) : 0;

        analytics.put("resolutionRate", String.format("%.1f%%", resolutionRate));

        return ResponseEntity.ok(analytics);
    }

    /**
     * Get department statistics
     */
    @GetMapping("/analytics/departments")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentAnalytics() {
        log.info("Admin fetching department analytics");

        List<Object[]> deptStats = userRepository.getDepartmentStatistics();

        List<Map<String, Object>> analytics = deptStats.stream()
                .map(result -> {
                    String dept = (String) result[0];
                    Long employeeCount = (Long) result[1];

                    Map<String, Object> stat = new HashMap<>();
                    stat.put("name", dept);
                    stat.put("employeeCount", employeeCount);

                    // Get leave count for this department
                    long leaveCount = leaveRequestRepository.findTeamLeavesInDateRange(
                            dept,
                            java.time.LocalDate.now().minusMonths(1),
                            java.time.LocalDate.now()
                    ).size();

                    stat.put("leaveCount", leaveCount);
                    stat.put("complaintCount", 0); // Add if you have department field in complaints
                    stat.put("avgResolutionTime", "2.5 days"); // Calculate if needed

                    return stat;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(analytics);
    }

    /**
     * Get system overview (dashboard data)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        log.info("Admin fetching dashboard data");

        Map<String, Object> dashboard = new HashMap<>();

        // User statistics
        Map<String, Object> userStats = new HashMap<>();
        userStats.put("total", userRepository.count());
        userStats.put("employees", userRepository.countByRole(Role.EMPLOYEE));
        userStats.put("managers", userRepository.countByRole(Role.MANAGER));
        userStats.put("admins", userRepository.countByRole(Role.ADMIN));
        dashboard.put("users", userStats);

        // Leave statistics
        Map<String, Object> leaveStats = new HashMap<>();
        leaveStats.put("total", leaveRequestRepository.count());
        leaveStats.put("pending", leaveRequestRepository.countByStatus(LeaveStatus.PENDING));
        leaveStats.put("approved", leaveRequestRepository.countByStatus(LeaveStatus.APPROVED));
        dashboard.put("leaves", leaveStats);

        // Complaint statistics
        Map<String, Object> complaintStats = new HashMap<>();
        complaintStats.put("total", complaintRepository.count());
        complaintStats.put("open", complaintRepository.countByStatus(ComplaintStatus.OPEN));
        complaintStats.put("resolved", complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        dashboard.put("complaints", complaintStats);

        return ResponseEntity.ok(dashboard);
    }

    // ============= HELPER METHODS =============

    /**
     * Map User entity to DTO (excludes sensitive information like password)
     */
    private Map<String, Object> mapUserToDto(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("firstName", user.getFirstName());
        dto.put("lastName", user.getLastName());
        dto.put("email", user.getEmail());
        dto.put("role", user.getRole());
        dto.put("active", user.getActive());
        dto.put("department", user.getDepartment());
        dto.put("phoneNumber", user.getPhoneNumber());
        dto.put("createdAt", user.getCreatedAt());
        dto.put("updatedAt", user.getUpdatedAt());
        return dto;
    }
}
