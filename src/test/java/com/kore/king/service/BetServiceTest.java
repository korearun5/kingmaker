package com.kore.king.service;

import java.util.List;

import com.kore.king.config.AppConfig;
import com.kore.king.entity.*;
import com.kore.king.repository.BetRepository;
import com.kore.king.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import jakarta.persistence.EntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BetServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private UserService userService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ReferralService referralService;

    @Mock
    private AppConfig appConfig;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BetService betService;

    private User testCreator;
    private User testAcceptor;
    private Bet testBet;

    @BeforeEach
    void setUp() {
        testCreator = new User();
        testCreator.setId(1L);
        testCreator.setUsername("creator");
        testCreator.setAvailablePoints(1000);
        testCreator.setHeldPoints(0);

        testAcceptor = new User();
        testAcceptor.setId(2L);
        testAcceptor.setUsername("acceptor");
        testAcceptor.setAvailablePoints(1000);
        testAcceptor.setHeldPoints(0);

        testBet = new Bet(testCreator, 100, "Ludo", "Test Bet");
        testBet.setId(1L);
        testBet.setStatus(BetStatus.PENDING);

        // Try below if above not working
        //testBet = new Bet();
        //testBet.setId(1L);
        //testBet.setCreator(testCreator);
        //testBet.setPoints(100);
        //testBet.setGame("Ludo");
        //testBet.setTitle("Test Bet");
        //testBet.setStatus(BetStatus.PENDING);
    }

    @Test
    void createBet_WithValidData_ShouldCreateBet() {
        // Arrange
        when(entityManager.find(User.class, 1L)).thenReturn(testCreator);
        when(betRepository.save(any(Bet.class))).thenReturn(testBet);
        when(appConfig.getPlatformFeeWithReferral()).thenReturn(0.03);
        when(appConfig.getPlatformFeeWithoutReferral()).thenReturn(0.04);
        when(appConfig.getReferralCommission()).thenReturn(0.01);

        // Act
        Bet result = betService.createBet(1L, 100, "Ludo", "Test Bet", "Test Description");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Bet");
        assertThat(result.getPoints()).isEqualTo(100);
        verify(betRepository).save(any(Bet.class));
        verify(transactionService).recordBetCreation(any(Bet.class));
        //:CHECK verify(messagingTemplate).convertAndSend(anyString(), any());
    }

    @Test
    void createBet_WithInsufficientPoints_ShouldThrowException() {
        // Arrange
        testCreator.setAvailablePoints(50);
        when(entityManager.find(User.class, 1L)).thenReturn(testCreator);

        // Act & Assert
        assertThatThrownBy(() -> betService.createBet(1L, 100, "Ludo", "Test Bet", "Test Description"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient points");
    }

    @Test
    void acceptBet_WithValidBet_ShouldAcceptBet() {
        // Arrange
        testBet.setStatus(BetStatus.PENDING);
        when(betRepository.findByIdWithAcceptor(1L)).thenReturn(Optional.of(testBet));
        when(entityManager.find(User.class, 2L)).thenReturn(testAcceptor);
        when(betRepository.save(any(Bet.class))).thenReturn(testBet);

        // Act
        Bet result = betService.acceptBet(1L, 2L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(BetStatus.ACCEPTED);
        assertThat(result.getAcceptor()).isEqualTo(testAcceptor);
        verify(betRepository).save(testBet);
        verify(transactionService).recordBetAcceptance(testBet);
    }

    @Test
    void acceptBet_WithNonPendingBet_ShouldThrowException() {
        // Arrange
        testBet.setStatus(BetStatus.ACCEPTED);
        when(betRepository.findByIdWithAcceptor(1L)).thenReturn(Optional.of(testBet));

        // Act & Assert
        assertThatThrownBy(() -> betService.acceptBet(1L, 2L)) // replace 2L with testAcceptor
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void acceptBet_WithOwnBet_ShouldThrowException() {
        // Arrange
        when(betRepository.findByIdWithAcceptor(1L)).thenReturn(Optional.of(testBet));
        when(entityManager.find(User.class, 1L)).thenReturn(testCreator);

        // Act & Assert
        assertThatThrownBy(() -> betService.acceptBet(1L, 1L)) // replace 2nd 1L with testAcceptor
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("your own bet");
    }

    @Test
    void setGameCode_AsCreator_ShouldSetGameCode() {
        // Arrange
        testBet.setStatus(BetStatus.ACCEPTED);
        testBet.setAcceptor(testAcceptor);
        when(betRepository.findById(1L)).thenReturn(Optional.of(testBet));
        when(betRepository.save(any(Bet.class))).thenReturn(testBet);

        // Act
        Bet result = betService.setGameCode(1L, "GAME123", "creator");

        // Assert
        assertThat(result.getGameCode()).isEqualTo("GAME123");
        assertThat(result.getStatus()).isEqualTo(BetStatus.CODE_SHARED);
        verify(betRepository).save(testBet);
    }

    @Test
    void setGameCode_AsNonCreator_ShouldThrowException() {
        // Arrange
        testBet.setStatus(BetStatus.ACCEPTED);
        when(betRepository.findById(1L)).thenReturn(Optional.of(testBet));

        // Act & Assert
        assertThatThrownBy(() -> betService.setGameCode(1L, "GAME123", "acceptor"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only creator can set game code");
    }

    @Test
    void submitResult_WithValidData_ShouldSubmitResult() {
        // Arrange
        testBet.setStatus(BetStatus.CODE_SHARED);
        testBet.setAcceptor(testAcceptor);
        when(betRepository.findById(1L)).thenReturn(Optional.of(testBet));
        when(betRepository.save(any(Bet.class))).thenReturn(testBet);

        // Act
        betService.submitResult(1L, "creator", "WIN", "screenshot.jpg", true);

        // Assert
        assertThat(testBet.getCreatorResult()).isEqualTo(Result.WIN);
        verify(betRepository).save(testBet);
    }

    @Test
    void submitResult_WithBothResults_ShouldResolveBet() {
        // Arrange
        testBet.setStatus(BetStatus.CODE_SHARED);
        testBet.setAcceptor(testAcceptor);
        testBet.setCreatorResult(Result.WIN);
        when(betRepository.findById(1L)).thenReturn(Optional.of(testBet));
        when(betRepository.save(any(Bet.class))).thenReturn(testBet);
        when(userService.saveUser(any(User.class))).thenReturn(testCreator);

        // Act
        betService.submitResult(1L, "acceptor", "LOSE", null, false);

        // Assert
        assertThat(testBet.getAcceptorResult()).isEqualTo(Result.LOSE);
        verify(betRepository, times(2)).save(testBet);
    }

    @Test
    void resolveBet_WithClearWinner_ShouldAwardPoints() {
        // Arrange
        testBet.setCreatorResult(Result.WIN);
        testBet.setAcceptorResult(Result.LOSE);
        testBet.setAcceptor(testAcceptor);
        
        when(referralService.hasActiveReferrer(testCreator)).thenReturn(false);
        when(appConfig.getPlatformFeeWithoutReferral()).thenReturn(0.04);
        when(userService.saveUser(any(User.class))).thenReturn(testCreator);

        // Act
        betService.resolveBet(testBet);

        // Assert
        assertThat(testBet.getStatus()).isEqualTo(BetStatus.COMPLETED);
        assertThat(testCreator.getWins()).isEqualTo(1);
        assertThat(testAcceptor.getLosses()).isEqualTo(1);
        verify(transactionService).recordBetWin(testBet, testCreator);
    }

    @Test
    void resolveBet_WithDispute_ShouldMarkAsDisputed() {
        // Arrange
        testBet.setCreatorResult(Result.WIN);
        testBet.setAcceptorResult(Result.WIN);
        testBet.setAcceptor(testAcceptor);
        
        when(userService.saveUser(any(User.class))).thenReturn(testCreator);

        // Act
        betService.resolveBet(testBet);

        // Assert
        assertThat(testBet.getStatus()).isEqualTo(BetStatus.DISPUTED);
        verify(transactionService).recordBetRefund(testBet);
    }

    @Test
    void cancelBet_ByCreator_ShouldCancelBet() {
        // Arrange
        testBet.setStatus(BetStatus.PENDING);
        when(betRepository.findById(1L)).thenReturn(Optional.of(testBet));
        when(userService.findByUsername("creator")).thenReturn(Optional.of(testCreator));
        when(userService.saveUser(any(User.class))).thenReturn(testCreator);

        // Act
        betService.cancelBet(1L, "creator");

        // Assert
        assertThat(testBet.getStatus()).isEqualTo(BetStatus.CANCELLED);
        verify(transactionService).recordBetRefund(testBet);
    }

    @Test
    void findAvailableBets_ShouldReturnPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Bet> expectedPage = new PageImpl<>(List.of(testBet));
        when(betRepository.findAvailableBets(BetStatus.PENDING, 1L, pageable)).thenReturn(expectedPage);

        // Act
        Page<Bet> result = betService.findAvailableBets(BetStatus.PENDING, 1L, pageable);

        // Assert
        assertThat(result).isEqualTo(expectedPage);
        verify(betRepository).findAvailableBets(BetStatus.PENDING, 1L, pageable);
    }

    @Test
    void findById_WithExistingBet_ShouldReturnBet() {
        // Arrange
        when(betRepository.findByIdWithAcceptor(1L)).thenReturn(Optional.of(testBet));

        // Act
        Optional<Bet> result = betService.findById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testBet);
    }

    @Test
    void canUserSubmitResult_WithValidConditions_ShouldReturnTrue() {
        // Arrange
        testBet.setStatus(BetStatus.CODE_SHARED);
        when(betRepository.findByIdWithAcceptor(1L)).thenReturn(Optional.of(testBet));

        // Act
        boolean result = betService.canUserSubmitResult(1L, "creator", true);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void canUserShareCode_WithValidConditions_ShouldReturnTrue() {
        // Arrange
        testBet.setStatus(BetStatus.ACCEPTED);
        when(betRepository.findByIdWithAcceptor(1L)).thenReturn(Optional.of(testBet));

        // Act
        boolean result = betService.canUserShareCode(1L, "creator");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void canUserCancelBet_WithValidConditions_ShouldReturnTrue() {
        // Arrange
        testBet.setStatus(BetStatus.PENDING);
        when(betRepository.findByIdWithAcceptor(1L)).thenReturn(Optional.of(testBet));
        when(userService.findByUsername("creator")).thenReturn(Optional.of(testCreator));

        // Act
        boolean result = betService.canUserCancelBet(1L, "creator");

        // Assert
        assertThat(result).isTrue();
    }
}