package com.kore.king.controller.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kore.king.dto.BetDTO;
import com.kore.king.dto.CreateBetRequest;
import com.kore.king.entity.Bet;
import com.kore.king.entity.User;
import com.kore.king.mapper.BetMapper;
import com.kore.king.service.BetService;
import com.kore.king.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/bets")
public class BetApiController {

    private final BetService betService;
    private final UserService userService;
    private final BetMapper betMapper;

    public BetApiController(BetService betService, UserService userService, BetMapper betMapper) {
        this.betService = betService;
        this.userService = userService;
        this.betMapper = betMapper;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAvailableBets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Bet> betsPage = betService.findAvailableBets(com.kore.king.entity.BetStatus.PENDING, user.getId(), pageable);
        Page<BetDTO> betsDTOPage = betsPage.map(betMapper::toDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("bets", betsDTOPage.getContent());
        response.put("currentPage", betsPage.getNumber());
        response.put("totalItems", betsPage.getTotalElements());
        response.put("totalPages", betsPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<BetDTO> createBet(
            @Valid @RequestBody CreateBetRequest createBetRequest,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Bet bet = betService.createBet(
            user.getId(),
            createBetRequest.getPoints(),
            createBetRequest.getGameType(),
            createBetRequest.getTitle(),
            createBetRequest.getDescription()
        );

        BetDTO betDTO = betMapper.toDTO(bet);
        return ResponseEntity.ok(betDTO);
    }

    @PostMapping("/{betId}/accept")
    public ResponseEntity<BetDTO> acceptBet(@PathVariable Long betId, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Bet bet = betService.acceptBet(betId, user.getId());
        BetDTO betDTO = betMapper.toDTO(bet);
        return ResponseEntity.ok(betDTO);
    }

    @PostMapping("/{betId}/cancel")
    public ResponseEntity<Map<String, String>> cancelBet(@PathVariable Long betId, Authentication authentication) {
        String username = authentication.getName();
        betService.cancelBet(betId, username);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Bet cancelled successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{betId}")
    public ResponseEntity<BetDTO> getBet(@PathVariable Long betId) {
        Optional<Bet> bet = betService.findById(betId);
        if (bet.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BetDTO betDTO = betMapper.toDTO(bet.get());
        return ResponseEntity.ok(betDTO);
    }
}