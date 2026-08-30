package com.booking.resourcebooking.dto.request;

import com.booking.resourcebooking.entity.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationUpdateRequest(

        @NotNull(message = "resourceId is required")
        Long resourceId,

        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        @NotNull(message = "endTime is required")
        LocalDateTime endTime,

        @NotNull(message = "status is required")
        ReservationStatus status,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "price must not be negative")
        BigDecimal price,

        String notes
) {
}
