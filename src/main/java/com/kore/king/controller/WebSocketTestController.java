// controller/WebSocketTestController.java
package com.kore.king.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebSocketTestController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketTestController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/test")
    @SendTo("/topic/test")
    public Map<String, Object> handleTestMessage(String message, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "TEST_RESPONSE");
        response.put("message", "Server received: " + message);
        response.put("username", principal != null ? principal.getName() : "anonymous");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/api/test-websocket")
    @ResponseBody
    public Map<String, Object> testWebSocket() {
        Map<String, Object> testMessage = new HashMap<>();
        testMessage.put("type", "MANUAL_TEST");
        testMessage.put("message", "This is a manual test message");
        testMessage.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/test", testMessage);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "sent");
        response.put("message", "Test message sent to /topic/test");
        return response;
    }

    @GetMapping("/api/test-user-websocket")
    @ResponseBody
    public Map<String, Object> testUserWebSocket(Principal principal) {
        if (principal == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "No authenticated user");
            return error;
        }

        Map<String, Object> testMessage = new HashMap<>();
        testMessage.put("type", "USER_TEST");
        testMessage.put("message", "This is a user-specific test message");
        testMessage.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(
            principal.getName(),
            "/queue/notifications",
            testMessage
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "sent");
        response.put("message", "Test message sent to user: " + principal.getName());
        return response;
    }
}