package com.kore.king.controller.user;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kore.king.entity.User;
import com.kore.king.entity.WithdrawalMethod;
import com.kore.king.entity.WithdrawalRequest;
import com.kore.king.service.UserService;
import com.kore.king.service.WithdrawalService;

@Controller
@RequestMapping("/user")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final UserService userService;

    public WithdrawalController(WithdrawalService withdrawalService, UserService userService) {
        this.withdrawalService = withdrawalService;
        this.userService = userService;
    }

    @GetMapping("/withdraw")
    public String withdraw(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<WithdrawalRequest> withdrawalHistory = withdrawalService.getUserWithdrawalRequests(user.getId());
        boolean dailyLimitReached = withdrawalService.hasReachedDailyLimit(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("withdrawalHistory", withdrawalHistory);
        model.addAttribute("withdrawalMethods", WithdrawalMethod.values());
        model.addAttribute("dailyLimitReached", dailyLimitReached);
        model.addAttribute("pageTitle", "Withdraw");
        return "user/withdraw-content";
    }

    @PostMapping("/withdraw/request")
    public String requestWithdrawal(@RequestParam Integer points,
                                  @RequestParam WithdrawalMethod method,
                                  @RequestParam(required = false) String upiId,
                                  @RequestParam(required = false) String accountNumber,
                                  @RequestParam(required = false) String ifscCode,
                                  @RequestParam(required = false) String accountHolderName,
                                  @RequestParam("screenshot") MultipartFile screenshot,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate file
            if (screenshot.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Screenshot is required");
                return "redirect:/user/withdraw-content";
            }

            // Create withdrawal request
            WithdrawalRequest withdrawalRequest = new WithdrawalRequest(user, points, method);
            withdrawalRequest.setUpiId(upiId);
            withdrawalRequest.setAccountNumber(accountNumber);
            withdrawalRequest.setIfscCode(ifscCode);
            withdrawalRequest.setAccountHolderName(accountHolderName);

            withdrawalService.createWithdrawalRequest(withdrawalRequest, user, screenshot);
            
            redirectAttributes.addFlashAttribute("success", 
                "Withdrawal request submitted successfully. We'll process it within 24 hours.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred");
        }

        return "redirect:/user/withdraw-content";
    }
}