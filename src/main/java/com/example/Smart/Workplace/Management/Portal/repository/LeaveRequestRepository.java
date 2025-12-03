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
 * PostgreSQL Compatible Version
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    default List<LeaveRequest> findPendingByEmployeeId(Long employeeId) {
        return findByEmployeeIdAndStatus(employeeId, LeaveStatus.PENDING);
    }


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

    // ============= Count Queries =============
    long countByStatus(LeaveStatus status);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
            "AND lr.status = 'APPROVED' " +
            "AND lr.startDate >= :startDate AND lr.endDate <= :endDate")
    long countApprovedLeavesInPeriod(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    // ============= Statistics Queries =============
    @Query("SELECT lr.status, COUNT(lr) FROM LeaveRequest lr GROUP BY lr.status")
    List<Object[]> getLeaveStatisticsByStatus();

    /**
     * Get monthly leave statistics
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
