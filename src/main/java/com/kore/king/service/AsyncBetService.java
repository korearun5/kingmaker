package com.kore.king.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.kore.king.dto.CreateBetRequest;

@Service
public class AsyncBetService {

    private final BetService betService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskExecutor taskExecutor;

    private static final Logger logger = LoggerFactory.getLogger(AsyncBetService.class);

    public AsyncBetService(BetService betService, SimpMessagingTemplate messagingTemplate, 
                         @Qualifier("taskExecutor") TaskExecutor taskExecutor) {
        this.betService = betService;
        this.messagingTemplate = messagingTemplate;
        this.taskExecutor = taskExecutor;
    }

    @Async("taskExecutor")
    public void resolveBetAsync(Long betId) {
        try {
            logger.info("Starting async bet resolution for bet ID: {}", betId);
            
            // Add artificial delay to simulate complex processing
            Thread.sleep(1000);
            
            betService.resolveBetWithRetry(betId);
            
            logger.info("Completed async bet resolution for bet ID: {}", betId);
            
        } catch (Exception e) {
            logger.error("Error in async bet resolution for bet ID: {}", betId, e);
            
            // Notify users of failure
            Map<String, Object> errorMessage = new HashMap<>();
            errorMessage.put("type", "BET_RESOLUTION_ERROR");
            errorMessage.put("betId", betId);
            errorMessage.put("error", "Failed to resolve bet");
            
            messagingTemplate.convertAndSend("/topic/bet/" + betId, errorMessage);
        }
    }

    @Async("taskExecutor")
    public void processBatchBetCreation(List<CreateBetRequest> betRequests, Long creatorId) {
        logger.info("Processing batch creation of {} bets for user ID: {}", betRequests.size(), creatorId);
        
        for (CreateBetRequest request : betRequests) {
            try {
                betService.createBet(creatorId, request.getPoints(), request.getGameType(), 
                                   request.getTitle(), request.getDescription());
                Thread.sleep(100); // Rate limiting
            } catch (Exception e) {
                logger.error("Failed to create bet in batch: {}", request.getTitle(), e);
            }
        }
        
        logger.info("Completed batch creation of {} bets", betRequests.size());
    }
}