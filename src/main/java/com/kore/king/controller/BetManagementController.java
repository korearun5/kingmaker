package com.kore.king.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kore.king.entity.Bet;
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
        
        return "redirect:/user/dashboard-content";
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
        
        return "redirect:/user/dashboard-content";
    }

    @PostMapping("/{id}/submit-result")
    public String submitResult(@PathVariable Long id, @RequestParam String result,
                             @RequestParam(required = false) MultipartFile screenshot,
                             Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Bet bet = betService.findById(id).orElseThrow(() -> new RuntimeException("Bet not found"));
            boolean isCreator = bet.getCreator().getUsername().equals(username);

            String screenshotName = null;
            if ("WIN".equals(result) && screenshot != null && !screenshot.isEmpty()) {
                screenshotName = storeScreenshot(screenshot);
            }

            betService.submitResult(id, username, result, screenshotName, isCreator);
            redirectAttributes.addFlashAttribute("success", "Result submitted successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/user/dashboard-content";
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
        
        return "redirect:/user/dashboard-content";
    }

    private String storeScreenshot(MultipartFile screenshot) {
        // Simple implementation - store original filename
        // In production, use FileStorageService
        return screenshot.getOriginalFilename();
    }
}