package com.kore.king.dto;

import java.time.LocalDateTime;

import com.kore.king.entity.UserRole;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private Integer availablePoints;
    private Integer heldPoints;
    private UserRole role;
    private LocalDateTime createdAt;
    private Integer wins;
    private Integer losses;
    private Integer disputes;

    // Constructors
    public UserDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(Integer availablePoints) { this.availablePoints = availablePoints; }
    public Integer getHeldPoints() { return heldPoints; }
    public void setHeldPoints(Integer heldPoints) { this.heldPoints = heldPoints; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getWins() { return wins; }
    public void setWins(Integer wins) { this.wins = wins; }
    public Integer getLosses() { return losses; }
    public void setLosses(Integer losses) { this.losses = losses; }
    public Integer getDisputes() { return disputes; }
    public void setDisputes(Integer disputes) { this.disputes = disputes; }
}