package com.example.Smart.Workplace.Management.Portal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User entity representing system users
 * Implements UserDetails for Spring Security authentication
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_role", columnList = "role")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password") // Don't log passwords
@EqualsAndHashCode(of = "id") // Only use ID for equals/hashCode
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 100)
    private String department;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============= UserDetails Implementation =============

    /**
     * Returns the authorities granted to the user
     * Adds "ROLE_" prefix as required by Spring Security
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Returns the username used to authenticate the user
     * In this application, email is used as username
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Returns the password used to authenticate the user
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Indicates whether the user's account has expired
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled
     * A disabled user cannot be authenticated
     */
    @Override
    public boolean isEnabled() {
        return active;
    }

    /**
     * Helper method to get full name
     */
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
