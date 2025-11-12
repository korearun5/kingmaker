package com.kore.king.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "bets", indexes = {
    @Index(name = "idx_bet_status", columnList = "status"),
    @Index(name = "idx_bet_creator", columnList = "creator_id"),
    @Index(name = "idx_bet_acceptor", columnList = "acceptor_id"),
    @Index(name = "idx_bet_created", columnList = "createdAt"),
    @Index(name = "idx_bet_status_created", columnList = "status, createdAt")
})
public class Bet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String description;
    private Integer points;
    
    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
    @ManyToOne
    @JoinColumn(name = "acceptor_id")
    private User acceptor;
    
    @Enumerated(EnumType.STRING)
    private BetStatus status = BetStatus.PENDING;
    
    private String gameCode;
    private String gameType;
    
    @Enumerated(EnumType.STRING)
    private Result creatorResult;
    
    @Enumerated(EnumType.STRING)
    private Result acceptorResult;
    
    private String winnerScreenshot;
    private String disputeReason;
    private String cancelReason;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;
    private LocalDateTime codeSharedAt;
    private LocalDateTime completedAt;
    
    private String creatorSocketId;
    private String acceptorSocketId;

    @JsonIgnore
    @OneToMany(mappedBy = "bet", cascade = CascadeType.ALL)
    private List<Transaction> transactions = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "bet", cascade = CascadeType.ALL)
    private List<Screenshot> screenshots = new ArrayList<>();

    public Bet() {}

    public Bet(User creator, Integer points, String gameType, String title) {
        this.creator = creator;
        this.points = points;
        this.gameType = gameType;
        this.title = title;
        this.expiresAt = LocalDateTime.now().plusHours(24);
    }
    
    // Helper methods
    public boolean isUserParticipant(User user) {
        return creator.equals(user) || (acceptor != null && acceptor.equals(user));
    }
    
    public boolean isCreator(User user) {
        return creator.equals(user);
    }
    
    public boolean isAcceptor(User user) {
        return acceptor != null && acceptor.equals(user);
    }
    
    public boolean bothResultsSubmitted() {
        return creatorResult != null && acceptorResult != null;
    }
    
    public User determineWinner() {
        if (!bothResultsSubmitted()) return null;
        
        if (creatorResult == Result.WIN && acceptorResult == Result.LOSE) {
            return creator;
        } else if (creatorResult == Result.LOSE && acceptorResult == Result.WIN) {
            return acceptor;
        }
        return null;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    public User getAcceptor() { return acceptor; }
    public void setAcceptor(User acceptor) { this.acceptor = acceptor; }
    public BetStatus getStatus() { return status; }
    public void setStatus(BetStatus status) { this.status = status; }
    public String getGameCode() { return gameCode; }
    public void setGameCode(String gameCode) { this.gameCode = gameCode; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public Result getCreatorResult() { return creatorResult; }
    public void setCreatorResult(Result creatorResult) { this.creatorResult = creatorResult; }
    public Result getAcceptorResult() { return acceptorResult; }
    public void setAcceptorResult(Result acceptorResult) { this.acceptorResult = acceptorResult; }
    public String getWinnerScreenshot() { return winnerScreenshot; }
    public void setWinnerScreenshot(String winnerScreenshot) { this.winnerScreenshot = winnerScreenshot; }
    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCodeSharedAt() { return codeSharedAt; }
    public void setCodeSharedAt(LocalDateTime codeSharedAt) { this.codeSharedAt = codeSharedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getCreatorSocketId() { return creatorSocketId; }
    public void setCreatorSocketId(String creatorSocketId) { this.creatorSocketId = creatorSocketId; }
    public String getAcceptorSocketId() { return acceptorSocketId; }
    public void setAcceptorSocketId(String acceptorSocketId) { this.acceptorSocketId = acceptorSocketId; }
    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
    public List<Screenshot> getScreenshots() { return screenshots; }
    public void setScreenshots(List<Screenshot> screenshots) { this.screenshots = screenshots; }
}