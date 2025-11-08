package com.kore.king.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get user's active bets
            List<Bet> userActiveBets = betService.getUserActiveBets(user.getId());
            
            // Get available bets (excluding user's own)
            List<Bet> availableBets = betService.findAvailableBets(
                BetStatus.PENDING, user.getId(), Pageable.unpaged()).getContent();
            
            // Get in-progress bets (ACCEPTED and CODE_SHARED)
            List<Bet> inProgressBets = userActiveBets.stream()
                .filter(bet -> bet.getStatus() == BetStatus.ACCEPTED || bet.getStatus() == BetStatus.CODE_SHARED)
                .collect(Collectors.toList());
            
            // Get all user bets for history
            Page<Bet> userBetsPage = betService.findUserBets(user.getId(), Pageable.unpaged());

            model.addAttribute("user", user);
            model.addAttribute("userActiveBets", userActiveBets);
            model.addAttribute("availableBets", availableBets);
            model.addAttribute("inProgressBets", inProgressBets);
            model.addAttribute("userBets", userBetsPage.getContent());

            return "dashboard";
        } catch (Exception e) {
            return "redirect:/login?error";
        }
    }

    @PostMapping("/create-bet")
    public String createBet(@RequestParam Integer points,
                          @RequestParam String gameType,
                          @RequestParam String title,
                          @RequestParam(required = false) String description,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            betService.createBet(user, points, gameType, title, description);
            
            redirectAttributes.addFlashAttribute("success", 
                "Bet created successfully! Waiting for opponent.");
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/dashboard";
    }
}