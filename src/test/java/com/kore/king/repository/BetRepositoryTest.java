package com.kore.king.repository;

import com.kore.king.config.TestContainersConfiguration;
import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.entity.User;
import com.kore.king.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestContainersConfiguration.class)
class BetRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private UserRepository userRepository;

    private User creator;
    private User acceptor;
    private Bet pendingBet;
    private Bet acceptedBet;

    @BeforeEach
    void setUp() {
        // Create users
        creator = new User();
        creator.setUsername("creator");
        creator.setEmail("creator@test.com");
        creator.setPassword("password");
        creator.setRole(UserRole.USER);
        creator = userRepository.save(creator);

        acceptor = new User();
        acceptor.setUsername("acceptor");
        acceptor.setEmail("acceptor@test.com");
        acceptor.setPassword("password");
        acceptor.setRole(UserRole.USER);
        acceptor = userRepository.save(acceptor);

        // Create bets
        pendingBet = new Bet(creator, 100, "Ludo", "Pending Bet");
        pendingBet.setStatus(BetStatus.PENDING);
        pendingBet = betRepository.save(pendingBet);

        acceptedBet = new Bet(creator, 200, "Chess", "Accepted Bet");
        acceptedBet.setStatus(BetStatus.ACCEPTED);
        acceptedBet.setAcceptor(acceptor);
        acceptedBet = betRepository.save(acceptedBet);
    }

    @Test
    void findAvailableBets_ShouldReturnOnlyPendingBetsExcludingUserOwn() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Bet> result = betRepository.findAvailableBets(BetStatus.PENDING, acceptor.getId(), pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(BetStatus.PENDING);
        assertThat(result.getContent().get(0).getCreator().getId()).isNotEqualTo(acceptor.getId());
    }

    @Test
    void findUserBets_ShouldReturnAllUserBets() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Bet> result = betRepository.findUserBets(creator.getId(), pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findUserActiveBets_ShouldReturnActiveBets() {
        // Act
        List<Bet> result = betRepository.findUserActiveBets(creator.getId());

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void findByIdWithAcceptor_ShouldReturnBetWithAcceptor() {
        // Act
        Optional<Bet> result = betRepository.findByIdWithAcceptor(acceptedBet.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAcceptor()).isNotNull();
        assertThat(result.get().getAcceptor().getUsername()).isEqualTo("acceptor");
    }

    @Test
    void findByStatus_ShouldReturnBetsWithGivenStatus() {
        // Act
        List<Bet> result = betRepository.findByStatus(BetStatus.PENDING);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BetStatus.PENDING);
    }

    @Test
    void hasActiveBets_WithActiveBets_ShouldReturnTrue() {
        // Act
        boolean result = betRepository.hasActiveBets(creator.getId());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void countByStatus_ShouldReturnCorrectCount() {
        // Act
        long result = betRepository.countByStatus(BetStatus.PENDING);

        // Assert
        assertThat(result).isEqualTo(1);
    }
}