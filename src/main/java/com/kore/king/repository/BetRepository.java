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
    
    @Query("SELECT b FROM Bet b WHERE b.status = :status AND b.creator.id != :userId ORDER BY b.createdAt DESC")
    Page<Bet> findAvailableBets(@Param("status") BetStatus status, 
                               @Param("userId") Long userId, 
                               Pageable pageable);

    @Query("SELECT b FROM Bet b WHERE b.status = :status AND (b.creator.id = :userId OR b.acceptor.id = :userId)")
    List<Bet> findByStatusAndUserId(@Param("status") BetStatus status, 
                                   @Param("userId") Long userId);

    @Query("SELECT b FROM Bet b WHERE (b.creator.id = :userId OR b.acceptor.id = :userId) AND b.status IN ('ACCEPTED', 'CODE_SHARED')")
    List<Bet> findActiveBetsByUser(@Param("userId") Long userId);

    @Query("SELECT b FROM Bet b WHERE b.creator.id = :userId OR b.acceptor.id = :userId ORDER BY b.createdAt DESC")
    Page<Bet> findUserBets(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT b FROM Bet b WHERE (b.creator.id = :userId OR b.acceptor.id = :userId) " +
           "AND b.status IN (com.kore.king.entity.BetStatus.PENDING, " +
           "com.kore.king.entity.BetStatus.ACCEPTED, " +
           "com.kore.king.entity.BetStatus.CODE_SHARED) " +
           "ORDER BY b.createdAt DESC")
    List<Bet> findUserActiveBets(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Bet b LEFT JOIN FETCH b.acceptor WHERE b.id = :betId")
    Optional<Bet> findByIdWithAcceptor(@Param("betId") Long betId);
    
    // REMOVE THE DUPLICATE: Only keep one findByStatus method
    List<Bet> findByStatus(BetStatus status); // KEEP THIS ONE
    
    List<Bet> findByCreatorId(Long creatorId);
    
    List<Bet> findByAcceptorId(Long acceptorId);
    
    @Query("SELECT b FROM Bet b WHERE b.status IN :statuses AND (b.creator.id = :userId OR b.acceptor.id = :userId)")
    List<Bet> findByStatusInAndUserId(@Param("statuses") List<BetStatus> statuses,
                                     @Param("userId") Long userId);
    
    @Query("SELECT COUNT(b) > 0 FROM Bet b WHERE (b.creator.id = :userId OR b.acceptor.id = :userId) " +
           "AND b.status IN (com.kore.king.entity.BetStatus.PENDING, " +
           "com.kore.king.entity.BetStatus.ACCEPTED, " +
           "com.kore.king.entity.BetStatus.CODE_SHARED)")
    boolean hasActiveBets(@Param("userId") Long userId);

    // KEEP ONLY ONE countByStatus method
    long countByStatus(BetStatus status);
    
    long countByStatusIn(List<BetStatus> statuses);
    
    long countByCreatedAtAfter(LocalDateTime dateTime);
    
    @Query("SELECT SUM(b.points) FROM Bet b WHERE b.status = 'COMPLETED'")
    Optional<Integer> sumCompletedBetPoints();
    
// In BetRepository
@Query("SELECT b FROM Bet b WHERE b.creator.id = :userId OR b.acceptor.id = :userId OR b.status = 'PENDING' ORDER BY b.createdAt DESC")
Page<Bet> findAllBetsForUserWithAvailable(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT b FROM Bet b WHERE b.creator.id = :userId OR b.acceptor.id = :userId ORDER BY b.createdAt DESC")
    Page<Bet> findAllBetsForUser(@Param("userId") Long userId, Pageable pageable);
}