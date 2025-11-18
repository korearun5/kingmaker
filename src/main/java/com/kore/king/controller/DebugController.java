package com.kore.king.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DebugController {
    
    @GetMapping("/debug/auth")
    @ResponseBody
    public String debugAuth(Authentication authentication) {
        if (authentication == null) {
            return "Authentication is NULL - User is not authenticated";
        }
        return String.format(
            "User: %s, Authenticated: %s, Authorities: %s", 
            authentication.getName(), 
            authentication.isAuthenticated(),
            authentication.getAuthorities()
        );
    }
    @GetMapping("/debug/csrf")
    @ResponseBody
    public String debugCsrf(@RequestParam("_csrf") String csrfToken) {
        return "CSRF Token: " + csrfToken;
    }
    
    @PostMapping("/debug/test-csrf")
    @ResponseBody
    public String testCsrf(@RequestParam("test") String test, 
                          @RequestParam("_csrf") String csrfToken) {
        return "CSRF Test Successful! Test: " + test + ", CSRF Token: " + csrfToken;
    }
}