package com.kore.king.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AppConfig appConfig() {
        AppConfig config = new AppConfig();
        config.setMinWithdrawalAmount(100);
        config.setMaxWithdrawalAmount(10000);
        config.setDailyWithdrawalLimit(3);
        config.setAutoApproveLimit(1000);
        config.setPlatformFeeWithReferral(0.03);
        config.setPlatformFeeWithoutReferral(0.04);
        config.setReferralCommission(0.01);
        return config;
    }

    // Use in-memory cache for tests
    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
            "userStats", 
            "availableBets", 
            "adminStats",
            "paymentRequests",
            "withdrawalRequests"
        );
    }
}