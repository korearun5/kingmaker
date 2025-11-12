package com.kore.king.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kore.king.entity.SupportTicket;
import com.kore.king.entity.TicketStatus;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    
    Page<SupportTicket> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
    
    Page<SupportTicket> findAllByOrderByUpdatedAtDesc(Pageable pageable);
    
    Page<SupportTicket> findByStatusOrderByUpdatedAtDesc(TicketStatus status, Pageable pageable);
    
    @Query("SELECT t FROM SupportTicket t LEFT JOIN FETCH t.messages WHERE t.id = :ticketId")
    Optional<SupportTicket> findByIdWithMessages(@Param("ticketId") Long ticketId);
    
    long countByStatusIn(List<TicketStatus> statuses);
    
    long countByUserIdAndStatusIn(Long userId, List<TicketStatus> statuses);
    
    @Query("SELECT t FROM SupportTicket t WHERE " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<SupportTicket> searchTickets(@Param("query") String query, Pageable pageable);
}