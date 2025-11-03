package com.kore.king.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kore.king.entity.Bet;
import com.kore.king.entity.User;
import com.kore.king.service.BetService;
import com.kore.king.service.UserService;



@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private BetService betService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        try {
        String username = authentication.getName();
        System.out.println("Loading dashboard for user: " + username); // Debug log
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
         System.out.println("User found: " + user.getUsername() + ", Points: " + user.getPoints()); // Debug log
        // Get user's active bets count
        //TEMP: long activeBetsCount = betService.findUserActiveBetsCount(user.getId());
        long activeBetsCount = 0;
        
        model.addAttribute("user", user);
        model.addAttribute("activeBets", activeBetsCount);

        return "dashboard";
        //return "test-dashboard";
        }catch(Exception e) {
                    System.err.println("Error in dashboard: " + e.getMessage());
        e.printStackTrace();
        // Redirect to error page or login
        return "redirect:/login?error";
        }
    }

    @GetMapping("/create-bet")
    public String showCreateBetForm(Model model) {
        model.addAttribute("bet", new Bet());
        return "create-bet";
    }

    @GetMapping("/available-bets")
    public String showAvailableBets(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get available bets (bets that are not matched and not created by the current user)
        List<Bet> availableBets = betService.findAvailableBets(user);
        
        model.addAttribute("availableBets", availableBets);
        return "available-bets";
    }
    @PostMapping("/create-bet")
    public String createBet(@RequestParam Integer points,
                           @RequestParam String gameType,
                           Authentication authentication,
                           Model model) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Bet bet = betService.createBet(user, points, gameType);
            
            if (bet.getStatus().equals("MATCHED")) {
                model.addAttribute("success", 
                    "Bet created and matched! Game code: " + bet.getSharedCode());
            } else {
                model.addAttribute("success", 
                    "Bet created! Waiting for opponent...");
            }
            
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }

        model.addAttribute("bet", new Bet());
        return "create-bet";
    }
}