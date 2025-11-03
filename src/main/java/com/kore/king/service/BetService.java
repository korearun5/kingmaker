package com.kore.king.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.entity.User;
import com.kore.king.repository.BetRepository;

@Service
public class BetService {
    
    @Autowired
    private BetRepository betRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<Bet> findAllOpenBets() {
        return betRepository.findByStatus(BetStatus.PENDING);
    }
    
    public Page<Bet> searchAvailableBets(BetStatus status, Long userId, String gameType, Integer points, Pageable pageable) {
        if ((gameType != null && !gameType.isEmpty()) || points != null) {
            return betRepository.searchAvailableBets(status, userId, gameType, points, pageable);
        } else {
            return betRepository.findAvailableBets(status, userId, pageable);
        }
    }
    
    public Page<Bet> findAvailableBets(BetStatus status, Long userId, Pageable pageable) {
        return betRepository.findAvailableBets(status, userId, pageable);
    }
    
    public List<Bet> findAvailableBets(User currentUser) {
        return betRepository.findByStatusAndCreatorNot(BetStatus.PENDING, currentUser);
    }
    
    public Page<Bet> findUserBets(Long userId, Pageable pageable) {
        return betRepository.findUserBets(userId, pageable);
    }
    
    // UPDATED: Remove MATCHED from active statuses
    public long findUserActiveBetsCount(Long userId) {
        List<BetStatus> activeStatuses = Arrays.asList(
            BetStatus.PENDING, 
            BetStatus.ACCEPTED, 
            BetStatus.CODE_SHARED
        );
        return betRepository.countUserActiveBets(userId, activeStatuses);
    }
    
    @Transactional
    public Bet createBet(Bet bet, String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return createBet(user, bet.getPoints(), bet.getGameType());
    }
    
    // MODIFIED: Remove userProvidedCode from creation - will be set after acceptance
    @Transactional
    public Bet createBet(User creator, Integer points, String gameType) {
        // Check if user has enough points
        if (creator.getPoints() < points) {
            throw new RuntimeException("Insufficient points");
        }
        
        // Reserve points by deducting them temporarily
        creator.setPoints(creator.getPoints() - points);
        userService.saveUser(creator);
        
        Bet bet = new Bet(creator, points, gameType);
        bet.setCreatorResult("PENDING");
        bet.setUserProvidedCode(""); // Will be set after acceptance
        
        Bet savedBet = betRepository.save(bet);
        
        // Record transaction
        transactionService.recordBetCreation(savedBet);
        
        // Try to auto-match
        tryAutoMatch(savedBet);
        
        // Broadcast new bet
        broadcastNewBet(savedBet);
        
        return savedBet;
    }

    // In your BetService, update these methods:

    @Transactional
    public Bet acceptBet(Long betId, User acceptor) {
        System.out.println("=== ACCEPT BET START ===");
        System.out.println("Bet ID: " + betId);
        System.out.println("Acceptor: " + acceptor.getUsername());

        Bet originalBet = betRepository.findById(betId)
            .orElseThrow(() -> {
                System.out.println("❌ Bet not found: " + betId);
                return new RuntimeException("Bet not found");
            });
        System.out.println("Original Bet Status: " + originalBet.getStatus());
        System.out.println("Original Bet Creator: " + originalBet.getCreator().getUsername());
        
        // Check if bet is still available
        if (originalBet.getStatus() != BetStatus.PENDING) {
            throw new RuntimeException("Bet is no longer available");
        }
        
        // Check if acceptor has enough points
        if (acceptor.getPoints() < originalBet.getPoints()) {
            throw new RuntimeException("Insufficient points to accept this bet");
        }
        
        // Reserve points for acceptor
        acceptor.setPoints(acceptor.getPoints() - originalBet.getPoints());
        userService.saveUser(acceptor);
        
        // Create acceptor's bet (THIS WAS MISSING!)
        Bet acceptorBet = new Bet(acceptor, originalBet.getPoints(), originalBet.getGameType());
        acceptorBet.setStatus(BetStatus.ACCEPTED);
        acceptorBet.setMatchedBet(originalBet);
        acceptorBet.setCreatorResult("PENDING");
        
        // Update original bet status
        originalBet.setStatus(BetStatus.ACCEPTED);
        originalBet.setMatchedBet(acceptorBet);
        
        // Save both bets (THIS WAS MISSING!)
        betRepository.save(acceptorBet); // Save acceptor's bet first
        betRepository.save(originalBet);
        
        // Record transaction for acceptor
        transactionService.recordBetCreation(acceptorBet);
        
        // Broadcast bet acceptance
        Map<String, Object> message = new HashMap<>();
        message.put("type", "BET_ACCEPTED");
        message.put("betId", originalBet.getId());
        message.put("acceptorUsername", acceptor.getUsername());
        messagingTemplate.convertAndSend("/topic/bet-updates", message);
        
        // Also send to specific bet room for real-time updates
        Map<String, Object> betRoomMessage = new HashMap<>();
        betRoomMessage.put("type", "BET_ACCEPTED");
        betRoomMessage.put("betId", originalBet.getId());
        betRoomMessage.put("acceptorUsername", acceptor.getUsername());
        messagingTemplate.convertAndSend("/topic/bet/" + originalBet.getId(), betRoomMessage);
        
        System.out.println("=== ACCEPT BET COMPLETE ===");
        System.out.println("Acceptor Bet ID: " + acceptorBet.getId());
        System.out.println("Original Bet Updated Status: " + originalBet.getStatus());

        return acceptorBet;
    }

    public Bet setGameCode(Long betId, String gameCode, String username) {
        Bet bet = betRepository.findById(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        if (!bet.getCreator().getUsername().equals(username)) {
            throw new RuntimeException("Only creator can set game code");
        }
        
        if (bet.getStatus() != BetStatus.ACCEPTED) {
            throw new RuntimeException("Bet must be accepted before setting game code");
        }
        
        bet.setUserProvidedCode(gameCode);
        bet.setStatus(BetStatus.CODE_SHARED);
        
        // Also update the matched bet
        if (bet.getMatchedBet() != null) {
            bet.getMatchedBet().setUserProvidedCode(gameCode);
            bet.getMatchedBet().setStatus(BetStatus.CODE_SHARED);
            betRepository.save(bet.getMatchedBet());
        }
        betRepository.save(bet);

                // Broadcast code sharing
        Map<String, Object> message = new HashMap<>();
        message.put("type", "CODE_SHARED");
        message.put("betId", betId);
        message.put("roomCode", gameCode);
        message.put("message", "Room code received! You can now start the game.");
        messagingTemplate.convertAndSend("/topic/bet/" + betId, message);

        return bet;
    }
     // UPDATED: Enhanced result submission with status validation
    @Transactional
    public void submitResult(Long betId, String username, String result, String screenshot, boolean isCreator) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Bet not found"));
        
        // Validate bet is in CODE_SHARED status
        if (bet.getStatus() != BetStatus.CODE_SHARED) {
            throw new RuntimeException("Game code must be shared before submitting results");
        }
        
        // Check if user has already submitted
        if (isCreator && bet.getCreatorSubmitted()) {
            throw new RuntimeException("You have already submitted your result");
        } else if (!isCreator && bet.getAcceptorSubmitted()) {
            throw new RuntimeException("You have already submitted your result");
        }
        
        Bet opponentBet = bet.getMatchedBet();
        
        if (isCreator) {
            bet.setCreatorResult(result);
            bet.setCreatorSubmitted(true);
            if ("WIN".equals(result) && screenshot != null) {
                bet.setWinnerScreenshot(screenshot);
            }
        } else {
            bet.setAcceptorResult(result);
            bet.setAcceptorSubmitted(true);
            if ("WIN".equals(result) && screenshot != null) {
                bet.setWinnerScreenshot(screenshot);
            }
        }
        
        // Check if we can resolve the bet
        if (bet.getCreatorSubmitted() && bet.getAcceptorSubmitted()) {
            resolveBetAutomatically(bet, opponentBet);
        }
        
        betRepository.save(bet);
        betRepository.save(opponentBet);
        
        // Broadcast result submission
        broadcastResultUpdate(bet, username, result);
    }

    private void tryAutoMatch(Bet newBet) {
        List<Bet> pendingBets = betRepository.findByGameTypeAndStatus(
            newBet.getGameType(), BetStatus.PENDING);
        
        Optional<Bet> matchingBet = pendingBets.stream()
            .filter(bet -> bet.getPoints().equals(newBet.getPoints()))
            .filter(bet -> !bet.getCreator().getId().equals(newBet.getCreator().getId()))
            .findFirst();
        
        if (matchingBet.isPresent()) {
            matchBets(newBet, matchingBet.get());
        }
    }
    
    // UPDATED: Use ACCEPTED instead of MATCHED
    @Transactional
    public void matchBets(Bet bet1, Bet bet2) {
        // Reserve points for bet2 creator (since they're effectively accepting)
        User bet2Creator = bet2.getCreator();
        if (bet2Creator.getPoints() < bet2.getPoints()) {
            return; // Can't auto-match if user doesn't have points
        }
        
        bet2Creator.setPoints(bet2Creator.getPoints() - bet2.getPoints());
        userService.saveUser(bet2Creator);
        
        // Update both bets to ACCEPTED
        bet1.setStatus(BetStatus.ACCEPTED);
        bet2.setStatus(BetStatus.ACCEPTED);
        bet1.setMatchedBet(bet2);
        bet2.setMatchedBet(bet1);
        
        betRepository.save(bet1);
        betRepository.save(bet2);
        
        // Record transaction for bet2
        transactionService.recordBetCreation(bet2);
        
        System.out.println("Bets auto-matched! Waiting for game code...");
        
        // Broadcast match creation
        broadcastBetMatched(bet1);
    }
    
    private void broadcastBetMatched(Bet bet) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "BET_MATCHED");
        message.put("betId", bet.getId());
        message.put("points", bet.getPoints());
        message.put("gameType", bet.getGameType());
        
        messagingTemplate.convertAndSend("/topic/bet-updates", message);
    }
    
    // NEW METHOD: Broadcast game code availability
    private void broadcastGameCodeAvailable(Bet bet, String gameCode) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "GAME_CODE_AVAILABLE");
        message.put("betId", bet.getId());
        message.put("gameCode", gameCode);
        message.put("userProvidedCode", gameCode); // For backward compatibility
        
        messagingTemplate.convertAndSend("/topic/bet-updates", message);
    }
    
    @Transactional
    public void resolveBet(Bet winningBet, String screenshotPath) {
        Bet losingBet = winningBet.getMatchedBet();
        
        if (losingBet == null) {
            throw new RuntimeException("No matched bet found");
        }
        
        // Transfer points
        User winner = winningBet.getCreator();
        User loser = losingBet.getCreator();
        
        int points = winningBet.getPoints();
        
        // Winner gets the points (their own points back + opponent's points)
        winner.setPoints(winner.getPoints() + (points * 2));
        
        // Update bet status
        winningBet.setStatus(BetStatus.COMPLETED);
        losingBet.setStatus(BetStatus.COMPLETED);
        
        userService.saveUser(winner);
        userService.saveUser(loser);
        betRepository.save(winningBet);
        betRepository.save(losingBet);
        
        // Record win transaction
        transactionService.recordBetWin(winningBet);
        
        System.out.println("Bet resolved! Winner: " + winner.getUsername());
    }
    
@Transactional
    public void cancelBet(Bet bet) {
        if (bet.getStatus() != BetStatus.PENDING && bet.getStatus() != BetStatus.ACCEPTED) {
            throw new RuntimeException("Cannot cancel bet that is already in progress");
        }
        
        User creator = bet.getCreator();
        int pointsToReturn = bet.getPoints();
        
        // Return points to creator
        creator.setPoints(creator.getPoints() + pointsToReturn);
        
        // If bet was accepted, also return points to acceptor
        if (bet.getStatus() == BetStatus.ACCEPTED && bet.getMatchedBet() != null) {
            User acceptor = bet.getMatchedBet().getCreator();
            acceptor.setPoints(acceptor.getPoints() + pointsToReturn);
            userService.saveUser(acceptor);
            
            // Also cancel the matched bet
            bet.getMatchedBet().setStatus(BetStatus.CANCELLED);
            betRepository.save(bet.getMatchedBet());
        }
        
        bet.setStatus(BetStatus.CANCELLED);
        
        userService.saveUser(creator);
        betRepository.save(bet);
        
        // Record refund transaction
        transactionService.recordBetRefund(bet);
        
        // Broadcast cancellation
        Map<String, Object> message = new HashMap<>();
        message.put("type", "BET_CANCELLED");
        message.put("betId", bet.getId());
        messagingTemplate.convertAndSend("/topic/bet-updates", message);
    }
    
    public Optional<Bet> findById(Long betId) {
        return betRepository.findByIdWithMatchedBet(betId);
    }
    
    public Bet saveBet(Bet bet) {
        return betRepository.save(bet);
    }
    

    public List<Bet> getActiveBetsForUser(User currentUser) {
        return betRepository.findByStatus(BetStatus.PENDING)
                .stream()
                .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // FIXED: Enhanced resolveBetAutomatically with better logging
    private void resolveBetAutomatically(Bet bet1, Bet bet2) {
        String creatorResult = bet1.getCreatorResult();
        String acceptorResult = bet2.getAcceptorResult();
        
        System.out.println("=== RESOLVING BET ===");
        System.out.println("Creator: " + bet1.getCreator().getUsername() + " - " + creatorResult);
        System.out.println("Acceptor: " + bet2.getCreator().getUsername() + " - " + acceptorResult);
        System.out.println("Points: " + bet1.getPoints());
        
        // Log current points before resolution
        System.out.println("Before resolution - Creator points: " + bet1.getCreator().getPoints());
        System.out.println("Before resolution - Acceptor points: " + bet2.getCreator().getPoints());
        
        if ("WIN".equals(creatorResult) && "LOSE".equals(acceptorResult)) {
            // Creator wins
            System.out.println("Resolution: Creator wins");
            awardWinner(bet1.getCreator(), bet2.getCreator(), bet1.getPoints());
            bet1.setWinnerUsername(bet1.getCreator().getUsername());
            bet2.setWinnerUsername(bet1.getCreator().getUsername());
            bet1.setLoserUsername(bet2.getCreator().getUsername());
            bet2.setLoserUsername(bet2.getCreator().getUsername());
        } else if ("LOSE".equals(creatorResult) && "WIN".equals(acceptorResult)) {
            // Acceptor wins
            System.out.println("Resolution: Acceptor wins");
            awardWinner(bet2.getCreator(), bet1.getCreator(), bet1.getPoints());
            bet1.setWinnerUsername(bet2.getCreator().getUsername());
            bet2.setWinnerUsername(bet2.getCreator().getUsername());
            bet1.setLoserUsername(bet1.getCreator().getUsername());
            bet2.setLoserUsername(bet1.getCreator().getUsername());
        } else if ("WIN".equals(creatorResult) && "WIN".equals(acceptorResult)) {
            // Both claim win - dispute
            System.out.println("Resolution: DISPUTE - both claimed WIN");
            refundBothPlayers(bet1.getCreator(), bet2.getCreator(), bet1.getPoints());
            bet1.setStatus(BetStatus.DISPUTED);
            bet2.setStatus(BetStatus.DISPUTED);
            bet1.setDisputeReason("Both players claimed victory");
            bet2.setDisputeReason("Both players claimed victory");
            return;
        } else if ("LOSE".equals(creatorResult) && "LOSE".equals(acceptorResult)) {
            // Both claim lose - refund both
            System.out.println("Resolution: Both lost - refund");
            refundBothPlayers(bet1.getCreator(), bet2.getCreator(), bet1.getPoints());
            bet1.setStatus(BetStatus.CANCELLED);
            bet2.setStatus(BetStatus.CANCELLED);
            return;
        } else {
            // Invalid combination - dispute
            System.out.println("Resolution: DISPUTE - invalid result combination");
            refundBothPlayers(bet1.getCreator(), bet2.getCreator(), bet1.getPoints());
            bet1.setStatus(BetStatus.DISPUTED);
            bet2.setStatus(BetStatus.DISPUTED);
            bet1.setDisputeReason("Invalid result combination: " + creatorResult + " vs " + acceptorResult);
            bet2.setDisputeReason("Invalid result combination: " + creatorResult + " vs " + acceptorResult);
            return;
        }
        
        bet1.setStatus(BetStatus.COMPLETED);
        bet2.setStatus(BetStatus.COMPLETED);
        
        // Log points after resolution
        System.out.println("After resolution - Winner points: " + 
            ("WIN".equals(creatorResult) ? bet1.getCreator().getPoints() : bet2.getCreator().getPoints()));
        System.out.println("After resolution - Loser points: " + 
            ("LOSE".equals(creatorResult) ? bet1.getCreator().getPoints() : bet2.getCreator().getPoints()));
        
        // Broadcast completion
        broadcastBetCompleted(bet1);
    }
// Enhanced awardWinner method with detailed logging
private void awardWinner(User winner, User loser, int points) {
    System.out.println("=== AWARDING WINNER ===");
    System.out.println("Winner before: " + winner.getUsername() + " - Points: " + winner.getPoints());
    System.out.println("Loser before: " + loser.getUsername() + " - Points: " + loser.getPoints());
    System.out.println("Points to award: " + (points * 2));
    
    // Winner gets both stakes (their points back + opponent's points)
    int newWinnerPoints = winner.getPoints() + (points * 2);
    winner.setPoints(newWinnerPoints);
    userService.saveUser(winner);
    transactionService.recordBetWin(winner, points * 2);
    
    System.out.println("Winner after: " + winner.getUsername() + " - Points: " + winner.getPoints());
    System.out.println("=== AWARD COMPLETE ===");
}

// Enhanced refundBothPlayers method
private void refundBothPlayers(User creator, User acceptor, int points) {
    System.out.println("=== REFUNDING BOTH PLAYERS ===");
    System.out.println("Creator before: " + creator.getUsername() + " - Points: " + creator.getPoints());
    System.out.println("Acceptor before: " + acceptor.getUsername() + " - Points: " + acceptor.getPoints());
    
    creator.setPoints(creator.getPoints() + points);
    acceptor.setPoints(acceptor.getPoints() + points);
    userService.saveUser(creator);
    userService.saveUser(acceptor);
    
    System.out.println("Creator after: " + creator.getUsername() + " - Points: " + creator.getPoints());
    System.out.println("Acceptor after: " + acceptor.getUsername() + " - Points: " + acceptor.getPoints());
    System.out.println("=== REFUND COMPLETE ===");
}

    private void broadcastNewBet(Bet bet) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NEW_BET");
            message.put("id", bet.getId());
            message.put("points", bet.getPoints());
            message.put("gameType", bet.getGameType());
            message.put("creatorUsername", bet.getCreator().getUsername());
            message.put("createdAt", bet.getCreatedAt());
            message.put("timestamp", System.currentTimeMillis());
            
            System.out.println("📢 Broadcasting new bet to /topic/bet-updates: " + message);
            
            messagingTemplate.convertAndSend("/topic/bet-updates", message);
            
        } catch (Exception e) {
            System.err.println("❌ Error broadcasting bet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcastResultUpdate(Bet bet, String username, String result) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "RESULT_SUBMITTED");
        message.put("betId", bet.getId());
        message.put("username", username);
        message.put("result", result);
        message.put("isCreatorSubmitted", bet.getCreatorSubmitted());
        message.put("isAcceptorSubmitted", bet.getAcceptorSubmitted());
        message.put("userProvidedCode", bet.getUserProvidedCode());
        
        messagingTemplate.convertAndSend("/topic/bet-updates", message);
    }

    private void broadcastBetCompleted(Bet bet) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "BET_COMPLETED");
        message.put("betId", bet.getId());
        message.put("winner", bet.getWinnerUsername());
        message.put("points", bet.getPoints());
        message.put("userProvidedCode", bet.getUserProvidedCode());
        
        messagingTemplate.convertAndSend("/topic/bet-updates", message);
    }

    public boolean canUserSubmitResult(Long betId, String username, boolean isCreator) {
        Optional<Bet> betOpt = betRepository.findByIdWithMatchedBet(betId);
        if (betOpt.isEmpty()) {
            return false;
        }
        
        Bet bet = betOpt.get();
        
        if (bet.getStatus() != BetStatus.CODE_SHARED) {
            return false;
        }
        
        // Primary check - use direct field access
        boolean canSubmit;
        if (isCreator) {
            canSubmit = !bet.getCreatorSubmitted();
        } else {
            canSubmit = !bet.getAcceptorSubmitted();
        }
        
        // Optional: Verify with database query (for debugging)
        boolean dbCheck = !betRepository.hasUserSubmittedResult(betId, username);
        
        // Log if there's a discrepancy (for debugging)
        if (canSubmit != dbCheck) {
            System.err.println("WARNING: Submission status mismatch for bet " + betId + 
                            ", user " + username + ". Object: " + canSubmit + 
                            ", DB: " + dbCheck);
        }
        
        return canSubmit; // Trust the object state
    }
    // Update dashboard-related methods to use the new query
    public List<Bet> getUserActiveMatches(Long userId) {
        return betRepository.findUserActiveMatches(userId);
    }
}