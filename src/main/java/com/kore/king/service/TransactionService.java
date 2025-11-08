package com.kore.king.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kore.king.entity.Bet;
import com.kore.king.entity.Transaction;
import com.kore.king.entity.TransactionType;
import com.kore.king.entity.User;
import com.kore.king.repository.TransactionRepository;

@Service
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    public void recordBetCreation(Bet bet) {
        Transaction transaction = new Transaction();
        transaction.setFromUser(bet.getCreator());
        transaction.setPoints(bet.getPoints());
        transaction.setType(TransactionType.BET_CREATION);
        transaction.setBet(bet);
        transaction.setDescription("Bet creation - " + bet.getTitle());
        
        transactionRepository.save(transaction);
    }
    
    public void recordBetAcceptance(Bet bet) {
        Transaction transaction = new Transaction();
        transaction.setFromUser(bet.getAcceptor());
        transaction.setPoints(bet.getPoints());
        transaction.setType(TransactionType.BET_ACCEPTANCE);
        transaction.setBet(bet);
        transaction.setDescription("Bet acceptance - " + bet.getTitle());
        
        transactionRepository.save(transaction);
    }
    
    public void recordBetWin(Bet bet, User winner) {
        Transaction transaction = new Transaction();
        transaction.setToUser(winner);
        transaction.setPoints(bet.getPoints() * 2); // Winner gets both stakes
        transaction.setType(TransactionType.WIN);
        transaction.setBet(bet);
        transaction.setDescription("Won bet - " + bet.getTitle());
        
        transactionRepository.save(transaction);
    }
    
    public void recordBetRefund(Bet bet) {
        // Refund creator
        Transaction creatorRefund = new Transaction();
        creatorRefund.setToUser(bet.getCreator());
        creatorRefund.setPoints(bet.getPoints());
        creatorRefund.setType(TransactionType.REFUND);
        creatorRefund.setBet(bet);
        creatorRefund.setDescription("Bet cancellation refund - " + bet.getTitle());
        transactionRepository.save(creatorRefund);
        
        // Refund acceptor if exists
        if (bet.getAcceptor() != null) {
            Transaction acceptorRefund = new Transaction();
            acceptorRefund.setToUser(bet.getAcceptor());
            acceptorRefund.setPoints(bet.getPoints());
            acceptorRefund.setType(TransactionType.REFUND);
            acceptorRefund.setBet(bet);
            acceptorRefund.setDescription("Bet cancellation refund - " + bet.getTitle());
            transactionRepository.save(acceptorRefund);
        }
    }
}