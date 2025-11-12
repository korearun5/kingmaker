package com.kore.king.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kore.king.entity.WithdrawalRequest;
import com.kore.king.entity.WithdrawalStatus;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status);
    List<WithdrawalRequest> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, WithdrawalStatus status);
    
    //@Query("SELECT COUNT(w) FROM WithdrawalRequest w WHERE w.user.id = :userId AND DATE(w.createdAt) = CURRENT_DATE")
    //long countByUserIdAndCreatedAtToday(@Param("userId") Long userId);

    // Fix: Use date range for today
    @Query("SELECT COUNT(w) FROM WithdrawalRequest w WHERE w.user.id = :userId AND w.createdAt >= :startOfDay AND w.createdAt < :endOfDay")
    long countByUserIdAndCreatedAtBetween(
        @Param("userId") Long userId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );
        // Helper default method for today's count
    default long countByUserIdAndCreatedAtToday(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return countByUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay);
    }

    long countByStatus(WithdrawalStatus status);
}