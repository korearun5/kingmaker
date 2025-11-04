package com.kore.king.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.repository.BetRepository;

@Controller
public class WebSocketEventHandler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private BetRepository betRepository;

    @MessageMapping("/bet/{betId}/join")
    public void handleJoinBet(@DestinationVariable String betId, Map<String, String> payload) {
        String username = payload.get("username");
        String socketId = payload.get("socketId");
        
        System.out.println("🔵 User " + username + " joining bet " + betId + " with socket " + socketId);
        
        // Update socket ID in bet
        Optional<Bet> betOpt = betRepository.findById(Long.parseLong(betId));
        if (betOpt.isPresent()) {
            Bet bet = betOpt.get();
            
            // Check if user is the creator of this bet
            if (bet.getCreator().getUsername().equals(username)) {
                bet.setCreatorSocketId(socketId);
                System.out.println("🔵 Set creator socket for " + username + " on bet " + betId);
            } 
            // Check if user is the acceptor (creator of the matched bet)
            else if (bet.getMatchedBet() != null && bet.getMatchedBet().getCreator().getUsername().equals(username)) {
                bet.setAcceptorSocketId(socketId);
                System.out.println("🟢 Set acceptor socket for " + username + " on bet " + betId);
            }
            // Also check if user is the acceptor of this bet (this is the acceptor's bet)
            else if (bet.getMatchedBet() != null) {
                // This is the acceptor's bet, user should be the creator of this bet
                if (bet.getCreator().getUsername().equals(username)) {
                    bet.setCreatorSocketId(socketId); // Acceptor's own bet
                    System.out.println("🟢 Set acceptor's own socket for " + username + " on acceptor bet " + betId);
                }
            }
            
            betRepository.save(bet);
        } else {
            System.out.println("❌ Bet not found: " + betId);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "JOINED");
        response.put("betId", betId);
        response.put("username", username);
        
        messagingTemplate.convertAndSend("/topic/bet/" + betId, response);
    }

    @MessageMapping("/bet/{betId}/share-code")
    public void handleShareCode(@DestinationVariable String betId, Map<String, String> payload) {
        String roomCode = payload.get("roomCode");
        
        System.out.println("🎮 Sharing game code " + roomCode + " for bet " + betId);
        
        // Update bet with room code
        Optional<Bet> betOpt = betRepository.findById(Long.parseLong(betId));
        
        if (betOpt.isPresent()) {
            Bet bet = betOpt.get();
            bet.setUserProvidedCode(roomCode);
            bet.setStatus(BetStatus.CODE_SHARED);
            betRepository.save(bet);
            
            // CRITICAL FIX: Notify BOTH players by sending to both bet topics
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CODE_SHARED");
            response.put("roomCode", roomCode);
            response.put("message", "Game code received! You can now start the game.");
            response.put("betId", betId);
            
            // Send to creator's bet topic
            messagingTemplate.convertAndSend("/topic/bet/" + betId, response);
            System.out.println("📢 Sent code to creator's bet topic: " + betId);
            
            // ALSO send to acceptor's bet topic if it exists
            if (bet.getMatchedBet() != null) {
                messagingTemplate.convertAndSend("/topic/bet/" + bet.getMatchedBet().getId(), response);
                System.out.println("✅ Code shared to both bet topics: " + betId + " (creator) and " + bet.getMatchedBet().getId() + " (acceptor)");
            } else {
                System.out.println("⚠️  No matched bet found for bet: " + betId);
                
                // Fallback: Try to find if there's any bet that has this bet as matchedBet
                Optional<Bet> acceptorBetOpt = betRepository.findByMatchedBetId(bet.getId());
                if (acceptorBetOpt.isPresent()) {
                    Bet acceptorBet = acceptorBetOpt.get();
                    messagingTemplate.convertAndSend("/topic/bet/" + acceptorBet.getId(), response);
                    System.out.println("🔄 Found acceptor bet via fallback: " + acceptorBet.getId());
                } else {
                    System.out.println("❌ Could not find acceptor bet for original bet: " + betId);
                }
            }
        } else {
            System.out.println("❌ Bet not found for code sharing: " + betId);
        }
    }
}