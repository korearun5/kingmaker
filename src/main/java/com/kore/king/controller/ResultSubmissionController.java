package com.kore.king.controller;


import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.service.BetService;
import com.kore.king.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Controller
@RequestMapping("/bet")
public class ResultSubmissionController {

    @Autowired
    private BetService betService;

    @Autowired
    private FileStorageService fileStorageService;

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
        if (!bet.getCreator().getUsername().equals(username) && 
            !bet.getMatchedBet().getCreator().getUsername().equals(username)) {
            model.addAttribute("error", "You are not part of this bet");
            return "redirect:/dashboard";
        }

        model.addAttribute("bet", bet);
        return "submit-result";
    }

    @PostMapping("/{betId}/submit-result")
    public String submitResult(@PathVariable Long betId,
                              @RequestParam("screenshot") MultipartFile screenshot,
                              Authentication authentication,
                              Model model) {
        try {
            Optional<Bet> betOpt = betService.findById(betId);
            if (betOpt.isEmpty()) {
                model.addAttribute("error", "Bet not found");
                return "redirect:/dashboard";
            }

            Bet bet = betOpt.get();
            String username = authentication.getName();

            // Validate user is part of the bet
            if (!bet.getCreator().getUsername().equals(username) && 
                !bet.getMatchedBet().getCreator().getUsername().equals(username)) {
                model.addAttribute("error", "You are not part of this bet");
                return "redirect:/dashboard";
            }

            // Validate screenshot
            if (screenshot.isEmpty()) {
                model.addAttribute("error", "Please upload a screenshot");
                return "submit-result";
            }

            // Store screenshot
            String screenshotPath = fileStorageService.storeFile(screenshot);

            // For now, auto-resolve in favor of submitter
            // In real implementation, you'd wait for opponent confirmation or admin review
            betService.resolveBet(bet, screenshotPath);

            model.addAttribute("success", "Result submitted successfully! Points will be awarded after verification.");
            return "redirect:/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Error submitting result: " + e.getMessage());
            return "submit-result";
        }
    }

    @PostMapping("/{betId}/dispute")
    public String createDispute(@PathVariable Long betId,
                               @RequestParam String reason,
                               Authentication authentication,
                               Model model) {
        try {
            Optional<Bet> betOpt = betService.findById(betId);
            if (betOpt.isEmpty()) {
                model.addAttribute("error", "Bet not found");
                return "redirect:/dashboard";
            }

            Bet bet = betOpt.get();
            
            // Update bet status to disputed
            bet.setStatus(BetStatus.DISPUTED);
            betService.saveBet(bet);

            // In real implementation, notify admin and opponent
            System.out.println("Dispute created for bet " + betId + ": " + reason);

            model.addAttribute("success", "Dispute created successfully. Admin will review your case.");
            return "redirect:/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Error creating dispute: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }
}