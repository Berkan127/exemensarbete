package com.example.userservice.integration;

import com.example.userservice.dto.CreateUserRequest;
import com.example.userservice.dto.UserDto;
import com.example.userservice.user.User;
import com.example.userservice.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebMvc
class UserServiceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "testuser",
                "test@example.com",
                "Test",
                "User"
        );

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    void shouldGetAllUsers() throws Exception {
        // Create test users
        User user1 = new User("user1", "user1@example.com", "User", "One");
        User user2 = new User("user2", "user2@example.com", "User", "Two");
        userRepository.saveAll(List.of(user1, user2));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].username").value("user2"));
    }

    @Test
    void shouldGetUserById() throws Exception {
        User user = new User("testuser", "test@example.com", "Test", "User");
        User savedUser = userRepository.save(user);

        mockMvc.perform(get("/users/" + savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetUserByUsername() throws Exception {
        User user = new User("testuser", "test@example.com", "Test", "User");
        userRepository.save(user);

        mockMvc.perform(get("/users/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        User user = new User("testuser", "test@example.com", "Test", "User");
        User savedUser = userRepository.save(user);

        UserDto updateRequest = new UserDto(
                savedUser.getId(),
                "updateduser",
                "updated@example.com",
                "Updated",
                "User"
        );

        mockMvc.perform(put("/users/" + savedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        User user = new User("testuser", "test@example.com", "Test", "User");
        User savedUser = userRepository.save(user);

        mockMvc.perform(delete("/users/" + savedUser.getId()))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(savedUser.getId()));
    }

    @Test
    void shouldReturn409WhenCreatingDuplicateUser() throws Exception {
        // Create first user
        User existingUser = new User("testuser", "test@example.com", "Test", "User");
        userRepository.save(existingUser);

        // Try to create duplicate
        CreateUserRequest request = new CreateUserRequest(
                "testuser",
                "another@example.com",
                "Another",
                "User"
        );

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldPersistUserInDatabase() {
        // Given
        User user = new User("persisttest", "persist@example.com", "Persist", "Test");
        
        // When
        User savedUser = userRepository.save(user);
        
        // Then
        assertTrue(userRepository.existsById(savedUser.getId()));
        
        User retrievedUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertNotNull(retrievedUser);
        assertEquals("persisttest", retrievedUser.getUsername());
        assertEquals("persist@example.com", retrievedUser.getEmail());
        assertEquals("Persist", retrievedUser.getFirstName());
        assertEquals("Test", retrievedUser.getLastName());
    }

    @Test
    void shouldFindUserByUsername() {
        // Given
        User user = new User("findtest", "find@example.com", "Find", "Test");
        userRepository.save(user);
        
        // When
        User foundUser = userRepository.findByUsername("findtest").orElse(null);
        
        // Then
        assertNotNull(foundUser);
        assertEquals("findtest", foundUser.getUsername());
        assertEquals("find@example.com", foundUser.getEmail());
    }

    @Test
    void shouldCheckUserExists() {
        // Given
        User user = new User("existstest", "exists@example.com", "Exists", "Test");
        userRepository.save(user);
        
        // When & Then
        assertTrue(userRepository.existsByUsername("existstest"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }
}
