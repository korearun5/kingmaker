package com.kore.king.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kore.king.dto.UserDTO;
import com.kore.king.entity.User;
import com.kore.king.mapper.UserMapper;
import com.kore.king.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserApiController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserApiController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDTO userDTO = userMapper.toDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> passwordRequest,
            Authentication authentication) {
        
        String username = authentication.getName();
        String currentPassword = passwordRequest.get("currentPassword");
        String newPassword = passwordRequest.get("newPassword");
        String confirmPassword = passwordRequest.get("confirmPassword");

        if (!newPassword.equals(confirmPassword)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "New passwords do not match");
            return ResponseEntity.badRequest().body(response);
        }

        boolean success = userService.changePassword(username, currentPassword, newPassword);
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Current password is incorrect");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("wins", user.getWins());
        stats.put("losses", user.getLosses());
        stats.put("availablePoints", user.getAvailablePoints());
        stats.put("heldPoints", user.getHeldPoints());
        stats.put("totalGames", user.getWins() + user.getLosses());

        return ResponseEntity.ok(stats);
    }
}