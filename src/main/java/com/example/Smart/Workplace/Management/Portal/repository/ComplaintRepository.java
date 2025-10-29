package com.example.Smart.Workplace.Management.Portal.repository;

import com.example.Smart.Workplace.Management.Portal.model.Complaint;
import com.example.Smart.Workplace.Management.Portal.model.ComplaintStatus;
import com.example.Smart.Workplace.Management.Portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // Find all complaints by user
    List<Complaint> findByUserId(Long userId);

    // Find complaints by status
    List<Complaint> findByStatus(ComplaintStatus status);

    // Find complaints assigned to a staff member
    List<Complaint> findByAssignedToId(Long assignedToId);

    // Find unassigned complaints
    List<Complaint> findByAssignedToIsNull();

    // Find by category
    List<Complaint> findByCategory(String category);

    // Count by status
    long countByStatus(ComplaintStatus status);

    // Count by user
    long countByUserId(Long userId);
}
