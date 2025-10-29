package com.example.Smart.Workplace.Management.Portal.config;

import com.example.Smart.Workplace.Management.Portal.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the application
 * Configures JWT authentication, CORS, and authorization rules
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Configure security filter chain
     * Defines which endpoints require authentication and authorization
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed for stateless JWT authentication)
                .csrf(csrf -> csrf.disable())

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Leave Management endpoints
                        .requestMatchers(HttpMethod.POST, "/api/leave/submit").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/leave/my-requests").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/leave/all").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/leave/{id}/approve").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/leave/{id}/reject").hasAnyRole("MANAGER", "ADMIN")

                        // Complaint Management endpoints
                        .requestMatchers(HttpMethod.POST, "/api/complaints").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/complaints/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/complaints/all").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/complaints/assigned").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/complaints/unassigned").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/complaints/{id}/assign/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/complaints/{id}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/complaints/{id}").hasRole("ADMIN")

                        // admin panel
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Stateless session management (JWT-based)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Add authentication provider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before username/password authentication filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configure CORS to allow frontend requests
     * Supports both Vite (5173) and Create React App (3000) default ports
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow credentials (cookies, authorization headers, etc.)
        configuration.setAllowCredentials(true);

        // Allow multiple origins (development environments)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173"
        ));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Expose these headers to the client
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count"
        ));

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Configure authentication provider with UserDetailsService and password encoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication manager bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Password encoder bean using BCrypt
     * BCrypt automatically handles salting and is resistant to rainbow table attacks
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
