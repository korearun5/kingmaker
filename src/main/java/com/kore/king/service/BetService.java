package com.kore.king.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kore.king.config.AppConfig;
import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.entity.Result;
import com.kore.king.entity.Transaction;
import com.kore.king.entity.TransactionType;
import com.kore.king.entity.User;
import com.kore.king.repository.BetRepository;
import com.kore.king.repository.TransactionRepository;

import jakarta.persistence.EntityManager;

@Service
@Transactional
public class BetService {
    
    private final BetRepository betRepository;
    private final UserService userService;
    private final TransactionService transactionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ReferralService referralService;
    private final AppConfig appConfig;
    private final TransactionRepository transactionRepository;
    private final EntityManager entityManager;
    
    public BetService(BetRepository betRepository, UserService userService,
                     TransactionService transactionService, SimpMessagingTemplate messagingTemplate,
                     ReferralService referralService, AppConfig appConfig,
                     TransactionRepository transactionRepository, EntityManager entityManager) {
        this.betRepository = betRepository;
        this.userService = userService;
        this.transactionService = transactionService;
        this.messagingTemplate = messagingTemplate;
        this.referralService = referralService;
        this.appConfig = appConfig;
        this.transactionRepository = transactionRepository;
        this.entityManager = entityManager;
    }

    // Basic CRUD operations
    public Page<Bet> findAvailableBets(BetStatus status, Long userId, Pageable pageable) {
        return betRepository.findAvailableBets(status, userId, pageable);
    }
    
    public Page<Bet> findUserBets(Long userId, Pageable pageable) {
        return betRepository.findUserBets(userId, pageable);
    }
    
    public List<Bet> getUserActiveBets(Long userId) {
        return betRepository.findUserActiveBets(userId);
    }
    
    public Optional<Bet> findById(Long betId) {
        return betRepository.findByIdWithAcceptor(betId);
    }

    public List<Bet> findAcceptedBetsByUser(Long userId) {
        return betRepository.findByStatusAndUserId(BetStatus.ACCEPTED, userId);
    }
    
    public List<Bet> findActiveBetsByUser(Long userId) {
        return betRepository.findActiveBetsByUser(userId);
    }

    @Transactional
    public Bet createBet(Long creatorId, Integer points, String gameType, String title, String description) {
        User creator = entityManager.find(User.class, creatorId);
        if (creator == null) {
            throw new RuntimeException("User not found");
        }

        System.out.println("Creating bet for user: " + creator.getUsername() + " with " + points + " points");
        
        if (!creator.canAffordBet(points)) {
            throw new RuntimeException("Insufficient points. Available: " + creator.getAvailablePoints());
        }

        creator.holdPoints(points);
        
        Bet bet = new Bet(creator, points, gameType, title);
        bet.setDescription(description);
        Bet savedBet = betRepository.save(bet);
        
        System.out.println("Bet created successfully - ID: " + savedBet.getId() + 
                        ", Status: " + savedBet.getStatus() + 
                        ", Creator: " + savedBet.getCreator().getUsername());
        
        transactionService.recordBetCreation(savedBet);
        
        broadcastNewBet(savedBet);
        
        return savedBet;
    }

    @Transactional
    public Bet acceptBet(Long betId, Long acceptorId) {
        Bet bet = betRepository.findByIdWithAcceptor(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        User acceptor = entityManager.find(User.class, acceptorId);
        if (acceptor == null) {
            throw new RuntimeException("User not found");
        }

        System.out.println("Accepting bet " + betId + " by user " + acceptor.getUsername());
        System.out.println("Current bet status: " + bet.getStatus());
        System.out.println("Bet creator: " + bet.getCreator().getUsername());

        if (bet.getStatus() != BetStatus.PENDING) {
            throw new RuntimeException("Bet is no longer available. Current status: " + bet.getStatus());
        }
        
        if (bet.getCreator().getId().equals(acceptorId)) {
            throw new RuntimeException("You cannot accept your own bet");
        }
        
        if (!acceptor.canAffordBet(bet.getPoints())) {
            throw new RuntimeException("Insufficient points to accept this bet");
        }
        
        acceptor.holdPoints(bet.getPoints());
        
        bet.setAcceptor(acceptor);
        bet.setStatus(BetStatus.ACCEPTED);
        Bet updatedBet = betRepository.save(bet);
        
        System.out.println("Bet accepted successfully. New status: " + updatedBet.getStatus());
        System.out.println("Creator: " + updatedBet.getCreator().getUsername());
        System.out.println("Acceptor: " + updatedBet.getAcceptor().getUsername());
        
        transactionService.recordBetAcceptance(updatedBet);
        
        broadcastBetAccepted(updatedBet);
        
        return updatedBet;
    }

    public boolean canCancelBet(Bet bet, User currentUser) {
        return bet.getStatus() == BetStatus.PENDING && 
               bet.getCreator().getId().equals(currentUser.getId());
    }
    
    public boolean canShareCode(Bet bet, User currentUser) {
        return bet.getStatus() == BetStatus.ACCEPTED && 
               bet.getCreator().getId().equals(currentUser.getId()) &&
               bet.getCodeSharedAt() == null;
    }

    @Retryable(value = { OptimisticLockingFailureException.class }, maxAttempts = 3)
    @Transactional
    public void resolveBetWithRetry(Long betId) {
        Bet bet = betRepository.findByIdWithAcceptor(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        resolveBet(bet);
    }

    @Transactional
    public Bet setGameCode(Long betId, String gameCode, String username) {
        Bet bet = betRepository.findById(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        if (!bet.getCreator().getUsername().equals(username)) {
            throw new RuntimeException("Only creator can set game code");
        }
        
        if (bet.getStatus() != BetStatus.ACCEPTED) {
            throw new RuntimeException("Bet must be accepted before setting game code");
        }
        
        if (gameCode == null || gameCode.trim().isEmpty()) {
            throw new RuntimeException("Game code cannot be empty");
        }
        
        bet.setGameCode(gameCode.trim());
        bet.setStatus(BetStatus.CODE_SHARED);
        bet.setCodeSharedAt(LocalDateTime.now());
        
        Bet updatedBet = betRepository.save(bet);
        
        broadcastCodeShared(updatedBet);
        
        return updatedBet;
    }

    @Transactional
    public void submitResult(Long betId, String username, String result, String screenshot, boolean isCreator) {
        Bet bet = betRepository.findById(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        if (bet.getStatus() != BetStatus.CODE_SHARED) {
            throw new RuntimeException("Game code must be shared before submitting results");
        }
        
        if ((isCreator && bet.getCreatorResult() != null) || 
            (!isCreator && bet.getAcceptorResult() != null)) {
            throw new RuntimeException("You have already submitted your result");
        }
        
        Result resultEnum = Result.valueOf(result.toUpperCase());
        if (isCreator) {
            bet.setCreatorResult(resultEnum);
        } else {
            bet.setAcceptorResult(resultEnum);
        }
        
        if (resultEnum == Result.WIN && screenshot != null) {
            bet.setWinnerScreenshot(screenshot);
        }
        
        if (bet.bothResultsSubmitted()) {
            resolveBet(bet);
        } else {
            bet.setStatus(BetStatus.RESULTS_SUBMITTED);
        }
        
        betRepository.save(bet);
        
        broadcastResultUpdate(bet, username, result);
    }

    @Transactional
    public void resolveBet(Bet bet) {
        User winner = bet.determineWinner();
        int totalPot = bet.getPoints() * 2;
        
        if (winner != null) {
            double platformFeeRate;
            if (referralService.hasActiveReferrer(winner)) {
                platformFeeRate = appConfig.getPlatformFeeWithReferral();
            } else {
                platformFeeRate = appConfig.getPlatformFeeWithoutReferral();
            }
            
            int platformFee = (int) (totalPot * platformFeeRate);
            int winnerAmount = totalPot - platformFee;
            
            winner.awardPoints(winnerAmount);
            
            if (referralService.hasActiveReferrer(winner)) {
                referralService.awardReferralCommission(winner, totalPot);
            }
            
            if (winner.getId().equals(bet.getCreator().getId())) {
                bet.getCreator().setWins(bet.getCreator().getWins() + 1);
                bet.getAcceptor().setLosses(bet.getAcceptor().getLosses() + 1);
            } else {
                bet.getAcceptor().setWins(bet.getAcceptor().getWins() + 1);
                bet.getCreator().setLosses(bet.getCreator().getLosses() + 1);
            }
            
            userService.saveUser(bet.getCreator());
            userService.saveUser(bet.getAcceptor());
            
            transactionService.recordBetWin(bet, winner);
            
        } else {
            bet.getCreator().releasePoints(bet.getPoints());
            bet.getAcceptor().releasePoints(bet.getPoints());
            bet.setStatus(BetStatus.DISPUTED);
            bet.setDisputeReason("Results conflict - both players claimed same result");
            
            userService.saveUser(bet.getCreator());
            userService.saveUser(bet.getAcceptor());
            
            transactionService.recordBetRefund(bet);
        }
        
        bet.setStatus(BetStatus.COMPLETED);
        bet.setCompletedAt(LocalDateTime.now());
        betRepository.save(bet);
        
        broadcastBetCompleted(bet);
    }

    @Transactional
    public void cancelBet(Long betId, String username) {
        Bet bet = betRepository.findById(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!bet.isUserParticipant(user)) {
            throw new RuntimeException("You are not involved in this bet");
        }
        
        if (bet.getStatus() != BetStatus.PENDING && bet.getStatus() != BetStatus.ACCEPTED) {
            throw new RuntimeException("Cannot cancel bet in current state: " + bet.getStatus());
        }
        
        if (bet.getStatus() == BetStatus.PENDING) {
            if (!bet.isCreator(user)) {
                throw new RuntimeException("Only creator can cancel pending bets");
            }
            bet.getCreator().releasePoints(bet.getPoints());
            userService.saveUser(bet.getCreator());
            transactionService.recordBetRefund(bet);
            
        } else if (bet.getStatus() == BetStatus.ACCEPTED) {
            bet.getCreator().releasePoints(bet.getPoints());
            bet.getAcceptor().releasePoints(bet.getPoints());
            userService.saveUser(bet.getCreator());
            userService.saveUser(bet.getAcceptor());
            
            transactionService.recordBetRefund(bet);
        }
        
        bet.setStatus(BetStatus.CANCELLED);
        bet.setCancelReason("Cancelled by " + username);
        betRepository.save(bet);
        
        broadcastBetCancelled(bet, username);
    }

    public boolean canUserSubmitResult(Long betId, String username, boolean isCreator) {
        Optional<Bet> betOpt = betRepository.findByIdWithAcceptor(betId);
        if (betOpt.isEmpty()) {
            return false;
        }
        
        Bet bet = betOpt.get();
        
        if (bet.getStatus() != BetStatus.CODE_SHARED) {
            return false;
        }
        
        if (isCreator) {
            return bet.getCreatorResult() == null;
        } else {
            return bet.getAcceptorResult() == null;
        }
    }
    
    public boolean canUserShareCode(Long betId, String username) {
        Optional<Bet> betOpt = betRepository.findByIdWithAcceptor(betId);
        if (betOpt.isEmpty()) {
            return false;
        }
        
        Bet bet = betOpt.get();
        
        if (!bet.getCreator().getUsername().equals(username)) {
            return false;
        }
        
        return bet.getStatus() == BetStatus.ACCEPTED;
    }
    
    public boolean canUserCancelBet(Long betId, String username) {
        Optional<Bet> betOpt = betRepository.findByIdWithAcceptor(betId);
        if (betOpt.isEmpty()) {
            return false;
        }
        
        Bet bet = betOpt.get();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!bet.isUserParticipant(user)) {
            return false;
        }
        
        return bet.getStatus() == BetStatus.PENDING || bet.getStatus() == BetStatus.ACCEPTED;
    }

    // Add these methods to BetService.java

private static final Logger logger = LoggerFactory.getLogger(BetService.class);

private void broadcastNewBet(Bet bet) {
    logger.info("📢 Broadcasting NEW_BET to all users. Bet: {}, Creator: {}", 
        bet.getId(), bet.getCreator().getUsername());
    
    Map<String, Object> message = new HashMap<>();
    message.put("type", "NEW_BET");
    message.put("bet", createBetDTO(bet));
    message.put("timestamp", System.currentTimeMillis());
    
    messagingTemplate.convertAndSend("/topic/bets/all", message);
    logger.info("✅ NEW_BET broadcast complete");
}

private void broadcastBetAccepted(Bet bet) {
    logger.info("📢 Broadcasting BET_ACCEPTED. Bet: {}, Acceptor: {}, Creator: {}", 
        bet.getId(), bet.getAcceptor().getUsername(), bet.getCreator().getUsername());
    
    Map<String, Object> message = new HashMap<>();
    message.put("type", "BET_ACCEPTED");
    message.put("bet", createBetDTO(bet));
    message.put("timestamp", System.currentTimeMillis());
    
    // Notify both users specifically
    if (bet.getCreator() != null) {
        messagingTemplate.convertAndSendToUser(
            bet.getCreator().getUsername(),
            "/queue/notifications",
            message
        );
        logger.info("✅ Notified creator: {}", bet.getCreator().getUsername());
    }
    
    if (bet.getAcceptor() != null) {
        messagingTemplate.convertAndSendToUser(
            bet.getAcceptor().getUsername(), 
            "/queue/notifications",
            message
        );
        logger.info("✅ Notified acceptor: {}", bet.getAcceptor().getUsername());
    }
    
    // Also broadcast to the bet room
    messagingTemplate.convertAndSend("/topic/bet/" + bet.getId(), message);
}
    
    private void broadcastCodeShared(Bet bet) {
        Map<String, Object> betDTO = createBetDTO(bet);
        
        // Specifically notify the acceptor
        if (bet.getAcceptor() != null) {
            broadcastToUser(bet.getAcceptor().getUsername(), "CODE_SHARED", betDTO);
        }
        
        broadcastToBetParticipants(bet, "CODE_SHARED", betDTO);
    }
    
    private void broadcastResultUpdate(Bet bet, String username, String result) {
        Map<String, Object> update = new HashMap<>();
        update.put("betId", bet.getId());
        update.put("username", username);
        update.put("result", result);
        update.put("bothSubmitted", bet.bothResultsSubmitted());
        
        // Notify the other participant
        String otherUser = bet.getCreator().getUsername().equals(username) ? 
            (bet.getAcceptor() != null ? bet.getAcceptor().getUsername() : null) : 
            bet.getCreator().getUsername();
        
        if (otherUser != null) {
            broadcastToUser(otherUser, "RESULT_SUBMITTED", update);
        }
        
        broadcastToBetParticipants(bet, "RESULT_SUBMITTED", update);
    }
    private void broadcastBetCancelled(Bet bet, String cancelledBy) {
        Map<String, Object> betDTO = createBetDTO(bet);
        betDTO.put("cancelledBy", cancelledBy);
        
        broadcastToBetParticipants(bet, "BET_CANCELLED", betDTO);
    }
    
    private void broadcastBetCompleted(Bet bet) {
        Map<String, Object> betDTO = createBetDTO(bet);
        
        broadcastToBetParticipants(bet, "BET_COMPLETED", betDTO);
        
        // Also update points for both users
        if (bet.getCreator() != null) {
            Map<String, Object> pointsUpdate = new HashMap<>();
            pointsUpdate.put("availablePoints", bet.getCreator().getAvailablePoints());
            pointsUpdate.put("heldPoints", bet.getCreator().getHeldPoints());
            broadcastToUser(bet.getCreator().getUsername(), "POINTS_UPDATED", pointsUpdate);
        }
        if (bet.getAcceptor() != null) {
            Map<String, Object> pointsUpdate = new HashMap<>();
            pointsUpdate.put("availablePoints", bet.getAcceptor().getAvailablePoints());
            pointsUpdate.put("heldPoints", bet.getAcceptor().getHeldPoints());
            broadcastToUser(bet.getAcceptor().getUsername(), "POINTS_UPDATED", pointsUpdate);
        }
    }

    // Helper method to create Bet DTO - ADD THIS METHOD
    private Map<String, Object> createBetDTO(Bet bet) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", bet.getId());
        dto.put("title", bet.getTitle());
        dto.put("description", bet.getDescription());
        dto.put("points", bet.getPoints());
        dto.put("status", bet.getStatus());
        dto.put("gameCode", bet.getGameCode());
        dto.put("gameType", bet.getGameType());
        dto.put("createdAt", bet.getCreatedAt());
        dto.put("codeSharedAt", bet.getCodeSharedAt());
        dto.put("completedAt", bet.getCompletedAt());
        dto.put("creatorResult", bet.getCreatorResult());
        dto.put("acceptorResult", bet.getAcceptorResult());
        
        if (bet.getCreator() != null) {
            Map<String, Object> creatorInfo = new HashMap<>();
            creatorInfo.put("id", bet.getCreator().getId());
            creatorInfo.put("username", bet.getCreator().getUsername());
            creatorInfo.put("availablePoints", bet.getCreator().getAvailablePoints());
            dto.put("creator", creatorInfo);
        }
        
        if (bet.getAcceptor() != null) {
            Map<String, Object> acceptorInfo = new HashMap<>();
            acceptorInfo.put("id", bet.getAcceptor().getId());
            acceptorInfo.put("username", bet.getAcceptor().getUsername());
            acceptorInfo.put("availablePoints", bet.getAcceptor().getAvailablePoints());
            dto.put("acceptor", acceptorInfo);
        }
        
        return dto;
    }

    public List<Bet> findByStatus(BetStatus status) {
        return betRepository.findByStatus(status);
    }

    @Transactional
    public void resolveBetWithCommission(Bet bet) {
        User winner = bet.determineWinner();
        int totalPot = bet.getPoints() * 2;
        
        if (winner != null) {
            double platformFeeRate = hasReferrer(winner) ? 0.03 : 0.04;
            int platformFee = (int) (totalPot * platformFeeRate);
            int winnerAmount = totalPot - platformFee;
            
            winner.awardPoints(winnerAmount);
            
            if (hasReferrer(winner)) {
                int referralBonus = (int) (totalPot * 0.01);
                awardReferralBonus(winner, referralBonus);
            }
            
            recordPlatformFee(platformFee);
            
            updateUserStats(bet, winner);
        } else {
            handleDispute(bet);
        }
    }
    
    private boolean hasReferrer(User winner) {
        return referralService.hasActiveReferrer(winner);
    }

    private void awardReferralBonus(User winner, int referralBonus) {
        referralService.awardReferralCommission(winner, referralBonus);
    }
    
    private void recordPlatformFee(int platformFee) {
        Transaction platformTransaction = new Transaction();
        platformTransaction.setPoints(platformFee);
        platformTransaction.setType(TransactionType.PLATFORM_FEE);
        platformTransaction.setDescription("Platform fee collected");
        platformTransaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(platformTransaction);
    }
    
    private void updateUserStats(Bet bet, User winner) {
        if (winner.getId().equals(bet.getCreator().getId())) {
            bet.getCreator().setWins(bet.getCreator().getWins() + 1);
            bet.getAcceptor().setLosses(bet.getAcceptor().getLosses() + 1);
        } else {
            bet.getAcceptor().setWins(bet.getAcceptor().getWins() + 1);
            bet.getCreator().setLosses(bet.getCreator().getLosses() + 1);
        }
        
        userService.saveUser(bet.getCreator());
        userService.saveUser(bet.getAcceptor());
    }
    
    private void handleDispute(Bet bet) {
        bet.getCreator().releasePoints(bet.getPoints());
        if (bet.getAcceptor() != null) {
            bet.getAcceptor().releasePoints(bet.getPoints());
        }
        
        bet.setStatus(BetStatus.DISPUTED);
        bet.setDisputeReason("Results conflict - requires admin review");
        
        userService.saveUser(bet.getCreator());
        if (bet.getAcceptor() != null) {
            userService.saveUser(bet.getAcceptor());
        }
        
        transactionService.recordBetRefund(bet);
    }

    public Page<Bet> findAllBetsForUser(Long userId, Pageable pageable) {
        return betRepository.findAllBetsForUser(userId, pageable);
    }
    
    public Page<Bet> findAllBetsForUserWithAvailable(Long userId, Pageable pageable) {
        return betRepository.findAllBetsForUserWithAvailable(userId, pageable);
    }
    
    public List<Bet> findByStatusInAndUserId(List<BetStatus> statuses, Long userId) {
        return betRepository.findByStatusInAndUserId(statuses, userId);
    }
    // Add these methods to your existing BetService.java

    private void broadcastToUser(String username, String type, Object payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("payload", payload);
        message.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
    }

    private void broadcastToBetParticipants(Bet bet, String type, Object payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("payload", payload);
        message.put("betId", bet.getId());
        message.put("timestamp", System.currentTimeMillis());
        
        // Send to bet-specific topic
        messagingTemplate.convertAndSend("/topic/bet/" + bet.getId(), message);
        
        // Send to individual users
        if (bet.getCreator() != null) {
            messagingTemplate.convertAndSendToUser(
                bet.getCreator().getUsername(), 
                "/queue/bet-updates", 
                message
            );
        }
        if (bet.getAcceptor() != null) {
            messagingTemplate.convertAndSendToUser(
                bet.getAcceptor().getUsername(), 
                "/queue/bet-updates", 
                message
            );
        }
    }
}