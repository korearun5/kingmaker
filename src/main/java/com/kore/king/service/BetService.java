package com.kore.king.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.entity.Result;
import com.kore.king.entity.User;
import com.kore.king.repository.BetRepository;

@Service
@Transactional
public class BetService {
    
    @Autowired
    private BetRepository betRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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

    // Bet creation
    @Transactional
    public Bet createBet(User creator, Integer points, String gameType, String title, String description) {
        // Validate and hold points
        if (!creator.canAffordBet(points)) {
            throw new RuntimeException("Insufficient points. Available: " + creator.getAvailablePoints());
        }
        
        creator.holdPoints(points);
        userService.saveUser(creator);
        
        // Create bet
        Bet bet = new Bet(creator, points, gameType, title);
        bet.setDescription(description);
        Bet savedBet = betRepository.save(bet);
        
        // Record transaction
        transactionService.recordBetCreation(savedBet);
        
        // Broadcast new bet
        broadcastNewBet(savedBet);
        
        return savedBet;
    }

    // Bet acceptance
    @Transactional
    public Bet acceptBet(Long betId, User acceptor) {
        Bet bet = betRepository.findById(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        // Validations
        if (bet.getStatus() != BetStatus.PENDING) {
            throw new RuntimeException("Bet is no longer available");
        }
        
        if (bet.getCreator().getId().equals(acceptor.getId())) {
            throw new RuntimeException("You cannot accept your own bet");
        }
        
        if (!acceptor.canAffordBet(bet.getPoints())) {
            throw new RuntimeException("Insufficient points to accept this bet");
        }
        
        // Hold points for acceptor
        acceptor.holdPoints(bet.getPoints());
        userService.saveUser(acceptor);
        
        // Update bet
        bet.setAcceptor(acceptor);
        bet.setStatus(BetStatus.ACCEPTED);
        Bet updatedBet = betRepository.save(bet);
        
        // Record transaction
        transactionService.recordBetAcceptance(updatedBet);
        
        // Broadcast acceptance
        broadcastBetAccepted(updatedBet);
        
        return updatedBet;
    }

    // Game code sharing
    @Transactional
    public Bet setGameCode(Long betId, String gameCode, String username) {
        Bet bet = betRepository.findById(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        // Validations
        if (!bet.getCreator().getUsername().equals(username)) {
            throw new RuntimeException("Only creator can set game code");
        }
        
        if (bet.getStatus() != BetStatus.ACCEPTED) {
            throw new RuntimeException("Bet must be accepted before setting game code");
        }
        
        if (gameCode == null || gameCode.trim().isEmpty()) {
            throw new RuntimeException("Game code cannot be empty");
        }
        
        // Update bet
        bet.setGameCode(gameCode.trim());
        bet.setStatus(BetStatus.CODE_SHARED);
        bet.setCodeSharedAt(LocalDateTime.now());
        
        Bet updatedBet = betRepository.save(bet);
        
        // Broadcast code sharing
        broadcastCodeShared(updatedBet);
        
        return updatedBet;
    }

    // Result submission
    @Transactional
    public void submitResult(Long betId, String username, String result, String screenshot, boolean isCreator) {
        Bet bet = betRepository.findById(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        // Validations
        if (bet.getStatus() != BetStatus.CODE_SHARED) {
            throw new RuntimeException("Game code must be shared before submitting results");
        }
        
        // Check if user has already submitted
        if ((isCreator && bet.getCreatorResult() != null) || 
            (!isCreator && bet.getAcceptorResult() != null)) {
            throw new RuntimeException("You have already submitted your result");
        }
        
        // Update result
        Result resultEnum = Result.valueOf(result.toUpperCase());
        if (isCreator) {
            bet.setCreatorResult(resultEnum);
        } else {
            bet.setAcceptorResult(resultEnum);
        }
        
        // Handle screenshot for WIN claims
        if (resultEnum == Result.WIN && screenshot != null) {
            bet.setWinnerScreenshot(screenshot);
        }
        
        // Check if both results are submitted
        if (bet.bothResultsSubmitted()) {
            resolveBet(bet);
        } else {
            bet.setStatus(BetStatus.RESULTS_SUBMITTED);
        }
        
        betRepository.save(bet);
        
        // Broadcast result submission
        broadcastResultUpdate(bet, username, result);
    }

    // Bet resolution
    @Transactional
    public void resolveBet(Bet bet) {
        User winner = bet.determineWinner();
        
        if (winner != null) {
            // Winner gets both stakes
            winner.awardPoints(bet.getPoints() * 2);
            
            // Update user statistics
            if (winner.getId().equals(bet.getCreator().getId())) {
                bet.getCreator().setWins(bet.getCreator().getWins() + 1);
                bet.getAcceptor().setLosses(bet.getAcceptor().getLosses() + 1);
            } else {
                bet.getAcceptor().setWins(bet.getAcceptor().getWins() + 1);
                bet.getCreator().setLosses(bet.getCreator().getLosses() + 1);
            }
            
            userService.saveUser(bet.getCreator());
            userService.saveUser(bet.getAcceptor());
            
            // Record win transaction
            transactionService.recordBetWin(bet, winner);
            
        } else {
            // Dispute or invalid results - refund both
            bet.getCreator().releasePoints(bet.getPoints());
            bet.getAcceptor().releasePoints(bet.getPoints());
            bet.setStatus(BetStatus.DISPUTED);
            bet.setDisputeReason("Results conflict - both players claimed same result");
            
            userService.saveUser(bet.getCreator());
            userService.saveUser(bet.getAcceptor());
            
            // Record refund transactions
            transactionService.recordBetRefund(bet);
        }
        
        bet.setStatus(BetStatus.COMPLETED);
        bet.setCompletedAt(LocalDateTime.now());
        betRepository.save(bet);
        
        // Broadcast completion
        broadcastBetCompleted(bet);
    }

    // Bet cancellation
    @Transactional
    public void cancelBet(Long betId, String username) {
        Bet bet = betRepository.findById(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate user can cancel
        if (!bet.isUserParticipant(user)) {
            throw new RuntimeException("You are not involved in this bet");
        }
        
        if (bet.getStatus() != BetStatus.PENDING && bet.getStatus() != BetStatus.ACCEPTED) {
            throw new RuntimeException("Cannot cancel bet in current state: " + bet.getStatus());
        }
        
        // Handle cancellation based on status
        if (bet.getStatus() == BetStatus.PENDING) {
            // Only creator can cancel pending bets
            if (!bet.isCreator(user)) {
                throw new RuntimeException("Only creator can cancel pending bets");
            }
            bet.getCreator().releasePoints(bet.getPoints());
            userService.saveUser(bet.getCreator());
            transactionService.recordBetRefund(bet);
            
        } else if (bet.getStatus() == BetStatus.ACCEPTED) {
            // Both players get refund
            bet.getCreator().releasePoints(bet.getPoints());
            bet.getAcceptor().releasePoints(bet.getPoints());
            userService.saveUser(bet.getCreator());
            userService.saveUser(bet.getAcceptor());
            
            transactionService.recordBetRefund(bet);
        }
        
        bet.setStatus(BetStatus.CANCELLED);
        bet.setCancelReason("Cancelled by " + username);
        betRepository.save(bet);
        
        // Broadcast cancellation
        broadcastBetCancelled(bet, username);
    }

    // Helper methods
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
        
        // Only creator can share code
        if (!bet.getCreator().getUsername().equals(username)) {
            return false;
        }
        
        // Code can only be shared when bet is ACCEPTED
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
        
        // Can cancel in PENDING or ACCEPTED states
        return bet.getStatus() == BetStatus.PENDING || bet.getStatus() == BetStatus.ACCEPTED;
    }

    // Updated Broadcasting methods with DTOs
    private void broadcastNewBet(Bet bet) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_BET");
        message.put("bet", createBetDTO(bet));
        messagingTemplate.convertAndSend("/topic/bet-updates", message);
    }
    
    private void broadcastBetAccepted(Bet bet) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "BET_ACCEPTED");
        message.put("bet", createBetDTO(bet));
        messagingTemplate.convertAndSend("/topic/bet/" + bet.getId(), message);
        if (bet.getAcceptor() != null) {
            messagingTemplate.convertAndSend("/topic/user/" + bet.getAcceptor().getUsername(), message);
        }
    }
    
    private void broadcastCodeShared(Bet bet) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "CODE_SHARED");
        message.put("bet", createBetDTO(bet));
        messagingTemplate.convertAndSend("/topic/bet/" + bet.getId(), message);
    }
    
    private void broadcastResultUpdate(Bet bet, String username, String result) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "RESULT_SUBMITTED");
        message.put("betId", bet.getId());
        message.put("username", username);
        message.put("result", result);
        messagingTemplate.convertAndSend("/topic/bet/" + bet.getId(), message);
    }
    
    private void broadcastBetCompleted(Bet bet) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "BET_COMPLETED");
        message.put("bet", createBetDTO(bet));
        messagingTemplate.convertAndSend("/topic/bet/" + bet.getId(), message);
    }
    
    private void broadcastBetCancelled(Bet bet, String cancelledBy) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "BET_CANCELLED");
        message.put("bet", createBetDTO(bet));
        message.put("cancelledBy", cancelledBy);
        messagingTemplate.convertAndSend("/topic/bet/" + bet.getId(), message);
    }

    // Helper method to create a simplified DTO for Bet (prevents circular references)
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
        
        // Add creator info without circular references
        if (bet.getCreator() != null) {
            Map<String, Object> creatorInfo = new HashMap<>();
            creatorInfo.put("id", bet.getCreator().getId());
            creatorInfo.put("username", bet.getCreator().getUsername());
            creatorInfo.put("availablePoints", bet.getCreator().getAvailablePoints());
            dto.put("creator", creatorInfo);
        }
        
        // Add acceptor info without circular references
        if (bet.getAcceptor() != null) {
            Map<String, Object> acceptorInfo = new HashMap<>();
            acceptorInfo.put("id", bet.getAcceptor().getId());
            acceptorInfo.put("username", bet.getAcceptor().getUsername());
            acceptorInfo.put("availablePoints", bet.getAcceptor().getAvailablePoints());
            dto.put("acceptor", acceptorInfo);
        }
        
        return dto;
    }
}