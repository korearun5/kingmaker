package com.kore.king.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.entity.User;
import com.kore.king.service.BetService;
import com.kore.king.service.UserService;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private BetService betService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get ALL available bets (not just user's bets)
            List<Bet> availableBets = betService.findAvailableBets(BetStatus.PENDING, user.getId(), Pageable.unpaged())
                .getContent();

            // Get user's own pending bets
            List<Bet> userPendingBets = betService.findUserBets(user.getId(), Pageable.unpaged())
                .getContent()
                .stream()
                .filter(bet -> bet.getStatus() == BetStatus.PENDING)
                .collect(Collectors.toList());

            long activeBetsCount = betService.findUserActiveBetsCount(user.getId());

            // Get user's matched bets
            List<Bet> userMatchedBets = betService.findUserBets(user.getId(), Pageable.unpaged())
                .getContent()
                .stream()
                .filter(bet -> bet.getStatus() == BetStatus.MATCHED)
                .collect(Collectors.toList());

            model.addAttribute("user", user);
            model.addAttribute("activeBets", activeBetsCount);
            model.addAttribute("preloadedBets", availableBets);
            model.addAttribute("userPendingBets", userPendingBets);
            model.addAttribute("userMatchedBets", userMatchedBets);

            return "dashboard";
        } catch (Exception e) {
            System.err.println("Error in dashboard: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/login?error";
        }
    }

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

    // SINGLE createBet method with userProvidedCode
    @PostMapping("/create-bet")
    public String createBet(@RequestParam Integer points,
                        @RequestParam String gameType,
                        @RequestParam String userProvidedCode,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Bet bet = betService.createBet(user, points, gameType, userProvidedCode);
            
            // Broadcast new bet to ALL users via WebSocket
            broadcastNewBet(bet);
            
            redirectAttributes.addFlashAttribute("success", 
                "Bet created! Share the code: " + userProvidedCode + " with your opponent.");
            
            return "redirect:/dashboard";
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/create-bet";
        }
    }

    // Broadcast new bet to all connected users
    private void broadcastNewBet(Bet bet) {
        try {
            java.util.Map<String, Object> message = new java.util.HashMap<>();
            message.put("type", "NEW_BET");
            message.put("id", bet.getId());
            message.put("points", bet.getPoints());
            message.put("gameType", bet.getGameType());
            message.put("creatorUsername", bet.getCreator().getUsername());
            message.put("userProvidedCode", bet.getUserProvidedCode());
            message.put("createdAt", bet.getCreatedAt());
            
            messagingTemplate.convertAndSend("/topic/bet-updates", message);
            System.out.println("📢 Broadcasted new bet: " + bet.getId() + " by " + bet.getCreator().getUsername());
        } catch (Exception e) {
            System.err.println("❌ Error broadcasting new bet: " + e.getMessage());
        }
    }
}