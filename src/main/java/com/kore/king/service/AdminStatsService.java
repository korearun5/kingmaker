package com.kore.king.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kore.king.entity.BetStatus;
import com.kore.king.entity.PaymentStatus;
import com.kore.king.entity.WithdrawalStatus;
import com.kore.king.repository.BetRepository;
import com.kore.king.repository.PaymentRequestRepository;
import com.kore.king.repository.UserRepository;
import com.kore.king.repository.WithdrawalRequestRepository;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final SupportService supportService;

    public AdminStatsService(UserRepository userRepository,
                           BetRepository betRepository,
                           PaymentRequestRepository paymentRequestRepository,
                           WithdrawalRequestRepository withdrawalRequestRepository,
                           SupportService supportService) {
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.paymentRequestRepository = paymentRequestRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.supportService = supportService;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // User Statistics
        stats.put("totalUsers", userRepository.count());
        stats.put("todayRegistrations", userRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay()));

        // Bet Statistics
        stats.put("totalBets", betRepository.count());
        stats.put("activeBets", betRepository.countByStatusIn(
            java.util.List.of(BetStatus.PENDING, BetStatus.ACCEPTED, BetStatus.CODE_SHARED)));
        stats.put("completedBets", betRepository.countByStatus(BetStatus.COMPLETED));
        stats.put("disputedBets", betRepository.countByStatus(BetStatus.DISPUTED));

        // Payment Statistics
        stats.put("pendingPayments", paymentRequestRepository.countByStatus(PaymentStatus.PENDING));
        stats.put("totalPaymentAmount", calculateTotalPaymentAmount());

        // Withdrawal Statistics
        stats.put("pendingWithdrawals", withdrawalRequestRepository.countByStatus(WithdrawalStatus.PENDING));
        stats.put("totalWithdrawalAmount", calculateTotalWithdrawalAmount());

        // Support Statistics
        stats.put("openTickets", supportService.getOpenTicketCount());

        // Platform Revenue (simplified calculation)
        stats.put("platformRevenue", calculatePlatformRevenue());

        return stats;
    }

    private BigDecimal calculateTotalPaymentAmount() {
        return paymentRequestRepository.findAll().stream()
                .map(request -> request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalWithdrawalAmount() {
        return withdrawalRequestRepository.findAll().stream()
                .map(request -> request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePlatformRevenue() {
        // This would need to be calculated from transaction records
        // For now, return a placeholder
        return BigDecimal.valueOf(0);
    }

    public Map<String, Long> getRecentActivity() {
        Map<String, Long> activity = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        // Recent registrations
        activity.put("registrationsLast7Days", 
            userRepository.countByCreatedAtAfter(weekAgo.atStartOfDay()));

        // Recent bets
        activity.put("betsLast7Days", 
            betRepository.countByCreatedAtAfter(weekAgo.atStartOfDay()));

        return activity;
    }
}