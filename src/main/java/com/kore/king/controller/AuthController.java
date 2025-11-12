package com.kore.king.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Use UserService instead of CustomUserDetailsService
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.kore.king.entity.User;
import com.kore.king.service.UserService;

@Controller
public class AuthController {

    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    //@GetMapping("/")
    //public String home() {
    //    return "redirect:/dashboard";
    //}

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
    public String showLoginForm() {
        return "login";
    }
}