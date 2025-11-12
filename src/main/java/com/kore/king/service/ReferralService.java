package com.kore.king.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kore.king.entity.Referral;
import com.kore.king.entity.User;
import com.kore.king.repository.ReferralRepository;

import java.util.Optional;

@Service
@Transactional
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final UserService userService;

    public ReferralService(ReferralRepository referralRepository, UserService userService) {
        this.referralRepository = referralRepository;
        this.userService = userService;
    }

    public Optional<Referral> findReferralByReferredUser(User referredUser) {
        return referralRepository.findByReferred(referredUser);
    }

    public boolean hasActiveReferrer(User user) {
        return referralRepository.findByReferredAndIsActive(user, true).isPresent();
    }

    public Referral getReferrer(User referredUser) {
        return referralRepository.findByReferredAndIsActive(referredUser, true)
                .orElse(null);
    }

    @Transactional
    public Referral createReferral(User referrer, User referredUser) {
        // Check if referral already exists
        if (referralRepository.findByReferred(referredUser).isPresent()) {
            throw new RuntimeException("User already referred");
        }

        Referral referral = new Referral(referrer, referredUser);
        return referralRepository.save(referral);
    }

    @Transactional
    public void awardReferralCommission(User winner, int betAmount) {
        Referral referral = getReferrer(winner);
        if (referral != null) {
            double commission = betAmount * 0.01; // 1% commission
            
            // Update referral stats
            referral.setTotalCommissionEarned(referral.getTotalCommissionEarned() + commission);
            referral.setTotalReferredWins(referral.getTotalReferredWins() + 1);
            referralRepository.save(referral);
            
            // Award commission to referrer
            User referrer = referral.getReferrer();
            referrer.setAvailablePoints(referrer.getAvailablePoints() + (int) commission);
            userService.saveUser(referrer);
        }
    }

    public long getReferralCount(User referrer) {
        return referralRepository.countByReferrerAndIsActive(referrer, true);
    }
}