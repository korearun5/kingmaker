package com.kore.king.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.kore.king.entity.Bet;
import com.kore.king.entity.User;
import com.kore.king.service.BetService;
import com.kore.king.service.UserService;

@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final BetService betService;
    
    public UserController(UserService userService, BetService betService) {
        this.userService = userService;
        this.betService = betService;
    }

@GetMapping("/play")
public String playPage(Authentication authentication, Model model,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "10") int size) {
    try {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("userPoints", user.getAvailablePoints());
        model.addAttribute("user", user);
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Get all bets for display
        Page<Bet> allBets = betService.findAllBetsForUserWithAvailable(user.getId(), pageable);
        
        // Debug logging
        System.out.println("Total bets found: " + allBets.getTotalElements());
        System.out.println("Bets content: " + allBets.getContent());
        
        model.addAttribute("allBets", allBets);
        
        // Get user's active bets for quick access
        List<Bet> userActiveBets = betService.getUserActiveBets(user.getId());
        model.addAttribute("userActiveBets", userActiveBets);
        
        model.addAttribute("pageTitle", "Play Game");
        model.addAttribute("content", "user/play-content");
        return "layouts/user-layout";
        
    } catch (Exception e) {
        System.out.println("Error in playPage: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("error", "Error loading play game page: " + e.getMessage());
        model.addAttribute("content", "user/play-content");
        return "layouts/user-layout";
    }
}
    
    // Keep other simple pages as they are
    @GetMapping("/history")
    public String historyPage(Authentication authentication, Model model) {
        return getUserPage(authentication, model, "Bet History", "user/history-content");
    }

    @GetMapping("/players")
    public String playersPage(Authentication authentication, Model model) {
        return getUserPage(authentication, model, "Players", "user/players-content");
    }

    @GetMapping("/refer")
    public String referPage(Authentication authentication, Model model) {
        return getUserPage(authentication, model, "Refer & Earn", "user/refer-content");
    }

    @GetMapping("/game-ids")
    public String gameIdsPage(Authentication authentication, Model model) {
        return getUserPage(authentication, model, "Game IDs", "user/game-ids-content");
    }

    @GetMapping("/support")
    public String supportPage(Authentication authentication, Model model) {
        return getUserPage(authentication, model, "Support", "user/support-content");
    }
    
    private String getUserPage(Authentication authentication, Model model, String pageTitle, String template) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("user", user);
            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("content", template);
            return "layouts/user-layout";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading page: " + e.getMessage());
            model.addAttribute("content", "user/dashboard-content");
            return "layouts/user-layout";
        }
    }
    @GetMapping("/debug/points")
    @ResponseBody
    public String debugPoints(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return String.format("User: %s, Available: %d, Held: %d, Total: %d", 
            username, user.getAvailablePoints(), user.getHeldPoints(), 
            user.getAvailablePoints() + user.getHeldPoints());
    }
    @GetMapping("/bets/{id}/card")
    public String getBetCard(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            Bet bet = betService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Bet not found"));
            
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            model.addAttribute("bet", bet);
            model.addAttribute("user", user);
            model.addAttribute("userPoints", user.getAvailablePoints());
            
            return "user/bet-card :: bet-card"; // You'll need to create this fragment
            
        } catch (Exception e) {
            return "<div class='alert alert-danger'>Error loading bet</div>";
        }
    }
}