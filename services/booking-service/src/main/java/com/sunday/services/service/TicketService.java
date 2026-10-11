package com.sunday.services.service;

import java.util.UUID;
import com.sunday.services.model.Booking;
import com.sunday.services.model.Ticket;

import java.util.List;

public interface TicketService {

    List<Ticket> generateTicketsForBooking(Booking booking);

    Ticket getTicketByNumber(String ticketNumber, UUID userId);

    List<Ticket> getTicketsByBooking(UUID bookingId, UUID userId);

    List<Ticket> getTicketsByPassenger(UUID passengerId, UUID userId);

    Ticket cancelTicket(UUID ticketId, UUID userId);

    Ticket markTicketAsUsed(UUID ticketId, UUID userId);

    Ticket refundTicket(UUID ticketId, UUID userId);
}
