package com.kore.king.controller.user;

import java.math.BigDecimal;
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

import com.kore.king.entity.PaymentMethod;
import com.kore.king.entity.PaymentRequest;
import com.kore.king.entity.User;
import com.kore.king.service.PaymentService;
import com.kore.king.service.UserService;

@Controller
@RequestMapping("/user")
public class UserPaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public UserPaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @GetMapping("/buy-points")
    public String buyPoints(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<PaymentRequest> paymentHistory = paymentService.getUserPaymentRequests(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("paymentHistory", paymentHistory);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("pageTitle", "Buy Points");
        return "user/buy-points-content";
    }

    @PostMapping("/buy-points/request")
    public String requestPayment(@RequestParam BigDecimal amount,
                               @RequestParam PaymentMethod paymentMethod,
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
                return "redirect:/buy-points-content";
            }

            // Create payment request
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setAmount(amount);
            paymentRequest.setPaymentMethod(paymentMethod);
            paymentRequest.setUpiId(upiId);
            paymentRequest.setAccountNumber(accountNumber);
            paymentRequest.setIfscCode(ifscCode);
            paymentRequest.setAccountHolderName(accountHolderName);

            paymentService.createPaymentRequest(paymentRequest, user, screenshot);
            
            redirectAttributes.addFlashAttribute("success", 
                "Payment request submitted successfully. We'll process it within 24 hours.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred");
        }

        return "redirect:/user/buy-points-content";
    }
}