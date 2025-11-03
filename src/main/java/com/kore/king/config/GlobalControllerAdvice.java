package com.kore.king.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.kore.king.entity.User;
import com.kore.king.service.UserService;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UserService userService;

    @ModelAttribute
    public void addUserToModel(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            try {
                String username = authentication.getName();
                User user = userService.findByUsername(username).orElse(null);
                if (user != null) {
                    model.addAttribute("user", user);
                }
            } catch (Exception e) {
                // Log the error but don't break the application
                System.err.println("Error loading user for global model: " + e.getMessage());
            }
        }
    }
}