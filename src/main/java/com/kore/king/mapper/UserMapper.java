package com.kore.king.mapper;

import org.springframework.stereotype.Component;

import com.kore.king.dto.UserDTO;
import com.kore.king.entity.User;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvailablePoints(user.getAvailablePoints());
        dto.setHeldPoints(user.getHeldPoints());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setWins(user.getWins());
        dto.setLosses(user.getLosses());
        dto.setDisputes(user.getDisputes());

        return dto;
    }
}