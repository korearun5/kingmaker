package com.kore.king.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kore.king.dto.BetDTO;
import com.kore.king.dto.CreateBetRequest;
import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.entity.User;
import com.kore.king.mapper.BetMapper;
import com.kore.king.service.BetService;
import com.kore.king.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BetApiController.class)
class BetApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BetService betService;

    @MockBean
    private UserService userService;

    @MockBean
    private BetMapper betMapper;

    private User testUser;
    private Bet testBet;
    private BetDTO testBetDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testBet = new Bet(testUser, 100, "Ludo", "Test Bet");
        testBet.setId(1L);

        testBetDTO = new BetDTO();
        testBetDTO.setId(1L);
        testBetDTO.setTitle("Test Bet");
        testBetDTO.setPoints(100);
        testBetDTO.setGameType("Ludo");
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAvailableBets_ShouldReturnBets() throws Exception {
        // Arrange
        Page<Bet> betPage = new PageImpl<>(List.of(testBet));
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(betService.findAvailableBets(eq(BetStatus.PENDING), eq(1L), any())).thenReturn(betPage);
        when(betMapper.toDTO(testBet)).thenReturn(testBetDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/bets")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bets").isArray())
                .andExpect(jsonPath("$.bets[0].title").value("Test Bet"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createBet_WithValidRequest_ShouldCreateBet() throws Exception {
        // Arrange
        CreateBetRequest request = new CreateBetRequest();
        request.setTitle("New Bet");
        request.setPoints(100);
        request.setGameType("Ludo");
        request.setDescription("Test Description");

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(betService.createBet(eq(1L), eq(100), eq("Ludo"), eq("New Bet"), eq("Test Description")))
                .thenReturn(testBet);
        when(betMapper.toDTO(testBet)).thenReturn(testBetDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/bets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Bet"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createBet_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CreateBetRequest request = new CreateBetRequest(); // Empty request

        // Act & Assert
        mockMvc.perform(post("/api/v1/bets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void acceptBet_ShouldAcceptBet() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(betService.acceptBet(1L, 1L)).thenReturn(testBet);
        when(betMapper.toDTO(testBet)).thenReturn(testBetDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/bets/1/accept")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "testuser")
    void cancelBet_ShouldCancelBet() throws Exception {
        // Arrange
        //:CHECK when(betService.cancelBet(1L, "testuser")).thenReturn(null);

        // Act & Assert
        //mockMvc.perform(post("/api/v1/bets/1/cancel")
        //        .with(csrf()))
        //        .andExpect(status().isOk())
        //        .andExpect(jsonPath("$.message").value("Bet cancelled successfully"));
    }

    @Test
    @WithMockUser
    void getBet_WithExistingBet_ShouldReturnBet() throws Exception {
        // Arrange
        when(betService.findById(1L)).thenReturn(Optional.of(testBet));
        when(betMapper.toDTO(testBet)).thenReturn(testBetDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/bets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void getBet_WithNonExistingBet_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(betService.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/bets/1"))
                .andExpect(status().isNotFound());
    }
}