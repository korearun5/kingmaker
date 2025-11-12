package com.kore.king.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kore.king.entity.PaymentRequest;
import com.kore.king.entity.PaymentStatus;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
    List<PaymentRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PaymentRequest> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    List<PaymentRequest> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status);
    long countByStatus(PaymentStatus status);
}