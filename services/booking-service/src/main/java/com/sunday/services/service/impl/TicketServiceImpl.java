package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.BookingPermissions;
import com.sunday.common_lib.enums.TicketStatus;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.Booking;
import com.sunday.services.model.Ticket;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.repository.TicketRepository;
import com.sunday.services.service.BookingAccessService;
import com.sunday.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final BookingAccessService bookingAccess;

    @Override
    @Transactional
    public List<Ticket> generateTicketsForBooking(Booking booking) {
        log.info("Generating tickets for booking: {}", booking.getBookingReference());

        List<Ticket> tickets = booking.getPassengers().stream()
                .map(passenger -> Ticket.builder()
                        .ticketNumber(generateTicketNumber())
                        .status(TicketStatus.BOOKED)
                        .issuedAt(LocalDateTime.now())
                        .booking(booking)
                        .passenger(passenger)
                        .build())
                .collect(Collectors.toList());

        tickets = ticketRepository.saveAll(tickets);

        log.info("Successfully generated {} tickets for booking {}", tickets.size(), booking.getBookingReference());
        return tickets;
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketByNumber(String ticketNumber, UUID userId) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.TICKET_NOT_FOUND_BY_NUMBER, ticketNumber)));
        bookingAccess.requireOwnerOrPermission(ticket.getBooking(), userId, BookingPermissions.TICKET_READ);
        return ticket;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByBooking(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_ID, bookingId)));
        bookingAccess.requireOwnerOrPermission(booking, userId, BookingPermissions.TICKET_READ);
        return ticketRepository.findByBookingIdWithDetails(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByPassenger(UUID passengerId, UUID userId) {
        List<Ticket> tickets = ticketRepository.findByPassengerId(passengerId);
        for (Ticket ticket : tickets) {
            bookingAccess.requireOwnerOrPermission(ticket.getBooking(), userId, BookingPermissions.TICKET_READ);
        }
        return tickets;
    }

    @Override
    @Transactional
    public Ticket cancelTicket(UUID ticketId, UUID userId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        bookingAccess.requireOwnerOrPermission(ticket.getBooking(), userId, BookingPermissions.TICKET_VOID);

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new ConflictException(ErrorMessageUtil.TICKET_USED_CANNOT_CANCEL);
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new ConflictException(ErrorMessageUtil.TICKET_REFUNDED_CANNOT_CANCEL);
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket updatedTicket = ticketRepository.save(ticket);

        log.info("Ticket {} cancelled", ticket.getTicketNumber());
        return updatedTicket;
    }

    @Override
    @Transactional
    public Ticket markTicketAsUsed(UUID ticketId, UUID userId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        bookingAccess.requireAirlinePermission(
                ticket.getBooking().getAirlineId(), userId, BookingPermissions.TICKET_ISSUE);

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ConflictException(ErrorMessageUtil.TICKET_CANCELLED_CANNOT_USE);
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new ConflictException(ErrorMessageUtil.TICKET_REFUNDED_CANNOT_USE);
        }

        ticket.setStatus(TicketStatus.USED);
        Ticket updatedTicket = ticketRepository.save(ticket);

        log.info("Ticket {} marked as used", ticket.getTicketNumber());
        return updatedTicket;
    }

    /** Refunds are an airline decision only: the customer never holds TICKET_REFUND_APPROVE and is not the owner check here. */
    @Override
    @Transactional
    public Ticket refundTicket(UUID ticketId, UUID userId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        bookingAccess.requireAirlinePermission(
                ticket.getBooking().getAirlineId(), userId, BookingPermissions.TICKET_REFUND_APPROVE);

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new ConflictException(ErrorMessageUtil.TICKET_USED_CANNOT_REFUND);
        }

        ticket.setStatus(TicketStatus.REFUNDED);
        Ticket updatedTicket = ticketRepository.save(ticket);

        log.info("Ticket {} refunded", ticket.getTicketNumber());
        return updatedTicket;
    }

    private Ticket findTicketOrThrow(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.TICKET_NOT_FOUND_BY_ID, ticketId)));
    }

    /** Relies on the column's UNIQUE constraint, not a pre-check: a collision on an 8-hex-char random suffix is astronomically unlikely. */
    private String generateTicketNumber() {
        String datePart = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("TKT-%s-%s", datePart, randomPart);
    }
}
