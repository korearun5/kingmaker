package com.kore.king.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kore.king.entity.Bet;
import com.kore.king.entity.User;
import com.kore.king.service.BetService;
import com.kore.king.service.UserService;

@Controller
@RequestMapping("/bets")
public class BetManagementController {

    private final BetService betService;
    private final UserService userService;

    public BetManagementController(BetService betService, UserService userService) {
        this.betService = betService;
        this.userService = userService;
    }

    @PostMapping("/create")
    public String createBet(@RequestParam Integer points,
                        @RequestParam String gameType,
                        @RequestParam(required = false) String title,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User creator = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Use provided title or generate default
            String betTitle = (title != null && !title.trim().isEmpty()) ? 
                            title : gameType + " Bet - " + creator.getUsername();
            
            String conditions = "Standard " + gameType + " match rules";
            
            Bet createdBet = betService.createBet(creator.getId(), points, gameType, betTitle, conditions);
            
            redirectAttributes.addFlashAttribute("success", "Bet created successfully!");
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/user/play";
    }

    @PostMapping("/{id}/accept")
    public String acceptBet(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User acceptor = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            betService.acceptBet(id, acceptor.getId());
            redirectAttributes.addFlashAttribute("success", "Bet accepted successfully!");
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/user/play";
    }

    @PostMapping("/{id}/set-game-code")
    public String setGameCode(@PathVariable Long id, @RequestParam String gameCode, 
                        Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            betService.setGameCode(id, gameCode, username);
            redirectAttributes.addFlashAttribute("success", "Game code shared successfully!");
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/user/play";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBet(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            betService.cancelBet(id, username);
            redirectAttributes.addFlashAttribute("success", "Bet cancelled successfully.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/user/play";
    }
}