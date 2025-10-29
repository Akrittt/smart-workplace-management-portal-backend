package com.example.Smart.Workplace.Management.Portal.service;

import com.example.Smart.Workplace.Management.Portal.dto.AuthRequest;
import com.example.Smart.Workplace.Management.Portal.dto.AuthResponse;
import com.example.Smart.Workplace.Management.Portal.dto.RegisterRequest;
import com.example.Smart.Workplace.Management.Portal.model.Role;
import com.example.Smart.Workplace.Management.Portal.model.User;
import com.example.Smart.Workplace.Management.Portal.repository.UserRepository;
import com.example.Smart.Workplace.Management.Portal.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service handling user registration and login
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user
     *
     * @param request Registration details
     * @return Authentication response with JWT token
     * @throws IllegalArgumentException if email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("Registration failed: Email already in use - {}", request.getEmail());
            throw new IllegalArgumentException("Email already in use");
        }

        // Validate password strength (optional but recommended)
        validatePassword(request.getPassword());

        // Build and save user using Builder pattern
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase().trim()) // Normalize email
                .password(passwordEncoder.encode(request.getPassword()))
                .role(determineUserRole(request)) // Dynamic role assignment
                .active(true)
                .department(request.getDepartment())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and role: {}",
                savedUser.getId(), savedUser.getRole());

        // Generate JWT token
        String jwtToken = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .fullName(savedUser.getFullName())
                .build();
    }

    /**
     * Authenticate and login user
     *
     * @param request Login credentials
     * @return Authentication response with JWT token
     * @throws BadCredentialsException if credentials are invalid
     * @throws DisabledException if user account is disabled
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );

            log.info("Authentication successful for: {}", request.getEmail());

        } catch (BadCredentialsException e) {
            log.error("Authentication failed: Invalid credentials for email: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");

        } catch (DisabledException e) {
            log.error("Authentication failed: Account disabled for email: {}", request.getEmail());
            throw new DisabledException("Account is disabled. Please contact administrator.");

        } catch (AuthenticationException e) {
            log.error("Authentication failed for email: {} - {}", request.getEmail(), e.getMessage());
            throw new BadCredentialsException("Authentication failed");
        }

        // Fetch user details
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> {
                    log.error("User not found after successful authentication: {}", request.getEmail());
                    return new UsernameNotFoundException("User not found");
                });

        // Check if user is active
        if (!user.getActive()) {
            log.error("Login attempt for inactive account: {}", request.getEmail());
            throw new DisabledException("Your account has been deactivated");
        }

        // Generate JWT token
        String jwtToken = jwtService.generateToken(user);

        log.info("JWT token generated successfully for user ID: {}", user.getId());

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .build();
    }

    /**
     * Validate password strength
     *
     * @param password Password to validate
     * @throws IllegalArgumentException if password doesn't meet requirements
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Check for at least one uppercase, one lowercase, and one digit
        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasUpperCase || !hasLowerCase || !hasDigit) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
            );
        }
    }

    /**
     * Determine user role based on registration request
     * Can be extended to support role-based registration
     *
     * @param request Registration request
     * @return User role
     */
    private Role determineUserRole(RegisterRequest request) {
        // Default role is EMPLOYEE
        // You can extend this to check request.getRole() if you want to allow role selection
        return Role.EMPLOYEE;
    }

    /**
     * Refresh JWT token (optional feature)
     *
     * @param oldToken Existing valid token
     * @return New JWT token
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String oldToken) {
        log.info("Attempting to refresh token");

        String email = jwtService.extractUsername(oldToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new DisabledException("User account is disabled");
        }

        String newToken = jwtService.generateToken(user);

        log.info("Token refreshed successfully for user: {}", email);

        return AuthResponse.builder()
                .token(newToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .build();
    }
}
