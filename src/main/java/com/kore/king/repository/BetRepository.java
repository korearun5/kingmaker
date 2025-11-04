package com.kore.king.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;
import com.kore.king.entity.User;

public interface BetRepository extends JpaRepository<Bet, Long> {
    
    // FIXED: Added missing method for finding available bets excluding current user
    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.creator != :creator ORDER BY b.createdAt DESC")
    List<Bet> findByStatusAndCreatorNot(@Param("status") BetStatus status, @Param("creator") User creator);

    // FIXED: Corrected search query to handle null search parameters properly
    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.creator.id != :userId " +
           "AND (:search IS NULL OR LOWER(b.gameType) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:points IS NULL OR b.points = :points) " +
           "ORDER BY b.createdAt DESC")
    Page<Bet> searchAvailableBets(@Param("status") BetStatus status,
                                 @Param("userId") Long userId,
                                 @Param("search") String search,
                                 @Param("points") Integer points,
                                 Pageable pageable);
    
    // FIXED: This method is correct
    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.creator.id != :userId ORDER BY b.createdAt DESC")
    Page<Bet> findAvailableBets(@Param("status") BetStatus status, 
                               @Param("userId") Long userId, 
                               Pageable pageable);
    
    // FIXED: This method is correct
    @Query("SELECT b FROM Bet b WHERE b.creator.id = :userId ORDER BY b.createdAt DESC")
    Page<Bet> findUserBets(@Param("userId") Long userId, Pageable pageable);
    
    // FIXED: This method is correct
    @Query("SELECT COUNT(b) FROM Bet b WHERE b.creator.id = :userId AND b.status IN :statuses")
    long countUserActiveBets(@Param("userId") Long userId, @Param("statuses") List<BetStatus> statuses);
    
    // FIXED: Added method to find bets by status
    List<Bet> findByStatus(BetStatus status);
    
    // FIXED: Added method to find bets by game type and status
    List<Bet> findByGameTypeAndStatus(String gameType, BetStatus status);
    
    // FIXED: Added method to find user's bets by status
    List<Bet> findByCreatorIdAndStatus(Long creatorId, BetStatus status);
    
    // FIXED: Added method to find bets that are accepted but waiting for code
    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.creator.id = :userId AND b.userProvidedCode IS NULL")
    List<Bet> findByCreatorIdAndStatusAndUserProvidedCodeIsNull(@Param("userId") Long userId, 
                                                               @Param("status") BetStatus status);
    
    // FIXED: Added method to find active matches for user (ACCEPTED or CODE_SHARED)
    @Query("SELECT b FROM Bet b WHERE b.creator.id = :userId AND (b.status = 'ACCEPTED' OR b.status = 'CODE_SHARED')")
    List<Bet> findUserActiveMatches(@Param("userId") Long userId);
    
    // FIXED: Simple method for findAllOpenBets (returns all PENDING bets)
    default List<Bet> findAllOpenBets() {
        return findByStatus(BetStatus.PENDING);
    }
    
    // FIXED: Added method to find bet with matched bet eager loaded
    @Query("SELECT b FROM Bet b LEFT JOIN FETCH b.matchedBet WHERE b.id = :betId")
    Optional<Bet> findByIdWithMatchedBet(@Param("betId") Long betId);
    
    // FIXED: Added method to check if user has already submitted result
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Bet b WHERE b.id = :betId AND " +
           "((b.creator.username = :username AND b.creatorSubmitted = true) OR " +
           "(b.matchedBet.creator.username = :username AND b.acceptorSubmitted = true))")
    boolean hasUserSubmittedResult(@Param("betId") Long betId, @Param("username") String username);

    // Add these to your BetRepository interface
    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.matchedBet.creator.id = :userId")
    List<Bet> findByStatusAndMatchedBet_Creator_Id(@Param("status") BetStatus status, @Param("userId") Long userId);

    // ADD THIS MISSING METHOD - Used in WebSocketEventHandler
    @Query("SELECT b FROM Bet b WHERE b.matchedBet.id = :matchedBetId")
    Optional<Bet> findByMatchedBetId(@Param("matchedBetId") Long matchedBetId);
}