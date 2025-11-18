// controller/WebSocketBetController.java
package com.kore.king.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.kore.king.service.BetService;
import com.kore.king.service.UserService;

@Controller
public class WebSocketBetController {

    private final SimpMessagingTemplate messagingTemplate;
    private final BetService betService;
    private final UserService userService;

    public WebSocketBetController(SimpMessagingTemplate messagingTemplate, 
                                BetService betService, 
                                UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.betService = betService;
        this.userService = userService;
    }

    @MessageMapping("/bets/subscribe")
    @SendToUser("/queue/bet-updates")
    public Map<String, Object> subscribeToBets(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "SUBSCRIBED");
        response.put("username", principal.getName());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @MessageMapping("/bets/{betId}/join")
    public void joinBetRoom(@DestinationVariable Long betId, Principal principal) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "USER_JOINED");
        message.put("betId", betId);
        message.put("username", principal.getName());
        message.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/bet/" + betId, message);
    }

    @MessageMapping("/bets/{betId}/leave")
    public void leaveBetRoom(@DestinationVariable Long betId, Principal principal) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "USER_LEFT");
        message.put("betId", betId);
        message.put("username", principal.getName());
        message.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/bet/" + betId, message);
    }
}