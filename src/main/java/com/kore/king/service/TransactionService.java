package com.kore.king.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kore.king.entity.Bet;
import com.kore.king.entity.Transaction;
import com.kore.king.entity.TransactionType;
import com.kore.king.repository.TransactionRepository;

@Service
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    public void recordBetCreation(Bet bet) {
        Transaction transaction = new Transaction();
        transaction.setFromUser(bet.getCreator());
        transaction.setPoints(bet.getPoints());
        transaction.setType(TransactionType.CREATION);
        transaction.setBet(bet);
        transaction.setDescription("Bet creation for " + bet.getGameType());
        
        transactionRepository.save(transaction);
    }
    
    public void recordBetWin(Bet winningBet) {
        Bet losingBet = winningBet.getMatchedBet();
        int points = winningBet.getPoints();
        
        // Record points transfer from loser to winner
        Transaction winTransaction = new Transaction();
        winTransaction.setFromUser(losingBet.getCreator());
        winTransaction.setToUser(winningBet.getCreator());
        winTransaction.setPoints(points * 2); // Winner gets both stakes
        winTransaction.setType(TransactionType.WIN);
        winTransaction.setBet(winningBet);
        winTransaction.setDescription("Won bet against " + losingBet.getCreator().getUsername());
        
        transactionRepository.save(winTransaction);
    }
    
    public void recordBetRefund(Bet bet) {
        Transaction transaction = new Transaction();
        transaction.setToUser(bet.getCreator());
        transaction.setPoints(bet.getPoints());
        transaction.setType(TransactionType.REFUND);
        transaction.setBet(bet);
        transaction.setDescription("Bet cancellation refund");
        
        transactionRepository.save(transaction);
    }
}