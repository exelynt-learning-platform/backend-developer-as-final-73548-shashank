package com.booking.resourcebooking.dto.request;

import com.booking.resourcebooking.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record ReservationStatusUpdateRequest(
        @NotNull(message = "status is required")
        ReservationStatus status
) {
}
