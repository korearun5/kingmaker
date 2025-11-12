package com.kore.king.mapper;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.kore.king.dto.BetDTO;
import com.kore.king.entity.Bet;

@Component
public class BetMapper {

    public BetDTO toDTO(Bet bet) {
        if (bet == null) {
            return null;
        }

        BetDTO dto = new BetDTO();
        dto.setId(bet.getId());
        dto.setTitle(bet.getTitle());
        dto.setDescription(bet.getDescription());
        dto.setPoints(bet.getPoints());
        dto.setStatus(bet.getStatus());
        dto.setGameCode(bet.getGameCode());
        dto.setGameType(bet.getGameType());
        dto.setCreatorResult(bet.getCreatorResult());
        dto.setAcceptorResult(bet.getAcceptorResult());
        dto.setCreatedAt(bet.getCreatedAt());
        dto.setExpiresAt(bet.getExpiresAt());
        dto.setCodeSharedAt(bet.getCodeSharedAt());
        dto.setCompletedAt(bet.getCompletedAt());

        // Map creator
        if (bet.getCreator() != null) {
            Map<String, Object> creatorInfo = new HashMap<>();
            creatorInfo.put("id", bet.getCreator().getId());
            creatorInfo.put("username", bet.getCreator().getUsername());
            creatorInfo.put("availablePoints", bet.getCreator().getAvailablePoints());
            dto.setCreator(creatorInfo);
        }

        // Map acceptor
        if (bet.getAcceptor() != null) {
            Map<String, Object> acceptorInfo = new HashMap<>();
            acceptorInfo.put("id", bet.getAcceptor().getId());
            acceptorInfo.put("username", bet.getAcceptor().getUsername());
            acceptorInfo.put("availablePoints", bet.getAcceptor().getAvailablePoints());
            dto.setAcceptor(acceptorInfo);
        }

        return dto;
    }
}