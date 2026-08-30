package com.booking.resourcebooking.controller;

import com.booking.resourcebooking.dto.request.LoginRequest;
import com.booking.resourcebooking.dto.request.RegisterRequest;
import com.booking.resourcebooking.dto.response.LoginResponse;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Login and user registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Open registration always creates USER accounts; provisioning ADMIN accounts is done via
    // seed data or by an existing ADMIN through /api/users (see UserController).
    @PostMapping("/register")
    @Operation(summary = "Self-register a new USER account")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        RegisterRequest forcedUserRole = new RegisterRequest(
                request.username(), request.email(), request.password(), com.booking.resourcebooking.entity.Role.USER);
        authService.register(forcedUserRole);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
