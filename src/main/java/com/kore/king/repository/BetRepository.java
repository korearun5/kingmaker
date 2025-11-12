package com.kore.king.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;

@Repository
public interface BetRepository extends JpaRepository<Bet, Long> {
    
    // Find available bets (PENDING status, excluding user's own bets)
    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.creator.id != :userId ORDER BY b.createdAt DESC")
    Page<Bet> findAvailableBets(@Param("status") BetStatus status, 
                               @Param("userId") Long userId, 
                               Pageable pageable);
    
    // Find user's bets (both created and accepted)
    @Query("SELECT b FROM Bet b WHERE b.creator.id = :userId OR b.acceptor.id = :userId ORDER BY b.createdAt DESC")
    Page<Bet> findUserBets(@Param("userId") Long userId, Pageable pageable);
    
    // Find user's active bets (PENDING, ACCEPTED, CODE_SHARED)
    @Query("SELECT b FROM Bet b WHERE (b.creator.id = :userId OR b.acceptor.id = :userId) " +
           "AND b.status IN (com.kore.king.entity.BetStatus.PENDING, " +
           "com.kore.king.entity.BetStatus.ACCEPTED, " +
           "com.kore.king.entity.BetStatus.CODE_SHARED) " +
           "ORDER BY b.createdAt DESC")
    List<Bet> findUserActiveBets(@Param("userId") Long userId);
    
    // Find bet with acceptor eagerly loaded
    @Query("SELECT b FROM Bet b LEFT JOIN FETCH b.acceptor WHERE b.id = :betId")
    Optional<Bet> findByIdWithAcceptor(@Param("betId") Long betId);
    
    // Find bets by status
    List<Bet> findByStatus(BetStatus status);
    
    // Find bets by creator
    List<Bet> findByCreatorId(Long creatorId);
    
    // Find bets by acceptor
    List<Bet> findByAcceptorId(Long acceptorId);
    
    // Check if user has active bets
    @Query("SELECT COUNT(b) > 0 FROM Bet b WHERE (b.creator.id = :userId OR b.acceptor.id = :userId) " +
           "AND b.status IN (com.kore.king.entity.BetStatus.PENDING, " +
           "com.kore.king.entity.BetStatus.ACCEPTED, " +
           "com.kore.king.entity.BetStatus.CODE_SHARED)")
    boolean hasActiveBets(@Param("userId") Long userId);

    long countByStatus(BetStatus status);
    
    long countByStatusIn(List<BetStatus> statuses);
    
    long countByCreatedAtAfter(LocalDateTime dateTime);
    
    @Query("SELECT SUM(b.points) FROM Bet b WHERE b.status = 'COMPLETED'")
    Optional<Integer> sumCompletedBetPoints();
}