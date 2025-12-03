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

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);


    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Check if user exists by ID
     */
    boolean existsById(Long id);


    /**
     * Count users by role
     */
    long countByRole(Role role);

    /**
     * Count active users
     */
    long countByActive(Boolean active);


    /**
     * Get department statistics
     */
    @Query("SELECT u.department, COUNT(u) FROM User u WHERE u.department IS NOT NULL GROUP BY u.department ORDER BY COUNT(u) DESC")
    List<Object[]> getDepartmentStatistics();
}
