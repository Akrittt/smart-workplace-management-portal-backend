package com.example.Smart.Workplace.Management.Portal.dto;

import com.example.Smart.Workplace.Management.Portal.model.ComplaintPriority;
import com.example.Smart.Workplace.Management.Portal.model.ComplaintStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDto {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String description;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    private ComplaintPriority priority;
    private ComplaintStatus status;

    // User who submitted
    private Long userId;
    private String userName;

    // Assigned staff
    private Long assignedToId;
    private String assignedToName;

    private String resolution;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;
}
