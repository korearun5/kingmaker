package com.kore.king.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kore.king.entity.User;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    
    long countByCreatedAtAfter(LocalDateTime dateTime);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role IN (MAIN_ADMIN, EMPLOYEE_ADMIN)")
    long countAdmins();
    
    @Query("SELECT u FROM User u WHERE u.availablePoints > :minPoints ORDER BY u.availablePoints DESC")
    List<User> findTopPlayersByPoints(@Param("minPoints") int minPoints);
}