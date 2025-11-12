package com.kore.king.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kore.king.entity.PaymentRequest;
import com.kore.king.entity.PaymentStatus;
import com.kore.king.entity.User;
import com.kore.king.repository.PaymentRequestRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;

    public PaymentService(PaymentRequestRepository paymentRequestRepository, 
                         FileStorageService fileStorageService,
                         UserService userService) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
    }

    public PaymentRequest createPaymentRequest(PaymentRequest paymentRequest, User user, MultipartFile screenshot) {
        // Validate amount
        if (paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        // Store screenshot
        String screenshotPath = fileStorageService.storeFile(screenshot);
        paymentRequest.setScreenshotPath(screenshotPath);
        paymentRequest.setUser(user);
        paymentRequest.setStatus(PaymentStatus.PENDING);
        paymentRequest.setCreatedAt(LocalDateTime.now());

        return paymentRequestRepository.save(paymentRequest);
    }

    public List<PaymentRequest> getUserPaymentRequests(Long userId) {
        return paymentRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<PaymentRequest> getPendingPaymentRequests() {
        return paymentRequestRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING);
    }

    @Transactional
    public PaymentRequest approvePaymentRequest(Long paymentRequestId, String approvedBy, String transactionId) {
        PaymentRequest paymentRequest = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found"));

        if (paymentRequest.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment request is not pending");
        }

        // Convert amount to points (1 rupee = 1 point for now)
        int pointsToAdd = paymentRequest.getAmount().intValue();

        // Add points to user
        User user = paymentRequest.getUser();
        user.setAvailablePoints(user.getAvailablePoints() + pointsToAdd);
        userService.saveUser(user);

        // Update payment request
        paymentRequest.setStatus(PaymentStatus.APPROVED);
        paymentRequest.setProcessedAt(LocalDateTime.now());
        paymentRequest.setTransactionId(transactionId);
        paymentRequest.setNotes("Approved by " + approvedBy);

        return paymentRequestRepository.save(paymentRequest);
    }

    @Transactional
    public PaymentRequest rejectPaymentRequest(Long paymentRequestId, String rejectedBy, String reason) {
        PaymentRequest paymentRequest = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found"));

        if (paymentRequest.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment request is not pending");
        }

        paymentRequest.setStatus(PaymentStatus.REJECTED);
        paymentRequest.setProcessedAt(LocalDateTime.now());
        paymentRequest.setNotes("Rejected by " + rejectedBy + ". Reason: " + reason);

        return paymentRequestRepository.save(paymentRequest);
    }

    public Optional<PaymentRequest> findById(Long id) {
        return paymentRequestRepository.findById(id);
    }
}