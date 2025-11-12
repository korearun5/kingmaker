package com.kore.king.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_game_ids", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_name", "game_id"}))
public class UserGameId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "game_name", nullable = false)
    private String gameName;

    @Column(name = "game_id", nullable = false)
    private String gameId;

    private boolean isDefault = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Constructors
    public UserGameId() {}

    public UserGameId(User user, String gameName, String gameId) {
        this.user = user;
        this.gameName = gameName;
        this.gameId = gameId;
    }

    // Helper methods
    public void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { 
        this.gameName = gameName;
        markAsUpdated();
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { 
        this.gameId = gameId;
        markAsUpdated();
    }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { 
        this.isDefault = isDefault;
        markAsUpdated();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}