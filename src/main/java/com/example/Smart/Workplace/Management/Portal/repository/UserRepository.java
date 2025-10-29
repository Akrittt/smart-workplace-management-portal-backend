package com.example.Smart.Workplace.Management.Portal.repository;

import com.example.Smart.Workplace.Management.Portal.model.Role;
import com.example.Smart.Workplace.Management.Portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity
 * Spring Data JPA automatically implements these methods
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ============= Find Methods =============

    /**
     * Find user by email (used for authentication)
     * @param email User's email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users by role
     * @param role User role (EMPLOYEE, MANAGER, ADMIN)
     * @return List of users with the specified role
     */
    List<User> findByRole(Role role);

    /**
     * Find all users by department
     * @param department Department name
     * @return List of users in the specified department
     */
    List<User> findByDepartment(String department);

    /**
     * Find all active users
     * @param active Active status
     * @return List of active/inactive users
     */
    List<User> findByActive(Boolean active);

    /**
     * Find users by role and active status
     * @param role User role
     * @param active Active status
     * @return List of users matching both criteria
     */
    List<User> findByRoleAndActive(Role role, Boolean active);

    /**
     * Find users by first name (case-insensitive)
     * @param firstName First name
     * @return List of users with matching first name
     */
    List<User> findByFirstNameContainingIgnoreCase(String firstName);

    /**
     * Find users by last name (case-insensitive)
     * @param lastName Last name
     * @return List of users with matching last name
     */
    List<User> findByLastNameContainingIgnoreCase(String lastName);

    // ============= Exists Methods =============

    /**
     * Check if user exists by email (for duplicate checking)
     * More efficient than findByEmail for existence checks
     * @param email User's email address
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if user exists by ID
     * @param id User ID
     * @return true if user exists, false otherwise
     */
    boolean existsById(Long id);

    // ============= Count Methods =============

    /**
     * Count users by role
     * @param role User role
     * @return Number of users with the specified role
     */
    long countByRole(Role role);

    /**
     * Count active users
     * @param active Active status
     * @return Number of active/inactive users
     */
    long countByActive(Boolean active);

    /**
     * Count users by department
     * @param department Department name
     * @return Number of users in the department
     */
    long countByDepartment(String department);

    // ============= Custom JPQL Queries =============

    /**
     * Find all managers and admins (for assigning complaints/leaves)
     * Using custom JPQL query for OR condition
     * @return List of users with MANAGER or ADMIN role
     */
    @Query("SELECT u FROM User u WHERE u.role = 'MANAGER' OR u.role = 'ADMIN' AND u.active = true")
    List<User> findAllManagersAndAdmins();

    /**
     * Find users by full name search (searches both first and last name)
     * @param searchTerm Search term
     * @return List of users matching the search
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchByFullName(@Param("searchTerm") String searchTerm);

    /**
     * Find all employees in a specific department
     * @param department Department name
     * @return List of employees in the department
     */
    @Query("SELECT u FROM User u WHERE u.department = :department AND u.role = 'EMPLOYEE' AND u.active = true")
    List<User> findActiveEmployeesByDepartment(@Param("department") String department);

    /**
     * Find users who can approve leaves (MANAGER and ADMIN)
     * @return List of approvers
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('MANAGER', 'ADMIN') AND u.active = true ORDER BY u.lastName")
    List<User> findAllApprovers();

    /**
     * Get user statistics by role
     * @return List of Object arrays containing [role, count]
     */
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> getUserStatisticsByRole();

    /**
     * Get department statistics
     * @return List of Object arrays containing [department, count]
     */
    @Query("SELECT u.department, COUNT(u) FROM User u WHERE u.department IS NOT NULL GROUP BY u.department ORDER BY COUNT(u) DESC")
    List<Object[]> getDepartmentStatistics();
}
