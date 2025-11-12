package com.kore.king.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_role", columnList = "role"),
    @Index(name = "idx_user_created", columnList = "createdAt")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Long version; // ADD OPTIMISTIC LOCKING
    
    @Column(unique = true, nullable = false)
    @Size(min = 3, max = 50)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(unique = true, nullable = false)
    @Email
    private String email;

    private int availablePoints = 1000;
    private int heldPoints = 0;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @JsonIgnore
    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Bet> createdBets = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "acceptor")
    private List<Bet> acceptedBets = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    private int wins = 0;
    private int losses = 0;
    private int disputes = 0;

    public User() {
        // Default constructor
    }
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    // FIXED: Thread-safe point operations
    public synchronized boolean canAffordBet(int points) {
        return availablePoints >= points;
    }
    
    public synchronized void holdPoints(int points) {
        if (!canAffordBet(points)) {
            throw new RuntimeException("Insufficient points. Available: " + availablePoints + ", Required: " + points);
        }
        this.availablePoints -= points;
        this.heldPoints += points;
    }
    
    public synchronized void releasePoints(int points) {
        if (this.heldPoints < points) {
            throw new RuntimeException("Cannot release more points than held. Held: " + heldPoints + ", Requested: " + points);
        }
        this.heldPoints -= points;
        this.availablePoints += points;
    }
    
    public synchronized void awardPoints(int points) {
        this.availablePoints += points;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(int availablePoints) { this.availablePoints = availablePoints; }
    public int getHeldPoints() { return heldPoints; }
    public void setHeldPoints(int heldPoints) { this.heldPoints = heldPoints; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public List<Bet> getCreatedBets() { return createdBets; }
    public void setCreatedBets(List<Bet> createdBets) { this.createdBets = createdBets; }
    public List<Bet> getAcceptedBets() { return acceptedBets; }
    public void setAcceptedBets(List<Bet> acceptedBets) { this.acceptedBets = acceptedBets; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getDisputes() { return disputes; }
    public void setDisputes(int disputes) { this.disputes = disputes; }
}