package com.example.Smart.Workplace.Management.Portal.repository;

import com.example.Smart.Workplace.Management.Portal.model.LeaveRequest;
import com.example.Smart.Workplace.Management.Portal.model.LeaveStatus;
import com.example.Smart.Workplace.Management.Portal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for LeaveRequest entity
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // ============= Find by Employee =============
    List<LeaveRequest> findByEmployeeId(Long employeeId);
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    List<LeaveRequest> findByEmployee(User employee);
    List<LeaveRequest> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    // ============= Find by Status =============
    List<LeaveRequest> findByStatus(LeaveStatus status);
    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);
    List<LeaveRequest> findByStatusOrderByStartDateAsc(LeaveStatus status);

    // ============= Find by Employee and Status =============
    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    default List<LeaveRequest> findPendingByEmployeeId(Long employeeId) {
        return findByEmployeeIdAndStatus(employeeId, LeaveStatus.PENDING);
    }

    // ============= Find by Manager =============
    List<LeaveRequest> findByManagerId(Long managerId);
    List<LeaveRequest> findByManager(User manager);

    // ============= Date Range Queries =============
    List<LeaveRequest> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
    List<LeaveRequest> findByEndDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
            "AND lr.status IN ('PENDING', 'APPROVED') " +
            "AND ((lr.startDate <= :endDate AND lr.endDate >= :startDate))")
    List<LeaveRequest> findOverlappingLeaves(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<LeaveRequest> findByStartDateGreaterThanEqual(LocalDate date);
    List<LeaveRequest> findByEndDateLessThanEqual(LocalDate date);

    // ============= Complex Queries =============
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'PENDING' ORDER BY lr.startDate ASC")
    List<LeaveRequest> findAllPendingLeaves();

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'PENDING' ORDER BY lr.startDate ASC")
    Page<LeaveRequest> findAllPendingLeaves(Pageable pageable);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'APPROVED' " +
            "AND ((lr.startDate <= :endDate AND lr.endDate >= :startDate)) " +
            "ORDER BY lr.startDate")
    List<LeaveRequest> findApprovedLeavesInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT lr FROM LeaveRequest lr " +
            "JOIN lr.employee e " +
            "WHERE e.department = :department " +
            "AND lr.status = 'APPROVED' " +
            "AND ((lr.startDate <= :endDate AND lr.endDate >= :startDate)) " +
            "ORDER BY lr.startDate")
    List<LeaveRequest> findTeamLeavesInDateRange(
            @Param("department") String department,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'APPROVED' " +
            "AND lr.startDate >= :today ORDER BY lr.startDate")
    List<LeaveRequest> findUpcomingApprovedLeaves(@Param("today") LocalDate today);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'APPROVED' " +
            "AND lr.startDate <= :today AND lr.endDate >= :today")
    List<LeaveRequest> findActiveLeaves(@Param("today") LocalDate today);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'PENDING' " +
            "AND lr.startDate <= :thresholdDate ORDER BY lr.startDate")
    List<LeaveRequest> findLeavesNeedingUrgentAction(@Param("thresholdDate") LocalDate thresholdDate);

    // ============= Count Queries =============
    long countByStatus(LeaveStatus status);
    long countByEmployeeId(Long employeeId);
    long countByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
            "AND lr.status = 'APPROVED' " +
            "AND lr.startDate >= :startDate AND lr.endDate <= :endDate")
    long countApprovedLeavesInPeriod(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate total leave days for an employee in a period
     * FIXED FOR POSTGRESQL: Uses date subtraction instead of DATEDIFF
     */
    @Query(value = "SELECT COALESCE(SUM(end_date - start_date + 1), 0) " +
            "FROM leave_requests " +
            "WHERE employee_id = :employeeId " +
            "AND status = 'APPROVED' " +
            "AND start_date >= :startDate AND end_date <= :endDate",
            nativeQuery = true)
    Long calculateTotalLeaveDaysInPeriod(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============= Exists Queries =============
    boolean existsByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    @Query("SELECT CASE WHEN COUNT(lr) > 0 THEN true ELSE false END " +
            "FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
            "AND lr.status IN ('PENDING', 'APPROVED') " +
            "AND ((lr.startDate <= :endDate AND lr.endDate >= :startDate))")
    boolean hasOverlappingLeaves(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============= Statistics Queries =============
    @Query("SELECT lr.status, COUNT(lr) FROM LeaveRequest lr GROUP BY lr.status")
    List<Object[]> getLeaveStatisticsByStatus();

    /**
     * Get monthly leave statistics
     * FIXED FOR POSTGRESQL: Uses EXTRACT instead of MONTH/YEAR
     */
    @Query(value = "SELECT EXTRACT(MONTH FROM start_date) as month, COUNT(*) " +
            "FROM leave_requests " +
            "WHERE EXTRACT(YEAR FROM start_date) = :year " +
            "AND status = 'APPROVED' " +
            "GROUP BY EXTRACT(MONTH FROM start_date) " +
            "ORDER BY EXTRACT(MONTH FROM start_date)",
            nativeQuery = true)
    List<Object[]> getMonthlyLeaveStatistics(@Param("year") int year);
}
