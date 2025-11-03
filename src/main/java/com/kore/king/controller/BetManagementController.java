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

    @GetMapping("/create-bet")
    public String showCreateBetForm(Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            model.addAttribute("user", user);
            model.addAttribute("bet", new Bet());
            
            return "create-bet";
        } catch (Exception e) {
            System.err.println("Error in create-bet form: " + e.getMessage());
            return "redirect:/dashboard?error";
        }
    }

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
            
            // Broadcast that bet was accepted
            Map<String, Object> message = new HashMap<>();
            message.put("type", "BET_ACCEPTED");
            message.put("betId", betId);
            message.put("acceptorUsername", username);
            messagingTemplate.convertAndSend("/topic/bet-updates", message);
            
            // Redirect to game code page instead of available bets
            return "redirect:/bets/" + betId + "/game-code";
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/bets/available";
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

        model.addAttribute("bet", bet);
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
            
            String message = "WIN".equals(result) ? 
                "Victory claimed! Waiting for opponent confirmation..." :
                "Defeat acknowledged. Waiting for opponent's result...";
                
            redirectAttributes.addFlashAttribute("success", message);
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