package com.kore.king.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kore.king.entity.Bet;
import com.kore.king.entity.Transaction;
import com.kore.king.entity.TransactionType;
import com.kore.king.entity.User;
import com.kore.king.repository.TransactionRepository;

@Service
@Transactional
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    @Transactional
    public void recordBetCreation(Bet bet) {
        Transaction transaction = new Transaction();
        transaction.setFromUser(bet.getCreator());
        transaction.setPoints(bet.getPoints());
        transaction.setType(TransactionType.BET_CREATION);
        transaction.setBet(bet);
        transaction.setDescription("Bet creation: " + bet.getTitle());
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }
    
    @Transactional
    public void recordBetAcceptance(Bet bet) {
        if (bet.getAcceptor() != null) {
            Transaction transaction = new Transaction();
            transaction.setFromUser(bet.getAcceptor());
            transaction.setPoints(bet.getPoints());
            transaction.setType(TransactionType.BET_ACCEPTANCE);
            transaction.setBet(bet);
            transaction.setDescription("Bet acceptance: " + bet.getTitle());
            transaction.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        }
    }
    
    @Transactional
    public void recordBetWin(Bet bet, User winner) {
        Transaction transaction = new Transaction();
        transaction.setToUser(winner);
        transaction.setPoints(bet.getPoints() * 2); // Winner gets both pots
        transaction.setType(TransactionType.WIN);
        transaction.setBet(bet);
        transaction.setDescription("Bet win: " + bet.getTitle());
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }
    
    @Transactional
    public void recordBetRefund(Bet bet) {
        // Refund creator
        Transaction creatorRefund = new Transaction();
        creatorRefund.setToUser(bet.getCreator());
        creatorRefund.setPoints(bet.getPoints());
        creatorRefund.setType(TransactionType.REFUND);
        creatorRefund.setBet(bet);
        creatorRefund.setDescription("Bet refund: " + bet.getTitle());
        creatorRefund.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(creatorRefund);
        
        // Refund acceptor if exists
        if (bet.getAcceptor() != null) {
            Transaction acceptorRefund = new Transaction();
            acceptorRefund.setToUser(bet.getAcceptor());
            acceptorRefund.setPoints(bet.getPoints());
            acceptorRefund.setType(TransactionType.REFUND);
            acceptorRefund.setBet(bet);
            acceptorRefund.setDescription("Bet refund: " + bet.getTitle());
            acceptorRefund.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(acceptorRefund);
        }
    }

    // Additional utility methods if needed
    public List<Transaction> getUserTransactions(Long userId) {
        return transactionRepository.findUserTransactions(userId);
    }
    
    public List<Transaction> getBetTransactions(Long betId) {
        return transactionRepository.findByBetIdOrderByCreatedAtDesc(betId);
    }
}