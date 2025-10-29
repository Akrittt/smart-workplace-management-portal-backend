package com.example.Smart.Workplace.Management.Portal.dto;

import com.example.Smart.Workplace.Management.Portal.model.LeaveStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDto {

    /**
     * Leave request ID (auto-generated, null for new requests)
     */
    private Long id;

    /**
     * Start date of the leave
     * Must not be null and should be today or in the future
     */
    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * End date of the leave
     * Must not be null and should be in the future
     */
    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * Reason for the leave request
     * Must be between 10 and 500 characters
     */
    @NotBlank(message = "Reason is required and cannot be blank")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;

    /**
     * Status of the leave request
     * Auto-set by system, not required in request
     */
    private LeaveStatus status;

    /**
     * Employee ID who requested the leave
     * Auto-set by system from authentication
     */
    private Long employeeId;

    /**
     * Employee name who requested the leave
     * Auto-set by system from authentication
     */
    private String employeeName;

    /**
     * Manager/Supervisor name who approved/rejected
     */
    private String managerName;

    /**
     * Date when the request was submitted
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime submittedAt;

    /**
     * Date when the request was approved/rejected
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime processedAt;

    /**
     * Custom validation: End date must be after start date
     */
    @AssertTrue(message = "End date must be after or equal to start date")
    private boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true; // Let @NotNull handle null validation
        }
        return !endDate.isBefore(startDate);
    }
}
