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
        
        Optional<Bet> betOpt = betRepository.findById(Long.parseLong(betId));
        if (betOpt.isPresent()) {
            Bet bet = betOpt.get();
            
            if (bet.getCreator().getUsername().equals(username)) {
                bet.setCreatorSocketId(socketId);
            } else if (bet.getAcceptor() != null && bet.getAcceptor().getUsername().equals(username)) {
                bet.setAcceptorSocketId(socketId);
            }
            
            betRepository.save(bet);
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
        
        Optional<Bet> betOpt = betRepository.findById(Long.parseLong(betId));
        if (betOpt.isPresent()) {
            Bet bet = betOpt.get();
            bet.setGameCode(roomCode);
            bet.setStatus(BetStatus.CODE_SHARED);
            betRepository.save(bet);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CODE_SHARED");
            response.put("roomCode", roomCode);
            response.put("betId", betId);
            
            messagingTemplate.convertAndSend("/topic/bet/" + betId, response);
        }
    }
}