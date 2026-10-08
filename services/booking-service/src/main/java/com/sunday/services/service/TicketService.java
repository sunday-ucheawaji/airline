package com.sunday.services.service;

import com.sunday.services.model.Booking;
import com.sunday.services.model.Ticket;

import java.util.List;

public interface TicketService {

    List<Ticket> generateTicketsForBooking(Booking booking);

    Ticket getTicketByNumber(String ticketNumber, Long userId);

    List<Ticket> getTicketsByBooking(Long bookingId, Long userId);

    List<Ticket> getTicketsByPassenger(Long passengerId, Long userId);

    Ticket cancelTicket(Long ticketId, Long userId);

    Ticket markTicketAsUsed(Long ticketId, Long userId);

    Ticket refundTicket(Long ticketId, Long userId);
}
