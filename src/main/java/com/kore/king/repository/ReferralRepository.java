package com.kore.king.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kore.king.entity.Referral;
import com.kore.king.entity.User;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {
    Optional<Referral> findByReferred(User referred);
    Optional<Referral> findByReferredAndIsActive(User referred, boolean isActive);
    long countByReferrerAndIsActive(User referrer, boolean isActive);
}