package com.sunday.services.service;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.services.model.Booking;
import com.sunday.services.model.Ticket;

import java.util.List;

public interface TicketService {

    List<Ticket> generateTicketsForBooking(Booking booking);

    Ticket getTicketByNumber(String ticketNumber) throws ResourceNotFoundException;

    List<Ticket> getTicketsByBooking(Long bookingId);

    List<Ticket> getTicketsByPassenger(Long passengerId);

    Ticket cancelTicket(Long ticketId) throws ResourceNotFoundException;

    Ticket markTicketAsUsed(Long ticketId) throws ResourceNotFoundException;

    Ticket refundTicket(Long ticketId) throws ResourceNotFoundException;
}
