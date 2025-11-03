package com.kore.king.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    private int points = 1000; // Changed from 0 to 1000 for starting points

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Bet> bets = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Bet> createdBets = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    private int availablePoints = 1000; // Added initialization
    private int heldPoints = 0; // Added initialization

    public User() {}
    
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters and Setters - ALL PUBLIC
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; } // FIXED: Made public

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public List<Bet> getBets() { return bets; } // FIXED: Made public
    public void setBets(List<Bet> bets) { this.bets = bets; } // FIXED: Made public

    public int getAvailablePoints() { return availablePoints; } // FIXED: Made public
    public void setAvailablePoints(int availablePoints) { this.availablePoints = availablePoints; } // FIXED: Made public

    public int getHeldPoints() { return heldPoints; } // FIXED: Made public
    public void setHeldPoints(int heldPoints) { this.heldPoints = heldPoints; } // FIXED: Made public

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public List<Bet> getCreatedBets() { return createdBets; }
    public void setCreatedBets(List<Bet> createdBets) { this.createdBets = createdBets; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}