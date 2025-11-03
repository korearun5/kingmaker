package com.kore.king.controller;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

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
                           Model model) {
        try {
            String username = authentication.getName();
            User acceptor = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Bet acceptedBet = betService.acceptBet(betId, acceptor);
            
            model.addAttribute("success", "Bet accepted! Game code: " + acceptedBet.getSharedCode());
            
            // Broadcast that bet was accepted
            Map<String, Object> message = new HashMap<>();
            message.put("type", "BET_ACCEPTED");
            message.put("betId", betId);
            message.put("acceptorUsername", username);
            messagingTemplate.convertAndSend("/topic/bet-updates", message);
            
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }
        
        return "redirect:/bets/available";
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
        if (!bet.getCreator().getUsername().equals(username) && 
            (bet.getMatchedBet() == null || !bet.getMatchedBet().getCreator().getUsername().equals(username))) {
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
                (bet.getMatchedBet() == null || !bet.getMatchedBet().getCreator().getUsername().equals(username))) {
                model.addAttribute("error", "You are not part of this bet");
                return "redirect:/dashboard";
            }

            // Validate screenshot
            if (screenshot.isEmpty()) {
                model.addAttribute("error", "Please upload a screenshot");
                return "submit-result";
            }

            // Store screenshot (simplified - just get filename)
            String screenshotName = screenshot.getOriginalFilename();
            
            // Auto-resolve in favor of submitter
            betService.resolveBet(bet, screenshotName);

            model.addAttribute("success", "Result submitted successfully! Points awarded.");
            return "redirect:/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Error submitting result: " + e.getMessage());
            return "submit-result";
        }
    }

    @PostMapping("/{betId}/cancel")
    public String cancelBet(@PathVariable Long betId,
                           Authentication authentication,
                           Model model) {
        try {
            Optional<Bet> betOpt = betService.findById(betId);
            if (betOpt.isEmpty()) {
                model.addAttribute("error", "Bet not found");
                return "redirect:/my-bets";
            }

            Bet bet = betOpt.get();
            String username = authentication.getName();

            // Check if user owns this bet
            if (!bet.getCreator().getUsername().equals(username)) {
                model.addAttribute("error", "You can only cancel your own bets");
                return "redirect:/my-bets";
            }

            betService.cancelBet(bet);
            model.addAttribute("success", "Bet cancelled successfully");

        } catch (Exception e) {
            model.addAttribute("error", "Error cancelling bet: " + e.getMessage());
        }

        return "redirect:/my-bets";
    }
}
