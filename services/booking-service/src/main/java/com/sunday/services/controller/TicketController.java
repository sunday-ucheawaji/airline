package com.sunday.services.controller;

import com.sunday.common_lib.payload.response.TicketResponse;
import com.sunday.services.mapper.TicketMapper;
import com.sunday.services.model.Ticket;
import com.sunday.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/{ticketNumber}")
    public ResponseEntity<TicketResponse> getTicketByNumber(
            @PathVariable String ticketNumber,
            @RequestHeader("X-User-Id") Long userId) {
        Ticket ticket = ticketService.getTicketByNumber(ticketNumber, userId);
        return ResponseEntity.ok(TicketMapper.toResponse(ticket));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<TicketResponse>> getTicketsByBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        List<Ticket> tickets = ticketService.getTicketsByBooking(bookingId, userId);
        return ResponseEntity.ok(toResponses(tickets));
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<TicketResponse>> getTicketsByPassenger(
            @PathVariable Long passengerId,
            @RequestHeader("X-User-Id") Long userId) {
        List<Ticket> tickets = ticketService.getTicketsByPassenger(passengerId, userId);
        return ResponseEntity.ok(toResponses(tickets));
    }

    @PutMapping("/{ticketId}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(
            @PathVariable Long ticketId,
            @RequestHeader("X-User-Id") Long userId) {
        Ticket ticket = ticketService.cancelTicket(ticketId, userId);
        return ResponseEntity.ok(TicketMapper.toResponse(ticket));
    }

    @PutMapping("/{ticketId}/use")
    public ResponseEntity<TicketResponse> markTicketAsUsed(
            @PathVariable Long ticketId,
            @RequestHeader("X-User-Id") Long userId) {
        Ticket ticket = ticketService.markTicketAsUsed(ticketId, userId);
        return ResponseEntity.ok(TicketMapper.toResponse(ticket));
    }

    @PutMapping("/{ticketId}/refund")
    public ResponseEntity<TicketResponse> refundTicket(
            @PathVariable Long ticketId,
            @RequestHeader("X-User-Id") Long userId) {
        Ticket ticket = ticketService.refundTicket(ticketId, userId);
        return ResponseEntity.ok(TicketMapper.toResponse(ticket));
    }

    private List<TicketResponse> toResponses(List<Ticket> tickets) {
        return tickets.stream().map(TicketMapper::toResponse).collect(Collectors.toList());
    }
}
