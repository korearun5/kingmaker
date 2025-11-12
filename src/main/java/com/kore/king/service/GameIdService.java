package com.kore.king.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kore.king.entity.User;
import com.kore.king.entity.UserGameId;
import com.kore.king.repository.UserGameIdRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GameIdService {

    private final UserGameIdRepository userGameIdRepository;
    private final UserService userService;

    // Maximum number of game IDs per user
    private static final int MAX_GAME_IDS_PER_USER = 10;

    public GameIdService(UserGameIdRepository userGameIdRepository, UserService userService) {
        this.userGameIdRepository = userGameIdRepository;
        this.userService = userService;
    }

    @Transactional
    public UserGameId addGameId(User user, String gameName, String gameId) {
        // Check maximum limit
        long currentCount = userGameIdRepository.countByUser(user);
        if (currentCount >= MAX_GAME_IDS_PER_USER) {
            throw new RuntimeException("Maximum " + MAX_GAME_IDS_PER_USER + " game IDs allowed per user");
        }

        // Check if game ID already exists for this user and game
        if (userGameIdRepository.existsByUserAndGameNameAndGameId(user, gameName, gameId)) {
            throw new RuntimeException("Game ID already exists for this game");
        }

        UserGameId userGameId = new UserGameId(user, gameName, gameId);
        return userGameIdRepository.save(userGameId);
    }

    @Transactional
    public UserGameId updateGameId(Long gameIdId, User user, String gameName, String newGameId) {
        UserGameId existingGameId = userGameIdRepository.findByIdAndUser(gameIdId, user)
                .orElseThrow(() -> new RuntimeException("Game ID not found"));

        // Check if new game ID already exists for this user and game
        if (!existingGameId.getGameId().equals(newGameId) &&
            userGameIdRepository.existsByUserAndGameNameAndGameId(user, gameName, newGameId)) {
            throw new RuntimeException("Game ID already exists for this game");
        }

        existingGameId.setGameName(gameName);
        existingGameId.setGameId(newGameId);

        return userGameIdRepository.save(existingGameId);
    }

    @Transactional
    public void deleteGameId(Long gameIdId, User user) {
        UserGameId gameId = userGameIdRepository.findByIdAndUser(gameIdId, user)
                .orElseThrow(() -> new RuntimeException("Game ID not found"));

        userGameIdRepository.delete(gameId);

        // If the deleted game ID was default, set another one as default
        if (gameId.isDefault()) {
            Optional<UserGameId> firstGameId = userGameIdRepository.findFirstByUser(user);
            firstGameId.ifPresent(otherGameId -> {
                otherGameId.setDefault(true);
                userGameIdRepository.save(otherGameId);
            });
        }
    }

    @Transactional
    public UserGameId setDefaultGameId(Long gameIdId, User user) {
        UserGameId newDefault = userGameIdRepository.findByIdAndUser(gameIdId, user)
                .orElseThrow(() -> new RuntimeException("Game ID not found"));

        // Remove default from all other game IDs for this user
        List<UserGameId> userGameIds = userGameIdRepository.findByUser(user);
        for (UserGameId gameId : userGameIds) {
            if (gameId.isDefault() && !gameId.getId().equals(gameIdId)) {
                gameId.setDefault(false);
                userGameIdRepository.save(gameId);
            }
        }

        newDefault.setDefault(true);
        return userGameIdRepository.save(newDefault);
    }

    public List<UserGameId> getUserGameIds(User user) {
        return userGameIdRepository.findByUserOrderByIsDefaultDescGameNameAsc(user);
    }

    public Optional<UserGameId> getDefaultGameId(User user) {
        return userGameIdRepository.findByUserAndIsDefault(user, true);
    }

    public Optional<UserGameId> getGameIdById(Long id) {
        return userGameIdRepository.findById(id);
    }

    public boolean canAddMoreGameIds(User user) {
        long currentCount = userGameIdRepository.countByUser(user);
        return currentCount < MAX_GAME_IDS_PER_USER;
    }
}