package com.kore.king.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kore.king.entity.User;
import com.kore.king.service.UserService;

@Controller
@RequestMapping("/user")
public class SettingsController {

    private final UserService userService;

    public SettingsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/settings")
    public String settings(Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "Settings");
            model.addAttribute("content", "user/settings-content");
            return "layouts/user-layout";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading settings: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("settings/change-password")
    public String changePassword(@RequestParam String currentPassword,
                            @RequestParam String newPassword,
                            @RequestParam String confirmPassword,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            
            // Validation
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New passwords do not match");
                return "redirect:/user/settings-content";
            }
            
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters long");
                return "redirect:/user/settings-content";
            }
            
            boolean success = userService.changePassword(username, currentPassword, newPassword);
            if (success) {
                redirectAttributes.addFlashAttribute("success", "Password changed successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred");
        }
        
        return "redirect:/user/settings-content";
    }
}