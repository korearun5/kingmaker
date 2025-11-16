package com.kore.king.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.kore.king.entity.BetStatus;
import com.kore.king.entity.Result;

public class BetDTO {
    private Long id;
    private String title;
    private String description;
    private Integer points;
    private BetStatus status;
    private String gameCode;
    private String gameType;
    private Result creatorResult;
    private Result acceptorResult;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime codeSharedAt;
    private LocalDateTime completedAt;
    private Map<String, Object> creator;
    private Map<String, Object> acceptor;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCodeSharedAt() { return codeSharedAt; }
    public void setCodeSharedAt(LocalDateTime codeSharedAt) { this.codeSharedAt = codeSharedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Map<String, Object> getCreator() { return creator; }
    public void setCreator(Map<String, Object> creator) { this.creator = creator; }
    public Map<String, Object> getAcceptor() { return acceptor; }
    public void setAcceptor(Map<String, Object> acceptor) { this.acceptor = acceptor; }
}