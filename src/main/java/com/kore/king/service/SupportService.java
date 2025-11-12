package com.kore.king.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kore.king.entity.*;
import com.kore.king.repository.SupportTicketRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SupportService {

    private final SupportTicketRepository supportTicketRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final BetService betService;

    public SupportService(SupportTicketRepository supportTicketRepository,
                         FileStorageService fileStorageService,
                         UserService userService,
                         BetService betService) {
        this.supportTicketRepository = supportTicketRepository;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
        this.betService = betService;
    }

    @Transactional
    public SupportTicket createTicket(User user, TicketCategory category, String title, 
                                    String description, Long relatedBetId, 
                                    List<MultipartFile> attachments) {
        SupportTicket ticket = new SupportTicket(user, category, title, description);

        // Set related bet if provided
        if (relatedBetId != null) {
            Bet relatedBet = betService.findById(relatedBetId)
                    .orElseThrow(() -> new RuntimeException("Related bet not found"));
            ticket.setRelatedBet(relatedBet);
        }

        // Handle attachments
        if (attachments != null) {
            for (MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String filePath = fileStorageService.storeFile(file);
                    ticket.addAttachment(filePath);
                }
            }
        }

        // Add initial message from user
        TicketMessage initialMessage = new TicketMessage(ticket, user, description, false);
        ticket.addMessage(initialMessage);

        return supportTicketRepository.save(ticket);
    }

    @Transactional
    public TicketMessage addMessage(Long ticketId, User user, String message, 
                                  boolean isAdminResponse, List<MultipartFile> attachments) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Update ticket status based on who is responding
        if (isAdminResponse && ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        } else if (!isAdminResponse && ticket.getStatus() == TicketStatus.RESOLVED) {
            ticket.setStatus(TicketStatus.REOPENED);
        }

        TicketMessage ticketMessage = new TicketMessage(ticket, user, message, isAdminResponse);

        // Handle attachments
        if (attachments != null) {
            for (MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String filePath = fileStorageService.storeFile(file);
                    ticketMessage.addAttachment(filePath);
                }
            }
        }

        ticket.addMessage(ticketMessage);
        supportTicketRepository.save(ticket);

        return ticketMessage;
    }

    public Page<SupportTicket> getUserTickets(Long userId, Pageable pageable) {
        return supportTicketRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
    }

    public Page<SupportTicket> getAllTickets(Pageable pageable) {
        return supportTicketRepository.findAllByOrderByUpdatedAtDesc(pageable);
    }

    public Page<SupportTicket> getTicketsByStatus(TicketStatus status, Pageable pageable) {
        return supportTicketRepository.findByStatusOrderByUpdatedAtDesc(status, pageable);
    }

    public Optional<SupportTicket> getTicketById(Long ticketId) {
        return supportTicketRepository.findByIdWithMessages(ticketId);
    }

    @Transactional
    public SupportTicket updateTicketStatus(Long ticketId, TicketStatus status, User updatedBy) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.setStatus(status);
        
        if (status == TicketStatus.CLOSED || status == TicketStatus.RESOLVED) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        // Add system message about status change
        String statusMessage = String.format("Ticket status changed to %s by %s", 
                status.toString(), updatedBy.getUsername());
        TicketMessage systemMessage = new TicketMessage(ticket, updatedBy, statusMessage, true);
        ticket.addMessage(systemMessage);

        return supportTicketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket updateTicketPriority(Long ticketId, TicketPriority priority, User updatedBy) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.setPriority(priority);

        // Add system message about priority change
        String priorityMessage = String.format("Ticket priority changed to %s by %s", 
                priority.toString(), updatedBy.getUsername());
        TicketMessage systemMessage = new TicketMessage(ticket, updatedBy, priorityMessage, true);
        ticket.addMessage(systemMessage);

        return supportTicketRepository.save(ticket);
    }

    public long getOpenTicketCount() {
        return supportTicketRepository.countByStatusIn(
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.REOPENED));
    }

    public long getUserOpenTicketCount(Long userId) {
        return supportTicketRepository.countByUserIdAndStatusIn(userId,
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.REOPENED));
    }
}