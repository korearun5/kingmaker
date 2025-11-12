package com.kore.king.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "referrals")
public class Referral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @ManyToOne
    @JoinColumn(name = "referred_id", nullable = false, unique = true)
    private User referred;

    private Double totalCommissionEarned = 0.0;
    private Integer totalReferredWins = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean isActive = true;

    // Constructors
    public Referral() {}

    public Referral(User referrer, User referred) {
        this.referrer = referrer;
        this.referred = referred;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getReferrer() { return referrer; }
    public void setReferrer(User referrer) { this.referrer = referrer; }

    public User getReferred() { return referred; }
    public void setReferred(User referred) { this.referred = referred; }

    public Double getTotalCommissionEarned() { return totalCommissionEarned; }
    public void setTotalCommissionEarned(Double totalCommissionEarned) { this.totalCommissionEarned = totalCommissionEarned; }

    public Integer getTotalReferredWins() { return totalReferredWins; }
    public void setTotalReferredWins(Integer totalReferredWins) { this.totalReferredWins = totalReferredWins; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}