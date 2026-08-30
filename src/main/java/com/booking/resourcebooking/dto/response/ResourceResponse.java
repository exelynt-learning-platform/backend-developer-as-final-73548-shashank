package com.booking.resourcebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ResourceResponse {
    private Long id;
    private String name;
    private String type;
    private String description;
    private String location;
    private boolean available;
    private Instant createdAt;
    private Instant updatedAt;
}
