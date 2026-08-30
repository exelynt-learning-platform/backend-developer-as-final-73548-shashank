package com.booking.resourcebooking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationRequest(

        @NotNull(message = "resourceId is required")
        Long resourceId,

        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        @NotNull(message = "endTime is required")
        LocalDateTime endTime,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "price must not be negative")
        BigDecimal price,

        String notes,

        // Optional. Only honored when the caller is an ADMIN booking on behalf of another user.
        // For a USER-role caller this field is always ignored — identity comes from the JWT.
        Long userId
) {
}
