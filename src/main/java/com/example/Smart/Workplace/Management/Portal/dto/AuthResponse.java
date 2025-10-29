package com.example.Smart.Workplace.Management.Portal.dto;

import com.example.Smart.Workplace.Management.Portal.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private Role role;
    private String fullName;
}
