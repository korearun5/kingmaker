package com.kore.king.controller.admin;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kore.king.entity.Bet;
import com.kore.king.service.BetService;

@Controller
@RequestMapping("/admin/dispute-management") // CHANGED from "/admin/disputes"
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController {

    @Autowired
    private BetService betService;

    @GetMapping
    public String viewDisputes(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Page<Bet> disputesPage = betService.findByStatus(BetStatus.DISPUTED, pageable);
        
        // model.addAttribute("disputesPage", disputesPage);
        model.addAttribute("currentPage", page);
        
        return "admin/disputes"; // Keep same template name
    }

    @GetMapping("/{betId}")
    public String disputeDetail(@PathVariable Long betId, Model model) {
        Optional<Bet> betOpt = betService.findById(betId);
        if (betOpt.isEmpty()) {
            model.addAttribute("error", "Bet not found");
            return "redirect:/admin/dispute-management"; // UPDATE redirect
        }

        Bet bet = betOpt.get();
        // Bet opponentBet = bet.getMatchedBet();
        
        // model.addAttribute("bet", bet);
        // model.addAttribute("opponentBet", opponentBet);
        
        return "admin/dispute-detail";
    }

    @PostMapping("/{betId}/resolve")
    public String resolveDispute(@PathVariable Long betId,
                                @RequestParam String winnerUsername,
                                @RequestParam String adminNotes,
                                Model model) {
        try {
            Optional<Bet> betOpt = betService.findById(betId);
            if (betOpt.isEmpty()) {
                model.addAttribute("error", "Bet not found");
                return "redirect:/admin/dispute-management"; // UPDATE redirect
            }

            Bet bet = betOpt.get();
            Bet opponentBet = bet.getMatchedBet();

            // Determine winner
            Bet winningBet = bet.getCreator().getUsername().equals(winnerUsername) ? bet : opponentBet;
            Bet losingBet = winningBet == bet ? opponentBet : bet;

            // Resolve the bet
            betService.resolveBet(winningBet, "Admin decision: " + adminNotes);

            model.addAttribute("success", "Dispute resolved successfully");
            
        } catch (Exception e) {
            model.addAttribute("error", "Error resolving dispute: " + e.getMessage());
        }

        return "redirect:/admin/dispute-management"; // UPDATE redirect
    }
}