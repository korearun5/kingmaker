package com.kore.king.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kore.king.entity.Transaction;
import com.kore.king.entity.User;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByFromUserOrToUserOrderByCreatedAtDesc(User fromUser, User toUser);
    
    @Query("SELECT t FROM Transaction t WHERE t.fromUser.id = :userId OR t.toUser.id = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findUserTransactions(@Param("userId") Long userId);
    
    List<Transaction> findByBetIdOrderByCreatedAtDesc(Long betId);
}