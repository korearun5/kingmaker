package com.kore.king.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
//Remove @RefreshScope (not needed for now)
public class AppConfig {
    
    // Withdrawal Settings
    @NotNull
    @Min(100)
    private Integer minWithdrawalAmount;
    
    @NotNull 
    @Max(100000)
    private Integer maxWithdrawalAmount;
    
    @Value("${app.withdrawal.daily-limit:1}")
    private Integer dailyWithdrawalLimit;
    
    @Value("${app.withdrawal.auto-approve-limit:1000}")
    private Integer autoApproveLimit;
    
    // Commission Settings
    @Value("${app.commission.platform.with-referral:0.03}")
    private Double platformFeeWithReferral;
    
    @Value("${app.commission.platform.without-referral:0.04}")
    private Double platformFeeWithoutReferral;
    
    @Value("${app.commission.referral:0.01}")
    private Double referralCommission;
    
    // Getters and setters
    public Integer getMinWithdrawalAmount() { return minWithdrawalAmount; }
    public void setMinWithdrawalAmount(Integer minWithdrawalAmount) { this.minWithdrawalAmount = minWithdrawalAmount; }
    
    public Integer getMaxWithdrawalAmount() { return maxWithdrawalAmount; }
    public void setMaxWithdrawalAmount(Integer maxWithdrawalAmount) { this.maxWithdrawalAmount = maxWithdrawalAmount; }
    
    public Integer getDailyWithdrawalLimit() { return dailyWithdrawalLimit; }
    public void setDailyWithdrawalLimit(Integer dailyWithdrawalLimit) { this.dailyWithdrawalLimit = dailyWithdrawalLimit; }
    
    public Integer getAutoApproveLimit() { return autoApproveLimit; }
    public void setAutoApproveLimit(Integer autoApproveLimit) { this.autoApproveLimit = autoApproveLimit; }
    
    public Double getPlatformFeeWithReferral() { return platformFeeWithReferral; }
    public void setPlatformFeeWithReferral(Double platformFeeWithReferral) { this.platformFeeWithReferral = platformFeeWithReferral; }
    
    public Double getPlatformFeeWithoutReferral() { return platformFeeWithoutReferral; }
    public void setPlatformFeeWithoutReferral(Double platformFeeWithoutReferral) { this.platformFeeWithoutReferral = platformFeeWithoutReferral; }
    
    public Double getReferralCommission() { return referralCommission; }
    public void setReferralCommission(Double referralCommission) { this.referralCommission = referralCommission; }
}