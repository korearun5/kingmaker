package com.kore.king.controller;

import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/admin")
@PreAuthorize("hasRole('MAIN_ADMIN') or hasRole('EMPLOYEE_ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("content", "admin/dashboard-content");
        return "layouts/admin-layout"; 
    }

    @GetMapping("/users")
    public String userManagement(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", user);
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("content", "admin/users-content");
        return "layouts/admin-layout"; // Uses admin-layout
    }

    @GetMapping("/create-admin")
    public String showCreateAdminForm(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Create Admin");
        model.addAttribute("content", "admin/create-admin-content");
        return "layouts/admin-layout"; // Uses admin-layout
    }

    @PostMapping("/users/promote")
    public String promoteToAdmin(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        try {
            boolean success = userService.promoteToAdmin(userId);
            if (success) {
                redirectAttributes.addFlashAttribute("success", "User promoted to admin successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "User not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error promoting user: " + e.getMessage());
        }
        return "redirect:/admin/users-content";
    }

    @PostMapping("/users/demote")
    public String demoteFromAdmin(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        try {
            boolean success = userService.demoteFromAdmin(userId);
            if (success) {
                redirectAttributes.addFlashAttribute("success", "User demoted to regular user successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "User not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error demoting user: " + e.getMessage());
        }
        return "redirect:/admin/users-content";
    }
}