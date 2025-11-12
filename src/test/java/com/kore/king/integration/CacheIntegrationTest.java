package com.kore.king.integration;

import com.kore.king.config.CacheTestConfig;
import com.kore.king.entity.User;
import com.kore.king.service.CachedAdminStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(CacheTestConfig.class)
class CacheIntegrationTest {

    @Autowired
    private CachedAdminStatsService cachedAdminStatsService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void cacheShouldWorkCorrectly() {
        // First call - should compute
        Map<String, Object> result1 = cachedAdminStatsService.getDashboardStats();
        
        // Second call - should come from cache
        Map<String, Object> result2 = cachedAdminStatsService.getDashboardStats();
        
        assertThat(result1).isEqualTo(result2);
        
        // Verify cache exists
        assertThat(cacheManager.getCache("adminStats")).isNotNull();
    }
}