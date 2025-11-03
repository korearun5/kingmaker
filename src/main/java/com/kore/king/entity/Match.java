package com.kore.king.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "matches")
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "bet_id")
    private Bet bet;

    @ManyToOne
    private User opponent;

    private String sharedCode; // unique code for the game

    private MatchStatus status = MatchStatus.ACTIVE; // e.g., ACTIVE, PENDING, PLAYED, DISPUTED, COMPLETED

    private LocalDateTime createdAt = LocalDateTime.now();

    public Match() {}

        public Match(Bet bet, String sharedCode) {
        this.bet = bet;
        this.sharedCode = sharedCode;
    }
    // getters and setters

    public Long getId() {
        return id;
    }
    public Bet getBet(){
        return bet;
    }
    public User getOpponent() {
        return opponent;
    }
    public String getGameCode() {
        return sharedCode;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBet(Bet bet) {
        this.bet = bet;
    }

    public void setOpponent(User opponent) {
        this.opponent = opponent;;
    }

    public void setGameCode(String sharedCode) {
        this.sharedCode = sharedCode;
    }

    public void setStatus(MatchStatus status){
        this.status = status;
    }

    public void getCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}