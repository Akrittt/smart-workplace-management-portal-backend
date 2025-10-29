package com.example.Smart.Workplace.Management.Portal.security;

import com.example.Smart.Workplace.Management.Portal.model.User;
import com.example.Smart.Workplace.Management.Portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation for Spring Security
 * Loads user-specific data from database for authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by email (username in our case)
     * This method is called by Spring Security during authentication
     *
     * @param username Email address of the user
     * @return UserDetails object containing user authentication information
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    @Cacheable(value = "userDetailsCache", key = "#username", unless = "#result == null")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        // Normalize email to lowercase for case-insensitive lookup
        String normalizedEmail = username.toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", normalizedEmail);
                    return new UsernameNotFoundException("User not found with email: " + username);
                });

        // Additional validation checks
        if (!user.getActive()) {
            log.warn("Attempt to authenticate with disabled account: {}", normalizedEmail);
            throw new UsernameNotFoundException("User account is disabled");
        }

        log.debug("Successfully loaded user: {} with role: {}", user.getEmail(), user.getRole());

        // User entity implements UserDetails, so we can return it directly
        return user;
    }
}
