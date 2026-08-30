package com.booking.resourcebooking.dto.response;

import com.booking.resourcebooking.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private Long resourceId;
    private String resourceName;
    private Long userId;
    private String username;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;
    private BigDecimal price;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
