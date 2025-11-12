package com.kore.king.factory;

import com.kore.king.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TestDataFactory {

    public static User createUser(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setAvailablePoints(1000);
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public static User createAdminUser(Long id, String username, String email) {
        User user = createUser(id, username, email);
        user.setRole(UserRole.MAIN_ADMIN);
        return user;
    }

    public static Bet createBet(Long id, User creator, Integer points, String gameType) {
        Bet bet = new Bet(creator, points, gameType, "Test Bet " + id);
        bet.setId(id);
        bet.setStatus(BetStatus.PENDING);
        bet.setCreatedAt(LocalDateTime.now());
        bet.setExpiresAt(LocalDateTime.now().plusHours(24));
        return bet;
    }

    public static Bet createAcceptedBet(Long id, User creator, User acceptor, Integer points) {
        Bet bet = createBet(id, creator, points, "Ludo");
        bet.setAcceptor(acceptor);
        bet.setStatus(BetStatus.ACCEPTED);
        return bet;
    }

    public static PaymentRequest createPaymentRequest(Long id, User user, BigDecimal amount) {
        PaymentRequest request = new PaymentRequest();
        request.setId(id);
        request.setUser(user);
        request.setAmount(amount);
        request.setPaymentMethod(PaymentMethod.MANUAL_UPI);
        request.setStatus(PaymentStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    public static WithdrawalRequest createWithdrawalRequest(Long id, User user, Integer points) {
        WithdrawalRequest request = new WithdrawalRequest(user, points, WithdrawalMethod.MANUAL_UPI);
        request.setId(id);
        request.setStatus(WithdrawalStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }
}