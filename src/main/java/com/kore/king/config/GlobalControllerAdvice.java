package com.kore.king.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerAdvice.class);
    
    @ModelAttribute
    public void addUserToModel(Authentication authentication, Model model) {
        if (isAuthenticatedUser(authentication)) {
            try {
                String username = authentication.getName();
                User user = userService.findByUsername(username).orElse(null);
                if (user != null) {
                    model.addAttribute("user", user);
                }
            } catch (Exception e) {
                // Log the error but don't break the application
                 logger.warn("Error loading user for global model: {}", e.getMessage());
            }
        }
    }
    private boolean isAuthenticatedUser(Authentication authentication) {
    return authentication != null && 
            authentication.isAuthenticated() && 
            !(authentication.getPrincipal() instanceof String && 
                "anonymousUser".equals(authentication.getPrincipal()));
    }
}