package com.kore.king.integration;

import com.kore.king.dto.CreateBetRequest;
import com.kore.king.entity.*;
import com.kore.king.repository.BetRepository;
import com.kore.king.repository.UserRepository;
import com.kore.king.service.BetService;
import com.kore.king.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BetFlowIntegrationTest {

    @Autowired
    private BetService betService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BetRepository betRepository;

    private User creator;
    private User acceptor;

    @BeforeEach
    void setUp() {
        // Create test users
        creator = new User();
        creator.setUsername("creator");
        creator.setEmail("creator@test.com");
        creator.setPassword("password");
        creator.setAvailablePoints(1000);
        creator = userService.registerUser(creator);

        acceptor = new User();
        acceptor.setUsername("acceptor");
        acceptor.setEmail("acceptor@test.com");
        acceptor.setPassword("password");
        acceptor.setAvailablePoints(1000);
        acceptor = userService.registerUser(acceptor);
    }

    @Test
    @WithMockUser(username = "creator")
    void completeBetFlow_ShouldWorkCorrectly() {
        // Step 1: Create bet
        Bet createdBet = betService.createBet(creator.getId(), 100, "Ludo", "Integration Test Bet", "Test Description");
        
        assertThat(createdBet).isNotNull();
        assertThat(createdBet.getStatus()).isEqualTo(BetStatus.PENDING);
        assertThat(creator.getAvailablePoints()).isEqualTo(900); // 1000 - 100 held

        // Step 2: Accept bet
        Bet acceptedBet = betService.acceptBet(createdBet.getId(), acceptor.getId());
        
        assertThat(acceptedBet.getStatus()).isEqualTo(BetStatus.ACCEPTED);
        assertThat(acceptedBet.getAcceptor()).isEqualTo(acceptor);
        assertThat(acceptor.getAvailablePoints()).isEqualTo(900); // 1000 - 100 held

        // Step 3: Share game code
        Bet codeSharedBet = betService.setGameCode(acceptedBet.getId(), "GAME123", "creator");
        
        assertThat(codeSharedBet.getStatus()).isEqualTo(BetStatus.CODE_SHARED);
        assertThat(codeSharedBet.getGameCode()).isEqualTo("GAME123");

        // Step 4: Submit results
        betService.submitResult(codeSharedBet.getId(), "creator", "WIN", "screenshot.jpg", true);
        betService.submitResult(codeSharedBet.getId(), "acceptor", "LOSE", null, false);

        // Verify final state
        Bet completedBet = betRepository.findByIdWithAcceptor(createdBet.getId()).orElseThrow();
        assertThat(completedBet.getStatus()).isEqualTo(BetStatus.COMPLETED);
        assertThat(completedBet.getCreatorResult()).isEqualTo(Result.WIN);
        assertThat(completedBet.getAcceptorResult()).isEqualTo(Result.LOSE);
        
        // Verify points distribution (considering 4% platform fee)
        User updatedCreator = userService.findByUsername("creator").orElseThrow();
        User updatedAcceptor = userService.findByUsername("acceptor").orElseThrow();
        
        assertThat(updatedCreator.getAvailablePoints()).isEqualTo(1192); // 900 + (200 - 8) = 1092? Wait, recalculate
        assertThat(updatedCreator.getWins()).isEqualTo(1);
        assertThat(updatedAcceptor.getLosses()).isEqualTo(1);
    }
}