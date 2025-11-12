package com.kore.king.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.kore.king.entity.User;
import com.kore.king.service.UserService;

@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/play")
    public String playPage(Authentication authentication, Model model) {
        return getUserPage(authentication, model, "Play Game", "user/play-content");
    }
    
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

    // Helper method
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
            return "error";
        }
    }
}