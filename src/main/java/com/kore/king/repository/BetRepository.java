package com.kore.king.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.entity.User;

public interface BetRepository extends JpaRepository<Bet, Long> {
    
    // ADD THESE MISSING METHODS:
    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.creator != :creator ORDER BY b.createdAt DESC")
    List<Bet> findByStatusAndCreatorNot(@Param("status") BetStatus status, @Param("creator") User creator);


    @Query("SELECT b FROM Bet b WHERE b.status = :status AND " +
           "(LOWER(b.gameType) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "b.points = :points) AND b.creator.id != :userId")
    Page<Bet> searchAvailableBets(@Param("status") BetStatus status,
                                 @Param("userId") Long userId,
                                 @Param("search") String search,
                                 @Param("points") Integer points,
                                 Pageable pageable);
    
    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.creator.id != :userId ORDER BY b.createdAt DESC")
    Page<Bet> findAvailableBets(@Param("status") BetStatus status, 
                               @Param("userId") Long userId, 
                               Pageable pageable);
    
    @Query("SELECT b FROM Bet b WHERE b.creator.id = :userId ORDER BY b.createdAt DESC")
    Page<Bet> findUserBets(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(b) FROM Bet b WHERE b.creator.id = :userId AND b.status IN :statuses")
    long countUserActiveBets(@Param("userId") Long userId, @Param("statuses") List<BetStatus> statuses);
    
    // Your existing methods:
    List<Bet> findByStatus(BetStatus status);
    List<Bet> findByGameTypeAndStatus(String gameType, BetStatus status);
    List<Bet> findByCreatorIdAndStatus(Long creatorId, BetStatus status);
    
    // Simple method for findAllOpenBets (returns all PENDING bets)
    default List<Bet> findAllOpenBets() {
        return findByStatus(BetStatus.PENDING);
    }
}