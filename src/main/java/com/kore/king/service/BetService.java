package com.kore.king.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    // ADD THESE MISSING METHODS:
    
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
    // Add this method to BetService:
    public List<Bet> findAvailableBets(User currentUser) {
        return betRepository.findByStatusAndCreatorNot(BetStatus.PENDING, currentUser);
    }
    public Page<Bet> findUserBets(Long userId, Pageable pageable) {
        return betRepository.findUserBets(userId, pageable);
    }
    
    public long findUserActiveBetsCount(Long userId) {
        List<BetStatus> activeStatuses = Arrays.asList(BetStatus.PENDING, BetStatus.MATCHED);
        return betRepository.countUserActiveBets(userId, activeStatuses);
    }
    
    // Overloaded createBet method that takes Authentication/principal
    @Transactional
    public Bet createBet(Bet bet, String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return createBet(user, bet.getPoints(), bet.getGameType());
    }
    
    // Your existing methods below:
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
        Bet savedBet = betRepository.save(bet);
        
        // Record transaction
        transactionService.recordBetCreation(savedBet);
        
        // Try to auto-match
        tryAutoMatch(savedBet);
            // Broadcast the new bet to all users
        //messagingTemplate.convertAndSend("/topic/new-bets", bet);
        broadcastNewBet(bet);
        return savedBet;
    }

    private void broadcastNewBet(Bet bet) {
        try {
            Map<String, Object> betMessage = new HashMap<>();
            betMessage.put("type", "NEW_BET");
            betMessage.put("id", bet.getId());
            betMessage.put("points", bet.getPoints());
            betMessage.put("gameType", bet.getGameType());
            betMessage.put("creatorUsername", bet.getCreator().getUsername());
            betMessage.put("createdAt", bet.getCreatedAt());
            betMessage.put("timestamp", System.currentTimeMillis());
            
            System.out.println("📢 Broadcasting new bet to /topic/bet-updates: " + betMessage);
            
            messagingTemplate.convertAndSend("/topic/bet-updates", betMessage);
            
            System.out.println("✅ Broadcast completed for bet: " + bet.getCreator().getUsername() + 
                            " - " + bet.getPoints() + " points - " + bet.getGameType());
            
        } catch (Exception e) {
            System.err.println("❌ Error broadcasting bet: " + e.getMessage());
            e.printStackTrace();
        }
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
    
    @Transactional
    public void matchBets(Bet bet1, Bet bet2) {
        String sharedCode = generateSharedCode();
        
        bet1.setStatus(BetStatus.MATCHED);
        bet2.setStatus(BetStatus.MATCHED);
        bet1.setSharedCode(sharedCode);
        bet2.setSharedCode(sharedCode);
        bet1.setMatchedBet(bet2);
        bet2.setMatchedBet(bet1);
        
        betRepository.save(bet1);
        betRepository.save(bet2);
        
        System.out.println("Bets matched! Shared code: " + sharedCode);
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
        if (bet.getStatus() != BetStatus.PENDING) {
            throw new RuntimeException("Cannot cancel bet that is not pending");
        }
        
        // Return points to creator
        User creator = bet.getCreator();
        creator.setPoints(creator.getPoints() + bet.getPoints());
        
        bet.setStatus(BetStatus.CANCELLED);
        
        userService.saveUser(creator);
        betRepository.save(bet);
        
        // Record refund transaction
        transactionService.recordBetRefund(bet);
    }
    
    public Optional<Bet> findById(Long betId) {
        return betRepository.findById(betId);
    }
    
    public Bet saveBet(Bet bet) {
        return betRepository.save(bet);
    }
    
    private String generateSharedCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
public Bet acceptBet(Long betId, User acceptor) {
    Optional<Bet> existingBetOpt = betRepository.findById(betId);
    if (existingBetOpt.isEmpty()) {
        throw new RuntimeException("Bet not found");
    }

    Bet existingBet = existingBetOpt.get();

    // Check if the bet is still available
    if (existingBet.getStatus() != BetStatus.PENDING) {
        throw new RuntimeException("Bet is no longer available");
    }

    // Check if the acceptor is not the creator
    if (existingBet.getCreator().getId().equals(acceptor.getId())) {
        throw new RuntimeException("You cannot accept your own bet");
    }

    // Check if acceptor has enough points
    if (acceptor.getPoints() < existingBet.getPoints()) {
        throw new RuntimeException("Insufficient points to accept this bet");
    }

    // Deduct points from acceptor
    acceptor.setPoints(acceptor.getPoints() - existingBet.getPoints());
    userService.saveUser(acceptor);

    // Create a new bet for the acceptor (which will be matched to the existing one)
    Bet newBet = new Bet(acceptor, existingBet.getPoints(), existingBet.getGameType());
    newBet.setStatus(BetStatus.PENDING);
    Bet savedNewBet = betRepository.save(newBet);

    // Record transaction for the new bet
    transactionService.recordBetCreation(savedNewBet);

    // Now, match the two bets
    matchBets(existingBet, savedNewBet);

    return savedNewBet;
}

public List<Bet> getActiveBetsForUser(User currentUser) {
    return betRepository.findByStatus(BetStatus.PENDING)
            .stream()
            // Don't filter out user's own bets - show all active bets
            .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
            .collect(Collectors.toList());
}
}