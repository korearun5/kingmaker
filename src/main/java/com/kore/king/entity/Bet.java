package com.kore.king.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
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

    private String userProvidedCode;     // Code provided by creator
    private String winnerUsername;       // Who won
    private String loserUsername;        // Who lost  
    private String winnerScreenshot;     // Only winner uploads
    private String creatorResult;        // WIN, LOSE, or PENDING
    private String acceptorResult;       // WIN, LOSE, or PENDING
    private Boolean creatorSubmitted = false;
    private Boolean acceptorSubmitted = false;

    @Column(name = "dispute_reason")
    private String disputeReason;

    // ADD THESE NEW FIELDS FOR REAL-TIME UPDATES
    private String creatorSocketId;
    private String acceptorSocketId;

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

    public String getUserProvidedCode() { return userProvidedCode; }
    public void setUserProvidedCode(String userProvidedCode) { this.userProvidedCode = userProvidedCode; }
    
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }
    
    public String getLoserUsername() { return loserUsername; }
    public void setLoserUsername(String loserUsername) { this.loserUsername = loserUsername; }
    
    public String getWinnerScreenshot() { return winnerScreenshot; }
    public void setWinnerScreenshot(String winnerScreenshot) { this.winnerScreenshot = winnerScreenshot; }
    
    public String getCreatorResult() { return creatorResult; }
    public void setCreatorResult(String creatorResult) { this.creatorResult = creatorResult; }
    
    public String getAcceptorResult() { return acceptorResult; }
    public void setAcceptorResult(String acceptorResult) { this.acceptorResult = acceptorResult; }
    
    public Boolean getCreatorSubmitted() { return creatorSubmitted; }
    public void setCreatorSubmitted(Boolean creatorSubmitted) { this.creatorSubmitted = creatorSubmitted; }
    
    public Boolean getAcceptorSubmitted() { return acceptorSubmitted; }
    public void setAcceptorSubmitted(Boolean acceptorSubmitted) { this.acceptorSubmitted = acceptorSubmitted; }

    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }

    // NEW GETTERS AND SETTERS
    public String getCreatorSocketId() { return creatorSocketId; }
    public void setCreatorSocketId(String creatorSocketId) { this.creatorSocketId = creatorSocketId; }
    
    public String getAcceptorSocketId() { return acceptorSocketId; }
    public void setAcceptorSocketId(String acceptorSocketId) { this.acceptorSocketId = acceptorSocketId; }
}