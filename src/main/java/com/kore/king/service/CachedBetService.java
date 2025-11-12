package com.kore.king.service;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CachedBetService {

    private final BetService betService;

    public CachedBetService(BetService betService) {
        this.betService = betService;
    }

    @Cacheable(value = "availableBets", key = "#status.name() + '_' + #userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<Bet> findAvailableBets(BetStatus status, Long userId, Pageable pageable) {
        return betService.findAvailableBets(status, userId, pageable);
    }

    @CacheEvict(value = {"availableBets", "userStats"}, allEntries = true)
    public Bet createBet(Long creatorId, Integer points, String gameType, String title, String description) {
        return betService.createBet(creatorId, points, gameType, title, description);
    }

    @CacheEvict(value = {"availableBets", "userStats"}, allEntries = true)
    public Bet acceptBet(Long betId, Long acceptorId) {
        return betService.acceptBet(betId, acceptorId);
    }

    @CacheEvict(value = {"availableBets", "userStats"}, allEntries = true)
    public void cancelBet(Long betId, String username) {
        betService.cancelBet(betId, username);
    }

    @CacheEvict(value = "userStats", allEntries = true)
    public void resolveBet(Bet bet) {
        betService.resolveBet(bet);
    }
}