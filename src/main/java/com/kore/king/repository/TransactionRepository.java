package com.kore.king.repository;


import com.kore.king.entity.Transaction;
import com.kore.king.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromUserOrToUserOrderByCreatedAtDesc(User fromUser, User toUser);
    
    @Query("SELECT t FROM Transaction t WHERE t.fromUser.id = :userId OR t.toUser.id = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findUserTransactions(@Param("userId") Long userId);
    
    List<Transaction> findByBetIdOrderByCreatedAtDesc(Long betId);
}