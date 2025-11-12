package com.kore.king.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kore.king.entity.User;
import com.kore.king.entity.UserGameId;

@Repository
public interface UserGameIdRepository extends JpaRepository<UserGameId, Long> {
    
    List<UserGameId> findByUser(User user);
    
    List<UserGameId> findByUserOrderByIsDefaultDescGameNameAsc(User user);
    
    Optional<UserGameId> findByUserAndIsDefault(User user, boolean isDefault);
    
    Optional<UserGameId> findByIdAndUser(Long id, User user);
    
    Optional<UserGameId> findFirstByUser(User user);
    
    boolean existsByUserAndGameNameAndGameId(User user, String gameName, String gameId);
    
    long countByUser(User user);
    
    void deleteByUserAndId(User user, Long id);
}