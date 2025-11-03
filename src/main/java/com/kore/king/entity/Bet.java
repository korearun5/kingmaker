package com.kore.king.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bets")
public class Bet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private Integer points; // points to bet
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    private BetStatus status = BetStatus.PENDING; // e.g., OPEN, MATCHED, COMPLETED, CANCELLED

    private String sharedCode; // This will be shared when matched

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;
    private String gameType; // classic, popular
    @OneToOne
    @JoinColumn(name = "matched_bet_id")
    private Bet matchedBet; // The bet this was matched with


    public Bet() {}

    public Bet(User creator, Integer points, String gameType) {
        this.creator = creator;
        this.points = points;
        this.gameType = gameType;
        this.expiresAt = LocalDateTime.now().plusHours(24);
    }


    // getters and setters
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }

    public String getSharedCode() { return sharedCode; }
    public void setSharedCode(String sharedCode) { this.sharedCode = sharedCode; }

    public Bet getMatchedBet() { return matchedBet; }
    public void setMatchedBet(Bet matchedBet) { this.matchedBet = matchedBet; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Long getId() { return id; }

    public String getTitle(){ return title; }

    public String getDescription() {
        return description;
    }

    public Integer getPoints() {
        return points;
    }

    public BetStatus getStatus() {
        return status;
    }
   
    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public void setStatus(BetStatus status) {
        this.status = status;
    }



}
