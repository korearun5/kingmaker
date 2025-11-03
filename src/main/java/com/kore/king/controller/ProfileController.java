package com.kore.king.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kore.king.entity.User;
import com.kore.king.service.BetService;
import com.kore.king.service.TransactionService;
import com.kore.king.service.UserService;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private BetService betService;

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public String profile(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get user statistics
        // UserStats stats = betService.getUserStats(user.getId());
        // model.addAttribute("stats", stats);
        model.addAttribute("user", user);

        return "profile";
    }

    @GetMapping("/edit")
    public String editProfileForm(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        return "edit-profile";
    }

    @PostMapping("/edit")
    public String updateProfile(@ModelAttribute User updatedUser,
                               Authentication authentication,
                               Model model) {
        try {
            String username = authentication.getName();
            User existingUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

   
            // Add other updatable fields

            userService.saveUser(existingUser);
            model.addAttribute("success", "Profile updated successfully");

        } catch (Exception e) {
            model.addAttribute("error", "Error updating profile: " + e.getMessage());
        }

        return "edit-profile";
    }

    @GetMapping("/transactions")
    public String transactionHistory(@RequestParam(defaultValue = "0") int page,
                                    Authentication authentication,
                                    Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Page<Transaction> transactions = transactionService.getUserTransactions(user.getId(), page, 20);
        // model.addAttribute("transactions", transactions);

        return "transaction-history";
    }
}
