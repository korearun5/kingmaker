package com.kore.king.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Use UserService instead of CustomUserDetailsService
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kore.king.entity.User;
import com.kore.king.service.UserService;

@Controller
public class AuthController {

    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/user/play";
        }
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        try {
            System.out.println("Registering user: " + user.getUsername());
            System.out.println("User email: " + user.getEmail()); // Debug email
            userService.registerUser(user); // Use UserService
            return "redirect:/login?success";
        } catch (RuntimeException e) {
            System.out.println("Registration error: " + e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                              @RequestParam(value = "logout", required = false) String logout,
                              Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }
}