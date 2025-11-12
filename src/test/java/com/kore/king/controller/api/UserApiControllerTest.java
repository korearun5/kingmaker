package com.kore.king.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kore.king.dto.UserDTO;
import com.kore.king.entity.User;
import com.kore.king.entity.UserRole;
import com.kore.king.mapper.UserMapper;
import com.kore.king.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserApiController.class)
class UserApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setAvailablePoints(1000);
        testUser.setWins(5);
        testUser.setLosses(3);
        testUser.setRole(UserRole.USER);

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setAvailablePoints(1000);
        testUserDTO.setWins(5);
        testUserDTO.setLosses(3);
    }

    @Test
    @WithMockUser(username = "testuser")
    void getProfile_ShouldReturnUserProfile() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.availablePoints").value(1000));
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_WithValidData_ShouldChangePassword() throws Exception {
        // Arrange
        Map<String, String> passwordRequest = new HashMap<>();
        passwordRequest.put("currentPassword", "oldPass");
        passwordRequest.put("newPassword", "newPass");
        passwordRequest.put("confirmPassword", "newPass");

        when(userService.changePassword("testuser", "oldPass", "newPass")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_WithMismatchedPasswords_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Map<String, String> passwordRequest = new HashMap<>();
        passwordRequest.put("currentPassword", "oldPass");
        passwordRequest.put("newPassword", "newPass");
        passwordRequest.put("confirmPassword", "differentPass");

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New passwords do not match"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_WithWrongCurrentPassword_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Map<String, String> passwordRequest = new HashMap<>();
        passwordRequest.put("currentPassword", "wrongPass");
        passwordRequest.put("newPassword", "newPass");
        passwordRequest.put("confirmPassword", "newPass");

        when(userService.changePassword("testuser", "wrongPass", "newPass")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Current password is incorrect"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getUserStats_ShouldReturnUserStats() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wins").value(5))
                .andExpect(jsonPath("$.losses").value(3))
                .andExpect(jsonPath("$.availablePoints").value(1000))
                .andExpect(jsonPath("$.totalGames").value(8));
    }
}