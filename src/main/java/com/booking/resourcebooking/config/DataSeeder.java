package com.booking.resourcebooking.config;

import com.booking.resourcebooking.entity.Resource;
import com.booking.resourcebooking.entity.Role;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.repository.ResourceRepository;
import com.booking.resourcebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a couple of test accounts (one ADMIN, one USER) and a handful of sample resources
 * so the API is immediately exercisable after startup. Controlled by app.seed.enabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.admin.username}")
    private String adminUsername;
    @Value("${app.seed.admin.password}")
    private String adminPassword;
    @Value("${app.seed.admin.email}")
    private String adminEmail;

    @Value("${app.seed.user.username}")
    private String userUsername;
    @Value("${app.seed.user.password}")
    private String userPassword;
    @Value("${app.seed.user.email}")
    private String userEmail;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        seedUser(adminUsername, adminEmail, adminPassword, Role.ADMIN);
        seedUser(userUsername, userEmail, userPassword, Role.USER);
        seedResources();
    }

    private void seedUser(String username, String email, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build();
        userRepository.save(user);
        log.info("Seeded {} account -> username: '{}'", role, username);
    }

    private void seedResources() {
        if (resourceRepository.count() > 0) {
            return;
        }

        resourceRepository.save(Resource.builder()
                .name("Conference Room A")
                .type("ROOM")
                .description("8-seat conference room with projector and video conferencing")
                .location("3rd Floor, East Wing")
                .available(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Conference Room B")
                .type("ROOM")
                .description("4-seat huddle room")
                .location("3rd Floor, West Wing")
                .available(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Toyota Camry - Fleet Car 1")
                .type("VEHICLE")
                .description("Company sedan for local business travel")
                .location("Basement Parking, Slot 12")
                .available(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Projector - Epson EB-2250U")
                .type("EQUIPMENT")
                .description("Portable projector, includes HDMI cable and remote")
                .location("IT Storage Room")
                .available(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Executive Boardroom")
                .type("ROOM")
                .description("Large boardroom, seats 16, used for leadership meetings")
                .location("5th Floor")
                .available(false)
                .build());

        log.info("Seeded 5 sample resources");
    }
}
