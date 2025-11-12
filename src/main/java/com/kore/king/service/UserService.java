package com.kore.king.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kore.king.entity.User;
import com.kore.king.entity.UserRole;
import com.kore.king.repository.UserRepository;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User createAdminUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        User admin = new User(username, email, passwordEncoder.encode(password));
        admin.setRole(UserRole.EMPLOYEE_ADMIN);
        admin.setAvailablePoints(10000);
        
        return userRepository.save(admin);
    }

    public List<User> getAllAdmins() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.EMPLOYEE_ADMIN)
                .collect(Collectors.toList());
    }

    public boolean promoteToAdmin(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRole(UserRole.EMPLOYEE_ADMIN);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean demoteFromAdmin(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRole(UserRole.USER);
            userRepository.save(user);
            return true;
        }
        return false;
    }
    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Validate current password
                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    return false;
                }
                
                // Validate new password is different
                if (passwordEncoder.matches(newPassword, user.getPassword())) {
                    throw new RuntimeException("New password must be different from current password");
                }
                
                // Validate new password length
                if (newPassword.length() < 6) {
                    throw new RuntimeException("New password must be at least 6 characters long");
                }
                
                // Update password
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error changing password: " + e.getMessage());
        }
    }
public List<User> getAllUsers() {
    return userRepository.findAll();
}
}