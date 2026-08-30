package com.booking.resourcebooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResourceRequest(

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        @NotBlank(message = "type is required")
        @Size(max = 50, message = "type must be at most 50 characters")
        String type,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        @NotBlank(message = "location is required")
        String location,

        Boolean available
) {
}
