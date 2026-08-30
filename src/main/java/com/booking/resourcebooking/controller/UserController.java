package com.booking.resourcebooking.controller;

import com.booking.resourcebooking.dto.request.RegisterRequest;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "Admin-only user provisioning (create ADMIN or USER accounts)")
public class UserController {

    private final AuthService authService;

    @PostMapping
    @Operation(summary = "Create a user with a specific role (ADMIN only)")
    public ResponseEntity<UserSummary> create(@Valid @RequestBody RegisterRequest request) {
        User created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(created));
    }

    private UserSummary toSummary(User u) {
        return new UserSummary(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name());
    }

    public record UserSummary(Long id, String username, String email, String role) {
    }
}
