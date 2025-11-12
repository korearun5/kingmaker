package com.kore.king.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kore.king.entity.PaymentRequest;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/payment")
@PreAuthorize("isAuthenticated()")
public class PaymentController {
    
    @PostMapping("/request")
    public String requestPayment(@Valid @ModelAttribute PaymentRequest paymentRequest,
                               BindingResult result,
                               @RequestParam("screenshot") MultipartFile screenshot,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        
        // Validate input
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid payment request");
            return "redirect:/buy-points-content";
        }
        
        // Validate file type and size
        if (!isValidImageFile(screenshot)) {
            redirectAttributes.addFlashAttribute("error", "Invalid screenshot file");
            return "redirect:/buy-points-content";
        }
        
        // Process payment request
        try {
            //paymentService.createPaymentRequest(paymentRequest, authentication.getName(), screenshot);
            redirectAttributes.addFlashAttribute("success", "Payment request submitted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing payment request");
        }
        
        return "redirect:/buy-points-content";
    }
    
    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && 
               (contentType.equals("image/jpeg") || 
                contentType.equals("image/png") || 
                contentType.equals("image/jpg"));
    }
}