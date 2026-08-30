package com.booking.resourcebooking;

import com.booking.resourcebooking.entity.Resource;
import com.booking.resourcebooking.entity.Role;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.repository.ResourceRepository;
import com.booking.resourcebooking.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceBookingSystemApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Long resourceId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        resourceRepository.deleteAll();

        userRepository.save(User.builder()
                .username("admin1").email("admin1@test.com")
                .password(passwordEncoder.encode("Admin@123")).role(Role.ADMIN).enabled(true).build());

        userRepository.save(User.builder()
                .username("user1").email("user1@test.com")
                .password(passwordEncoder.encode("User@123")).role(Role.USER).enabled(true).build());

        Resource resource = resourceRepository.save(Resource.builder()
                .name("Test Room").type("ROOM").description("desc").location("HQ").available(true).build());
        resourceId = resource.getId();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin1", "password", "Admin@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin1", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequest_isRejected() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void user_cannot_createResource() throws Exception {
        String token = loginAndGetToken("user1", "User@123");
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Room","type":"ROOM","location":"HQ","available":true}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_can_createResource() throws Exception {
        String token = loginAndGetToken("admin1", "Admin@123");
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Room","type":"ROOM","location":"HQ","available":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Room"));
    }

    @Test
    void user_can_createReservation_andIdentityComesFromToken() throws Exception {
        String token = loginAndGetToken("user1", "User@123");

        String body = objectMapper.writeValueAsString(Map.of(
                "resourceId", resourceId,
                "startTime", LocalDateTime.now().plusDays(1).toString(),
                "endTime", LocalDateTime.now().plusDays(1).plusHours(2).toString(),
                "price", 50.00
        ));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void user_cannot_seeOtherUsersReservations() throws Exception {
        String user1Token = loginAndGetToken("user1", "User@123");

        String body = objectMapper.writeValueAsString(Map.of(
                "resourceId", resourceId,
                "startTime", LocalDateTime.now().plusDays(1).toString(),
                "endTime", LocalDateTime.now().plusDays(1).plusHours(2).toString(),
                "price", 50.00
        ));
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        userRepository.save(User.builder()
                .username("user2").email("user2@test.com")
                .password(passwordEncoder.encode("User@123")).role(Role.USER).enabled(true).build());
        String user2Token = loginAndGetToken("user2", "User@123");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
