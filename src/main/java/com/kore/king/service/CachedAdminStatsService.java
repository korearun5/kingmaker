package com.kore.king.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CachedAdminStatsService {

    private final AdminStatsService adminStatsService;

    public CachedAdminStatsService(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @Cacheable(value = "adminStats", key = "'dashboard'")
    public Map<String, Object> getDashboardStats() {
        return adminStatsService.getDashboardStats();
    }

    @Cacheable(value = "adminStats", key = "'recentActivity'")
    public Map<String, Long> getRecentActivity() {
        return adminStatsService.getRecentActivity();
    }

    // Auto-evict cache every 5 minutes
    @CacheEvict(value = "adminStats", allEntries = true)
    @Scheduled(fixedRate = 300000)
    public void clearAdminStatsCache() {
        // Auto-cleared by annotation
    }

    // Manual cache eviction
    @CacheEvict(value = "adminStats", allEntries = true)
    public void evictAdminStatsCache() {
        // Manual cache eviction
    }
}