package com.kore.king.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kore.king.config.AppConfig;
import com.kore.king.entity.User;
import com.kore.king.entity.WithdrawalRequest;
import com.kore.king.entity.WithdrawalStatus;
import com.kore.king.repository.WithdrawalRequestRepository;

@Service
@Transactional
public class WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final AppConfig appConfig;

    public WithdrawalService(WithdrawalRequestRepository withdrawalRequestRepository,
                           FileStorageService fileStorageService,
                           UserService userService,
                           AppConfig appConfig) {
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
        this.appConfig = appConfig;
    }

    public WithdrawalRequest createWithdrawalRequest(WithdrawalRequest withdrawalRequest, 
                                                   User user, 
                                                   MultipartFile screenshot) {
        // Validate withdrawal amount
        validateWithdrawalRequest(user, withdrawalRequest.getPoints());

        // Store screenshot
        String screenshotPath = fileStorageService.storeFile(screenshot);
        withdrawalRequest.setScreenshotPath(screenshotPath);
        withdrawalRequest.setUser(user);
        withdrawalRequest.setStatus(WithdrawalStatus.PENDING);
        withdrawalRequest.setCreatedAt(LocalDateTime.now());

        // Hold points from user
        user.holdPoints(withdrawalRequest.getPoints());
        userService.saveUser(user);

        return withdrawalRequestRepository.save(withdrawalRequest);
    }

    private void validateWithdrawalRequest(User user, Integer points) {
        // Check minimum withdrawal
        if (points < appConfig.getMinWithdrawalAmount()) {
            throw new RuntimeException("Minimum withdrawal is " + appConfig.getMinWithdrawalAmount() + " points");
        }

        // Check maximum withdrawal
        if (points > appConfig.getMaxWithdrawalAmount()) {
            throw new RuntimeException("Maximum withdrawal is " + appConfig.getMaxWithdrawalAmount() + " points");
        }

        // Check daily limit
        long todayWithdrawals = withdrawalRequestRepository.countByUserIdAndCreatedAtToday(user.getId());
        if (todayWithdrawals >= appConfig.getDailyWithdrawalLimit()) {
            throw new RuntimeException("Daily withdrawal limit reached. Maximum " + 
                appConfig.getDailyWithdrawalLimit() + " withdrawals per day");
        }

        // Check user has enough points
        if (user.getAvailablePoints() < points) {
            throw new RuntimeException("Insufficient points. Available: " + user.getAvailablePoints());
        }
    }

    public List<WithdrawalRequest> getUserWithdrawalRequests(Long userId) {
        return withdrawalRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<WithdrawalRequest> getPendingWithdrawalRequests() {
        return withdrawalRequestRepository.findByStatusOrderByCreatedAtDesc(WithdrawalStatus.PENDING);
    }

    @Transactional
    public WithdrawalRequest approveWithdrawalRequest(Long withdrawalId, String approvedBy, String transactionId) {
        WithdrawalRequest withdrawalRequest = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal request not found"));

        if (withdrawalRequest.getStatus() != WithdrawalStatus.PENDING) {
            throw new RuntimeException("Withdrawal request is not pending");
        }

        // Deduct points from user (they were already held)
        User user = withdrawalRequest.getUser();
        user.releasePoints(withdrawalRequest.getPoints()); // Release held points
        user.setAvailablePoints(user.getAvailablePoints() - withdrawalRequest.getPoints()); // Actually deduct
        userService.saveUser(user);

        // Update withdrawal request
        withdrawalRequest.setStatus(WithdrawalStatus.APPROVED);
        withdrawalRequest.setProcessedAt(LocalDateTime.now());
        withdrawalRequest.setTransactionId(transactionId);
        withdrawalRequest.setAdminNotes("Approved by " + approvedBy);

        return withdrawalRequestRepository.save(withdrawalRequest);
    }

    @Transactional
    public WithdrawalRequest rejectWithdrawalRequest(Long withdrawalId, String rejectedBy, String reason) {
        WithdrawalRequest withdrawalRequest = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal request not found"));

        if (withdrawalRequest.getStatus() != WithdrawalStatus.PENDING) {
            throw new RuntimeException("Withdrawal request is not pending");
        }

        // Return held points to user
        User user = withdrawalRequest.getUser();
        user.releasePoints(withdrawalRequest.getPoints());
        userService.saveUser(user);

        withdrawalRequest.setStatus(WithdrawalStatus.REJECTED);
        withdrawalRequest.setProcessedAt(LocalDateTime.now());
        withdrawalRequest.setAdminNotes("Rejected by " + rejectedBy + ". Reason: " + reason);

        return withdrawalRequestRepository.save(withdrawalRequest);
    }

    public Optional<WithdrawalRequest> findById(Long id) {
        return withdrawalRequestRepository.findById(id);
    }

    public boolean hasReachedDailyLimit(Long userId) {
        long todayWithdrawals = withdrawalRequestRepository.countByUserIdAndCreatedAtToday(userId);
        return todayWithdrawals >= appConfig.getDailyWithdrawalLimit();
    }
}