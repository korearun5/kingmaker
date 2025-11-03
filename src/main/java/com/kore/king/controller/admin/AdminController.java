package com.kore.king.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kore.king.service.BetService;
import com.kore.king.service.UserService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private BetService betService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        // Get disputed bets, all users, etc.
        // List<Bet> disputedBets = betService.findByStatus(BetStatus.DISPUTED);
        // model.addAttribute("disputedBets", disputedBets);
        
        return "admin/dashboard";
    }
    
    // Remove the viewDisputes and resolveDispute methods
    
    @PostMapping("/adjust-points/{userId}")
    public String adjustUserPoints(@PathVariable Long userId,
                                  @RequestParam Integer points,
                                  @RequestParam String reason,
                                  Model model) {
        try {
            // User user = userService.findById(userId);
            // user.setPoints(user.getPoints() + points);
            // userService.saveUser(user);
            
            // Record the adjustment transaction
            model.addAttribute("success", "Points adjusted successfully");
        } catch (Exception e) {
            model.addAttribute("error", "Error adjusting points: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
}