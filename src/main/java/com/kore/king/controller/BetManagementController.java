package com.kore.king.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.entity.User;
import com.kore.king.service.BetService;
import com.kore.king.service.UserService;

@Controller
@RequestMapping("/bets")
public class BetManagementController {

    @Autowired
    private BetService betService;

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/available")
    public String availableBets(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Authentication authentication,
                               Model model) {
        
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Bet> betsPage = betService.findAvailableBets(BetStatus.PENDING, user.getId(), pageable);

        model.addAttribute("betsPage", betsPage);
        model.addAttribute("currentPage", page);
        
        return "available-bets";
    }

    @GetMapping("/my-bets")
    public String myBets(@RequestParam(defaultValue = "0") int page,
                        Authentication authentication,
                        Model model) {
        
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        Page<Bet> betsPage = betService.findUserBets(user.getId(), pageable);

        model.addAttribute("betsPage", betsPage);
        model.addAttribute("currentPage", page);
        
        return "my-bets";
    }

    @PostMapping("/{betId}/accept")
    public String acceptBet(@PathVariable Long betId,
                    Authentication authentication,
                    RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User acceptor = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Bet acceptedBet = betService.acceptBet(betId, acceptor);
            
            // FIXED: Proper redirect logic
            Optional<Bet> originalBetOpt = betService.findById(betId);
            if (originalBetOpt.isPresent()) {
                Bet originalBet = originalBetOpt.get();
                
                if (originalBet.getCreator().getUsername().equals(username)) {
                    // Current user is the creator - they should set game code
                    return "redirect:/bets/" + originalBet.getId() + "/set-game-code";
                } else {
                    // Current user is the acceptor - they should wait for code
                    return "redirect:/bets/" + acceptedBet.getId() + "/wait-for-code";
                }
            } else {
                // Fallback
                return "redirect:/bets/" + acceptedBet.getId() + "/wait-for-code";
            }
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/bets/available";
        }
    }

    // Page for creator to set game code after acceptance
    @GetMapping("/{betId}/set-game-code")
    public String showSetGameCodeForm(@PathVariable Long betId, Authentication authentication, Model model) {
        try {
            Optional<Bet> betOpt = betService.findById(betId);
            if (betOpt.isEmpty()) {
                return "redirect:/dashboard?error=Bet+not+found";
            }

            Bet bet = betOpt.get();
            String username = authentication.getName();

            // Verify user is the creator of this bet
            if (!bet.getCreator().getUsername().equals(username)) {
                return "redirect:/dashboard?error=Only+creator+can+set+game+code";
            }

            // FIXED: Verify bet is ACCEPTED (not MATCHED)
            if (bet.getStatus() != BetStatus.ACCEPTED) {
                // If code is already shared, redirect to game code page
                if (bet.getStatus() == BetStatus.CODE_SHARED) {
                    return "redirect:/bets/" + betId + "/game-code";
                }
                return "redirect:/dashboard?error=Bet+not+accepted+yet+or+already+has+code";
            }

            model.addAttribute("bet", bet);
            return "set-game-code";

        } catch (Exception e) {
            return "redirect:/dashboard?error=" + e.getMessage();
        }
    }

    // Endpoint to set game code
    @PostMapping("/{betId}/set-game-code")
    public String setGameCode(@PathVariable Long betId,
                            @RequestParam String gameCode,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Bet updatedBet = betService.setGameCode(betId, gameCode, username);
            
            // Broadcast code sharing via WebSocket
            Map<String, Object> message = new HashMap<>();
            message.put("type", "CODE_SHARED");
            message.put("betId", betId);
            message.put("roomCode", gameCode);
            message.put("message", "Room code received! You can now start the game.");
            messagingTemplate.convertAndSend("/topic/bet/" + betId, message);
            
            redirectAttributes.addFlashAttribute("success", 
                "Game code set successfully! Your opponent has been notified.");
            return "redirect:/bets/" + betId + "/game-code";
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/bets/" + betId + "/set-game-code";
        }
    }

    // Page for acceptor to wait for game code
    @GetMapping("/{betId}/wait-for-code")
    public String showWaitForCode(@PathVariable Long betId, Authentication authentication, Model model) {
        try {
            Optional<Bet> betOpt = betService.findById(betId);
            if (betOpt.isEmpty()) {
                return "redirect:/dashboard?error=Bet+not+found";
            }

            Bet bet = betOpt.get();
            String username = authentication.getName();

            // Verify user is part of this bet (acceptor)
            if (!isUserInBet(bet, username)) {
                return "redirect:/dashboard?error=Not+authorized+to+view+this+bet";
            }

            // FIXED: If code is already available, redirect to game code page
            if (bet.getStatus() == BetStatus.CODE_SHARED && bet.getUserProvidedCode() != null) {
                return "redirect:/bets/" + betId + "/game-code";
            }

            // FIXED: If bet is not in accepted state, redirect appropriately
            if (bet.getStatus() != BetStatus.ACCEPTED && bet.getStatus() != BetStatus.CODE_SHARED) {
                return "redirect:/dashboard?error=Invalid+bet+state";
            }

            model.addAttribute("bet", bet);
            return "wait-for-code";

        } catch (Exception e) {
            return "redirect:/dashboard?error=" + e.getMessage();
        }
    }

    @GetMapping("/{betId}/game-code")
    public String showGameCode(@PathVariable Long betId, Authentication authentication, Model model) {
        try {
            Optional<Bet> betOpt = betService.findById(betId);
            if (betOpt.isEmpty()) {
                return "redirect:/dashboard?error=Bet+not+found";
            }

            Bet bet = betOpt.get();
            String username = authentication.getName();

            // Verify user is part of this bet
            if (!isUserInBet(bet, username)) {
                return "redirect:/dashboard?error=Not+authorized+to+view+this+bet";
            }

            // FIXED: Check if game code is available and status is CODE_SHARED
            if (bet.getStatus() != BetStatus.CODE_SHARED || bet.getUserProvidedCode() == null) {
                if (bet.getCreator().getUsername().equals(username)) {
                    // Creator should set the code
                    return "redirect:/bets/" + betId + "/set-game-code";
                } else {
                    // Acceptor should wait for code
                    return "redirect:/bets/" + betId + "/wait-for-code";
                }
            }

            model.addAttribute("bet", bet);
            model.addAttribute("isCreator", bet.getCreator().getUsername().equals(username));
            return "game-code";

        } catch (Exception e) {
            return "redirect:/dashboard?error=" + e.getMessage();
        }
    }

    @GetMapping("/{betId}/submit-result")
    public String showSubmitResultForm(@PathVariable Long betId, 
                                      Authentication authentication,
                                      Model model) {
        Optional<Bet> betOpt = betService.findById(betId);
        if (betOpt.isEmpty()) {
            model.addAttribute("error", "Bet not found");
            return "redirect:/dashboard";
        }

        Bet bet = betOpt.get();
        String username = authentication.getName();

        // Check if user is part of this bet
        if (!isUserInBet(bet, username)) {
            model.addAttribute("error", "You are not part of this bet");
            return "redirect:/dashboard";
        }

        // FIXED: Check if bet is in CODE_SHARED status (ready for results)
        if (bet.getStatus() != BetStatus.CODE_SHARED) {
            model.addAttribute("error", "Game code not shared yet");
            return "redirect:/dashboard";
        }

        // Check if user has already submitted
        boolean isCreator = bet.getCreator().getUsername().equals(username);
        if (!betService.canUserSubmitResult(betId, username, isCreator)) {
            model.addAttribute("error", "You have already submitted your result");
            return "redirect:/dashboard";
        }

        model.addAttribute("bet", bet);
        model.addAttribute("isCreator", isCreator);
        return "submit-result";
    }

    @PostMapping("/{betId}/submit-result")
    public String submitResult(@PathVariable Long betId,
                              @RequestParam String result,
                              @RequestParam(value = "screenshot", required = false) MultipartFile screenshot,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Optional<Bet> betOpt = betService.findById(betId);
            
            if (betOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Bet not found");
                return "redirect:/dashboard";
            }
            
            Bet bet = betOpt.get();
            boolean isCreator = bet.getCreator().getUsername().equals(username);
            
            // Check if user has already submitted
            if (!betService.canUserSubmitResult(betId, username, isCreator)) {
                redirectAttributes.addFlashAttribute("error", "You have already submitted your result");
                return "redirect:/dashboard";
            }
            
            // Only require screenshot if user claims they won
            String screenshotName = null;
            if ("WIN".equals(result)) {
                if (screenshot == null || screenshot.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Screenshot is required when claiming victory");
                    return "redirect:/bets/" + betId + "/submit-result";
                }
                screenshotName = storeScreenshot(screenshot);
            }
            
            betService.submitResult(betId, username, result, screenshotName, isCreator);
            
            // Broadcast result submission
            Map<String, Object> message = new HashMap<>();
            message.put("type", "RESULT_SUBMITTED");
            message.put("betId", betId);
            message.put("username", username);
            message.put("result", result);
            messagingTemplate.convertAndSend("/topic/bet/" + betId, message);
            
            String messageText = "WIN".equals(result) ? 
                "Victory claimed! Waiting for opponent confirmation..." :
                "Defeat acknowledged. Waiting for opponent's result...";
                
            redirectAttributes.addFlashAttribute("success", messageText);
            return "redirect:/dashboard";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/bets/" + betId + "/submit-result";
        }
    }

    @PostMapping("/{betId}/cancel")
    public String cancelBet(@PathVariable Long betId,
                        Authentication authentication,
                        Model model) {
        try {
            Optional<Bet> betOpt = betService.findById(betId);
            if (betOpt.isEmpty()) {
                return "redirect:/dashboard?error=Bet+not+found";
            }

            Bet bet = betOpt.get();
            String username = authentication.getName();

            // Check if user owns this bet
            if (!bet.getCreator().getUsername().equals(username)) {
                return "redirect:/dashboard?error=You+can+only+cancel+your+own+bets";
            }

            betService.cancelBet(bet);
            
            // Broadcast cancellation
            Map<String, Object> message = new HashMap<>();
            message.put("type", "BET_CANCELLED");
            message.put("betId", betId);
            messagingTemplate.convertAndSend("/topic/bet-updates", message);
            
            return "redirect:/dashboard?success=Bet+cancelled+successfully";

        } catch (Exception e) {
            return "redirect:/dashboard?error=Error+cancelling+bet:+ " + e.getMessage();
        }
    }

    // Helper method to store screenshot
    private String storeScreenshot(MultipartFile screenshot) {
        try {
            // Simple implementation - store original filename
            // In production, use FileStorageService
            return screenshot.getOriginalFilename();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store screenshot: " + e.getMessage());
        }
    }

    // Helper method to check if user is in bet
    private boolean isUserInBet(Bet bet, String username) {
        return bet.getCreator().getUsername().equals(username) || 
               (bet.getMatchedBet() != null && bet.getMatchedBet().getCreator().getUsername().equals(username));
    }

    // WebSocket test methods
    @MessageMapping("/test")
    @SendTo("/topic/bet-updates")
    public Map<String, Object> testWebSocket() {
        Map<String, Object> testMessage = new HashMap<>();
        testMessage.put("type", "TEST");
        testMessage.put("message", "WebSocket is working!");
        testMessage.put("timestamp", System.currentTimeMillis());
        return testMessage;
    }

    @GetMapping("/test-ws")
    @ResponseBody
    public String testWebSocketManual() {
        Map<String, Object> testMessage = new HashMap<>();
        testMessage.put("type", "MANUAL_TEST");
        testMessage.put("message", "Manual test message");
        testMessage.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/bet-updates", testMessage);
        return "Test message sent to /topic/bet-updates";
    }
}