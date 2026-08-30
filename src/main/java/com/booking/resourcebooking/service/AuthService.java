package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.request.LoginRequest;
import com.booking.resourcebooking.dto.request.RegisterRequest;
import com.booking.resourcebooking.dto.response.LoginResponse;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.exception.ConflictException;
import com.booking.resourcebooking.repository.UserRepository;
import com.booking.resourcebooking.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        // Delegates to the configured AuthenticationProvider (DAO + BCrypt), which throws
        // BadCredentialsException / DisabledException on failure — handled centrally.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Authenticated user vanished from the database"));

        String token = jwtUtil.generateToken(user, user.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build();

        return userRepository.save(user);
    }
}
