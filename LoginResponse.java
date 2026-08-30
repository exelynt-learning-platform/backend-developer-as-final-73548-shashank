package com.booking.resourcebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType;
    private String username;
    private String role;
    private long expiresInMs;
}
