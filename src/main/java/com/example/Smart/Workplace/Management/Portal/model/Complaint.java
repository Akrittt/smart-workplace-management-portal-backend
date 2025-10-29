package com.example.Smart.Workplace.Management.Portal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Complaint entity for tracking workplace issues
 */
@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_submitted_at", columnList = "submitted_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
@EqualsAndHashCode(of = "id")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who submitted the complaint
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Title/subject of the complaint
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Detailed description of the complaint
     */
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    @Lob
    private String description;

    /**
     * Category of complaint (IT, HR, Facilities, etc.)
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * Priority level
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @Builder.Default
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    /**
     * Current status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.OPEN;

    /**
     * Staff member assigned to handle the complaint
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    /**
     * Resolution/response from staff
     */
    @Column(name = "resolution", columnDefinition = "TEXT")
    @Lob
    private String resolution;

    /**
     * Timestamp when complaint was submitted
     */
    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    /**
     * Timestamp of last update
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Timestamp when complaint was resolved
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Auto-set resolved time when status changes to RESOLVED
     */
    @PreUpdate
    protected void onUpdate() {
        if (status == ComplaintStatus.RESOLVED && resolvedAt == null) {
            resolvedAt = LocalDateTime.now();
        }
    }

    /**
     * Helper methods
     */
    @Transient
    public boolean isOpen() {
        return status == ComplaintStatus.OPEN;
    }

    @Transient
    public boolean isAssigned() {
        return assignedTo != null;
    }
}
