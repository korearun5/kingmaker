package com.kore.king.service;

import org.springframework.stereotype.Service;

import com.kore.king.entity.Bet;
import com.kore.king.entity.BetStatus;

@Service
public class LudoValidationService {

    public void validateGameStateTransition(Bet currentBet, BetStatus newStatus) {
        BetStatus currentStatus = currentBet.getStatus();
        
        switch (currentStatus) {
            case PENDING:
                if (newStatus != BetStatus.ACCEPTED && newStatus != BetStatus.CANCELLED) {
                    throw new InvalidGameStateException("Pending bets can only be accepted or cancelled");
                }
                break;
            case ACCEPTED:
                if (newStatus != BetStatus.CODE_SHARED && newStatus != BetStatus.CANCELLED) {
                    throw new InvalidGameStateException("Accepted bets can only have code shared or be cancelled");
                }
                break;
            case CODE_SHARED:
                if (newStatus != BetStatus.RESULTS_SUBMITTED && newStatus != BetStatus.DISPUTED) {
                    throw new InvalidGameStateException("Code shared bets can only have results submitted or be disputed");
                }
                break;
            case RESULTS_SUBMITTED:
                if (newStatus != BetStatus.COMPLETED && newStatus != BetStatus.DISPUTED) {
                    throw new InvalidGameStateException("Results submitted bets can only be completed or disputed");
                }
                break;
            default:
                throw new InvalidGameStateException("Invalid state transition from " + currentStatus);
        }
    }

    public void validateBothPlayersReady(Bet bet) {
        if (bet.getCreatorSocketId() == null || bet.getAcceptorSocketId() == null) {
            throw new GameNotReadyException("Both players must be connected to proceed");
        }
        
        if (bet.getCreator() == null || bet.getAcceptor() == null) {
            throw new GameNotReadyException("Both creator and acceptor must be present");
        }
    }

    public void validateGameCompletion(Bet bet) {
        if (!bet.bothResultsSubmitted()) {
            throw new InvalidGameStateException("Both players must submit results before completion");
        }
        
        if (bet.getCreatorResult() == null || bet.getAcceptorResult() == null) {
            throw new InvalidGameStateException("Both results must be submitted");
        }
    }
}

// Custom Exceptions
class InvalidGameStateException extends RuntimeException {
    public InvalidGameStateException(String message) {
        super(message);
    }
}

class GameNotReadyException extends RuntimeException {
    public GameNotReadyException(String message) {
        super(message);
    }
}