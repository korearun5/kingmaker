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

    // Existing methods
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
    
    public long findUserActiveBetsCount(Long userId) {
        List<BetStatus> activeStatuses = Arrays.asList(BetStatus.PENDING, BetStatus.MATCHED);
        return betRepository.countUserActiveBets(userId, activeStatuses);
    }
    
    @Transactional
    public Bet createBet(Bet bet, String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return createBet(user, bet.getPoints(), bet.getGameType(), bet.getUserProvidedCode());
    }
    
    // SINGLE createBet method with userProvidedCode
    @Transactional
    public Bet createBet(User creator, Integer points, String gameType, String userProvidedCode) {
        // Check if user has enough points
        if (creator.getPoints() < points) {
            throw new RuntimeException("Insufficient points");
        }
        
        // Validate user provided code
        if (userProvidedCode == null || userProvidedCode.trim().isEmpty()) {
            throw new RuntimeException("Game code is required");
        }
        
        // Reserve points by deducting them temporarily
        creator.setPoints(creator.getPoints() - points);
        userService.saveUser(creator);
        
        Bet bet = new Bet(creator, points, gameType);
        bet.setUserProvidedCode(userProvidedCode.trim());
        bet.setCreatorResult("PENDING");
        
        Bet savedBet = betRepository.save(bet);
        
        // Record transaction
        transactionService.recordBetCreation(savedBet);
        
        // Try to auto-match
        tryAutoMatch(savedBet);
        
        // Broadcast new bet
        broadcastNewBet(savedBet);
        
        return savedBet;
    }

    // SINGLE acceptBet method
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

        // Create a new bet for the acceptor
        Bet newBet = new Bet(acceptor, existingBet.getPoints(), existingBet.getGameType());
        newBet.setStatus(BetStatus.PENDING);
        newBet.setUserProvidedCode(existingBet.getUserProvidedCode()); // Same game code
        newBet.setAcceptorResult("PENDING");
        
        Bet savedNewBet = betRepository.save(newBet);

        // Record transaction
        transactionService.recordBetCreation(savedNewBet);

        // Match the bets
        matchBets(existingBet, savedNewBet);

        return savedNewBet;
    }

    @Transactional
    public void submitResult(Long betId, String username, String result, String screenshot, boolean isCreator) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Bet not found"));
        
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

    // Rest of the methods remain the same...
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
        
        // Broadcast match creation
        broadcastBetMatched(bet1);
    }
    
    private void broadcastBetMatched(Bet bet) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "BET_MATCHED");
        message.put("betId", bet.getId());
        message.put("userProvidedCode", bet.getUserProvidedCode());
        message.put("points", bet.getPoints());
        message.put("gameType", bet.getGameType());
        
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

    public List<Bet> getActiveBetsForUser(User currentUser) {
        return betRepository.findByStatus(BetStatus.PENDING)
                .stream()
                .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private void resolveBetAutomatically(Bet bet1, Bet bet2) {
        String creatorResult = bet1.getCreatorResult();
        String acceptorResult = bet2.getAcceptorResult();
        
        if ("WIN".equals(creatorResult) && "LOSE".equals(acceptorResult)) {
            // Creator wins
            awardWinner(bet1.getCreator(), bet2.getCreator(), bet1.getPoints());
            bet1.setWinnerUsername(bet1.getCreator().getUsername());
            bet2.setWinnerUsername(bet1.getCreator().getUsername());
            bet1.setLoserUsername(bet2.getCreator().getUsername());
            bet2.setLoserUsername(bet2.getCreator().getUsername());
        } else if ("LOSE".equals(creatorResult) && "WIN".equals(acceptorResult)) {
            // Acceptor wins
            awardWinner(bet2.getCreator(), bet1.getCreator(), bet1.getPoints());
            bet1.setWinnerUsername(bet2.getCreator().getUsername());
            bet2.setWinnerUsername(bet2.getCreator().getUsername());
            bet1.setLoserUsername(bet1.getCreator().getUsername());
            bet2.setLoserUsername(bet1.getCreator().getUsername());
        } else {
            // Dispute - refund both
            refundBothPlayers(bet1.getCreator(), bet2.getCreator(), bet1.getPoints());
            bet1.setStatus(BetStatus.DISPUTED);
            bet2.setStatus(BetStatus.DISPUTED);
            return;
        }
        
        bet1.setStatus(BetStatus.COMPLETED);
        bet2.setStatus(BetStatus.COMPLETED);
        
        // Broadcast completion
        broadcastBetCompleted(bet1);
    }

    private void awardWinner(User winner, User loser, int points) {
        winner.setPoints(winner.getPoints() + (points * 2));
        userService.saveUser(winner);
        transactionService.recordBetWin(winner, points * 2);
    }

    private void refundBothPlayers(User creator, User acceptor, int points) {
        creator.setPoints(creator.getPoints() + points);
        acceptor.setPoints(acceptor.getPoints() + points);
        userService.saveUser(creator);
        userService.saveUser(acceptor);
    }

    // Real-time broadcasting methods
    private void broadcastNewBet(Bet bet) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NEW_BET");
            message.put("id", bet.getId());
            message.put("points", bet.getPoints());
            message.put("gameType", bet.getGameType());
            message.put("creatorUsername", bet.getCreator().getUsername());
            message.put("userProvidedCode", bet.getUserProvidedCode());
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
}